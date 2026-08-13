package com.nc.formengine.ui.render;

import com.nc.formengine.model.dto.FieldDefinitionDTO;
import com.nc.formengine.model.dto.FieldLayoutDTO;
import com.nc.formengine.model.dto.FormLayoutDTO;
import com.nc.formengine.model.enums.FieldType;
import com.nc.formengine.ui.shared.FieldComponentFactory;
import com.nc.formengine.ui.shared.FieldEditor;
import com.vaadin.flow.component.Component;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LayoutGridRendererTest {

    @Test
    void withoutALayoutTheFieldsAreStacked() {
        var editors = editors(field(1L, "a", 0), field(2L, "b", 1));

        List<Component> cells = cellsOf(LayoutGridRenderer.render(editors, null));

        assertThat(cells).hasSize(2);
        assertThat(cells).allSatisfy(cell ->
                assertThat(cell.getStyle().get("grid-column")).isEqualTo("1 / span 12"));
        // No explicit row: a stacked form has to close up when a dependency hides one of its fields.
        assertThat(cells).allSatisfy(cell ->
                assertThat(cell.getStyle().get("grid-row")).isNull());
    }

    @Test
    void aLayoutPutsEachFieldWhereItSays() {
        var editors = editors(field(1L, "a", 0), field(2L, "b", 1));
        var layout = layout(
                placement(1L, 0, 0, 6, 1),
                placement(2L, 0, 6, 6, 1));

        List<Component> cells = cellsOf(LayoutGridRenderer.render(editors, layout));

        // Zero-based rows and columns become one-based CSS grid lines.
        assertThat(cells.get(0).getStyle().get("grid-column")).isEqualTo("1 / span 6");
        assertThat(cells.get(0).getStyle().get("grid-row")).isEqualTo("1 / span 1");
        assertThat(cells.get(1).getStyle().get("grid-column")).isEqualTo("7 / span 6");
        assertThat(cells.get(1).getStyle().get("grid-row")).isEqualTo("1 / span 1");
    }

    @Test
    void cellsAreOrderedByPositionRegardlessOfTheOrderTheLayoutWasLoadedIn() {
        var editors = editors(field(1L, "a", 0), field(2L, "b", 1));
        var layout = layout(
                placement(1L, 3, 0, 12, 1),
                placement(2L, 1, 0, 12, 1));

        List<Component> cells = cellsOf(LayoutGridRenderer.render(editors, layout));

        assertThat(cells.get(0).getStyle().get("grid-row")).isEqualTo("2 / span 1");
        assertThat(cells.get(1).getStyle().get("grid-row")).isEqualTo("4 / span 1");
    }

    /**
     * A layout places every field it names. Whether a field is shown at all is the dependency
     * engine's answer, and the validator reads the same answer — so the two cannot disagree.
     */
    @Test
    void everyPlacedFieldIsRendered() {
        var editors = editors(field(1L, "a", 0), field(2L, "b", 1));
        var layout = layout(placement(1L, 0, 0, 12, 1), placement(2L, 1, 0, 12, 1));

        assertThat(cellsOf(LayoutGridRenderer.render(editors, layout))).hasSize(2);
    }

    @Test
    void aFieldTheLayoutForgotIsRenderedAfterTheRowsItDoesDescribe() {
        var editors = editors(field(1L, "a", 0), field(2L, "b", 1));
        var layout = layout(placement(1L, 0, 0, 6, 2));

        List<Component> cells = cellsOf(LayoutGridRenderer.render(editors, layout));

        // Dropping it would leave a required field with no way to answer it.
        assertThat(cells).hasSize(2);
        assertThat(cells.get(1).getStyle().get("grid-column")).isEqualTo("1 / span 12");
        assertThat(cells.get(1).getStyle().get("grid-row")).isEqualTo("3 / span 1");
    }

    @Test
    void positionsOutsideTheTwelveColumnGridAreClamped() {
        var editors = editors(field(1L, "a", 0), field(2L, "b", 1));
        var layout = layout(
                placement(1L, 0, 20, 4, 1),
                placement(2L, 1, 9, 99, 1));

        List<Component> cells = cellsOf(LayoutGridRenderer.render(editors, layout));

        assertThat(cells.get(0).getStyle().get("grid-column")).isEqualTo("12 / span 1");
        assertThat(cells.get(1).getStyle().get("grid-column")).isEqualTo("10 / span 3");
    }

    @Test
    void aPlacementWithoutAnySizeFallsBackToAFullWidthSingleRow() {
        var editors = editors(field(1L, "a", 0));
        var bare = FieldLayoutDTO.builder().fieldDefinitionId(1L).build();

        List<Component> cells = cellsOf(LayoutGridRenderer.render(editors, layout(bare)));

        assertThat(cells.get(0).getStyle().get("grid-column")).isEqualTo("1 / span 12");
        assertThat(cells.get(0).getStyle().get("grid-row")).isEqualTo("1 / span 1");
    }

    private static List<Component> cellsOf(Component grid) {
        return grid.getChildren().toList();
    }

    private static List<FieldEditor> editors(FieldDefinitionDTO... fields) {
        List<FieldEditor> editors = new ArrayList<>();
        for (FieldDefinitionDTO field : fields) {
            editors.add(FieldComponentFactory.create(field));
        }
        return editors;
    }

    private static FieldDefinitionDTO field(Long id, String name, int orderIndex) {
        return FieldDefinitionDTO.builder()
                .id(id)
                .name(name)
                .label(name)
                .type(FieldType.TEXT)
                .orderIndex(orderIndex)
                .required(false)
                .build();
    }

    private static FieldLayoutDTO placement(Long fieldId, int row, int column, int colspan, int rowspan) {
        return FieldLayoutDTO.builder()
                .fieldDefinitionId(fieldId)
                .row(row)
                .column(column)
                .colspan(colspan)
                .rowspan(rowspan)
                .build();
    }

    private static FormLayoutDTO layout(FieldLayoutDTO... placements) {
        return FormLayoutDTO.builder()
                .formDefinitionId(1L)
                .fieldLayouts(new ArrayList<>(List.of(placements)))
                .build();
    }
}
