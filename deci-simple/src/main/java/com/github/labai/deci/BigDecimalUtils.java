/*
MIT License
Copyright (c) 2026 Augustus
*/
package com.github.labai.deci;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteOrder;

/*
 * @author Augustus
 * created on 2026-07-01
 *
 * parseString is faster than standard BigDecimal parser
 *
 * decimalToString is faster than standard BigDecimal.toPlainString() with trim
 *
 * (for numbers see DeciPerfTest)
 *
 */
class BigDecimalUtils {

    private BigDecimalUtils() {
    }

    private static final short[] DEC_HEX_MAP = buildDecHexMap();

    private static final BigInteger TEN_POW_16 = BigInteger.valueOf(10_000_000_000_000_000L);
    private static final BigInteger LIMIT_32_DIGITS = BigInteger.TEN.pow(32);
    private static final int[] DIGIT_PAIRS_INT = buildDigitPairsAsInt();

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

    private static int[] buildDigitPairsAsInt() {
        int[] pairs = new int[100];
        for (int i = 0; i < 100; i++) {
            int a = '0' + (i / 10);
            int b = '0' + (i % 10);
            pairs[i] = (a << 16) | b;
        }
        return pairs;
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

    @Nullable
    static BigDecimal parseString(@Nullable String str) {
        if (str == null || str.isEmpty()) return null;
        return parseStringOrCharArray(str, null, 0, str.length());
    }

    @Nullable
    static BigDecimal parseCharArray(@NotNull char[] chars, int offset, int length) {
        if (length == 0 || chars.length < offset + length || offset < 0)
            return null;
        return parseStringOrCharArray(null, chars, offset, length);
    }

    // return decimal in plain string, with trimmed trailing zeros,
    // (or null in exceptional cases, e.g. number is bigger than 32 digits)
    @Nullable
    static String decimalToString(@NotNull BigDecimal decimal) {
        int scale = decimal.scale();
        int precision = decimal.precision();
        if (scale < 0)
            return null;

        BigInteger unscaled = decimal.unscaledValue();
        boolean negative = unscaled.signum() < 0;
        unscaled = negative ? unscaled.negate() : unscaled;
        boolean leadingIntZero = scale >= precision;
        int leadingZeros = Math.max(scale - precision, 0);
        int extraSpace = (scale > 0 ? 1 : 0) + (negative ? 1 : 0) + (leadingIntZero ? 1 : 0);

        int bitLength = unscaled.bitLength();

        if (bitLength >= 107 && unscaled.compareTo(LIMIT_32_DIGITS) >= 0)
            return null;

        // single long
        if (bitLength <= 63) {
            long v = unscaled.longValueExact();
            char[] buffer = new char[decimalDigitCount(v) + extraSpace + leadingZeros];
            int dotPos = scale > 0 ? buffer.length - scale - 1 : -1;
            writeDigitsOf1Long(v, buffer, dotPos, negative);
            return prepareString(buffer, dotPos, negative);
        }

        // two longs
        BigInteger[] qr = unscaled.divideAndRemainder(TEN_POW_16);
        long high = qr[0].longValueExact(); // <= 16 digits
        long low = qr[1].longValueExact();  // 16 digits
        int highDigits = decimalDigitCount(high);
        char[] buffer = new char[highDigits + 16 + extraSpace + leadingZeros];
        int dotPos = scale > 0 ? buffer.length - scale - 1 : -1;
        writeDigitsOf2Longs(high, low, buffer, dotPos, negative);
        return prepareString(buffer, dotPos, negative);
    }

    @NotNull
    static String trimTrailingZeros(@NotNull String str) {
        int end = -1;
        boolean stripping = true;
        boolean dotFound = false;

        for (int i = str.length() - 1; i >= 0; i--) {
            char c = str.charAt(i);

            if (c == '.') {
                dotFound = true;
                if (stripping)
                    end = i;
                break;
            }

            if (stripping) {
                if (c == '0') {
                    end = i;
                } else {
                    stripping = false;
                }
            }
        }

        return dotFound && end != -1 ? str.substring(0, end) : str;
    }

    //
    // private
    //

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
            int n = c - '0';
            if (n >= 0 && n < 10) {
                if (digitCount >= 32)
                    return null;
                buf = (buf << 4) | n;
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
            while (digitCount > dot && digitCount > 16 && (buf & 0xF) == 0L) {
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

    private static int decimalDigitCount(long x) {
        if (x < 10000L) {
            if (x < 10L) return 1;
            if (x < 100L) return 2;
            if (x < 1000L) return 3;
            return 4;
        }
        if (x < 10000000000L) {
            if (x < 100000L) return 5;
            if (x < 1000000L) return 6;
            if (x < 10000000L) return 7;
            if (x < 100000000L) return 8;
            if (x < 1000000000L) return 9;
            return 10;
        }
        if (x < 100000000000L) return 11;
        if (x < 1000000000000L) return 12;
        if (x < 10000000000000L) return 13;
        if (x < 100000000000000L) return 14;
        if (x < 1000000000000000L) return 15;
        if (x < 10000000000000000L) return 16;
        if (x < 100000000000000000L) return 17;
        if (x < 1000000000000000000L) return 18;
        return 19;
    }

    // write digits from single long to buffer, reserve place for dot
    private static void writeDigitsOf1Long(long value, char[] buf, int dotPos, boolean negative) {
        int startPos = writeLongDigits(value, buf, buf.length - 1, dotPos);
        fillZeros(buf, (negative ? 1 : 0), startPos);
    }

    // write digits from 2 longs to buffer, reserve place for dot
    private static void writeDigitsOf2Longs(long high, long low, char[] buf, int dotPos, boolean negative) {
        int pos = writeLongDigits(low, buf, buf.length - 1, dotPos);
        int end = buf.length - 1 - 16;
        if (dotPos >= end) // include dot
            end--;
        fillZeros(buf, end, pos);

        pos = writeLongDigits(high, buf, end, dotPos);
        fillZeros(buf, (negative ? 1 : 0), pos);
    }

    // write digits from long to buffer, reserve place for dot
    private static int writeLongDigits(long value, char[] buf, int end, int dotPos) {
        int pos = end;
        while (value >= 100) {
            int idx = (int) (value % 100);
            value /= 100;
            int p = DIGIT_PAIRS_INT[idx];
            buf[pos--] = (char) (p);
            if (dotPos == pos)
                pos--;
            buf[pos--] = (char) (p >>> 16);
            if (dotPos == pos)
                pos--;
        }
        if (value > 0) {
            int idx = ((int) value);
            int p = DIGIT_PAIRS_INT[idx];
            buf[pos--] = (char) (p);
            if (dotPos == pos)
                pos--;
            if (pos >= 0) {
                buf[pos--] = (char) (p >>> 16);
                if (dotPos == pos)
                    pos--;
            }
        }
        return pos;
    }

    private static void fillZeros(char[] buf, int startIncl, int endIncl) {
        for (int i = startIncl; i <= endIncl; i++) {
            buf[i] = '0';
        }
    }

    // prepare decimal string from buffer of digits,
    // finalize by adding minus, dot, also trim trailing zeros
    private static String prepareString(char[] buf, int dotPos, boolean negative) {
        // trim trailing zeros
        int end;
        if (dotPos >= 0) {
            //noinspection StatementWithEmptyBody
            for (end = buf.length - 1; end > dotPos && buf[end] == '0'; end--)
                ;
            if (end == dotPos)
                end--;
        } else {
            end = buf.length - 1;
        }

        if (negative) {
            buf[0] = '-';
        }

        if (dotPos >= 0) {
            buf[dotPos] = '.';
        }

        return String.valueOf(buf, 0, end + 1);
    }
}
