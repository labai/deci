package com.github.labai.deci.impl

import com.github.labai.deci.RoundingMode
import java.math.BigDecimal

/*
 * @author Augustus
 * created on 2026-04-06
 *
 * tiny decimal, stored in Int32.
 * can keep 9 digits (max 1_073_741_824, but will use max 999_999_999)
 *
 * Int (32 bits), where
 *  - 2 bits for decimal point position (->3 digits)
 *  - last 30 bits for value
 *  - unsigned (no negative values)
 *
 *
 *  Can do simple operations:
 *  - add
 *  - subtract
 *  - multiply
 *  - reminder
 *  - compare
 *  - round
 *  - parseString
 *
 * Remarks:
 * - in case of overflow usually returns ERR and then will need to continue with BigDecimal.
 *
 */
internal object TinyUDecMath {
    const val MASK_VALUE: Int  = 0b00111111111111111111111111111111
    const val MAX_VALUE = 999_999_999 //
    const val ERR_VALUE = MAX_VALUE + 2 // 1000000001
    const val MAX_POS = 3
    const val MAX_STR_LEN = 10 // count dot, but don't count '+'
    const val MAX_INT_LEN = 9  // number of digits
    val ERR = TinyUDec(ERR_VALUE)
    val ZERO = TinyUDec(0)

    @Suppress("NOTHING_TO_INLINE")
    @JvmInline
    value class TinyUDec (
        internal val tiny: Int
    ) : Comparable<TinyUDec> {
        inline fun pos() = tiny ushr 30
        inline fun unscaled() = (tiny and MASK_VALUE)
        internal inline fun getPow10() = POW[pos()]
        fun add(other: TinyUDec) = addOrErr(this, other)
        fun sub(other: TinyUDec) = subOrErr(this, other)
        fun mul(other: TinyUDec) = mulOrErr(this, other)
        fun tryDiv(other: TinyUDec) = tryDivOrErr(this, other) // support only few cases w/o rounding
        fun rem(other: TinyUDec) = remOrErr(this, other)
        fun round(scale: Int, roundingMode: RoundingMode) = round(this, scale, roundingMode)
        fun intPart() = getIntPart(this)
        fun decPart() = getDecPart(this)
        fun toBigDecimal(): BigDecimal = toBigDecimal(this)
        override fun compareTo(other: TinyUDec) = compare(this, other)
        override fun toString(): String = toString(this)
        fun isEqual(other: TinyUDec): Boolean = isEqual(this, other)
        internal fun trimTrailingZeros(): TinyUDec = trimTrailingZeros(this)
        internal fun isZero(): Boolean = unscaled() == 0

        companion object {
            fun parseString(str: String) = TinyUDecMath.parseString(str)
            fun valueOf(bigdec: BigDecimal): TinyUDec = convertToTinyOrErr(bigdec)
            fun valueOf(int: Int): TinyUDec = convertToTinyOrErr(int)
            fun valueOf(long: Long): TinyUDec = convertToTinyOrErr(long)
        }
    }

    private val POW: IntArray = intArrayOf(
        1,                    // 0 / 10^0
        10,                   // 1 / 10^1
        100,                  // 2 / 10^2
        1000,                 // 3 / 10^3
        10000,                // 4 / 10^4
        100000,               // 5 / 10^5
        1000000,              // 6 / 10^6
        10000000,             // 7 / 10^7
        100000000,            // 8 / 10^8
        1000000000,           // 9 / 10^9
    )

    // no check, for private usage
    private inline fun makeDec30(unscaled: Int, pos: Int): TinyUDec {
        return TinyUDec((pos shl 30) or unscaled)
    }

    // no check, for private usage
    private fun makeDec30Compact(unscaled: Int, pos: Int): TinyUDec {
        var res = unscaled
        var pos = pos
        while (pos > 0) {
            if (res % 10 != 0)
                break
            res /= 10
            pos--
        }
        return makeDec30(res, pos)
    }

    fun trimTrailingZeros(tiny: TinyUDec): TinyUDec {
        require(tiny != ERR) { "Invalid tinyDec value (err)" }
        return makeDec30Compact(tiny.unscaled(), tiny.pos())
    }

    // integer part
    fun getIntPart(tiny: TinyUDec): Int {
        require(tiny != ERR) { "Invalid tinyDec value (err)" }
        return tiny.unscaled() / tiny.getPow10()
    }

    // decimals part
    fun getDecPart(tiny: TinyUDec): Int {
        require(tiny != ERR) { "Invalid tinyDec value (err)" }
        return tiny.unscaled() % tiny.getPow10()
    }

    private fun buildTinyOrErr(value: Int, pos: Int): TinyUDec {
        if (pos !in 0..MAX_POS)
            return ERR
        if (value !in 1..MAX_VALUE)
            return if (value == 0) ZERO else ERR
        return makeDec30(value, pos)
    }

    private fun buildTinyOrErr(value: Long, pos: Int): TinyUDec {
        if (pos !in 0..MAX_POS)
            return ERR
        if (value !in 1..MAX_VALUE)
            return if (value == 0L) ZERO else ERR
        return makeDec30(value.toInt(), pos)
    }

    private fun buildTinyCompactOrErr(value: Int, pos: Int): TinyUDec {
        if (pos !in 0..MAX_POS)
            return ERR
        if (value !in 1..MAX_VALUE)
            return if (value == 0) ZERO else ERR
        return makeDec30Compact(value, pos)
    }

    private fun buildTinyCompactOrErr(value: Long, pos: Int): TinyUDec {
        if (pos !in 0..MAX_POS)
            return ERR
        if (value !in 1..MAX_VALUE)
            return if (value == 0L) ZERO else ERR
        return makeDec30Compact(value.toInt(), pos)
    }

    fun buildTiny(value: Int, pos: Int): TinyUDec {
        require(pos in 0..MAX_POS) { "Pos must be in 0..$MAX_POS ($pos)" }
        if (value !in 1..MAX_VALUE) {
            if (value == 0)
                return ZERO
            throw IllegalArgumentException("Value is too large ($value)")
        }
        return makeDec30(value, pos)
    }

    fun toBigDecimal(tiny: TinyUDec): BigDecimal {
        require(tiny != ERR) { "Invalid tinyDec value (err)" }
        return BigDecimal.valueOf(tiny.unscaled().toLong(), tiny.pos())
    }

    internal fun parseStringOrErr(str: String): TinyUDec {
        return parseStringOrErr(str, true)
    }

    internal fun parseString(str: String): TinyUDec {
        return parseStringOrErr(str, false)
    }

    private fun errOrRaise(silent: Boolean, lazyMessage: () -> Any): TinyUDec {
        if (!silent)
            throw IllegalArgumentException(lazyMessage().toString())
        return ERR
    }

    private fun parseStringOrErr(str: String, silent: Boolean): TinyUDec {
        if (str.isEmpty()) return errOrRaise(silent) { "String is empty" }

        val start = when (str[0]) {
            '+' -> 1
            '-' -> return errOrRaise(silent) { "Only positive numbers are allowed." }
            else -> 0
        }

        var value = 0
        var digitCount = 0
        var dot = -1
        var trailingZeros = 0

        for (i in start until str.length) {
            when (val c = str[i]) {
                '.' -> {
                    if (dot >= 0) return errOrRaise(silent) { "Invalid number format: $str" }
                    dot = digitCount // position in digit sequence, not in string
                }
                in '0'..'9' -> {
                    if (dot >= 0) {
                        if (c == '0') {
                            trailingZeros++
                        } else if (trailingZeros == 0) {
                            value = value * 10 + (c - '0')
                            digitCount++
                            if (digitCount > MAX_INT_LEN) return errOrRaise(silent) { "String value too long: $str" }
                        } else { // reset, flush deferred zeros and add current digit
                            trailingZeros++
                            digitCount += trailingZeros
                            if (digitCount > MAX_INT_LEN) return errOrRaise(silent) { "String value too long: $str" }
                            value = value * POW[trailingZeros] + (c - '0')
                            trailingZeros = 0
                        }
                    } else {
                        value = value * 10 + (c - '0')
                        digitCount++
                        if (digitCount > MAX_INT_LEN) return errOrRaise(silent) { "String value too long: $str" }
                    }
                }
                else -> return errOrRaise(silent) { "Invalid character '$c' in: $str" }
            }
        }

        val pos = if (dot < 0) 0 else maxOf(digitCount - dot, 0)

        if (pos > MAX_POS)
            return errOrRaise(silent) { "Too big precision: $str" }

        return if (silent) buildTinyOrErr(value, pos) else buildTiny(value, pos)
    }

    fun convertToTinyOrErr(value: Long): TinyUDec {
        if (value !in 0..MAX_VALUE)
            return ERR
        return buildTiny(value.toInt(), 0)
    }

    fun convertToTinyOrErr(value: Int): TinyUDec {
        if (value !in 0..MAX_VALUE)
            return ERR
        return buildTiny(value, 0)
    }

    internal fun round(tiny: TinyUDec, scale: Int, roundingMode: RoundingMode): TinyUDec {
        if (tiny == ERR)
            return ERR
        val pos = tiny.pos()
        if (pos <= scale)
            return tiny
        val unscaled = tiny.unscaled()

        val drop = pos - scale
        val shrank = divideAndRound(unscaled, POW[drop], roundingMode)
        return buildTiny(shrank, scale)
    }

    private fun divideAndRound(dividend: Int, divisor: Int, roundingMode: RoundingMode): Int {
        val qt = dividend / divisor
        if (roundingMode == RoundingMode.DOWN)
            return qt
        val rem = dividend % divisor
        if (rem == 0)
            return qt
        val needIncrement = needIncrement(roundingMode, divisor, qt, rem)
        return if (needIncrement) qt + 1 else qt
    }

    // simplified version, for positive only, no check for overflow
    private fun needIncrement(roundingMode: RoundingMode, divisor: Int, qt: Int, rem: Int): Boolean {
        val fractHalfCmp = (rem shl 1).compareTo(divisor) // rem*2 < divisor --> less than 0.5

        return when (roundingMode) {
            RoundingMode.UP, RoundingMode.CEILING -> true
            RoundingMode.DOWN, RoundingMode.FLOOR -> false
            else -> {
                if (fractHalfCmp < 0)  // closer to lower digit
                    false
                else if (fractHalfCmp > 0)  // closer to higher digit
                    true
                else {
                    when (roundingMode) {
                        RoundingMode.HALF_DOWN -> false
                        RoundingMode.HALF_UP -> true
                        RoundingMode.HALF_EVEN -> (qt and 1) != 0
                    }
                }
            }
        }
    }

    // try to convert BigDecimal to tinyDec
    // return ERR if out of limits
    // don't try to trim zeros from dec (?)
    fun convertToTinyOrErr(dec: BigDecimal): TinyUDec {
        when (dec.signum()) {
            -1 -> return ERR
            0 -> return ZERO
        }
        if (dec.precision() > MAX_INT_LEN) {
            // may try to trimTrailingZeros, but that will create new objects, and small chance for success won't pay off (?)
            return ERR
        }

        if (dec.scale() > MAX_POS)
            return ERR

        val unscaled: Long
        val pos: Int
        if (dec.scale() <= 0) {
            unscaled = dec.toLong()
            pos = 0
        } else {
            unscaled = dec.unscaledValue().toLong() // will be intermediate BigInteger allocation (?)
            pos = dec.scale()
        }

        return buildTinyOrErr(unscaled, pos)
    }

    fun addOrErr(a: TinyUDec, b: TinyUDec): TinyUDec {
        // no explicit check for ERR - if either is ERR, sum will be > MAX_VALUE
        val apos = a.pos()
        val bpos = b.pos()
        val aval = a.unscaled()
        val bval = b.unscaled()

        val rpos: Int
        val rval: Long
        if (apos == bpos) {
            rpos = apos
            rval = aval.toLong() + bval
        } else if (apos < bpos) {
            rpos = bpos
            rval = aval.toLong() * POW[bpos - apos] + bval
        } else {
            rpos = apos
            rval = aval.toLong() + bval.toLong() * POW[apos - bpos]
        }

        if (rval > MAX_VALUE)
            return ERR
        return buildTiny(rval.toInt(), rpos)
    }

    fun subOrErr(a: TinyUDec, b: TinyUDec): TinyUDec {
        if (a == ERR || b == ERR)
            return ERR
        val apos = a.pos()
        val aval = a.unscaled()
        val bpos = b.pos()
        val bval = b.unscaled()

        val rpos: Int
        var rval: Long
        if (apos == bpos) {
            rpos = apos
            rval = aval.toLong() - bval
        } else if (apos < bpos) {
            rpos = bpos
            rval = aval.toLong() * POW[bpos - apos] - bval
        } else {
            rpos = apos
            rval = aval.toLong() - bval.toLong() * POW[apos - bpos]
        }
        if (rval !in 0..MAX_VALUE)
            return ERR
        return buildTiny(rval.toInt(), rpos)
    }

    fun mulOrErr(a: TinyUDec, b: TinyUDec): TinyUDec {
        if (a == ERR || b == ERR)
            return ERR
        val apos = a.pos()
        val aval = a.unscaled()
        val bpos = b.pos()
        val bval = b.unscaled()

        var rval: Long = aval.toLong() * bval
        var rpos = apos + bpos
        if (rpos <= MAX_POS && rval <= MAX_VALUE) {
            // all good, fit everywhere
            return buildTiny(rval.toInt(), rpos)
        }

        // chance there are trailing zeros
        while (rpos > MAX_POS || rval > MAX_VALUE) {
            if (rpos == 0)
                return ERR
            if (rval % 10 != 0L)
                return ERR
            rval /= 10
            rpos--
        }
        return buildTinyOrErr(rval.toInt(), rpos)
    }


    // try to divide, but only if no rounding is needed
    //
    // quick check for few special cases
    // (0, 1, 10^n)
    //
    fun tryDivOrErr(u: TinyUDec, v: TinyUDec): TinyUDec {
        // assume tiny is normalized (w/o trailing zeros)
        if (u == ERR || v == ERR)
            return ERR
        val uval = u.unscaled()
        val vval = v.unscaled()
        val vpos = v.pos()
        if (vval == 0)
            return ERR
        if (uval == 0)
            return ZERO
        if (vpos == 0) {
            for (i in POW.indices) {
                if (POW[i] >= vval) {
                    if (vval == POW[i]) { // exact power of 10
                        return buildTinyCompactOrErr(uval, u.pos() + i) // if overflowed, it will be handled here
                    }
                    break
                }
            }
        }
        if (vval == 1) { // 1, 0.1, 0.01, ...
            val rpos = u.pos() - vpos
            if (rpos < 0) {
                val rval = uval.toLong() * POW[-rpos]
                return buildTinyCompactOrErr(rval, 0)
            } else {
                return buildTinyCompactOrErr(uval, rpos)
            }
        }

        if (uval % vval == 0) {
            return buildTinyCompactOrErr(uval / vval, u.pos() - vpos)
        }
        return ERR
    }

    // returns a % b
    fun remOrErr(a: TinyUDec, b: TinyUDec): TinyUDec {
        if (a == ERR || b == ERR)
            return ERR
        val bval = b.unscaled()
        if (bval == 0)
            return ERR

        val apos = a.pos()
        val bpos = b.pos()
        val aval = a.unscaled()

        val rpos: Int
        val rval: Long
        if (apos == bpos) {
            rpos = apos
            rval = aval.toLong() % bval
        } else if (apos < bpos) {
            rpos = bpos
            rval = aval.toLong() * POW[bpos - apos] % bval
        } else {
            rpos = apos
            rval = aval.toLong() % (bval.toLong() * POW[apos - bpos])
        }

        return buildTinyOrErr(rval.toInt(), rpos)
    }


    fun toString(tiny: TinyUDec): String {
        if (tiny == ERR)
            return "Err"
        val pos = tiny.pos()
        val unscaled = tiny.unscaled()

        if (pos == 0)
            return unscaled.toString()

        val pow = POW[pos]
        val intPart = unscaled / pow
        val decPart = unscaled % pow

        val buf = CharArray(MAX_STR_LEN) // 9 digits + 1 dot
        var idx = buf.size

        var d = decPart
        for (i in 0 until pos) {
            buf[--idx] = '0' + (d % 10)
            d /= 10
        }

        buf[--idx] = '.'

        d = intPart
        if (d == 0) {
            buf[--idx] = '0'
        } else {
            while (d > 0) {
                buf[--idx] = '0' + (d % 10)
                d /= 10
            }
        }

        return String(buf, idx, buf.size - idx)
    }

    fun compare(a: TinyUDec, b: TinyUDec): Int {
        if (a == b)
            return 0
        val apos = a.pos()
        val aval = a.unscaled()
        val bpos = b.pos()
        val bval = b.unscaled()
        return if (apos == bpos) {
            aval.compareTo(bval)
        } else if (apos < bpos) {
            val aa = aval.toLong() * POW[bpos - apos]
            aa.compareTo(bval)
        } else {
            val bb = bval.toLong() * POW[apos - bpos]
            aval.compareTo(bb)
        }
    }

    fun isEqual(a: TinyUDec, b: TinyUDec): Boolean {
        if (a == b)
            return true
        if (a == ERR || b == ERR)
            return false
        return a.compareTo(b) == 0
    }
}
