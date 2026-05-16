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
import com.github.labai.deci.impl.TinyUDecMath.ERR_VALUE
import com.github.labai.deci.impl.TinyUDecMath.MAX_UNSCALED
import com.github.labai.deci.impl.TinyUDecMath.divideAndRound

/**
 * @author Augustus
 * created on 2026-05-01
 *
 * for Deci
 *
 * Use this utility, when not sure about number of decimals in results
 * Then as result of those methods is TwoInt (in form of one Long)
 * then it can be converted to TinyDec or TinyDec4d, depending on decimal places
 *
 */
internal object Tiny2iUtils {
    internal val TWOINT_ERR = TwoInt(-1L)

    // to avoid creation of objects, will use long and put there 2 ints
    // this version for internal usage only!
    @Suppress("NOTHING_TO_INLINE")
    @JvmInline
    internal value class TwoInt (
        internal val long: Long
    ) {
        inline fun first() = (long ushr 32).toInt()
        inline fun second() = long.toInt()
        companion object {
            inline fun toTwoInt(first: Int, second: Int) = TwoInt((first.toLong() shl 32) or second.toLong())
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

    internal fun round(unscaled: Int, pos: Int, roundScale: Int, roundingMode: RoundingMode): TwoInt {
        if (unscaled == ERR_VALUE)
            return TWOINT_ERR
        if (pos <= roundScale)
            return TWOINT_ERR
        if (roundScale < 0)
            return TWOINT_ERR
        val drop = pos - roundScale
        val shrank = divideAndRound(unscaled, TinyUDecMath.POW[drop], roundingMode)
        return TwoInt.toTwoInt(shrank, roundScale)
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
            rval = aval.toLong() * TinyUDecMath.POW[bpos - apos] - bval
        } else {
            rpos = apos
            rval = aval.toLong() - bval.toLong() * TinyUDecMath.POW[apos - bpos]
        }
        if (rval !in 0..MAX_UNSCALED)
            return TWOINT_ERR
        return TwoInt.toTwoInt(rval.toInt(), rpos)
    }

}
