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

import com.github.labai.deci.Deci.CtxMixed
import com.github.labai.deci.DeciContextImpl.Companion.MASK_25BITS
import java.io.Serializable
import java.math.RoundingMode as JavaRoundingMode

/*
 * @author Augustus
 * created on 2025-12-21
 *
 * JVM version of DeciContext
 *
*/
actual interface DeciContext : Serializable {
    actual val scale: Int
    actual val roundingMode: RoundingMode
    actual val precision: Int

    actual companion object {
        actual fun of(scale: Int, roundingMode: RoundingMode, precision: Int): DeciContext {
            return DeciContextImpl(scale, roundingMode, precision)
        }

        actual fun of(scale: Int, roundingMode: RoundingMode): DeciContext {
            return DeciContextImpl(scale, roundingMode, scale)
        }

        actual fun of(scale: Int): DeciContext {
            return DeciContextImpl(scale, RoundingMode.HALF_UP, scale)
        }
    }
}

internal class DeciContextImpl : DeciContext {
    override val scale: Int
    override val roundingMode: RoundingMode
    override val precision: Int
    internal val mixed: Int

    constructor(scale: Int, roundingMode: RoundingMode, precision: Int) {
        this.scale = scale
        this.roundingMode = roundingMode
        this.precision = precision
        check(scale >= 0) { "scale must be >= 0 (is $scale)" }
        check(scale <= 200) { "scale must be <= 200 (is $scale)" } // could be increased to 2k
        check(precision >= 1) { "precision must be >= 1 (is $precision)" }
        check(precision <= 200) { "precision must be <= 200 (is $precision)" } // could be increased to 2k
        this.mixed = convDeciCtxValue(scale, roundingMode, precision)
    }

    override fun toString(): String = "DeciContext($scale:$precision:${roundingMode.toString().lowercase()})"

    companion object {
        internal const val MASK_3BITS = 0b111
        internal const val MASK_11BITS = 0b111_11111111
        internal const val MASK_25BITS = 0b00000001_11111111_11111111_11111111
        internal const val MASK_25BITS_INV = 0b01111110_00000000_00000000_00000000

        // convert to 25 bits
        internal fun convDeciCtxValue(ctx: DeciContext): Int {
            return convDeciCtxValue(ctx.scale, ctx.roundingMode, ctx.precision)
        }
        internal fun convDeciCtxValue(scale: Int, roundingMode: RoundingMode, precision: Int): Int {
            return ((roundingMode.ordinal and MASK_3BITS) shl 22) or
                ((precision and MASK_11BITS) shl 11) or
                (scale and MASK_11BITS)
        }
    }
}

internal class DeciContextMixedImpl(internal val mixed: CtxMixed): DeciContext {
    override val precision: Int
        get() = mixed.precision()
    override val roundingMode: RoundingMode
        get() = mixed.roundingMode()
    override val scale: Int
        get() = mixed.scale()
}

internal val DeciContext.javaRoundingMode: JavaRoundingMode
    get() = this.roundingMode.toJava()

internal fun RoundingMode.toJava(): JavaRoundingMode = when (this) {
    RoundingMode.HALF_UP -> JavaRoundingMode.HALF_UP
    RoundingMode.DOWN -> JavaRoundingMode.DOWN
    RoundingMode.HALF_EVEN -> JavaRoundingMode.HALF_EVEN
    RoundingMode.UP -> JavaRoundingMode.UP
    RoundingMode.HALF_DOWN -> JavaRoundingMode.HALF_DOWN
    RoundingMode.CEILING -> JavaRoundingMode.CEILING
    RoundingMode.FLOOR -> JavaRoundingMode.FLOOR
}

internal fun CtxMixed.isDeciCtxEqual(other: DeciContext): Boolean {
    return other.isDeciCtxEqual(this)
}

internal fun DeciContext.isDeciCtxEqual(other: CtxMixed): Boolean {
    val mixed = when (this) {
        is DeciContextImpl -> this.mixed
        is DeciContextMixedImpl -> this.mixed.raw
        else -> 0
    }
    if (mixed != 0)
        return (other.raw and MASK_25BITS) == (mixed and MASK_25BITS)

    if (this.scale != other.scale())
        return false
    if (this.precision != other.precision())
        return false
    if (this.roundingMode != other.roundingMode())
        return false

    return true
}

internal fun DeciContext.isDeciCtxEqual(other: DeciContext): Boolean {
    if (this === other)
        return true

    val mixed1 = when (this) {
        is DeciContextImpl -> this.mixed
        is DeciContextMixedImpl -> this.mixed.raw
        else -> 0
    }

    if (mixed1 != 0) {
        val mixed2 = when (other) {
            is DeciContextImpl -> other.mixed
            is DeciContextMixedImpl -> other.mixed.raw
            else -> 0
        }
        if (mixed2 != 0)
            return (mixed1 and MASK_25BITS) == (mixed2 and MASK_25BITS)
    }

    if (scale != other.scale)
        return false
    if (precision != other.precision)
        return false
    if (roundingMode != other.roundingMode)
        return false

    return true
}
