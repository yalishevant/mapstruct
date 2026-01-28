# KSP Demo: Generating Java Mappers from Kotlin Code

This demo project demonstrates how to use **KSP (Kotlin Symbol Processing)** to generate **Java mapper implementations** from Kotlin interfaces annotated with `@Mapper`.

## Related Components

This demo is part of MapStruct KSP integration:

- **`processor-ksp/`** — Full MapStruct KSP processor module (Maven, integrates with MapStruct architecture)
- **`ksp-demo/`** — Standalone proof-of-concept demonstrating the core approach (Gradle)

## What This Demo Shows

- KSP processor that reads Kotlin mapper interfaces
- Generates Java source code (not Kotlin!) using JavaPoet
- Supports `@Mapping` annotations for field renaming
- Generated Java code is fully compatible with Kotlin consumers

## Project Structure

```
ksp-demo/
├── processor/          # KSP processor module
│   └── src/main/kotlin/org/mapstruct/ksp/
│       ├── Mapper.kt           # @Mapper and @Mapping annotations
│       └── MapperProcessor.kt  # KSP processor (generates Java code)
│
└── sample/             # Sample usage module
    └── src/
        ├── main/kotlin/org/mapstruct/demo/
        │   ├── Models.kt       # Source and target data classes
        │   └── Mappers.kt      # Mapper interfaces
        └── test/kotlin/org/mapstruct/demo/
            └── MapperTest.kt   # Tests verifying generated mappers work
```

## How It Works

### 1. Define Kotlin Models

```kotlin
// Source - Kotlin data class
data class Person(
    val firstName: String,
    val lastName: String,
    val age: Int
)

// Target - mutable class (needs setters for Java-style mapping)
class PersonDto {
    var firstName: String = ""
    var lastName: String = ""
    var age: Int = 0
}
```

### 2. Define Mapper Interface

```kotlin
@Mapper
interface PersonMapper {
    fun toDto(person: Person): PersonDto
}

// With field renaming
@Mapper
interface AddressMapper {
    @Mapping(source = "zipCode", target = "postalCode")
    @Mapping(source = "country", target = "countryName")
    fun toDto(address: Address): AddressDto
}
```

### 3. KSP Generates Java Implementation

The processor generates `PersonMapperImpl.java`:

```java
public class PersonMapperImpl implements PersonMapper {
  @Override
  public PersonDto toDto(Person person) {
    PersonDto result = new PersonDto();
    result.setFirstName(person.getFirstName());
    result.setLastName(person.getLastName());
    result.setAge(person.getAge());
    return result;
  }
}
```

### 4. Use in Kotlin Code

```kotlin
val mapper = PersonMapperImpl()
val dto = mapper.toDto(Person("John", "Doe", 30))
```

## Key Technical Details

- **KSP** processes Kotlin source code and can generate both `.kt` and `.java` files
- **JavaPoet** library is used for Java code generation
- Kotlin `data class` generates `getXxx()` methods compatible with Java
- Target classes need `var` properties (which generate setters) for Java-style bean mapping

## Build & Run Tests

```bash
cd ksp-demo
JAVA_HOME=/path/to/jdk17 ./gradlew clean build
```

Generated sources are placed in:
```
sample/build/generated/ksp/main/java/
```

## Why Java Output?

MapStruct's core codebase is Java. Generating Java mappers:
- Maintains consistency with existing MapStruct architecture
- Allows reuse of existing Java code generation infrastructure
- Ensures compatibility with Java-only projects consuming the mappers
