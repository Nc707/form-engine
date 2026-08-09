# Field Validation using Specification Pattern

Este módulo implementa el patrón Specification para validación de campos de formularios de manera flexible, reusable y desacoplada.

## 🎯 Conceptos Clave

### FieldSpecification
Interface funcional que define el contrato de validación. Permite composición usando operadores lógicos (AND, OR, NOT).

### FieldContext
Contexto de validación que proporciona información adicional como:
- Tipo de campo
- Nombre del campo
- Datos del formulario (para validaciones entre campos)
- Metadata adicional

### SpecificationResult
Resultado de la validación con:
- Estado (satisfecho/no satisfecho)
- Lista de razones de error

### FieldRestrictionDTO
DTO que almacena la configuración de una restricción:
- Tipo de restricción
- Parámetros de configuración
- Mensaje de error personalizado
- Tipos de campo aplicables
- Orden de evaluación

## 📋 Restricciones Disponibles

### Restricciones Universales
- **NOT_NULL**: Valida que el valor no sea nulo

### Restricciones para TEXT
- **NOT_EMPTY**: Valida que el string no esté vacío
- **MIN_LENGTH**: Valida longitud mínima
- **MAX_LENGTH**: Valida longitud máxima
- **PATTERN**: Valida contra expresión regular
- **EMAIL**: Valida formato de email

### Restricciones para NUMBER
- **MIN_VALUE**: Valida valor mínimo
- **MAX_VALUE**: Valida valor máximo

## 💡 Ejemplos de Uso

### Crear una restricción simple

```java
FieldRestrictionDTO restriction = FieldRestrictionDTO.builder()
    .restrictionType(RestrictionType.MIN_LENGTH)
    .parameters(Map.of("minLength", 5))
    .errorMessage("El campo debe tener al menos 5 caracteres")
    .build();
```

### Crear un campo con restricciones

```java
FieldDefinitionDTO field = FieldDefinitionDTO.builder()
    .name("username")
    .label("Nombre de Usuario")
    .type(FieldType.TEXT)
    .required(true)
    .restrictions(List.of(
        FieldRestrictionDTO.builder()
            .restrictionType(RestrictionType.NOT_EMPTY)
            .build(),
        FieldRestrictionDTO.builder()
            .restrictionType(RestrictionType.MIN_LENGTH)
            .parameters(Map.of("minLength", 3))
            .build(),
        FieldRestrictionDTO.builder()
            .restrictionType(RestrictionType.MAX_LENGTH)
            .parameters(Map.of("maxLength", 20))
            .build()
    ))
    .build();
```

### Validar un valor programáticamente

```java
// Crear la specification
MinLengthSpecification spec = new MinLengthSpecification(5);

// Crear el contexto
FieldContext context = FieldContext.builder()
    .fieldType(FieldType.TEXT)
    .fieldName("username")
    .build();

// Validar
SpecificationResult result = spec.isSatisfiedBy("abc", context);
if (!result.isSatisfied()) {
    System.out.println("Errores: " + result.getReasons());
}
```

### Combinar specifications con operadores lógicos

```java
FieldSpecification combined = new MinLengthSpecification(5)
    .and(new MaxLengthSpecification(20))
    .and(new PatternSpecification("^[a-zA-Z0-9_]+$"));

SpecificationResult result = combined.isSatisfiedBy("user123", context);
```

### Construir specifications desde restricciones persistidas

`FieldSpecificationFactory` es el puente entre los datos y el comportamiento: toma los
`FieldRestrictionDTO` guardados con el campo y devuelve la specification que los hace cumplir.

```java
// Una sola restricción
FieldSpecification spec = FieldSpecificationFactory.from(restriction);

// Todas las de un campo, combinadas con AND en orden de orderIndex
FieldSpecification all = FieldSpecificationFactory.composite(field.getRestrictions());
```

La factory es permisiva ante restricciones mal formadas: un tipo desconocido, o un parámetro faltante
o ilegible, produce una specification que acepta todo. Una regla imposible de cumplir dejaría el
formulario sin poder enviarse y sin forma de que el usuario lo resuelva.

Como `and` corta en la primera falla, `composite` responde si el valor es aceptable, no todo lo que
está mal. Para un error por regla rota —lo que hace `FormValidationService`— se evalúa cada
restricción por separado con `from`.

### Filtrar restricciones por tipo de campo

```java
// Obtener todas las restricciones aplicables a TEXT
Set<RestrictionType> textRestrictions = 
    RestrictionTypeRegistry.getApplicableRestrictionTypes(FieldType.TEXT);

// Verificar si una restricción es aplicable
boolean applicable = RestrictionTypeRegistry.isApplicable(
    RestrictionType.MIN_LENGTH, 
    FieldType.TEXT
); // true
```

## 🔧 Extensibilidad

Para crear una nueva restriction:

1. Crear una clase que implemente `FieldSpecification`
2. Definir los tipos de campo aplicables como constante
3. Implementar el método `isSatisfiedBy`
4. Agregar el tipo al enum `RestrictionType`
5. Registrar en `RestrictionTypeRegistry`
6. Agregar el caso al `switch` de `FieldSpecificationFactory.from`, que es donde el tipo persistido
   se convierte en la specification. Sin este paso la restricción se guarda pero nunca se aplica; el
   `switch` no tiene rama `default`, así que el compilador avisa al agregar el valor al enum.

Ejemplo:

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomSpecification implements FieldSpecification {
    
    private String customParam;
    private String errorMessage;
    
    private static final Set<FieldType> APPLICABLE_TYPES = Set.of(FieldType.TEXT);
    
    @Override
    public SpecificationResult isSatisfiedBy(Object value, FieldContext context) {
        // Check applicability
        if (context.getFieldType() != null && !APPLICABLE_TYPES.contains(context.getFieldType())) {
            return SpecificationResult.satisfied();
        }
        
        // Your validation logic here
        boolean valid = /* your logic */;
        
        return valid 
            ? SpecificationResult.satisfied()
            : SpecificationResult.notSatisfied(errorMessage);
    }
    
    public static Set<FieldType> getApplicableTypes() {
        return APPLICABLE_TYPES;
    }
}
```

## 🏗️ Arquitectura

```
form-engine-definition-model/
├── specification/
│   ├── FieldSpecification.java      (Interface principal)
│   ├── FieldContext.java            (Contexto de validación)
│   ├── SpecificationResult.java     (Resultado)
│   ├── RestrictionTypeRegistry.java (Registro de aplicabilidad)
│   ├── FieldSpecificationFactory.java (DTO persistido -> specification)
│   └── impl/
│       ├── NotNullSpecification.java
│       ├── NotEmptySpecification.java
│       ├── MinLengthSpecification.java
│       ├── MaxLengthSpecification.java
│       ├── MinValueSpecification.java
│       ├── MaxValueSpecification.java
│       ├── PatternSpecification.java
│       └── EmailSpecification.java
├── dto/
│   ├── FieldDefinitionDTO.java      (Usa List<FieldRestrictionDTO>)
│   └── FieldRestrictionDTO.java     (Configuración de restricción)
├── validation/
│   ├── ValidationMode.java          (DRAFT / SUBMIT)
│   ├── ValidationReport.java        (Resultado de validar un formulario)
│   └── FieldValidationError.java    (Una regla rota de un campo)
└── enums/
    ├── FieldType.java
    └── RestrictionType.java
```

Las restricciones se persisten en la entidad `FieldRestriction`
(`form-engine-definition-dataimpl`), y `FormValidationService`
(`form-engine-definition-business`) es quien las aplica a las respuestas de un formulario.

## ✅ Ventajas de este Diseño

1. **Desacoplamiento**: Las restricciones están separadas de FieldDefinitionDTO
2. **Reusabilidad**: Las specifications se pueden reutilizar en diferentes contextos
3. **Composición**: Se pueden combinar restrictions con AND/OR/NOT
4. **Type-Safety**: Cada specification sabe a qué tipos de campo aplica
5. **Extensibilidad**: Fácil agregar nuevas restrictions sin modificar código existente
6. **Testabilidad**: Cada specification es independiente y fácil de testear
7. **Validación centralizada**: Toda la lógica de validación en un solo lugar
8. **Metadata-driven**: Las restrictions se configuran via DTOs, no código
