import kotlinx.cinterop.CPointer
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import mysql.*
import kotlin.test.Test

class Test {
    @Test
    fun testQuery() {
        memScoped {
            val blaat = mysql_library_init!!.invoke(0, null, null)
            val mysequel = mysql_init(null)
            val mysqlconnected = mysql_real_connect(
                mysequel,
                "localhost",
                "onsdb",
                "",
                "onsdb",
                3306,
                null,
                1
            )
            mysql_query(mysqlconnected, "INSERT into blaat(foo) VALUES('cinterop');")

        }
        println("Hello, Kotlin/Native!")
    }
}