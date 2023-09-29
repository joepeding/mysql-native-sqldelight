package nl.joepeding.sqldelight.mysqldriver

import app.cash.sqldelight.Query
import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import co.touchlab.stately.concurrency.AtomicInt
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * This file was copied from the SQLDelight repository on 28/09/2023
 * The docs say 'driver-test' module should be available
 * https://github.com/cashapp/sqldelight/blob/6591d9f03c0abfd65efb208cfd4ef2d805a251f1/CONTRIBUTING.md?plain=1#L42-L43
 * But this seems not to be the case.
 *
 * The intention is to remove this file if this test dependency is published.
 *
 * TODO: The original QueryTest from the SQLDelight repo does not the 'drop table' statement in schema creation
 */
abstract class QueryTest {
    private val mapper = { cursor: SqlCursor ->
        TestData(
            cursor.getLong(0)!!,
            cursor.getString(1)!!,
        )
    }

    private lateinit var driver: SqlDriver

    abstract fun setupDatabase(schema: SqlSchema<QueryResult.Value<Unit>>): SqlDriver

    @BeforeTest fun setup() {
        driver = setupDatabase(
            schema = object : SqlSchema<QueryResult.Value<Unit>> {
                override val version: Long = 1

                override fun create(driver: SqlDriver): QueryResult.Value<Unit> {
                    driver.execute(null, "DROP TABLE IF EXISTS querytest", 0)
                    driver.execute(
                        null,
                        """
              CREATE TABLE querytest (
                id INTEGER NOT NULL PRIMARY KEY,
                value TEXT NOT NULL
               );
            """.trimIndent(),
                        0,
                    )
                    return QueryResult.Unit
                }

                override fun migrate(
                    driver: SqlDriver,
                    oldVersion: Long,
                    newVersion: Long,
                    vararg callbacks: AfterVersion,
                ): QueryResult.Value<Unit> {
                    // No-op.
                    return QueryResult.Unit
                }
            },
        )
    }

    @AfterTest fun tearDown() {
        driver.close()
    }

    @Test fun executeAsOne() {
        val data1 = TestData(1, "val1")
        insertTestData(data1)

        assertEquals(data1, testDataQuery().executeAsOne())
    }

    @Test fun executeAsOneTwoTimes() {
        val data1 = TestData(1, "val1")
        insertTestData(data1)

        val query = testDataQuery()

        assertEquals(query.executeAsOne(), query.executeAsOne())
    }

    @Test fun executeAsOneThrowsNpeForNoRows() {
        try {
            testDataQuery().executeAsOne()
            throw AssertionError("Expected an IllegalStateException")
        } catch (ignored: NullPointerException) {
        }
    }

    @Test fun executeAsOneThrowsIllegalStateExceptionForManyRows() {
        try {
            insertTestData(TestData(1, "val1"))
            insertTestData(TestData(2, "val2"))

            testDataQuery().executeAsOne()
            throw AssertionError("Expected an IllegalStateException")
        } catch (ignored: IllegalStateException) {
        }
    }

    @Test fun executeAsOneOrNull() {
        val data1 = TestData(1, "val1")
        insertTestData(data1)

        val query = testDataQuery()
        assertEquals(data1, query.executeAsOneOrNull())
    }

    @Test fun executeAsOneOrNullReturnsNullForNoRows() {
        assertNull(testDataQuery().executeAsOneOrNull())
    }

    @Test fun executeAsOneOrNullThrowsIllegalStateExceptionForManyRows() {
        try {
            insertTestData(TestData(1, "val1"))
            insertTestData(TestData(2, "val2"))

            testDataQuery().executeAsOneOrNull()
            throw AssertionError("Expected an IllegalStateException")
        } catch (ignored: IllegalStateException) {
        }
    }

    @Test fun executeAsList() {
        val data1 = TestData(1, "val1")
        val data2 = TestData(2, "val2")

        insertTestData(data1)
        insertTestData(data2)

        assertEquals(listOf(data1, data2), testDataQuery().executeAsList())
    }

    @Test fun executeAsListForNoRows() {
        assertTrue(testDataQuery().executeAsList().isEmpty())
    }

    @Test fun notifyDataChangedNotifiesListeners() {
        val notifies = AtomicInt(0)
        val query = testDataQuery()
        val listener = Query.Listener { notifies.incrementAndGet() }

        query.addListener(listener)
        assertEquals(0, notifies.get())

        driver.notifyListeners("querytest")
        assertEquals(1, notifies.get())
    }

    @Test fun removeListenerActuallyRemovesListener() {
        val notifies = AtomicInt(0)
        val query = testDataQuery()
        val listener = Query.Listener { notifies.incrementAndGet() }

        query.addListener(listener)
        query.removeListener(listener)
        driver.notifyListeners("querytest")
        assertEquals(0, notifies.get())
    }

    private fun insertTestData(testData: TestData) {
        driver.execute(1, "INSERT INTO querytest VALUES (?, ?)", 2) {
            bindLong(0, testData.id)
            bindString(1, testData.value)
        }
    }

    private fun testDataQuery(): Query<TestData> {
        return object : Query<TestData>(mapper) {
            override fun <R> execute(mapper: (SqlCursor) -> QueryResult<R>): QueryResult<R> {
                return driver.executeQuery(0, "SELECT * FROM querytest", mapper, 0, null)
            }

            override fun addListener(listener: Listener) {
                driver.addListener("querytest", listener = listener)
            }

            override fun removeListener(listener: Listener) {
                driver.removeListener("querytest", listener = listener)
            }
        }
    }

    private data class TestData(val id: Long, val value: String)
}
