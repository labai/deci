# Deci – a decimal class for Kotlin without tricks

Working with decimals in Java is unpleasant.  
The `float` and `double` classes can be used, but they rely on floating-point arithmetic and are not recommended when fixed-point precision is required (e.g., in financial calculations).  
For such cases, the `BigDecimal` class is typically used.

Unfortunately, code written with `BigDecimal` is often verbose and difficult to read. In addition, `BigDecimal` has several pitfalls (e.g., `equals` behavior, loss of scale during division).

*Kotlin* provides operators (`-`, `+`, `*`, `/`), which should improve readability. However, some problems inherited from `BigDecimal` still remain. For example:
- Division (`div`) uses the original scale and applies `HALF_EVEN` rounding, which is not suitable in most situations. As a result, the division operator (`/`) often cannot be used directly in formulas.
- Equality checks take scale into account, so `2.0 != 2.00`. Therefore, `compareTo` must be used instead of `==`.

To address these issues, the *Deci* class can be used.  
The idea is to create a simple `BigDecimal` wrapper that behaves slightly differently:
- Uses `HALF_UP` rounding
- Produces division results with a high scale
- Provides additional math operators with `BigDecimal`, `Int`, and `Long`
- Equality (`==`) ignores scale

Additional functions:
- `round` – rounds a number to the specified number of decimal places and returns a `Deci`
- `eq` – compares numbers of various types (including `null`)
- `BigDecimal`, `Int`, and `Long` have `.deci` extension functions to convert values to *Deci*

## Math in code

With _Deci_, you can use operators, making formulas easier to read compared to method calls with _BigDecimal_.

```kotlin
val result = (price * quantity - fee) * 100 / (price * quantity) round 2
```

## BigDecimal vs Deci examples

### 1. Equals 

You would expect numbers to be equal regardless of trailing decimal zeros.
This is not true for _BigDecimal_:

```kotlin
println(BigDecimal("1.0") == BigDecimal("1"))
```
> false

With _BigDecimal_, you must use `compareTo` instead of `equals`.
With _Deci_, the behavior is as expected:

```kotlin
println(Deci("1.0") == Deci("1"))
```
> true

### 2. Dividing 

_BigDecimal_ keeps the scale of the first operand when dividing:

```kotlin
   println(BigDecimal("5") / BigDecimal("2"))
```
> 2

_Deci_ uses a high scale (up to 20 decimal places), which is sufficient for most real-world cases.

```kotlin
   println(5.deci / 2.deci)
```
> 2.5

```kotlin
   println(100000.deci / 3.deci)
```
> 33333.33333333333333333333

```kotlin
   println(Deci("0.00001") / 3.deci)
```
> 0.0000033333333333333333333

### 3. Rounding

_BigDecimal_ uses half-even rounding by default, while _Deci_ uses half-up rounding, which is more common.

```kotlin
println(BigDecimal("2.5") / BigDecimal("2"))
```
> 1.2

```kotlin
println(Deci("2.5") / Deci("2") round 1)
```
> 1.3


### Usage

Add the Maven dependency:

<details>
<summary><strong>Maven</strong></summary>

```xml
<dependency>
  <groupId>com.github.labai</groupId>
  <artifactId>deci</artifactId>
  <version>0.0.1</version>
</dependency>
```
</details>

## deci.kt
[more info](../../tree/main/deci) 

