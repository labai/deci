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
 *
 * <p>Defines numeric formatting and precision rules for Deci calculations.
 *
 * <p>This context controls how decimal numbers are represented, rounded, and
 * constrained during arithmetic operations.</p>
 *
 * <h3>Key concepts</h3>
 * <ul>
 *   <li><b>Scale</b> – the target number of digits after the decimal point.</li>
 *   <li><b>Precision</b> – the minimum number of significant fractional digits that must be preserved.</li>
 *   <li><b>Rounding mode</b> – determines how values are rounded when required.</li>
 * </ul>
 *
 * <p>For very small numbers, the actual number of fractional digits may exceed
 * <code>scale</code> in order to preserve the required {@code precision}.</p>
 *
 * <h3>Examples</h3>
 * With <code>scale = 4</code> and <code>precision = 3</code>, the following values are valid:
 * <ul>
 *   <li><code>1.3333</code> – scale is exactly 4, precision exceeds 3</li>
 *   <li><code>0.000333</code> – precision is preserved even though scale exceeds 4</li>
 * </ul>
 *
 * <p>Platform-specific behavior configuration may be applied additionally</p>
 *
 */
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
expect class DeciContext {
    constructor(scale: Int, roundingMode: RoundingMode, precision: Int)
    constructor(scale: Int, roundingMode: RoundingMode)
    constructor(scale: Int)

    val scale: Int
    val roundingMode: RoundingMode
    val precision: Int
}
