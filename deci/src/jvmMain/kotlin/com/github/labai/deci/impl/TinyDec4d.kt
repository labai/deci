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
import com.github.labai.deci.impl.TinyUDecMath.POW
import com.github.labai.deci.impl.TinyUDecMath.TWOINT_ERR
import com.github.labai.deci.impl.TinyUDecMath.makeDec30Compact
import java.math.BigDecimal

/*
 * Version of TinyDec, when number of decimals can be 4..7.
 * Total count of digits still max 9.
 *
 * Example of possible numbers
 *   0
 *   0.1234567
 *   0.0000001
 *   0.0001
 *   0.12345
 *   12.3456789
 *   12345.6789
 *   99999.9999
 *   99.9999999
 *
 * Invalid numbers
 *   0.01234567  - too many decimals
 *   123456      - too big, max 99999
 *   12345.12345 - too many digits
 *   -1          - negatives are not supported
 *
 */
@Suppress("NOTHING_TO_INLINE", "OVERRIDE_BY_INLINE")
@JvmInline
internal value class TinyDec4d (
    internal val raw: Int
) : ITinyDec<TinyDec4d> {
    override inline fun pos() = (raw ushr 30) + 4
    override inline fun unscaled() = raw and TinyUDecMath.MASK_VALUE
    override fun isZero(): Boolean = unscaled() == 0
    override inline fun isErr(): Boolean = raw == ERR_VALUE
    override inline fun isValid(): Boolean = raw != ERR_VALUE
    override fun toString(): String = TinyUDecMath.toString(unscaled(), pos())
    fun trimTrailingZeros(): TinyDec4d = trimTrailingZeros(this)
    fun toBigDecimal(): BigDecimal = toBigDecimal(this)

    companion object {
        val ERR = TinyDec4d(ERR_VALUE)
        val ZERO = TinyDec4d(0)
        private const val minPos = 4
        internal const val maxPos = 7

        fun parseString(str: String): TinyDec4d {
            val pair = TinyUDecMath.parseString(str, MAX_INT_LEN, 0, maxPos, false)
            return buildTiny4d(pair.first(), pair.second())
        }

        fun parseStringOrErr(str: String): TinyDec4d {
            val pair = TinyUDecMath.parseString(str, MAX_INT_LEN, 0, maxPos, true)
            if (pair == TWOINT_ERR)
                return ERR
            return buildTiny4dOrErr(pair.first(), pair.second())
        }

        fun valueOf(int: Int): TinyDec4d {
            return buildTiny4dOrErr(int, 0)
        }

        fun valueOf(long: Long): TinyDec4d {
            if (long !in 0..MAX_UNSCALED)
                return ERR
            return buildTiny4dOrErr(long.toInt(), 0)
        }

        fun buildTiny4dOrErr(unscaled: Int, pos: Int): TinyDec4d {
            if (pos !in minPos..maxPos) {
                if (pos in 0..<minPos) {
                    if (unscaled == 0)
                        return ZERO
                    val shifted: Long = unscaled.toLong() * POW[minPos - pos]
                    if (shifted in 1..MAX_UNSCALED)
                        return makeTinyDec4d(shifted.toInt(), minPos)
                }
                return ERR
            }
            if (unscaled !in 1..MAX_UNSCALED)
                return if (unscaled == 0) ZERO else ERR
            return makeTinyDec4d(unscaled, pos)
        }

        fun buildTiny4d(unscaled: Int, pos: Int): TinyDec4d {
            if (pos !in minPos..maxPos) {
                if (pos in 0..<minPos) {
                    if (unscaled == 0)
                        return ZERO
                    val shifted: Long = unscaled.toLong() * POW[minPos - pos]
                    if (shifted !in 0..MAX_UNSCALED)
                        throw IllegalArgumentException("Value is too large for precision ($unscaled:$pos)")
                    return makeTinyDec4d(shifted.toInt(), minPos)
                }
                throw IllegalArgumentException("Pos must be in 0..$maxPos ($pos) ")
            }
            if (unscaled !in 1..MAX_UNSCALED) {
                if (unscaled == 0)
                    return ZERO
                throw IllegalArgumentException("Value is too large ($unscaled)")
            }
            return makeTinyDec4d(unscaled, pos)
        }

        inline fun makeTinyDec4d(unscaled: Int, pos: Int): TinyDec4d {
            return TinyDec4d((pos shl 30) or unscaled)
        }

        fun trimTrailingZeros(tiny: TinyDec4d): TinyDec4d {
            require(tiny.isValid()) { "Invalid tinyDec value (err)" }
            val posAdjusted = tiny.pos() - 4 // makeDec30Compact will do an Int for TinyDec, which is same as TinyDec4d with -4 pos
            val raw = makeDec30Compact(tiny.unscaled(), posAdjusted)
            return TinyDec4d(raw)
        }

        fun toBigDecimal(tiny: TinyDec4d): BigDecimal {
            require(tiny.isValid()) { "Invalid tinyDec value (err)" }
            return BigDecimal.valueOf(tiny.unscaled().toLong(), tiny.pos())
        }
    }
}
