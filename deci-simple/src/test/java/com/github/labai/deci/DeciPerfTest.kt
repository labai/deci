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

    private val sampleSmall = arrayOf(
        "12.12",
        "-1113.1112",
        "-13.010100",
        "789000.100",
        "-1.0",
        "123.00",
        "1234.12",
        "-1234.12",
        "12345.99",
        "1.145242",
        "0.00045241",
        "1",
        "0",
    )

    private val sampleBig = arrayOf(
        "1234567890.12345678901234567890",
        "-111222333.11122233",
        "-123456789000.00010101010000",
        "123456789000.000101010100",
    )

    // decimalToString
    // ~1.2x faster for small numbers
    // ~2x faster for big numbers

    // toPlainString (no trim):
    //   time=580ms
    // toPlainString + trimTrailingZeros:
    //   time=636ms
    // stripTrailingZeros + toPlainString:
    //   time=888ms
    // decimalToString:
    //   time=439ms
    @Disabled
    @Test
    fun test_toPlainString_perf() {
        val times = 500_000

        val ar = sampleBig.map { BigDecimal(it) }

        var n = 0
        val testFn1 = {
            var s: String
            for (i in 1..times) {
                for (j in ar.indices) {
                    s = ar[j].toPlainString()
                    n += s.length
                }
            }
            n
        }

        val testFn2 = {
            var s: String
            for (i in 1..times) {
                for (j in ar.indices) {
                    s = BigDecimalUtils.trimTrailingZeros(ar[j].toPlainString())!!
                    n += s.length
                }
            }
            n
        }

        val testFn3 = {
            var s: String
            for (i in 1..times) {
                for (j in ar.indices) {
                    s = ar[j].stripTrailingZeros().toPlainString()
                    n += s.length
                }
            }
            n
        }

        val testFn4 = {
            var s: String
            for (i in 1..times) {
                for (j in ar.indices) {
                    s = BigDecimalUtils.decimalToString(ar[j])!!
                    n += s.length
                }
            }
            n
        }

        println("toPlainString (no trim):")
        runTestFn(testFn1)
        println("toPlainString + trimTrailingZeros:")
        runTestFn(testFn2)
        println("stripTrailingZeros + toPlainString:")
        runTestFn(testFn3)
        println("decimalToString:")
        runTestFn(testFn4)

        if (n == 0) println("") // dummy use of 'n'

    }

    // jvm25
    // Java native BigDec  : time=755
    // Deci parser         : time=295
    @Disabled
    @Test
    fun test_perf_bigDec_fromString() {
        val times = 1_000_000
        val ar = sampleBig

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

        for (i in 1..4) {
            val tinyTm = measureTime {
                n += testFn()
            }
            println("$i time=${tinyTm.inWholeMilliseconds}ms")
        }
        return n
    }
}
