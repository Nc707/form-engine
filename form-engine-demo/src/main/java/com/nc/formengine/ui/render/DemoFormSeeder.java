package com.nc.formengine.ui.render;

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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Puts one published form in the database at startup, so the renderer has something to render.
 *
 * <p>The demo runs on an in-memory database created from scratch on every boot, and the only view
 * that writes definitions leaves them as drafts. Without this the list of forms would open empty and
 * nothing here could be tried out.
 *
 * <p>The form is chosen to exercise the whole engine rather than to be realistic: every field type,
 * a restriction of each kind that has one, a conditional dependency, and a layout that is not one
 * field per row. Set {@code formengine.demo.seed=false} to skip it.
 */
@Component
@Slf4j
class DemoFormSeeder {

    private static final String CODE = "job_application";

    private final FormDefinitionService formService;
    private final FormLayoutService layoutService;
    private final FieldDependencyService dependencyService;
    private final boolean enabled;

    DemoFormSeeder(FormDefinitionService formService,
                   FormLayoutService layoutService,
                   FieldDependencyService dependencyService,
                   @Value("${formengine.demo.seed:true}") boolean enabled) {
        this.formService = formService;
        this.layoutService = layoutService;
        this.dependencyService = dependencyService;
        this.enabled = enabled;
    }

    @EventListener(ApplicationReadyEvent.class)
    void seed() {
        if (!enabled) {
            return;
        }
        // Only this seeder's own form is checked. Standing down whenever *any* published form exists
        // would mean standing down whenever another seeder got there first — and one does: the
        // responses demo data runs as a CommandLineRunner, which is before this event fires.
        if (formService.existsByCode(CODE)) {
            return;
        }

        FormDefinitionDTO created = formService.create(definition());
        Map<String, Long> fieldIds = fieldIdsByName(created);

        // Both have to exist before the form is published: publishing freezes the definition.
        layoutService.createLayout(layout(created.getId(), fieldIds));
        dependencyService.create(FieldDependencyDTO.builder()
                .triggerFieldId(fieldIds.get("needs_visa"))
                .dependentFieldId(fieldIds.get("visa_country"))
                .condition(DependencyCondition.EQUALS)
                // The value a checkbox answer takes, per FieldComponentFactory's boolean editor.
                .triggerValue("true")
                .effect(DependencyEffect.SHOW)
                .build());

        FormDefinitionDTO published = formService.publish(created.getId());
        log.info("Seeded demo form '{}' (id {}) as {}",
                published.getTitle(), published.getId(), published.getStatus());
    }

    private FormDefinitionDTO definition() {
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
                field("visa_country", "Passport country", FieldType.TEXT, 7, true,
                        restriction(RestrictionType.NOT_EMPTY, Map.of(), null))));

        return FormDefinitionDTO.builder()
                .code(CODE)
                .title("Job application")
                .description("A form that uses every field type, a rule of each kind, "
                        + "a conditional field and a two-column layout.")
                .version(1)
                .fields(fields)
                .build();
    }

    /**
     * A layout that is worth having: three rows of more than one field, so the twelve-column grid is
     * visibly doing something. {@code visa_country} shares a row with the checkbox that reveals it.
     */
    private FormLayoutDTO layout(Long formId, Map<String, Long> fieldIds) {
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

    private static Map<String, Long> fieldIdsByName(FormDefinitionDTO form) {
        return form.getFields().stream()
                .collect(Collectors.toMap(FieldDefinitionDTO::getName, FieldDefinitionDTO::getId));
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
