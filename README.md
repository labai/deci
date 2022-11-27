# deci

## deci.kt 
[deci.kt](../../tree/main/deci) - decimal class for Kotlin without tricks


### BigDecimal vs Deci examples

#### 1. Equals 
You would expect numbers are equal independently on decimal zeros.
It is not true with BigDecimal:
```kotlin
println(BigDecimal("1.0") == BigDecimal("1"))
```
> false

You need to use the `compareTo` instead of the `equals` for BigDecimal. 
With Deci it is correct:
```kotlin
println(Deci("1.0") == Deci("1"))
```
> true

#### 2. Dividing 
BigDecimal keeps the scale of the first argument when dividing
```kotlin
   println(BigDecimal("5") / BigDecimal("2"))
```
> 2

Deci use high scale - up to 20 decimals for precision or scale 
which is enough for most real world cases.

```kotlin
   println(BigDecimal("5") / BigDecimal("2"))
```
> 2.5

```kotlin
   println(BigDecimal("100000") / BigDecimal("3"))
```
> 33333.33333333333333333333

```kotlin
   println(BigDecimal("0.00001") / BigDecimal("3"))
```
> 0.0000033333333333333333333

#### 3. Rounding

BigDecimal use the half-even rounding by default, while Deci - half-up, which is more common

```kotlin
println(BigDecimal("2.5") / BigDecimal("2"))
```
> 1.2

```kotlin
println(Deci("2.5") / Deci("2") round 1)
```
> 1.3
