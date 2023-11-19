package nl.joepeding.sqldelight.mysql.native.driver

import app.cash.sqldelight.db.QueryResult.Value
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
            "127.0.0.1",
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
                Value(buildList {
                    while (it.next().value) {
                        add(
                            Pair(
                                it.getLong(1),
                                it.getString(2)
                            )
                        )
                    }
                })
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
                    ") VALUES(?, ?);",
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
                Value(buildList {
                    while (it.next().value) {
                        add(
                            it.getLong(1),
                        )
                    }
                })
            }
        )

        assertEquals(2050, result.value.first(), "Inserted YEAR value does not match to expected Long")
    }

    @Test
    fun `MySQL DATE field type can be read to kotlinx LocalDate and String and set with String or LocalDate`() {
        val stringVal = "dateField-" + MinimalTest.randomString()

        // Create table
        driver.execute(
            null, "CREATE TABLE IF NOT EXISTS `datefieldtest`(" +
                    "`$TESTNAME_FIELD` VARCHAR(255) NOT NULL," +
                    "`datefield` DATE DEFAULT NULL," +
                    "`datefield2` DATE DEFAULT NULL" +
                    ");", 0
        )

        // Insert
        driver.execute(
            null,
            "INSERT into datefieldtest(" +
                    "$TESTNAME_FIELD, " +
                    "datefield," +
                    "datefield2" +
                    ") VALUES(?, ?, ?);",
            6
        ) {
            check(this is MySQLPreparedStatement)
            bindString(0, stringVal)
            bindString(1, "2023-08-27")
            bindDate(2, LocalDate(2023, 8, 27))
        }

        val result = driver.executeQuery(
            identifier = null,
            sql = "SELECT $TESTNAME_FIELD, datefield, datefield2 FROM datefieldtest WHERE $TESTNAME_FIELD = '$stringVal';",
            parameters = 0,
            binders = null,
            mapper = {
                check(it is MySQLCursor)
                Value(buildList {
                    while (it.next().value) {
                        add(
                            Triple(
                                it.getString(1),
                                it.getDate(1),
                                it.getString(2)
                            )
                        )
                    }
                })
            }
        )

        assertEquals("2023-08-27", result.value.first().first, "Inserted DATE value does not match to expected String")
        assertEquals(LocalDate(2023, 8, 27), result.value.first().second, "Inserted DATE value does not match to expected String")
        assertEquals("2023-08-27", result.value.first().third, "Inserted DATE value does not match to expected String")
    }

    @Test
    fun `MySQL DATETIME field type can be read to kotlinx LocalDateTime and String and set with String and LocalDateTime`() {
        val stringVal = "datetimeField-" + MinimalTest.randomString()

        // Create table
        driver.execute(
            null, "CREATE TABLE IF NOT EXISTS `datetimefieldtest`(" +
                    "`$TESTNAME_FIELD` VARCHAR(255) NOT NULL," +
                    "`datetimefield` DATETIME DEFAULT NULL," +
                    "`datetimefieldmid` DATETIME(3) DEFAULT NULL," +
                    "`datetimefieldmax` DATETIME(6) DEFAULT NULL," +
                    "`datetimefieldfromdatetime` DATETIME(6) DEFAULT NULL" +
                    ");", 0
        )

        // Insert
        driver.execute(
            null,
            "INSERT into datetimefieldtest(" +
                    "$TESTNAME_FIELD, " +
                    "datetimefield," +
                    "datetimefieldmid," +
                    "datetimefieldmax," +
                    "datetimefieldfromdatetime" +
                    ") VALUES(?, ?, ?, ?, ?);",
            5
        ) {
            check(this is MySQLPreparedStatement)
            bindString(0, stringVal)
            bindString(1, "2023-08-27 13:37:31.337")
            bindString(2, "2023-08-27 13:37:31.337")
            bindString(3, "2023-08-27 13:37:31.337133")
            bindDateTime(4, LocalDateTime(2023,8,27,13,37,31,337133000))
        }

        val result = driver.executeQuery(
            identifier = null,
            sql = "SELECT $TESTNAME_FIELD, datetimefield, datetimefieldmid, datetimefieldmax, datetimefieldfromdatetime FROM datetimefieldtest WHERE $TESTNAME_FIELD = '$stringVal';",
            parameters = 0,
            binders = null,
            mapper = {
                require(it is MySQLCursor)
                Value(buildList {
                    while (it.next().value) {
                        add(
                            mapOf(
                                "datetimefield" to Pair(
                                    it.getString(1),
                                    it.getDateTime(1)
                                ),
                                "datetimefieldmid" to Pair(
                                    it.getString(2),
                                    it.getDateTime(2)
                                ),
                                "datetimefieldmax" to Pair(
                                    it.getString(3),
                                    it.getDateTime(3)
                                ),
                                "datetimefieldfromdatetime" to Pair(
                                    it.getString(4),
                                    it.getDateTime(4)
                                ),
                            )
                        )
                    }
                })
            }
        )

        assertEquals("2023-08-27T13:37:31", result.value.first()["datetimefield"]!!.first)
        assertEquals(LocalDateTime(2023,8,27,13,37,31,0), result.value.first()["datetimefield"]!!.second)

        assertEquals("2023-08-27T13:37:31.337", result.value.first()["datetimefieldmid"]!!.first)
        assertEquals(LocalDateTime(2023,8,27,13,37,31,337000000), result.value.first()["datetimefieldmid"]!!.second)

        assertEquals("2023-08-27T13:37:31.337133", result.value.first()["datetimefieldmax"]!!.first)
        assertEquals(LocalDateTime(2023,8,27,13,37,31,337133000), result.value.first()["datetimefieldmax"]!!.second)

        assertEquals("2023-08-27T13:37:31.337133", result.value.first()["datetimefieldfromdatetime"]!!.first)
        assertEquals(LocalDateTime(2023,8,27,13,37,31,337133000), result.value.first()["datetimefieldfromdatetime"]!!.second)
    }

    @Test
    fun `MySQL TIME field type can be read to kotlin Duration and String and set with String and Duration`() {
        val stringVal = "timeField-" + MinimalTest.randomString()

        // Create table
        driver.execute(
            null, "CREATE TABLE IF NOT EXISTS `timefieldtest`(" +
                    "`$TESTNAME_FIELD` VARCHAR(255) NOT NULL," +
                    "`timefield` TIME DEFAULT NULL," +
                    "`timefieldmid` TIME(3) DEFAULT NULL," +
                    "`timefieldmax` TIME(6) DEFAULT NULL," +
                    "`timefieldfromduration` TIME(6) DEFAULT NULL" +
                    ");", 0
        )

        // Insert
        driver.execute(
            null,
            "INSERT into timefieldtest(" +
                    "$TESTNAME_FIELD, " +
                    "timefield," +
                    "timefieldmid," +
                    "timefieldmax," +
                    "timefieldfromduration" +
                    ") VALUES(?, ?, ?, ?, ?);",
            5
        ) {
            check(this is MySQLPreparedStatement)
            bindString(0, stringVal)
            bindString(1, "13:37:31.337")
            bindString(2, "13:37:31.337")
            bindString(3, "13:37:31.337133")
            bindDuration(
                4,
                Duration.ZERO
                    .plus(13.toDuration(DurationUnit.HOURS))
                    .plus(37.toDuration(DurationUnit.MINUTES))
                    .plus(31.toDuration(DurationUnit.SECONDS))
                    .plus(337133.toDuration(DurationUnit.MICROSECONDS))
            )
        }

        val result = driver.executeQuery(
            identifier = null,
            sql = "SELECT $TESTNAME_FIELD, timefield, timefieldmid, timefieldmax, timefieldfromduration FROM timefieldtest WHERE $TESTNAME_FIELD = '$stringVal';",
            parameters = 0,
            binders = null,
            mapper = {
                require(it is MySQLCursor)
                Value(buildList {
                    while (it.next().value) {
                        add(
                            mapOf(
                                "timefield" to Pair(
                                    it.getString(1),
                                    it.getDuration(1)
                                ),
                                "timefieldmid" to Pair(
                                    it.getString(2),
                                    it.getDuration(2)
                                ),
                                "timefieldmax" to Pair(
                                    it.getString(3),
                                    it.getDuration(3)
                                ),
                                "timefieldfromduration" to Pair(
                                    it.getString(4),
                                    it.getDuration(4)
                                ),
                            )
                        )
                    }
                })
            }
        )

        assertEquals("PT13H37M31S", result.value.first()["timefield"]!!.first)
        assertEquals(13.toDuration(DurationUnit.HOURS) + 37.toDuration(DurationUnit.MINUTES) + 31.toDuration(DurationUnit.SECONDS), result.value.first()["timefield"]!!.second)

        assertEquals("PT13H37M31.337S", result.value.first()["timefieldmid"]!!.first)
        assertEquals(
            13.toDuration(DurationUnit.HOURS) +
                37.toDuration(DurationUnit.MINUTES) +
                31.toDuration(DurationUnit.SECONDS) +
                337000.toDuration(DurationUnit.MICROSECONDS),
            result.value.first()["timefieldmid"]!!.second
        )

        assertEquals("PT13H37M31.337133S", result.value.first()["timefieldmax"]!!.first)
        assertEquals(
            13.toDuration(DurationUnit.HOURS) +
                    37.toDuration(DurationUnit.MINUTES) +
                    31.toDuration(DurationUnit.SECONDS) +
                    337133.toDuration(DurationUnit.MICROSECONDS),
            result.value.first()["timefieldmax"]!!.second
        )

        assertEquals("PT13H37M31.337133S", result.value.first()["timefieldfromduration"]!!.first)
        assertEquals(
            13.toDuration(DurationUnit.HOURS) +
                    37.toDuration(DurationUnit.MINUTES) +
                    31.toDuration(DurationUnit.SECONDS) +
                    337133.toDuration(DurationUnit.MICROSECONDS),
            result.value.first()["timefieldfromduration"]!!.second
        )
    }

    @Test
    fun `MySQL TIMESTAMP field type can be read to kotlinx DateTime and String and set with String and LocalDateTime`() {
        val stringVal = "timestampField-" + MinimalTest.randomString()

        // Create table
        driver.execute(
            null, "CREATE TABLE IF NOT EXISTS `timestampfieldtest`(" +
                    "`$TESTNAME_FIELD` VARCHAR(255) NOT NULL," +
                    "`timestampfield` TIMESTAMP NULL DEFAULT NULL," +
                    "`timestampfieldmid` TIMESTAMP(3) NULL DEFAULT NULL," +
                    "`timestampfieldmax` TIMESTAMP(6) NULL DEFAULT NULL," +
                    "`timestampfielfromdatetime` TIMESTAMP(6) NULL DEFAULT NULL" +
                    ");", 0
        )

        // Insert
        driver.execute(
            null,
            "INSERT into timestampfieldtest(" +
                    "$TESTNAME_FIELD, " +
                    "timestampfield," +
                    "timestampfieldmid," +
                    "timestampfieldmax," +
                    "timestampfielfromdatetime" +
                    ") VALUES(?, ?, ?, ?, ?);",
            5
        ) {
            check(this is MySQLPreparedStatement)
            bindString(0, stringVal)
            bindString(1, "2023-08-27 13:37:31.337")
            bindString(2, "2023-08-27 13:37:31.337")
            bindString(3, "2023-08-27 13:37:31.337133")
            bindDateTime(4, LocalDateTime(2023, 8, 27, 13, 37, 31, 337133000))
        }

        val result = driver.executeQuery(
            identifier = null,
            sql = "SELECT $TESTNAME_FIELD, timestampfield, timestampfieldmid, timestampfieldmax, timestampfielfromdatetime FROM timestampfieldtest WHERE $TESTNAME_FIELD = '$stringVal';",
            parameters = 0,
            binders = null,
            mapper = {
                require(it is MySQLCursor)
                Value(buildList {
                    while (it.next().value) {
                        add(
                            mapOf(
                                "timestampfield" to Pair(
                                    it.getString(1),
                                    it.getDateTime(1)
                                ),
                                "timestampfieldmid" to Pair(
                                    it.getString(2),
                                    it.getDateTime(2)
                                ),
                                "timestampfieldmax" to Pair(
                                    it.getString(3),
                                    it.getDateTime(3)
                                ),
                                "timestampfielfromdatetime" to Pair(
                                    it.getString(4),
                                    it.getDateTime(4)
                                ),
                            )
                        )
                    }
                })
            }
        )

        assertEquals("2023-08-27T13:37:31", result.value.first()["timestampfield"]!!.first)
        assertEquals(LocalDateTime(2023,8,27,13,37,31,0), result.value.first()["timestampfield"]!!.second)

        assertEquals("2023-08-27T13:37:31.337", result.value.first()["timestampfieldmid"]!!.first)
        assertEquals(LocalDateTime(2023,8,27,13,37,31,337000000), result.value.first()["timestampfieldmid"]!!.second)

        assertEquals("2023-08-27T13:37:31.337133", result.value.first()["timestampfieldmax"]!!.first)
        assertEquals(LocalDateTime(2023,8,27,13,37,31,337133000), result.value.first()["timestampfieldmax"]!!.second)

        assertEquals("2023-08-27T13:37:31.337133", result.value.first()["timestampfielfromdatetime"]!!.first)
        assertEquals(LocalDateTime(2023,8,27,13,37,31,337133000), result.value.first()["timestampfielfromdatetime"]!!.second)
    }

    @Test
    fun `MySQL SET field type can be read to String and set with String`() {
        val stringVal = "setField-" + MinimalTest.randomString()

        // Create table
        driver.execute(
            null, "CREATE TABLE IF NOT EXISTS `setfieldtest`(\n" +
                    "`$TESTNAME_FIELD` VARCHAR(255) NOT NULL,\n" +
                    "`setfield` SET('a','b','c','d')\n" +
                    ");", 0
        )

        // Insert
        driver.execute(
            null,
            "INSERT into setfieldtest(" +
                    "$TESTNAME_FIELD, " +
                    "setfield" +
                    ") VALUES(?, ?);",
            4
        ) {
            bindString(0, stringVal)
            bindString(1, "a,c")
        }

        val result = driver.executeQuery(
            identifier = null,
            sql = "SELECT $TESTNAME_FIELD, setfield FROM setfieldtest WHERE $TESTNAME_FIELD = '$stringVal';",
            parameters = 0,
            binders = null,
            mapper = {
                require(it is MySQLCursor)
                Value(buildList {
                    while (it.next().value) {
                        add(
                            it.getString(1)
                        )
                    }
                })
            }
        )

        assertEquals("a,c", result.value.first())
    }

    @Test
    fun `MySQL ENUM field type can be read to String and set with String`() {
        val stringVal = "enumField-" + MinimalTest.randomString()

        // Create table
        driver.execute(
            null, "CREATE TABLE IF NOT EXISTS `enumfieldtest`(\n" +
                    "`$TESTNAME_FIELD` VARCHAR(255) NOT NULL,\n" +
                    "`enumfield` ENUM('a','b','c','d')\n" +
                    ");", 0
        )

        // Insert
        driver.execute(
            null,
            "INSERT into enumfieldtest(" +
                    "$TESTNAME_FIELD, " +
                    "enumfield" +
                    ") VALUES(?, ?);",
            4
        ) {
            bindString(0, stringVal)
            bindString(1, "a")
        }

        val result = driver.executeQuery(
            identifier = null,
            sql = "SELECT $TESTNAME_FIELD, enumfield FROM enumfieldtest WHERE $TESTNAME_FIELD = '$stringVal';",
            parameters = 0,
            binders = null,
            mapper = {
                require(it is MySQLCursor)
                Value(buildList {
                    while (it.next().value) {
                        add(
                            it.getString(1)
                        )
                    }
                })
            }
        )

        assertEquals("a", result.value.first())
    }

    @Test
    fun `MySQL GEOMETRY field type can be read to String or ByteArray and set with String or ByteArray`() {
        val stringVal = "geometryField-" + MinimalTest.randomString()

        // Create table
        driver.execute(
            null, "CREATE TABLE IF NOT EXISTS `geomfieldtest`(\n" +
                    "`$TESTNAME_FIELD` VARCHAR(255) NOT NULL,\n" +
                    "`geomStringfield` GEOMETRY,\n" +
                    "`geomBlobfield` GEOMETRY" +
                    ");", 0
        )

        // WKB for 'POINT(1 -1)' from Table 11.2 in https://dev.mysql.com/doc/refman/8.0/en/gis-data-formats.html:
        val wkb = ByteArray(21).apply {
            // Byte order
            set(0, 1.toByte())

            // WKB Type
            set(1, 1.toByte()) // 2-4 stay zero

            // X-coord
            // 5-10 stay zero
            set(11, 240.toByte())
            set(12, 63.toByte())

            // Y-coord
            // 13-18 stay zero
            set(19, 240.toByte())
            set(20, 191.toByte())
        }

        // Insert
        driver.execute(
            null,
            "INSERT into geomfieldtest(" +
                    "$TESTNAME_FIELD, " +
                    "geomStringfield," +
                    "geomBlobfield" +
                    ") VALUES(?, ST_GeomFromText(?), ST_GeomFromWKB(?));",
            4
        ) {
            bindString(0, stringVal)
            bindString(1, "POINT(1 1)")
            bindBytes(2, wkb)
        }

        val result = driver.executeQuery(
            identifier = null,
            sql = "SELECT $TESTNAME_FIELD, ST_AsText(geomStringfield), ST_AsBinary(geomStringField), ST_AsText(geomBlobField), ST_AsBinary(geomBlobField) FROM geomfieldtest WHERE $TESTNAME_FIELD = '$stringVal';",
            parameters = 0,
            binders = null,
            mapper = {
                require(it is MySQLCursor)
                Value(buildList {
                    while (it.next().value) {
                        add(
                            Pair(
                                Pair(
                                    it.getString(1),
                                    it.getString(3),
                                ),
                                Pair(
                                    it.getBytes(2),
                                    it.getBytes(4),
                                )
                            )
                        )
                    }
                })
            }
        )

        result.value.first().let { (strings, bytes) ->
            assertEquals("POINT(1 1)", strings.first)
            assertEquals("POINT(1 -1)", strings.second)
            assertContentEquals(wkb.copyOf().apply { set(20, 63.toByte()) }, bytes.first)
            assertContentEquals(wkb, bytes.second)
        }
    }
}