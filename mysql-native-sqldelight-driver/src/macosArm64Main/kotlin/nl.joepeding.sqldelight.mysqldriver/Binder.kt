package nl.joepeding.sqldelight.mysqldriver

import kotlinx.cinterop.*
import mysql.MYSQL_BIND
import mysql.MYSQL_FIELD

actual class Binder actual constructor(val memScope: Arena, val numFields: Int) {
    private var bindings: CArrayPointer<MYSQL_BIND>
    private var lengths: CArrayPointer<CPointerVar<ULongVar>>
    private var nulls: CArrayPointer<CPointerVar<BooleanVar>>

    init {
        bindings = memScope.allocArray(numFields)
        nulls = memScope.allocArray(numFields)
        lengths = memScope.allocArray(numFields)
    }

    actual fun get(index: Int): MYSQL_BIND = bindings[index]

    actual fun bind(index: Int, field: MYSQL_FIELD, buffer: CVariable) {
        val length = memScope.alloc<ULongVar>().ptr.also { lengths[index] = it }
        val isNull = memScope.alloc<BooleanVar>().ptr.also { nulls[index] = it }
        bindings[index].apply {
            this.buffer_type = field.type
            this.buffer = buffer.ptr
            this.buffer_length = field.max_length
            this.length = length
            this.is_null = isNull
        }
    }

    actual fun getBindings(): CValuesRef<MYSQL_BIND>? = bindings

    actual fun getLength(index: Int) = lengths[index]!!.pointed.value.toInt()

    actual fun isNull(index: Int) = nulls[index]!!.reinterpret<ByteVar>().pointed.value == true.toByte()
}