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

import com.github.labai.deci.Deci.Companion
import com.github.labai.deci.DeciContextImpl.Companion.MASK_11BITS
import com.github.labai.deci.DeciContextImpl.Companion.MASK_25BITS
import com.github.labai.deci.DeciContextImpl.Companion.MASK_3BITS
import com.github.labai.deci.impl.BigDecimalUtils
import com.github.labai.deci.impl.TinyDec
import com.github.labai.deci.impl.TinyDec.Companion.ERR
import com.github.labai.deci.impl.TinyDec4d
import com.github.labai.deci.impl.TinyUDecMath
import com.github.labai.deci.impl.TinyUDecMath.MAX_INT_LEN
import com.github.labai.deci.impl.TinyUDecMath.MAX_UNSCALED
import com.github.labai.deci.impl.TinyUDecMath.TwoInt
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min
import java.math.RoundingMode as JavaRoundingMode

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
 * In total Deci contains
 * - 12 bytes - Deci object header itself (depends on jvm)
 * - 4 bytes - Int for "tinyDec"
 * - 4 bytes - Int for "mixed"
 * - 4 bytes - reference to BigDecimal
 *
 * In total 24 bytes if BigDecimal is not used.
 *
 * In case BigDecimal is used, it will consume additional
 * 36-96+ bytes for BigDecimal instance (can be more, depends on size of the number)
 *
 * For internal calculation here we use primitives, with no intermediate objects
 * (following "zero-garbage computations" idea)
 *
*/
actual class Deci : Number, Comparable<Deci> {

    internal actual val deciContext: DeciContext
        get() = if (mixed.isDeciCtxEqual(defaultDeciContext)) defaultDeciContext else mixed.getAsDeciContext()

    private var decimal: BigDecimal? = null
    // 'tinyDec' may contain TinyDec (0..3 decimals) or TinyDec4d (4..7 decimals). The flag FLAG_TINY_DEC4 indicates which one is used
    private var tinyDec: TinyDec = ERR
    // 'mixed' contains: a) deciContext (25 bits); b) flags
    private var mixed: CtxMixed  = MIXED_NOTINIT

    @JvmSynthetic
    internal inline fun getMixed() = mixed

    private inline fun getTinyPos() = if (isFlag(FLAG_TINY_DEC4)) tinyDec.pos() + 4 else tinyDec.pos()
    private inline fun getTinyUnscaled() = tinyDec.unscaled()

    // BigDecimal
    constructor(decimal: BigDecimal, deciContext: DeciContext) {
        this.initDeciContext(deciContext)
        this.decimal = applyDeciContext(decimal, deciContext)
    }
    constructor(decimal: BigDecimal) : this(decimal, defaultDeciContext)
    private constructor(decimal: BigDecimal, ctxMix: CtxMixed) {
        this.initDeciContext(ctxMix)
        this.decimal = applyDeciContext(decimal, ctxMix)
    }

    // String
    actual constructor(str: String, deciContext: DeciContext) {
        this.initDeciContext(deciContext)
        val isNeg = str.startsWith('-', false)
        val len = if (isNeg) str.length - 1 else str.length
        if (len <= 9) {
            val pair = TinyUDecMath.parseString(str, MAX_INT_LEN, TinyDec4d.maxPos, true)
            val pos = pair.second()
            if (!pair.isErr() && pos <= deciContext.scale) {
                assignTiny(pair.first(), pos, isNeg)
                setFlagOn(FLAG_TINY_TRIM)
                return
            }
        }
        var bd: BigDecimal? = null
        if (len <= 33 && useCustomBigDecimalParser)
            bd = BigDecimalUtils.parseString(str)
        if (bd == null)
            bd = BigDecimal(str)
        this.decimal = applyDeciContext(bd, deciContext)
        setFlagOn(FLAG_TINY_INIT)

    }
    actual constructor(str: String) : this(str, defaultDeciContext)

    // CharArray
    constructor(chars: CharArray, offset: Int, length: Int, deciCtx: DeciContext) {
        this.initDeciContext(deciCtx)
        chars[offset]
        val isNeg = (chars[offset] == '-')
        val len = if (isNeg) length - 1 else length
        if (len <= 9) {
            val pair = TinyUDecMath.parseCharArray(chars, offset, length, MAX_INT_LEN, TinyDec4d.maxPos, true)
            val pos = pair.second()
            if (!pair.isErr() && pos <= deciCtx.scale) {
                assignTiny(pair.first(), pos, isNeg)
                setFlagOn(FLAG_TINY_TRIM)
                return
            }
        }
        var bd: BigDecimal? = null
        if (len <= 33 && useCustomBigDecimalParser)
            bd = BigDecimalUtils.parseCharArray(chars, offset, length)
        if (bd == null)
            bd = BigDecimal(chars, offset, length)
        this.decimal = applyDeciContext(bd, deciCtx)
        setFlagOn(FLAG_TINY_INIT)
    }
    constructor(chars: CharArray, offset: Int, length: Int) : this(chars, offset, length, defaultDeciContext)

    private fun assignTiny(unscaled: Int, pos: Int, isNeg: Boolean) {
        when (pos) {
            in 0..3 -> {
                this.tinyDec = TinyDec.buildTinyOrErr(unscaled, pos)
            }
            in 4..7 -> {
                val d4d = TinyDec4d.buildTiny4dOrErr(unscaled, pos)
                this.tinyDec = TinyDec(d4d.raw)
                this.setFlagOn(FLAG_TINY_DEC4)
            }
            else -> {
                error("Invalid decimal count")
            }
        }
        if (isNeg)
            setFlagOn(FLAG_NEGATIVE)
    }

    // Int
    actual constructor(int: Int, deciContext: DeciContext) {
        this.initDeciContext(deciContext)
        val tiny = TinyDec.buildTinyOrErr(int.absoluteValue, 0)
        if (tiny.isValid()) {
            this.decimal = null
            this.tinyDec = tiny
            if (int < 0)
                setFlagOn(FLAG_NEGATIVE)
        } else {
            this.decimal = BigDecimal(int)
            setFlagOn(FLAG_TINY_INIT)
        }
    }
    actual constructor(int: Int) : this(int, defaultDeciContext)

    // Long
    actual constructor(long: Long, deciContext: DeciContext) {
        this.initDeciContext(deciContext)
        val absVal = long.absoluteValue // nb. abs(Long.MIN_VALUE) -> Long.MIN_VALUE
        val tiny = if (absVal !in 0..MAX_UNSCALED) ERR else TinyDec.buildTinyOrErr(long.absoluteValue.toInt(), 0)
        if (tiny.isValid()) {
            this.decimal = null
            this.tinyDec = tiny
            if (long < 0)
                setFlagOn(FLAG_NEGATIVE)
        } else {
            this.decimal = BigDecimal.valueOf(long)
            setFlagOn(FLAG_TINY_INIT)
        }
    }
    actual constructor(long: Long) : this(long, defaultDeciContext)

    // TinyDec
    private constructor(tinyDec: TinyDec, tinyNegative: Boolean, ctxMix: CtxMixed) {
        this.initDeciContext(ctxMix)

        if (tinyDec.pos() <= ctxMix.scale()) {
            this.decimal = null
            this.tinyDec = tinyDec
            if (tinyNegative) {
                setFlagOn(FLAG_NEGATIVE)
            }
        } else {
            var unscaled = tinyDec.unscaled()
            if (tinyNegative)
                unscaled = -unscaled
            val bd = BigDecimal.valueOf(unscaled.toLong(), tinyDec.pos())
            this.decimal = applyDeciContext(bd, ctxMix)
            setFlagOn(FLAG_TINY_INIT)
        }
    }

    // TinyDec4
    private constructor(tiny4: TinyDec4d, tinyNegative: Boolean, ctxMix: CtxMixed, markForTiny4Arg: Boolean) {
        this.initDeciContext(ctxMix)
        if (tiny4.pos() <= ctxMix.scale()) {
            this.decimal = null
            this.tinyDec = TinyDec(tiny4.raw)
            setFlagOn(FLAG_TINY_DEC4)
            if (tinyNegative)
                setFlagOn(FLAG_NEGATIVE)
        } else {
            var unscaled = tiny4.unscaled()
            if (tinyNegative)
                unscaled = -unscaled
            val bd = BigDecimal.valueOf(unscaled.toLong(), tiny4.pos())
            this.decimal = applyDeciContext(bd, ctxMix)
            setFlagOn(FLAG_TINY_INIT)
        }
    }

    // if to use here "operator fun plus()", it conflicts with deciExpr - expression takes this one, and ignores deciExpr context,
    // but if to make as extension function, then it is not visible in Java.
    // So,
    // these functions are for Java, and for kotlin operator functions - there are extension ones
    @JvmName("plus")
    fun plusInternal(other: Deci): Deci = this.plusInternal(other, mixed)
    @JvmName("minus")
    fun minusInternal(other: Deci): Deci = this.minusInternal(other, mixed)
    @JvmName("times")
    fun timesInternal(other: Deci): Deci = this.timesInternal(other, mixed)
    @JvmName("div")
    fun divInternal(other: Deci): Deci = this.divInternal(other, mixed)
    @JvmName("rem")
    fun remInternal(other: Deci): Deci = this.remInternal(other, mixed)

    actual operator fun unaryMinus(): Deci {
        if (tinyDec.isValid()) {
            if (tinyDec.isZero())
                return this
            return Deci(tinyDec, !isNegative(), mixed)
        }
        return Deci(decimal!!.negate(), mixed)
    }

    actual override fun toByte(): Byte {
        return toInt().toByte()
    }

    actual override fun toDouble(): Double {
        return toBigDecimal().toDouble()
    }
    actual override fun toFloat(): Float {
        return toBigDecimal().toFloat()
    }
    actual override fun toInt(): Int {
        return decimal?.toInt()
            ?: TinyUDecMath.getIntPart(getTinyUnscaled(), getTinyPos())
                .let { (if (isNegative()) -it else it) }
    }
    actual override fun toLong(): Long {
        return decimal?.toLong() ?: toInt().toLong()
    }
    actual override fun toShort(): Short {
        return toInt().toShort()
    }

    actual fun applyDeciContext(deciContext: DeciContext): Deci {
        return if (this.mixed.isDeciCtxEqual(deciContext)) this else Deci(asDecimal(), deciContext = deciContext)
    }

    /** round to n decimals. Unlike BigDecimal.round(), here parameter 'scale' means scale, not precision */
    actual infix fun round(scale: Int): Deci {
        val roundMode = mixed.roundingMode()
        if (this.tinyDec.isValid()) {
            val isDec4 = isFlag(FLAG_TINY_DEC4)
            val isNeg = isFlag(FLAG_NEGATIVE)
            if (!isDec4 && scale in 0..3) {
                if (scale >= tinyDec.pos())
                    return this
                val pair = TinyUDecMath.round(getTinyUnscaled(), getTinyPos(), scale, roundMode)
                return createFromTwoInt(pair, isNeg, mixed) ?: error("Can't round, invalid number")
            }
            if (isDec4) {
                val tiny4Pos = getTinyPos()
                if (scale >= tiny4Pos)
                    return this
                val pair = TinyUDecMath.round(getTinyUnscaled(), tiny4Pos, scale, roundMode)
                val res = createFromTwoInt(pair, isNeg, mixed)
                if (res != null)
                    return res
            }
        }
        val bdec = this.asDecimal()
        if (bdec.scale() <= scale)
            return this
        val dec = bdec.setScale(scale, roundMode.toJava())
        return Deci(dec, mixed)
    }

    actual override fun compareTo(other: Deci): Int {
        if (tinyDec.isValid() && other.tinyDec.isValid()) {
            val aneg = isNegative()
            val bneg = other.isNegative()
            when {
                aneg && !bneg -> return -1
                !aneg && bneg -> return 1
            }

            val aval = getTinyUnscaled()
            val apos = getTinyPos()
            val bval = other.getTinyUnscaled()
            val bpos = other.getTinyPos()

            return when {
                aneg -> TinyUDecMath.compare(bval, bpos, aval, apos) // both negative: reverse magnitude order
                else -> TinyUDecMath.compare(aval, apos, bval, bpos)
            }
        }
        return toBigDecimal().compareTo(other.toBigDecimal())
    }

    actual override fun toString(): String {
        if (tinyDec.isValid()) {
            val unscaled = getTinyUnscaled()
            val pos = getTinyPos()
            return if (isNegative()) "-${TinyUDecMath.toString(unscaled, pos)}" else TinyUDecMath.toString(unscaled, pos)
        }
        return normalizeDecimal()
            .toPlainString()
    }

    actual override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Deci
        return compareTo(other) == 0
    }

    actual override fun hashCode(): Int {
        tryInitTinyDec()
        if (tinyDec.isValid()) {
            val h = tinyDec.hashCode()
            return if (isNegative()) h.inv() else h
        }
        return normalizeDecimal()
            .hashCode()
    }

    private fun calcDivScale(divisor: BigDecimal): Int {
        val ctxScale = mixed.scale()
        val dec = this.decimal!!
        val thisIntDigits = if (dec.signum() == 0) 1 else dec.precision() - dec.scale()
        val divisorIntDigits = if (divisor.signum() == 0) 1 else divisor.precision() - divisor.scale()
        if (divisorIntDigits < 0)
            return max(dec.scale(), ctxScale) // dividing will increase result
        val ctxPrecision = mixed.precision()
        return max(ctxScale, ctxPrecision + divisorIntDigits - thisIntDigits)
    }

    private fun applyDeciContext(dec: BigDecimal, ctxMix: CtxMixed): BigDecimal {
        val ctxScale = ctxMix.scale()
        return when {
            dec.scale() > ctxScale -> {
                val zeros = max(0, dec.scale() - dec.precision())
                val scale = max(ctxScale, min(zeros + ctxMix.precision(), dec.scale()))
                dec.setScale(scale, ctxMix.javaRoundingMode())
            }
            dec.scale() < 0 -> dec.setScale(0, ctxMix.javaRoundingMode())
            else -> dec
        }
    }

    private fun applyDeciContext(dec: BigDecimal, deciCtx: DeciContext): BigDecimal {
        return when {
            dec.scale() > deciCtx.scale -> {
                val zeros = max(0, dec.scale() - dec.precision())
                val scale = max(deciCtx.scale, min(zeros + deciCtx.precision, dec.scale()))
                dec.setScale(scale, deciCtx.javaRoundingMode)
            }
            dec.scale() < 0 -> dec.setScale(0, deciCtx.javaRoundingMode)
            else -> dec
        }
    }

    @JvmSynthetic
    internal fun plusInternal(other: Deci, ctxMix: CtxMixed): Deci {
        if (tinyDec.isValid() && other.tinyDec.isValid()) {
            val r = addTinySigned(other, negateOther = false, ctxMix)
            if (r != null)
                return r
        }
        return Deci(toBigDecimal().add(other.toBigDecimal()), ctxMix)
    }

    @JvmSynthetic
    internal fun minusInternal(other: Deci, ctxMix: CtxMixed): Deci {
        if (tinyDec.isValid() && other.tinyDec.isValid()) {
            // subtract = add with sign of other flipped
            val r = addTinySigned(other, negateOther = true, ctxMix)
            if (r != null)
                return r
        }
        return Deci(toBigDecimal().subtract(other.toBigDecimal()), ctxMix)
    }

    @JvmSynthetic
    internal fun timesInternal(other: Deci, ctxMix: CtxMixed): Deci {
        if (tinyDec.isValid() && other.tinyDec.isValid()) {
            val mulRes = TinyUDecMath.mulOrErr(getTinyUnscaled(), getTinyPos(), other.getTinyUnscaled(), other.getTinyPos())
            val resultNeg = isNegative() xor other.isNegative()
            val r = createFromTwoInt(mulRes, resultNeg, ctxMix)
            if (r != null)
                return r
        }
        return Deci(toBigDecimal().multiply(other.toBigDecimal()), ctxMix)
    }

    @JvmSynthetic
    internal fun divInternal(other: Deci, ctxMix: CtxMixed): Deci {
        if (tinyDec.isValid() && other.tinyDec.isValid()) {
            val divRes = TinyUDecMath.tryDivOrErr(getTinyUnscaled(), getTinyPos(), other.getTinyUnscaled(), other.getTinyPos())
            val resultNeg = isNegative() xor other.isNegative()
            val r = createFromTwoInt(divRes, resultNeg, ctxMix)
            if (r != null)
                return r
        }
        val a = toBigDecimal()
        val b = other.toBigDecimal()
        val dec = a.divide(b, calcDivScale(b), ctxMix.javaRoundingMode())
        return Deci(dec, ctxMix)
    }

    @JvmSynthetic
    internal fun remInternal(other: Deci, ctxMix: CtxMixed): Deci {
        if (tinyDec.isValid() && other.tinyDec.isValid()) {
            val r = remTinySigned(other, ctxMix)
            if (r != null)
                return r
        }
        return Deci(this.toBigDecimal().rem(other.toBigDecimal()), ctxMix)
    }

    private fun createFromTwoInt(pair: TwoInt, neg: Boolean, ctxMix: CtxMixed) : Deci? {
        if (pair.isErr())
            return null
        return createFromUnscaledPos(pair.first(), pair.second(), neg, ctxMix)
    }

    @JvmName("createFromUnscaledPos") // for tests only
    private fun createFromUnscaledPos(unscaled: Int, pos: Int, neg: Boolean, ctxMix: CtxMixed) : Deci? {
        val rtiny = TinyDec.buildTinyOrErr(unscaled, pos)
        if (rtiny.isValid()) {
            return Deci(rtiny, neg && unscaled != 0, ctxMix)
        }
        val rtiny4 = TinyDec4d.buildTiny4dOrErr(unscaled, pos)
        if (rtiny4.isValid())
            return Deci(rtiny4, neg, ctxMix, true)
        return null
    }

    // Signed addition of two tinyDec values, optionally negating the other operand (for subtraction).
    // Returns null if the result overflows TinyUDec range — caller falls back to BigDecimal.
    private fun addTinySigned(other: Deci, negateOther: Boolean, ctxMix: CtxMixed): Deci? {
        val aneg = isNegative()
        val aval = this.getTinyUnscaled()
        val apos = this.getTinyPos()
        val bneg = other.isNegative() xor negateOther
        val bval = other.getTinyUnscaled()
        val bpos = other.getTinyPos()
        if (aneg == bneg) {
            // same sign: magnitudes add, sign is preserved
            val pair = TinyUDecMath.addOrErr(aval, apos, bval, bpos)
            return createFromTwoInt(pair, aneg, ctxMix)
        }
        // different signs: subtract smaller magnitude from larger, sign follows the larger
        val comp = TinyUDecMath.compare(aval, apos, bval, bpos)
        if (comp > 0) {
            val pair = TinyUDecMath.subOrErr(aval, apos, bval, bpos)
            return createFromTwoInt(pair, aneg, ctxMix)
        }
        val res = TinyUDecMath.subOrErr(bval, bpos, aval, apos)
        return createFromTwoInt(res, bneg, ctxMix)
    }

    // Signed remainder of two tinyDec values.
    // Returns null if the result overflows TinyUDec range — caller falls back to BigDecimal.
    private fun remTinySigned(other: Deci, ctxMix: CtxMixed): Deci? {
        val aval = this.getTinyUnscaled()
        val apos = this.getTinyPos()
        val bval = other.getTinyUnscaled()
        val bpos = other.getTinyPos()
        val res = TinyUDecMath.remOrErr(aval, apos, bval, bpos)
        return createFromTwoInt(res, isNegative(), ctxMix)
    }

    fun toBigDecimal(): BigDecimal {
        if (decimal != null)
            return decimal!!
        normalizeTinyDec()
        var unscaled = getTinyUnscaled()
        if (isNegative())
            unscaled = -unscaled
        val d = BigDecimal.valueOf(unscaled.toLong(), getTinyPos())
        setFlagOn(FLAG_BIGD_TRIM)
        decimal = d
        return d
    }

    private fun normalizeDecimal(): BigDecimal {
        if (isFlag(FLAG_BIGD_TRIM))
            return decimal!!
        val dec = decimal
        if (dec == null) {
            check(tinyDec.isValid()) { "Deci is invalid" }
            return toBigDecimal()
        }
        val decn = dec.stripTrailingZeros()
        decimal = decn
        setFlagOn(FLAG_BIGD_TRIM)
        return decn
    }

    private fun asDecimal(): BigDecimal {
        var unscaled = getTinyUnscaled()
        if (isNegative())
            unscaled = -unscaled
        return decimal ?: BigDecimal.valueOf(unscaled.toLong(), getTinyPos())
    }

    // both TinyDec and TinyDec4d are value objects for Int, i.e. after compile both are primitive int
    private inline fun TinyDec4d.asTinyDec(): TinyDec {
        return TinyDec(this.raw)
    }

    private inline fun TinyDec.asTinyDec4d(): TinyDec4d {
        return TinyDec4d(this.raw)
    }

    // sometimes we want to have tinyDec even we already have decimal.
    // also normalize tiny (if valid)
    private fun tryInitTinyDec() {
        if (!isFlag(FLAG_TINY_INIT)) {
            setFlagOn(FLAG_TINY_INIT)
            val dec = normalizeDecimal()
            val isNeg = dec.signum() < 0
            val pair = TinyUDecMath.fromBigDecimalUnsigned(dec, TinyDec4d.maxPos)
            if (pair.isErr())
                return
            val unscaled = pair.first()
            val pos = pair.second()
            val rtiny = TinyDec.buildTinyOrErr(unscaled, pos)
            if (rtiny.isValid()) {
                tinyDec = if (rtiny.isZero()) TinyDec.ZERO else rtiny
            } else {
                val rtiny4 = TinyDec4d.buildTiny4dOrErr(unscaled, pos)
                if (rtiny4.isErr())
                    return
                tinyDec = rtiny4.asTinyDec()
                setFlagOn(FLAG_TINY_DEC4)
            }
            if (isNeg)
                setFlagOn(FLAG_NEGATIVE)
            setFlagOn(FLAG_TINY_TRIM)
        } else {
            normalizeTinyDec()
        }
    }

    private fun normalizeTinyDec() {
        if (tinyDec.isErr())
            return
        if (isFlag(FLAG_TINY_TRIM))
            return

        if (isFlag(FLAG_TINY_DEC4)) {
            val dn = tinyDec.asTinyDec4d().trimTrailingZeros()
            this.tinyDec = dn.asTinyDec()
        } else {
            this.tinyDec = tinyDec.trimTrailingZeros()
        }
        setFlagOn(FLAG_TINY_TRIM)
        return
    }

    private fun initDeciContext(deciCtx: DeciContext) {
        val mix: Int = when (deciCtx) {
            is DeciContextMixedImpl -> deciCtx.mixed.raw
            is DeciContextImpl -> deciCtx.mixed
            else -> DeciContextImpl.convDeciCtxValue(deciCtx)
        }
        this.mixed = CtxMixed(mix and MASK_25BITS) // will clear flags, for constructors only
    }

    private inline fun initDeciContext(ctxMix: CtxMixed) {
        this.mixed = CtxMixed(ctxMix.raw and MASK_25BITS) // will clear flags, for constructors only
    }

    private inline fun isFlag(flag: Int) = mixed.raw and flag != 0
    private fun setFlagOn(flag: Int) {
        mixed = CtxMixed(mixed.raw or flag)
    }

    private inline fun isNegative() = isFlag(FLAG_NEGATIVE)
    // ----------------------------------------------------------------

    // 'mixed' contains: a) deciContext (25 bits); b) flags
    @JvmInline
    internal value class CtxMixed(val raw: Int) {
        internal inline fun precision(): Int = (raw ushr 11) and MASK_11BITS
        internal inline fun roundingMode(): com.github.labai.deci.RoundingMode = RoundingMode.entries[(raw ushr 22) and MASK_3BITS]
        internal inline fun scale(): Int = raw and MASK_11BITS
        internal inline fun javaRoundingMode(): JavaRoundingMode = roundingMode().toJava()

        internal fun getAsDeciContext(): DeciContext = DeciContextMixedImpl(this)
    }


    actual companion object {
        private val originalDefaultDeciContext: DeciContext = DeciContextImpl(20, RoundingMode.HALF_UP, 20)
        actual var defaultDeciContext: DeciContext = originalDefaultDeciContext
        private val useCustomBigDecimalParser = true // is 2x faster

        // few popular numbers
        actual val ZERO = Deci(0L).apply { toBigDecimal() }
        private val D1 = Deci(1L).apply { toBigDecimal() }
        private val D2 = Deci(2L).apply { toBigDecimal() }
        private val D10 = Deci(10L).apply { toBigDecimal() }
        private val D100 = Deci(100L).apply { toBigDecimal() }
        private val D1000 = Deci(1000L).apply { toBigDecimal() }
        private val MIXED_NOTINIT = CtxMixed(0)

        private const val FLAG_TINY_INIT: Int = 1 shl 26 // have tried to init tinyDec (even if failed)
        private const val FLAG_TINY_TRIM: Int = 1 shl 27
        private const val FLAG_BIGD_TRIM: Int = 1 shl 28
        private const val FLAG_NEGATIVE: Int = 1 shl 29
        private const val FLAG_TINY_DEC4: Int = 1 shl 30 // tinyDec is TinyDec4d, not TinyDec
        private const val FLAG_RESERVED: Int = 1 shl 31 // not for use (first bit, need to review)

        @JvmStatic
        actual fun valueOf(int: Int): Deci {
            return if (int in 0..1000 && defaultDeciContext == originalDefaultDeciContext) {
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
            else Deci(int)
        }

        @JvmStatic
        actual fun valueOf(long: Long): Deci {
            return if (long in 0L..1000L && defaultDeciContext == originalDefaultDeciContext) {
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
            else Deci(long)
        }
    }
}

operator fun Deci.plus(other: BigDecimal): Deci = this.plus(other.deci)
operator fun Deci.minus(other: BigDecimal): Deci = this.minus(other.deci)
operator fun Deci.times(other: BigDecimal): Deci = this.times(other.deci)
operator fun Deci.div(other: BigDecimal): Deci = this.div(other.deci)
operator fun Deci.rem(other: BigDecimal): Deci = this.rem(other.deci)

actual operator fun Deci.plus(other: Deci): Deci = this.plusInternal(other, this.getMixed())
actual operator fun Deci.minus(other: Deci): Deci = this.minusInternal(other, this.getMixed())
actual operator fun Deci.times(other: Deci): Deci = this.timesInternal(other, this.getMixed())
actual operator fun Deci.div(other: Deci): Deci = this.divInternal(other, this.getMixed())
actual operator fun Deci.rem(other: Deci): Deci = this.remInternal(other, this.getMixed())

actual operator fun Deci.plus(other: Int): Deci = this.plus(other.deci)
actual operator fun Deci.minus(other: Int): Deci = this.minus(other.deci)
actual operator fun Deci.times(other: Int): Deci = this.times(other.deci)
actual operator fun Deci.div(other: Int): Deci = this.div(other.deci)
actual operator fun Deci.rem(other: Int): Deci = this.rem(other.deci)

actual operator fun Deci.plus(other: Long): Deci = this.plus(other.deci)
actual operator fun Deci.minus(other: Long): Deci = this.minus(other.deci)
actual operator fun Deci.times(other: Long): Deci = this.times(other.deci)
actual operator fun Deci.div(other: Long): Deci = this.div(other.deci)
actual operator fun Deci.rem(other: Long): Deci = this.rem(other.deci)

//
// BigDecimal extensions
//
val BigDecimal.deci: Deci
    inline get() = Deci(this)

infix fun BigDecimal.eq(other: Deci) = this.compareTo(other.toBigDecimal()) == 0

//
// additional Deci methods
//
actual fun Companion.valueOf(num: Number): Deci {
    return when (num) {
        is Deci -> num
        is BigDecimal -> Deci(num)
        is Int -> valueOf(num)
        is Long -> valueOf(num)
        is Double -> Deci(BigDecimal.valueOf(num))
        is Float -> Deci(BigDecimal.valueOf(num.toDouble()))
        is Short -> valueOf(num.toInt())
        is Byte -> valueOf(num.toInt())
        is BigInteger -> Deci(num.toBigDecimal())
        else -> Deci(num.toString())
    }
}

actual fun Companion.valueOf(str: String): Deci = Deci(str)

actual fun Companion.valueOf(num: Number, deciContext: DeciContext): Deci {
    return when (num) {
        is Deci -> if (deciContext.isDeciCtxEqual(num.getMixed())) num else Deci(num.toBigDecimal(), deciContext)
        is BigDecimal -> Deci(num, deciContext)
        is Int -> Deci(int = num, deciContext)
        is Long -> Deci(long = num, deciContext)
        is Double -> Deci(BigDecimal.valueOf(num), deciContext)
        is Float -> Deci(BigDecimal.valueOf(num.toDouble()), deciContext)
        is Short -> Deci(int = num.toInt(), deciContext)
        is Byte -> Deci(int = num.toInt(), deciContext)
        is BigInteger -> Deci(num.toBigDecimal(), deciContext)
        else -> Deci(num.toString(), deciContext)
    }
}

actual fun Companion.valueOf(str: String, deciContext: DeciContext): Deci = Deci(str, deciContext)

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
        is BigInteger -> compareTo(other.toBigDecimal().deci)
        else -> this.compareTo(Deci(other.toString()))
    }
}
