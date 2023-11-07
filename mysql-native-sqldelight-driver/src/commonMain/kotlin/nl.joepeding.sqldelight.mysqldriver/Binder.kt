package nl.joepeding.sqldelight.mysqldriver

import kotlinx.cinterop.Arena
import kotlinx.cinterop.CValuesRef
import kotlinx.cinterop.CVariable
import mysql.MYSQL_BIND
import mysql.MYSQL_FIELD

expect class Binder(memScope: Arena, numFields: Int) {
    fun get(index: Int): MYSQL_BIND
    fun bind(index: Int, field: MYSQL_FIELD, buffer: CVariable)
    fun getBindings(): CValuesRef<MYSQL_BIND>?
    fun getLength(index: Int): Int
    fun isNull(index: Int): Boolean
}
