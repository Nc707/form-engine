package com.nc.formengine.flow.responses;

import com.nc.formengine.model.dto.FieldDefinitionDTO;
import com.nc.formengine.model.dto.FieldOptionDTO;
import com.nc.formengine.model.enums.FieldType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** Reading the storage encoding back out, including the parts of it that no longer make sense. */
class AnswerFormatterTest {

    @Test
    void selectShowsTheLabelThatWasPickedRatherThanTheStoredValue() {
        FieldDefinitionDTO field = withOptions(FieldType.SELECT, option("AR", "Argentina"), option("UY", "Uruguay"));

        assertThat(AnswerFormatter.format(field, "UY")).isEqualTo("Uruguay");
    }

    @Test
    void multiSelectIsSplitAndEachPartNamed() {
        FieldDefinitionDTO field = withOptions(FieldType.MULTI_SELECT,
                option("AR", "Argentina"), option("UY", "Uruguay"), option("BR", "Brasil"));

        assertThat(AnswerFormatter.format(field, "AR,BR")).isEqualTo("Argentina, Brasil");
    }

    /** An option removed after the fact leaves its value with nothing to name it. */
    @Test
    void aValueWithNoMatchingOptionIsShownAsStored() {
        FieldDefinitionDTO field = withOptions(FieldType.SELECT, option("AR", "Argentina"));

        assertThat(AnswerFormatter.format(field, "CL")).isEqualTo("CL");
        assertThat(AnswerFormatter.format(withOptions(FieldType.MULTI_SELECT, option("AR", "Argentina")), "AR,CL"))
                .isEqualTo("Argentina, CL");
    }

    @Test
    void booleansReadAsWords() {
        FieldDefinitionDTO field = of(FieldType.BOOLEAN);

        assertThat(AnswerFormatter.format(field, "true")).isEqualTo("Yes");
        assertThat(AnswerFormatter.format(field, "false")).isEqualTo("No");
    }

    @Test
    void datesAreShownTheWayPeopleWriteThem() {
        assertThat(AnswerFormatter.format(of(FieldType.DATE), "1990-04-12")).isEqualTo("12/04/1990");
    }

    /** A viewer reports what is stored; a value it cannot parse is exactly what someone needs to see. */
    @Test
    void unparseableValuesSurviveUntouched() {
        assertThat(AnswerFormatter.format(of(FieldType.DATE), "ayer")).isEqualTo("ayer");
        assertThat(AnswerFormatter.format(of(FieldType.BOOLEAN), "1")).isEqualTo("1");
    }

    @Test
    void anEmptyAnswerFormatsToNothing() {
        assertThat(AnswerFormatter.format(of(FieldType.TEXT), null)).isEmpty();
        assertThat(AnswerFormatter.format(of(FieldType.TEXT), "")).isEmpty();
    }

    /** A retired field has no definition, and its value has to come through anyway. */
    @Test
    void aValueWithNoFieldAtAllIsShownRaw() {
        assertThat(AnswerFormatter.format(null, "true")).isEqualTo("true");
    }

    private FieldDefinitionDTO of(FieldType type) {
        return FieldDefinitionDTO.builder().id(1L).name("f").label("F").type(type).build();
    }

    private FieldDefinitionDTO withOptions(FieldType type, FieldOptionDTO... options) {
        return FieldDefinitionDTO.builder()
                .id(1L).name("f").label("F").type(type).options(List.of(options))
                .build();
    }

    private FieldOptionDTO option(String value, String label) {
        return FieldOptionDTO.builder().value(value).label(label).build();
    }
}
