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
import com.github.labai.deci.impl.TinyDec.Companion.ERR
import com.github.labai.deci.impl.TinyDec.Companion.ZERO
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
    const val MAX_UNSCALED = 999_999_999 //
    const val ERR_VALUE = MAX_UNSCALED + 2 // 1000000001
    const val MAX_POS = 3
    const val MAX_STR_LEN = 10 // count dot, but don't count '+'
    const val MAX_INT_LEN = 9  // number of digits

    internal val POW: IntArray = intArrayOf(
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
    private inline fun makeDec30(unscaled: Int, pos: Int): Int {
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

    fun trimTrailingZeros(tiny: TinyDec): TinyDec {
        require(tiny.isValid()) { "Invalid tinyDec value (err)" }
        return TinyDec(makeDec30Compact(tiny.unscaled(), tiny.pos()))
    }

    // integer part
    fun getIntPart(unscaled: Int, pos: Int): Int {
        require(unscaled != ERR_VALUE) { "Invalid tinyDec value (err)" }
        return unscaled / POW[pos]
    }

    fun buildTinyOrErr(value: Int, pos: Int): TinyDec {
        if (pos !in 0..MAX_POS)
            return ERR
        if (value !in 1..MAX_UNSCALED)
            return if (value == 0) ZERO else ERR
        return TinyDec(makeDec30(value, pos))
    }

    private fun buildTinyOrErr(value: Long, pos: Int): TinyDec {
        if (pos !in 0..MAX_POS)
            return ERR
        if (value !in 1..MAX_UNSCALED)
            return if (value == 0L) ZERO else ERR
        return TinyDec(makeDec30(value.toInt(), pos))
    }

    private fun buildTinyCompactOrErr(value: Int, pos: Int): TinyDec {
        if (pos !in 0..MAX_POS)
            return ERR
        if (value !in 1..MAX_UNSCALED)
            return if (value == 0) ZERO else ERR
        return TinyDec(makeDec30Compact(value, pos))
    }

    private fun buildTinyCompactOrErr(value: Long, pos: Int): TinyDec {
        if (pos !in 0..MAX_POS)
            return ERR
        if (value !in 1..MAX_UNSCALED)
            return if (value == 0L) ZERO else ERR
        return TinyDec(makeDec30Compact(value.toInt(), pos))
    }

    fun buildTiny(value: Int, pos: Int): TinyDec {
        require(pos in 0..MAX_POS) { "Pos must be in 0..$MAX_POS ($pos)" }
        if (value !in 1..MAX_UNSCALED) {
            if (value == 0)
                return ZERO
            throw IllegalArgumentException("Value is too large ($value)")
        }
        return TinyDec(makeDec30(value, pos))
    }

    fun toBigDecimal(tiny: TinyDec): BigDecimal {
        require(tiny.isValid()) { "Invalid tinyDec value (err)" }
        return BigDecimal.valueOf(tiny.unscaled().toLong(), tiny.pos())
    }

    fun convertToTinyOrErr(value: Long): TinyDec {
        if (value !in 0..MAX_UNSCALED)
            return ERR
        return buildTiny(value.toInt(), 0)
    }

    fun convertToTinyOrErr(value: Int): TinyDec {
        if (value !in 0..MAX_UNSCALED)
            return ERR
        return buildTiny(value, 0)
    }

    internal fun round(tiny: TinyDec, scale: Int, roundingMode: RoundingMode): TinyDec {
        if (tiny.isErr())
            return tiny
        val pos = tiny.pos()
        if (pos <= scale)
            return tiny
        val unscaled = tiny.unscaled()

        val drop = pos - scale
        val shrank = divideAndRound(unscaled, POW[drop], roundingMode)
        return buildTiny(shrank, scale)
    }

    internal fun divideAndRound(dividend: Int, divisor: Int, roundingMode: RoundingMode): Int {
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
    fun convertToTinyOrErr(dec: BigDecimal): TinyDec {
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

    fun addOrErr(a: TinyDec, b: TinyDec): TinyDec {
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

        if (rval > MAX_UNSCALED)
            return ERR
        return buildTiny(rval.toInt(), rpos)
    }

    fun subOrErr(a: TinyDec, b: TinyDec): TinyDec {
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
        if (rval !in 0..MAX_UNSCALED)
            return ERR
        return buildTiny(rval.toInt(), rpos)
    }

    fun mulOrErr(a: TinyDec, b: TinyDec): TinyDec {
        if (a == ERR || b == ERR)
            return ERR
        val apos = a.pos()
        val aval = a.unscaled()
        val bpos = b.pos()
        val bval = b.unscaled()

        var rval: Long = aval.toLong() * bval
        var rpos = apos + bpos
        if (rpos <= MAX_POS && rval <= MAX_UNSCALED) {
            // all good, fit everywhere
            return buildTiny(rval.toInt(), rpos)
        }

        // chance there are trailing zeros
        while (rpos > MAX_POS || rval > MAX_UNSCALED) {
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
    fun tryDivOrErr(u: TinyDec, v: TinyDec): TinyDec {
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
    fun remOrErr(a: TinyDec, b: TinyDec): TinyDec {
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
}
