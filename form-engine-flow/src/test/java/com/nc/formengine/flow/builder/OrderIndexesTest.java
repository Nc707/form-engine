package com.nc.formengine.flow.builder;

import com.nc.formengine.model.dto.FieldOptionDTO;
import com.nc.formengine.model.dto.FieldRestrictionDTO;
import com.nc.formengine.model.dto.FieldDefinitionDTO;
import com.nc.formengine.model.enums.FieldType;
import com.nc.formengine.model.enums.RestrictionType;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class OrderIndexesTest {

    @Test
    void movingAnElementSwapsItWithItsNeighbour() {
        var items = new ArrayList<>(List.of("a", "b", "c"));

        assertThat(OrderIndexes.swap(items, 0, 1)).isTrue();
        assertThat(items).containsExactly("b", "a", "c");

        assertThat(OrderIndexes.swap(items, 2, -1)).isTrue();
        assertThat(items).containsExactly("b", "c", "a");
    }

    @Test
    void movingPastEitherEndChangesNothing() {
        var items = new ArrayList<>(List.of("a", "b"));

        assertThat(OrderIndexes.swap(items, 0, -1)).isFalse();
        assertThat(OrderIndexes.swap(items, 1, 1)).isFalse();
        assertThat(OrderIndexes.swap(items, 5, -1)).isFalse();
        assertThat(items).containsExactly("a", "b");
    }

    @Test
    void reindexingClosesTheGapLeftByARemoval() {
        var options = new ArrayList<>(List.of(
                option("a", 0), option("b", 1), option("c", 2)));
        options.remove(1);

        OrderIndexes.reindex(options, FieldOptionDTO::setOrderIndex);

        assertThat(options).extracting(FieldOptionDTO::getOrderIndex).containsExactly(0, 1);
        assertThat(options).extracting(FieldOptionDTO::getValue).containsExactly("a", "c");
    }

    @Test
    void fieldsSortByOrderWithMissingIndexesLast() {
        var fields = new ArrayList<>(List.of(
                field(3L, "third", 2),
                field(1L, "first", 0),
                field(9L, "unordered", null),
                field(2L, "second", 1)));

        fields.sort(OrderIndexes.byOrder());

        assertThat(fields).extracting(FieldDefinitionDTO::getName)
                .containsExactly("first", "second", "third", "unordered");
    }

    /**
     * The direct guard on the "absent is not empty" contract: a reorder must not be able to delete
     * the rules and options of the field it moves.
     */
    @Test
    void aReorderPatchCarriesEveryScalarAndSpeaksForNeitherCollection() {
        var field = field(7L, "email", 4);
        field.setFormDefinitionId(42L);
        field.setLabel("Email address");
        field.setRequired(true);
        field.setRestrictions(new ArrayList<>(List.of(FieldRestrictionDTO.builder()
                .restrictionType(RestrictionType.EMAIL).build())));
        field.setOptions(new ArrayList<>(List.of(option("a", 0))));

        var patch = OrderIndexes.reorderPatch(field, 1);

        assertThat(patch.getOrderIndex()).isEqualTo(1);
        assertThat(patch.getRestrictions()).isNull();
        assertThat(patch.getOptions()).isNull();
        // Every NOT NULL column has to be on the patch: updateEntity assigns them unconditionally.
        assertThat(patch.getId()).isEqualTo(7L);
        assertThat(patch.getFormDefinitionId()).isEqualTo(42L);
        assertThat(patch.getName()).isEqualTo("email");
        assertThat(patch.getLabel()).isEqualTo("Email address");
        assertThat(patch.getType()).isEqualTo(FieldType.TEXT);
        assertThat(patch.getRequired()).isTrue();
    }

    private static FieldDefinitionDTO field(Long id, String name, Integer orderIndex) {
        return FieldDefinitionDTO.builder()
                .id(id)
                .name(name)
                .label(name)
                .type(FieldType.TEXT)
                .orderIndex(orderIndex)
                .required(false)
                .build();
    }

    private static FieldOptionDTO option(String value, Integer orderIndex) {
        return FieldOptionDTO.builder().label(value).value(value).orderIndex(orderIndex).build();
    }
}
