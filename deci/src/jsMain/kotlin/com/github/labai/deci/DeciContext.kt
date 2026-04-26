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


/*
 * @author Augustus
 * created on 2025-12-21
 *
 * JS version of DeciContext
*/

actual interface DeciContext {
    actual val scale: Int
    actual val roundingMode: RoundingMode
    actual val precision: Int

    fun withConfig(config: DeciContextConfig): DeciContext
    val config: DeciContextConfig

    actual companion object {
        actual fun of(scale: Int, roundingMode: RoundingMode, precision: Int): DeciContext {
            return DeciContextImpl(scale, roundingMode, precision)
        }

        actual fun of(scale: Int, roundingMode: RoundingMode): DeciContext {
            return DeciContextImpl(scale, roundingMode)
        }

        actual fun of(scale: Int): DeciContext {
            return DeciContextImpl(scale, RoundingMode.HALF_UP)
        }
    }
}

internal class DeciContextImpl : DeciContext {
    override val scale: Int
    override val roundingMode: RoundingMode
    override val precision: Int

    constructor(scale: Int, roundingMode: RoundingMode) : this(scale, roundingMode, scale)

    constructor(scale: Int, roundingMode: RoundingMode, precision: Int) {
        this.scale = scale
        this.roundingMode = roundingMode
        this.precision = precision
        check(scale >= 0) { "scale must be >= 0 (is $scale)" }
        check(scale <= 2000) { "scale must be <= 2000 (is $scale)" }
        check(precision >= 1) { "precision must be >= 1 (is $precision)" }
        check(precision <= 2000) { "precision must be <= 2000 (is $precision)" }
    }

    override fun toString(): String = "DeciContext($scale:$precision:${roundingMode})"

    @Suppress("UNNECESSARY_SAFE_CALL") // is undefined while initiating Deci.defaultDeciContex itself
    override var config: DeciContextConfig = Deci.defaultDeciContext ?.config ?: DeciContextConfig()
        private set

    override fun withConfig(config: DeciContextConfig): DeciContext {
        val ctx = DeciContextImpl(scale, roundingMode, precision)
        ctx.config = config
        return ctx
    }
}

internal val DeciContext.jsRoundingMode: Int
    get() = roundingMode.toJs()

class DeciContextConfig(
    val roundToScale: Boolean = true
)

internal fun RoundingMode.toJs(): Int = when (this) {
    RoundingMode.HALF_UP -> 4
    RoundingMode.DOWN -> 1
    RoundingMode.HALF_EVEN -> 6
    RoundingMode.UP -> 0
    RoundingMode.HALF_DOWN -> 5
    RoundingMode.CEILING -> 2
    RoundingMode.FLOOR -> 3
}
