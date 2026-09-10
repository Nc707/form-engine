package com.nc.formengine.flow.render;

import com.nc.formengine.model.dto.FieldLayoutDTO;
import com.nc.formengine.model.dto.FormLayoutDTO;
import com.nc.formengine.flow.shared.FieldEditor;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Places a form's editors on screen: on the twelve-column grid its layout describes, or stacked when
 * it has none.
 *
 * <p>A layout only decides <em>where</em> a field goes. <em>What</em> it looks like is decided by
 * {@link com.nc.formengine.flow.shared.FieldComponentFactory} from the field's
 * {@link com.nc.formengine.model.enums.FieldType}, so a form is the same widget-for-widget however it
 * happens to be laid out.
 */
final class LayoutGridRenderer {

    /** The grid every layout is expressed in. Columns and spans are read against this width. */
    private static final int COLUMNS = 12;

    private LayoutGridRenderer() {
    }

    /**
     * Builds the component holding every editor.
     *
     * @param editors the editors to place, in field {@code orderIndex} order
     * @param layout  the layout to honour, or null to stack the editors in the order given
     * @return the component to add to the view
     */
    static Component render(List<FieldEditor> editors, FormLayoutDTO layout) {
        Map<Long, FieldLayoutDTO> placements = placementsById(layout);
        var grid = grid();

        if (placements.isEmpty()) {
            editors.forEach(editor -> grid.add(fullWidthCell(editor, null)));
            return grid;
        }

        // Split first so the outcome cannot depend on the order the layout was loaded in: the placed
        // fields go where they say, sorted, and whatever the layout forgot follows them.
        List<FieldEditor> placed = new ArrayList<>();
        List<FieldEditor> unplaced = new ArrayList<>();
        for (FieldEditor editor : editors) {
            FieldLayoutDTO placement = placements.get(editor.field().getId());
            if (placement == null) {
                unplaced.add(editor);
            } else {
                placed.add(editor);
            }
        }

        placed.sort(Comparator
                .comparingInt((FieldEditor editor) -> row(placements.get(editor.field().getId())))
                .thenComparingInt(editor -> column(placements.get(editor.field().getId())))
                .thenComparingInt(LayoutGridRenderer::orderIndex));

        int rowsUsed = 0;
        for (FieldEditor editor : placed) {
            FieldLayoutDTO placement = placements.get(editor.field().getId());
            grid.add(cell(editor,
                    column(placement), colspan(placement), row(placement), rowspan(placement)));
            rowsUsed = Math.max(rowsUsed, row(placement) + rowspan(placement));
        }

        // A field the layout says nothing about still has to be answerable: dropping it would leave a
        // required field with no way to fill it, and the form permanently unsubmittable. It goes
        // after the rows the layout does describe, on an explicit row so that grid auto-placement
        // cannot slot it into a hole the layout left on purpose.
        for (FieldEditor editor : unplaced) {
            grid.add(fullWidthCell(editor, rowsUsed));
            rowsUsed++;
        }

        return grid;
    }

    private static Div grid() {
        var grid = new Div();
        grid.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(" + COLUMNS + ", 1fr)")
                .set("gap", "var(--lumo-space-m)")
                .setWidth("100%");
        return grid;
    }

    private static Div fullWidthCell(FieldEditor editor, Integer row) {
        return cell(editor, 0, COLUMNS, row, 1);
    }

    /**
     * Wraps one editor in its grid cell.
     *
     * <p>The position lives on the wrapper rather than on the editor's own component, which belongs
     * to the shared factory and should not have styles pushed onto it from here.
     *
     * @param row the zero-based row, or null to let the grid place the cell after the previous one,
     *            which is what makes a stacked form close up when a dependency hides a field
     */
    private static Div cell(FieldEditor editor, int column, int colspan, Integer row, int rowspan) {
        var cell = new Div(editor.component());
        // CSS grid lines are one-based; the layout's rows and columns are zero-based.
        cell.getStyle().set("grid-column", (column + 1) + " / span " + colspan);
        if (row != null) {
            cell.getStyle().set("grid-row", (row + 1) + " / span " + rowspan);
        }
        return cell;
    }

    private static Map<Long, FieldLayoutDTO> placementsById(FormLayoutDTO layout) {
        Map<Long, FieldLayoutDTO> placements = new HashMap<>();
        if (layout == null || layout.getFieldLayouts() == null) {
            return placements;
        }
        for (FieldLayoutDTO placement : layout.getFieldLayouts()) {
            if (placement != null && placement.getFieldDefinitionId() != null) {
                placements.put(placement.getFieldDefinitionId(), placement);
            }
        }
        return placements;
    }

    // Every read below clamps rather than trusts: a layout is data, and a field in a slightly wrong
    // place is a better answer to a bad row than a form nobody can fill in.

    private static int row(FieldLayoutDTO placement) {
        return Math.max(0, placement.getRow() == null ? 0 : placement.getRow());
    }

    private static int rowspan(FieldLayoutDTO placement) {
        return Math.max(1, placement.getRowspan() == null ? 1 : placement.getRowspan());
    }

    private static int column(FieldLayoutDTO placement) {
        int column = placement.getColumn() == null ? 0 : placement.getColumn();
        return Math.clamp(column, 0, COLUMNS - 1);
    }

    private static int colspan(FieldLayoutDTO placement) {
        int colspan = placement.getColspan() == null ? COLUMNS : placement.getColspan();
        return Math.clamp(colspan, 1, COLUMNS - column(placement));
    }

    private static int orderIndex(FieldEditor editor) {
        Integer orderIndex = editor.field().getOrderIndex();
        return orderIndex == null ? Integer.MAX_VALUE : orderIndex;
    }
}
