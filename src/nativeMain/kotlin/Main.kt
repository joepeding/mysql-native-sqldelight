import kotlinx.cinterop.*
import mysql.*

fun main() {
    memScoped {
        println("HHH")
        val blaat = mysql_library_init!!.invoke(0, null, null)
        println("HOHOHO")
        val mysequel = mysql_init(null)
        println("HOIHOIHOI")
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
        println("HOI!HOI!HOI!")
        mysql_query(mysqlconnected, "INSERT into blaat(foo) VALUES('cinterop');")
        println("HOI1HOI1HOI1")
//        println(blaat)
    }

    println("Hello, Kotlin/Native! \n ") //${args.joinToString { " " }}")
}