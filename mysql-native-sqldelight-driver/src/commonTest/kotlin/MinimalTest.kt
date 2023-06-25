import kotlinx.cinterop.*
import mysql.*
import nl.joepeding.sqldelight.mysqldriver.MySQLNativeDriver
import kotlin.test.Test
import kotlin.test.assertEquals

@Test
fun testSuccessfulQuery() {
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