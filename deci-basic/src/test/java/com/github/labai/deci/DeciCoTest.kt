package com.github.labai.deci

import com.github.labai.deci.Deci.DeciContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode.HALF_UP

internal class DeciCoTest {

    @ParameterizedTest
    @CsvSource(
        "-1,  10",  // scale below minimum
        "201, 10",  // scale above maximum
    )
    fun deciContext_invalidScale_throws(scale: Int, precision: Int) {
        assertThrows(IllegalStateException::class.java) { DeciContext(scale, HALF_UP, precision) }
    }

    @ParameterizedTest
    @CsvSource(
        "10, 0",    // precision below minimum
        "10, 201",  // precision above maximum
    )
    fun deciContext_invalidPrecision_throws(scale: Int, precision: Int) {
        assertThrows(IllegalStateException::class.java) { DeciContext(scale, HALF_UP, precision) }
    }

    @Test
    fun deciContext_boundaryValues_allowed() {
        assertEquals(0, DeciContext(0, HALF_UP, 1).scale)
        assertEquals(200, DeciContext(200, HALF_UP, 200).scale)
    }

    @Test
    fun numberConversions() {
        val d = Deci("12.7")

        assertEquals(12, d.toInt())
        assertEquals(12L, d.toLong())
        assertEquals(12.toByte(), d.toByte())
        assertEquals(12.toShort(), d.toShort())
        assertEquals(12.7, d.toDouble())
        assertEquals(12.7f, d.toFloat())
    }

    @Test
    fun constructor_negativeScaleBigDecimal_normalizedToZeroScale() {
        // scale() < 0 e.g. "1.2E+3" has scale -2 -> constructor rounds to scale 0
        val negativeScaleDecimal = BigDecimal("1.2E+3")
        assertTrue(negativeScaleDecimal.scale() < 0)

        val result = Deci(negativeScaleDecimal)

        assertEquals(0, result.toBigDecimal().scale())
        assertEquals("1200", result.toString())
    }

    @Test
    fun nullableRound_returnsNullForNullReceiver() {
        val num: Deci? = null

        assertNull(num round 2)
    }

    @Test
    fun nullableToBigDecimal_returnsNullForNullReceiver() {
        val num: Deci? = null

        assertNull(num.toBigDecimal())
    }

    @Test
    fun nullableEq_bothNull_isTrue() {
        val d1: Deci? = null
        val d2: Deci? = null
        val bd: BigDecimal? = null
        val n: Number? = null

        assertTrue(d1 eq d2)
        assertTrue(d1 eq bd)
        assertTrue(d1 eq n)
    }

    @Test
    fun nullableEq_oneNull_isFalse() {
        val nullDeci: Deci? = null

        assertFalse(nullDeci eq 1.deci)
        assertFalse(1.deci eq nullDeci)
        assertFalse(nullDeci eq BigDecimal.ONE)
        assertFalse(nullDeci eq (1 as Number))
    }

    @Test
    fun bigDecimal_eq_deci_extension() {
        assertTrue(BigDecimal("2.20") eq Deci("2.2"))
        assertFalse(BigDecimal("2.21") eq Deci("2.2"))
    }

    @Test
    fun compareTo_number_otherNumericTypes() {
        val d = 2.deci

        assertEquals(0, d.compareTo(2.0))
        assertEquals(0, d.compareTo(2.0f))
        assertEquals(0, d.compareTo(2.toByte()))
        assertEquals(0, d.compareTo(2.toShort()))
        // fallback branch: unknown Number subtype -> parsed via toString()
        assertEquals(0, d.compareTo(BigInteger.valueOf(2)))
    }

    @Test
    fun valueOf_number_fallbackBranch_usesToString() {
        val result = Deci.valueOf(BigInteger("42"))

        assertEquals(42.deci, result)
    }

    @Test
    fun valueOf_number_passesThroughExistingDeci() {
        val existing = Deci("3.3")

        val result = Deci.valueOf(existing as Number)

        assertTrue(result === existing)
    }

    @Test
    fun orZero_nonNull_returnsSameValue() {
        val num: Deci? = Deci("7.5")

        assertEquals(Deci("7.5"), num.orZero())
    }
}
