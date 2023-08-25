package nl.joepeding.sqldelight.mysqldriver

import app.cash.sqldelight.db.SqlCursor
import kotlinx.cinterop.*
import mysql.*

class MySQLCursor(
    val stmt: CPointer<MYSQL_STMT>
) : SqlCursor {
    private val memScope: Arena = Arena()
    private val buffers: MutableList<CVariable> = mutableListOf()
    private var bindings: CArrayPointer<MYSQL_BIND>
    private var lengths: CArrayPointer<CPointerVar<ULongVar>>
    private var nulls: CArrayPointer<CPointerVar<ByteVar>>

    init {
        val meta = mysql_stmt_result_metadata(stmt)
        val fieldCount: Int = mysql_num_fields(meta).toInt()

        // Buffer full response so max length of each column is known
        require(
            mysql_stmt_attr_set(
                stmt,
                enum_stmt_attr_type.STMT_ATTR_UPDATE_MAX_LENGTH,
                memScope.alloc<BooleanVar>().also { it.value = true }.ptr
            ) == 0.toByte()
        ) { "Error setting MySQL statement attribute: ${ stmt.error() }"}
        require(mysql_stmt_store_result(stmt) == 0) { "Error storing MySQL result: ${ stmt.error() }"}

        // Create bindings
        bindings = memScope.allocArray(fieldCount)
        lengths = memScope.allocArray(fieldCount)
        nulls = memScope.allocArray(fieldCount)
        (0 until fieldCount).forEach { index ->
            val field = mysql_fetch_field(meta)!!.pointed
            println("$index: ${field.name!!.toKString()} - ${field.type} - ${field.length}")
            val buffer = when (field.type) {
                MYSQL_TYPE_TINY -> memScope.alloc<ByteVar>() // MySQL BOOLEAN is an alias for TINYINT(1)
                MYSQL_TYPE_SHORT,
                MYSQL_TYPE_LONG,
                MYSQL_TYPE_INT24,
                MYSQL_TYPE_LONGLONG,
                MYSQL_TYPE_TIMESTAMP,
                MYSQL_TYPE_TIMESTAMP2 -> memScope.alloc<LongVar>()
                MYSQL_TYPE_DECIMAL,
                MYSQL_TYPE_NEWDECIMAL,
                MYSQL_TYPE_FLOAT,
                MYSQL_TYPE_DOUBLE -> memScope.alloc<DoubleVar>()
                MYSQL_TYPE_BIT -> TODO()
                MYSQL_TYPE_DATE -> TODO()
                MYSQL_TYPE_TIME,
                MYSQL_TYPE_TIME2 -> TODO()
                MYSQL_TYPE_DATETIME,
                MYSQL_TYPE_DATETIME2 -> TODO()
                MYSQL_TYPE_YEAR -> TODO()
                MYSQL_TYPE_STRING,
                MYSQL_TYPE_VAR_STRING,
                MYSQL_TYPE_BLOB -> memScope.allocArray<ByteVar>(field.max_length.toInt()).pointed
                MYSQL_TYPE_SET -> TODO()
                MYSQL_TYPE_ENUM -> TODO()
                MYSQL_TYPE_GEOMETRY -> TODO()
                MYSQL_TYPE_NULL -> TODO()
                else -> { error("Encountered unknown field type: ${field.type}") }
            }
            bindings[index].buffer_type = field.type
            bindings[index].buffer = buffer.ptr
            bindings[index].buffer_length = field.max_length
            lengths[index] = memScope.alloc<ULongVar>().ptr
            bindings[index].length = lengths[index]
            nulls[index] = memScope.alloc<ByteVar>().ptr
            bindings[index].is_null = nulls[index]
            buffers.add(buffer)
        }
        mysql_stmt_bind_result(stmt, bindings)
    }

    override fun getBoolean(index: Int): Boolean? {
        println("Fetch bool (null=${isNullByIndex(index)}): ${buffers[index].reinterpret<ByteVar>().value}")
        if (isNullByIndex(index)) { return null }
        return buffers[index].reinterpret<ByteVar>().value != 0.toUInt().toByte()
    }

    override fun getBytes(index: Int): ByteArray? {
        val bytes = interpretCPointer<CArrayPointerVar<ByteVar>>(buffers[index].rawPtr)
            ?.pointed
            ?.readValues<ByteVar>(lengths[index]!!.pointed.value.toInt(), alignOf<ByteVar>())
            ?.getBytes()
        println("Fetch bytes (null=${isNullByIndex(index)}): ${bytes?.joinToString(" ") { it.toString(16) }}")
        if (isNullByIndex(index)) { return null }
        return bytes
    }

    override fun getDouble(index: Int): Double? {
        println("Fetch double (null=${isNullByIndex(index)}): ${buffers[index].reinterpret<DoubleVar>().value}")
        if (isNullByIndex(index)) { return null }
        return buffers[index].reinterpret<DoubleVar>().value
    }

    override fun getLong(index: Int): Long? {
        println("Fetch long (null=${isNullByIndex(index)}): ${buffers[index].reinterpret<LongVarOf<Long>>().value}")
        if (isNullByIndex(index)) { return null }
        return buffers[index].reinterpret<LongVar>().value
    }

    override fun getString(index: Int): String? {
        val string = interpretCPointer<CArrayPointerVar<ByteVar>>(buffers[index].rawPtr)
            ?.pointed
            ?.readValues<ByteVar>(lengths[index]!!.pointed.value.toInt(), alignOf<ByteVar>())
            ?.getBytes()
            ?.joinToString("") { Char(it.toInt()).toString() }
        println("Fetch string (null = ${isNullByIndex(index)}): $string (${string?.length} chars)")
        if (isNullByIndex(index)) { return null }
        return string
    }

    // TODO: Might need rebinding of buffers for every fetch, because otherwise the pass-by-reference nature
    //       of kotlin will overwrite (or: the `get`-functions here should return copies of what's in the buffers.
    //       Might not be a problem if the conversion from C-type to Kotlin-type also copies.
    // TODO: Better exception type?
    override fun next(): Boolean = mysql_stmt_fetch(stmt).let {
        println("Next row")
        return@let when (it) {
            0 -> true
            MYSQL_NO_DATA -> false
            1 -> throw Exception("Error fetching next row: ${mysql_stmt_error(stmt)?.toKString()}")
            MYSQL_DATA_TRUNCATED -> throw Exception("MySQL stmt fetch MYSQL_DATA_TRUNCATED")
            else -> throw Exception("Unexpected result for `mysql_stmt_fetch`: $it")
        }
    }

    private fun isNullByIndex(index: Int): Boolean = nulls[index]!!.reinterpret<ByteVar>().pointed.value == true.toByte()

    fun clear(): Unit {
        println("Clearing")
        memScope.clear()
        println("Clearing done")
    }
}