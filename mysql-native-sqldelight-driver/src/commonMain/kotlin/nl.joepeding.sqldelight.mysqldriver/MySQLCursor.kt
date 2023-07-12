package nl.joepeding.sqldelight.mysqldriver

import app.cash.sqldelight.db.SqlCursor
import kotlinx.cinterop.*
import mysql.*
import platform.darwin.StringPtrVar

class MySQLCursor(
    val stmt: CPointer<MYSQL_STMT>
) : SqlCursor {
    val memScope: Arena = Arena()
    val buffers: MutableList<CVariable> = mutableListOf()
    lateinit var bindings: CArrayPointer<MYSQL_BIND>


    init {
        val meta = mysql_stmt_result_metadata(stmt)
        val fieldCount: Int = mysql_num_fields(meta).toInt()
        bindings = memScope.allocArray<MYSQL_BIND>(fieldCount)
        (0 until fieldCount).forEach { index ->
            val field = mysql_fetch_field(meta)!!.pointed
            println("$index: ${field.name!!.toKString()} - ${field.type} - ${field.length}")
            val buffer = when (field.type) {
                MYSQL_TYPE_TINY,
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
                MYSQL_TYPE_BLOB -> memScope.alloc<StringPtrVar>()
                MYSQL_TYPE_SET -> TODO()
                MYSQL_TYPE_ENUM -> TODO()
                MYSQL_TYPE_GEOMETRY -> TODO()
                MYSQL_TYPE_NULL -> TODO()
                else -> { error("Encountered unknown field type: ${field.type}") }
            }
            bindings[index].buffer_type = field.type
            bindings[index].buffer = buffer.ptr
            bindings[index].buffer_length = (1000).toULong()
            buffers.add(buffer)
        }
        println("---")
        println("vc: ${MYSQL_TYPE_STRING}")
        println("short: ${MYSQL_TYPE_SHORT}")
        println("bytes: ${MYSQL_TYPE_BLOB}")
        println("double: ${MYSQL_TYPE_DOUBLE}")
        println("longlong: ${MYSQL_TYPE_LONGLONG}")
        println("tiny: ${MYSQL_TYPE_TINY}")

        mysql_stmt_bind_result(stmt, bindings)
        println("Bound")
    }

    override fun getBoolean(index: Int): Boolean? {
        println("Fetch: ${buffers[index].reinterpret<LongVar>().value}")
        return buffers[index].reinterpret<LongVar>().value != 0L
//        println("prefetch: ${(buffers[index] as LongVarOf<*>).value}")
//        require(buffers[index] is LongVarOf<*>) { "Column with index $index is not castable to boolean" }
//        return (buffers[index] as LongVarOf<*>).value != 0L
    }

    override fun getBytes(index: Int): ByteArray? {
        TODO("Not yet implemented")
    }

    override fun getDouble(index: Int): Double? {
        TODO("Not yet implemented")
    }

    override fun getLong(index: Int): Long? {
        println("Fetch-b: ${buffers[index].reinterpret<LongVarOf<Long>>().value}")
        return buffers[index].reinterpret<LongVar>().value
    }

    override fun getString(index: Int): String? {
        TODO("Not yet implemented")
    }

    // TODO: Might need rebinding of buffers for every fetch, because otherwise the pass-by-reference nature
    //       of kotlin will overwrite (or: the `get`-functions here should return copies of what's in the buffers.
    //       Might not be a problem if the conversion from C-type to Kotlin-type also copies.
    // TODO: Better exception type?
    override fun next(): Boolean = mysql_stmt_fetch(stmt).let {
        when (it) {
            0 -> true
            MYSQL_NO_DATA -> false
            1 -> throw Exception("Error fetching next row: ${mysql_stmt_error(stmt)?.toKString()}")
            MYSQL_DATA_TRUNCATED -> throw Exception("MySQL stmt fetch MYSQL_DATA_TRUNCATED")
            else -> throw Exception("Unexpected result for `mysql_stmt_fetch`: $it")
        }
    }
}