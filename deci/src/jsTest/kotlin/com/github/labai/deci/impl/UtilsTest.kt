package com.github.labai.deci.impl

import com.github.labai.deci.DeciContext
import com.github.labai.deci.DeciContextConfig
import com.github.labai.deci.DecimalJsFactory
import com.github.labai.deci.RoundingMode.HALF_UP
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * @author Augustus
 *   created on 2026-01-04
 */
class UtilsTest {

    @Test
    fun js_calcScale() {
        val ctx4 = DeciContext(scale = 4, roundingMode = HALF_UP, precision = 3)
        fun checkScale(decStr: String, expectedScale: Int) {
            val d = DecimalJsFactory.createDecimalJs(decStr)
            val s = Utils.calcScale(d, ctx4)
            assertEquals(expectedScale, s, "scale for $decStr ")
        }
        checkScale("10", 4)
        checkScale("1", 4)
        checkScale("1.1", 4)
        checkScale("0.1", 4)
        checkScale("0.01", 4) // 0.0100 = scale = 1(0) + 3(precision)
        checkScale("0.012345", 4)
        checkScale("0.0012345", 5) // 2(00) + 3(precision)
        checkScale("0.00012345", 6) // 3(000) + 3(precision)
    }

    @Test
    fun js_toRoundedDeci_withRounding() {
        val ctx4 = DeciContext(scale = 4, roundingMode = HALF_UP, precision = 3)
            .withConfig(DeciContextConfig(roundToScale = true))

        fun checkScaleRound(decStr: String, expectedStr: String) {
            val d = DecimalJsFactory.createDecimalJs(decStr)
            val dec = Utils.toRoundedDeci(d, ctx4)
            assertEquals(expectedStr, dec.decimal.toString(), "for $decStr")
        }

        checkScaleRound("10", "10")
        checkScaleRound("1", "1")
        checkScaleRound("0.1", "0.1")
        checkScaleRound("0.012345", "0.0123")
        checkScaleRound("-0.0012345", "-0.00123")
    }

    @Test
    fun js_toRoundedDeci_withNoRound() {
        val ctx4 = DeciContext(scale = 4, roundingMode = HALF_UP, precision = 3)
            .withConfig(DeciContextConfig(roundToScale = false))

        fun checkScaleNoRound(decStr: String, expectedStr: String) {
            val d = DecimalJsFactory.createDecimalJs(decStr)
            val dec = Utils.toRoundedDeci(d, ctx4)
            assertEquals(expectedStr, dec.decimal.toString(), "for $decStr")
        }

        checkScaleNoRound("10", "10")
        checkScaleNoRound("1", "1")
        checkScaleNoRound("0.1", "0.1")
        checkScaleNoRound("0.012345", "0.012345")
        checkScaleNoRound("-0.0012345", "-0.0012345")
    }

    @Test
    fun js_utils_round() {
        val ctx4 = DeciContext(scale = 4, roundingMode = HALF_UP, precision = 3)
        fun checkRound(decStr: String, scale: Int, expectedStr: String) {
            val d = DecimalJsFactory.createDecimalJs(decStr)
            val dec = Utils.round(d, scale, ctx4)
            assertEquals(expectedStr, dec.decimal.toString(), "for $decStr")
        }

        checkRound("0.012345", 5, "0.01235")
        checkRound("0.012345", 4, "0.0123")
        checkRound("-0.012345", 5, "-0.01235")
    }
}
