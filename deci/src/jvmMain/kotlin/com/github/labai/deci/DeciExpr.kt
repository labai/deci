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

import java.math.BigDecimal

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
actual class DeciExpr {
    actual val deciContext: DeciContext

    actual constructor(deciContext: DeciContext) {
        this.deciContext = deciContext
    }
    actual constructor() : this(Deci.defaultDeciContext)

    actual operator fun Deci?.unaryMinus(): Deci? = this?.unaryMinus()

    operator fun Deci?.plus(other: BigDecimal?): Deci? = if (this == null || other == null) null else this.deci.plusInternal(other)
    operator fun Deci?.minus(other: BigDecimal?): Deci? = if (this == null || other == null) null else this.deci.minusInternal(other)
    operator fun Deci?.times(other: BigDecimal?): Deci? = if (this == null || other == null) null else this.deci.timesInternal(other)
    operator fun Deci?.div(other: BigDecimal?): Deci? = if (this == null || other == null) null else this.deci.divInternal(other)
    operator fun Deci?.rem(other: BigDecimal?): Deci? = if (this == null || other == null) null else this.deci.remInternal(other)

    actual operator fun Deci?.plus(other: Deci?): Deci? = if (this == null || other == null) null else this.deci.plus(other.toBigDecimal())
    actual operator fun Deci?.minus(other: Deci?): Deci? = if (this == null || other == null) null else this.deci.minus(other.toBigDecimal())
    actual operator fun Deci?.times(other: Deci?): Deci? = if (this == null || other == null) null else this.deci.times(other.toBigDecimal())
    actual operator fun Deci?.div(other: Deci?): Deci? = if (this == null || other == null) null else this.deci.div(other.toBigDecimal())
    actual operator fun Deci?.rem(other: Deci?): Deci? = if (this == null || other == null) null else this.deci.rem(other.toBigDecimal())

    actual operator fun Deci?.plus(other: Int?): Deci? = if (this == null || other == null) null else this.deci.plus(BigDecimal.valueOf(other.toLong()))
    actual operator fun Deci?.minus(other: Int?): Deci? = if (this == null || other == null) null else this.deci.minus(BigDecimal.valueOf(other.toLong()))
    actual operator fun Deci?.times(other: Int?): Deci? = if (this == null || other == null) null else this.deci.times(BigDecimal.valueOf(other.toLong()))
    actual operator fun Deci?.div(other: Int?): Deci? = if (this == null || other == null) null else this.deci.div(BigDecimal.valueOf(other.toLong()))
    actual operator fun Deci?.rem(other: Int?): Deci? = if (this == null || other == null) null else this.deci.rem(BigDecimal.valueOf(other.toLong()))

    actual operator fun Deci?.plus(other: Long?): Deci? = if (this == null || other == null) null else this.deci.plus(BigDecimal.valueOf(other))
    actual operator fun Deci?.minus(other: Long?): Deci? = if (this == null || other == null) null else this.deci.minus(BigDecimal.valueOf(other))
    actual operator fun Deci?.times(other: Long?): Deci? = if (this == null || other == null) null else this.deci.times(BigDecimal.valueOf(other))
    actual operator fun Deci?.div(other: Long?): Deci? = if (this == null || other == null) null else this.deci.div(BigDecimal.valueOf(other))
    actual operator fun Deci?.rem(other: Long?): Deci? = if (this == null || other == null) null else this.deci.rem(BigDecimal.valueOf(other))

    actual operator fun Int?.unaryMinus(): Deci? = this?.deci.unaryMinus()

    actual operator fun Int?.plus(other: Long?): Deci? = if (this == null || other == null) null else this.deci.plus(other)
    actual operator fun Int?.minus(other: Long?): Deci? = if (this == null || other == null) null else this.deci.minus(other)
    actual operator fun Int?.times(other: Long?): Deci? = if (this == null || other == null) null else this.deci.times(other)
    actual operator fun Int?.div(other: Long?): Deci? = if (this == null || other == null) null else this.deci.div(other)
    actual operator fun Int?.rem(other: Long?): Deci? = if (this == null || other == null) null else this.deci.rem(other)

    actual operator fun Int?.plus(other: Int?): Deci? = if (this == null || other == null) null else this.deci.plus(other)
    actual operator fun Int?.minus(other: Int?): Deci? = if (this == null || other == null) null else this.deci.minus(other)
    actual operator fun Int?.times(other: Int?): Deci? = if (this == null || other == null) null else this.deci.times(other)
    actual operator fun Int?.div(other: Int?): Deci? = if (this == null || other == null) null else this.deci.div(other)
    actual operator fun Int?.rem(other: Int?): Deci? = if (this == null || other == null) null else this.deci.rem(other)

    operator fun Int?.plus(other: BigDecimal?): Deci? = if (this == null || other == null) null else this.deci.plus(other)
    operator fun Int?.minus(other: BigDecimal?): Deci? = if (this == null || other == null) null else this.deci.minus(other)
    operator fun Int?.times(other: BigDecimal?): Deci? = if (this == null || other == null) null else this.deci.times(other)
    operator fun Int?.div(other: BigDecimal?): Deci? = if (this == null || other == null) null else this.deci.div(other)
    operator fun Int?.rem(other: BigDecimal?): Deci? = if (this == null || other == null) null else this.deci.rem(other)

    actual operator fun Int?.plus(other: Deci?): Deci? = if (this == null || other == null) null else this.deci.plus(other)
    actual operator fun Int?.minus(other: Deci?): Deci? = if (this == null || other == null) null else this.deci.minus(other)
    actual operator fun Int?.times(other: Deci?): Deci? = if (this == null || other == null) null else this.deci.times(other)
    actual operator fun Int?.div(other: Deci?): Deci? = if (this == null || other == null) null else this.deci.div(other)
    actual operator fun Int?.rem(other: Deci?): Deci? = if (this == null || other == null) null else this.deci.rem(other)

    actual operator fun Long?.unaryMinus(): Deci? = this?.deci.unaryMinus()

    actual operator fun Long?.plus(other: Long?): Deci? = if (this == null || other == null) null else this.deci.plus(other)
    actual operator fun Long?.minus(other: Long?): Deci? = if (this == null || other == null) null else this.deci.minus(other)
    actual operator fun Long?.times(other: Long?): Deci? = if (this == null || other == null) null else this.deci.times(other)
    actual operator fun Long?.div(other: Long?): Deci? = if (this == null || other == null) null else this.deci.div(other)
    actual operator fun Long?.rem(other: Long?): Deci? = if (this == null || other == null) null else this.deci.rem(other)

    actual operator fun Long?.plus(other: Int?): Deci? = if (this == null || other == null) null else this.deci.plus(other)
    actual operator fun Long?.minus(other: Int?): Deci? = if (this == null || other == null) null else this.deci.minus(other)
    actual operator fun Long?.times(other: Int?): Deci? = if (this == null || other == null) null else this.deci.times(other)
    actual operator fun Long?.div(other: Int?): Deci? = if (this == null || other == null) null else this.deci.div(other)
    actual operator fun Long?.rem(other: Int?): Deci? = if (this == null || other == null) null else this.deci.rem(other)

    operator fun Long?.plus(other: BigDecimal?): Deci? = if (this == null || other == null) null else this.deci.plus(other)
    operator fun Long?.minus(other: BigDecimal?): Deci? = if (this == null || other == null) null else this.deci.minus(other)
    operator fun Long?.times(other: BigDecimal?): Deci? = if (this == null || other == null) null else this.deci.times(other)
    operator fun Long?.div(other: BigDecimal?): Deci? = if (this == null || other == null) null else this.deci.div(other)
    operator fun Long?.rem(other: BigDecimal?): Deci? = if (this == null || other == null) null else this.deci.rem(other)

    actual operator fun Long?.plus(other: Deci?): Deci? = if (this == null || other == null) null else this.deci.plus(other)
    actual operator fun Long?.minus(other: Deci?): Deci? = if (this == null || other == null) null else this.deci.minus(other)
    actual operator fun Long?.times(other: Deci?): Deci? = if (this == null || other == null) null else this.deci.times(other)
    actual operator fun Long?.div(other: Deci?): Deci? = if (this == null || other == null) null else this.deci.div(other)
    actual operator fun Long?.rem(other: Deci?): Deci? = if (this == null || other == null) null else this.deci.rem(other)

    operator fun BigDecimal?.unaryMinus(): Deci? = this?.deci.unaryMinus()

    operator fun BigDecimal?.plus(other: Long?): Deci? = if (this == null || other == null) null else this.deci.plus(other)
    operator fun BigDecimal?.minus(other: Long?): Deci? = if (this == null || other == null) null else this.deci.minus(other)
    operator fun BigDecimal?.times(other: Long?): Deci? = if (this == null || other == null) null else this.deci.times(other)
    operator fun BigDecimal?.div(other: Long?): Deci? = if (this == null || other == null) null else this.deci.div(other)
    operator fun BigDecimal?.rem(other: Long?): Deci? = if (this == null || other == null) null else this.deci.rem(other)

    operator fun BigDecimal?.plus(other: Int?): Deci? = if (this == null || other == null) null else this.deci.plus(other)
    operator fun BigDecimal?.minus(other: Int?): Deci? = if (this == null || other == null) null else this.deci.minus(other)
    operator fun BigDecimal?.times(other: Int?): Deci? = if (this == null || other == null) null else this.deci.times(other)
    operator fun BigDecimal?.div(other: Int?): Deci? = if (this == null || other == null) null else this.deci.div(other)
    operator fun BigDecimal?.rem(other: Int?): Deci? = if (this == null || other == null) null else this.deci.rem(other)

    operator fun BigDecimal?.plus(other: BigDecimal?): Deci? = if (this == null || other == null) null else this.deci.plus(other)
    operator fun BigDecimal?.minus(other: BigDecimal?): Deci? = if (this == null || other == null) null else this.deci.minus(other)
    operator fun BigDecimal?.times(other: BigDecimal?): Deci? = if (this == null || other == null) null else this.deci.times(other)
    operator fun BigDecimal?.div(other: BigDecimal?): Deci? = if (this == null || other == null) null else this.deci.div(other)
    operator fun BigDecimal?.rem(other: BigDecimal?): Deci? = if (this == null || other == null) null else this.deci.rem(other)

    operator fun BigDecimal?.plus(other: Deci?): Deci? = if (this == null || other == null) null else this.deci.plus(other)
    operator fun BigDecimal?.minus(other: Deci?): Deci? = if (this == null || other == null) null else this.deci.minus(other)
    operator fun BigDecimal?.times(other: Deci?): Deci? = if (this == null || other == null) null else this.deci.times(other)
    operator fun BigDecimal?.div(other: Deci?): Deci? = if (this == null || other == null) null else this.deci.div(other)
    operator fun BigDecimal?.rem(other: Deci?): Deci? = if (this == null || other == null) null else this.deci.rem(other)

    val BigDecimal.deci: Deci
        inline get() = Deci(this, this@DeciExpr.deciContext)

    actual val Int.deci: Deci
        inline get() = Deci(BigDecimal.valueOf(this.toLong()), this@DeciExpr.deciContext)

    actual val Long.deci: Deci
        inline get() = Deci(BigDecimal.valueOf(this), this@DeciExpr.deciContext)

    actual val String.deci: Deci
        inline get() = Deci(BigDecimal(this), this@DeciExpr.deciContext)

    private val Deci.deci: Deci
        inline get() = this.applyDeciContext(this@DeciExpr.deciContext)
}
