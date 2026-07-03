package com.github.labai.deci

import ch.randelshofer.fastdoubleparser.JavaBigDecimalParser
import com.github.labai.deci.impl.BigDecimalUtils
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.time.measureTime

/*
 * @author Augustus
 * created on 2026-04-25
 *
 *
 * Deci
 *  220 - when only tinyInt as back
 *  710 - when bigDecimal as back
 * BigDecimal
 *  2630 - if to use setScale(20)
 *  (460 - if don't change scale, but then result is incorrect)
 *
 * Memory usage
 *  51 Mb (with tinyDec) vs 186 Mb (orig Deci)
 */
class DeciPerfTest {

    // 220ms
    @Disabled
    @Test
    fun test_perf_1_when_allTiny() {
        val times = 1_000_000
        val testFn = {
            val perc = 30.deci
            var d = Deci("1")
            for (i in 1..times) {
                d = (d + "12.25".deci * 12 - "-1.200".deci)
                d += Deci("1.2") * (i % 1100) * perc / 100
                d -= (i.deci / 10) % 600
                if (d > 500.deci)
                    d -= 500
            }
            d.toInt()
        }
        runTestFn(testFn)
    }

    // time=710s
    @Disabled
    @Test
    fun test_perf_3_deci_when_bigDec() {
        val times = 1_000_000
        val testFn = {
            val perc = 30.deci
            var d = Deci("1")
            for (i in 1..times) {
                d = (d + "12.0025".deci * 12 - "-1.000200".deci)
                val dd = "1.0002".deci * i * perc / 100
                d += dd
                d += (i.deci / 10) % 600
                if (d > 500.deci)
                    d -= 500
            }
            d.toInt()
        }
        val res = runTestFn(testFn)
        println(res)
    }

    // time=2630ms
    @Disabled
    @Test
    fun test_perf_4_bigDec_native() {
        val times = 1_000_000

        val testFn = {
            val perc = 30.toBigDecimal()
            var d = "1".toBigDecimal().setScale(20)
            for (i in 1..times) {
                d = (d + "12.0025".toBigDecimal() * 12.toBigDecimal() - "-1.000200".toBigDecimal())
                val dd = "1.0002".toBigDecimal().setScale(20) * i.toBigDecimal() * perc / 100.toBigDecimal()
                d += dd
                d += (i.toBigDecimal().setScale(20) / 10.toBigDecimal()) % 600.toBigDecimal()
                if (d > 500.toBigDecimal())
                    d -= 500.toBigDecimal()
            }
            d.toInt()
        }
        val res = runTestFn(testFn)
        println(res)
    }

    // Java native BigDec  : time=309ms
    // JavaBigDecimalParser: time=345ms
    // Deci parser         : time=172ms
    @Disabled
    @Test
    fun test_perf_bigDec_fromString() {
        val times = 1_000_000
        val ar = arrayOf("1234567890.12345678901234567890", "-111222333.11122233")
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
        val testFn2 = {
            var d: BigDecimal
            for (i in 1..times) {
                for (j in ar.indices) {
                    d = JavaBigDecimalParser.parseBigDecimal(ar[j])
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
        println("JavaBigDecimalParser:")
        runTestFn(testFn2)
        println("Deci parser:")
        runTestFn(testFn3)

        if (n == 0) println("") // dummy use of 'n'
    }

    data class TestDto(
        val d1: Deci,
        val d2: Deci,
        val d3: Deci,
        val d4: Deci,
    )

    @Disabled
    @Test
    fun test_mem_allTiny() {
        val list = ArrayList<TestDto>(1_000_000)
        for (i in 1..1_000_000) {
            list.add(TestDto(i.deci, (i + 1).deci, (i - 1).deci, (-i).deci))
        }
        println(list.size)
    }

    data class TestDtoBd(
        val d1: BigDecimal,
        val d2: BigDecimal,
        val d3: BigDecimal,
        val d4: BigDecimal,
    )

    @Disabled
    @Test
    fun test_mem_bigDecimal() {
        val list = ArrayList<TestDtoBd>(1_000_000)
        for (i in 1..1_000_000) {
            list.add(TestDtoBd(i.toBigDecimal(), (i + 1).toBigDecimal(), (i - 1).toBigDecimal(), (-i).toBigDecimal()))
        }
        println(list.size)
    }

    //
    // helpers
    //

    fun runTestFn(testFn: () -> Int): Int {
        var n = 1

        // warmup
        testFn()

        for (i in 1..3) {
            val tinyTm = measureTime {
                n += testFn()
            }
            println("$i time=${tinyTm.inWholeMilliseconds}ms")
        }
        return n
    }
}
