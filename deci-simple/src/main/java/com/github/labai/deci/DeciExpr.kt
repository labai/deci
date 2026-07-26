/*
MIT License
Copyright (c) 2023 Augustus
*/
package com.github.labai.deci

import com.github.labai.deci.Deci.DeciContext
import java.math.BigDecimal

/**
 * An extension that enables the use of nullable Deci values in math expressions.
 *
 * <p>By default, Kotlin does not allow nullable values to be used directly in arithmetic
 * expressions. To preserve consistent behavior, {@code Deci} also does not allow nullable
 * values in mathematical operations.
 *
 * <p>This extension allows nullable values to participate in expressions by propagating
 * {@code null}: if any operand in the expression is {@code null}, the result is {@code null}.
 *
 * <p><strong>Example:</strong>
 * <pre>
 * val num: Deci? = null
 * val res: Deci? = deciExpr {
 *     3.deci + 2.deci * num
 * }
 * assertNull(res)
 * </pre>
 *
 * <p>A custom {@link DeciContext} can be provided to {@code deciExpr} when a different
 * scale or precision is required.
 *
 * <p><strong>Example:</strong>
 * <pre>
 * val ctx40 = DeciContext(scale = 40, roundingMode = HALF_UP, precision = 30)
 * val res = deciExpr(ctx40) {
 *     "1.0123456789012345678901234567890123456789".deci * "1e10".deci
 * }
 * assertEquals(
 *     "10123456789.012345678901234567890123456789",
 *     res.toString()
 * ) // scale is preserved inside deciExpr
 * </pre>
 *
 * <p>If {@code null} values should be treated as zero instead, use the
 * {@link Deci#orZero()} extension.
 *
 * <p><strong>Example:</strong>
 * <pre>
 * val num: Deci? = null
 * val res: Deci = 3.deci + 2.deci * num.orZero()
 * assertEquals(3.deci, res)
 * </pre>
 *
 * @author Augustus
 * @since 2023.11.08
 * @see Deci
 * @see DeciContext
 * @see #deciExpr(DeciContext, kotlin.jvm.functions.Function0)
 */
class DeciExpr(val deciContext: DeciContext = Deci.defaultDeciContext) {
    operator fun Deci?.unaryMinus(): Deci? = this?.unaryMinus()

    operator fun Deci?.plus(other: BigDecimal?): Deci? = if (this == null || other == null) null else this.deci.plusInternal(other)
    operator fun Deci?.minus(other: BigDecimal?): Deci? = if (this == null || other == null) null else this.deci.minusInternal(other)
    operator fun Deci?.times(other: BigDecimal?): Deci? = if (this == null || other == null) null else this.deci.timesInternal(other)
    operator fun Deci?.div(other: BigDecimal?): Deci? = if (this == null || other == null) null else this.deci.divInternal(other)
    operator fun Deci?.rem(other: BigDecimal?): Deci? = if (this == null || other == null) null else this.deci.remInternal(other)

    operator fun Deci?.plus(other: Deci?): Deci? = if (this == null || other == null) null else this.deci.plus(other.toBigDecimal())
    operator fun Deci?.minus(other: Deci?): Deci? = if (this == null || other == null) null else this.deci.minus(other.toBigDecimal())
    operator fun Deci?.times(other: Deci?): Deci? = if (this == null || other == null) null else this.deci.times(other.toBigDecimal())
    operator fun Deci?.div(other: Deci?): Deci? = if (this == null || other == null) null else this.deci.div(other.toBigDecimal())
    operator fun Deci?.rem(other: Deci?): Deci? = if (this == null || other == null) null else this.deci.rem(other.toBigDecimal())

    operator fun Deci?.plus(other: Int?): Deci? = if (this == null || other == null) null else this.deci.plus(BigDecimal.valueOf(other.toLong()))
    operator fun Deci?.minus(other: Int?): Deci? = if (this == null || other == null) null else this.deci.minus(BigDecimal.valueOf(other.toLong()))
    operator fun Deci?.times(other: Int?): Deci? = if (this == null || other == null) null else this.deci.times(BigDecimal.valueOf(other.toLong()))
    operator fun Deci?.div(other: Int?): Deci? = if (this == null || other == null) null else this.deci.div(BigDecimal.valueOf(other.toLong()))
    operator fun Deci?.rem(other: Int?): Deci? = if (this == null || other == null) null else this.deci.rem(BigDecimal.valueOf(other.toLong()))

    operator fun Deci?.plus(other: Long?): Deci? = if (this == null || other == null) null else this.deci.plus(BigDecimal.valueOf(other))
    operator fun Deci?.minus(other: Long?): Deci? = if (this == null || other == null) null else this.deci.minus(BigDecimal.valueOf(other))
    operator fun Deci?.times(other: Long?): Deci? = if (this == null || other == null) null else this.deci.times(BigDecimal.valueOf(other))
    operator fun Deci?.div(other: Long?): Deci? = if (this == null || other == null) null else this.deci.div(BigDecimal.valueOf(other))
    operator fun Deci?.rem(other: Long?): Deci? = if (this == null || other == null) null else this.deci.rem(BigDecimal.valueOf(other))

    operator fun Int?.unaryMinus(): Deci? = this?.deci.unaryMinus()

    operator fun Int?.plus(other: Long?): Deci? = if (this == null || other == null) null else this.deci.plus(other)
    operator fun Int?.minus(other: Long?): Deci? = if (this == null || other == null) null else this.deci.minus(other)
    operator fun Int?.times(other: Long?): Deci? = if (this == null || other == null) null else this.deci.times(other)
    operator fun Int?.div(other: Long?): Deci? = if (this == null || other == null) null else this.deci.div(other)
    operator fun Int?.rem(other: Long?): Deci? = if (this == null || other == null) null else this.deci.rem(other)

    operator fun Int?.plus(other: Int?): Deci? = if (this == null || other == null) null else this.deci.plus(other)
    operator fun Int?.minus(other: Int?): Deci? = if (this == null || other == null) null else this.deci.minus(other)
    operator fun Int?.times(other: Int?): Deci? = if (this == null || other == null) null else this.deci.times(other)
    operator fun Int?.div(other: Int?): Deci? = if (this == null || other == null) null else this.deci.div(other)
    operator fun Int?.rem(other: Int?): Deci? = if (this == null || other == null) null else this.deci.rem(other)

    operator fun Int?.plus(other: BigDecimal?): Deci? = if (this == null || other == null) null else this.deci.plus(other)
    operator fun Int?.minus(other: BigDecimal?): Deci? = if (this == null || other == null) null else this.deci.minus(other)
    operator fun Int?.times(other: BigDecimal?): Deci? = if (this == null || other == null) null else this.deci.times(other)
    operator fun Int?.div(other: BigDecimal?): Deci? = if (this == null || other == null) null else this.deci.div(other)
    operator fun Int?.rem(other: BigDecimal?): Deci? = if (this == null || other == null) null else this.deci.rem(other)

    operator fun Int?.plus(other: Deci?): Deci? = if (this == null || other == null) null else this.deci.plus(other)
    operator fun Int?.minus(other: Deci?): Deci? = if (this == null || other == null) null else this.deci.minus(other)
    operator fun Int?.times(other: Deci?): Deci? = if (this == null || other == null) null else this.deci.times(other)
    operator fun Int?.div(other: Deci?): Deci? = if (this == null || other == null) null else this.deci.div(other)
    operator fun Int?.rem(other: Deci?): Deci? = if (this == null || other == null) null else this.deci.rem(other)

    operator fun Long?.unaryMinus(): Deci? = this?.deci.unaryMinus()

    operator fun Long?.plus(other: Long?): Deci? = if (this == null || other == null) null else this.deci.plus(other)
    operator fun Long?.minus(other: Long?): Deci? = if (this == null || other == null) null else this.deci.minus(other)
    operator fun Long?.times(other: Long?): Deci? = if (this == null || other == null) null else this.deci.times(other)
    operator fun Long?.div(other: Long?): Deci? = if (this == null || other == null) null else this.deci.div(other)
    operator fun Long?.rem(other: Long?): Deci? = if (this == null || other == null) null else this.deci.rem(other)

    operator fun Long?.plus(other: Int?): Deci? = if (this == null || other == null) null else this.deci.plus(other)
    operator fun Long?.minus(other: Int?): Deci? = if (this == null || other == null) null else this.deci.minus(other)
    operator fun Long?.times(other: Int?): Deci? = if (this == null || other == null) null else this.deci.times(other)
    operator fun Long?.div(other: Int?): Deci? = if (this == null || other == null) null else this.deci.div(other)
    operator fun Long?.rem(other: Int?): Deci? = if (this == null || other == null) null else this.deci.rem(other)

    operator fun Long?.plus(other: BigDecimal?): Deci? = if (this == null || other == null) null else this.deci.plus(other)
    operator fun Long?.minus(other: BigDecimal?): Deci? = if (this == null || other == null) null else this.deci.minus(other)
    operator fun Long?.times(other: BigDecimal?): Deci? = if (this == null || other == null) null else this.deci.times(other)
    operator fun Long?.div(other: BigDecimal?): Deci? = if (this == null || other == null) null else this.deci.div(other)
    operator fun Long?.rem(other: BigDecimal?): Deci? = if (this == null || other == null) null else this.deci.rem(other)

    operator fun Long?.plus(other: Deci?): Deci? = if (this == null || other == null) null else this.deci.plus(other)
    operator fun Long?.minus(other: Deci?): Deci? = if (this == null || other == null) null else this.deci.minus(other)
    operator fun Long?.times(other: Deci?): Deci? = if (this == null || other == null) null else this.deci.times(other)
    operator fun Long?.div(other: Deci?): Deci? = if (this == null || other == null) null else this.deci.div(other)
    operator fun Long?.rem(other: Deci?): Deci? = if (this == null || other == null) null else this.deci.rem(other)

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

    val Int.deci: Deci
        inline get() = Deci(BigDecimal.valueOf(this.toLong()), this@DeciExpr.deciContext)

    val Long.deci: Deci
        inline get() = Deci(BigDecimal.valueOf(this), this@DeciExpr.deciContext)

    val String.deci: Deci
        inline get() = Deci(BigDecimal(this), this@DeciExpr.deciContext)

    private val Deci.deci: Deci
        inline get() = this.applyDeciContext(this@DeciExpr.deciContext)
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
