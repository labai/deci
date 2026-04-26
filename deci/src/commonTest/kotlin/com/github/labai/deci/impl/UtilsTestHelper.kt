package com.github.labai.deci.impl

import com.github.labai.deci.DeciContext
import com.github.labai.deci.RoundingMode.HALF_UP

/**
 * @author Augustas
 * created on 2026-01-04
 *
 * Common test data for JS and iOS
 *
 */
object UtilsTestHelper {
    internal val CTX4 = DeciContext.of(scale = 4, roundingMode = HALF_UP, precision = 3)

    // decStr -> expectedScale
    internal fun calcScale_testCases(): List<Pair<String, Int>> {
        return listOf(
            Pair("10", 4),
            Pair("1", 4),
            Pair("1.1", 4),
            Pair("0.1", 4),
            Pair("0.01", 4), // 0.0100 = scale = 1(0) + 3(precision)
            Pair("0.012345", 4),
            Pair("0.0012345", 5), // 2(00) + 3(precision)
            Pair("0.00012345", 6), // 3(000) + 3(precision)
        )
    }

    // decStr -> expectedStr
    internal fun toRoundedDeci_rounded_testCases(): List<Pair<String, String>> {
        return listOf(
            Pair("10", "10"),
            Pair("1", "1"),
            Pair("0.1", "0.1"),
            Pair("0.012345", "0.0123"),
            Pair("-0.0012345", "-0.00123"),
        )
    }

    // decStr -> expectedStr
    internal fun toRoundedDeci_notRounded_testCases(): List<Pair<String, String>> {
        return listOf(
            Pair("10", "10"),
            Pair("1", "1"),
            Pair("0.1", "0.1"),
            Pair("0.012345", "0.012345"),
            Pair("-0.0012345", "-0.0012345"),
        )
    }

    // decStr, scale -> expectedStr
    internal fun utilsRound_testCases(): List<Triple<String, Int, String>> {
        return listOf(
            Triple("0.012345", 5, "0.01235"),
            Triple("0.012345", 4, "0.0123"),
            Triple("-0.012345", 5, "-0.01235"),
        )
    }
}
