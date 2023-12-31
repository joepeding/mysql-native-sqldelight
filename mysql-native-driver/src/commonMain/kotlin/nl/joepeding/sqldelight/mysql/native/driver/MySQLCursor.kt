package nl.joepeding.sqldelight.mysql.native.driver

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.QueryResult.Value
import app.cash.sqldelight.db.SqlCursor
import co.touchlab.kermit.CommonWriter
import co.touchlab.kermit.DefaultFormatter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.loggerConfigInit
import kotlinx.cinterop.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import mysql.*
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

/**
 * MySQL cursor used to iterate over rows in the result and extract
 * fields as typed variables.
 *
 * Intended to be used through [MySQLNativeDriver] along with a mapper that converts the complete
 * result into some collection.
 *
 * @param stmt `CPointer` to a `MYSQL_STMT` that has been executed and has no errors
 */
class MySQLCursor(
    private val stmt: CPointer<MYSQL_STMT>
) : SqlCursor {
    private val memScope: Arena = Arena()
    private val buffers: MutableList<CVariable> = mutableListOf()
    private var bindings: CArrayPointer<MYSQL_BIND>
    private var lengths: CArrayPointer<CPointerVar<ULongVar>>
    private var nulls: CArrayPointer<CPointerVar<BooleanVar>>
    private val log = Logger(
        loggerConfigInit(CommonWriter(DefaultFormatter)),
        this::class.qualifiedName ?: this::class.toString()
    )

    init {
        val meta = mysql_stmt_result_metadata(stmt)
        val fieldCount: Int = mysql_num_fields(meta).toInt()

        // Buffer full response so max length of each column is known
        require(
            mysql_stmt_attr_set( // From docs: "Return values: Zero for success. Nonzero if option is unknown."
                stmt,
                enum_stmt_attr_type.STMT_ATTR_UPDATE_MAX_LENGTH,
                memScope.alloc<BooleanVar>().also { it.value = true }.ptr
            ).not() // False is 'success', so it needs inversion
        ) { "Error setting MySQL statement attribute: ${ stmt.error() }"}
        require(mysql_stmt_store_result(stmt) == 0) { "Error storing MySQL result: ${ stmt.error() }"}

        // Create bindings
        bindings = memScope.allocArray(fieldCount)
        lengths = memScope.allocArray(fieldCount)
        nulls = memScope.allocArray(fieldCount)
        (0 until fieldCount).forEach { index ->
            val field = mysql_fetch_field(meta)?.pointed ?: throw IndexOutOfBoundsException(
                "Did not find MYSQL_FIELD where one was expected."
            )
            val buffer = bufferForField(field).also { buffers.add(it) }
            val length = memScope.alloc<ULongVar>().ptr.also { lengths[index] = it }
            val isNull = memScope.alloc<BooleanVar>().ptr.also { nulls[index] = it }
            bindings[index].apply {
                this.buffer_type = field.type
                this.buffer = buffer.ptr
                this.buffer_length = field.max_length
                this.length = length
                this.is_null = isNull
            }
        }
        mysql_stmt_bind_result(stmt, bindings)
    }

    private fun bufferForField(field: MYSQL_FIELD) = when (field.type) {
        MYSQL_TYPE_TINY -> memScope.alloc<ByteVar>() // MySQL BOOLEAN is an alias for TINYINT(1)
        MYSQL_TYPE_SHORT,
        MYSQL_TYPE_LONG,
        MYSQL_TYPE_INT24,
        MYSQL_TYPE_BIT,
        MYSQL_TYPE_LONGLONG,
        MYSQL_TYPE_YEAR -> memScope.alloc<LongVar>()
        MYSQL_TYPE_DECIMAL,
        MYSQL_TYPE_NEWDECIMAL,
        MYSQL_TYPE_FLOAT,
        MYSQL_TYPE_DOUBLE -> memScope.alloc<DoubleVar>()
        MYSQL_TYPE_DATE,
        MYSQL_TYPE_TIME,
        MYSQL_TYPE_TIME2,
        MYSQL_TYPE_DATETIME,
        MYSQL_TYPE_DATETIME2,
        MYSQL_TYPE_TIMESTAMP,
        MYSQL_TYPE_TIMESTAMP2 -> memScope.alloc<MYSQL_TIME>()
        MYSQL_TYPE_STRING,
        MYSQL_TYPE_VAR_STRING,
        MYSQL_TYPE_VARCHAR,
        MYSQL_TYPE_TINY_BLOB,
        MYSQL_TYPE_MEDIUM_BLOB,
        MYSQL_TYPE_LONG_BLOB,
        MYSQL_TYPE_BLOB,
        MYSQL_TYPE_SET,
        MYSQL_TYPE_ENUM -> memScope.allocArray<ByteVar>(field.max_length.toInt()).pointed
        MYSQL_TYPE_GEOMETRY -> throw IllegalStateException("GEOMETRY field type not supported, use ST_AsText or ST_AsBinary to read as String or ByteArray")
        MYSQL_TYPE_NULL -> TODO("Can not find documentation about what a NULL-type column is for.")
        else -> { error("Encountered unknown field type: ${field.type}") }
    }

    /**
     * Converts the buffer for provided column index to a Boolean?
     *
     * Returns null if that index was `NULL` in the result.
     *
     * @param index Integer index of the column to read
     */
    override fun getBoolean(index: Int): Boolean? {
        log.d { "Fetch bool (null=${isNullByIndex(index)}): ${buffers[index].reinterpret<ByteVar>().value}" }
        if (isNullByIndex(index)) { return null }
        return buffers[index].reinterpret<ByteVar>().value != 0.toUInt().toByte()
    }

    /**
     * Converts the buffer for provided column index to a ByteArray?
     *
     * Returns null if that index was `NULL` in the result.
     *
     * @param index Integer index of the column to read
     */
    override fun getBytes(index: Int): ByteArray? {
        val bytes = interpretCPointer<CArrayPointerVar<ByteVar>>(buffers[index].rawPtr)
            ?.pointed
            ?.readValues<ByteVar>(lengths[index]!!.pointed.value.toInt(), alignOf<ByteVar>())
            ?.getBytes()
        log.d { "Fetch bytes (null=${isNullByIndex(index)}): ${bytes?.joinToString(" ") { it.toString(16) }}" }
        if (isNullByIndex(index)) { return null }
        return bytes
    }

    /**
     * Converts the buffer for provided column index to a Double?
     *
     * Returns null if that index was `NULL` in the result.
     *
     * @param index Integer index of the column to read
     */
    override fun getDouble(index: Int): Double? {
        log.d { "Fetch double (null=${isNullByIndex(index)}): ${buffers[index].reinterpret<DoubleVar>().value}" }
        if (isNullByIndex(index)) { return null }
        return buffers[index].reinterpret<DoubleVar>().value
    }

    /**
     * Converts the buffer for provided column index to a Long?
     *
     * Returns null if that index was `NULL` in the result.
     *
     * @param index Integer index of the column to read
     */
    override fun getLong(index: Int): Long? {
        log.d { "Fetch long (null=${isNullByIndex(index)}): ${buffers[index].reinterpret<LongVarOf<Long>>().value}" }
        if (isNullByIndex(index)) { return null }
        return buffers[index].reinterpret<LongVar>().value
    }

    /**
     * Converts the buffer for provided column index to a String?
     *
     * Returns null if that index was `NULL` in the result.
     * Conversion of the result to `String` happens as follows:
     * - For column with `DATE` type, delegates to [getDate], then converts to string
     * - For column with `TIME`/`TIME2` type, delegates to [getDuration], then converts to String
     * - For column with `DATETIME`/`TIMESTAMP`/`DATETIME2`/`TIMESTAMP2` type, delegates to [getDateTime], converts to String
     * - Otherwise, delegates to [getBytes] and converts individual bytes to chars.
     *
     * @param index Integer index of the column to read
     */
    override fun getString(index: Int): String? {
        val string: String? = when (bindings[index].buffer_type) {
            MYSQL_TYPE_DATE -> getDate(index)?.toString()
            MYSQL_TYPE_TIME,
            MYSQL_TYPE_TIME2 -> getDuration(index)?.toIsoString()
            MYSQL_TYPE_DATETIME,
            MYSQL_TYPE_DATETIME2,
            MYSQL_TYPE_TIMESTAMP,
            MYSQL_TYPE_TIMESTAMP2 -> getDateTime(index)?.toString()
            else -> interpretCPointer<CArrayPointerVar<ByteVar>>(buffers[index].rawPtr)
                ?.pointed
                ?.readValues<ByteVar>(lengths[index]!!.pointed.value.toInt(), alignOf<ByteVar>())
                ?.getBytes()
                ?.also { log.d { it.joinToString(" ") { it.toString(radix = 16) } } }
                ?.joinToString("") { Char(it.toInt()).toString() }
        }
        log.d { "Fetch string (null=${isNullByIndex(index)}): $string (${string?.length} chars)" }
        if (isNullByIndex(index)) { return null }
        return string
    }

    /**
     * Converts the buffer for provided column index to a kotlinx.datetime.LocalDate?
     *
     * Returns null if that index was `NULL` in the result.
     *
     * @param index Integer index of the column to read
     */
    fun getDate(index: Int): LocalDate? {
        if (isNullByIndex(index)) { return null }
        val date = buffers[index].reinterpret<MYSQL_TIME>()
        return LocalDate(date.year.toInt(), date.month.toInt(), date.day.toInt())
    }

    /**
     * Converts the buffer for provided column index to a kotlinx.datetime.LocalDateTime?
     *
     * Returns null if that index was `NULL` in the result.
     *
     * @param index Integer index of the column to read
     */
    fun getDateTime(index: Int): LocalDateTime? {
        if (isNullByIndex(index)) { return null }
        val datetime = buffers[index].reinterpret<MYSQL_TIME>()
        return LocalDateTime(
            datetime.year.toInt(),
            datetime.month.toInt(),
            datetime.day.toInt(),
            datetime.hour.toInt(),
            datetime.minute.toInt(),
            datetime.second.toInt(),
            // MySQL stores partial seconds in up to 6 digits, but we need 9 for nanoseconds
            datetime.second_part.toString().padEnd(9, '0').toInt()
        )
    }

    /**
     * Converts the buffer for provided column index to a kotlin.time.Duration?
     *
     * Returns null if that index was `NULL` in the result.
     *
     * @param index Integer index of the column to read
     */
    fun getDuration(index: Int): Duration? {
        if (isNullByIndex(index)) { return null }
        val time = buffers[index].reinterpret<MYSQL_TIME>()
        return  time.hour.toInt().toDuration(DurationUnit.HOURS) +
                time.minute.toInt().toDuration(DurationUnit.MINUTES) +
                time.second.toInt().toDuration(DurationUnit.SECONDS) +
                time.second_part.toString().padEnd(9, '0').toInt().toDuration(DurationUnit.NANOSECONDS)
    }

    /**
     * Move the cursor to the next row
     *
     * Returns a `QueryResult.Value` with `true` for success and `false` when there are no more rows.
     *
     * @param index Integer index of the column to read
     * @throws Exception when data was truncated
     * @throws IllegalStateException for unexpected return codes from `mysql_stmt_fetch`.
     */
    override fun next(): QueryResult<Boolean> = mysql_stmt_fetch(stmt).let {
        log.d { "Next row" }
        return@let when (it) {
            0 -> true
            MYSQL_NO_DATA -> false
            1 -> throw IllegalStateException("Error fetching next row: ${mysql_stmt_error(stmt)?.toKString()}")
            MYSQL_DATA_TRUNCATED -> throw Exception("MySQL stmt fetch MYSQL_DATA_TRUNCATED")
            else -> throw IllegalStateException("Unexpected result for `mysql_stmt_fetch`: $it")
        }
    }.let { Value(it) }

    private fun isNullByIndex(index: Int): Boolean = nulls[index]!!.reinterpret<ByteVar>().pointed.value == true.toByte()

    /**
     * Clears the internal `Arena` used holding references to memory allocated in C-interoperability.
     */
    fun clear(): Unit {
        log.d { "Clearing" }
        memScope.clear()
        log.d { "Clearing done" }
    }
}