package nl.joepeding.sqldelight.mysqldriver

import app.cash.sqldelight.TransacterImpl
import app.cash.sqldelight.db.AfterVersion
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import kotlin.test.*

/**
 * This file was copied from the SQLDelight repository on 28/09/2023
 * The docs say 'driver-test' module should be available
 * https://github.com/cashapp/sqldelight/blob/6591d9f03c0abfd65efb208cfd4ef2d805a251f1/CONTRIBUTING.md?plain=1#L42-L43
 * But this seems not to be the case.
 *
 * The intention is to remove this file if this test dependency is published.
 *
 * TODO: The original TransacterTest from the SQLDelight repo uses a `changes`-function that implements
 *       SQLite-specific functionality. It seems like this should no longer be necessary, because
 *       the `execute` function on the `SqlDriver` should return the number of affected rows already.
 */
abstract class TransacterTest {
    protected lateinit var transacter: TransacterImpl
    private lateinit var driver: SqlDriver

    abstract fun setupDatabase(schema: SqlSchema<QueryResult.Value<Unit>>): SqlDriver

    @BeforeTest
    fun setup() {
        val driver = setupDatabase(
            object : SqlSchema<QueryResult.Value<Unit>> {
                override val version = 1L
                override fun create(driver: SqlDriver): QueryResult.Value<Unit> = QueryResult.Unit
                override fun migrate(
                    driver: SqlDriver,
                    oldVersion: Long,
                    newVersion: Long,
                    vararg callbacks: AfterVersion,
                ): QueryResult.Value<Unit> = QueryResult.Unit
            },
        )
        transacter = object : TransacterImpl(driver) {}
        this.driver = driver
    }

    @AfterTest
    fun teardown() {
        driver.close()
    }

    @Test
    fun afterCommitRunsAfterTransactionCommits() {
        var counter = 0
        transacter.transaction {
            afterCommit { counter++ }
            assertEquals(0, counter)
        }

        assertEquals(1, counter)
    }

    @Test
    fun afterCommitDoesNotRunAfterTransactionRollbacks() {
        var counter = 0
        transacter.transaction {
            afterCommit { counter++ }
            assertEquals(0, counter)
            rollback()
        }

        assertEquals(0, counter)
    }

    @Test
    fun afterCommitRunsAfterEnclosingTransactionCommits() {
        var counter = 0
        transacter.transaction {
            afterCommit { counter++ }
            assertEquals(0, counter)

            transaction {
                afterCommit { counter++ }
                assertEquals(0, counter)
            }

            assertEquals(0, counter)
        }

        assertEquals(2, counter)
    }

    @Test
    fun afterCommitDoesNotRunInNestedTransactionWhenEnclosingRollsBack() {
        var counter = 0
        transacter.transaction {
            afterCommit { counter++ }
            assertEquals(0, counter)

            transaction {
                afterCommit { counter++ }
            }

            rollback()
        }

        assertEquals(0, counter)
    }

    @Test
    fun afterCommitDoesNotRunInNestedTransactionWhenNestedRollsBack() {
        var counter = 0
        transacter.transaction {
            afterCommit { counter++ }
            assertEquals(0, counter)

            transaction {
                afterCommit { counter++ }
                rollback()
            }

            throw AssertionError()
        }

        assertEquals(0, counter)
    }

    @Test
    fun afterRollbackNoOpsIfTheTransactionNeverRollsBack() {
        var counter = 0
        transacter.transaction {
            afterRollback { counter++ }
        }

        assertEquals(0, counter)
    }

    @Test
    fun afterRollbackRunsAfterARollbackOccurs() {
        var counter = 0
        transacter.transaction {
            afterRollback { counter++ }
            rollback()
        }

        assertEquals(1, counter)
    }

    @Test
    fun afterRollbackRunsAfterAnInnerTransactionRollsBack() {
        var counter = 0
        transacter.transaction {
            afterRollback { counter++ }
            transaction {
                rollback()
            }
            throw AssertionError()
        }

        assertEquals(1, counter)
    }

    @Test
    fun afterRollbackRunsInAnInnerTransactionWhenTheOuterTransactionRollsBack() {
        var counter = 0
        transacter.transaction {
            transaction {
                afterRollback { counter++ }
            }
            rollback()
        }

        assertEquals(1, counter)
    }

    @Test
    fun transactionsCloseThemselvesOutProperly() {
        var counter = 0
        transacter.transaction {
            afterCommit { counter++ }
        }

        transacter.transaction {
            afterCommit { counter++ }
        }

        assertEquals(2, counter)
    }

    @Test
    fun settingNoEnclosingFailsIfThereIsACurrentlyRunningTransaction() {
        transacter.transaction(noEnclosing = true) {
            assertFailsWith<IllegalStateException> {
                transacter.transaction(noEnclosing = true) {
                    throw AssertionError()
                }
            }
        }
    }

    @Test
    fun anExceptionThrownInPostRollbackFunctionIsCombinedWithTheExceptionInTheMainBody() {
        class ExceptionA : RuntimeException()
        class ExceptionB : RuntimeException()
        val t = assertFailsWith<Throwable>() {
            transacter.transaction {
                afterRollback {
                    throw ExceptionA()
                }
                throw ExceptionB()
            }
        }
        assertTrue("Exception thrown in body not in message($t)") { t.toString().contains("ExceptionA") }
        assertTrue("Exception thrown in rollback not in message($t)") { t.toString().contains("ExceptionB") }
    }

    @Test
    fun weCanReturnAValueFromATransaction() {
        val result: String = transacter.transactionWithResult {
            return@transactionWithResult "sup"
        }

        assertEquals(result, "sup")
    }

    @Test
    fun weCanRollbackWithValueFromATransaction() {
        val result: String = transacter.transactionWithResult {
            rollback("rollback")

            @Suppress("UNREACHABLE_CODE")
            return@transactionWithResult "sup"
        }

        assertEquals(result, "rollback")
    }
}