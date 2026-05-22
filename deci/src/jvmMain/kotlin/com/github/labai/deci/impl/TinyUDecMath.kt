/*
MIT License

Copyright (c) 2026 Augustus

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
package com.github.labai.deci.impl

import com.github.labai.deci.RoundingMode
import java.math.BigDecimal
import kotlin.math.absoluteValue

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
    const val MASK_VALUE: Int  = 0b00111111_11111111_11111111_11111111
    const val MAX_UNSCALED = 999_999_999 //
    const val ERR_VALUE = MAX_UNSCALED + 2 // 1000000001
    const val MAX_STR_LEN = 10 // count dot, but don't count '+'
    const val MAX_INT_LEN = 9  // number of digits
    private const val MAX_POS = TinyDec4d.maxPos // =7

    val TWOINT_ERR = TwoInt(-1L)
    val TWOINT_ZERO = TwoInt(0L)

    // to avoid creation of objects, will use long and put there 2 ints
    // this version for internal usage only!
    @Suppress("NOTHING_TO_INLINE")
    @JvmInline
    value class TwoInt (
        internal val long: Long
    ) {
        inline fun first() = (long ushr 32).toInt()
        inline fun second() = long.toInt()
        inline fun isErr() = long == -1L
        companion object {
            inline fun toTwoInt(first: Int, second: Int) = TwoInt((first.toLong() shl 32) or second.toLong())

            fun toTwoIntWithTrimZero(unscaled: Int, pos: Int): TwoInt {
                var res = unscaled
                var pos = pos
                while (pos > 0) {
                    if (res % 10 != 0)
                        break
                    res /= 10
                    pos--
                }
                return toTwoInt(res, pos)
            }
        }
    }

    val POW: IntArray = intArrayOf(
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
    internal inline fun makeDec30(unscaled: Int, pos: Int): Int {
        return (pos shl 30) or unscaled
    }

    // no check, for private usage
    internal fun makeDec30Compact(unscaled: Int, pos: Int): Int {
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

    // integer part
    fun getIntPart(unscaled: Int, pos: Int): Int {
        require(unscaled != ERR_VALUE) { "Invalid tinyDec value (err)" }
        return unscaled / POW[pos]
    }

    internal fun round(unscaled: Int, pos: Int, roundScale: Int, roundingMode: RoundingMode): TwoInt {
        if (unscaled == ERR_VALUE)
            return TWOINT_ERR
        val drop = maxOf(pos - maxOf(roundScale, 0), 0)
        if (drop == 0)
            return TwoInt.toTwoInt(unscaled, pos)
        val shrank = divideAndRound(unscaled, POW[drop], roundingMode)
        return TwoInt.toTwoInt(shrank, roundScale)
    }

    fun divideAndRound(dividend: Int, divisor: Int, roundingMode: RoundingMode): Int {
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

    fun addOrErr(aval: Int, apos: Int, bval: Int, bpos: Int): TwoInt {
        if (aval == ERR_VALUE || bval == ERR_VALUE)
            return TWOINT_ERR

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

        if (rval > MAX_UNSCALED)
            return TWOINT_ERR
        return TwoInt.toTwoInt(rval.toInt(), rpos)
    }

    fun subOrErr(aval: Int, apos: Int, bval: Int, bpos: Int): TwoInt {
        if (aval == ERR_VALUE || bval == ERR_VALUE)
            return TWOINT_ERR

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
        if (rval !in 0..MAX_UNSCALED)
            return TWOINT_ERR
        return TwoInt.toTwoInt(rval.toInt(), rpos)
    }

    fun mulOrErr(aval: Int, apos: Int, bval: Int, bpos: Int): TwoInt {
        if (aval == ERR_VALUE || bval == ERR_VALUE)
            return TWOINT_ERR

        var rval: Long = aval.toLong() * bval
        var rpos = apos + bpos
        if (rpos <= MAX_POS && rval <= MAX_UNSCALED) {
            // all good, fit everywhere
            return TwoInt.toTwoInt(rval.toInt(), rpos)
        }

        // chance there are trailing zeros
        while (rpos > MAX_POS || rval > MAX_UNSCALED) {
            if (rpos == 0)
                return TWOINT_ERR
            if (rval % 10 != 0L)
                return TWOINT_ERR
            rval /= 10
            rpos--
        }
        return TwoInt.toTwoInt(rval.toInt(), rpos)
    }


    // try to divide, but only if no rounding is needed
    //
    // quick check for few special cases
    // (0, 1, 10^n)
    //
    fun tryDivOrErr(aval: Int, apos: Int, bval: Int, bpos: Int): TwoInt {
        // assume tiny is normalized (w/o trailing zeros)
        if (aval == ERR_VALUE || bval == ERR_VALUE)
            return TWOINT_ERR
        if (bval == 0)
            return TWOINT_ERR
        if (aval == 0)
            return TWOINT_ZERO
        if (bpos == 0) {
            for (i in POW.indices) {
                if (POW[i] >= bval) {
                    if (bval == POW[i]) { // exact power of 10
                        return TwoInt.toTwoIntWithTrimZero(aval, apos + i) // if overflowed, it will be handled later
                    }
                    break
                }
            }
        }
        if (bval == 1) { // 1, 0.1, 0.01, ...
            val rpos = apos - bpos
            if (rpos < 0) {
                val rval = aval.toLong() * POW[-rpos]
                if (rval !in 0..MAX_UNSCALED)
                    return TWOINT_ERR
                return TwoInt.toTwoInt(rval.toInt(), 0)
            } else {
                return TwoInt.toTwoIntWithTrimZero(aval, rpos)
            }
        }

        if (aval % bval == 0) {
            return TwoInt.toTwoIntWithTrimZero(aval / bval, apos - bpos)
        }
        return TWOINT_ERR
    }

    // returns a % b
    fun remOrErr(aval: Int, apos: Int, bval: Int, bpos: Int): TwoInt {
        if (aval == ERR_VALUE || bval == ERR_VALUE)
            return TWOINT_ERR
        if (bval == 0)
            return TWOINT_ERR

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

        return TwoInt.toTwoInt(rval.toInt(), rpos)
    }

    fun toString(unscaled: Int, pos: Int): String {
        if (unscaled == ERR_VALUE)
            return "Err"

        if (pos == 0)
            return unscaled.toString()

        val pow = POW[pos]
        val intPart = unscaled / pow
        val decPart = unscaled % pow
        var nonTrail = false // not in trailing zero

        val buf = CharArray(MAX_STR_LEN) // 9 digits + 1 dot
        var idx = buf.size

        var d = decPart
        for (i in 0 until pos) {
            val dig = d % 10
            d /= 10
            if (dig != 0) {
                nonTrail = true
            }
            if (nonTrail)
                buf[--idx] = '0' + dig
        }

        if (nonTrail)
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

    fun compare(aUnscaled: Int, aPos: Int, bUnscaled: Int, bPos: Int): Int {
        return if (aPos == bPos) {
            aUnscaled.compareTo(bUnscaled)
        } else if (aPos < bPos) {
            val aa = aUnscaled.toLong() * POW[bPos - aPos]
            aa.compareTo(bUnscaled)
        } else {
            val bb = bUnscaled.toLong() * POW[aPos - bPos]
            aUnscaled.compareTo(bb)
        }
    }

    fun isEqual(aUnscaled: Int, aPos: Int, bUnscaled: Int, bPos: Int): Boolean {
        if (aUnscaled == ERR_VALUE || bUnscaled == ERR_VALUE)
            return aUnscaled == bUnscaled // ERR == ERR
        return compare(aUnscaled, aPos, bUnscaled, bPos) == 0
    }

    private inline fun errOrRaise(silent: Boolean, lazyMessage: () -> Any): TwoInt {
        if (!silent)
            throw IllegalArgumentException(lazyMessage().toString())
        return TWOINT_ERR
    }

    // as we don't know yet which of TinyDec will use (it depends on number of decimals),
    // we return result as bigger number (stored in Long) and then caller will decide how to use it.
    // but still we limit number of digits (9) and range of decimals
    internal fun parseString(str: String, limitDigits: Int, minPos: Int, maxPos: Int, silent: Boolean): TwoInt {
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
                            if (digitCount > limitDigits) return errOrRaise(silent) { "String value too long: $str" }
                        } else { // reset, flush deferred zeros and add current digit
                            trailingZeros++
                            digitCount += trailingZeros
                            if (digitCount > limitDigits) return errOrRaise(silent) { "String value too long: $str" }
                            value = value * POW[trailingZeros] + (c - '0')
                            trailingZeros = 0
                        }
                    } else {
                        value = value * 10 + (c - '0')
                        digitCount++
                        if (digitCount > limitDigits) return errOrRaise(silent) { "String value too long: $str" }
                    }
                }
                else -> return errOrRaise(silent) { "Invalid character '$c' in: $str" }
            }
        }

        val pos = if (dot < 0) 0 else maxOf(digitCount - dot, 0)

        if (pos < minPos)
            return errOrRaise(silent) { "Too small number of decimals: $str" }
        if (pos > maxPos)
            return errOrRaise(silent) { "Too big number of decimals: $str" }

        return TwoInt.toTwoInt(value, pos)
    }

    fun fromBigDecimalUnsigned(dec: BigDecimal, maxPos: Int,): TwoInt {
        when (dec.signum()) {
            -1 -> return TWOINT_ERR
            0 -> return TWOINT_ZERO
        }
        if (dec.precision() > MAX_INT_LEN) {
            // may try to trimTrailingZeros, but that will create new objects, and small chance for success won't pay off (?)
            return TWOINT_ERR
        }

        if (dec.scale() > maxPos) {
            return TWOINT_ERR
        }

        var unscaled: Long
        val pos: Int
        if (dec.scale() <= 0) {
            unscaled = dec.toLong()
            pos = 0
        } else {
            unscaled = dec.unscaledValue().toLong() // will be intermediate BigInteger allocation (?)
            pos = dec.scale()
        }
        if (unscaled < 0)
            unscaled = unscaled.absoluteValue

        if (unscaled !in 0..MAX_UNSCALED)
            return TWOINT_ERR

        return TwoInt.toTwoInt(unscaled.toInt(), pos)
    }
}
