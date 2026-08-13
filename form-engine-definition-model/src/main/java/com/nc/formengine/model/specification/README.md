# Field validation, as specifications

A form's rules are data, not code. A field carries a list of `FieldRestrictionDTO` rows, and each row
is turned into a `FieldSpecification` at evaluation time. Adding a rule to a form is therefore an edit
to the form, not a change to this package.

## The pieces

**`FieldSpecification`** — a functional interface: does this value satisfy this rule? Specifications
compose with `and`, `or` and `not`.

**`FieldContext`** — what a rule needs to know beyond the value itself: the field's type and name, the
rest of the form's answers (for cross-field rules), and any extra metadata.

**`SpecificationResult`** — satisfied or not, with the reasons when not.

**`FieldRestrictionDTO`** — the persisted form of a rule: its type, its parameters, an optional custom
message, which field types it applies to, and its evaluation order.

**`FieldSpecificationFactory`** — the bridge from stored data to behaviour.

**`RestrictionTypeRegistry`** — which restriction types make sense for which field types.

## What is *not* a restriction

Two things about an answer are always true, so neither is configurable.

**Presence.** Whether a field has to be answered is the field's own `required` flag — which a
dependency can also switch on and off per submission — and never a rule. Saying it twice gave two
answers that disagreed: the flag counted whitespace and an empty selection as unanswered, a `NOT_NULL`
rule counted only a literal null, and a field carrying both was judged by whichever ran first. Worse,
a rule cannot know the form is only being saved as a draft, so draft mode had to skip it by name.
`Answers.isMissing` is now the one definition, and `FieldDefinition.requiredMessage` is where a form
author words the message.

**Being one of the field's own options.** A `SELECT` or `MULTI_SELECT` answer outside the choices the
field offers is never valid, so the engine checks it whether or not anyone asked.

Both report themselves through `ValidationErrorCause` — `REQUIRED` and `NOT_AN_OPTION` — rather than
pretending some restriction failed.

## Available restrictions

| Restriction | Applies to | Parameter key |
|---|---|---|
| `EMAIL` | `TEXT` | — |
| `MIN_LENGTH` | `TEXT` | `minLength` |
| `MAX_LENGTH` | `TEXT` | `maxLength` |
| `PATTERN` | `TEXT` | `pattern` |
| `MIN_VALUE` | `NUMBER` | `minValue` |
| `MAX_VALUE` | `NUMBER` | `maxValue` |

Only `TEXT` and `NUMBER` have any. `DATE`, `BOOLEAN`, `SELECT` and `MULTI_SELECT` have none: a rule
judges the shape of an answer, and for those types the shape is already settled by the field type, the
`required` flag and the option list. The builder says so out loud instead of offering an empty dropdown.

**The parameter key matters and is not checked for you.** The factory is permissive by design: an
unknown restriction type, or a missing or unreadable parameter, yields a specification that accepts
every value. A rule nobody can satisfy would leave a form unsubmittable with no way for the user to
resolve it, so failing open is the right trade — but it does mean a misspelled key produces a rule
that silently never fires rather than an error. `RestrictionParameterSpec` in the demo's builder is
where the UI keeps these names in one place, and `RestrictionParameterSpecTest` pins the failure mode.

## Using it

Declaring a rule:

```java
FieldRestrictionDTO restriction = FieldRestrictionDTO.builder()
    .restrictionType(RestrictionType.MIN_LENGTH)
    .parameters(Map.of("minLength", 5))
    .errorMessage("Must be at least 5 characters")
    .build();
```

A field with several:

```java
FieldDefinitionDTO field = FieldDefinitionDTO.builder()
    .name("username")
    .label("Username")
    .type(FieldType.TEXT)
    .required(true)
    .requiredMessage("We need a username")
    .restrictions(List.of(
        FieldRestrictionDTO.builder()
            .restrictionType(RestrictionType.MIN_LENGTH)
            .parameters(Map.of("minLength", 3))
            .build(),
        FieldRestrictionDTO.builder()
            .restrictionType(RestrictionType.MAX_LENGTH)
            .parameters(Map.of("maxLength", 20))
            .build()))
    .build();
```

Building specifications from what was stored:

```java
// One restriction
FieldSpecification spec = FieldSpecificationFactory.from(restriction);

// All of a field's, combined with AND in orderIndex order
FieldSpecification all = FieldSpecificationFactory.composite(field.getRestrictions());
```

Because `and` short-circuits on the first failure, `composite` answers whether a value is acceptable,
not everything that is wrong with it. To report one error per broken rule — which is what
`FormValidationService` does — evaluate each restriction separately with `from`.

Evaluating by hand:

```java
FieldSpecification spec = new MinLengthSpecification(5);
FieldContext context = FieldContext.builder()
    .fieldType(FieldType.TEXT)
    .fieldName("username")
    .build();

SpecificationResult result = spec.isSatisfiedBy("abc", context);
if (!result.isSatisfied()) {
    System.out.println(result.getReasons());
}
```

Asking what applies to a field type:

```java
Set<RestrictionType> forText = RestrictionTypeRegistry.getApplicableRestrictionTypes(FieldType.TEXT);
boolean applicable = RestrictionTypeRegistry.isApplicable(RestrictionType.MIN_LENGTH, FieldType.TEXT);
```

## Adding a restriction

1. Write a class implementing `FieldSpecification`.
2. Declare the field types it applies to as a constant.
3. Implement `isSatisfiedBy`, returning satisfied when the field type does not apply.
4. Add the constant to `RestrictionType`.
5. Register its applicability in `RestrictionTypeRegistry`.
6. **Add the case to the `switch` in `FieldSpecificationFactory.from`.** Without this the restriction
   is stored but never enforced. The `switch` has no `default` branch on purpose, so adding the enum
   constant makes the compiler point at this step.

## Where it lives

```
form-engine-definition-model/
├── specification/
│   ├── FieldSpecification.java        the interface
│   ├── FieldContext.java              what a rule may read
│   ├── SpecificationResult.java       satisfied, plus reasons
│   ├── RestrictionTypeRegistry.java   applicability
│   ├── FieldSpecificationFactory.java stored restriction -> specification
│   └── impl/                          one class per RestrictionType
├── validation/
│   ├── Answers.java                   the one definition of "answered"
│   ├── ValidationMode.java            DRAFT tolerates gaps, SUBMIT does not
│   ├── ValidationReport.java          the outcome of validating a form
│   ├── ValidationErrorCause.java      required, not an option, or a restriction
│   └── FieldValidationError.java      one thing wrong with one answer
└── dto/
    ├── FieldDefinitionDTO.java
    └── FieldRestrictionDTO.java
```

Restrictions are persisted as the `FieldRestriction` entity in `form-engine-definition-dataimpl`, and
`FormValidationService` in `form-engine-definition-business` is what applies them to a form's answers.
