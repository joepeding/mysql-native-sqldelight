package nl.joepeding.sqldelight.mysqldriver

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema

/**
 * The TransacterTest was copied from the SQLDelight repository on 28/09/2023
 * The docs say 'driver-test' module should be available
 * https://github.com/cashapp/sqldelight/blob/6591d9f03c0abfd65efb208cfd4ef2d805a251f1/CONTRIBUTING.md?plain=1#L42-L43
 * But this seems not to be the case.
 *
 * The intention is to remove this file if this test dependency is published.
 *
 * TODO: The original DriverTest from the SQLDelight repo uses a `changes`-function that implements
 *       SQLite-specific functionality. It seems like this should no longer be necessary, because
 *       the `execute` function on the `SqlDriver` should return the number of affected rows already.
 */
class MySQLNativeTransacterTest : TransacterTest() {
    override fun setupDatabase(schema: SqlSchema<QueryResult.Value<Unit>>): SqlDriver = MySQLNativeDriver(
        "localhost",
        "onsdb",
        "root",
        "",
        3306
    ).also { schema.create(it) }
}
