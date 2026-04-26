package com.github.labai.deci

import com.github.labai.deci.impl.TinyUDecMath
import com.github.labai.deci.impl.TinyUDecMath.TinyUDec
import org.junit.jupiter.api.Disabled
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.measureTime

/*
    per 10mi in ms
                tiny    bdec
    create      19.4    19.4
    create(min) 18.5    1.4     - some jvm optimization?
    create2     4.0     1.4     - w/o trimming zeros. Also, may be significant loop overhead
    add         1.3     1.5
    parse       356     364
    toString    228     585
    round(min)  3.3     3.6
    round(max)  45      114     - sometimes much slower (due to GC or jit?)
*/

class TiniUDecPerfTest {

    //
    // (1) tinyTm=975.182915ms bdecTm=935.025606ms
    // (2) tinyTm=946.954774ms bdecTm=69.093121ms
    // (3) tinyTm=941.636280ms bdecTm=67.439588ms

    // if remove trimZeros
    // (1) tinyTm=203.261483ms bdecTm=934.884622ms
    // (2) tinyTm=200.038398ms bdecTm=66.536673ms
    // (3) tinyTm=197.199615ms bdecTm=66.730628ms
    @Disabled
    @Test
    fun test_perf_create() {
        val timesInt = 500_000_000
        val timesLong = timesInt.toLong()
        var n = 0

        val tinyFn = {
            for (i in 1..timesInt) {
                val d = TinyUDecMath.buildTiny(i, 3)
                n += d.tiny
            }
        }

        val bdecFn = {
            for (i in 1..timesLong) {
                val d = BigDecimal.valueOf(i, 3)
                n += d.scale()
            }
        }

        // warmup
        tinyFn()
        bdecFn()

        for (i in 1..3) {
            val bdecTm = measureTime { bdecFn() }
            val tinyTm = measureTime { tinyFn() }
            println("($i) tinyTm=$tinyTm bdecTm=$bdecTm")
        }
        assertTrue(n > Int.MIN_VALUE)
    }



    // (3) tinyTm=39.080540ms bdecTm=44.048723ms
    @Disabled
    @Test
    fun test_perf_add() {
        val times = 300_000_000

        var n = 0

        val tiny1 = TinyUDecMath.parseString("12345.78")
        val tiny2 = TinyUDecMath.parseString("12.345")

        val tinyFn = {
            val b1 = tiny1
            val b2 = tiny2
            var tmp: TinyUDec
            for (i in 1..times) {
                tmp = TinyUDecMath.addOrErr(b1, b2)
                n += tmp.tiny
            }
        }

        val bdec1 = BigDecimal("12345.78")
        val bdec2 = BigDecimal("12.345")

        val bdecFn = {
            val b1 =  bdec1
            val b2 = bdec2
            var tmp = BigDecimal.ZERO
            for (i in 1..times) {
                tmp = b1.add(b2)
                n += tmp.scale()
            }
        }

        // warmup
        tinyFn()
        bdecFn()

        for (i in 1..3) {
            val bdecTm = measureTime { bdecFn() }
            val tinyTm = measureTime { tinyFn() }
            println("($i) tinyTm=$tinyTm bdecTm=$bdecTm")
        }
        assertTrue(n > 0)
    }

    // add#3 tinyTm=8.378107ms bdecTm=10.145398ms
    @Disabled
    @Test
    fun test_perf_add2() {
        val times = 500_000_000

        val tinyFn = {
            var n = 0
            val b1 = TinyUDecMath.parseString("12345.78")
            val b2 = TinyUDecMath.parseString("12.345")
            var tmp: TinyUDec
            for (i in 1..times) {
                tmp = TinyUDecMath.addOrErr(b1, b2)
                n += tmp.tiny
            }
            n
        }

        val bdecFn = {
            var n = 0
            val b1 = BigDecimal("12345.78")
            val b2 = BigDecimal("12.345")
            for (i in 1..times) {
                n += b1.add(b2)
                    .scale()
            }
            n
        }

        runCompareTest("add", tinyFn, bdecFn)
    }


    // parse#3 tiny=336ms bdec=454ms
    @Disabled
    @Test
    fun test_perf_parseString() {
        val times = 10_000_000

        val tinyFn = {
            var n = 0
            var tmp: TinyUDec
            for (i in 1..times) {
                val b1 = TinyUDecMath.parseString("12345.7080000")
                n += b1.tiny
            }
            n
        }

        val bdecFn = {
            var n = 0
            for (i in 1..times) {
                val b1 = BigDecimal("12345.7080000")
                n += b1.scale()
            }
            n
        }

        runCompareTest("parse", tinyFn, bdecFn)
    }

    // toString#3 tinyTm=228.333023ms bdecTm=585.179450ms
    @Disabled
    @Test
    fun test_perf_toString() {
        val times = 10_000_000

        val tinyFn = {
            var n = 0
            for (i in 1..times) {
                val d = TinyUDecMath.buildTiny(12345780, 3)
                val s = d.toString()
                n += s.length
            }
            n
        }

        val bdecFn = {
            var n = 0
            for (i in 1..times) {
                val d = BigDecimal.valueOf(12345780, 3) // creating new to avoid caching
                val s = d.toString()
                n += s.length
            }
            n
        }

        runCompareTest("toString", tinyFn, bdecFn)
    }

    // round#1 tinyTm=31.797945ms bdecTm=18.106893ms
    // round#2 tinyTm=226.192564ms bdecTm=20.795356ms
    // round#3 tinyTm=16.511007ms bdecTm=568.620969ms // GC for BigDecimal(?)
    @Disabled
    @Test
    fun test_perf_round() {
        val times = 50_000_000

        val tinyFn = {
            var n = 0
            val d = TinyUDecMath.buildTiny(12345780, 3)
            for (i in 1..times) {
                val r = TinyUDecMath.round(d, 1, RoundingMode.HALF_UP)
                n += r.tiny
            }
            n
        }

        val bdecFn = {
            var n = 0
            val d = BigDecimal.valueOf(12345780, 3)
            for (i in 1..times) {
                val r = d.setScale(1, java.math.RoundingMode.HALF_UP)
                n += r.scale()
            }
            n
        }

        runCompareTest("round", tinyFn, bdecFn)
    }



    // (3) tinyTm=35.755893ms bdecTm=41.447494ms
    @Disabled
    @Test
    fun test_perf_addWithCreation() {
        val times = 300_000_000

        var n = 0

        val tinyFn = {
            var tmp: TinyUDec
            for (i in 1..times) {
                val b1 = TinyUDecMath.buildTiny(1234578, 2)
                val b2 = TinyUDecMath.buildTiny(12345, 3)
                tmp = TinyUDecMath.addOrErr(b1, b2)
                n += tmp.tiny
            }
        }

        val bdecFn = {
            var tmp = BigDecimal.ZERO
            for (i in 1..times) {
                val b1 = BigDecimal.valueOf(12345578L, 2)
                val b2 = BigDecimal.valueOf(12345, 3)
                tmp = b1.add(b2)
                n += tmp.scale()
            }
        }

        // warmup
        tinyFn()
        bdecFn()

        for (i in 1..3) {
            val bdecTm = measureTime { bdecFn() }
            val tinyTm = measureTime { tinyFn() }
            println("($i) tiny=${tinyTm.inWholeMilliseconds}ms bdec=${bdecTm.inWholeMilliseconds}ms")
        }
        assertTrue(n > 0)
    }

    // loop should be inside fn
    inline fun runCompareTest(name: String, tinyLoopFn: () -> Int, bdecLoopFn: () -> Int) {
        var n = 0
        for (i in 1..3) {
            val bdecTm = measureTime {
                n += bdecLoopFn()
            }
            val tinyTm = measureTime {
                n += tinyLoopFn()
            }
            println("$name#$i tiny=${tinyTm.inWholeMilliseconds}ms bdec=${bdecTm.inWholeMilliseconds}ms")
        }
        assertTrue(n > Int.MIN_VALUE)
    }
}
