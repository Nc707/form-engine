package com.nc.formengine.flow.builder;

import com.nc.formengine.business.service.FieldDependencyService;
import com.nc.formengine.business.service.FieldDefinitionService;
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
import com.nc.formengine.model.exception.FormDefinitionNotEditableException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The persistence contract {@link FormDraftSession} is built on.
 *
 * <p>Every one of these is a rule the builder would otherwise have to take on faith, and most of
 * them fail silently rather than loudly when broken. The important one is {@link
 * #updatingAFormDoesNotSaveItsFields()}: it is the reason the editor saves a field at a time instead
 * of assembling one big DTO, and without it the next person to look at this will "simplify" the
 * session back into a single save and quietly lose data.
 */
// A datasource of its own, deliberately. application.properties pins jdbc:h2:mem:formengine with
// ddl-auto=create-drop, so a second context in the same JVM would re-create that schema underneath
// RendererWorkflowIntegrationTest and wipe the form its assertions rely on.
@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:builder;DB_CLOSE_DELAY=-1")
class BuilderPersistenceIntegrationTest {

    /** A code belongs to a whole version history, so no two tests may share one. */
    private static final AtomicInteger CODES = new AtomicInteger();

    @Autowired
    private FormDefinitionService formService;

    @Autowired
    private FieldDefinitionService fieldService;

    @Autowired
    private FieldDependencyService dependencyService;

    @Autowired
    private FormLayoutService layoutService;

    // --- what a create does ------------------------------------------------------------------

    @Test
    void creatingAFormPersistsItsFieldsRulesAndOptions() {
        var field = text("nickname");
        field.setRestrictions(new ArrayList<>(List.of(minLength(3))));

        var select = FieldDefinitionDTO.builder()
                .name("country").label("Country").type(FieldType.SELECT).orderIndex(1).required(false)
                .options(new ArrayList<>(List.of(
                        option("Argentina", "ar", 0),
                        option("Austria", "at", 1))))
                .build();

        var stored = reload(formService.create(draft(field, select)).getId());

        assertThat(stored.getFields()).hasSize(2);
        assertThat(fieldNamed(stored, "nickname").getRestrictions())
                .extracting(FieldRestrictionDTO::getRestrictionType)
                .containsExactly(RestrictionType.MIN_LENGTH);
        assertThat(fieldNamed(stored, "country").getOptions())
                .extracting(FieldOptionDTO::getValue)
                .containsExactly("ar", "at");
    }

    // --- what an update does NOT do ----------------------------------------------------------

    /**
     * The finding the whole editing model turns on. {@code FormDefinitionMapper.updateEntity} copies
     * the form's own columns and never looks at {@code fields}, so this loses the new field without
     * throwing anything and hands back a DTO that reads as though it worked.
     */
    @Test
    void updatingAFormDoesNotSaveItsFields() {
        Long formId = formService.create(draft(text("nickname"))).getId();

        var edited = reload(formId);
        edited.setTitle("Renamed");
        edited.getFields().add(text("second"));
        formService.update(formId, edited);

        var stored = reload(formId);
        assertThat(stored.getTitle()).isEqualTo("Renamed");
        assertThat(stored.getFields()).extracting(FieldDefinitionDTO::getName)
                .containsExactly("nickname");
    }

    // --- what a field update does ------------------------------------------------------------

    @Test
    void anEmptyListDeletesTheRulesAndNullLeavesThemAlone() {
        var field = text("nickname");
        field.setRestrictions(new ArrayList<>(List.of(minLength(3))));
        Long formId = formService.create(draft(field)).getId();
        Long fieldId = reload(formId).getFields().get(0).getId();

        // Null: this request says nothing about them.
        var untouched = OrderIndexes.reorderPatch(reload(formId).getFields().get(0), 0);
        fieldService.update(fieldId, untouched);
        assertThat(reload(formId).getFields().get(0).getRestrictions()).hasSize(1);

        // Empty: there are none.
        var cleared = reload(formId).getFields().get(0);
        cleared.setRestrictions(new ArrayList<>());
        fieldService.update(fieldId, cleared);
        assertThat(reload(formId).getFields().get(0).getRestrictions()).isEmpty();
    }

    @Test
    void aChangedRuleListIsReconciledByIdRatherThanReplaced() {
        var field = text("nickname");
        field.setRestrictions(new ArrayList<>(List.of(minLength(3))));
        Long formId = formService.create(draft(field)).getId();

        var stored = reload(formId).getFields().get(0);
        Long keptRuleId = stored.getRestrictions().get(0).getId();
        stored.getRestrictions().get(0).setErrorMessage("Too short");
        stored.getRestrictions().add(maxLength(40));
        fieldService.update(stored.getId(), stored);

        var rules = reload(formId).getFields().get(0).getRestrictions();
        assertThat(rules).hasSize(2);
        assertThat(rules).filteredOn(r -> r.getRestrictionType() == RestrictionType.MIN_LENGTH)
                .singleElement()
                .satisfies(rule -> {
                    assertThat(rule.getId()).isEqualTo(keptRuleId);
                    assertThat(rule.getErrorMessage()).isEqualTo("Too short");
                });
    }

    @Test
    void aReorderPatchMovesTheFieldAndKeepsItsRulesAndOptions() {
        var first = text("nickname");
        first.setRestrictions(new ArrayList<>(List.of(minLength(3))));
        var second = text("surname");
        second.setOrderIndex(1);
        Long formId = formService.create(draft(first, second)).getId();

        var stored = reload(formId);
        var moved = fieldNamed(stored, "nickname");
        fieldService.update(moved.getId(), OrderIndexes.reorderPatch(moved, 5));

        var after = fieldNamed(reload(formId), "nickname");
        assertThat(after.getOrderIndex()).isEqualTo(5);
        assertThat(after.getRestrictions()).hasSize(1);
        assertThat(after.getLabel()).isEqualTo(moved.getLabel());
        assertThat(after.getType()).isEqualTo(FieldType.TEXT);
    }

    // --- the session -------------------------------------------------------------------------

    @Test
    void aFieldPlacedInALayoutCannotBeDeletedUntilItsPlacementGoes() {
        Long formId = formService.create(draft(text("nickname"))).getId();
        Long fieldId = reload(formId).getFields().get(0).getId();
        layoutService.createLayout(FormLayoutDTO.builder()
                .formDefinitionId(formId)
                .fieldLayouts(new ArrayList<>(List.of(FieldLayoutDTO.builder()
                        .fieldDefinitionId(fieldId).row(0).column(0).colspan(12).rowspan(1)
                        .build())))
                .build());

        // The raw delete cannot work: field_layouts.field_definition_id is NOT NULL and nothing
        // cascades. Which wrapper the constraint surfaces as is not the point; that it refuses is.
        assertThatThrownBy(() -> fieldService.deleteById(fieldId)).isInstanceOf(Exception.class);

        // The session takes the placement out first, so the same delete goes through.
        session(formId).deleteField(fieldId);

        assertThat(reload(formId).getFields()).isEmpty();
        assertThat(layoutService.getLayoutsByFormDefinition(formId))
                .allSatisfy(layout -> assertThat(layout.getFieldLayouts()).isEmpty());
    }

    @Test
    void deletingAFieldClosesTheGapInTheOrdering() {
        Long formId = formService.create(draft(
                text("a"), ordered("b", 1), ordered("c", 2))).getId();
        var session = session(formId);

        session.deleteField(fieldNamed(reload(formId), "b").getId());

        assertThat(session.fields()).extracting(FieldDefinitionDTO::getName).containsExactly("a", "c");
        assertThat(session.fields()).extracting(FieldDefinitionDTO::getOrderIndex).containsExactly(0, 1);
    }

    @Test
    void movingAFieldSwapsItWithItsNeighbour() {
        Long formId = formService.create(draft(text("a"), ordered("b", 1))).getId();
        var session = session(formId);

        session.moveField(session.fields().get(1).getId(), -1);

        assertThat(session.fields()).extracting(FieldDefinitionDTO::getName).containsExactly("b", "a");
        assertThat(session.fields()).extracting(FieldDefinitionDTO::getOrderIndex).containsExactly(0, 1);
    }

    @Test
    void aPublishedFormRefusesEveryWriteTheSessionOffers() {
        Long formId = formService.create(draft(text("nickname"))).getId();
        formService.publish(formId);
        var session = session(formId);
        Long fieldId = session.fields().get(0).getId();

        assertThat(session.actions().editable()).isFalse();
        assertThatThrownBy(() -> session.saveDetails("x", "y")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> session.addField(text("second"))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> session.deleteField(fieldId)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> session.moveField(fieldId, 1)).isInstanceOf(IllegalStateException.class);

        // And the engine's own guard behind it, for the one call that has one.
        var stored = reload(formId);
        assertThatThrownBy(() -> formService.update(formId, stored))
                .isInstanceOf(FormDefinitionNotEditableException.class);
    }

    /**
     * The check that validates the dependency-listing strategy: gathering per trigger field has to
     * find everything a version copy produced, or the editor would show a form missing rules it has.
     */
    @Test
    void aNewVersionIsAnEditableDraftWhoseDependenciesFollowItsOwnFields() {
        Long formId = formService.create(draft(text("visa"), ordered("passport", 1))).getId();
        var fields = reload(formId).getFields();
        Long triggerId = fieldNamed(reload(formId), "visa").getId();
        Long dependentId = fieldNamed(reload(formId), "passport").getId();
        dependencyService.create(FieldDependencyDTO.builder()
                .triggerFieldId(triggerId).dependentFieldId(dependentId)
                .condition(DependencyCondition.EQUALS).effect(DependencyEffect.SHOW)
                .triggerValue("yes")
                .build());
        formService.publish(formId);

        Long newId = session(formId).createNewVersion();
        var branched = session(newId);

        assertThat(branched.form().getStatus()).isEqualTo(FormDefinitionStatus.DRAFT);
        assertThat(branched.form().getVersion()).isEqualTo(2);
        assertThat(branched.actions().editable()).isTrue();
        assertThat(branched.fields()).extracting(FieldDefinitionDTO::getId)
                .doesNotContainAnyElementsOf(fields.stream().map(FieldDefinitionDTO::getId).toList());
        assertThat(branched.dependencies()).singleElement().satisfies(dependency -> {
            assertThat(dependency.getTriggerFieldId())
                    .isEqualTo(fieldNamed(reload(newId), "visa").getId());
            assertThat(dependency.getDependentFieldId())
                    .isEqualTo(fieldNamed(reload(newId), "passport").getId());
        });
    }

    @Test
    void theSessionRoundTripsAFieldWithItsRulesAndOptions() {
        Long formId = formService.create(draft(text("nickname"))).getId();
        var session = session(formId);

        // A select's answer is judged by its options and the Required flag, so it carries no rules.
        var select = FieldDefinitionDTO.builder()
                .name("country").label("Country").type(FieldType.SELECT)
                .required(true).requiredMessage("Pick a country")
                .options(new ArrayList<>(List.of(option("Argentina", "ar", 0))))
                .build();
        session.addField(select);

        var text = FieldDefinitionDTO.builder()
                .name("bio").label("Bio").type(FieldType.TEXT).required(false)
                .restrictions(new ArrayList<>(List.of(FieldRestrictionDTO.builder()
                        .restrictionType(RestrictionType.MIN_LENGTH)
                        .parameters(RestrictionParameterSpec.parameters(RestrictionType.MIN_LENGTH, 10))
                        .errorMessage("Tell us more")
                        .orderIndex(0)
                        .build())))
                .build();
        session.addField(text);

        var storedSelect = session.fields().get(1);
        assertThat(storedSelect.getName()).isEqualTo("country");
        assertThat(storedSelect.getOrderIndex()).isEqualTo(1);
        assertThat(storedSelect.getOptions()).extracting(FieldOptionDTO::getValue).containsExactly("ar");
        assertThat(storedSelect.getRequiredMessage()).isEqualTo("Pick a country");

        var storedText = session.fields().get(2);
        assertThat(storedText.getRestrictions()).singleElement().satisfies(rule -> {
            assertThat(rule.getRestrictionType()).isEqualTo(RestrictionType.MIN_LENGTH);
            assertThat(rule.getParameters()).containsEntry("minLength", 10);
            assertThat(rule.getErrorMessage()).isEqualTo("Tell us more");
        });
    }

    // --- fixtures ----------------------------------------------------------------------------

    private FormDraftSession session(Long formId) {
        return new FormDraftSession(formService, fieldService, dependencyService, layoutService, formId);
    }

    private FormDefinitionDTO reload(Long formId) {
        return formService.findById(formId).orElseThrow();
    }

    private static FormDefinitionDTO draft(FieldDefinitionDTO... fields) {
        return FormDefinitionDTO.builder()
                .code("builder_test_" + CODES.incrementAndGet())
                .title("Builder test")
                .version(1)
                .fields(new ArrayList<>(List.of(fields)))
                .build();
    }

    private static FieldDefinitionDTO fieldNamed(FormDefinitionDTO form, String name) {
        return form.getFields().stream()
                .filter(field -> name.equals(field.getName()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no field called " + name));
    }

    private static FieldDefinitionDTO text(String name) {
        return ordered(name, 0);
    }

    private static FieldDefinitionDTO ordered(String name, int orderIndex) {
        return FieldDefinitionDTO.builder()
                .name(name).label(name + " label").type(FieldType.TEXT)
                .orderIndex(orderIndex).required(false)
                .build();
    }

    private static FieldOptionDTO option(String label, String value, int orderIndex) {
        return FieldOptionDTO.builder().label(label).value(value).orderIndex(orderIndex).build();
    }

    private static FieldRestrictionDTO minLength(int length) {
        return FieldRestrictionDTO.builder()
                .restrictionType(RestrictionType.MIN_LENGTH)
                .parameters(RestrictionParameterSpec.parameters(RestrictionType.MIN_LENGTH, length))
                .orderIndex(0)
                .build();
    }

    private static FieldRestrictionDTO maxLength(int length) {
        return FieldRestrictionDTO.builder()
                .restrictionType(RestrictionType.MAX_LENGTH)
                .parameters(RestrictionParameterSpec.parameters(RestrictionType.MAX_LENGTH, length))
                .orderIndex(1)
                .build();
    }
}
