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

import com.github.labai.deci.impl.TinyUDecMath.ERR_VALUE
import com.github.labai.deci.impl.TinyUDecMath.MAX_INT_LEN
import com.github.labai.deci.impl.TinyUDecMath.MAX_UNSCALED
import com.github.labai.deci.impl.TinyUDecMath.TWOINT_ERR
import com.github.labai.deci.impl.TinyUDecMath.makeDec30
import com.github.labai.deci.impl.TinyUDecMath.makeDec30Compact
import java.math.BigDecimal

/*
 * Tiny decimal, stored in Int32.
 * Can keep 9 digits (max 1_073_741_824, but will use max 999_999_999)
 *
 * Int (32 bits), where:
 *  - 2 bits for decimal point position (->3 digits)
 *  - last 30 bits for value
 *  - unsigned (no negative values)
 *
 * Default version of TinyDec:
 *  - number of decimals (scale) can be from 0 to 3.
 *  - total count of digits (precision) still max 9.
 *
 * Example of correct numbers
 *   0
 *   12345678.9
 *   123456.789
 *   0.001 (min non-zero)
 *   999999999 (max)
 *
 * Invalid numbers
 *   0.0123      - too many decimals
 *   1234567890  - too big
 *   12345.12345 - too many digits
 *   -1          - negatives are not supported
 */
@Suppress("NOTHING_TO_INLINE", "OVERRIDE_BY_INLINE")
@JvmInline
internal value class TinyDec(
    internal val raw: Int
) : ITinyDec<TinyDec> {
    override inline fun pos() = raw ushr 30
    override inline fun unscaled() = raw and TinyUDecMath.MASK_VALUE
    override inline fun isZero(): Boolean = unscaled() == 0
    override inline fun isErr(): Boolean = raw == ERR_VALUE
    override inline fun isValid(): Boolean = raw != ERR_VALUE
    override fun toString(): String = TinyUDecMath.toString(unscaled(), pos())
    fun trimTrailingZeros(): TinyDec = trimTrailingZeros(this)
    fun toBigDecimal(): BigDecimal = toBigDecimal(this)

    companion object {
        val ERR = TinyDec(ERR_VALUE)
        val ZERO = TinyDec(0)
        private const val maxPos = 3

        fun parseString(str: String): TinyDec {
            val pair = TinyUDecMath.parseString(str, MAX_INT_LEN, 0, maxPos, false)
            return buildTiny(pair.first(), pair.second())
        }

        fun parseStringOrErr(str: String): TinyDec {
            val pair = TinyUDecMath.parseString(str, MAX_INT_LEN, 0, maxPos, true)
            if (pair.isErr())
                return ERR
            return buildTinyOrErr(pair.first(), pair.second())
        }

        fun valueOf(int: Int): TinyDec {
            return buildTinyOrErr(int, 0)
        }

        fun valueOf(long: Long): TinyDec {
            if (long !in 0..MAX_UNSCALED)
                return ERR
            return buildTinyOrErr(long.toInt(), 0)
        }

        fun buildTinyOrErr(value: Int, pos: Int): TinyDec {
            if (pos !in 0..maxPos)
                return ERR
            if (value !in 1..MAX_UNSCALED)
                return if (value == 0) ZERO else ERR
            return TinyDec(makeDec30(value, pos))
        }

        fun buildTiny(unscaled: Int, pos: Int): TinyDec {
            require(pos in 0..maxPos) { "Pos must be in 0..$maxPos ($pos)" }
            if (unscaled !in 1..MAX_UNSCALED) {
                if (unscaled == 0)
                    return ZERO
                throw IllegalArgumentException("Value is too large ($unscaled)")
            }
            return TinyDec(makeDec30(unscaled, pos))
        }

        fun trimTrailingZeros(tiny: TinyDec): TinyDec {
            require(tiny.isValid()) { "Invalid tinyDec value (err)" }
            return TinyDec(makeDec30Compact(tiny.unscaled(), tiny.pos()))
        }

        fun toBigDecimal(tiny: TinyDec): BigDecimal {
            require(tiny.isValid()) { "Invalid tinyDec value (err)" }
            return BigDecimal.valueOf(tiny.unscaled().toLong(), tiny.pos())
        }
    }
}
