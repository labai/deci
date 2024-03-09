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

import com.github.labai.deci.Deci.DeciContext
import java.io.Serializable
import java.math.BigDecimal
import java.math.RoundingMode
import java.math.RoundingMode.HALF_UP
import kotlin.math.max
import kotlin.math.min

/*
 * @author Augustus
 *   com.github.labai
 *   created on 2020.11.18
 *
 * Latest version https://github.com/labai/deci/tree/main/deci
 *
 * wrapped BigDecimal with features:
 *  - use HALF_UP rounding
 *  - division result with high scale (20+)
 *  - math operators with BigDecimal, Int, Long
 *  - equal ('==') ignores scale (uses compareTo)
 *  - scale and rounding mode can be set on first element of formula
 *
 * E.g.
 *  val d1: Deci = (price * quantity - fee) * 100 / (price * quantity) round 2
 *  val d2: BigDecimal = ((1.deci - 1.deci / 365) * (1.deci - 2.deci / 365) round 11).toBigDecimal()
 *
 * Additional infix functions
 *   round - round number by provided number of decimal, return Deci
 *   eq - comparison between numbers (various types, including null)
 *
 * DeciContext
 *
 * In case default scale and rounding (20 and round_up) is not suitable, it is possible to use own setup.
 * When creating Deci, provide additional parameter - DeciContext (similar, but different to MathContext).
 *
 * It has such fields:
 * - scale - indicates, how many digits need to keep after dot
 * - precision - indicates, how many significant digits to keep, when number is small and scale is not enough
 * - roundingMode - rounding mode (java.math.RoundingMode)
 *
 * Example:
 *   DeciContext(scale = 4, roundingMode = HALF_UP, precision = 3)
 *   - means to keep 4 numbers after dot, but not less than 3 significant number, e.g.:
 *      123.1234 - number big enough, keep 4 digits after dot
 *      0.000123 - number is smaller and 4 digits after dot is not enough - keep minimum 3 significant digits
 *
 * Default is
 *   DeciContext(20, HALF_UP, 20)
 *
 */
class Deci @JvmOverloads constructor(decimal: BigDecimal, internal val deciContext: DeciContext = defaultDeciContext) : Number(), Comparable<Deci> {

    constructor(str: String) : this(BigDecimal(str))
    constructor(int: Int) : this(BigDecimal(int))
    constructor(long: Long) : this(long.toBigDecimal())

    data class DeciContext(val scale: Int, val roundingMode: RoundingMode, val precision: Int) : Serializable {
        constructor(scale: Int, roundingMode: RoundingMode = HALF_UP) : this(scale, roundingMode, scale)

        init {
            check(scale >= 0) { "scale must be >= 0 (is $scale)" }
            check(scale <= 2000) { "scale must be <= 2000 (is $scale)" }
            check(precision >= 1) { "precision must be >= 1 (is $precision)" }
            check(precision <= 2000) { "precision must be <= 2000 (is $precision)" }
        }

        override fun toString(): String = "DeciContext($scale:$precision:${roundingMode.toString().lowercase()})"
    }

    private val decimal: BigDecimal = when {
        decimal.scale() < 0 -> decimal.setScale(0, deciContext.roundingMode)
        decimal.scale() > deciContext.scale -> {
            val zeros = max(0, decimal.scale() - decimal.precision())
            val scale = max(deciContext.scale, min(zeros + deciContext.precision, decimal.scale()))
            decimal.setScale(scale, deciContext.roundingMode)
        }

        else -> decimal
    }
    private var _hashCode: Int? = null

    operator fun unaryMinus(): Deci = Deci(decimal.negate(), deciContext)

    override fun toByte(): Byte = decimal.toByte()

    @Deprecated(message = "Deprecated since kotlin 1.9")
    override fun toChar(): Char = decimal.toInt().toChar()
    override fun toDouble(): Double = decimal.toDouble()
    override fun toFloat(): Float = decimal.toFloat()
    override fun toInt(): Int = decimal.toInt()
    override fun toLong(): Long = decimal.toLong()
    override fun toShort(): Short = decimal.toShort()

    fun toBigDecimal(): BigDecimal = decimal

    fun applyDeciContext(deciContext: DeciContext): Deci = if (this.deciContext == deciContext) this else Deci(this.decimal, deciContext)

    /** round to n decimals. Unlike BigDecimal.round(), here parameter 'scale' means scale, not precision */
    infix fun round(scale: Int): Deci = Deci(this.decimal.setScale(scale, deciContext.roundingMode))

    override fun compareTo(other: Deci): Int = decimal.compareTo(other.decimal)

    override fun toString(): String = decimal.stripTrailingZeros().toPlainString()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Deci
        return decimal.compareTo(other.decimal) == 0
    }

    override fun hashCode(): Int {
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
    internal fun divInternal(other: BigDecimal): Deci = Deci(decimal.divide(other, calcDivScale(other), deciContext.roundingMode), deciContext)
    internal fun remInternal(other: BigDecimal): Deci = Deci(decimal.remainder(other), deciContext)

    companion object {
        internal val defaultDeciContext = DeciContext(20, HALF_UP, 20)

        private val d0 = Deci(0L)

        fun valueOf(int: Int): Deci {
            return when (int) {
                0 -> d0
                in 1..10 -> Deci(int.toLong()) // reuse cached bigDecimal
                else -> Deci(int)
            }
        }

        fun valueOf(long: Long): Deci {
            return if (long == 0L) d0 else Deci(long)
        }
    }
}

operator fun Deci.plus(other: BigDecimal): Deci = this.plusInternal(other)
operator fun Deci.minus(other: BigDecimal): Deci = this.minusInternal(other)
operator fun Deci.times(other: BigDecimal): Deci = this.timesInternal(other)
operator fun Deci.div(other: BigDecimal): Deci = this.divInternal(other)
operator fun Deci.rem(other: BigDecimal): Deci = this.remInternal(other)

operator fun Deci.plus(other: Deci): Deci = this.plusInternal(other.toBigDecimal())
operator fun Deci.minus(other: Deci): Deci = this.minusInternal(other.toBigDecimal())
operator fun Deci.times(other: Deci): Deci = this.timesInternal(other.toBigDecimal())
operator fun Deci.div(other: Deci): Deci = this.divInternal(other.toBigDecimal())
operator fun Deci.rem(other: Deci): Deci = this.remInternal(other.toBigDecimal())

operator fun Deci.plus(other: Int): Deci = this.plusInternal(other.toBigDecimal())
operator fun Deci.minus(other: Int): Deci = this.minusInternal(other.toBigDecimal())
operator fun Deci.times(other: Int): Deci = this.timesInternal(other.toBigDecimal())
operator fun Deci.div(other: Int): Deci = this.divInternal(other.toBigDecimal())
operator fun Deci.rem(other: Int): Deci = this.remInternal(other.toBigDecimal())

operator fun Deci.plus(other: Long): Deci = this.plusInternal(other.toBigDecimal())
operator fun Deci.minus(other: Long): Deci = this.minusInternal(other.toBigDecimal())
operator fun Deci.times(other: Long): Deci = this.timesInternal(other.toBigDecimal())
operator fun Deci.div(other: Long): Deci = this.divInternal(other.toBigDecimal())
operator fun Deci.rem(other: Long): Deci = this.remInternal(other.toBigDecimal())

infix fun Deci?.round(scale: Int): Deci? = this?.round(scale)
infix fun Deci?.eq(other: Deci?): Boolean = if (this == null || other == null) this == other else this.compareTo(other) == 0
infix fun Deci?.eq(other: BigDecimal?): Boolean =
    if (this == null || other == null) (this == null && other == null) else this.toBigDecimal().compareTo(other) == 0

infix fun Deci?.eq(other: Number?): Boolean = if (this == null || other == null) (this == null && other == null) else this.compareTo(other) == 0

fun Deci?.toBigDecimal(): BigDecimal? = this?.toBigDecimal()

//
// BigDecimal extensions
//
val BigDecimal.deci: Deci
    inline get() = Deci(this)

infix fun BigDecimal.eq(other: Deci) = this.compareTo(other.toBigDecimal()) == 0

//
// Int extensions
//
val Int.deci: Deci
    inline get() = Deci.valueOf(this)

//
// Long extensions
//
val Long.deci: Deci
    inline get() = Deci.valueOf(this)

//
// String extensions
//
val String.deci: Deci
    inline get() = Deci(this)

//
// additional Deci methods
//
fun Deci.Companion.valueOf(num: Number): Deci {
    return when (num) {
        is Deci -> num
        is BigDecimal -> Deci(num)
        is Int -> valueOf(num as Int)
        is Long -> valueOf(num as Long)
        is Double -> Deci(BigDecimal.valueOf(num))
        is Float -> Deci(BigDecimal.valueOf(num.toDouble()))
        is Short -> valueOf(num.toInt() as Int)
        is Byte -> valueOf(num.toInt() as Int)
        else -> Deci(BigDecimal(num.toString()))
    }
}

fun Deci.Companion.valueOf(str: String): Deci = Deci(str.toBigDecimal())

fun Deci.Companion.valueOf(num: Number, deciContext: DeciContext): Deci {
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

fun Deci.Companion.valueOf(str: String, deciContext: DeciContext): Deci = Deci(str.toBigDecimal(), deciContext)

operator fun Deci.compareTo(other: Number): Int {
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

// null to zero - useful in formulas, reduces an expression '(nullableValue ?: 0.deci)' to 'nullableValue.orZero()'
fun Deci?.orZero(): Deci = this ?: 0.deci

//
// Iterable extensions
//
@JvmName("sumOfDeci")
inline fun <T> Iterable<T>.sumOf(selector: (T) -> Deci): Deci {
    var sum: Deci = 0.deci
    for (element in this) {
        sum += selector(element)
    }
    return sum
}
