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

import com.github.labai.deci.impl.Tiny2iUtils.TWOINT_ERR
import com.github.labai.deci.impl.TinyDec.Companion.ERR
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import kotlin.test.assertTrue

class Tiny2iUtilsTest {


    @ParameterizedTest
    @CsvSource(
        "3,     5,      80000, 4",
        "0,     0,      0,     4",
        "0,     5,      50000, 4",
        "5,     0,      50000, 4",
        "1.10,  2.20,   33000, 4",
        "1.1,   2.02,   31200, 4",
        "1.1,   2,      31000, 4",
        "1.1,   0,      11000, 4",
        "1.5,   2.6,    41000, 4",
        "1,     0.001,  10010, 4",
        "0.001, 0.002,     30, 4",
        "1.5,   1.5,    30000, 4",
        "0.25,  0.75,   10000, 4",
        "90000, 900, 909000000, 4",
    )
    fun test_add_scaled(a: String, b: String, expUnscaled: Int, expPos: Int) {
        val a1: TinyDec = TinyDec.parseString(a)
        val b1: TinyDec4d = TinyDec4d.parseString(b)
        val res1 = Tiny2iUtils.addOrErr(a1.unscaled(), a1.pos(), b1.unscaled(), b1.pos())
        assertEquals(expUnscaled, res1.first())
        assertEquals(expPos, res1.second())

        val a2: TinyDec4d = TinyDec4d.parseString(a)
        val b2: TinyDec = TinyDec.parseString(b)
        val res2 = Tiny2iUtils.addOrErr(a2.unscaled(), a2.pos(), b2.unscaled(), b2.pos())
        assertEquals(expUnscaled, res2.first())
        assertEquals(expPos, res2.second())

        val a3: TinyDec4d = TinyDec4d.parseString(a)
        val b3: TinyDec4d = TinyDec4d.parseString(b)
        val res3 = Tiny2iUtils.addOrErr(a3.unscaled(), a3.pos(), b3.unscaled(), b3.pos())
        assertEquals(expUnscaled, res3.first())
        assertEquals(expPos, res3.second())

    }

    @ParameterizedTest
    @CsvSource(
        "900000, 900",
        "0,   ERR",
        "0.1, ERR",
        "ERR, 0",
        "ERR, 0.1",
        "ERR, ERR",
    )
    fun test_addsub_invalid(a: String, b: String) {
        val a1: TinyDec = if (a == "ERR") ERR else TinyDec.parseString(a)
        val b1: TinyDec4d = if (b == "ERR") TinyDec4d.ERR else TinyDec4d.parseString(b)
        val res1 = Tiny2iUtils.addOrErr(a1.unscaled(), a1.pos(), b1.unscaled(), b1.pos())
        assertEquals(TWOINT_ERR, res1)

        val res2 = Tiny2iUtils.subOrErr(a1.unscaled(), a1.pos(), b1.unscaled(), b1.pos())
        assertEquals(TWOINT_ERR, res2)
    }

    @ParameterizedTest
    @CsvSource(
        "8,     3,      50000, 4",
        "0,     0,          0, 4",
        "5,     0,      50000, 4",
        "5,     5,          0, 4",
        "3.3,   1.10,   22000, 4",
        "3.12,  1.1,    20200, 4",
        "3.1,   1.1,    20000, 4",
        "1.1,   1.1,        0, 4",
        "4.1,   1.5,    26000, 4",
        "1.001, 0.001,  10000, 4",
        "0.003, 0.001,     20, 4",
        "3.5,   1.5,    20000, 4",
        "1.25,  0.25,   10000, 4",
    )
    fun test_sub_tiny4(a: String, b: String, expUnscaled: Int, expPos: Int) {
        val a1: TinyDec = TinyDec.parseString(a)
        val b1: TinyDec4d = TinyDec4d.parseString(b)
        val res1 = Tiny2iUtils.subOrErr(a1.unscaled(), a1.pos(), b1.unscaled(), b1.pos())
        assertEquals(expUnscaled, res1.first())
        assertEquals(expPos, res1.second())

        val a2: TinyDec4d = TinyDec4d.parseString(a)
        val b2: TinyDec = TinyDec.parseString(b)
        val res2 = Tiny2iUtils.subOrErr(a2.unscaled(), a2.pos(), b2.unscaled(), b2.pos())
        assertEquals(expUnscaled, res2.first())
        assertEquals(expPos, res2.second())

        val a3: TinyDec4d = TinyDec4d.parseString(a)
        val b3: TinyDec4d = TinyDec4d.parseString(b)
        val res3 = Tiny2iUtils.subOrErr(a3.unscaled(), a3.pos(), b3.unscaled(), b3.pos())
        assertEquals(expUnscaled, res3.first())
        assertEquals(expPos, res3.second())
    }

    //
    // helpers
    //

    private fun dec4(s: String): TinyDec4d {
        if (s == "ERR")
            return TinyDec4d.ERR
        return TinyDec4d.parseString(s)
    }

    private fun dec0(s: String): TinyDec {
        if (s == "ERR")
            return ERR
        return TinyDec.parseString(s)
    }

    private fun assertDecEquals(dec1: TinyDec4d, dec2: TinyDec4d) {
        val comp = TinyUDecMath.compare(dec1.unscaled(), dec1.pos(), dec2.unscaled(), dec2.pos())
        assertTrue(comp == 0, "Decimals are not equal ($dec1 vs $dec2)")
    }
}
