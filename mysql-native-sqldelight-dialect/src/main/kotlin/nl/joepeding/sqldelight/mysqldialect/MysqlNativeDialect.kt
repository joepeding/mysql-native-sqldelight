package nl.joepeding.sqldelight.mysqldialect

import app.cash.sqldelight.dialect.api.*
import app.cash.sqldelight.dialects.mysql.MySqlTypeResolver
import app.cash.sqldelight.dialects.mysql.grammar.MySqlParserUtil
import app.cash.sqldelight.dialects.mysql.grammar.psi.MySqlExtensionExpr
import app.cash.sqldelight.dialects.mysql.grammar.psi.MySqlTypeName
import com.alecstrong.sql.psi.core.SqlParserUtil
import com.alecstrong.sql.psi.core.psi.*
import com.intellij.icons.AllIcons
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.TokenSet
import com.intellij.psi.util.PsiTreeUtil
import com.squareup.kotlinpoet.*

class MysqlNativeDialect : SqlDelightDialect { // TODO: Delegate to MySqlDialect?
    override val runtimeTypes: RuntimeTypes = RuntimeTypes(
        ClassName("nl.joepeding.sqldelight.mysqldriver", "MySQLCursor"),
        ClassName("nl.joepeding.sqldelight.mysqldriver", "MySQLPreparedStatement"),
    )

    override val asyncRuntimeTypes: RuntimeTypes
        get() = error("Async native driver is not yet supported")

    override val icon = AllIcons.Providers.Mysql

    // TODO
//    override val connectionManager: ConnectionManager by lazy { MySqlConnectionManager() }
    override val connectionManager: ConnectionManager by lazy { TODO() }

    override fun setup() {
        SqlParserUtil.reset()
        MySqlParserUtil.reset()
        MySqlParserUtil.overrideSqlParser()

        //TODO
        val currentElementCreation = MySqlParserUtil.createElement
//        MySqlParserUtil.createElement = {
//            when (it.elementType) {
//                SqlTypes.COLUMN_DEF -> ColumnDefMixin(it)
//                SqlTypes.BINARY_EQUALITY_EXPR -> MySqlBinaryEqualityExpr(it)
//                else -> currentElementCreation(it)
//            }
//        }
    }

    override fun typeResolver(parentResolver: TypeResolver): TypeResolver = MySQLNativeTypeResolver(parentResolver)

    //TODO
//    override fun migrationSquasher(parentSquasher: MigrationSquasher): MigrationSquasher {
//        return MySqlMigrationSquasher(parentSquasher)
//    }
    override fun migrationSquasher(parentSquasher: MigrationSquasher) = TODO()
}

private class MySQLNativeTypeResolver(val parentResolver: TypeResolver) : TypeResolver by MySqlTypeResolver(parentResolver) {
    override fun resolvedType(expr: SqlExpr): IntermediateType {
        return when (expr) {
            is MySqlExtensionExpr -> encapsulatingType(
                PsiTreeUtil.findChildrenOfType(expr.ifExpr, SqlExpr::class.java).drop(1),
                NativeMySqlType.TINY_INT,
                NativeMySqlType.SMALL_INT,
                NativeMySqlType.INTEGER,
                PrimitiveType.INTEGER,
                NativeMySqlType.BIG_INT,
                PrimitiveType.REAL,
                NativeMySqlType.TIMESTAMP,
                NativeMySqlType.DATE,
                NativeMySqlType.DATETIME,
                NativeMySqlType.TIME,
                PrimitiveType.TEXT,
                PrimitiveType.BLOB,
            )
            is SqlBinaryExpr -> {
                if (expr.childOfType(
                        TokenSet.create(
//                            SqlTypes.EQ, SqlTypes.EQ2, SqlTypes.NEQ,
//                            SqlTypes.NEQ2, SqlTypes.AND, SqlTypes.OR, SqlTypes.GT, SqlTypes.GTE,
//                            SqlTypes.LT, SqlTypes.LTE,
                        ),
                    ) != null
                ) {
                    IntermediateType(PrimitiveType.BOOLEAN)
                } else {
                    encapsulatingType(
                        exprList = expr.getExprList(),
                        nullableIfAny = (expr is SqlBinaryAddExpr || expr is SqlBinaryMultExpr || expr is SqlBinaryPipeExpr),
                        NativeMySqlType.TINY_INT,
                        NativeMySqlType.SMALL_INT,
                        NativeMySqlType.INTEGER,
                        PrimitiveType.INTEGER,
                        NativeMySqlType.BIG_INT,
                        PrimitiveType.REAL,
                        PrimitiveType.TEXT,
                        PrimitiveType.BLOB,
                    )
                }
            }
            else -> parentResolver.resolvedType(expr)
        }
    }

    override fun argumentType(parent: PsiElement, argument: SqlExpr): IntermediateType {
        when (parent) {
            is MySqlExtensionExpr -> {
                return if (argument == parent.ifExpr?.children?.first()) {
                    IntermediateType(PrimitiveType.BOOLEAN)
                } else {
                    IntermediateType(PrimitiveType.ARGUMENT)
                }
            }
        }
        return parentResolver.argumentType(parent, argument)
    }

    override fun functionType(functionExpr: SqlFunctionExpr): IntermediateType? {
        return functionExpr.mySqlFunctionType() ?: parentResolver.functionType(functionExpr)
    }

    private fun SqlFunctionExpr.mySqlFunctionType() = when (functionName.text.lowercase()) {
        "greatest" -> encapsulatingType(exprList,
            PrimitiveType.INTEGER,
            PrimitiveType.REAL,
            PrimitiveType.TEXT,
            PrimitiveType.BLOB
        )
        "concat" -> encapsulatingType(exprList, PrimitiveType.TEXT)
        "last_insert_id" -> IntermediateType(PrimitiveType.INTEGER)
        "row_count" -> IntermediateType(PrimitiveType.INTEGER)
        "microsecond", "second", "minute", "hour", "day", "week", "month", "year" -> IntermediateType(
            PrimitiveType.INTEGER,
        )
        "sin", "cos", "tan" -> IntermediateType(PrimitiveType.REAL)
        "coalesce", "ifnull" -> encapsulatingType(exprList,
            NativeMySqlType.TINY_INT,
            NativeMySqlType.SMALL_INT, NativeMySqlType.INTEGER,
            PrimitiveType.INTEGER,
            NativeMySqlType.BIG_INT,
            PrimitiveType.REAL,
            PrimitiveType.TEXT,
            PrimitiveType.BLOB
        )
        "max" -> encapsulatingType(
            exprList,
            NativeMySqlType.TINY_INT,
            NativeMySqlType.SMALL_INT,
            NativeMySqlType.INTEGER,
            PrimitiveType.INTEGER,
            NativeMySqlType.BIG_INT,
            PrimitiveType.REAL,
            NativeMySqlType.TIMESTAMP,
            NativeMySqlType.DATE,
            NativeMySqlType.DATETIME,
            NativeMySqlType.TIME,
            PrimitiveType.TEXT,
            PrimitiveType.BLOB,
        ).asNullable()
        "min" -> encapsulatingType(
            exprList,
            PrimitiveType.BLOB,
            PrimitiveType.TEXT,
            NativeMySqlType.TIME,
            NativeMySqlType.DATETIME,
            NativeMySqlType.DATE,
            NativeMySqlType.TIMESTAMP,
            NativeMySqlType.TINY_INT,
            NativeMySqlType.SMALL_INT,
            PrimitiveType.INTEGER,
            NativeMySqlType.INTEGER,
            NativeMySqlType.BIG_INT,
            PrimitiveType.REAL,
        ).asNullable()
        "sum" -> {
            val type = resolvedType(exprList.single())
            if (type.dialectType == PrimitiveType.REAL) {
                IntermediateType(PrimitiveType.REAL).asNullable()
            } else {
                IntermediateType(PrimitiveType.INTEGER).asNullable()
            }
        }
        "unix_timestamp" -> IntermediateType(PrimitiveType.TEXT)
        "to_seconds" -> IntermediateType(PrimitiveType.INTEGER)
        "json_arrayagg" -> IntermediateType(PrimitiveType.TEXT)
        "date_add", "date_sub" -> IntermediateType(PrimitiveType.TEXT)
        "now" -> IntermediateType(PrimitiveType.TEXT)
        "char_length", "character_length" -> IntermediateType(PrimitiveType.INTEGER).nullableIf(resolvedType(exprList[0]).javaType.isNullable)
        else -> null
    }

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
//    NUMERIC(ClassName("java.math", "BigDecimal")),
    DATE(ClassName("kotlinx.datetime", "LocalDate")),
    TIME(ClassName("kotlin.time", "Duration")), // TODO: Check
    TIMESTAMP(ClassName("kotlinx.datetime", "LocalDateTime")), // TODO: Check
    DATETIME(ClassName("kotlinx.datetime", "Instant")), // TODO: Check
    ;

    override fun prepareStatementBinder(columnIndex: CodeBlock, value: CodeBlock): CodeBlock {
        return CodeBlock.builder()
            .add(
                when (this) {
                    TINY_INT, TINY_INT_BOOL, SMALL_INT, INTEGER, BIG_INT, BIT -> "bindLong"
                    DATE -> "bindDate"
                    TIME -> "bindDuration"
                    DATETIME, TIMESTAMP -> "bindDateTime"
//                    NUMERIC -> "bindBigDecimal"
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
//                NUMERIC -> "$cursorName.getBigDecimal($columnIndex)"
            },
            javaType,
        )
    }
}

private fun PsiElement.childOfType(types: TokenSet): PsiElement? {
    return node.findChildByType(types)?.psi
}