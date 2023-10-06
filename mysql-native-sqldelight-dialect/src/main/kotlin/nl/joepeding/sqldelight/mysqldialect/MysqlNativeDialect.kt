package nl.joepeding.sqldelight.mysqldialect

import app.cash.sqldelight.dialect.api.*
import app.cash.sqldelight.dialects.mysql.MySqlDialect
import app.cash.sqldelight.dialects.mysql.MySqlTypeResolver
import app.cash.sqldelight.dialects.mysql.grammar.psi.MySqlTypeName
import com.alecstrong.sql.psi.core.psi.*
import com.squareup.kotlinpoet.*

class MysqlNativeDialect : SqlDelightDialect by MySqlDialect() {
    override val runtimeTypes: RuntimeTypes = RuntimeTypes(
        ClassName("nl.joepeding.sqldelight.mysqldriver", "MySQLCursor"),
        ClassName("nl.joepeding.sqldelight.mysqldriver", "MySQLPreparedStatement"),
    )

    override val asyncRuntimeTypes: RuntimeTypes
        get() = error("Async native driver is not yet supported")

    override fun typeResolver(parentResolver: TypeResolver): TypeResolver = MySQLNativeTypeResolver(parentResolver)
}

private class MySQLNativeTypeResolver(val parentResolver: TypeResolver) : TypeResolver by MySqlTypeResolver(parentResolver) {
    override fun definitionType(typeName: SqlTypeName): IntermediateType = with(typeName) {
        check(this is MySqlTypeName)
        return IntermediateType(
            when {
                approximateNumericDataType != null -> PrimitiveType.REAL
                binaryDataType != null -> PrimitiveType.BLOB
                dateDataType != null -> {
                    when (dateDataType!!.firstChild.text.uppercase()) {
                        "DATE" -> NativeMySqlType.DATE
                        "TIME" -> NativeMySqlType.TIME
                        "DATETIME" -> NativeMySqlType.DATETIME
                        "TIMESTAMP" -> NativeMySqlType.TIMESTAMP
                        "YEAR" -> PrimitiveType.TEXT
                        else -> throw IllegalArgumentException("Unknown date type ${dateDataType!!.text}")
                    }
                }
                tinyIntDataType != null -> if (tinyIntDataType!!.text == "BOOLEAN") {
                    NativeMySqlType.TINY_INT_BOOL
                } else {
                    NativeMySqlType.TINY_INT
                }
                smallIntDataType != null -> NativeMySqlType.SMALL_INT
                mediumIntDataType != null -> NativeMySqlType.INTEGER
                intDataType != null -> NativeMySqlType.INTEGER
                bigIntDataType != null -> NativeMySqlType.BIG_INT
//                fixedPointDataType != null -> NativeMySqlType.NUMERIC //TODO?
                jsonDataType != null -> PrimitiveType.TEXT
                enumSetType != null -> PrimitiveType.TEXT
                characterType != null -> PrimitiveType.TEXT
                bitDataType != null -> NativeMySqlType.BIT
                else -> throw IllegalArgumentException("Unknown kotlin type for sql type ${this.text}")
            },
        )
    }
}

private enum class NativeMySqlType(override val javaType: TypeName) : DialectType {
    TINY_INT(BYTE) {
        override fun decode(value: CodeBlock) = CodeBlock.of("%L.toByte()", value)

        override fun encode(value: CodeBlock) = CodeBlock.of("%L.toLong()", value)
    },
    TINY_INT_BOOL(BOOLEAN) {
        override fun decode(value: CodeBlock) = CodeBlock.of("%L == 1L", value)

        override fun encode(value: CodeBlock) = CodeBlock.of("if (%L) 1L else 0L", value)
    },
    SMALL_INT(SHORT) {
        override fun decode(value: CodeBlock) = CodeBlock.of("%L.toShort()", value)

        override fun encode(value: CodeBlock) = CodeBlock.of("%L.toLong()", value)
    },
    INTEGER(INT) {
        override fun decode(value: CodeBlock) = CodeBlock.of("%L.toInt()", value)

        override fun encode(value: CodeBlock) = CodeBlock.of("%L.toLong()", value)
    },
    BIG_INT(LONG),
    BIT(BOOLEAN) {
        override fun decode(value: CodeBlock) = CodeBlock.of("%L == 1L", value)

        override fun encode(value: CodeBlock) = CodeBlock.of("if (%L) 1L else 0L", value)
    },
//    NUMERIC(ClassName("java.math", "BigDecimal")), // TODO?
    DATE(ClassName("kotlinx.datetime", "LocalDate")),
    TIME(ClassName("kotlin.time", "Duration")),
    TIMESTAMP(ClassName("kotlinx.datetime", "LocalDateTime")),
    DATETIME(ClassName("kotlinx.datetime", "Instant")),
    ;

    override fun prepareStatementBinder(columnIndex: CodeBlock, value: CodeBlock): CodeBlock {
        return CodeBlock.builder()
            .add(
                when (this) {
                    TINY_INT, TINY_INT_BOOL, SMALL_INT, INTEGER, BIG_INT, BIT -> "bindLong"
                    DATE -> "bindDate"
                    TIME -> "bindDuration"
                    DATETIME, TIMESTAMP -> "bindDateTime"
//                    NUMERIC -> "bindBigDecimal" // TODO?
                },
            )
            .add("(%L, %L)\n", columnIndex, value)
            .build()
    }

    override fun cursorGetter(columnIndex: Int, cursorName: String): CodeBlock {
        return CodeBlock.of(
            when (this) {
                TINY_INT, TINY_INT_BOOL, SMALL_INT, INTEGER, BIG_INT, BIT -> "$cursorName.getLong($columnIndex)"
                DATE -> "$cursorName.getDate($columnIndex)"
                TIME -> "$cursorName.getDuration($columnIndex)"
                DATETIME, TIMESTAMP -> "$cursorName.getDateTime($columnIndex)"
//                NUMERIC -> "$cursorName.getBigDecimal($columnIndex)" // TODO?
            },
            javaType,
        )
    }
}