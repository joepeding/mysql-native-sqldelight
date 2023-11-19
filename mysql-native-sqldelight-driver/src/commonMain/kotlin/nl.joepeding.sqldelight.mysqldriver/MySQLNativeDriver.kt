package nl.joepeding.sqldelight.mysqldriver

import app.cash.sqldelight.Query
import app.cash.sqldelight.Transacter
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import co.touchlab.kermit.CommonWriter
import co.touchlab.kermit.DefaultFormatter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.loggerConfigInit
import kotlinx.cinterop.*
import mysql.*

class MySQLNativeDriver(
    val conn: CPointer<MYSQL>,
) : SqlDriver {
    private val statementCache = mutableMapOf<Int, CPointer<MYSQL_STMT>>()
    private var transaction: Transaction? = null
    private val listeners = mutableMapOf<String, MutableSet<Query.Listener>>()
    private val log = Logger(
        loggerConfigInit(CommonWriter(DefaultFormatter)),
        this::class.qualifiedName ?: this::class.toString()
    )

    /**
     * Execute provided query and return number of affected rows
     *
     * Gets the prepared statement from cache, or prepares it and puts it in cache. Then creates a
     * [MySQLPreparedStatement] and applies the binders to it. Then executes the statements, clears the
     * allocated C-objects, checks for errors in execution and returns the number of affected rows.
     *
     * @param identifier Unique identifier for this statement so it can be put in or retrieved from cache
     * @param sql Query string with placeholders for parameter
     * @param parameters number of parameters
     * @param binders lambda to bind parameters
     */
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
        require(!statement.hasError()) { statement.error().also { log.e { it } } }

        // Return dummy query result
        return QueryResult.Value(mysql_stmt_affected_rows(statement).toLong())
    }

    /**
     * Execute provided query and return number of affected rows
     *
     * Gets the prepared statement from cache, or prepares it and puts it in cache. Then creates a
     * [MySQLPreparedStatement] and applies the binders to it. Then executes the statements, clears the
     * allocated C-objects, checks for errors in execution and finally maps the result using the
     * mappers and a [MySQLCursor].
     *
     * @param identifier Unique identifier for this statement so it can be put in or retrieved from cache
     * @param sql Query string with placeholders for parameter
     * @param mapper lambda that takes an `SQLCursor` and produces a [QueryResult] with mapped data
     * @param parameters number of parameters
     * @param binders lambda to bind parameters
     */
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
        require(!statement.hasError()) { statement.error().also { log.e { it } } }

        return mapper(MySQLCursor(statement))
    }


    /**
     * Returns the current [Transaction]
     */
    override fun currentTransaction(): Transacter.Transaction? = transaction

    /**
     * Starts a new transaction (WARNING: Nested transactions not supported)
     *
     * Starts a new transaction (simply by executing the `START TRANSACTION` statement). Nested transactions
     * are not supported by MySQL itself. Future versions may offer a workaround by using `SAVEPOINT` statements.
     */
    override fun newTransaction(): QueryResult.Value<Transacter.Transaction> {
        if (transaction != null) {
            // TODO: 1. Replace with warning level logger
            // TODO: 2. Consider the user of SAVEPOINT statements to mimic nested transactions?
            log.w { "MySQL does not support nested transaction. Outer transaction is automatically committed." }
        }
        mysql_query(conn, "START TRANSACTION") // TODO: Check success
        transaction = Transaction(transaction)
        return QueryResult.Value(transaction as Transacter.Transaction)
    }

    /**
     * MySQL-specific subclass of SQLDelight's Transaction
     */
    inner class Transaction(
        override val enclosingTransaction: Transaction?
    ) : Transacter.Transaction() {
        /**
         * Ends the current transaction with a commit if [successful] is `true` or a rollback if not.
         *
         * Sets the current transaction to the enclosing transaction if any exists, even though this currently has
         * no real meaning because MySQL does not support nesting transactions.
         */
        public override fun endTransaction(successful: Boolean) = when {
            enclosingTransaction == null && successful -> commit()
            enclosingTransaction == null && !successful -> rollback()
            else -> {
                transaction = enclosingTransaction
                QueryResult.Unit
            }
        }

        private fun commit(): QueryResult.Value<Unit> {
            log.d { "COMMIT" }
            if(mysql_commit(conn)) { error(conn.error()) }
            transaction = enclosingTransaction
            return QueryResult.Unit
        }

        private fun rollback(): QueryResult.Value<Unit> {
            log.d { "ROLLBACK" }
            if(mysql_rollback(conn)) { error(conn.error()) }
            transaction = enclosingTransaction
            return QueryResult.Unit
        }
    }

    /**
     * Convenience function to commit the current transaction
     */
    fun commit() {
        transaction?.endTransaction(true)
    }

    /**
     * Convenience function to rollbac the current transaction
     */
    fun rollback() {
        transaction?.endTransaction(false)
    }


    /**
     * SQLDelight function for adding a Listener to the Driver
     */
    override fun addListener(vararg queryKeys: String, listener: Query.Listener) {
        queryKeys.forEach {
            listeners.getOrPut(it) { mutableSetOf() }.add(listener)
        }
    }

    /**
     * SQLDelight function for removing a Listener from the Driver
     */
    override fun removeListener(vararg queryKeys: String, listener: Query.Listener) {
        queryKeys.forEach {
            listeners[it]?.remove(listener)
        }
    }

    /**
     * SQLDelight function for notifying Listener listening to the provided keys
     */
    override fun notifyListeners(vararg queryKeys: String) {
        val listenersToNotify = mutableSetOf<Query.Listener>()
        queryKeys.forEach { key -> listeners[key]?.let { listenersToNotify.addAll(it) } }
        listenersToNotify.forEach(Query.Listener::queryResultsChanged)
    }

    /**
     * Closes this [MySQLNativeDriver], clearing memory and terminating the connection. Not implemented yet.
     */
    override fun close() {
        return
        TODO("Not yet implemented")
    }
}

/**
 * Constructor for a [MySQLNativeDriver]
 *
 * Initiates the MySQL client library, sets options and initiates the connection.
 *
 * @return [MySQLNativeDriver] if the connectino succeeds.
 * @throws IllegalArgumentException if there's a connection error
 */
fun MySQLNativeDriver(
    host: String,
    database: String,
    user: String,
    password: String,
    port: Int = 3306,
//        options: String? = null,
): MySQLNativeDriver {
    mysql_library_init!!.invoke(0, null, null)
    val conn = mysql_init(null)
    requireNotNull(conn) { "Insufficient memory to allocate MYSQL handler." }

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

    return MySQLNativeDriver(conn)
}

/**
 * Convenience extension function to check if there's an error on a `CPointer<MYSQL>`
 */
internal fun CPointer<MYSQL>?.hasError(): Boolean =
    mysql_errno(this) != 0.toUInt()

/**
 * Convenience extension function to get a Kotlin String of the error on a `CPointer<MYSQL>`
 */
internal fun CPointer<MYSQL>?.error(): String =
    mysql_error(this)?.toKString() ?: ""

/**
 * Convenience extension function to check if there's an error on a `CPointer<MYSQL_STMT>`
 */
internal fun CPointer<MYSQL_STMT>?.hasError(): Boolean =
    mysql_stmt_errno(this) != 0.toUInt()

/**
 * Convenience extension function to get a Kotlin String of the error on a `CPointer<MYSQL_STMT>`
 */
internal fun CPointer<MYSQL_STMT>?.error(): String =
    mysql_stmt_error(this)?.toKString() ?: ""