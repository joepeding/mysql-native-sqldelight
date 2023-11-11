package nl.joepeding.sqldelight.mysqldriver

import app.cash.sqldelight.db.SqlPreparedStatement
import co.touchlab.kermit.*
import kotlinx.cinterop.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import mysql.*
import kotlin.time.Duration

public class MySQLPreparedStatement(
    private val statement: CPointer<MYSQL_STMT>,
    private val parameters: Int
): SqlPreparedStatement {
    private val memScope: Arena = Arena()
    private val log = Logger(
        loggerConfigInit(CommonWriter(DefaultFormatter)),
        this::class.qualifiedName ?: this::class.toString()
    )
    public val bindings = memScope.allocArray<MYSQL_BIND>(parameters)

    override fun bindBoolean(index: Int, boolean: Boolean?) {
        log.d { "Binding boolean" }
        bindings[index].apply {
            buffer_type = MYSQL_TYPE_SHORT
            buffer = boolean?.let {
                memScope.alloc<BooleanVarOf<Boolean>>().apply { value = boolean }.ptr
            }
            buffer_length = sizeOf<BooleanVar>().toULong()
            is_null = memScope.alloc<BooleanVar>().apply { value = (boolean == null) }.ptr
        }
    }

    override fun bindBytes(index: Int, bytes: ByteArray?) {
        log.d { "Binding bytes" }
        val cRepresentation = bytes?.toCValues()
        bindings[index].apply {
            buffer_type = MYSQL_TYPE_BLOB
            buffer = cRepresentation?.getPointer(memScope)
            buffer_length = cRepresentation?.size?.toULong() ?: 0.toULong()
            is_null = memScope.alloc<BooleanVar>().apply { value = (bytes == null) }.ptr
        }
    }

    override fun bindDouble(index: Int, double: Double?) {
        log.d { "Binding double" }
        bindings[index].apply {
            buffer_type = MYSQL_TYPE_DOUBLE
            buffer = double?.let {
                memScope.alloc<DoubleVar>().apply { value = double }.ptr
            }
            buffer_length = sizeOf<DoubleVar>().toULong()
            is_null = memScope.alloc<BooleanVar>().apply { value = (double == null) }.ptr
        }
    }

    override fun bindLong(index: Int, long: Long?) {
        log.d { "Binding long" }
        bindings[index].apply {
            buffer_type = MYSQL_TYPE_LONGLONG
            buffer = long?.let {
                memScope.alloc<LongVarOf<Long>>().apply { value = long }.ptr
            }
            buffer_length = sizeOf<LongVarOf<Long>>().toULong()
            is_null = memScope.alloc<BooleanVar>().apply { value = (long == null) }.ptr
        }
    }

    override fun bindString(index: Int, string: String?) {
        log.d { "Binding string" }
        bindings[index].apply {
            buffer_type = MYSQL_TYPE_STRING
            buffer = string?.cstr?.getPointer(memScope)
            buffer_length = (string?.length ?: 0).toULong()
            is_null = memScope.alloc<BooleanVar>().apply { value = (string == null) }.ptr
        }
    }

    fun bindDate(index: Int, date: LocalDate?) {
        log.d { "Binding date" }
        bindings[index].apply {
            is_null = memScope.alloc<BooleanVar>().apply { value = (date == null) }.ptr
            if (date == null) { return }
            buffer_type = MYSQL_TYPE_DATE
            buffer = memScope.alloc<MYSQL_TIME>().also {
                it.year = date.year.toUInt()
                it.month = date.monthNumber.toUInt()
                it.day = date.dayOfMonth.toUInt()
            }.ptr
            buffer_length = sizeOf<MYSQL_TIME>().toULong()

        }
    }

    // TODO: Test
    // TODO: Time zones
    // TODO: Partial seconds
    fun bindDateTime(index: Int, dateTime: LocalDateTime?) {
        log.d { "Binding date" }
        bindings[index].apply {
            is_null = memScope.alloc<BooleanVar>().apply { value = (dateTime == null) }.ptr
            if (dateTime == null) { return }
            buffer_type = MYSQL_TYPE_DATETIME
            buffer = memScope.alloc<MYSQL_TIME>().also {
                it.year = dateTime.year.toUInt()
                it.month = dateTime.monthNumber.toUInt()
                it.day = dateTime.dayOfMonth.toUInt()
                it.hour = dateTime.hour.toUInt()
                it.minute = dateTime.minute.toUInt()
                it.second = dateTime.second.toUInt()
                it.second_part = (dateTime.nanosecond / 1000).toULong()
            }.ptr
            buffer_length = sizeOf<MYSQL_TIME>().toULong()
        }
    }

    // TODO: Test (esp with durations of > 24 hours and negative durations)
    fun bindDuration(index: Int, duration: Duration?) {
        log.d { "Binding date" }
        bindings[index].apply {
            is_null = memScope.alloc<BooleanVar>().apply { value = (duration == null) }.ptr
            if (duration == null) { return }
            buffer_type = MYSQL_TYPE_DATETIME
            buffer = memScope.alloc<MYSQL_TIME>().also {
                duration.toComponents { h, m, s, ns ->
                    it.hour = h.toUInt()
                    it.minute = m.toUInt()
                    it.second = s.toUInt()
                    it.second_part = (ns / 1000).toULong()
                }
            }.ptr
            buffer_length = sizeOf<MYSQL_TIME>().toULong()
        }
    }

    public fun clear() {
        log.d { "Clearing" }
        memScope.clear()
        log.d { "Cleared" }
    }

    companion object {
        fun prepareStatement(conn: CPointer<MYSQL>, sql: String): CPointer<MYSQL_STMT> {
            val stmt = mysql_stmt_init(conn) ?: throw OutOfMemoryError("Failed to initialize statement, out of memory")
            val result = mysql_stmt_prepare(stmt, sql, sql.length.toULong())

            require(result == 0) { "Error preparing statement: ${stmt.error()}" }

            return stmt
        }
    }
}