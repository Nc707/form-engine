package com.nc.formengine.flow;

import com.nc.formengine.business.service.FieldDependencyService;
import com.nc.formengine.business.service.FormDefinitionService;
import com.nc.formengine.business.service.FormLayoutService;
import com.nc.formengine.model.dto.FieldDefinitionDTO;
import com.nc.formengine.model.dto.FieldDependencyDTO;
import com.nc.formengine.model.dto.FieldLayoutDTO;
import com.nc.formengine.model.dto.FieldOptionDTO;
import com.nc.formengine.model.dto.FieldRestrictionDTO;
import com.nc.formengine.model.dto.FormDefinitionDTO;
import com.nc.formengine.model.dto.FormLayoutDTO;
import com.nc.formengine.model.enums.DependencyCondition;
import com.nc.formengine.model.enums.DependencyEffect;
import com.nc.formengine.model.enums.FieldType;
import com.nc.formengine.model.enums.FormDefinitionStatus;
import com.nc.formengine.model.enums.RestrictionType;
import com.nc.formengine.model.validation.AnswerCodec;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Publishes the form the renderer tests run against: every field type, a rule of each kind that
 * takes one, a conditional dependency, and a layout of more than one field per row.
 *
 * <p>{@code form-engine-demo} seeds a form of the same shape, and on purpose this does not reuse it.
 * The two have different jobs — that one is an example to look at and may be changed for how it
 * reads, this one is a fixture whose exact shape the assertions depend on — and these tests used to
 * read the demo's, which is what made them breakable for cosmetic reasons.
 */
final class PublishedFormFixture {

    static final String CODE = "job_application";

    private PublishedFormFixture() {
    }

    /** Idempotent: the form is published once and found again on later calls. */
    static FormDefinitionDTO publish(FormDefinitionService formService,
                                     FormLayoutService layoutService,
                                     FieldDependencyService dependencyService) {
        if (formService.existsByCode(CODE)) {
            return published(formService);
        }

        FormDefinitionDTO created = formService.create(definition());
        Map<String, Long> fieldIds = created.getFields().stream()
                .collect(Collectors.toMap(FieldDefinitionDTO::getName, FieldDefinitionDTO::getId));

        // Both have to exist before the form is published: publishing freezes the definition.
        layoutService.createLayout(layout(created.getId(), fieldIds));
        dependencyService.create(FieldDependencyDTO.builder()
                .triggerFieldId(fieldIds.get("needs_visa"))
                .dependentFieldId(fieldIds.get("visa_country"))
                .condition(DependencyCondition.EQUALS)
                .triggerValue(AnswerCodec.encodeBoolean(true))
                .effect(DependencyEffect.SHOW)
                .build());

        formService.publish(created.getId());
        return published(formService);
    }

    private static FormDefinitionDTO published(FormDefinitionService formService) {
        return formService.findByStatus(FormDefinitionStatus.PUBLISHED).stream()
                .filter(form -> CODE.equals(form.getCode()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("the fixture form was not published"));
    }

    private static FormDefinitionDTO definition() {
        List<FieldDefinitionDTO> fields = new ArrayList<>(List.of(
                field("full_name", "Full name", FieldType.TEXT, 0, true,
                        restriction(RestrictionType.MIN_LENGTH, Map.of("minLength", 3),
                                "Please write your full name."),
                        restriction(RestrictionType.MAX_LENGTH, Map.of("maxLength", 60), null)),
                field("email", "Email", FieldType.TEXT, 1, true,
                        restriction(RestrictionType.EMAIL, Map.of(), "That does not look like an email address.")),
                field("years_experience", "Years of experience", FieldType.NUMBER, 2, true,
                        restriction(RestrictionType.MIN_VALUE, Map.of("minValue", 0), null),
                        restriction(RestrictionType.MAX_VALUE, Map.of("maxValue", 60), null)),
                field("available_from", "Available from", FieldType.DATE, 3, false),
                withOptions(field("role", "Role", FieldType.SELECT, 4, true),
                        option("Backend", "backend", 0),
                        option("Frontend", "frontend", 1),
                        option("Full stack", "fullstack", 2)),
                withOptions(field("stack", "Stack", FieldType.MULTI_SELECT, 5, false),
                        option("Java", "java", 0),
                        option("Spring", "spring", 1),
                        option("Vaadin", "vaadin", 2),
                        option("SQL", "sql", 3)),
                field("needs_visa", "I need a work visa", FieldType.BOOLEAN, 6, false),
                field("visa_country", "Passport country", FieldType.TEXT, 7, true)));

        return FormDefinitionDTO.builder()
                .code(CODE)
                .title("Job application")
                .description("A form that uses every field type, a rule of each kind, "
                        + "a conditional field and a two-column layout.")
                .version(1)
                .fields(fields)
                .build();
    }

    private static FormLayoutDTO layout(Long formId, Map<String, Long> fieldIds) {
        return FormLayoutDTO.builder()
                .formDefinitionId(formId)
                // No device type: the generic layout every device falls back to.
                .fieldLayouts(new ArrayList<>(List.of(
                        placement(fieldIds.get("full_name"), 0, 0, 6),
                        placement(fieldIds.get("email"), 0, 6, 6),
                        placement(fieldIds.get("years_experience"), 1, 0, 4),
                        placement(fieldIds.get("available_from"), 1, 4, 4),
                        placement(fieldIds.get("role"), 1, 8, 4),
                        placement(fieldIds.get("stack"), 2, 0, 12),
                        placement(fieldIds.get("needs_visa"), 3, 0, 6),
                        placement(fieldIds.get("visa_country"), 3, 6, 6))))
                .build();
    }

    private static FieldLayoutDTO placement(Long fieldId, int row, int column, int colspan) {
        return FieldLayoutDTO.builder()
                .fieldDefinitionId(fieldId)
                .row(row)
                .column(column)
                .colspan(colspan)
                .rowspan(1)
                .build();
    }

    private static FieldDefinitionDTO field(String name, String label, FieldType type,
                                            int orderIndex, boolean required,
                                            FieldRestrictionDTO... restrictions) {
        return FieldDefinitionDTO.builder()
                .name(name)
                .label(label)
                .type(type)
                .orderIndex(orderIndex)
                .required(required)
                .restrictions(new ArrayList<>(List.of(restrictions)))
                .options(new ArrayList<>())
                .build();
    }

    private static FieldDefinitionDTO withOptions(FieldDefinitionDTO field, FieldOptionDTO... options) {
        field.setOptions(new ArrayList<>(List.of(options)));
        return field;
    }

    private static FieldOptionDTO option(String label, String value, int orderIndex) {
        return FieldOptionDTO.builder()
                .label(label)
                .value(value)
                .orderIndex(orderIndex)
                .build();
    }

    /** Parameter names are the ones {@code FieldSpecificationFactory} reads. */
    private static FieldRestrictionDTO restriction(RestrictionType type,
                                                   Map<String, Object> parameters,
                                                   String message) {
        return FieldRestrictionDTO.builder()
                .restrictionType(type)
                .parameters(parameters)
                .errorMessage(message)
                .build();
    }
}
