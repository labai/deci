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

import com.github.labai.deci.DeciContextConfig
import com.github.labai.deci.impl.UtilsTestHelper.CTX4
import platform.Foundation.NSDecimalNumber
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * @author Augustus
 * created on 2026-01-04
 */
class UtilsTest {

    @Test
    fun ios_calcScale() {
        fun checkScale(decStr: String, expectedScale: Int) {
            val d = NSDecimalNumber(decStr)
            val s = Utils.calcScale(d, CTX4)
            assertEquals(expectedScale, s, "scale for $decStr ")
        }

        val cases = UtilsTestHelper.calcScale_testCases()
        for ((decStr, expectedScale) in cases) {
            checkScale(decStr, expectedScale)
        }
    }

    @Test
    fun ios_toRoundedDeci_withRounding() {

        fun checkScaleRound(decStr: String, expectedStr: String) {
            val ctx = CTX4.withConfig(DeciContextConfig(roundToScale = true))
            val d = NSDecimalNumber(decStr)
            val dec = Utils.toRoundedDeci(d, ctx)
            assertEquals(expectedStr, dec.decimal.toString(), "for $decStr")
        }

        val cases = UtilsTestHelper.toRoundedDeci_rounded_testCases()
        for ((decStr, expectedScale) in cases) {
            checkScaleRound(decStr, expectedScale)
        }
    }

    @Test
    fun ios_toRoundedDeci_withNoRound() {

        fun checkScaleNoRound(decStr: String, expectedStr: String) {
            val ctx = CTX4.withConfig(DeciContextConfig(roundToScale = false))
            val d = NSDecimalNumber(decStr)
            val dec = Utils.toRoundedDeci(d, ctx)
            assertEquals(expectedStr, dec.decimal.toString(), "for $decStr")
        }

        val cases = UtilsTestHelper.toRoundedDeci_notRounded_testCases()
        for ((decStr, expectedScale) in cases) {
            checkScaleNoRound(decStr, expectedScale)
        }
    }

    @Test
    fun ios_utils_round() {
        fun checkRound(decStr: String, scale: Int, expectedStr: String) {
            val d = NSDecimalNumber(decStr)
            val dec = Utils.round(d, scale, CTX4)
            assertEquals(expectedStr, dec.decimal.toString(), "for $decStr")
        }

        val cases = UtilsTestHelper.utilsRound_testCases()
        for ((decStr, scale, expectedStr) in cases) {
            checkRound(decStr, scale, expectedStr)
        }
    }
}
