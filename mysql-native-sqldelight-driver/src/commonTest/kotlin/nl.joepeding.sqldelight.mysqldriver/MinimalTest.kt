package nl.joepeding.sqldelight.mysqldriver

import kotlinx.cinterop.*
import mysql.*
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MinimalTest {
    @Test
    fun testSuccessfulPlainQuery() {
        val driver = MySQLNativeDriver(
            "localhost",
            "onsdb",
            "root",
            "",
            3306
        )
        val result = driver.execute(null, "INSERT into blaat(foo) VALUES('cinterop');", 0)
        assertEquals(0L, result.value)
    }

    @Test
    fun testSuccessfulPreparedStatement() {
        val driver = MySQLNativeDriver(
            "localhost",
            "onsdb",
            "root",
            "",
            3306
        )
        val result = driver.execute(null, "INSERT into blaat(foo) VALUES(?);", 1) {
            bindString(1, "cinterop")
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
            val result = mysql_query(mysqlconnected, "INSERT into blaat(fool) VALUES('cinterop');")
            println("MysqlQuery")
            assertEquals(1, result) // 0 return code indicates success
        }
    }
}