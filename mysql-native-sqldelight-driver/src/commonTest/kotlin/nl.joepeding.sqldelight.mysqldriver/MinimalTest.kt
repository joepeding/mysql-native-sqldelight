package nl.joepeding.sqldelight.mysqldriver

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
    @BeforeTest // @BeforeClass not supported
    fun setup() {
        driver = MySQLNativeDriver(
            "localhost",
            "onsdb",
            "root",
            "",
            3306
        )
//        driver.execute(null, "DROP TABLE blaat;", 0) // Not enabled by default, convenient to be able to view DB
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
        println("Insert1")

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
        println("Insert2")

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
        println("Insert3")

        // Fetch
        val result = driver.executeQuery(
            identifier = null,
            sql = "SELECT $TESTNAME_FIELD, $BOOLEAN_FIELD, $BYTES_FIELD, $DOUBLE_FIELD, $LONG_FIELD, $VARCHAR_FIELD FROM blaat WHERE $TESTNAME_FIELD = '$stringVal';",
            parameters = 0,
            binders = null,
            mapper = {
                buildList {
                    while (it.next()) {
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
                }
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
            println(e.message)
            throw e
        }
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
                "localhost",
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