package nl.joepeding.sqldelight.mysqldriver

import app.cash.sqldelight.db.QueryResult.Value
import co.touchlab.kermit.CommonWriter
import co.touchlab.kermit.DefaultFormatter
import co.touchlab.kermit.Logger
import co.touchlab.kermit.loggerConfigInit
import kotlinx.cinterop.*
import mysql.*
import kotlin.random.Random
import kotlin.test.*

class MinimalTest {
    private final val TESTNAME_FIELD = "testname"
    private final val VARCHAR_FIELD = "vcfield"
    private final val BOOLEAN_FIELD = "boolfield"
    private final val BYTES_FIELD = "bytesfield"
    private final val DOUBLE_FIELD = "doublefield"
    private final val LONG_FIELD = "longfield"
    private lateinit var driver: MySQLNativeDriver
    private val log = Logger(
        loggerConfigInit(CommonWriter(DefaultFormatter)),
        this::class.qualifiedName ?: this::class.toString()
    )
    @BeforeTest // @BeforeClass not supported
    fun setup() {
        driver = MySQLNativeDriver(
            "127.0.0.1",
            "onsdb",
            "root",
            "",
            3306
        )
        driver.execute(null, "CREATE TABLE IF NOT EXISTS `blaat`(" +
                "`$TESTNAME_FIELD` VARCHAR(255) NOT NULL," +
                "`$BOOLEAN_FIELD` BOOLEAN DEFAULT NULL," +
                "`$BYTES_FIELD` BLOB DEFAULT NULL," +
                "`$DOUBLE_FIELD` DOUBLE DEFAULT NULL," +
                "`$LONG_FIELD` BIGINT(40) DEFAULT NULL," +
                "`$VARCHAR_FIELD` VARCHAR(255) DEFAULT NULL" +
                ");", 0)
    }

    @Test
    fun testSuccessfulPlainQuery() {
        val result = driver.execute(
            null,
            "INSERT into blaat($TESTNAME_FIELD, $BYTES_FIELD) " +
                    "VALUES('testSuccessfulPlainQuery', '${"binary"}');",
            0)
        assertEquals(1L, result.value)
    }

    @Test
    fun testSuccessfulPreparedStatementWithAllTypes() {
        val result = driver.execute(
            null,
            "INSERT into blaat(" +
                    "$TESTNAME_FIELD, " +
                    "$BOOLEAN_FIELD, " +
                    "$BYTES_FIELD, " +
                    "$DOUBLE_FIELD, " +
                    "$LONG_FIELD, " +
                    "$VARCHAR_FIELD" +
                    ") VALUES(?, ?, ?, ?, ?, ?);",
            6
        ) {
            bindString(0, "testSuccessfulPreparedStatementWithAllTypes")
            bindBoolean(1, true)
            bindBytes(2, "binary".encodeToByteArray())
            bindDouble(3, 3.14)
            bindLong(4, (Int.MAX_VALUE.toLong() * 5))
            bindString(5, "someString")
        }
        assertEquals(1L, result.value)
    }

    @Test
    fun testTwoInsertsThenCheckValues() {
        val stringVal = "testInsertThenSelect-" + randomString()
        val byteArrayVal1 = "binarybinary".encodeToByteArray()
        val byteArrayVal2 = "twinarytwinary".encodeToByteArray()
        // Insert
        var insert = driver.execute(
            null,
            "INSERT into blaat(" +
                    "$TESTNAME_FIELD, " +
                    "$BOOLEAN_FIELD, " +
                    "$BYTES_FIELD, " +
                    "$DOUBLE_FIELD, " +
                    "$LONG_FIELD, " +
                    "$VARCHAR_FIELD" +
                    ") VALUES(?, ?, ?, ?, ?, ?);",
            6
        ) {
            bindString(0, stringVal)
            bindBoolean(1, true)
            bindBytes(2, byteArrayVal1)
            bindDouble(3, 3.14)
            bindLong(4, (Int.MAX_VALUE.toLong() * 5))
            bindString(5, "firstString")
        }
        assertEquals(1L, insert.value, "First insert failed")
        log.i { "Insert1" }

        insert = driver.execute(
            null,
            "INSERT into blaat(" +
                    "$TESTNAME_FIELD, " +
                    "$BOOLEAN_FIELD, " +
                    "$BYTES_FIELD, " +
                    "$DOUBLE_FIELD, " +
                    "$LONG_FIELD, " +
                    "$VARCHAR_FIELD" +
                    ") VALUES(?, ?, ?, ?, ?, ?);",
            6
        ) {
            bindString(0, stringVal)
            bindBoolean(1, false)
            bindBytes(2, byteArrayVal2)
            bindDouble(3, 14.3)
            bindLong(4, (Int.MAX_VALUE.toLong() * 6))
            bindString(5, "secondString")
        }
        assertEquals(1L, insert.value, "Second insert failed")
        log.i {"Insert2" }

        insert = driver.execute(
            null,
            "INSERT into blaat(" +
                    "$TESTNAME_FIELD, " +
                    "$BOOLEAN_FIELD, " +
                    "$BYTES_FIELD, " +
                    "$DOUBLE_FIELD, " +
                    "$LONG_FIELD, " +
                    "$VARCHAR_FIELD" +
                    ") VALUES(?, ?, ?, ?, ?, ?);",
            6
        ) {
            bindString(0, stringVal)
            bindBoolean(1, null)
            bindBytes(2, null)
            bindDouble(3, null)
            bindLong(4, null)
            bindString(5, null)
        }
        assertEquals(1L, insert.value, "Third insert failed")
        log.i { "Insert3" }

        // Fetch
        val result = driver.executeQuery(
            identifier = null,
            sql = "SELECT $TESTNAME_FIELD, $BOOLEAN_FIELD, $BYTES_FIELD, $DOUBLE_FIELD, $LONG_FIELD, $VARCHAR_FIELD FROM blaat WHERE $TESTNAME_FIELD = '$stringVal';",
            parameters = 0,
            binders = null,
            mapper = {
                Value(buildList {
                    while (it.next().value) {
                        add(
                            MinimalRow(
                                it.getString(0),
                                it.getBoolean(1),
                                it.getBytes(2),
                                it.getDouble(3),
                                it.getLong(4),
                                it.getString(5)
                            )
                        )
                    }
                })
            }
        )
        try {
            assertEquals(3, result.value.size)
            assertEquals(3, result.value.count { it.testName == stringVal }, "Test name doesn't match")
            assertEquals(1, result.value.count { it.bool == true }, "No bool true found")
            assertEquals(1, result.value.count { it.bool == false }, "No bool false found" )
            assertEquals(1, result.value.count { it.bool == null }, "No bool null found" )
            assertEquals(1, result.value.count { it.long == (Int.MAX_VALUE.toLong() * 5) }, "No 5 * Int.MAX_VALUE found" )
            assertEquals(1, result.value.count { it.long == (Int.MAX_VALUE.toLong() * 6) }, "No 6 * Int.MAX_VALUE found" )
            assertEquals(1, result.value.count { it.long == null }, "No null Long found" )
            assertEquals(1, result.value.count { it.double == 3.14 }, "No 3.14 found" )
            assertEquals(1, result.value.count { it.double == 14.3 }, "No 14.3 found" )
            assertEquals(1, result.value.count { it.double == null }, "No null double found" )
            assertEquals(1, result.value.count { it.bytes.contentEquals(byteArrayVal1) }, "Bytes 1 don't match")
            assertEquals(1, result.value.count { it.bytes.contentEquals(byteArrayVal2) }, "Bytes 2 don't match")
            assertEquals(1, result.value.count { it.bytes == null }, "Bytes 3 is not null")
            assertEquals(1, result.value.count { it.varchar == "firstString" }, "First string doesn't match")
            assertEquals(1, result.value.count { it.varchar == "secondString" }, "Second string doesn't match")
            assertEquals(1, result.value.count { it.varchar == null }, "First string doesn't match")
        } catch (e: Throwable) {
            log.e(e) { "Exception checking values" }
            throw e
        }
    }

    @Test
    fun testRollback() {
        val stringVal = "transaction-" + randomString()

        // Create table
        driver.execute(
            null, "CREATE TABLE IF NOT EXISTS `transactiontest`(" +
                    "`$TESTNAME_FIELD` VARCHAR(255) NOT NULL," +
                    "`data` INT(11) DEFAULT NULL" +
                    ");", 0
        )

        // Add a row
        driver.execute(
            null,
            "INSERT INTO transactiontest($TESTNAME_FIELD, data) VALUES(?, ?)",
            2
        ) {
            bindString(0, stringVal)
            bindLong(1, null)
        }

        // Start transaction
        val transaction = driver.newTransaction().value

        // Update row
        driver.execute(null, "UPDATE transactiontest SET data = ? WHERE $TESTNAME_FIELD = ?", 2) {
            bindLong(0, 1L)
            bindString(1, stringVal)
        }

        // Read row within transaction
        val readWithinTransaction = driver.executeQuery(
            identifier = null,
            sql =  "SELECT data from transactiontest where $TESTNAME_FIELD = ?",
            parameters = 1,
            binders = { bindString(0, stringVal) },
            mapper = {
                Value(
                    buildList {
                        while (it.next().value) {
                            add(it.getLong(0))
                        }
                    }
                )
            }
        )
        assertEquals(1L, readWithinTransaction.value.first())

        // Rollback transaction
        driver.rollback()

        // Read row after rollback
        val readAfterTransaction = driver.executeQuery(
            identifier = null,
            sql =  "SELECT data from transactiontest where $TESTNAME_FIELD = ?",
            parameters = 1,
            binders = { bindString(0, stringVal) },
            mapper = {
                Value(
                    buildList {
                        while (it.next().value) {
                            add(it.getLong(0))
                        }
                    }
                )
            }
        )
        assertEquals(null, readAfterTransaction.value.first())
    }

    @Test
    fun testCommit() {
        val stringVal = "transaction-" + randomString()

        // Create table
        driver.execute(
            null, "CREATE TABLE IF NOT EXISTS `transactiontest`(" +
                    "`$TESTNAME_FIELD` VARCHAR(255) NOT NULL," +
                    "`data` INT(11) DEFAULT NULL" +
                    ");", 0
        )

        // Add a row
        driver.execute(
            null,
            "INSERT INTO transactiontest($TESTNAME_FIELD, data) VALUES(?, ?)",
            2
        ) {
            bindString(0, stringVal)
            bindLong(1, null)
        }

        // Start transaction
        val transaction = driver.newTransaction().value

        // Update row
        driver.execute(null, "UPDATE transactiontest SET data = ? WHERE $TESTNAME_FIELD = ?", 2) {
            bindLong(0, 1L)
            bindString(1, stringVal)
        }

        // Read row within transaction
        val readWithinTransaction = driver.executeQuery(
            identifier = null,
            sql =  "SELECT data from transactiontest where $TESTNAME_FIELD = ?",
            parameters = 1,
            binders = { bindString(0, stringVal) },
            mapper = {
                Value(
                    buildList {
                        while (it.next().value) {
                            add(it.getLong(0))
                        }
                    }
                )
            }
        )
        assertEquals(1L, readWithinTransaction.value.first())

        // Commit transaction
        (transaction as MySQLNativeDriver.Transaction).endTransaction(true)

        // Read row after commit
        val readAfterTransaction = driver.executeQuery(
            identifier = null,
            sql =  "SELECT data from transactiontest where $TESTNAME_FIELD = ?",
            parameters = 1,
            binders = { bindString(0, stringVal) },
            mapper = {
                Value(
                    buildList {
                        while (it.next().value) {
                            add(it.getLong(0))
                        }
                    }
                )
            }
        )
        assertEquals(1L, readAfterTransaction.value.first())
    }

    @Test
    fun testConnectionProblem() {
        val e = assertFailsWith<IllegalArgumentException>("Connection to 127.1.2.3 should not succeed.") {
            MySQLNativeDriver(
                "127.1.2.3",
                "onsdb",
                "root",
                "",
                3306
            )
        }
        requireNotNull(e.message)
        assertContains(e.message!!, "Can't connect to MySQL server on '127.1.2.3:3306'")
    }

    @Test
    fun testErrorQuery() {
        memScoped {
            mysql_library_init!!.invoke(0, null, null)
            val mysequel = mysql_init(null)
            val mysqlconnected = mysql_real_connect(
                mysequel,
                "127.0.0.1",
                "root",
                "",
                "onsdb",
                3306u,
                null,
                1u
            )
            val result = mysql_query(mysqlconnected, "INSERT into blaat(fakefield) VALUES('cinterop');")
            assertEquals(1, result) // 0 return code indicates success
        }
    }

    data class MinimalRow(
        val testName: String?,
        val bool: Boolean?,
        val bytes: ByteArray?,
        val double: Double?,
        val long: Long?,
        val varchar: String?
    )

    companion object {
        fun randomString(lenght: Int = 6): String = (1..lenght)
            .map { Random.nextInt(0, charPool.size).let { charPool[it] } }
            .joinToString("")

        val charPool = ('a'..'z') + ('A'..'Z') + ('0'..'9')
    }
}