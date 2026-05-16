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
import com.github.labai.deci.impl.Tiny2iUtils.TWOINT_ERR
import com.github.labai.deci.impl.TinyUDecMath.ERR_VALUE
import com.github.labai.deci.impl.TinyUDecMath.MAX_INT_LEN
import com.github.labai.deci.impl.TinyUDecMath.MAX_UNSCALED
import com.github.labai.deci.impl.TinyUDecMath.POW
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
 * Not used yet
 *
 */
@Suppress("NOTHING_TO_INLINE", "OVERRIDE_BY_INLINE")
@JvmInline
internal value class TinyDec4d (
    internal val raw: Int
) : ITinyDec<TinyDec4d> {
    override inline fun pos() = (raw ushr 30) + 4
    override inline fun unscaled() = raw and TinyUDecMath.MASK_VALUE

    fun trimTrailingZeros(): TinyDec4d = trimTrailingZeros(this)

    override fun toBigDecimal(): BigDecimal {
        require(this.isValid()) { "Invalid tinyDec4d value (err)" }
        return BigDecimal.valueOf(unscaled().toLong(), pos())
    }
    override fun intPart() = TinyUDecMath.getIntPart(unscaled(), pos())
    override fun isZero(): Boolean = unscaled() == 0
    override fun isErr(): Boolean = raw == ERR_VALUE
    override fun isValid(): Boolean = raw != ERR_VALUE
    override fun toString(): String = TinyUDecMath.toString(unscaled(), pos())

    companion object {
        private val ZERO: TinyDec4d = TinyDec4d(0)
        internal val ERR: TinyDec4d = TinyDec4d(ERR_VALUE)
        private const val minPos = 4
        internal const val maxPos = 7

        fun valueOf(dec: BigDecimal): TinyDec4d {
            when (dec.signum()) {
                -1 -> return ERR
                0 -> return ZERO
            }
            if (dec.precision() > MAX_INT_LEN) {
                // may try to trimTrailingZeros, but that will create new objects, and small chance for success won't pay off (?)
                return ERR
            }

            if (dec.scale() > maxPos) {
                return ERR
            }

            val unscaled: Long
            val pos: Int
            if (dec.scale() <= 0) {
                unscaled = dec.toLong()
                pos = 0
            } else {
                unscaled = dec.unscaledValue().toLong() // will be intermediate BigInteger allocation (?)
                pos = dec.scale()
            }

            return if (unscaled > MAX_UNSCALED) ERR else buildTiny4dOrErr(unscaled.toInt(), pos)
        }

        fun buildTiny4dOrErr(unscaled: Int, pos: Int): TinyDec4d {
            if (unscaled !in 1..MAX_UNSCALED)
                return if (unscaled == 0) ZERO else ERR
            if (pos !in minPos..maxPos) {
                if (pos in 0..<minPos) {
                    val shifted: Long = unscaled.toLong() * POW[minPos - pos]
                    if (shifted <= MAX_UNSCALED)
                        return makeTinyDec4d(shifted.toInt(), minPos)
                }
                return ERR
            }
            return makeTinyDec4d(unscaled, pos)
        }

        fun buildTiny4d(unscaled: Int, pos: Int): TinyDec4d {
            if (unscaled !in 1..MAX_UNSCALED) {
                @Suppress("UNCHECKED_CAST")
                if (unscaled == 0)
                    return ZERO
                throw IllegalArgumentException("Value is too large ($unscaled)")
            }
            if (pos !in minPos..maxPos) {
                if (pos in 0..<minPos) {
                    val shifted: Long = unscaled.toLong() * POW[minPos - pos]
                    if (shifted >= MAX_UNSCALED)
                        throw IllegalArgumentException("Value is too large for precision ($unscaled:$pos)")
                    return makeTinyDec4d(shifted.toInt(), minPos)
                }
                throw IllegalArgumentException("Pos must be in $minPos..$maxPos ($pos) ")
            }

            return makeTinyDec4d(unscaled, pos)
        }

        inline fun makeTinyDec4d(unscaled: Int, pos: Int): TinyDec4d {
            return TinyDec4d((pos shl 30) or unscaled)
        }

        // will throw an exception in case of error
        fun parseString(str: String): TinyDec4d {
            val pair = Tiny2iUtils.parseString(str, MAX_INT_LEN, 0, maxPos, false)
            return buildTiny4d(pair.first(), pair.second())
        }

        fun parseStringOrErr(str: String): TinyDec4d {
            val pair = Tiny2iUtils.parseString(str, MAX_INT_LEN, 0, maxPos, true)
            if (pair == TWOINT_ERR)
                return ERR
            return buildTiny4dOrErr(pair.first(), pair.second())
        }

        fun trimTrailingZeros(tiny: TinyDec4d): TinyDec4d {
            require(tiny.isValid()) { "Invalid tinyDec value (err)" }
            val posAdjusted = tiny.pos() - 4 // makeDec30Compact will do an Int for TinyDec, which is same as TinyDec4d with -4 pos
            val raw = makeDec30Compact(tiny.unscaled(), posAdjusted)
            return TinyDec4d(raw)
        }
    }
}
