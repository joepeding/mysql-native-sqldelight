package nl.joepeding.sqldelight.mysqldriver
import app.cash.sqldelight.Transacter
import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import app.cash.sqldelight.db.SqlSchema
import co.touchlab.stately.concurrency.AtomicReference
import co.touchlab.stately.concurrency.value
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * This file was copied from the SQLDelight repository on 28/09/2023
 * The docs say DriverTest should be available
 * https://github.com/cashapp/sqldelight/blob/6591d9f03c0abfd65efb208cfd4ef2d805a251f1/CONTRIBUTING.md?plain=1#L42-L43
 * But this seems not to be the case.
 *
 * The intention is to remove this file if this test dependency is published.
 *
 * TODO: The original DriverTest from the SQLDelight repo uses a `changes`-function that implements
 *       SQLite-specific functionality. It seems like this should no longer be necessary, because
 *       the `execute` function on the `SqlDriver` should return the number of affected rows already.
 */
abstract class DriverTest {
    protected lateinit var driver: SqlDriver
    protected val schema = object : SqlSchema<QueryResult.Value<Unit>> {
        override val version: Long = 1

        override fun create(driver: SqlDriver): QueryResult.Value<Unit> {
            driver.execute(null, "DROP TABLE IF EXISTS test;", 0)
            driver.execute(
                0,
                """
              |CREATE TABLE test (
              |  id INTEGER PRIMARY KEY,
              |  value TEXT
              |);
        """.trimMargin(),
                0,
            )
            driver.execute(null, "DROP TABLE IF EXISTS nullability_test;", 0)
            driver.execute(
                1,
                """
              |CREATE TABLE nullability_test (
              |  id INTEGER PRIMARY KEY,
              |  integer_value INTEGER,
              |  text_value TEXT,
              |  blob_value BLOB,
              |  real_value REAL
              |);
        """.trimMargin(),
                0,
            )
            return QueryResult.Unit
        }

        override fun migrate(
            driver: SqlDriver,
            oldVersion: Long,
            newVersion: Long,
            vararg callbacks: AfterVersion,
        ) = QueryResult.Unit
    }
    private var transacter = AtomicReference<Transacter?>(null)

    abstract fun setupDatabase(schema: SqlSchema<QueryResult.Value<Unit>>): SqlDriver

    private fun changes(): Long? {
        // wrap in a transaction to ensure read happens on transaction thread/connection
        return transacter.value!!.transactionWithResult {
            val mapper: (SqlCursor) -> QueryResult<Long?> = {
                it.next()
                QueryResult.Value(it.getLong(0))
            }
            driver.executeQuery(null, "SELECT changes()", mapper, 0).value
        }
    }

    @BeforeTest fun setup() {
        driver = setupDatabase(schema = schema)
        transacter.value = object : TransacterImpl(driver) {}
    }

    @AfterTest fun tearDown() {
        transacter.value = null
        driver.close()
    }

    @Test
    fun insertCanRunMultipleTimes() {
        val insert = { binders: SqlPreparedStatement.() -> Unit ->
            driver.execute(2, "INSERT INTO test VALUES (?, ?);", 2, binders)
        }
        fun query(mapper: (SqlCursor) -> QueryResult<Unit>) {
            driver.executeQuery(3, "SELECT * FROM test", mapper, 0)
        }

        println("------ QUERY #1 ------")
        query {
            assertFalse(it.next().value)
            QueryResult.Unit
        }

        println("------ INSERT #1 ------")
        insert {
            bindLong(0, 1)
            bindString(1, "Alec")
        }.let { assertEquals(1L, it.value) }

        println("------ QUERY #2 ------")
        query {
            assertTrue(it.next().value)
            assertFalse(it.next().value)
            QueryResult.Unit
        }

        println("------ QUERY #3 ------")
        query {
            assertTrue(it.next().value)
            assertEquals(1, it.getLong(0))
            assertEquals("Alec", it.getString(1))
            QueryResult.Unit
        }

        println("------ INSERT #2 ------")
        insert {
            bindLong(0, 2)
            bindString(1, "Jake")
        }.let { assertEquals(1L, it.value) }

        println("------ QUERY #4 ------")
        query {
            assertTrue(it.next().value)
            assertEquals(1, it.getLong(0))
            assertEquals("Alec", it.getString(1))
            assertTrue(it.next().value)
            assertEquals(2, it.getLong(0))
            assertEquals("Jake", it.getString(1))
            QueryResult.Unit
        }

        driver
            .execute(5, "DELETE FROM test", 0)
            .let { assertEquals(2L, it.value) }

        query {
            assertFalse(it.next().value)
            QueryResult.Unit
        }
    }

    @Test
    fun queryCanRunMultipleTimes() {
        val insert = { binders: SqlPreparedStatement.() -> Unit ->
            driver.execute(2, "INSERT INTO test VALUES (?, ?);", 2, binders)
        }

        insert {
            bindLong(0, 1)
            bindString(1, "Alec")
        }.let { assertEquals(1L, it.value) }

        insert {
            bindLong(0, 2)
            bindString(1, "Jake")
        }.let { assertEquals(1L, it.value) }

        fun query(binders: SqlPreparedStatement.() -> Unit, mapper: (SqlCursor) -> QueryResult<Unit>) {
            driver.executeQuery(6, "SELECT * FROM test WHERE value = ?", mapper, 1, binders)
        }

        query(
            binders = {
                bindString(0, "Jake")
            },
            mapper = {
                assertTrue(it.next().value)
                assertEquals(2, it.getLong(0))
                assertEquals("Jake", it.getString(1))
                QueryResult.Unit
            },
        )

        // Second time running the query is fine
        query(
            binders = {
                bindString(0, "Jake")
            },
            mapper = {
                assertTrue(it.next().value)
                assertEquals(2, it.getLong(0))
                assertEquals("Jake", it.getString(1))
                QueryResult.Unit
            },
        )
    }

    @Test fun sqlResultSetGettersReturnNullIfTheColumnValuesAreNULL() {
        val insert = { binders: SqlPreparedStatement.() -> Unit ->
            driver.execute(7, "INSERT INTO nullability_test VALUES (?, ?, ?, ?, ?);", 5, binders)
        }
        insert {
            bindLong(0, 1)
            bindLong(1, null)
            bindString(2, null)
            bindBytes(3, null)
            bindDouble(4, null)
        }.let { assertEquals(1L, it.value) }

        val mapper: (SqlCursor) -> QueryResult<Unit> = {
            assertTrue(it.next().value)
            assertEquals(1, it.getLong(0))
            assertNull(it.getLong(1))
            assertNull(it.getString(2))
            assertNull(it.getBytes(3))
            assertNull(it.getDouble(4))
            QueryResult.Unit
        }
        driver.executeQuery(8, "SELECT * FROM nullability_test", mapper, 0)
    }
}