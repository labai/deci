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

import com.github.labai.deci.DeciContextImpl.Companion.MASK_11BITS
import com.github.labai.deci.DeciContextImpl.Companion.MASK_25BITS
import com.github.labai.deci.DeciContextImpl.Companion.MASK_3BITS
import com.github.labai.deci.impl.TinyUDecMath
import com.github.labai.deci.impl.TinyUDecMath.ERR
import com.github.labai.deci.impl.TinyUDecMath.TinyUDec
import java.math.BigDecimal
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min

/*
 * @author Augustus
 *   created on 2020.11.18
 *
 * JVM version of Deci
 *
 * here we try to fit into minimum amount of memory.
 * For small decimals we use TinyUDec, which fits into 32 bits
 * And only when TinyUDec isn't enough, then use BigDecimal.
 *
 * For DeciContext also use part of integer (25 bits), other few bits are used as flags in Deci,
 *
 *
 *
*/
actual class Deci : Number, Comparable<Deci>, DeciContext {

    internal actual val deciContext: DeciContext
        inline get() = this

    private var decimal: BigDecimal? = null
    private var tinyDec: TinyUDec = ERR
    private var mixed: Int = 0

    constructor(decimal: BigDecimal, deciContext: DeciContext) : this(decimal, null, false, null, null, null, deciContext)

    private constructor(
        decimal: BigDecimal?,
        tinyDec: TinyUDec? = null,
        tinyNegative: Boolean = false,
        str: String? = null,
        long: Long? = null,
        int: Int? = null,
        deciCtx: DeciContext? = null,
    ) : super() {
        val deciCtx = deciCtx ?: defaultDeciContext
        this.setDeciContext(deciCtx)
        if (decimal != null) {
            this.decimal = applyDeciContext(decimal, deciCtx)
        } else if (tinyDec != null) {
            if (tinyDec.pos() <= deciCtx.scale) {
                this.decimal = null
                this.tinyDec = tinyDec
                if (tinyNegative)
                    setFlagOn(FLAG_NEGATIVE)
            } else {
                val bd = if (tinyNegative) tinyDec.toBigDecimal().negate() else tinyDec.toBigDecimal()
                this.decimal = applyDeciContext(bd, deciCtx)
                setFlagOn(FLAG_TINY_INIT)
            }
        } else if (str != null) {
            val isNeg = str.startsWith('-')
            val absStr = if (isNeg) str.substring(1) else str
            val tiny = TinyUDecMath.parseStringOrErr(absStr)
            if (tiny != ERR && tiny.pos() <= deciCtx.scale) {
                this.decimal = null
                this.tinyDec = tiny
                if (isNeg)
                    setFlagOn(FLAG_NEGATIVE)
                setFlagOn(FLAG_TINY_TRIM)
            } else {
                this.decimal = applyDeciContext(BigDecimal(str), deciCtx)
                setFlagOn(FLAG_TINY_INIT)
            }
        } else if (long != null) {
            val tiny = TinyUDecMath.convertToTinyOrErr(long.absoluteValue)
            if (tiny != ERR) {
                this.decimal = null
                this.tinyDec = tiny
                if (long < 0)
                    setFlagOn(FLAG_NEGATIVE)
            } else {
                this.decimal = BigDecimal.valueOf(long)
                setFlagOn(FLAG_TINY_INIT)
            }
        } else if (int != null) {
            val tiny = TinyUDecMath.convertToTinyOrErr(int.absoluteValue)
            if (tiny != ERR) {
                this.decimal = null
                this.tinyDec = tiny
                if (int < 0)
                    setFlagOn(FLAG_NEGATIVE)
            } else {
                this.decimal = BigDecimal(int)
                setFlagOn(FLAG_TINY_INIT)
            }
        } else {
            error("Missing constructor parameter")
        }
    }

    constructor(decimal: BigDecimal) : this(decimal, defaultDeciContext)

    actual constructor(str: String, deciContext: DeciContext) : this(decimal = null, str = str, deciCtx = deciContext)
    actual constructor(str: String) : this(decimal = null, str = str)
    actual constructor(int: Int, deciContext: DeciContext) : this(decimal = null, int = int, deciCtx = deciContext)
    actual constructor(int: Int) : this(decimal = null, int = int)
    actual constructor(long: Long, deciContext: DeciContext) : this(decimal = null, long = long, deciCtx = deciContext)
    actual constructor(long: Long) : this(decimal = null, long = long)

    actual operator fun unaryMinus(): Deci {
        if (tinyDec != ERR) {
            if (tinyDec.isZero())
                return this
            return Deci(null, tinyDec = tinyDec, tinyNegative = !isNegative(), deciCtx = deciContext)
        }
        return Deci(decimal!!.negate(), deciContext)
    }

    actual override fun toByte(): Byte = decimal?.toByte() ?: (if (isNegative()) -tinyDec.intPart() else tinyDec.intPart()).toByte()

    actual override fun toDouble(): Double = toBigDec().toDouble()
    actual override fun toFloat(): Float = toBigDec().toFloat()
    actual override fun toInt(): Int = decimal?.toInt() ?: (if (isNegative()) -tinyDec.intPart() else tinyDec.intPart())
    actual override fun toLong(): Long = decimal?.toLong() ?: (if (isNegative()) -tinyDec.intPart().toLong() else tinyDec.intPart().toLong())
    actual override fun toShort(): Short = decimal?.toShort() ?: (if (isNegative()) -tinyDec.intPart() else tinyDec.intPart()).toShort()

    actual fun applyDeciContext(deciContext: DeciContext): Deci {
        return if (this.deciContext == deciContext) this else Deci(this.decimal, this.tinyDec, deciCtx = deciContext)
    }

    /** round to n decimals. Unlike BigDecimal.round(), here parameter 'scale' means scale, not precision */
    actual infix fun round(scale: Int): Deci {
        return if (this.tinyDec != ERR) {
            Deci(null, tinyDec = tinyDec.round(scale, deciContext.roundingMode), deciCtx = deciContext)
        } else {
            val dec = this.asDecimal().setScale(scale, deciContext.javaRoundingMode)
            Deci(dec, deciContext)
        }
    }

    actual override fun compareTo(other: Deci): Int {
        if (tinyDec != ERR && other.tinyDec != ERR) {
            val aneg = isNegative()
            val bneg = other.isNegative()
            return when {
                aneg && !bneg -> -1
                !aneg && bneg -> 1
                aneg -> other.tinyDec.compareTo(tinyDec) // both negative: reverse magnitude order
                else -> tinyDec.compareTo(other.tinyDec)
            }
        }
        return toBigDec().compareTo(other.toBigDec())
    }

    actual override fun toString(): String {
        if (tinyDec != ERR)
            return if (isNegative()) "-${tinyDec}" else tinyDec.toString()
        return decimal!!.stripTrailingZeros().toPlainString()
    }

    actual override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Deci
        if (tinyDec != ERR && other.tinyDec != ERR) {
            if (isNegative() != other.isNegative()) return false
            return tinyDec.isEqual(other.tinyDec)
        }
        return compareTo(other) == 0
    }

    actual override fun hashCode(): Int {
        tryInitTinyDec()
        if (tinyDec != ERR) {
            val h = tinyDec.hashCode()
            return if (isNegative()) h.inv() else h
        }
        normalizeDecimal()
        return decimal!!.hashCode()
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
        if (tinyDec != ERR && other.tinyDec != ERR) {
            val r = addTinySigned(other)
            if (r != null)
                return r
        }
        return Deci(toBigDec().add(other.toBigDec()), deciContext)
    }

    internal fun minusInternal(other: Deci): Deci {
        if (tinyDec != ERR && other.tinyDec != ERR) {
            // subtract = add with sign of other flipped
            val r = addTinySigned(other, negateOther = true)
            if (r != null)
                return r
        }
        return Deci(toBigDec().subtract(other.toBigDec()), deciContext)
    }

    internal fun timesInternal(other: Deci): Deci {
        if (tinyDec != ERR && other.tinyDec != ERR) {
            val r = mulDivTinySigned(other) { a, b -> a.mul(b) }
            if (r != null)
                return r
        }
        return Deci(toBigDec().multiply(other.toBigDec()), deciContext)
    }

    internal fun divInternal(other: Deci): Deci {
        if (tinyDec != ERR && other.tinyDec != ERR) {
            val r = mulDivTinySigned(other) { a, b -> a.tryDiv(b) }
            if (r != null)
                return r
        }
        val a = toBigDec()
        val b = other.toBigDec()
        val dec = a.divide(b, calcDivScale(b), deciContext.javaRoundingMode)
        return Deci(dec, deciContext)
    }

    internal fun remInternal(other: Deci): Deci {
        if (tinyDec != ERR && other.tinyDec != ERR) {
            val r = remTinySigned(other)
            if (r != null)
                return r
        }
        return Deci(this.toBigDec().rem(other.toBigDec()), deciContext)
    }

    // Signed addition of two tinyDec values, optionally negating the other operand (for subtraction).
    // Returns null if the result overflows TinyUDec range — caller falls back to BigDecimal.
    private fun addTinySigned(other: Deci, negateOther: Boolean = false): Deci? {
        val aneg = isNegative()
        val bneg = other.isNegative() xor negateOther
        if (aneg == bneg) {
            // same sign: magnitudes add, sign is preserved
            val r = this.tinyDec.add(other.tinyDec)
            if (r == ERR) return null
            return Deci(null, tinyDec = r, tinyNegative = aneg, deciCtx = deciContext)
        }
        // different signs: subtract smaller magnitude from larger, sign follows the larger
        if (this.tinyDec > other.tinyDec) {
            val r = this.tinyDec.sub(other.tinyDec)
            if (r == ERR) return null
            return Deci(null, tinyDec = r, tinyNegative = aneg, deciCtx = deciContext)
        }
        val r = other.tinyDec.sub(this.tinyDec)
        if (r == ERR) return null
        return Deci(null, tinyDec = r, tinyNegative = bneg, deciCtx = deciContext)
    }

    // Signed multiplication/division of two tinyDec values.
    // Returns null if the result overflows TinyUDec range — caller falls back to BigDecimal.
    private inline fun mulDivTinySigned(other: Deci, mulDivFn: (TinyUDec, TinyUDec) -> TinyUDec): Deci? {
        val r = mulDivFn(this.tinyDec, other.tinyDec)
        if (r == ERR) return null
        val resultNeg = (isNegative() xor other.isNegative()) && !r.isZero()
        return Deci(null, tinyDec = r, tinyNegative = resultNeg, deciCtx = deciContext)
    }

    // Signed remainder of two tinyDec values.
    // Returns null if the result overflows TinyUDec range — caller falls back to BigDecimal.
    private fun remTinySigned(other: Deci): Deci? {
        val r = this.tinyDec.rem(other.tinyDec)
        if (r == ERR) return null
        val resultNeg = isNegative() && !r.isZero() // take form 1st operand
        return Deci(null, tinyDec = r, tinyNegative = resultNeg, deciCtx = deciContext)
    }

    internal fun toBigDec(): BigDecimal {
        if (decimal != null)
            return decimal!!
        val tiny = normalizeTinyDec()
        val d = TinyUDecMath.toBigDecimal(tiny).let { if (isNegative()) it.negate() else it }
        setFlagOn(FLAG_BIGD_TRIM)
        decimal = d
        return d
    }

    private fun normalizeDecimal(): BigDecimal {
        if (isFlag(FLAG_BIGD_TRIM))
            return decimal!!
        val dec = if (decimal != null) {
            decimal!!
        } else {
            check(tinyDec != ERR) { "Deci is invalid" }
            TinyUDecMath.toBigDecimal(tinyDec)
        }
        val decn = dec.stripTrailingZeros()
        decimal = decn
        setFlagOn(FLAG_BIGD_TRIM)
        return decn
    }

    private inline fun asDecimal(): BigDecimal {
        return decimal ?: TinyUDecMath.toBigDecimal(tinyDec).let { if (isNegative()) it.negate() else it }
    }

    // sometimes we want to have tinyDec even we already have decimal.
    // also normalize tiny (if valid)
    private fun tryInitTinyDec() {
        if (!isFlag(FLAG_TINY_INIT)) {
            val dec = normalizeDecimal()
            tinyDec = TinyUDec.valueOf(dec)
            setFlagOn(FLAG_TINY_TRIM)
            setFlagOn(FLAG_TINY_INIT)
        } else {
            normalizeTinyDec()
        }
    }

    private fun normalizeTinyDec(): TinyUDec {
        if (tinyDec == ERR)
            return ERR
        if (isFlag(FLAG_TINY_TRIM))
            return tinyDec
        val dn = tinyDec.trimTrailingZeros()
        this.tinyDec = dn
        setFlagOn(FLAG_TINY_TRIM)
        return dn
    }

    // ----------------------------------------------------------------
    // implement DeciContext using "mixed" value and share it with boolean flags for Deci
    // so we can fit into single 32 bit integer
    override val scale: Int
        get() = mixed and MASK_11BITS
    override val roundingMode: RoundingMode
        get() = RoundingMode.entries[(mixed ushr 22) and MASK_3BITS]
    override val precision: Int
        get() = (mixed shr 11) and MASK_11BITS

    private fun setDeciContextValue(ctxVal: Int) {
        mixed = (mixed and DeciContextImpl.MASK_25BITS_INV) or (ctxVal and MASK_25BITS)
    }

    private fun setDeciContext(deciCtx: DeciContext) {
        val mixed: Int = when (deciCtx) {
            is Deci -> deciCtx.mixed
            is DeciContextImpl -> deciCtx.mixed
            else -> DeciContextImpl.convDeciCtxValue(deciCtx)
        }
        setDeciContextValue(mixed)
    }

    private inline fun isFlag(flag: Int) = mixed and flag != 0
    private fun setFlagOn(flag: Int) {
        mixed = mixed or flag
    }

    private inline fun isNegative() = isFlag(FLAG_NEGATIVE)
    // ----------------------------------------------------------------

    actual companion object {
        internal actual val originalDefaultDeciContext: DeciContext = DeciContextImpl(20, RoundingMode.HALF_UP, 20)
        private val defaultDeciContextValue = (originalDefaultDeciContext as DeciContextImpl).mixed
        actual var defaultDeciContext: DeciContext = originalDefaultDeciContext

        // few popular numbers
        actual val ZERO = Deci(0L).apply { toBigDec() }
        private val D1 = Deci(1L).apply { toBigDec() }
        private val D2 = Deci(2L).apply { toBigDec() }
        private val D10 = Deci(10L).apply { toBigDec() }
        private val D100 = Deci(100L).apply { toBigDec() }
        private val D1000 = Deci(1000L).apply { toBigDec() }

        private const val FLAG_TINY_INIT: Int = 1 shl 26 // have tried to init tinyDec (even if failed)
        private const val FLAG_TINY_TRIM: Int = 1 shl 27
        private const val FLAG_BIGD_TRIM: Int = 1 shl 28
        private const val FLAG_NEGATIVE: Int = 1 shl 29

        actual fun valueOf(int: Int): Deci {
            return when (int) {
                in 0..1000 -> {
                    when (int) {
                        0 -> ZERO
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
                in 0L..1000L -> {
                    when (long) {
                        0L -> ZERO
                        1L -> D1
                        2L -> D2
                        10L -> D10
                        100L -> D100
                        1000L -> D1000
                        else -> Deci(long)
                    }
                }
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

fun Deci?.toBigDecimal(): BigDecimal? = this?.toBigDec()

//
// BigDecimal extensions
//
val BigDecimal.deci: Deci
    inline get() = Deci(this)

infix fun BigDecimal.eq(other: Deci) = this.compareTo(other.toBigDec()) == 0

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

actual fun Deci.Companion.valueOf(str: String): Deci = Deci(str)

actual fun Deci.Companion.valueOf(num: Number, deciContext: DeciContext): Deci {
    return when (num) {
        is Deci -> if (deciContext == num.deciContext) num else Deci(num.toBigDec(), deciContext)
        is BigDecimal -> Deci(num, deciContext)
        is Int -> Deci(int = num, deciContext)
        is Long -> Deci(long = num, deciContext)
        is Double -> Deci(BigDecimal.valueOf(num), deciContext)
        is Float -> Deci(BigDecimal.valueOf(num.toDouble()), deciContext)
        is Short -> Deci(int = num.toInt(), deciContext)
        is Byte -> Deci(int = num.toInt(), deciContext)
        else -> Deci(num.toString(), deciContext)
    }
}

actual fun Deci.Companion.valueOf(str: String, deciContext: DeciContext): Deci = Deci(str, deciContext)

actual operator fun Deci.compareTo(other: Number): Int {
    return when (other) {
        is Deci -> compareTo(other)
        is BigDecimal -> compareTo(other.deci)
        is Int -> compareTo(other.deci)
        is Long -> compareTo(other.deci)
        is Double -> compareTo(BigDecimal.valueOf(other).deci)
        is Float -> compareTo(BigDecimal.valueOf(other.toDouble()).deci)
        is Short -> compareTo(other.toInt().deci)
        is Byte -> compareTo(other.toInt().deci)
        else -> this.compareTo(Deci(other.toString()))
    }
}
