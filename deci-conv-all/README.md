# deci-conv-all

Several _Deci_ adapters for:

#### gson
```kotlin
gson = GsonBuilder()
   .registerTypeAdapter(Deci::class.java, GsonDeciRegister.deciTypeAdapter())
   .create();
```
#### jackson

```java
ObjectMapper mapper = new ObjectMapper();
mapper.registerModule(JacksonDeciConverters.deciTypeModule());
```
#### jpa

To register for JPA, need to add in package scanning configuration
```kotlin
 factory.setPackagesToScan("com.your.app.domain", Jpa2DeciRegister.PACKAGE);
```
