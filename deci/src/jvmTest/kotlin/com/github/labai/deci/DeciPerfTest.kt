package com.github.labai.deci

import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import kotlin.time.measureTime

/*
 * @author Augustus
 * created on 2026-04-25
 */
class DeciPerfTest {

    // tiny version 385ms, orig 2675ms
    @Disabled
    @Test
    fun test_perf_allTiny() {
        val times = 1_000_000
        var n = 1
        val testFn = {
            val perc = 30.deci
            var d = Deci("1")
            for (i in 1..times) {
                d = (d + "12.25".deci * 12 + "1.200".deci)
                d += Deci("1.2") * (i % 1100) * perc / 100
                d -= (i.deci / 10) % 600
                if (d > 500.deci)
                    d -= 500
            }
            d.toInt()
        }

        // warmup
        testFn()

        for (i in 1..3) {
            val tinyTm = measureTime {
                n += testFn()
            }
            println("$i time=${tinyTm.inWholeMilliseconds}ms")
        }
    }

    // tiny version 381ms, orig 660ms
    @Disabled
    @Test
    fun test_perf_mixed() {
        val times = 1_000_000
        var n = 1
        val testFn = {
            val perc = 30.deci
            var d = Deci("1")
            for (i in 1..times) {
                d = (d + "12.25".deci * 12 + "1.200".deci)
                d += Deci("1.2") * i * perc / 100
                d += i.deci / 10
                if (d > 500.deci)
                    d -= 500
            }
            d.toInt()
        }

        // warmup
        testFn()

        for (i in 1..3) {
            val tinyTm = measureTime {
                n += testFn()
            }
            println("$i time=${tinyTm.inWholeMilliseconds}ms")
        }
    }

    data class TestDto(
        val d1: Deci,
        val d2: Deci,
        val d3: Deci,
        val d4: Deci,
    )

    // 69 Mb (with tinyDec) vs 186 Mb (orig Deci)
    @Disabled
    @Test
    fun test_mem_allTiny() {
        val list = ArrayList<TestDto>(1_000_000)
        for (i in 1..1_000_000) {
            list.add(TestDto(i.deci, (i + 1).deci, (i - 1).deci, (-i).deci))
        }
        println(list.size)
    }
}
