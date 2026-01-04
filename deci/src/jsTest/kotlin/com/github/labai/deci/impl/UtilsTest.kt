package com.github.labai.deci.impl

import com.github.labai.deci.DeciContextConfig
import com.github.labai.deci.DecimalJsFactory
import kotlin.test.Test

/**
 * @author Augustus
 * created on 2026-01-04
 */
class UtilsTest {
    val testHelper = UtilsTestHelper(
        { s -> DecimalJsFactory.createDecimalJs(s) },
        { d -> d.decimal },
    )

    @Test
    fun js_calcScale() {
        testHelper.CalcScaleTestHelper { d, ctx -> Utils.calcScale(d, ctx) }
            .testCalcScale()
    }

    @Test
    fun js_toRoundedDeci_withRounding() {
        val t = testHelper.RoundedDeciTestHelper(
            { d, ctx -> Utils.toRoundedDeci(d, ctx) },
            { ctx, roundToScale -> ctx.withConfig(DeciContextConfig(roundToScale)) }
        )
        t.testWithNoRound()
        t.testWithRounding()
    }

    @Test
    fun js_utils_round() {
        val t = testHelper.UtilsRoundTestHelper { d, scale, ctx -> Utils.round(d, scale, ctx) }
        t.testUtilsRound()
    }
}
