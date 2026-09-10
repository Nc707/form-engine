package com.nc.formengine.flow;

import com.nc.formengine.model.dto.FieldDefinitionDTO;
import com.nc.formengine.model.dto.FieldDependencyDTO;
import com.nc.formengine.model.dto.FieldOptionDTO;
import com.nc.formengine.model.dto.FormDefinitionDTO;
import com.nc.formengine.model.enums.DependencyCondition;
import com.nc.formengine.model.enums.DependencyEffect;
import com.nc.formengine.model.enums.FieldType;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.select.Select;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FormPreviewTest {

    @Test
    void fieldsAreStackedInTheirOrder() {
        var preview = new FormPreview();

        preview.refresh(form(field(3L, "third", 2), field(1L, "first", 0), field(2L, "second", 1)),
                List.of());

        assertThat(preview.editors()).extracting(FieldEditor::name)
                .containsExactly("first", "second", "third");
    }

    @Test
    void aFieldWithoutAnOrderGoesLast() {
        var preview = new FormPreview();

        preview.refresh(form(field(2L, "unordered", null), field(1L, "first", 0)), List.of());

        assertThat(preview.editors()).extracting(FieldEditor::name)
                .containsExactly("first", "unordered");
    }

    @Test
    void everyInputIsReadOnly() {
        var select = field(2L, "country", 1);
        select.setType(FieldType.SELECT);
        select.setOptions(new ArrayList<>(List.of(
                FieldOptionDTO.builder().label("Argentina").value("ar").orderIndex(0).build())));

        var preview = new FormPreview();
        preview.refresh(form(field(1L, "nickname", 0), select), List.of());

        assertThat(preview.editors()).hasSize(2);
        assertThat(preview.editors()).allSatisfy(editor ->
                assertThat(((HasValue<?, ?>) editor.component()).isReadOnly()).isTrue());
    }

    @Test
    void aSelectPreviewCarriesItsOptions() {
        var select = field(1L, "country", 0);
        select.setType(FieldType.SELECT);
        select.setOptions(new ArrayList<>(List.of(
                FieldOptionDTO.builder().label("Argentina").value("ar").orderIndex(0).build(),
                FieldOptionDTO.builder().label("Austria").value("at").orderIndex(1).build())));

        var preview = new FormPreview();
        preview.refresh(form(select), List.of());

        var component = (Select<?>) preview.editors().get(0).component();
        assertThat(component.getListDataView().getItems().map(String::valueOf))
                .containsExactly("ar", "at");
    }

    @Test
    void refreshingReplacesTheFieldsRatherThanAddingToThem() {
        var preview = new FormPreview();

        preview.refresh(form(field(1L, "nickname", 0)), List.of());
        preview.refresh(form(field(1L, "nickname", 0), field(2L, "surname", 1)), List.of());

        assertThat(preview.editors()).extracting(FieldEditor::name)
                .containsExactly("nickname", "surname");
    }

    @Test
    void aFieldWithoutATypeIsSkippedRatherThanBreakingThePage() {
        var broken = field(2L, "broken", 1);
        broken.setType(null);

        var preview = new FormPreview();
        preview.refresh(form(field(1L, "nickname", 0), broken), List.of());

        assertThat(preview.editors()).extracting(FieldEditor::name).containsExactly("nickname");
    }

    @Test
    void anEmptyFormSaysSoInsteadOfShowingNothing() {
        var preview = new FormPreview();

        preview.refresh(form(), List.of());

        assertThat(preview.editors()).isEmpty();
    }

    @Test
    void aConditionalFieldSaysWhenItApplies() {
        var trigger = field(1L, "visa", 0);
        trigger.setLabel("Needs visa");
        var dependent = field(2L, "passport", 1);
        dependent.setLabel("Passport country");

        var dependency = FieldDependencyDTO.builder()
                .triggerFieldId(1L).dependentFieldId(2L)
                .condition(DependencyCondition.EQUALS).effect(DependencyEffect.SHOW)
                .triggerValue("true")
                .build();

        var preview = new FormPreview();
        preview.refresh(form(trigger, dependent), List.of(dependency));

        assertThat(DependencyText.note(dependency, "Needs visa"))
                .isEqualTo("Shown when 'Needs visa' is 'true'");
        assertThat(preview.editors()).hasSize(2);
    }

    @Test
    void aDependencySentenceReadsAsOne() {
        var dependency = FieldDependencyDTO.builder()
                .triggerFieldId(1L).dependentFieldId(2L)
                .condition(DependencyCondition.GREATER_THAN).effect(DependencyEffect.REQUIRE)
                .triggerValue("18")
                .build();

        assertThat(DependencyText.sentence(dependency, "Age", "Guardian"))
                .isEqualTo("Require 'Guardian' when 'Age' is greater than '18'");
    }

    private static FormDefinitionDTO form(FieldDefinitionDTO... fields) {
        return FormDefinitionDTO.builder()
                .id(1L).code("preview").title("Preview").version(1)
                .fields(new ArrayList<>(List.of(fields)))
                .build();
    }

    private static FieldDefinitionDTO field(Long id, String name, Integer orderIndex) {
        return FieldDefinitionDTO.builder()
                .id(id).name(name).label(name).type(FieldType.TEXT)
                .orderIndex(orderIndex).required(false)
                .build();
    }
}
