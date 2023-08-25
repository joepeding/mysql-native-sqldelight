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
        bindings[index].buffer_type = MYSQL_TYPE_SHORT
        bindings[index].buffer = boolean?.let {
            memScope.alloc<BooleanVarOf<Boolean>>().apply { value = boolean }.ptr
        }
        bindings[index].buffer_length = sizeOf<BooleanVar>().toULong()
        bindings[index].is_null = memScope.alloc<ByteVar>().apply { value = (boolean == null).toByte() }.ptr
    }

    override fun bindBytes(index: Int, bytes: ByteArray?) {
        println("Binding bytes")
        val cRepresentation = bytes?.toCValues()
        bindings[index].buffer_type = MYSQL_TYPE_BLOB
        bindings[index].buffer = cRepresentation?.getPointer(memScope)
        bindings[index].buffer_length = cRepresentation?.size?.toULong() ?: 0.toULong()
        bindings[index].is_null = memScope.alloc<ByteVar>().apply { value = (bytes == null).toByte() }.ptr
    }

    override fun bindDouble(index: Int, double: Double?) {
        println("Binding double")
        bindings[index].buffer_type = MYSQL_TYPE_DOUBLE
        bindings[index].buffer = double?.let {
            memScope.alloc<DoubleVar>().apply { value = double }.ptr
        }
        bindings[index].buffer_length = sizeOf<DoubleVar>().toULong()
        bindings[index].is_null = memScope.alloc<ByteVar>().apply { value = (double == null).toByte() }.ptr
    }

    override fun bindLong(index: Int, long: Long?) {
        println("Binding long")
        bindings[index].buffer_type = MYSQL_TYPE_LONGLONG
        bindings[index].buffer = long?.let {
            memScope.alloc<LongVarOf<Long>>().apply { value = long }.ptr
        }
        bindings[index].buffer_length = sizeOf<LongVarOf<Long>>().toULong()
        bindings[index].is_null = memScope.alloc<ByteVar>().apply { value = (long == null).toByte() }.ptr
    }

    override fun bindString(index: Int, string: String?) {
        println("Binding string")
        bindings[index].buffer_type = MYSQL_TYPE_STRING
        bindings[index].buffer = string?.cstr?.getPointer(memScope)
        bindings[index].buffer_length = (string?.length ?: 0).toULong()
        bindings[index].is_null = memScope.alloc<ByteVar>().apply { value = (string == null).toByte() }.ptr
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