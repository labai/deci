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

/**
 * @author Augustus
 *   created on 2020.11.18
 * <p>
 * Latest version https://github.com/labai/deci/tree/main/deci
 * <h3>Deci</h3>
 * <p> Wrapped BigDecimal with features:
 * <ul>
 *   <li>Uses HALF_UP rounding
 *   <li>Division results with high scale (20+)
 *   <li>Math operators with BigDecimal, Int, Long
 *   <li>Equality (<code>==</code>) ignores scale (uses compareTo)
 *   <li>Scale and rounding mode can be set on the first element of a formula
 * </ul>
 *
 * <p><strong>Example:</strong></p>
 * <pre>
 * val d1: Deci = (price * quantity - fee) * 100 / (price * quantity) round 2
 * val d2: BigDecimal = ((1.deci - 1.deci / 365) * (1.deci - 2.deci / 365) round 11).toBigDecimal()
 * </pre>
 *
 * <p>
 * Additional infix functions
 *   <li><code>round</code> - round number by provided number of decimal, return Deci
 *   <li><code>eq</code> - comparison between numbers (various types, including null)
 *
 * <h5>DeciContext</h5>
 *
 * <p>If the default scale and rounding (20 and HALF_UP) are not suitable, a custom setup may be provided.
 * When creating Deci, pass an additional parameter - <code>DeciContext</code> (similar, but not identical, to MathContext).</p>
 *
 * <p>It contains the following fields:</p>
 * <ul>
 * <li><strong>scale</strong> - number of digits to keep after the decimal point</li>
 * <li><strong>precision</strong> - number of significant digits to retain when number is small and scale is not enough
 * <li><strong>roundingMode</strong> - rounding mode
 * </ul>
 *
 * <p><strong>Example:</strong></p>
 * <pre>
 * DeciContext(scale = 4, roundingMode = HALF_UP, precision = 3)
 * </pre>
 * <p>This means:</p>
 * <ul>
 *   <li>Keep 4 digits after the decimal when the number is large enough (i.e. 123.1234)</li>
 *   <li>But maintain at least 3 significant digits when the number is very small (i.e. 0.000123)</li>
 * </ul>
 *
 * <p>Default value:</p>
 * <pre>DeciContext(20, HALF_UP, 20)</pre>
 */
expect class Deci : Number, Comparable<Deci> {
    internal val deciContext: DeciContext

    constructor(str: String, deciContext: DeciContext)
    constructor(str: String)
    constructor(int: Int, deciContext: DeciContext)
    constructor(int: Int)
    constructor(long: Long, deciContext: DeciContext)
    constructor(long: Long)

    operator fun unaryMinus(): Deci

    override fun toByte(): Byte

    override fun toDouble(): Double
    override fun toFloat(): Float
    override fun toInt(): Int
    override fun toLong(): Long
    override fun toShort(): Short

    fun applyDeciContext(deciContext: DeciContext): Deci

    /** round to n decimals. Unlike BigDecimal.round(), here parameter 'scale' means scale, not precision */
    infix fun round(scale: Int): Deci

    override fun compareTo(other: Deci): Int

    override fun toString(): String

    override fun equals(other: Any?): Boolean

    override fun hashCode(): Int

    companion object {
        val ZERO: Deci
        internal val defaultDeciContext: DeciContext
        fun valueOf(int: Int): Deci
        fun valueOf(long: Long): Deci
    }
}
