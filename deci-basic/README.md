# Deci

Work with decimals in Java is unpleasant.
The float and double classes can be used 
but they use a floating point and are not recommended for cases, 
when fixed point is required, e.g. in financial calculations.
Instead, the BigDecimal class is for such cases.

But unfortunately, a code, written with BigDecimals, is ugly, difficult to read.
In addition, BigDecimal has pitfalls (e.g. equals, lose scale on division).

Luckily, kotlin have operators (- + * /), which should improve readability.
Unfortunately, it still has some problems, derived from BigDecimal, an example:
- div (division) use original scale and the HALF_EVEN rounding is used,
which is not useful in most of the situations. Thus, normally you can't use div 
operator (/) in your formulas
- equality check takes into account a scale, so 2.0 != 2.00. So you need to use compareTo instead of "==" check

To solve these problems the *Deci* class can be used. 
Idea is to create simple BigDecimal wrapper, which behaves slightly differently:
- use HALF_UP rounding
- division result with high scale
- additional math operators with BigDecimal, Int, Long
- equal ('==') ignores scale

Few additional functions:
- round - round number by provided count of decimal places, return Deci
- eq - comparison between numbers (various types, including null)
- BigDecimal, Int and Long classes have extension functions *.deci* 
to convert to *Deci*.


#### Examples

```kotlin
val d1: Deci = (price * quantity - fee) * 100 / (price * quantity) round 2
```
```kotlin
val d2: BigDecimal = ((1.deci - 1.deci / 365) * (1.deci - 2.deci / 365) round 11).toBigDecimal()
```

#### Usage
Use maven dependency:

```xml
<dependency>
    <groupId>com.github.labai</groupId>
    <artifactId>deci</artifactId>
    <version>0.0.1</version>
</dependency>
```

### Nullables

#### Extension deciExpr

By default, nullable variables are not allowed to be used in math expressions in kotlin.
To keep consistent behaviour, _Deci_ also doesn't allow nullables in expressions.

But you may use `deciExpr` extension to allow nullables in math expression with such logic,
that if any of part is null, then result is null.

Example:
```kotlin
  val num: Deci? = null
  val res: Deci? = deciExpr {
     3.deci + 2.deci * num
  }
  assertNull(res)
```

#### Extension orZero

In case you want nulls treat as zeros, you may just use an extension `Deci?.orZero()`.

Example:
```kotlin
  val num: Deci? = null
  val res: Deci = 3.deci + 2.deci * num.orZero()
  assertEquals(3.deci, res)
```

### DeciContext

In case default scale and rounding (20 and round_up) is not suitable, it is possible to use own setup.
When creating a Deci number, provide additional parameter - DeciContext.

It has such fields:
- scale - indicates, how many digits need to keep after dot;
- precision - indicates, how many significant digits to keep, when number is small and scale is not enough;
- roundingMode - rounding mode (java.math.RoundingMode).

Example:
```kotlin
DeciContext(scale = 4, roundingMode = HALF_UP, precision = 3)
```
means to keep 4 numbers after dot, but not less than 3 significant number, e.g.:
- 123.1234 - number big enough, keep 4 digits after dot
- 0.000123 - number is smaller and 4 digits after dot is not enough - keep minimum 3 significant digits

Default is ```DeciContext(20, HALF_UP, 20)```
