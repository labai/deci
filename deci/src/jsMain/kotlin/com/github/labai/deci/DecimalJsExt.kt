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

import kotlin.js.js

internal external interface IDecimalJsClone {
    @JsName("clone")
    fun clone(config: dynamic = definedExternally): dynamic
}

@JsModule("decimal.js")
@JsNonModule
internal external val DecimalJsCon: IDecimalJsClone

internal object DecimalJsFactory {
    internal val defaultConstr = DecimalJsCon.clone(js(jsStrDefault))

    // set default precision with reserve (20 + 20)
    private const val jsStrDefault = "{ toExpPos: 999999999, toExpNeg: -999999999, precision: 40 }"
    private val cacheJs = mutableMapOf(
        20 to js("{precision:20}"),
        40 to js("{precision:40}"),
        100 to js("{precision:100}"),
        200 to js("{precision:200}"),
        400 to js("{precision:400}"),
        1000 to js("{precision:1000}"),
        2000 to js("{precision:2000}"),
        4000 to js("{precision:4000}"),
    )
    private val precisionLevels: List<Int> = cacheJs.keys.sorted()
    private val cacheCon = mutableMapOf(
        40 to defaultConstr
    )

    internal fun getForPrecision(precision: Int): dynamic {
        val precKey = getPrecisionLevel(precision)
        return cacheCon.getOrPut(precKey) {
            val jsCon = DecimalJsCon.clone(js(jsStrDefault))
            jsCon.set(cacheJs[precKey])
            jsCon
        }
    }

    internal fun getForDeciContext(ctx: DeciContext): dynamic {
        if (ctx == Deci.defaultDeciContext)
            return defaultConstr
        val precKey = getPrecisionLevel(ctx.precision + ctx.scale)
        return cacheCon.getOrPut(precKey) {
            val jsCon = DecimalJsCon.clone(js(jsStrDefault))
            jsCon.set(cacheJs[precKey])
            jsCon
        }
    }

    fun createDecimalJs(str: String): DecimalJs {
        return defaultConstr(str)
    }

    fun createDecimalJs(str: String, deciContext: DeciContext): DecimalJs {
        val decCon = getForDeciContext(deciContext)
        return decCon(str)
    }

    private fun getPrecisionLevel(precision: Int): Int {
        for (precLvl in precisionLevels) {
            if (precision <= precLvl)
                return precLvl
        }
        return precisionLevels.last()
    }
}
