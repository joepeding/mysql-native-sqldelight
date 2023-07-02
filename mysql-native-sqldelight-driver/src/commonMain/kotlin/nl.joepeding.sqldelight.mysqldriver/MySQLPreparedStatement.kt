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
        val cRepresentation = memScope.alloc<BooleanVar>()
        cRepresentation.value = boolean ?: false
        bindings[index].buffer_type = MYSQL_TYPE_SHORT
        bindings[index].buffer = cRepresentation.ptr
        bindings[index].buffer_length = sizeOf<BooleanVar>().toULong()
    }

    override fun bindBytes(index: Int, bytes: ByteArray?) {
        val cRepresenation = (bytes ?: ByteArray(0)).toCValues()
        bindings[index].buffer_type = MYSQL_TYPE_BLOB
        bindings[index].buffer = cRepresenation.getPointer(memScope)
        bindings[index].buffer_length = cRepresenation.size.toULong()
    }

    override fun bindDouble(index: Int, double: Double?) {
        val cRepresentation = memScope.alloc<DoubleVar>()
        cRepresentation.value = double ?: 0.0
        bindings[index].buffer_type = MYSQL_TYPE_DOUBLE
        bindings[index].buffer = cRepresentation.ptr
        bindings[index].buffer_length = sizeOf<DoubleVar>().toULong()
    }

    override fun bindLong(index: Int, long: Long?) {
        val cRepresentation = memScope.alloc<LongVarOf<Long>>()
        cRepresentation.value = long ?: 0L
        bindings[index].buffer_type = MYSQL_TYPE_LONGLONG
        bindings[index].buffer = cRepresentation.ptr
        bindings[index].buffer_length = sizeOf<LongVarOf<Long>>().toULong()
    }

    override fun bindString(index: Int, string: String?) {
        bindings[index].buffer_type = MYSQL_TYPE_STRING
        bindings[index].buffer = (string ?: "").cstr.getPointer(memScope)
        bindings[index].buffer_length = (string ?: "").length.toULong()
    }

    public fun clear() {
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