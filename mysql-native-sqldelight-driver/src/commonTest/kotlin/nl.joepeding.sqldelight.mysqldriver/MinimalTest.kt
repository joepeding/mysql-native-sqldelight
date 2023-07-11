package nl.joepeding.sqldelight.mysqldriver

import kotlinx.cinterop.*
import mysql.*
import kotlin.random.Random
import kotlin.test.*

class MinimalTest {
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
                "`$VARCHAR_FIELD` VARCHAR(255) DEFAULT NULL," +
                "`$BOOLEAN_FIELD` BOOLEAN NOT NULL DEFAULT 0," +
                "`$BYTES_FIELD` BLOB NOT NULL," +
                "`$DOUBLE_FIELD` DOUBLE NOT NULL DEFAULT 0.0," +
                "`$LONG_FIELD` BIGINT(40) NOT NULL DEFAULT 0" +
                ");", 0)
    }

    @Test
    fun testSuccessfulPlainQuery() {
        val result = driver.execute(
            null,
            "INSERT into blaat($VARCHAR_FIELD, $BYTES_FIELD) " +
                    "VALUES('testSuccessfulPlainQuery', '${"binary"}');",
            0)
        assertEquals(1L, result.value)
    }

    @Test
    fun testSuccessfulPreparedStatementWithAllTypes() {
        val result = driver.execute(
            null,
            "INSERT into blaat(" +
                    "$VARCHAR_FIELD, " +
                    "$BOOLEAN_FIELD, " +
                    "$BYTES_FIELD, " +
                    "$DOUBLE_FIELD, " +
                    "$LONG_FIELD" +
                    ") VALUES(?, ?, ?, ?, ?);",
            2
        ) {
            bindString(0, "testSuccessfulPreparedStatementWithAllTypes")
            bindBoolean(1, true)
            bindBytes(2, "binary".encodeToByteArray())
            bindDouble(3, 3.14)
            bindLong(4, (Int.MAX_VALUE.toLong() * 5))
        }
        assertEquals(1L, result.value)
    }

    @Test
    fun testInsertThenSelectBooleanValue() {
        val stringVal = "testInsertThenSelectBooleanValue-" + randomString()
        // Insert
        var insert = driver.execute(
            null,
            "INSERT into blaat(" +
                    "$VARCHAR_FIELD, " +
                    "$BOOLEAN_FIELD, " +
                    "$BYTES_FIELD, " +
                    "$DOUBLE_FIELD, " +
                    "$LONG_FIELD" +
                    ") VALUES(?, ?, ?, ?, ?);",
            5
        ) {
            bindString(0, stringVal)
            bindBoolean(1, true)
            bindBytes(2, "binarybinary".encodeToByteArray())
            bindDouble(3, 3.14)
            bindLong(4, (Int.MAX_VALUE.toLong() * 5))
        }
        assertEquals(1L, insert.value, "First insert failed")
        insert = driver.execute(
            null,
            "INSERT into blaat(" +
                    "$VARCHAR_FIELD, " +
                    "$BOOLEAN_FIELD, " +
                    "$BYTES_FIELD, " +
                    "$DOUBLE_FIELD, " +
                    "$LONG_FIELD" +
                    ") VALUES(?, ?, ?, ?, ?);",
            5
        ) {
            bindString(0, stringVal)
            bindBoolean(1, false)
            bindBytes(2, "binarybinary".encodeToByteArray())
            bindDouble(3, 14.3)
            bindLong(4, (Int.MAX_VALUE.toLong() * 5))
        }
        assertEquals(1L, insert.value, "Second insert failed")

        // Fetch
        val result = driver.executeQuery(
            identifier = null,
            sql = "SELECT $BOOLEAN_FIELD, $VARCHAR_FIELD, $BYTES_FIELD, $DOUBLE_FIELD, $LONG_FIELD FROM blaat WHERE $VARCHAR_FIELD = '$stringVal';",
            parameters = 0,
            binders = null,
            mapper = {
                buildList {
                    while (it.next()) {
                        add(it.getBoolean(0))
                    }
                }
            }
        )
        assertEquals(2, result.value.size)
        assertEquals(1, result.value.count { it == true } )
        assertEquals(1, result.value.count { it == false } )
    }

    @Test
    fun testConnectionProblem() {
        val e = assertFailsWith<IllegalArgumentException>("Connection to 127.1.2.3 should not succeed.") {
            val driver = MySQLNativeDriver(
                "127.1.2.3",
                "onsdb",
                "root",
                "",
                3306
            )
        }
        requireNotNull(e.message)
        assertContains(e.message!!, "Can't connect to MySQL server on '127.1.2.3'")
    }

    @Test
    fun testErrorQuery() {
        memScoped {
            println("Teststart")
            mysql_library_init!!.invoke(0, null, null)
            println("LibraryInit")
            val mysequel = mysql_init(null)
            println("MysqlInit")
            val mysqlconnected = mysql_real_connect(
                mysequel,
                "localhost",
                "root",
                "",
                "onsdb",
                3306,
                null,
                1
            )
            println("MysqlConnected")
            val result = mysql_query(mysqlconnected, "INSERT into blaat(fakefield) VALUES('cinterop');")
            println("MysqlQuery")
            assertEquals(1, result) // 0 return code indicates success
        }
    }

    fun randomString(lenght: Int = 6): String = (1..lenght)
        .map { Random.nextInt(0, charPool.size).let { charPool[it] } }
        .joinToString { "" }

    companion object {
        val charPool = ('a'..'z') + ('A'..'Z') + ('0'..'9')
    }
}