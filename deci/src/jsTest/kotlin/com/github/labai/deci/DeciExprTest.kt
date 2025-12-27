/*
MIT License

Copyright (c) 2023 Augustus

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
package com.github.labai.deci

import com.github.labai.deci.RoundingMode.HALF_UP
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * @author Augustus
 *         created on 2023.11.09
 */
class DeciExprTest {

    @Test
    fun js_deciContext_useFromDeciExpr_biggerScale__jsVersion() {
        val ctx4 = DeciContext(scale = 4, roundingMode = HALF_UP, precision = 3)
        val ctx40 = DeciContext(scale = 40, roundingMode = HALF_UP, precision = 30)

        // if bigger precision - also apply from deciExpr
        val num: Deci? = Deci.valueOf("1.012345", ctx4)
        val dec = deciExpr(ctx40) { num * "1.0123465789".deci }
        assertEquals(ctx40, dec!!.deciContext)
        // real results:
        // 1.0123 x 1.0123465789 = 1.02479844182047 (jvm)
        // 1.012345 x 1.0123465789 = 1.0248439974165205 (js)
        // i.e. here we use bigger precision (different from JVM)
        assertEquals("1.0248439974165205", dec.toString())
    }
}
