package nl.joepeding.sqldelight.mysqldriver

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlin.test.*
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class DataTypeTest {
    private final val TESTNAME_FIELD = "testname"
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
    fun `MySQL BIT field type can be read to Long and set with ByteArray`() {
        val stringVal = "testBitField-" + MinimalTest.randomString()

        // Create table
        driver.execute(
            null, "CREATE TABLE IF NOT EXISTS `bitfieldtest`(" +
                    "`$TESTNAME_FIELD` VARCHAR(255) NOT NULL," +
                    "`bitfield` BIT(6) DEFAULT NULL" +
                    ");", 0
        )

        // Insert
        driver.execute(
            null,
            "INSERT into bitfieldtest(" +
                    "$TESTNAME_FIELD, " +
                    "bitfield" +
                    ") VALUES(?, ?);", // Bit field can also be set with `b'000111'`
            6
        ) {
            bindString(0, stringVal)
            bindBytes(1, ByteArray(1) { 7.toByte() })
        }

        val result = driver.executeQuery(
            identifier = null,
            sql = "SELECT $TESTNAME_FIELD, bitfield, BIN(bitfield) as bitfieldstring FROM bitfieldtest WHERE $TESTNAME_FIELD = '$stringVal';",
            parameters = 0,
            binders = null,
            mapper = {
                buildList {
                    while (it.next()) {
                        add(
                            Pair(
                                it.getLong(1),
                                it.getString(2)
                            )
                        )
                    }
                }
            }
        )

        assertEquals(7L, result.value.first().first, "Inserted BIT value does not match to expected Long")
        assertEquals(
            "111",
            result.value.first().second,
            "Inserted BIT value does not match to expected string conversion"
        )
    }

    @Test
    fun `MySQL YEAR field type can be read to Long and set with Long`() {
        val stringVal = "yearField-" + MinimalTest.randomString()

        // Create table
        driver.execute(
            null, "CREATE TABLE IF NOT EXISTS `yearfieldtest`(" +
                    "`$TESTNAME_FIELD` VARCHAR(255) NOT NULL," +
                    "`yearfield` YEAR DEFAULT NULL" +
                    ");", 0
        )

        // Insert
        driver.execute(
            null,
            "INSERT into yearfieldtest(" +
                    "$TESTNAME_FIELD, " +
                    "yearfield" +
                    ") VALUES(?, ?);", // Bit field can also be set with `b'000111'`
            6
        ) {
            bindString(0, stringVal)
            bindLong(1, 2050L)
        }

        val result = driver.executeQuery(
            identifier = null,
            sql = "SELECT $TESTNAME_FIELD, yearfield FROM yearfieldtest WHERE $TESTNAME_FIELD = '$stringVal';",
            parameters = 0,
            binders = null,
            mapper = {
                buildList {
                    while (it.next()) {
                        add(
                            it.getLong(1),
                        )
                    }
                }
            }
        )

        assertEquals(2050, result.value.first(), "Inserted YEAR value does not match to expected Long")
    }

    @Test
    fun `MySQL DATE field type can be read to kotlinx LocalDate and String and set with String`() {
        val stringVal = "dateField-" + MinimalTest.randomString()

        // Create table
        driver.execute(
            null, "CREATE TABLE IF NOT EXISTS `datefieldtest`(" +
                    "`$TESTNAME_FIELD` VARCHAR(255) NOT NULL," +
                    "`datefield` DATE DEFAULT NULL" +
                    ");", 0
        )

        // Insert
        driver.execute(
            null,
            "INSERT into datefieldtest(" +
                    "$TESTNAME_FIELD, " +
                    "datefield" +
                    ") VALUES(?, ?);", // Bit field can also be set with `b'000111'`
            6
        ) {
            bindString(0, stringVal)
            bindString(1, "2023-08-27")
        }

        val result = driver.executeQuery(
            identifier = null,
            sql = "SELECT $TESTNAME_FIELD, datefield FROM datefieldtest WHERE $TESTNAME_FIELD = '$stringVal';",
            parameters = 0,
            binders = null,
            mapper = {
                buildList {
                    while (it.next()) {
                        add(
                            Pair(
                                it.getString(1),
                                (it as MySQLCursor).getDate(1)
                            )
                        )
                    }
                }
            }
        )

        assertEquals("2023-08-27", result.value.first().first, "Inserted DATE value does not match to expected String")
        assertEquals(LocalDate(2023, 8, 27), result.value.first().second, "Inserted DATE value does not match to expected String")
    }

    @Test
    fun `MySQL DATETIME field type can be read to kotlinx LocalDateTime and String and set with String`() {
        val stringVal = "datetimeField-" + MinimalTest.randomString()

        // Create table
        driver.execute(
            null, "CREATE TABLE IF NOT EXISTS `datetimefieldtest`(" +
                    "`$TESTNAME_FIELD` VARCHAR(255) NOT NULL," +
                    "`datetimefield` DATETIME DEFAULT NULL," +
                    "`datetimefieldmid` DATETIME(3) DEFAULT NULL," +
                    "`datetimefieldmax` DATETIME(6) DEFAULT NULL" +
                    ");", 0
        )

        // Insert
        driver.execute(
            null,
            "INSERT into datetimefieldtest(" +
                    "$TESTNAME_FIELD, " +
                    "datetimefield," +
                    "datetimefieldmid," +
                    "datetimefieldmax" +
                    ") VALUES(?, ?, ?, ?);", // Bit field can also be set with `b'000111'`
            4
        ) {
            bindString(0, stringVal)
            bindString(1, "2023-08-27 13:37:31.337")
            bindString(2, "2023-08-27 13:37:31.337")
            bindString(3, "2023-08-27 13:37:31.337133")
        }

        val result = driver.executeQuery(
            identifier = null,
            sql = "SELECT $TESTNAME_FIELD, datetimefield, datetimefieldmid, datetimefieldmax FROM datetimefieldtest WHERE $TESTNAME_FIELD = '$stringVal';",
            parameters = 0,
            binders = null,
            mapper = {
                buildList {
                    while (it.next()) {
                        add(
                            mapOf(
                                "datetimefield" to Pair(
                                    it.getString(1),
                                    (it as MySQLCursor).getDateTime(1)
                                ),
                                "datetimefieldmid" to Pair(
                                    it.getString(2),
                                    (it as MySQLCursor).getDateTime(2)
                                ),
                                "datetimefieldmax" to Pair(
                                    it.getString(3),
                                    (it as MySQLCursor).getDateTime(3)
                                ),
                            )
                        )
                    }
                }
            }
        )

        assertEquals("2023-08-27T13:37:31", result.value.first()["datetimefield"]!!.first)
        assertEquals(LocalDateTime(2023,8,27,13,37,31,0), result.value.first()["datetimefield"]!!.second)

        assertEquals("2023-08-27T13:37:31.337", result.value.first()["datetimefieldmid"]!!.first)
        assertEquals(LocalDateTime(2023,8,27,13,37,31,337000000), result.value.first()["datetimefieldmid"]!!.second)

        assertEquals("2023-08-27T13:37:31.337133", result.value.first()["datetimefieldmax"]!!.first)
        assertEquals(LocalDateTime(2023,8,27,13,37,31,337133000), result.value.first()["datetimefieldmax"]!!.second)
    }

    @Test
    fun `MySQL TIME field type can be read to kotlin Duration and String and set with String`() {
        val stringVal = "timeField-" + MinimalTest.randomString()

        // Create table
        driver.execute(
            null, "CREATE TABLE IF NOT EXISTS `timefieldtest`(" +
                    "`$TESTNAME_FIELD` VARCHAR(255) NOT NULL," +
                    "`timefield` TIME DEFAULT NULL," +
                    "`timefieldmid` TIME(3) DEFAULT NULL," +
                    "`timefieldmax` TIME(6) DEFAULT NULL" +
                    ");", 0
        )

        // Insert
        driver.execute(
            null,
            "INSERT into timefieldtest(" +
                    "$TESTNAME_FIELD, " +
                    "timefield," +
                    "timefieldmid," +
                    "timefieldmax" +
                    ") VALUES(?, ?, ?, ?);", // Bit field can also be set with `b'000111'`
            4
        ) {
            bindString(0, stringVal)
            bindString(1, "13:37:31.337")
            bindString(2, "13:37:31.337")
            bindString(3, "13:37:31.337133")
        }

        val result = driver.executeQuery(
            identifier = null,
            sql = "SELECT $TESTNAME_FIELD, timefield, timefieldmid, timefieldmax FROM timefieldtest WHERE $TESTNAME_FIELD = '$stringVal';",
            parameters = 0,
            binders = null,
            mapper = {
                buildList {
                    while (it.next()) {
                        add(
                            mapOf(
                                "timefield" to Pair(
                                    it.getString(1),
                                    (it as MySQLCursor).getDuration(1)
                                ),
                                "timefieldmid" to Pair(
                                    it.getString(2),
                                    (it as MySQLCursor).getDuration(2)
                                ),
                                "timefieldmax" to Pair(
                                    it.getString(3),
                                    (it as MySQLCursor).getDuration(3)
                                ),
                            )
                        )
                    }
                }
            }
        )

        assertEquals("13h 37m 31s", result.value.first()["timefield"]!!.first)
        assertEquals(13.toDuration(DurationUnit.HOURS) + 37.toDuration(DurationUnit.MINUTES) + 31.toDuration(DurationUnit.SECONDS), result.value.first()["timefield"]!!.second)

        assertEquals("13h 37m 31.337s", result.value.first()["timefieldmid"]!!.first)
        assertEquals(
            13.toDuration(DurationUnit.HOURS) +
                37.toDuration(DurationUnit.MINUTES) +
                31.toDuration(DurationUnit.SECONDS) +
                337000.toDuration(DurationUnit.MICROSECONDS),
            result.value.first()["timefieldmid"]!!.second
        )

        assertEquals("13h 37m 31.337133s", result.value.first()["timefieldmax"]!!.first)
        assertEquals(
            13.toDuration(DurationUnit.HOURS) +
                    37.toDuration(DurationUnit.MINUTES) +
                    31.toDuration(DurationUnit.SECONDS) +
                    337133.toDuration(DurationUnit.MICROSECONDS),
            result.value.first()["timefieldmax"]!!.second
        )
    }

    @Test
    fun `MySQL TIMESTAMP field type can be read to kotlinx DateTime and String and set with String`() {
        val stringVal = "timeField-" + MinimalTest.randomString()

        // Create table
        driver.execute(
            null, "CREATE TABLE IF NOT EXISTS `timestampfieldtest`(" +
                    "`$TESTNAME_FIELD` VARCHAR(255) NOT NULL," +
                    "`timestampfield` TIMESTAMP NULL DEFAULT NULL," +
                    "`timestampfieldmid` TIMESTAMP(3) NULL DEFAULT NULL," +
                    "`timestampfieldmax` TIMESTAMP(6) NULL DEFAULT NULL" +
                    ");", 0
        )

        // Insert
        driver.execute(
            null,
            "INSERT into timestampfieldtest(" +
                    "$TESTNAME_FIELD, " +
                    "timestampfield," +
                    "timestampfieldmid," +
                    "timestampfieldmax" +
                    ") VALUES(?, ?, ?, ?);", // Bit field can also be set with `b'000111'`
            4
        ) {
            bindString(0, stringVal)
            bindString(1, "2023-08-27 13:37:31.337")
            bindString(2, "2023-08-27 13:37:31.337")
            bindString(3, "2023-08-27 13:37:31.337133")
        }

        val result = driver.executeQuery(
            identifier = null,
            sql = "SELECT $TESTNAME_FIELD, timestampfield, timestampfieldmid, timestampfieldmax FROM timestampfieldtest WHERE $TESTNAME_FIELD = '$stringVal';",
            parameters = 0,
            binders = null,
            mapper = {
                buildList {
                    while (it.next()) {
                        add(
                            mapOf(
                                "timestampfield" to Pair(
                                    it.getString(1),
                                    (it as MySQLCursor).getDateTime(1)
                                ),
                                "timestampfieldmid" to Pair(
                                    it.getString(2),
                                    (it as MySQLCursor).getDateTime(2)
                                ),
                                "timestampfieldmax" to Pair(
                                    it.getString(3),
                                    (it as MySQLCursor).getDateTime(3)
                                ),
                            )
                        )
                    }
                }
            }
        )

        assertEquals("2023-08-27T13:37:31", result.value.first()["timestampfield"]!!.first)
        assertEquals(LocalDateTime(2023,8,27,13,37,31,0), result.value.first()["timestampfield"]!!.second)

        assertEquals("2023-08-27T13:37:31.337", result.value.first()["timestampfieldmid"]!!.first)
        assertEquals(LocalDateTime(2023,8,27,13,37,31,337000000), result.value.first()["timestampfieldmid"]!!.second)

        assertEquals("2023-08-27T13:37:31.337133", result.value.first()["timestampfieldmax"]!!.first)
        assertEquals(LocalDateTime(2023,8,27,13,37,31,337133000), result.value.first()["timestampfieldmax"]!!.second)
    }
}