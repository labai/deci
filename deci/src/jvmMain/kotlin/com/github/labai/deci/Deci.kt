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

import com.github.labai.deci.impl.TinyUDecMath
import com.github.labai.deci.impl.TinyUDecMath.ERR
import com.github.labai.deci.impl.TinyUDecMath.TinyUDec
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

    constructor(decimal: BigDecimal, deciContext: DeciContext) : this(decimal, null, null, null, deciContext)

    private constructor(decimal: BigDecimal?, tinyDec: TinyUDec? = null, str: String? = null, long: Long? = null, deciCtx: DeciContext = defaultDeciContext) : super() {
        this.deciContext = deciCtx
        if (decimal != null) {
            this.decimal = applyDeciContext(decimal, deciCtx)
        } else if (tinyDec != null) {
            if (tinyDec.pos() <= deciCtx.scale) {
                this.decimal = null
                this.tinyDec = tinyDec
            } else {
                this.decimal = applyDeciContext(tinyDec.toBigDecimal(), deciCtx)
            }
        } else if (str != null) {
            val tiny = TinyUDecMath.parseStringOrErr(str)
            if (tiny != ERR && tiny.pos() <= deciCtx.scale) {
                this.decimal = null
                this.tinyDec = tiny
            } else {
                this.decimal = applyDeciContext(BigDecimal(str), deciCtx)
            }
        } else if (long != null) {
            val tiny = TinyUDecMath.convertToTinyOrErr(long)
            if (tiny != ERR) {
                this.decimal = null
                this.tinyDec = tiny
            } else {
                this.decimal = BigDecimal(long)
            }
        } else {
            error("Missing constructor parameter")
        }
    }
    constructor(decimal: BigDecimal) : this(decimal, defaultDeciContext)

    actual constructor(str: String, deciContext: DeciContext) : this(decimal = null, str = str, deciCtx = deciContext)
    actual constructor(str: String) : this(decimal = null, str = str)
    actual constructor(int: Int, deciContext: DeciContext) : this(decimal = null, long = int.toLong(), deciCtx = deciContext)
    actual constructor(int: Int) : this(decimal = null, long = int.toLong())
    actual constructor(long: Long, deciContext: DeciContext) : this(decimal = null, long = long, deciCtx = deciContext)
    actual constructor(long: Long) : this(decimal = null, long = long)

    private var decimal: BigDecimal?

    private var tinyDec: TinyUDec = ERR

    actual operator fun unaryMinus(): Deci = Deci(toBigDecimal().negate(), deciContext)

    actual override fun toByte(): Byte = decimal?.toByte() ?: tinyDec.intPart().toByte()

    actual override fun toDouble(): Double = toBigDecimal().toDouble()
    actual override fun toFloat(): Float = toBigDecimal().toFloat()
    actual override fun toInt(): Int = decimal?.toInt() ?: tinyDec.intPart()
    actual override fun toLong(): Long = decimal?.toLong() ?: tinyDec.intPart().toLong()
    actual override fun toShort(): Short = decimal?.toShort() ?: tinyDec.intPart().toShort()

    actual fun applyDeciContext(deciContext: DeciContext): Deci {
        return if (this.deciContext == deciContext) this else Deci(this.decimal, this.tinyDec, deciCtx = deciContext)
    }

    /** round to n decimals. Unlike BigDecimal.round(), here parameter 'scale' means scale, not precision */
    actual infix fun round(scale: Int): Deci {
        return if (this.tinyDec != ERR) {
            Deci(null, tinyDec = tinyDec.round(scale, deciContext.roundingMode))
        } else {
            Deci(this.asDecimal().setScale(scale, deciContext.javaRoundingMode))
        }
    }

    actual override fun compareTo(other: Deci): Int {
        if (tinyDec != ERR && other.tinyDec != ERR) {
            return tinyDec.compareTo(other.tinyDec)
        }
        if (decimal != null && other.decimal != null) {
            return decimal!!.compareTo(other.decimal)
        }
        val a: BigDecimal
        val b: TinyUDec
        if (tinyDec == ERR && other.tinyDec != ERR) {
            a = this.decimal!!
            b = other.tinyDec
        } else if (tinyDec != ERR) {
            a = other.decimal!!
            b = tinyDec
        } else {
            error("Illegal state")
        }
        val at = TinyUDecMath.convertToTinyOrErr(a)
        if (at != ERR)
            return at.compareTo(b)
        return a.compareTo(b.toBigDecimal())
    }

    actual override fun toString(): String {
        if (tinyDec != ERR)
            return tinyDec.toString()
        return decimal!!.stripTrailingZeros().toPlainString()
    }

    actual override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Deci
        if (tinyDec != ERR && other.tinyDec != ERR) {
            return tinyDec.isEqual(other.tinyDec)
        }
        return compareTo(other) == 0
    }

    actual override fun hashCode(): Int {
        if (tinyDec != ERR)
            return tinyDec.hashCode()
        val dec = normalizeDecimal()
        val tiny = TinyUDecMath.convertToTinyOrErr(dec)
        if (tiny != ERR)
            return tiny.hashCode()
        return dec.hashCode()
    }

    internal fun calcDivScale(divisor: BigDecimal): Int {
        val dec = this.decimal!!
        val thisIntDigits = if (dec.signum() == 0) 1 else dec.precision() - dec.scale()
        val divisorIntDigits = if (divisor.signum() == 0) 1 else divisor.precision() - divisor.scale()
        if (divisorIntDigits < 0)
            return max(dec.scale(), deciContext.scale) // dividing will increase result
        return max(deciContext.scale, deciContext.precision + divisorIntDigits - thisIntDigits)
    }

    private fun applyDeciContext(dec: BigDecimal, deciCtx: DeciContext): BigDecimal {
        return when {
            dec.scale() < 0 -> dec.setScale(0, deciCtx.javaRoundingMode)
            dec.scale() > deciCtx.scale -> {
                val zeros = max(0, dec.scale() - dec.precision())
                val scale = max(deciCtx.scale, min(zeros + deciCtx.precision, dec.scale()))
                dec.setScale(scale, deciCtx.javaRoundingMode)
            }
            else -> dec
        }
    }

    internal fun plusInternal(other: Deci): Deci {
        return doOperation(other, { a, b -> a.add(b) }, { a, b -> a.add(b) })
    }
    internal fun minusInternal(other: Deci): Deci {
        return doOperation(other, { a, b -> a.sub(b) }, { a, b -> a.subtract(b) })
    }
    internal fun timesInternal(other: Deci): Deci {
        return doOperation(other, { a, b -> a.mul(b) }, { a, b -> a.multiply(b) })
    }
    internal fun divInternal(other: Deci): Deci {
        return doOperation(other, { a, b -> a.tryDiv(b) }, { a, b -> a.divide(b, calcDivScale(b), deciContext.javaRoundingMode) })
    }
    internal fun remInternal(other: Deci): Deci {
        return doOperation(other, { a, b -> a.rem(b) }, { a, b -> a.rem(b) })
    }

    private inline fun doOperation(other: Deci, tinyOp: (a: TinyUDec, b: TinyUDec) -> TinyUDec, bdecOp: (a: BigDecimal, b: BigDecimal) -> BigDecimal): Deci {
        if (tinyDec != ERR && other.tinyDec != ERR) {
            val rtin = tinyOp(tinyDec, other.tinyDec)
            if (rtin != ERR)
                return Deci(null, rtin, deciCtx = deciContext)
        }
        val rdec = bdecOp(toBigDecimal(), other.asDecimal())
        return Deci(rdec, deciContext)
    }

    fun toBigDecimal(): BigDecimal {
        if (decimal != null)
            return decimal!!
        val d = TinyUDecMath.toBigDecimal(tinyDec)
        decimal = d
        return d
    }

    private fun normalizeDecimal(): BigDecimal {
        val dec = if (decimal != null) {
            decimal!!
        } else {
            check(tinyDec != ERR) { "Deci is invalid" }
            TinyUDecMath.toBigDecimal(tinyDec)
        }
        val decn = dec.stripTrailingZeros()
        decimal = decn
        return decn
    }

    private inline fun asDecimal(): BigDecimal {
        return decimal ?: TinyUDecMath.toBigDecimal(tinyDec)
    }


    actual companion object {
        internal actual val originalDefaultDeciContext = DeciContext(20, RoundingMode.HALF_UP, 20)
        actual var defaultDeciContext = originalDefaultDeciContext

        actual val ZERO = Deci(0L).apply { toBigDecimal() }
        // few popular numbers
        private val D1 = Deci(1L).apply { toBigDecimal() }
        private val D2 = Deci(2L).apply { toBigDecimal() }
        private val D10 = Deci(10L).apply { toBigDecimal() }
        private val D100 = Deci(100L).apply { toBigDecimal() }
        private val D1000 = Deci(1000L).apply { toBigDecimal() }

        actual fun valueOf(int: Int): Deci {
            return when (int) {
                0 -> ZERO
                in 1..1000 -> {
                    when (int) {
                        1 -> D1
                        2 -> D2
                        10 -> D10
                        100 -> D100
                        1000 -> D1000
                        else -> Deci(int)
                    }
                }
                else -> Deci(int)
            }
        }

        actual fun valueOf(long: Long): Deci {
            return when (long) {
                0L -> ZERO
                in 1L..1000L -> Deci(long.toInt())
                else -> Deci(long)
            }
        }
    }
}

operator fun Deci.plus(other: BigDecimal): Deci = this.plusInternal(other.deci)
operator fun Deci.minus(other: BigDecimal): Deci = this.minusInternal(other.deci)
operator fun Deci.times(other: BigDecimal): Deci = this.timesInternal(other.deci)
operator fun Deci.div(other: BigDecimal): Deci = this.divInternal(other.deci)
operator fun Deci.rem(other: BigDecimal): Deci = this.remInternal(other.deci)

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
        else -> Deci(num.toString())
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
        else -> Deci(num.toString(), deciContext)
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
