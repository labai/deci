package com.github.labai.deci.impl

import java.lang.invoke.MethodHandles
import java.lang.invoke.VarHandle
import java.math.BigDecimal
import java.math.BigInteger
import java.nio.ByteOrder

/**
 * @author Augustus
 * created on 2026-07-01
 */
object BigDecimalUtils {

    private val DEC_HEX_MAP: ShortArray = ShortArray(2458).also { map -> // size 9*256+9*16+10
        for (i in 0..9) for (j in 0..9) for (k in 0..9) {
            map[i * 256 + j * 16 + k] = (i * 100 + j * 10 + k).toShort()
        }
    }

    private val TEN_POW: LongArray = longArrayOf(
        1,  // 0 / 10^0
        10,  // 1 / 10^1
        100,  // 2 / 10^2
        1000,  // 3 / 10^3
        10000,  // 4 / 10^4
        100000,  // 5 / 10^5
        1000000,  // 6 / 10^6
        10000000,  // 7 / 10^7
        100000000,  // 8 / 10^8
        1000000000,  // 9 / 10^9
        10000000000L,  // 10 / 10^10
        100000000000L,  // 11 / 10^11
        1000000000000L,  // 12 / 10^12
        10000000000000L,  // 13 / 10^13
        100000000000000L,  // 14 / 10^14
        1000000000000000L,  // 15 / 10^15
        10000000000000000L,  // 16 / 10^16
        100000000000000000L,  // 17 / 10^17
        1000000000000000000L,  // 18 / 10^18
    )

    private val LONG_BE_VH: VarHandle = MethodHandles.byteArrayViewVarHandle(LongArray::class.java, ByteOrder.BIG_ENDIAN)

    internal fun parseString(str: String): BigDecimal? {
        if (str.isEmpty()) return null
        return parseStringOrCharArray({ str[it] }, 0, str.length)
    }

    internal fun parseCharArray(chars: CharArray, offset: Int, length: Int): BigDecimal? {
        if (length == 0 || chars.size < offset + length || offset < 0)
            return null
        return parseStringOrCharArray({ chars[it] }, offset, length)
    }

    // decode a 16-nibble packed-decimal Long into its binary value.
    private fun nibblesToBinary(nibbleLong: Long): Long {
        var result = (nibbleLong ushr 60) and 0xF
        var shift = 48
        do {
            result = result * 1000 + DEC_HEX_MAP[((nibbleLong ushr shift) and 0xFFF).toInt()]
            shift -= 12
        } while (shift >= 0)
        return result
    }

    // result = highVal * 10^lowDigits + lowVal
    private fun mergeToBigInteger(highVal: Long, lowVal: Long, lowDigits: Int, negative: Boolean): BigInteger {
        val mult = TEN_POW[lowDigits]
        val prodLow = highVal * mult
        val prodHigh = Math.multiplyHigh(highVal, mult)
        val sumLow = prodLow + lowVal
        val carry = if (java.lang.Long.compareUnsigned(sumLow, prodLow) < 0) 1L else 0L
        val sumHigh = prodHigh + carry

        val bytes = ByteArray(16)
        // alternative naive approach is slightly slower (few %)
        //   for (i in 0 until 8) bytes[i] = (sumHigh ushr (8 * (7 - i))).toByte()
        //   for (i in 0 until 8) bytes[8 + i] = (sumLow ushr (8 * (7 - i))).toByte()
        LONG_BE_VH.set(bytes, 0, sumHigh)
        LONG_BE_VH.set(bytes, 8, sumLow)
        return BigInteger(if (negative) -1 else 1, bytes)
    }

    // NB: supports maxDigits <= 32 (two 16-nibble Long buffers).
    private inline fun parseStringOrCharArray(
        charGetFn: (Int) -> Char,
        offset: Int,
        length: Int,
    ): BigDecimal? {

        var start = offset
        val negative: Boolean
        when (charGetFn(offset)) {
            '-' -> { start = offset + 1; negative = true }
            '+' -> { start = offset + 1; negative = false }
            else -> negative = false
        }
        val endExcl = offset + length

        var digitCount = 0
        var dot = -1
        var buf2 = 0L
        var buf2used = false
        var buf = 0L
        for (i in start until endExcl) {
            when (val c = charGetFn(i)) {
                in '0'..'9' -> {
                    if (digitCount >= 32)
                        return null
                    buf = (buf shl 4) + (c - '0')
                    digitCount++
                    if (digitCount == 16) { // switch to 2 longs
                        buf2 = buf
                        buf2used = true
                        buf = 0L
                    }
                }
                '.' -> {
                    if (dot >= 0)
                        return null
                    dot = digitCount
                }
                else -> return null
            }
        }

        // trim trailing zeros
        if (dot >= 0) {
            while (digitCount > dot && (buf and 0xF) == 0L && digitCount > 16) {
                buf = buf ushr 4
                digitCount--
            }
            if (buf2used && digitCount == 16 && digitCount > dot) { // 'buf' consists only of trailing zeros, continue as with single 'buf'
                buf = buf2
                buf2 = 0L
                buf2used = false
            }
            while (digitCount > dot && (buf and 0xF) == 0L) {
                buf = buf ushr 4
                digitCount--
            }
        }

        if (digitCount == 0)
            return null
        val pos = if (dot < 0) 0 else maxOf(digitCount - dot, 0)
        val lowVal = nibblesToBinary(buf)

        if (!buf2used) {
            return BigDecimal.valueOf(if (negative) -lowVal else lowVal, pos)
        }

        val highVal = nibblesToBinary(buf2)
        val lowDigits = maxOf(digitCount - 16, 0)
        val unscaled = mergeToBigInteger(highVal, lowVal, lowDigits, negative)

        return BigDecimal(unscaled, pos)
    }
}
