package com.github.labai.deci.converter

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.labai.deci.Deci
import org.junit.Test
import com.github.labai.deci.converter.jackson.JacksonDeciRegister
import kotlin.test.assertEquals

/**
 * @author Augustus
 * created on 2020.11.29
 */
class JacksonDeciTest {
    val objectMapper: ObjectMapper
    init {
        objectMapper = ObjectMapper()
        objectMapper.registerModule(JacksonDeciRegister.deciTypeModule())
        //objectMapper.writerWithDefaultPrettyPrinter()
    }

    class Data1 {
        var deci: Deci? = null
        var str: String? = null

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as Data1
            if (deci != other.deci) return false
            if (str != other.str) return false
            return true
        }

        override fun hashCode(): Int {
            var result = deci?.hashCode() ?: 0
            result = 31 * result + (str?.hashCode() ?: 0)
            return result
        }

        override fun toString(): String {
            return "Data1(deci=$deci, str=$str)"
        }
    }

    @Test
    fun test_jackson() {
        val d = Data1()
        d.deci = Deci("-12.345")
        d.str = "abra"
        val json = objectMapper.writeValueAsString(d)
        println(json)
        assertEquals("""{"deci":-12.345,"str":"abra"}""", json)
        val d2 = objectMapper.readValue(json, Data1::class.java)
        println(d2)
        assertEquals("Data1(deci=-12.345, str=abra)", d2.toString())
    }
}
