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

/**
 * @author Augustus
 *         created on 2023.11.08
 *
 * An extension for using nullable variables in math expression.
 *
 * By default, nullable variables are not allowed to be used in math expressions in kotlin.
 * To keep consistent behaviour, Deci also do not allow nullables in math.
 *
 * This extension allows to use nullables in math expression with such logic,
 * that if any of part is null, then result is null.
 *
 * Example:
 *   val num: Deci? = null
 *   val res: Deci? = deciExpr {
 *      3.deci + 2.deci * num
 *   }
 *   assertNull(res)
 *
 * If needed bigger scale, it can be provided as parameter for deciExpr and will be used inside it.
 *
 * Example:
 *   val ctx40 = DeciContext(scale = 40, roundingMode = HALF_UP, precision = 30)
 *   val res = deciExpr(ctx40) { "1.0123456789012345678901234567890123456789".deci * "1e10".deci }
 *   assertEquals("10123456789.012345678901234567890123456789", dec.toString()) // scale are kept inside deciExpr
 *
 * In case you just want nulls treat as zeros, you can also use an extension Deci?.orZero().
 *
 * Example:
 *   val num: Deci? = null
 *   val res: Deci = 3.deci + 2.deci * num.orZero()
 *   assertEquals(3.deci, res)
 *
*/

expect class DeciExpr {
    val deciContext: DeciContext

    constructor(deciContext: DeciContext = Deci.defaultDeciContext)

    constructor()

    operator fun Deci?.unaryMinus(): Deci?

    operator fun Deci?.plus(other: Deci?): Deci?
    operator fun Deci?.minus(other: Deci?): Deci?
    operator fun Deci?.times(other: Deci?): Deci?
    operator fun Deci?.div(other: Deci?): Deci?
    operator fun Deci?.rem(other: Deci?): Deci?

    operator fun Deci?.plus(other: Int?): Deci?
    operator fun Deci?.minus(other: Int?): Deci?
    operator fun Deci?.times(other: Int?): Deci?
    operator fun Deci?.div(other: Int?): Deci?
    operator fun Deci?.rem(other: Int?): Deci?

    operator fun Deci?.plus(other: Long?): Deci?
    operator fun Deci?.minus(other: Long?): Deci?
    operator fun Deci?.times(other: Long?): Deci?
    operator fun Deci?.div(other: Long?): Deci?
    operator fun Deci?.rem(other: Long?): Deci?

    operator fun Int?.unaryMinus(): Deci?

    operator fun Int?.plus(other: Long?): Deci?
    operator fun Int?.minus(other: Long?): Deci?
    operator fun Int?.times(other: Long?): Deci?
    operator fun Int?.div(other: Long?): Deci?
    operator fun Int?.rem(other: Long?): Deci?

    operator fun Int?.plus(other: Int?): Deci?
    operator fun Int?.minus(other: Int?): Deci?
    operator fun Int?.times(other: Int?): Deci?
    operator fun Int?.div(other: Int?): Deci?
    operator fun Int?.rem(other: Int?): Deci?

    operator fun Int?.plus(other: Deci?): Deci?
    operator fun Int?.minus(other: Deci?): Deci?
    operator fun Int?.times(other: Deci?): Deci?
    operator fun Int?.div(other: Deci?): Deci?
    operator fun Int?.rem(other: Deci?): Deci?

    operator fun Long?.unaryMinus(): Deci?

    operator fun Long?.plus(other: Long?): Deci?
    operator fun Long?.minus(other: Long?): Deci?
    operator fun Long?.times(other: Long?): Deci?
    operator fun Long?.div(other: Long?): Deci?
    operator fun Long?.rem(other: Long?): Deci?

    operator fun Long?.plus(other: Int?): Deci?
    operator fun Long?.minus(other: Int?): Deci?
    operator fun Long?.times(other: Int?): Deci?
    operator fun Long?.div(other: Int?): Deci?
    operator fun Long?.rem(other: Int?): Deci?

    operator fun Long?.plus(other: Deci?): Deci?
    operator fun Long?.minus(other: Deci?): Deci?
    operator fun Long?.times(other: Deci?): Deci?
    operator fun Long?.div(other: Deci?): Deci?
    operator fun Long?.rem(other: Deci?): Deci?

    val Int.deci: Deci
    val Long.deci: Deci
    val String.deci: Deci
}

fun deciExpr(expression: DeciExpr.() -> Number?): Deci? {
    val deciExprScope = DeciExpr()
    val res: Number? = deciExprScope.expression()
    return if (res == null) null else Deci.valueOf(res)
}

fun deciExpr(deciContext: DeciContext, expression: DeciExpr.() -> Number?): Deci? {
    val deciExprScope = DeciExpr(deciContext)
    val res: Number? = deciExprScope.expression()
    return if (res == null) null else Deci.valueOf(res, deciContext)
}
