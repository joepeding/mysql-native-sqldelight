package nl.joepeding.sqldelight.mysqldriver

import kotlinx.cinterop.*
import mysql.*
import kotlin.test.*

class MinimalTest {
    private final val VARCHAR_FIELD = "vcfield"
    private final val BOOLEAN_FIELD = "boolfield"
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
                "`$BOOLEAN_FIELD` BOOLEAN NOT NULL DEFAULT 0" +
                ");", 0)
    }

    @Test
    fun testSuccessfulPlainQuery() {
        val result = driver.execute(null, "INSERT into blaat($VARCHAR_FIELD) VALUES('testSuccessfulPlainQuery');", 0)
        assertEquals(0L, result.value)
    }

    @Test
    fun testSuccessfulPreparedStatement() {
        val result = driver.execute(null, "INSERT into blaat($VARCHAR_FIELD) VALUES(?);", 1) {
            bindString(0, "testSuccessfulPreparedStatement")
        }
        assertEquals(0L, result.value)
    }

    @Test
    fun testSuccessfulPreparedStatementWithBooleans() {
        val result = driver.execute(null, "INSERT into blaat($VARCHAR_FIELD, $BOOLEAN_FIELD) VALUES(?, ?);", 2) {
            bindString(0, "testSuccessfulPreparedStatementWithBooleans")
            bindBoolean(1, true)
        }
        assertEquals(0L, result.value)
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
}