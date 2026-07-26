package com.github.labai.deci.impl

import com.github.labai.deci.Deci
import com.github.labai.deci.Deci.CtxMixed
import com.github.labai.deci.DeciContext
import com.github.labai.deci.RoundingMode
import com.github.labai.deci.impl.TinyUDecMath.TWOINT_ERR
import com.github.labai.deci.impl.TinyUDecMath.TwoInt
import java.math.BigDecimal

/**
 * @author Augustus
 * created on 2026-05-17
 *
 * currently for testing only
 */

internal fun TwoInt.toTinyDec() = if (this == TWOINT_ERR) TinyDec.ERR else TinyDec.buildTinyOrErr(this.first(), this.second())

internal fun TinyDec.add(other: TinyDec) = TinyUDecMath.addOrErr(this.unscaled(), this.pos(), other.unscaled(), other.pos()).toTinyDec()
internal fun TinyDec.sub(other: TinyDec) = TinyUDecMath.subOrErr(this.unscaled(), this.pos(), other.unscaled(), other.pos()).toTinyDec()
internal fun TinyDec.mul(other: TinyDec) = TinyUDecMath.mulOrErr(this.unscaled(), this.pos(), other.unscaled(), other.pos()).toTinyDec()
internal fun TinyDec.tryDiv(other: TinyDec) = TinyUDecMath.tryDivOrErr(this.unscaled(), this.pos(), other.unscaled(), other.pos()).toTinyDec()
internal fun TinyDec.rem(other: TinyDec) = TinyUDecMath.remOrErr(this.unscaled(), this.pos(), other.unscaled(), other.pos()).toTinyDec()
internal fun TinyDec.round(scale: Int, roundingMode: RoundingMode) = TinyUDecMath.round(this.unscaled(), this.pos(), scale, roundingMode).toTinyDec()

internal fun TinyDec.intPart() = TinyUDecMath.getIntPart(unscaled(), pos())
internal fun TinyDec.isEqual(other: TinyDec): Boolean = TinyUDecMath.isEqual(this.unscaled(), this.pos(), other.unscaled(), other.pos())
internal fun TinyDec.isEqual(other: TinyDec4d): Boolean = TinyUDecMath.isEqual(this.unscaled(), this.pos(), other.unscaled(), other.pos())
internal fun TinyDec.compareTo(other: TinyDec) = TinyUDecMath.compare(this.unscaled(), this.pos(), other.unscaled(), other.pos())
internal fun TinyDec.compareTo(other: TinyDec4d) = TinyUDecMath.compare(this.unscaled(), this.pos(), other.unscaled(), other.pos())

// access private methods/fields - for testing only

internal fun Deci.priv_createFromUnscaledPos(unscaled: Int, pos: Int, neg: Boolean, deciCtx: CtxMixed) : Deci? {
    val method = Deci::class.java.getDeclaredMethod("createFromUnscaledPos", Int::class.java, Int::class.java, Boolean::class.java, Int::class.java)
    method.isAccessible = true
    return method.invoke(this, unscaled, pos, neg, deciCtx.raw) as Deci?
}

internal fun Deci.priv_calcDivScale(dec: BigDecimal) : Int {
    val method = Deci::class.java.getDeclaredMethod("calcDivScale", BigDecimal::class.java)
    method.isAccessible = true
    return method.invoke(this, dec) as Int
}

internal fun Deci.priv_tryInitTinyDec() {
    val method = Deci::class.java.getDeclaredMethod("tryInitTinyDec")
    method.isAccessible = true
    method.invoke(this)
}

internal fun Deci.priv_decimal(): BigDecimal? {
    val field = Deci::class.java.getDeclaredField("decimal")
    field.isAccessible = true
    return field.get(this) as BigDecimal?
}

internal fun Deci.priv_tinyDec(): TinyDec {
    val field = Deci::class.java.getDeclaredField("tinyDec")
    field.isAccessible = true
    val int = field.get(this) as Int
    return TinyDec(int)
}
