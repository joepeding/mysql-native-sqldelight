package nl.joepeding.sqldelight.mysqldriver

import kotlin.test.*

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
    fun `MySQL DATE field type can be read to String and set with String`() {
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
                            it.getString(1),
                        )
                    }
                }
            }
        )

        assertEquals("2023-08-27", result.value.first(), "Inserted DATE value does not match to expected String")
    }
}