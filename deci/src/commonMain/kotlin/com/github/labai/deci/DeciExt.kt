/*
MIT License

Copyright (c) 2020 Augustus

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

import java.math.BigDecimal

/**
 * @author Augustus
 * created on 2025-12-21
*/
expect operator fun Deci.plus(other: BigDecimal): Deci
expect operator fun Deci.minus(other: BigDecimal): Deci
expect operator fun Deci.times(other: BigDecimal): Deci
expect operator fun Deci.div(other: BigDecimal): Deci
expect operator fun Deci.rem(other: BigDecimal): Deci

expect operator fun Deci.plus(other: Deci): Deci
expect operator fun Deci.minus(other: Deci): Deci
expect operator fun Deci.times(other: Deci): Deci
expect operator fun Deci.div(other: Deci): Deci
expect operator fun Deci.rem(other: Deci): Deci

expect operator fun Deci.plus(other: Int): Deci
expect operator fun Deci.minus(other: Int): Deci
expect operator fun Deci.times(other: Int): Deci
expect operator fun Deci.div(other: Int): Deci
expect operator fun Deci.rem(other: Int): Deci

expect operator fun Deci.plus(other: Long): Deci
expect operator fun Deci.minus(other: Long): Deci
expect operator fun Deci.times(other: Long): Deci
expect operator fun Deci.div(other: Long): Deci
expect operator fun Deci.rem(other: Long): Deci

//
// Int extensions
//
val Int.deci: Deci
    inline get() = Deci.valueOf(this)

//
// Long extensions
//
val Long.deci: Deci
    inline get() = Deci.valueOf(this)

//
// String extensions
//
val String.deci: Deci
    inline get() = Deci(this)

//
// additional Deci methods
//
expect fun Deci.Companion.valueOf(num: Number): Deci

expect fun Deci.Companion.valueOf(str: String): Deci

expect fun Deci.Companion.valueOf(num: Number, deciContext: DeciContext): Deci

expect fun Deci.Companion.valueOf(str: String, deciContext: DeciContext): Deci

expect operator fun Deci.compareTo(other: Number): Int

// null to zero - useful in formulas, reduces an expression '(nullableValue ?: 0.deci)' to 'nullableValue.orZero()'
fun Deci?.orZero(): Deci = this ?: Deci.ZERO

//
// Iterable extensions
//
@JvmName("sumOfDeci")
inline fun <T> Iterable<T>.sumOf(selector: (T) -> Deci): Deci {
    var sum: Deci = Deci.ZERO
    for (element in this) {
        sum += selector(element)
    }
    return sum
}
