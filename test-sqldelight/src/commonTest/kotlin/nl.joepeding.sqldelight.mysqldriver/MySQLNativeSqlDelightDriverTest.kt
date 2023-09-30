package nl.joepeding.sqldelight.mysqldriver

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import nl.joepeding.sqldelight.testmysql.NativeMySQL
import nljoepedingsqldelightmysqldriver.Foo
import kotlin.random.Random
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class MySQLNativeSqlDelightDriverTest {
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
    }

    @Test
    fun allTypes() {
        val queries = NativeMySQL(driver).fooQueries
        NativeMySQL.Schema.migrate(driver, 0, NativeMySQL.Schema.version)

        try {
            assertEquals(emptyList(), queries.get().executeAsList())
        } catch (e: Throwable) {
            println(e.message)
            throw e
        }

        println("Migrations?")
        val foo = Foo(
            a = 42,
            b = "Foo-" + randomString(),
            date = LocalDate(2020, Month.DECEMBER, 12),
            time = 12.toDuration(DurationUnit.HOURS) +  42.toDuration(DurationUnit.MINUTES),
            timestamp = LocalDateTime(2014, Month.AUGUST, 1, 12, 1, 2, 0),
        )

        println("Creating Foo")
        queries.create(
            a = foo.a,
            b = foo.b,
            date = foo.date,
            time = foo.time,
            timestamp = foo.timestamp,
        )
        println("Created Foo")

        assertEquals(foo, queries.get().executeAsOne())
    }

    companion object {
        fun randomString(lenght: Int = 6): String = (1..lenght)
            .map { Random.nextInt(0, charPool.size).let { charPool[it] } }
            .joinToString("")

        val charPool = ('a'..'z') + ('A'..'Z') + ('0'..'9')
    }
}