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
        val boolString = (if (boolean == true) { 1 } else { -1 }).toString()
        bindings[index].buffer_type = MYSQL_TYPE_STRING
        bindings[index].buffer = boolString.cstr.getPointer(memScope)
        bindings[index].buffer_length = boolString.length.toULong()
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