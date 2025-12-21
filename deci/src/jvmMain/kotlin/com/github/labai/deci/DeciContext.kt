/*
MIT License

Copyright (c) 2023 Augustus

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
package com.github.labai.deci

import java.io.Serializable
import java.math.RoundingMode as JavaRoundingMode

/*
 * @author Augustus
 * created on 2025-12-21
*/
actual data class DeciContext actual constructor(
    val scale: Int,
    val roundingMode: RoundingMode,
    val precision: Int
) : Serializable {

    internal val javaRoundingMode: JavaRoundingMode
        get() = roundingMode.toJava()

    actual constructor(scale: Int, roundingMode: RoundingMode) : this(scale, roundingMode, scale)

    init {
        check(scale >= 0) { "scale must be >= 0 (is $scale)" }
        check(scale <= 2000) { "scale must be <= 2000 (is $scale)" }
        check(precision >= 1) { "precision must be >= 1 (is $precision)" }
        check(precision <= 2000) { "precision must be <= 2000 (is $precision)" }
    }

    override fun toString(): String = "DeciContext($scale:$precision:${roundingMode.toString().lowercase()})"

    actual constructor(scale: Int) : this(scale, RoundingMode.HALF_UP)
}

internal fun RoundingMode.toJava(): JavaRoundingMode = when (this) {
    RoundingMode.HALF_UP -> JavaRoundingMode.HALF_UP
    RoundingMode.DOWN -> JavaRoundingMode.DOWN
    RoundingMode.HALF_EVEN -> JavaRoundingMode.HALF_EVEN
    RoundingMode.UP -> JavaRoundingMode.UP
    RoundingMode.HALF_DOWN -> JavaRoundingMode.HALF_DOWN
    RoundingMode.CEILING -> JavaRoundingMode.CEILING
    RoundingMode.FLOOR -> JavaRoundingMode.FLOOR
}
