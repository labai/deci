/*
MIT License

Copyright (c) 2020 Augustus

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/
package com.github.labai.deci

import com.github.labai.deci.impl.Utils

/*
 * @author Augustus
 * created on 2025-12-21
 *
 * JS version of Deci
*/
actual class Deci : Number, Comparable<Deci> {
    internal actual val deciContext: DeciContext
    internal val decimal: DecimalJs
    private var _hashCode: Int? = null

    internal constructor(decimal: DecimalJs, deciContext: DeciContext = defaultDeciContext) {
        this.decimal = decimal
        this.deciContext = deciContext
    }

    actual constructor(str: String, deciContext: DeciContext) {
        this.decimal = DecimalJsFactory.createDecimalJs(str, deciContext)
        this.deciContext = deciContext
    }
    actual constructor(str: String) : this(str, defaultDeciContext)
    actual constructor(int: Int, deciContext: DeciContext) : this(int.toString(), deciContext)
    actual constructor(int: Int) : this(int.toString(), defaultDeciContext)
    actual constructor(long: Long, deciContext: DeciContext) : this(long.toString(), deciContext)
    actual constructor(long: Long) : this(long.toString(), defaultDeciContext)

    actual operator fun unaryMinus(): Deci = Deci(decimal.negated(), deciContext)

    actual override fun toByte(): Byte = decimal.toNumber().toInt().toByte()
    actual override fun toDouble(): Double = decimal.toNumber()
    actual override fun toFloat(): Float = decimal.toNumber().toFloat()
    actual override fun toInt(): Int = decimal.toNumber().toInt()
    actual override fun toLong(): Long = decimal.toNumber().toLong()
    actual override fun toShort(): Short = decimal.toNumber().toInt().toShort()

    actual fun applyDeciContext(deciContext: DeciContext): Deci = if (this.deciContext == deciContext) this else Deci(this.decimal, deciContext)

    actual infix fun round(scale: Int): Deci = Utils.round(decimal, scale, deciContext)

    actual override fun compareTo(other: Deci): Int {
        return decimal.comparedTo(other.decimal)
    }

    actual override fun toString(): String {
        val scale = Utils.calcScale(decimal, deciContext)
        val d = decimal.toDecimalPlaces(scale, deciContext.jsRoundingMode)
        return d.toString()
    }

    actual override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Deci) return false
        return decimal.comparedTo(other.decimal) == 0
    }

    actual override fun hashCode(): Int {
        if (_hashCode == null)
            _hashCode = decimal.toString().hashCode()
        return _hashCode!!
    }

    internal fun plusInternal(other: Deci): Deci = plusInternal(other.decimal)
    internal fun minusInternal(other: Deci): Deci = minusInternal(other.decimal)
    internal fun timesInternal(other: Deci): Deci = timesInternal(other.decimal)
    internal fun divInternal(other: Deci): Deci = divInternal(other.decimal)
    internal fun remInternal(other: Deci): Deci = remInternal(other.decimal)

    internal fun plusInternal(other: DecimalJs): Deci {
        return Utils.toRoundedDeci(decimal.add(other), deciContext)
    }
    internal fun minusInternal(other: DecimalJs): Deci {
        return Utils.toRoundedDeci(decimal.sub(other), deciContext)
    }
    internal fun timesInternal(other: DecimalJs): Deci {
        val e1 = decimal.exponentNum
        val e2 = other.exponentNum
        val eres = e1 + e2 + 1
        val dec = if (eres >= decimal.precision) {
            val decCon = DecimalJsFactory.getForPrecision(eres)
            decCon(decimal.toString())
        } else {
            decimal
        }
        val d: DecimalJs = dec.mul(other)
        return Utils.toRoundedDeci(d, deciContext)
    }
    internal fun divInternal(other: DecimalJs): Deci {
        val e1 = decimal.exponentNum
        val e2 = other.exponentNum
        val eres = e1 - e2 + 1
        val dec = if (eres >= decimal.precision) {
            val decCon = DecimalJsFactory.getForPrecision(eres)
            decCon(decimal.toString())
        } else {
            decimal
        }
        val d: DecimalJs = dec.dividedBy(other)
        return Utils.toRoundedDeci(d, deciContext)
    }
    internal fun remInternal(other: DecimalJs): Deci = Deci(decimal.modulo(other), deciContext)

    actual companion object {
        internal actual val originalDefaultDeciContext: DeciContext = DeciContext.of(20, RoundingMode.HALF_UP, 20)
        actual var defaultDeciContext: DeciContext = originalDefaultDeciContext
        actual val ZERO: Deci = Deci("0", defaultDeciContext)

        actual fun valueOf(int: Int): Deci {
            return if (int == 0) ZERO else Deci(int)
        }

        actual fun valueOf(long: Long): Deci {
            return if (long == 0L) ZERO else Deci(long)
        }
    }

    fun toDecimalJs(): DecimalJs = this.decimal
}

fun Deci?.toDecimalJs(): DecimalJs? = this?.decimal

actual operator fun Deci.plus(other: Deci): Deci = this.plusInternal(other)
actual operator fun Deci.minus(other: Deci): Deci = this.minusInternal(other)
actual operator fun Deci.times(other: Deci): Deci = this.timesInternal(other)
actual operator fun Deci.div(other: Deci): Deci = this.divInternal(other)
actual operator fun Deci.rem(other: Deci): Deci = this.remInternal(other)

actual operator fun Deci.plus(other: Int): Deci = this.plusInternal(other.deci)
actual operator fun Deci.minus(other: Int): Deci = this.minusInternal(other.deci)
actual operator fun Deci.times(other: Int): Deci = this.timesInternal(other.deci)
actual operator fun Deci.div(other: Int): Deci = this.divInternal(other.deci)
actual operator fun Deci.rem(other: Int): Deci = this.remInternal(other.deci)

actual operator fun Deci.plus(other: Long): Deci = this.plusInternal(other.deci)
actual operator fun Deci.minus(other: Long): Deci = this.minusInternal(other.deci)
actual operator fun Deci.times(other: Long): Deci = this.timesInternal(other.deci)
actual operator fun Deci.div(other: Long): Deci = this.divInternal(other.deci)
actual operator fun Deci.rem(other: Long): Deci = this.remInternal(other.deci)

//
// additional Deci methods
//
actual fun Deci.Companion.valueOf(num: Number): Deci {
    return when (num) {
        is Deci -> num
        is Int -> valueOf(num)
        is Long -> valueOf(num)
        is Double -> Deci(num.toString())
        is Float -> Deci(num.toString())
        is Short -> valueOf(num.toInt())
        is Byte -> valueOf(num.toInt())
        else -> Deci(num.toString())
    }
}

actual fun Deci.Companion.valueOf(str: String): Deci = Deci(str)

actual fun Deci.Companion.valueOf(num: Number, deciContext: DeciContext): Deci {
    return when (num) {
        is Deci -> if (deciContext == num.deciContext) num else Deci(num.decimal, deciContext)
        else -> Deci(num.toString(), deciContext)
    }
}

actual fun Deci.Companion.valueOf(str: String, deciContext: DeciContext): Deci = Deci(str, deciContext)

actual operator fun Deci.compareTo(other: Number): Int {
    return when (other) {
        is Deci -> compareTo(other)
        is Int -> compareTo(other.deci)
        is Long -> compareTo(other.deci)
        is Double -> compareTo(other.toString().deci)
        is Float -> compareTo(other.toString().deci)
        is Short -> compareTo(other.toInt().deci)
        is Byte -> compareTo(other.toInt().deci)
        else -> this.compareTo(Deci(other.toString()))
    }
}
