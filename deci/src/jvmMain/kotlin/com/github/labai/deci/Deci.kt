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

import java.math.BigDecimal
import kotlin.math.max
import kotlin.math.min

/*
 * @author Augustus
 *   created on 2020.11.18
 *
 * JVM version of Deci
*/
actual class Deci : Number, Comparable<Deci> {

    internal actual val deciContext: DeciContext

    constructor(decimal: BigDecimal, deciContext: DeciContext) : super() {
        this.deciContext = deciContext
        this.decimal = when {
            decimal.scale() < 0 -> decimal.setScale(0, deciContext.javaRoundingMode)
            decimal.scale() > deciContext.scale -> {
                val zeros = max(0, decimal.scale() - decimal.precision())
                val scale = max(deciContext.scale, min(zeros + deciContext.precision, decimal.scale()))
                decimal.setScale(scale, deciContext.javaRoundingMode)
            }

            else -> decimal
        }
    }
    constructor(decimal: BigDecimal) : this(decimal, defaultDeciContext)

    actual constructor(str: String, deciContext: DeciContext) : this(BigDecimal(str), deciContext)
    actual constructor(str: String) : this(BigDecimal(str))
    actual constructor(int: Int, deciContext: DeciContext) : this(BigDecimal(int), deciContext)
    actual constructor(int: Int) : this(BigDecimal(int))
    actual constructor(long: Long, deciContext: DeciContext) : this(long.toBigDecimal(), deciContext)
    actual constructor(long: Long) : this(BigDecimal(long))

    private val decimal: BigDecimal
    private var _hashCode: Int? = null

    actual operator fun unaryMinus(): Deci = Deci(decimal.negate(), deciContext)

    actual override fun toByte(): Byte = decimal.toByte()

    actual override fun toDouble(): Double = decimal.toDouble()
    actual override fun toFloat(): Float = decimal.toFloat()
    actual override fun toInt(): Int = decimal.toInt()
    actual override fun toLong(): Long = decimal.toLong()
    actual override fun toShort(): Short = decimal.toShort()

    fun toBigDecimal(): BigDecimal = decimal

    actual fun applyDeciContext(deciContext: DeciContext): Deci = if (this.deciContext == deciContext) this else Deci(this.decimal, deciContext)

    /** round to n decimals. Unlike BigDecimal.round(), here parameter 'scale' means scale, not precision */
    actual infix fun round(scale: Int): Deci = Deci(this.decimal.setScale(scale, deciContext.javaRoundingMode))

    actual override fun compareTo(other: Deci): Int = decimal.compareTo(other.decimal)

    actual override fun toString(): String = decimal.stripTrailingZeros().toPlainString()

    actual override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Deci
        return decimal.compareTo(other.decimal) == 0
    }

    actual override fun hashCode(): Int {
        if (_hashCode == null)
            _hashCode = decimal.stripTrailingZeros().hashCode()
        return _hashCode!!
    }

    internal fun calcDivScale(divisor: BigDecimal): Int {
        val thisIntDigits = if (decimal.signum() == 0) 1 else decimal.precision() - decimal.scale()
        val divisorIntDigits = if (divisor.signum() == 0) 1 else divisor.precision() - divisor.scale()
        if (divisorIntDigits < 0)
            return max(decimal.scale(), deciContext.scale) // dividing will increase result
        return max(deciContext.scale, deciContext.precision + divisorIntDigits - thisIntDigits)
    }

    // for internal usage - explicitly call to avoid recursive loops by mistake
    internal fun plusInternal(other: BigDecimal): Deci = Deci(decimal.add(other), deciContext)
    internal fun minusInternal(other: BigDecimal): Deci = Deci(decimal.subtract(other), deciContext)
    internal fun timesInternal(other: BigDecimal): Deci = Deci(decimal.multiply(other), deciContext)
    internal fun divInternal(other: BigDecimal): Deci = Deci(decimal.divide(other, calcDivScale(other), deciContext.javaRoundingMode), deciContext)
    internal fun remInternal(other: BigDecimal): Deci = Deci(decimal.remainder(other), deciContext)

    actual companion object {
        internal actual val originalDefaultDeciContext = DeciContext(20, RoundingMode.HALF_UP, 20)
        actual var defaultDeciContext = originalDefaultDeciContext

        actual val ZERO = Deci(0L)

        actual fun valueOf(int: Int): Deci {
            return when (int) {
                0 -> ZERO
                in 1..10 -> Deci(int.toLong()) // reuse cached bigDecimal
                else -> Deci(int)
            }
        }

        actual fun valueOf(long: Long): Deci {
            return if (long == 0L) ZERO else Deci(long)
        }
    }
}

operator fun Deci.plus(other: BigDecimal): Deci = this.plusInternal(other)
operator fun Deci.minus(other: BigDecimal): Deci = this.minusInternal(other)
operator fun Deci.times(other: BigDecimal): Deci = this.timesInternal(other)
operator fun Deci.div(other: BigDecimal): Deci = this.divInternal(other)
operator fun Deci.rem(other: BigDecimal): Deci = this.remInternal(other)

actual operator fun Deci.plus(other: Deci): Deci = this.plusInternal(other.toBigDecimal())
actual operator fun Deci.minus(other: Deci): Deci = this.minusInternal(other.toBigDecimal())
actual operator fun Deci.times(other: Deci): Deci = this.timesInternal(other.toBigDecimal())
actual operator fun Deci.div(other: Deci): Deci = this.divInternal(other.toBigDecimal())
actual operator fun Deci.rem(other: Deci): Deci = this.remInternal(other.toBigDecimal())

actual operator fun Deci.plus(other: Int): Deci = this.plusInternal(other.toBigDecimal())
actual operator fun Deci.minus(other: Int): Deci = this.minusInternal(other.toBigDecimal())
actual operator fun Deci.times(other: Int): Deci = this.timesInternal(other.toBigDecimal())
actual operator fun Deci.div(other: Int): Deci = this.divInternal(other.toBigDecimal())
actual operator fun Deci.rem(other: Int): Deci = this.remInternal(other.toBigDecimal())

actual operator fun Deci.plus(other: Long): Deci = this.plusInternal(other.toBigDecimal())
actual operator fun Deci.minus(other: Long): Deci = this.minusInternal(other.toBigDecimal())
actual operator fun Deci.times(other: Long): Deci = this.timesInternal(other.toBigDecimal())
actual operator fun Deci.div(other: Long): Deci = this.divInternal(other.toBigDecimal())
actual operator fun Deci.rem(other: Long): Deci = this.remInternal(other.toBigDecimal())

fun Deci?.toBigDecimal(): BigDecimal? = this?.toBigDecimal()

//
// BigDecimal extensions
//
val BigDecimal.deci: Deci
    inline get() = Deci(this)

infix fun BigDecimal.eq(other: Deci) = this.compareTo(other.toBigDecimal()) == 0

//
// additional Deci methods
//
actual fun Deci.Companion.valueOf(num: Number): Deci {
    return when (num) {
        is Deci -> num
        is BigDecimal -> Deci(num)
        is Int -> valueOf(num)
        is Long -> valueOf(num)
        is Double -> Deci(BigDecimal.valueOf(num))
        is Float -> Deci(BigDecimal.valueOf(num.toDouble()))
        is Short -> valueOf(num.toInt())
        is Byte -> valueOf(num.toInt())
        else -> Deci(BigDecimal(num.toString()))
    }
}

actual fun Deci.Companion.valueOf(str: String): Deci = Deci(str.toBigDecimal())

actual fun Deci.Companion.valueOf(num: Number, deciContext: DeciContext): Deci {
    return when (num) {
        is Deci -> if (deciContext == num.deciContext) num else Deci(num.toBigDecimal(), deciContext)
        is BigDecimal -> Deci(num, deciContext)
        is Int -> Deci(num.toLong().toBigDecimal(), deciContext)
        is Long -> Deci(num.toBigDecimal(), deciContext)
        is Double -> Deci(BigDecimal.valueOf(num), deciContext)
        is Float -> Deci(BigDecimal.valueOf(num.toDouble()), deciContext)
        is Short -> Deci(num.toLong().toBigDecimal(), deciContext)
        is Byte -> Deci(num.toLong().toBigDecimal(), deciContext)
        else -> Deci(BigDecimal(num.toString()), deciContext)
    }
}

actual fun Deci.Companion.valueOf(str: String, deciContext: DeciContext): Deci = Deci(str.toBigDecimal(), deciContext)

actual operator fun Deci.compareTo(other: Number): Int {
    return when (other) {
        is Deci -> compareTo(other as Deci)
        is BigDecimal -> compareTo(other.deci)
        is Int -> compareTo(other.deci)
        is Long -> compareTo(BigDecimal(other).deci)
        is Double -> compareTo(BigDecimal(other).deci)
        is Float -> compareTo(BigDecimal(other.toDouble()).deci)
        is Short -> compareTo(BigDecimal(other.toInt()).deci)
        is Byte -> compareTo(BigDecimal(other.toInt()).deci)
        else -> this.compareTo(Deci(other.toString()))
    }
}
