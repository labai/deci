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

import com.github.labai.deci.Deci
import com.github.labai.deci.DeciContext
import com.github.labai.deci.toIos
import platform.Foundation.NSDecimalNumber
import platform.Foundation.NSDecimalNumberHandler
import kotlin.math.absoluteValue
import kotlin.math.log10
import kotlin.math.max

/*
 * @author Augustus
 * created on 2026-01-03
 *
 * Internal (private) helpers and utils.
 *
 * Not for API!
 */
internal object Utils {

    internal fun calcExponent(decimal: NSDecimalNumber): Int {
        val dbl = decimal.doubleValue.absoluteValue
        if (dbl == 0.0) return 0
        return log10(dbl).toInt()
    }

    internal fun calcScale(d: NSDecimalNumber, ctx: DeciContext): Int {
        val exp = calcExponent(d)
        return max(ctx.scale, ctx.precision + max(exp - 1, 0))
    }

    internal fun toRoundedDeci(d: NSDecimalNumber, ctx: DeciContext): Deci {
        if (!ctx.config.roundToScale)
            return Deci(d, ctx)
        val scale = calcScale(d, ctx)
        return round(d, scale, ctx)
    }

    internal fun round(decimal: NSDecimalNumber, scale: Int, ctx: DeciContext): Deci {
        val handler = NSDecimalNumberHandler(
            ctx.roundingMode.toIos(decimal),
            scale.toShort(),
            raiseOnExactness = false,
            raiseOnOverflow = false,
            raiseOnUnderflow = false,
            raiseOnDivideByZero = false
        )
        return Deci(decimal.decimalNumberByRoundingAccordingToBehavior(handler), ctx)
    }
}
