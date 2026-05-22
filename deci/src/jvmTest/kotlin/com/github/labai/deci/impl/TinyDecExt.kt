package com.github.labai.deci.impl

import com.github.labai.deci.RoundingMode
import com.github.labai.deci.impl.TinyUDecMath.TWOINT_ERR
import com.github.labai.deci.impl.TinyUDecMath.TwoInt

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
