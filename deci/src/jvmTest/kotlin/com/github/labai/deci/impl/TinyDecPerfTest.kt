/*
MIT License

Copyright (c) 2026 Augustus

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
*/
package com.github.labai.deci.impl

import com.github.labai.deci.RoundingMode
import org.junit.jupiter.api.Disabled
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.measureTime

/*
    per 10mln in ms
                tiny    bdec
    create(max) 4.1     20.2
    create(min) 4.0     1.4     - some jvm optimization?
    add         1.3     1.4
    parse       287     373
    toString    215     585
    round       55      278
*/

class TinyDecPerfTest {

    // (1) tinyTm=208ms bdecTm=981ms
    // (2) tinyTm=202ms bdecTm=68ms
    // (3) tinyTm=206ms bdecTm=68ms
    @Disabled
    @Test
    fun test_perf_create() {
        val timesInt = 500_000_000
        val timesLong = timesInt.toLong()
        var n = 0

        val tinyFn = {
            for (i in 1..timesInt) {
                val d = TinyDec.buildTinyOrErr(i, 3)
                n += d.raw
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
            println("($i) tinyTm=${tinyTm.inWholeMilliseconds}ms bdecTm=${bdecTm.inWholeMilliseconds}ms")
        }
        assertTrue(n > Int.MIN_VALUE)
    }



    // (3) tinyTm=52ms bdecTm=57ms
    @Disabled
    @Test
    fun test_perf_add() {
        val times = 400_000_000

        var n = 0

        val tiny1 = TinyDec.parseString("12345.78")
        val tiny2 = TinyDec.parseString("12.345")

        val tinyFn = {
            val b1 = tiny1
            val b2 = tiny2
            var tmp: TinyDec
            for (i in 1..times) {
                tmp = b1.add(b2)
                n += tmp.raw
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
            println("($i) tinyTm=${tinyTm.inWholeMilliseconds}ms bdecTm=${bdecTm.inWholeMilliseconds}ms")
        }
        assertTrue(n > 0)
    }

    // parse#3 tiny=286ms bdec=363ms
    @Disabled
    @Test
    fun test_perf_parseString() {
        val times = 10_000_000

        val tinyFn = {
            var n = 0
            var tmp: TinyDec
            for (i in 1..times) {
                val b1 = TinyDec.parseString("12345.008000")
                n += b1.raw
            }
            n
        }

        val bdecFn = {
            var n = 0
            for (i in 1..times) {
                val b1 = BigDecimal("12345.008000")
                n += b1.scale()
            }
            n
        }

        runCompareTest("parse", tinyFn, bdecFn)
    }

    // toString#3 tiny=215ms bdec=545ms
    @Disabled
    @Test
    fun test_perf_toString() {
        val times = 10_000_000

        val tinyFn = {
            var n = 0
            for (i in 1..times) {
                val d = TinyDec.buildTiny(12345780, 3)
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

    // round#3 tiny=286ms bdec=834ms
    @Disabled
    @Test
    fun test_perf_round() {
        val times = 10_000_000

        val tinyFn = {
            var n = 0
            for (i in 1..times) {
                val d1 = TinyDec.buildTiny(i, 3)
                val d2 = d1.round(2, RoundingMode.HALF_UP)
                val d3 = d2.round(1, RoundingMode.HALF_UP)
                val d4 = d3.round(0, RoundingMode.HALF_UP)
                n += d4.raw
            }
            n + (Math.random() * 10000).toInt()
        }

        val bdecFn = {
            var n = 0
            for (i in 1L..times.toLong()) {
                val d1 = BigDecimal.valueOf(i, 3)
                val d2 = d1.setScale(2, java.math.RoundingMode.HALF_UP)
                val d3 = d2.setScale(1, java.math.RoundingMode.HALF_UP)
                val d4 = d3.setScale(0, java.math.RoundingMode.HALF_UP)
                n += d4.scale()
            }
            n + (Math.random() * 10000).toInt()
        }
        runCompareTest("round", tinyFn, bdecFn)
    }

    // loop should be inside fn
    inline fun runCompareTest(name: String, tinyLoopFn: () -> Int, bdecLoopFn: () -> Int) {
        // warmup
        tinyLoopFn()
        bdecLoopFn()

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
