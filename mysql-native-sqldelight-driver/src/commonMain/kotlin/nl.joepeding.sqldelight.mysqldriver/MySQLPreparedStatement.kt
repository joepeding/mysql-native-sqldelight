package nl.joepeding.sqldelight.mysqldriver

import app.cash.sqldelight.db.SqlPreparedStatement
import kotlinx.cinterop.*
import mysql.*

public class MySQLPreparedStatement(
    private val statement: CPointer<MYSQL_STMT>
): SqlPreparedStatement {
    private val memScope: Arena = Arena()

    fun bind(index: Int, value: String?, oid: UInt) {
        val bind = memScope.alloc<MYSQL_BIND>()
        bind.buffer_type = oid
        bind.param_number = index.toUInt()
        bind.buffer = (value ?: "").cstr.getPointer(memScope)
        bind.buffer_length = (value ?: "").length.toULong()
        mysql_stmt_bind_param(statement, bind.ptr)
    }

    override fun bindBoolean(index: Int, boolean: Boolean?) {
        TODO("Not yet implemented")
    }

    override fun bindBytes(index: Int, bytes: ByteArray?) {
        TODO("Not yet implemented")
    }

    override fun bindDouble(index: Int, double: Double?) {
        TODO("Not yet implemented")
    }

    override fun bindLong(index: Int, long: Long?) {
        TODO("Not yet implemented")
    }

    override fun bindString(index: Int, string: String?) {
        bind(index, string, MYSQL_TYPE_STRING)
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