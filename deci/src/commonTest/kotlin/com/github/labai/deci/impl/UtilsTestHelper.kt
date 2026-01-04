package com.github.labai.deci.impl

import com.github.labai.deci.Deci
import com.github.labai.deci.DeciContext
import com.github.labai.deci.RoundingMode.HALF_UP
import kotlin.test.assertEquals

/**
 * @author Augustas
 * created on 2026-01-04
 *
 * Common test for JS and iOS for result rounding
 * (JVM doesn't need as it always use rounding base on deciContext).
 *
 * This is a way, how share same test between 2 platforms -
 * each (JS/iOS) will call those test from their test-wrappers
 *
 */
class UtilsTestHelper<T>(
    private val createDecimalFn: (String) -> T,
    private val getBackingDecimalFn: (Deci) -> T,
) {

    inner class CalcScaleTestHelper(
        private val calcScaleFn: (T, DeciContext) -> Int,
    ) {

        fun testCalcScale() {
            val ctx4 = DeciContext(scale = 4, roundingMode = HALF_UP, precision = 3)
            fun checkScale(decStr: String, expectedScale: Int) {
                val d = createDecimalFn(decStr)
                val s = calcScaleFn(d, ctx4)
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
    }

    inner class RoundedDeciTestHelper(
        private val toRoundedDeciFn: (T, DeciContext) -> Deci,
        private val prepareRoundDeciContext: (DeciContext, Boolean) -> DeciContext,
    ) {
        fun testWithRounding() {
            val ctx4 = DeciContext(scale = 4, roundingMode = HALF_UP, precision = 3)
            val ctx = prepareRoundDeciContext(ctx4, true)

            fun checkScaleRound(decStr: String, expectedStr: String) {
                val d = createDecimalFn(decStr)
                val dec = toRoundedDeciFn(d, ctx)
                val res = getBackingDecimalFn(dec)
                assertEquals(expectedStr, res.toString(), "for $decStr")
            }

            checkScaleRound("10", "10")
            checkScaleRound("1", "1")
            checkScaleRound("0.1", "0.1")
            checkScaleRound("0.012345", "0.0123")
            checkScaleRound("-0.0012345", "-0.00123")
        }

        fun testWithNoRound() {
            val ctx4 = DeciContext(scale = 4, roundingMode = HALF_UP, precision = 3)
            val ctx = prepareRoundDeciContext(ctx4, false)

            fun checkScaleNoRound(decStr: String, expectedStr: String) {
                val d = createDecimalFn(decStr)
                val dec = toRoundedDeciFn(d, ctx)
                val res = getBackingDecimalFn(dec)
                assertEquals(expectedStr, res.toString(), "for $decStr")
            }

            checkScaleNoRound("10", "10")
            checkScaleNoRound("1", "1")
            checkScaleNoRound("0.1", "0.1")
            checkScaleNoRound("0.012345", "0.012345")
            checkScaleNoRound("-0.0012345", "-0.0012345")
        }
    }

    inner class UtilsRoundTestHelper(
        private val roundFn: (T, Int, DeciContext) -> Deci,
    ) {
        internal fun testUtilsRound() {
            val ctx4 = DeciContext(scale = 4, roundingMode = HALF_UP, precision = 3)
            fun checkRound(decStr: String, scale: Int, expectedStr: String) {
                val d = createDecimalFn(decStr)
                val dec = roundFn(d, scale, ctx4)
                val res = getBackingDecimalFn(dec)
                assertEquals(expectedStr, res.toString(), "for $decStr")
            }

            checkRound("0.012345", 5, "0.01235")
            checkRound("0.012345", 4, "0.0123")
            checkRound("-0.012345", 5, "-0.01235")
        }
    }
}
