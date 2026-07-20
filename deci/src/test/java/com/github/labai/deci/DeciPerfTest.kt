/*
MIT License
Copyright (c) 2026 Augustus
*/
package com.github.labai.deci

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.time.measureTime

/**
 * @author Augustus
 * created on 2026-07-19
 */
class DeciPerfTest {

    // jvm25
    // Java native BigDec  : time=761
    // Deci parser         : time=310
    @Disabled
    @Test
    fun test_perf_bigDec_fromString() {
        val times = 1_000_000
        val ar = arrayOf(
            "1234567890.12345678901234567890",
            "-111222333.11122233",
            "-123456789000.00010101010000",
            "123456789000.000101010100",
            "1"
            )
        var n = 0
        val testFn1 = {
            var d: BigDecimal
            for (i in 1..times) {
                for (j in ar.indices) {
                    d = BigDecimal(ar[j])
                    n += d.scale()
                }
            }
            n
        }

        val testFn3 = {
            var d: BigDecimal
            for (i in 1..times) {
                for (j in ar.indices) {
                    d = BigDecimalUtils.parseString(ar[j]) ?: BigDecimal.ZERO
                    n += d.scale()
                }
            }
            n
        }
        println("BigDecimal:")
        runTestFn(testFn1)
        println("Deci parser:")
        runTestFn(testFn3)

        if (n == 0) println("") // dummy use of 'n'
    }

    fun runTestFn(testFn: () -> Int): Int {
        var n = 1

        // warmup
        for (i in 1..5) {
            testFn()
        }

        for (i in 1..5) {
            val tinyTm = measureTime {
                n += testFn()
            }
            println("$i time=${tinyTm.inWholeMilliseconds}ms")
        }
        return n
    }
}
