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
import platform.Foundation.NSDecimalNumber
import kotlin.test.Test

/**
 * @author Augustus
 * created on 2026-01-04
 */
class UtilsTest {
    val testHelper = UtilsTestHelper(
        { s -> NSDecimalNumber(s) },
        { d -> d.decimal },
    )

    @Test
    fun ios_calcScale() {
        testHelper.CalcScaleTestHelper { d, ctx -> Utils.calcScale(d, ctx) }
            .testCalcScale()
    }

    @Test
    fun ios_toRoundedDeci_withRounding() {
        val t = testHelper.RoundedDeciTestHelper(
            { d, ctx -> Utils.toRoundedDeci(d, ctx) },
            { ctx, roundToScale -> ctx.withConfig(DeciContextConfig(roundToScale = roundToScale)) },
        )
        t.testWithNoRound()
        t.testWithRounding()
    }

    @Test
    fun ios_utils_round() {
        val t = testHelper.UtilsRoundTestHelper { d, scale, ctx -> Utils.round(d, scale, ctx) }
        t.testUtilsRound()
    }
}
