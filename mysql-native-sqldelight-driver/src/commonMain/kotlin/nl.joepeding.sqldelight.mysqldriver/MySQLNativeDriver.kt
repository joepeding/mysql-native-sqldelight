package nl.joepeding.sqldelight.mysqldriver

import app.cash.sqldelight.Query
import app.cash.sqldelight.Transacter
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import kotlinx.cinterop.*
import mysql.*

public class MySQLNativeDriver(
    val conn: CPointer<MYSQL>,
//    listenerSupport: ListenerSupport
) : SqlDriver {
    override fun addListener(listener: Query.Listener, queryKeys: Array<String>) {
        TODO("Not yet implemented")
    }

    override fun currentTransaction(): Transacter.Transaction? {
        TODO("Not yet implemented")
    }

    override fun execute(
        identifier: Int?,
        sql: String,
        parameters: Int,
        binders: (SqlPreparedStatement.() -> Unit)?
    ): QueryResult<Long> {
        memScoped {
            mysql_query(conn, sql)

            // Check error
            if (conn.hasError()) {
                println("Query error: ${conn.error()}")
                return QueryResult.Value(333L) // TODO: Throw exception
            }

            // TODO: Implement query result parsing

            // Return dummy query result
            return QueryResult.Value(0L)
        }
    }

    override fun <R> executeQuery(
        identifier: Int?,
        sql: String,
        mapper: (SqlCursor) -> R,
        parameters: Int,
        binders: (SqlPreparedStatement.() -> Unit)?
    ): QueryResult<R> {
        TODO("Not yet implemented")
    }

    override fun newTransaction(): QueryResult<Transacter.Transaction> {
        TODO("Not yet implemented")
    }

    override fun notifyListeners(queryKeys: Array<String>) {
        TODO("Not yet implemented")
    }

    override fun removeListener(listener: Query.Listener, queryKeys: Array<String>) {
        TODO("Not yet implemented")
    }

    override fun close() {
        TODO("Not yet implemented")
    }
}

public fun MySQLNativeDriver(
    host: String,
    database: String,
    user: String,
    password: String,
    port: Int = 3306,
//        options: String? = null,
//        listenerSupport: ListenerSupport = ListenerSupport.None
): MySQLNativeDriver {
    mysql_library_init!!.invoke(0, null, null)
    val conn = mysql_init(null)
    mysql_options(conn, mysql_option.MYSQL_OPT_CONNECT_TIMEOUT, cValuesOf(10))
    mysql_real_connect(
        conn,
        host = host,
        user = user,
        passwd = password,
        db = database,
        port = port.toUInt(),
        unix_socket = null,
        clientflag = 1,
    )

    require(!conn.hasError()) { conn.error() }
    return MySQLNativeDriver(
        conn!!,
//        listenerSupport = listenerSupport
    )
}

internal fun CPointer<MYSQL>?.hasError(): Boolean =
    mysql_errno(this) != 0.toUInt()

internal fun CPointer<MYSQL>?.error(): String =
    mysql_error(this)!!.toKString()