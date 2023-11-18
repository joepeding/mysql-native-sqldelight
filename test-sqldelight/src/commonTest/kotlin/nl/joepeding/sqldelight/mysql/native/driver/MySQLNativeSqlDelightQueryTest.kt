package nl.joepeding.sqldelight.mysql.native.driver

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema

/**
 * The QueryTest file was copied from the SQLDelight repository on 28/09/2023
 * The docs say 'driver-test' module should be available
 * https://github.com/cashapp/sqldelight/blob/6591d9f03c0abfd65efb208cfd4ef2d805a251f1/CONTRIBUTING.md?plain=1#L42-L43
 * But this seems not to be the case.
 *
 * The intention is to remove this file if this test dependency is published.
 */
class MySQLNativeSqlDelightQueryTest : QueryTest() {
    override fun setupDatabase(schema: SqlSchema<QueryResult.Value<Unit>>): SqlDriver = MySQLNativeDriver(
        "127.0.0.1",
        "onsdb",
        "root",
        "",
        3306
    ).also { schema.create(it) }
}