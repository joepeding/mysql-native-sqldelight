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
    private val statementCache = mutableMapOf<Int, CPointer<MYSQL_STMT>>()
    private var transaction: Transaction? = null

    override fun addListener(vararg queryKeys: String, listener: Query.Listener) {
        return
        TODO("Not yet implemented")
    }

    override fun currentTransaction(): Transacter.Transaction? = transaction

    override fun execute(
        identifier: Int?,
        sql: String,
        parameters: Int,
        binders: (SqlPreparedStatement.() -> Unit)?
    ): QueryResult<Long> {
        // Load prepared statement from cache, or prepare it
        val statement = identifier?.let {
            statementCache.getOrPut(it) {
                MySQLPreparedStatement.prepareStatement(conn, sql)
            }
        } ?: MySQLPreparedStatement.prepareStatement(conn, sql)

        // Bind parameters, if any
        val bindings = binders?.let { MySQLPreparedStatement(statement, parameters).apply(it) }

        // Execute
        bindings?.let { mysql_stmt_bind_param(statement, it.bindings) }
        mysql_stmt_execute(statement)

        // Clear memory for bindings, if any
        bindings?.clear()

        // Check error
        require(!statement.hasError()) { statement.error().also { println(it) } }

        // Return dummy query result
        return QueryResult.Value(mysql_stmt_affected_rows(statement).toLong())
    }

    override fun <R> executeQuery(
        identifier: Int?,
        sql: String,
        mapper: (SqlCursor) -> QueryResult<R>,
        parameters: Int,
        binders: (SqlPreparedStatement.() -> Unit)?
    ): QueryResult<R> {
        // Load prepared statement from cache, or prepare it
        val statement = identifier?.let {
            statementCache.getOrPut(it) {
                MySQLPreparedStatement.prepareStatement(conn, sql)
            }
        } ?: MySQLPreparedStatement.prepareStatement(conn, sql)

        // Bind parameters, if any
        val bindings = binders?.let { MySQLPreparedStatement(statement, parameters).apply(it) }

        // Execute
        bindings?.let { mysql_stmt_bind_param(statement, it.bindings) }
        mysql_stmt_execute(statement)

        // Clear memory for bindings, if any
        bindings?.clear()

        // Check error
        require(!statement.hasError()) { statement.error().also { println(it) } }

        return mapper(MySQLCursor(statement))
    }


    override fun newTransaction(): QueryResult.Value<Transacter.Transaction> {
        if (transaction != null) {
            // TODO: 1. Replace with warning level logger
            // TODO: 2. Consider the user of SAVEPOINT statements to mimic nested transactions?
            println("Warning: MySQL does not support nested transaction. Outer transaction is automatically committed.")
        }
        mysql_query(conn, "START TRANSACTION") // TODO: Check success
        return QueryResult.Value(Transaction(transaction))
    }

    private inner class Transaction(
        override val enclosingTransaction: Transaction?
    ) : Transacter.Transaction() {
        override fun endTransaction(successful: Boolean) = if(successful) {
            commit()
        } else {
            rollback()
        }

        fun getEnclosingTransaction(): Transaction? = enclosingTransaction
    }

    fun commit(): QueryResult.Value<Unit> {
        if(mysql_commit(conn)) { error(conn.error()) }
        transaction = transaction?.getEnclosingTransaction()
        return QueryResult.Unit
    }

    fun rollback(): QueryResult.Value<Unit> {
        if(mysql_rollback(conn)) { error(conn.error()) }
        transaction = transaction?.getEnclosingTransaction()
        return QueryResult.Unit
    }

    override fun notifyListeners(vararg queryKeys: String) {
        return
        TODO("Not yet implemented")
    }

    override fun removeListener(vararg queryKeys: String, listener: Query.Listener) {
        return
        TODO("Not yet implemented")
    }

    override fun close() {
        return
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
    mysql_options(conn, mysql_option.MYSQL_OPT_CONNECT_TIMEOUT, cValuesOf(2))
    mysql_real_connect(
        conn,
        host = host,
        user = user,
        passwd = password,
        db = database,
        port = port.toUInt(),
        unix_socket = null,
        clientflag = 1u,
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

internal fun CPointer<MYSQL_STMT>?.hasError(): Boolean =
    mysql_stmt_errno(this) != 0.toUInt()

internal fun CPointer<MYSQL_STMT>?.error(): String =
    mysql_stmt_error(this)!!.toKString()