/*
MIT License
Copyright (c) 2026 Augustus
*/

package com.github.labai.deci;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteOrder;

/**
 * @author Augustus
 * created on 2026-07-01
 *
 * parseString is faster than standard BigDecimal parser
 *
 */
class BigDecimalUtils {

    private BigDecimalUtils() {
    }

    private static final short[] DEC_HEX_MAP = buildDecHexMap();

    private static short[] buildDecHexMap() {
        short[] map = new short[2458];
        for (int i = 0; i <= 9; i++) {
            for (int j = 0; j <= 9; j++) {
                for (int k = 0; k <= 9; k++) {
                    map[i * 256 + j * 16 + k] = (short) (i * 100 + j * 10 + k);
                }
            }
        }
        return map;
    }

    private static final long[] TEN_POW = {
        1L,                    // 0 / 10^0
        10L,                   // 1 / 10^1
        100L,                  // 2 / 10^2
        1000L,                 // 3 / 10^3
        10000L,                // 4 / 10^4
        100000L,               // 5 / 10^5
        1000000L,              // 6 / 10^6
        10000000L,             // 7 / 10^7
        100000000L,            // 8 / 10^8
        1000000000L,           // 9 / 10^9
        10000000000L,          // 10 / 10^10
        100000000000L,         // 11 / 10^11
        1000000000000L,        // 12 / 10^12
        10000000000000L,       // 13 / 10^13
        100000000000000L,      // 14 / 10^14
        1000000000000000L,     // 15 / 10^15
        10000000000000000L,    // 16 / 10^16
        100000000000000000L,   // 17 / 10^17
        1000000000000000000L,  // 18 / 10^18
    };

    private static final VarHandle LONG_BE_VH = MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.BIG_ENDIAN);

    static BigDecimal parseString(String str) {
        if (str.isEmpty()) return null;
        return parseStringOrCharArray(str, null, 0, str.length());
    }

    static BigDecimal parseCharArray(char[] chars, int offset, int length) {
        if (length == 0 || chars.length < offset + length || offset < 0)
            return null;
        return parseStringOrCharArray(null, chars, offset, length);
    }

    // decode a 16-nibble packed-decimal long into its binary value.
    private static long nibblesToBinary(long nibbleLong) {
        long result = nibbleLong >>> 60;
        int shiftAmt = 48;
        do {
            result = result * 1000 + DEC_HEX_MAP[((int) (nibbleLong >>> shiftAmt) & 0xFFF)];
            shiftAmt -= 12;
        } while (shiftAmt >= 0);
        return result;
    }

    // result = highVal * 10^lowDigits + lowVal
    private static BigInteger mergeToBigInteger(long highVal, long lowVal, int lowDigits, boolean negative) {
        long mult = TEN_POW[lowDigits];
        long prodLow = highVal * mult;
        long prodHigh = Math.multiplyHigh(highVal, mult);
        long sumLow = prodLow + lowVal;
        long carry = (Long.compareUnsigned(sumLow, prodLow) < 0) ? 1L : 0L;
        long sumHigh = prodHigh + carry;

        // copy longs to bytes
        byte[] bytes = new byte[16];
        // alternative naive approach is slightly slower (few %), but can work on Java 1.8
        // for (int i = 0; i < 8; i++) {bytes[i] = (byte) (sumHigh >>> (8 * (7 - i)));}
        // for (int i = 0; i < 8; i++) {bytes[8 + i] = (byte) (sumLow >>> (8 * (7 - i)));}

        // copy longs to bytes
        LONG_BE_VH.set(bytes, 0, sumHigh);
        LONG_BE_VH.set(bytes, 8, sumLow);
        return new BigInteger(negative ? -1 : 1, bytes);
    }

    // NB: supports maxDigits <= 32 (two 16-nibble long buffers).
    private static BigDecimal parseStringOrCharArray(
        String str,
        char[] chars,
        int offset,
        int length
    ) {

        boolean useString = str != null;
        int start = offset;
        boolean negative;
        char ch = useString ? str.charAt(offset) : chars[offset];
        switch (ch) {
            case '-':
                start = offset + 1;
                negative = true;
                break;
            case '+':
                start = offset + 1;
                negative = false;
                break;
            default:
                negative = false;
                break;
        }
        int endExcl = offset + length;

        int digitCount = 0;
        int dot = -1;
        long buf2 = 0L;
        boolean buf2used = false;
        long buf = 0L;
        for (int i = start; i < endExcl; i++) {
            char c = useString ? str.charAt(i) : chars[i];
            if (c >= '0' && c <= '9') {
                if (digitCount >= 32)
                    return null;
                buf = (buf << 4) | (c - '0');
                digitCount++;
                if (digitCount == 16) { // switch to 2 longs
                    buf2 = buf;
                    buf2used = true;
                    buf = 0L;
                }
            } else if (c == '.') {
                if (dot >= 0)
                    return null;
                dot = digitCount;
            } else {
                return null;
            }
        }

        // trim trailing zeros
        if (dot >= 0) {
            while (digitCount > dot && (buf & 0xF) == 0L && digitCount > 16) {
                buf = buf >>> 4;
                digitCount--;
            }
            if (buf2used && digitCount == 16 && digitCount > dot) { // 'buf' consists only of trailing zeros, continue as with single 'buf'
                buf = buf2;
                buf2 = 0L;
                buf2used = false;
            }
            while (digitCount > dot && (buf & 0xF) == 0L) {
                buf = buf >>> 4;
                digitCount--;
            }
        }

        if (digitCount == 0)
            return null;
        int pos = (dot < 0) ? 0 : Math.max(digitCount - dot, 0);
        long lowVal = nibblesToBinary(buf);

        if (!buf2used) {
            return BigDecimal.valueOf(negative ? -lowVal : lowVal, pos);
        }

        long highVal = nibblesToBinary(buf2);
        int lowDigits = Math.max(digitCount - 16, 0);
        BigInteger unscaled = mergeToBigInteger(highVal, lowVal, lowDigits, negative);

        return new BigDecimal(unscaled, pos);
    }
}
