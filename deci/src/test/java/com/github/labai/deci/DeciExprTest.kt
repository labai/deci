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

import com.github.labai.deci.Deci.DeciContext
import org.junit.Test
import java.math.BigDecimal
import java.math.RoundingMode.HALF_UP
import kotlin.test.*

/**
 * @author Augustus
 *         created on 2023.11.09
 */
class DeciExprTest {

    @Test
    fun test_deciExpr_simple() {
        val num: Deci? = null

        val res: Deci? = deciExpr {
            3.deci + 2.deci * num
        }
        assertNull(res)
    }

    @Test
    fun test_deciExpr_with_null() {
        val num: Deci? = null

        assertNull(deciExpr { num + 1L })
        assertNull(deciExpr { num - 1L })
        assertNull(deciExpr { num * 1L })
        assertNull(deciExpr { num / 1L })

        assertNull(deciExpr { 1L + num })
        assertNull(deciExpr { 1L - num })
        assertNull(deciExpr { 1L * num })
        assertNull(deciExpr { 1L / num })

        // Kotlin: Overload resolution ambiguity - use long instead
        // assertNull(deciExpr { num + 1 })
        // assertNull(deciExpr { num - 1 })
        // assertNull(deciExpr { num * 1 })
        // assertNull(deciExpr { num / 1 })

        assertNull(deciExpr { 1 + num })
        assertNull(deciExpr { 1 - num })
        assertNull(deciExpr { 1 * num })
        assertNull(deciExpr { 1 / num })

        assertNull(deciExpr { num + BigDecimal.ONE })
        assertNull(deciExpr { num - BigDecimal.ONE })
        assertNull(deciExpr { num * BigDecimal.ONE })
        assertNull(deciExpr { num / BigDecimal.ONE })

        assertNull(deciExpr { BigDecimal.ONE + num })
        assertNull(deciExpr { BigDecimal.ONE - num })
        assertNull(deciExpr { BigDecimal.ONE * num })
        assertNull(deciExpr { BigDecimal.ONE / num })

        assertNull(deciExpr { num + 1.deci })
        assertNull(deciExpr { num - 1.deci })
        assertNull(deciExpr { num * 1.deci })
        assertNull(deciExpr { num / 1.deci })

        assertNull(deciExpr { 1.deci + num })
        assertNull(deciExpr { 1.deci - num })
        assertNull(deciExpr { 1.deci * num })
        assertNull(deciExpr { 1.deci / num })
    }

    @Test
    fun test_with_deci() {
        val num: Deci? = 5.deci

        assertDecEquals(15.deci, deciExpr { num + 10L })
        assertDecEquals((-5).deci, deciExpr { num - 10L })
        assertDecEquals(50.deci, deciExpr { num * 10L })
        assertDecEquals("0.5".deci, deciExpr { num / 10L })

        assertDecEquals(15.deci, deciExpr { 10L + num })
        assertDecEquals(5.deci, deciExpr { 10L - num })
        assertDecEquals(50.deci, deciExpr { 10L * num })
        assertDecEquals(2.deci, deciExpr { 10L / num })

        // Kotlin: Overload resolution ambiguity - use long instead
        // assertDecEquals(15.deci, deciExpr { num + 10 })
        // assertDecEquals((-5).deci, deciExpr { num - 10 })
        // assertDecEquals(50.deci, deciExpr { num * 10 })
        // assertDecEquals("0.5".deci, deciExpr { num / 10 })

        assertDecEquals(15.deci, deciExpr { 10 + num })
        assertDecEquals(5.deci, deciExpr { 10 - num })
        assertDecEquals(50.deci, deciExpr { 10 * num })
        assertDecEquals(2.deci, deciExpr { 10 / num })

        assertDecEquals(15.deci, deciExpr { num + BigDecimal.TEN })
        assertDecEquals((-5).deci, deciExpr { num - BigDecimal.TEN })
        assertDecEquals(50.deci, deciExpr { num * BigDecimal.TEN })
        assertDecEquals("0.5".deci, deciExpr { num / BigDecimal.TEN })

        assertDecEquals(15.deci, deciExpr { BigDecimal.TEN + num })
        assertDecEquals(5.deci, deciExpr { BigDecimal.TEN - num })
        assertDecEquals(50.deci, deciExpr { BigDecimal.TEN * num })
        assertDecEquals(2.deci, deciExpr { BigDecimal.TEN / num })

        assertDecEquals(15.deci, deciExpr { num + 10.deci })
        assertDecEquals((-5).deci, deciExpr { num - 10.deci })
        assertDecEquals(50.deci, deciExpr { num * 10.deci })
        assertDecEquals("0.5".deci, deciExpr { num / 10.deci })

        assertDecEquals(15.deci, deciExpr { 10.deci + num })
        assertDecEquals(5.deci, deciExpr { 10.deci - num })
        assertDecEquals(50.deci, deciExpr { 10.deci * num })
        assertDecEquals(2.deci, deciExpr { 10.deci / num })
    }

    @Test
    fun test_with_long() {
        val num: Long? = 5

        assertDecEquals(15.deci, deciExpr { num + 10L })
        assertDecEquals((-5).deci, deciExpr { num - 10L })
        assertDecEquals(50.deci, deciExpr { num * 10L })
        assertDecEquals("0.5".deci, deciExpr { num / 10L })

        assertDecEquals(15.deci, deciExpr { 10L + num })
        assertDecEquals(5.deci, deciExpr { 10L - num })
        assertDecEquals(50.deci, deciExpr { 10L * num })
        assertDecEquals(2.deci, deciExpr { 10L / num })

        // Kotlin: Overload resolution ambiguity - use long instead
        // assertDecEquals(15.deci, deciExpr { num + 10 })
        // assertDecEquals((-5).deci, deciExpr { num - 10 })
        // assertDecEquals(50.deci, deciExpr { num * 10 })
        // assertDecEquals("0.5".deci, deciExpr { num / 10 })

        assertDecEquals(15.deci, deciExpr { 10 + num })
        assertDecEquals(5.deci, deciExpr { 10 - num })
        assertDecEquals(50.deci, deciExpr { 10 * num })
        assertDecEquals(2.deci, deciExpr { 10 / num })

        assertDecEquals(15.deci, deciExpr { num + BigDecimal.TEN })
        assertDecEquals((-5).deci, deciExpr { num - BigDecimal.TEN })
        assertDecEquals(50.deci, deciExpr { num * BigDecimal.TEN })
        assertDecEquals("0.5".deci, deciExpr { num / BigDecimal.TEN })

        assertDecEquals(15.deci, deciExpr { BigDecimal.TEN + num })
        assertDecEquals(5.deci, deciExpr { BigDecimal.TEN - num })
        assertDecEquals(50.deci, deciExpr { BigDecimal.TEN * num })
        assertDecEquals(2.deci, deciExpr { BigDecimal.TEN / num })

        assertDecEquals(15.deci, deciExpr { num + 10.deci })
        assertDecEquals((-5).deci, deciExpr { num - 10.deci })
        assertDecEquals(50.deci, deciExpr { num * 10.deci })
        assertDecEquals("0.5".deci, deciExpr { num / 10.deci })

        assertDecEquals(15.deci, deciExpr { 10.deci + num })
        assertDecEquals(5.deci, deciExpr { 10.deci - num })
        assertDecEquals(50.deci, deciExpr { 10.deci * num })
        assertDecEquals(2.deci, deciExpr { 10.deci / num })
    }

    @Test
    fun test_with_int() {
        val num: Int? = 5

        assertDecEquals(15.deci, deciExpr { num + 10L })
        assertDecEquals((-5).deci, deciExpr { num - 10L })
        assertDecEquals(50.deci, deciExpr { num * 10L })
        assertDecEquals("0.5".deci, deciExpr { num / 10L })

        assertDecEquals(15.deci, deciExpr { 10L + num })
        assertDecEquals(5.deci, deciExpr { 10L - num })
        assertDecEquals(50.deci, deciExpr { 10L * num })
        assertDecEquals(2.deci, deciExpr { 10L / num  })

        // Kotlin: Overload resolution ambiguity - use long instead
        // assertDecEquals(15.deci, deciExpr { num + 10 })
        // assertDecEquals((-5).deci, deciExpr { num - 10 })
        // assertDecEquals(50.deci, deciExpr { num * 10 })
        // assertDecEquals("0.5".deci, deciExpr { num / 10 })

        assertDecEquals(15.deci, deciExpr { 10 + num })
        assertDecEquals(5.deci, deciExpr { 10 - num })
        assertDecEquals(50.deci, deciExpr { 10 * num })
        assertDecEquals(2.deci, deciExpr { 10 / num })

        assertDecEquals(15.deci, deciExpr { num + BigDecimal.TEN })
        assertDecEquals((-5).deci, deciExpr { num - BigDecimal.TEN })
        assertDecEquals(50.deci, deciExpr { num * BigDecimal.TEN })
        assertDecEquals("0.5".deci, deciExpr { num / BigDecimal.TEN })

        assertDecEquals(15.deci, deciExpr { BigDecimal.TEN + num })
        assertDecEquals(5.deci, deciExpr { BigDecimal.TEN - num })
        assertDecEquals(50.deci, deciExpr { BigDecimal.TEN * num })
        assertDecEquals(2.deci, deciExpr { BigDecimal.TEN / num })

        assertDecEquals(15.deci, deciExpr { num + 10.deci })
        assertDecEquals((-5).deci, deciExpr { num - 10.deci })
        assertDecEquals(50.deci, deciExpr { num * 10.deci })
        assertDecEquals("0.5".deci, deciExpr { num / 10.deci })

        assertDecEquals(15.deci, deciExpr { 10.deci + num })
        assertDecEquals(5.deci, deciExpr { 10.deci - num })
        assertDecEquals(50.deci, deciExpr { 10.deci * num })
        assertDecEquals(2.deci, deciExpr { 10.deci / num })
    }

    @Test
    fun test_with_bigDecimal() {
        val num: BigDecimal? = "5".toBigDecimal()

        assertDecEquals(15.deci, deciExpr { num + 10L })
        assertDecEquals((-5).deci, deciExpr { num - 10L })
        assertDecEquals(50.deci, deciExpr { num * 10L })
        assertDecEquals("0.5".deci, deciExpr { num / 10L })

        assertDecEquals(15.deci, deciExpr { 10L + num })
        assertDecEquals(5.deci, deciExpr { 10L - num })
        assertDecEquals(50.deci, deciExpr { 10L * num })
        assertDecEquals(2.deci, deciExpr { 10L / num })

        // Kotlin: Overload resolution ambiguity - use long instead
        // assertDecEquals(15.deci, deciExpr { num + 10 })
        // assertDecEquals((-5).deci, deciExpr { num - 10 })
        // assertDecEquals(50.deci, deciExpr { num * 10 })
        // assertDecEquals("0.5".deci, deciExpr { num / 10 })

        assertDecEquals(15.deci, deciExpr { 10 + num })
        assertDecEquals(5.deci, deciExpr { 10 - num })
        assertDecEquals(50.deci, deciExpr { 10 * num })
        assertDecEquals(2.deci, deciExpr { 10 / num })

        assertDecEquals(15.deci, deciExpr { num + BigDecimal.TEN })
        assertDecEquals((-5).deci, deciExpr { num - BigDecimal.TEN })
        assertDecEquals(50.deci, deciExpr { num * BigDecimal.TEN })
        assertDecEquals("0.5".deci, deciExpr { num / BigDecimal.TEN })

        assertDecEquals(15.deci, deciExpr { BigDecimal.TEN + num })
        assertDecEquals(5.deci, deciExpr { BigDecimal.TEN - num })
        assertDecEquals(50.deci, deciExpr { BigDecimal.TEN * num })
        assertDecEquals(2.deci, deciExpr { BigDecimal.TEN / num })

        assertDecEquals(15.deci, deciExpr { num + 10.deci })
        assertDecEquals((-5).deci, deciExpr { num - 10.deci })
        assertDecEquals(50.deci, deciExpr { num * 10.deci })
        assertDecEquals("0.5".deci, deciExpr { num / 10.deci })

        assertDecEquals(15.deci, deciExpr { 10.deci + num })
        assertDecEquals(5.deci, deciExpr { 10.deci - num })
        assertDecEquals(50.deci, deciExpr { 10.deci * num })
        assertDecEquals(2.deci, deciExpr { 10.deci / num })
    }

    @Test
    fun test_deciContext_simple() {
        val ctx4 = DeciContext(scale = 4, roundingMode = HALF_UP, precision = 3)

        var dec: Deci? = deciExpr(ctx4) { 1 }
        assertEquals(ctx4, dec!!.context)

        dec = deciExpr(ctx4) { 1.deci + "2.123456".deci }
        assertEquals(ctx4, dec!!.context)
        assertEquals("3.1235", dec.toString())

        dec = Deci.valueOf(2, ctx4)
        assertEquals(ctx4, dec.context)

        dec = Deci.valueOf(2.deci, ctx4)
        assertEquals(ctx4, dec.context)
    }

    @Test
    fun test_deciContext_whenNotProvided_thenUseDefault() {
        // default is 20, we are losing decimals after 20+ digits inside expression
        val dec = deciExpr { "1.0123456789012345678901234567890123456789".deci * "1e10".deci }
        assertEquals("10123456789.0123456789", dec.toString())
    }

    @Test
    fun test_deciContext_whenProvided_thenUseIt() {
        // with bigger context (40), we should keep the scale inside expression
        val ctx40 = DeciContext(scale = 40, roundingMode = HALF_UP, precision = 30)
        val dec = deciExpr(ctx40) { "1.0123456789012345678901234567890123456789".deci * "1e10".deci }
        assertEquals(ctx40, dec!!.context)
        assertEquals("10123456789.012345678901234567890123456789", dec.toString())
    }

    @Test
    fun test_deciContext_whenNotProvidedAndDeci_thenUseOriginal() {
        // when default (no context provided) - leave deciContext of original first deci element
        val ctx4 = DeciContext(scale = 4, roundingMode = HALF_UP, precision = 3)
        val dec1 = Deci("2".toBigDecimal(), ctx4)
        val dec = deciExpr { dec1 + 1 }
        assertEquals(ctx4, dec!!.context)
    }

    private fun assertDecEquals(dec1: Deci, dec2: Deci?) = assertTrue(dec1 eq dec2, "Decimals are not equal ($dec1 vs $dec2)")
}