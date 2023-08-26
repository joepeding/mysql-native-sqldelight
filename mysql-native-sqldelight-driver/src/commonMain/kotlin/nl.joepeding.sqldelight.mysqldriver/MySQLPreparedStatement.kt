package nl.joepeding.sqldelight.mysqldriver

import app.cash.sqldelight.db.SqlPreparedStatement
import kotlinx.cinterop.*
import mysql.*

public class MySQLPreparedStatement(
    private val statement: CPointer<MYSQL_STMT>,
    private val parameters: Int
): SqlPreparedStatement {
    private val memScope: Arena = Arena()
    public val bindings = memScope.allocArray<MYSQL_BIND>(parameters)

    override fun bindBoolean(index: Int, boolean: Boolean?) {
        println("Binding boolean")
        bindings[index].apply {
            buffer_type = MYSQL_TYPE_SHORT
            buffer = boolean?.let {
                memScope.alloc<BooleanVarOf<Boolean>>().apply { value = boolean }.ptr
            }
            buffer_length = sizeOf<BooleanVar>().toULong()
            is_null = memScope.alloc<ByteVar>().apply { value = (boolean == null).toByte() }.ptr
        }
    }

    override fun bindBytes(index: Int, bytes: ByteArray?) {
        println("Binding bytes")
        val cRepresentation = bytes?.toCValues()
        bindings[index].apply {
            buffer_type = MYSQL_TYPE_BLOB
            buffer = cRepresentation?.getPointer(memScope)
            buffer_length = cRepresentation?.size?.toULong() ?: 0.toULong()
            is_null = memScope.alloc<ByteVar>().apply { value = (bytes == null).toByte() }.ptr
        }
    }

    override fun bindDouble(index: Int, double: Double?) {
        println("Binding double")
        bindings[index].apply {
            buffer_type = MYSQL_TYPE_DOUBLE
            buffer = double?.let {
                memScope.alloc<DoubleVar>().apply { value = double }.ptr
            }
            buffer_length = sizeOf<DoubleVar>().toULong()
            is_null = memScope.alloc<ByteVar>().apply { value = (double == null).toByte() }.ptr
        }
    }

    override fun bindLong(index: Int, long: Long?) {
        println("Binding long")
        bindings[index].apply {
            buffer_type = MYSQL_TYPE_LONGLONG
            buffer = long?.let {
                memScope.alloc<LongVarOf<Long>>().apply { value = long }.ptr
            }
            buffer_length = sizeOf<LongVarOf<Long>>().toULong()
            is_null = memScope.alloc<ByteVar>().apply { value = (long == null).toByte() }.ptr
        }
    }

    override fun bindString(index: Int, string: String?) {
        println("Binding string")
        bindings[index].apply {
            buffer_type = MYSQL_TYPE_STRING
            buffer = string?.cstr?.getPointer(memScope)
            buffer_length = (string?.length ?: 0).toULong()
            is_null = memScope.alloc<ByteVar>().apply { value = (string == null).toByte() }.ptr
        }
    }

    public fun clear() {
        println("Clearing")
        memScope.clear()
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