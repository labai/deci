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
@Suppress("NOTHING_TO_INLINE")
@JvmInline
internal value class TinyDec (
    internal val raw: Int
) : Comparable<TinyDec> {
    inline fun pos() = raw ushr 30
    inline fun unscaled() = raw and TinyUDecMath.MASK_VALUE
    internal inline fun getPow10() = TinyUDecMath.POW[pos()]
    fun add(other: TinyDec) = TinyUDecMath.addOrErr(this, other)
    fun sub(other: TinyDec) = TinyUDecMath.subOrErr(this, other)
    fun mul(other: TinyDec) = TinyUDecMath.mulOrErr(this, other)
    fun tryDiv(other: TinyDec) = TinyUDecMath.tryDivOrErr(this, other) // support only few cases w/o rounding
    fun rem(other: TinyDec) = TinyUDecMath.remOrErr(this, other)
    fun round(scale: Int, roundingMode: RoundingMode) = TinyUDecMath.round(this, scale, roundingMode)
    fun intPart() = TinyUDecMath.getIntPart(this)
    fun decPart() = TinyUDecMath.getDecPart(this)
    fun toBigDecimal(): BigDecimal = TinyUDecMath.toBigDecimal(this)
    override fun compareTo(other: TinyDec) = TinyUDecMath.compare(this, other)
    override fun toString(): String = TinyUDecMath.toString(this)
    fun isEqual(other: TinyDec): Boolean = TinyUDecMath.isEqual(this, other)
    internal fun trimTrailingZeros(): TinyDec = TinyUDecMath.trimTrailingZeros(this)
    internal fun isZero(): Boolean = unscaled() == 0

    companion object {
        fun buildTiny(unscaled: Int, pos: Int) = TinyUDecMath.buildTiny(unscaled, pos)
        fun buildTinyOrErr(unscaled: Int, pos: Int) = TinyUDecMath.buildTinyOrErr(unscaled, pos)
        fun parseString(str: String) = TinyUDecMath.parseString(str)
        fun parseStringOrErr(str: String) = TinyUDecMath.parseStringOrErr(str)
        fun valueOf(bigdec: BigDecimal): TinyDec = TinyUDecMath.convertToTinyOrErr(bigdec)
        fun valueOf(int: Int): TinyDec = TinyUDecMath.convertToTinyOrErr(int)
        fun valueOf(long: Long): TinyDec = TinyUDecMath.convertToTinyOrErr(long)
    }
}
