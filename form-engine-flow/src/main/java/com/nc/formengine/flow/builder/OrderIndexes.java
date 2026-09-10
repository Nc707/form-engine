package com.nc.formengine.flow.builder;

import com.nc.formengine.model.dto.FieldDefinitionDTO;

import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Keeps {@code orderIndex} honest across adds, removals and moves.
 *
 * <p>Order is not decoration. Fields render top to bottom by {@code orderIndex}, and restrictions
 * are combined in {@code orderIndex} order by {@code FieldSpecificationFactory.composite}, which
 * stops at the first failure — so the order of a field's rules is the order its error messages come
 * out in.
 */
final class OrderIndexes {

    private OrderIndexes() {
    }

    /**
     * The order fields are shown and saved in.
     *
     * <p>{@code orderIndex} is nullable in the schema, and a field written by the REST API or by a
     * version copy may arrive without one. Sorting nulls last and breaking ties on the id keeps the
     * list stable instead of letting it shuffle between reloads.
     */
    static Comparator<FieldDefinitionDTO> byOrder() {
        return Comparator
                .comparing(FieldDefinitionDTO::getOrderIndex,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(FieldDefinitionDTO::getId,
                        Comparator.nullsLast(Comparator.naturalOrder()));
    }

    /**
     * Numbers a list 0..n-1 through the setter that writes each element's index.
     *
     * @param items  the list in its intended order
     * @param setter writes the index onto one element
     */
    static <T> void reindex(List<T> items, BiConsumer<T, Integer> setter) {
        for (int index = 0; index < items.size(); index++) {
            setter.accept(items.get(index), index);
        }
    }

    /**
     * Moves one element by {@code delta} positions, if that lands inside the list.
     *
     * @return true when the list changed, false at a boundary
     */
    static <T> boolean swap(List<T> items, int index, int delta) {
        int target = index + delta;
        if (index < 0 || index >= items.size() || target < 0 || target >= items.size()) {
            return false;
        }
        items.set(target, items.set(index, items.get(target)));
        return true;
    }

    /**
     * A field DTO that changes only where it sits, leaving its rules and options untouched.
     *
     * <p>Two halves of the persistence contract meet here. {@code FieldDefinitionMapper} reads a null
     * collection as "this request says nothing about them" and keeps what is stored, while an empty
     * list means "there are none" and deletes every row — so a reorder must send null, or moving a
     * field one place up would silently strip its restrictions and options.
     *
     * <p>The scalars, on the other hand, must all be present: {@code updateEntity} assigns every one
     * of them unconditionally, and {@code name}, {@code label} and {@code type} are {@code NOT NULL}
     * columns. A patch carrying only the new index would fail at flush.
     *
     * @param field      the field being moved
     * @param orderIndex where it is moving to
     * @return a DTO safe to hand to {@code FieldDefinitionService.update}
     */
    static FieldDefinitionDTO reorderPatch(FieldDefinitionDTO field, Integer orderIndex) {
        return FieldDefinitionDTO.builder()
                .id(field.getId())
                .formDefinitionId(field.getFormDefinitionId())
                .name(field.getName())
                .label(field.getLabel())
                .type(field.getType())
                .required(field.getRequired())
                .orderIndex(orderIndex)
                .restrictions(null)
                .options(null)
                .build();
    }
}
