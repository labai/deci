package com.github.labai.deci.impl

import com.github.labai.deci.DeciContextConfig
import com.github.labai.deci.DecimalJsFactory
import com.github.labai.deci.impl.UtilsTestHelper.CTX4
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * @author Augustus
 * created on 2026-01-04
 */
class UtilsTest {

    @Test
    fun js_calcScale() {
        fun checkScale(decStr: String, expectedScale: Int) {
            val d = DecimalJsFactory.createDecimalJs(decStr)
            val s = Utils.calcScale(d, CTX4)
            assertEquals(expectedScale, s, "scale for $decStr ")
        }

        val cases = UtilsTestHelper.calcScale_testCases()
        for ((decStr, expectedScale) in cases) {
            checkScale(decStr, expectedScale)
        }
    }

    @Test
    fun js_toRoundedDeci_withRounding() {
        fun checkScaleRound(decStr: String, expectedStr: String) {
            val ctx = CTX4.withConfig(DeciContextConfig(roundToScale = true))
            val d = DecimalJsFactory.createDecimalJs(decStr)
            val dec = Utils.toRoundedDeci(d, ctx)
            assertEquals(expectedStr, dec.decimal.toString(), "for $decStr")
        }

        val cases = UtilsTestHelper.toRoundedDeci_rounded_testCases()
        for ((decStr, expectedScale) in cases) {
            checkScaleRound(decStr, expectedScale)
        }
    }

    @Test
    fun js_toRoundedDeci_withNoRound() {
        fun checkScaleNoRound(decStr: String, expectedStr: String) {
            val ctx = CTX4.withConfig(DeciContextConfig(roundToScale = false))
            val d = DecimalJsFactory.createDecimalJs(decStr)
            val dec = Utils.toRoundedDeci(d, ctx)
            assertEquals(expectedStr, dec.decimal.toString(), "for $decStr")
        }

        val cases = UtilsTestHelper.toRoundedDeci_notRounded_testCases()
        for ((decStr, expectedScale) in cases) {
            checkScaleNoRound(decStr, expectedScale)
        }
    }

    @Test
    fun js_utils_round() {
        fun checkRound(decStr: String, scale: Int, expectedStr: String) {
            val d = DecimalJsFactory.createDecimalJs(decStr)
            val dec = Utils.round(d, scale, CTX4)
            assertEquals(expectedStr, dec.decimal.toString(), "for $decStr")
        }

        val cases = UtilsTestHelper.utilsRound_testCases()
        for ((decStr, scale, expectedStr) in cases) {
            checkRound(decStr, scale, expectedStr)
        }
    }
}
