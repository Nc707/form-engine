package com.nc.formengine.ui.responses;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** What a free-text answer does to a CSV file. */
class SubmissionCsvWriterTest {

    @Test
    void aPlainTableNeedsNoQuoting() {
        String csv = SubmissionCsvWriter.write(List.of("a", "b"), List.of(List.of("1", "2")));

        assertThat(csv).isEqualTo(SubmissionCsvWriter.BYTE_ORDER_MARK + "a,b\r\n1,2\r\n");
    }

    @Test
    void cellsWithCommasQuotesOrNewlinesAreQuoted() {
        String csv = SubmissionCsvWriter.write(
                List.of("respuesta"),
                List.of(List.of("Buenos Aires, Argentina"),
                        List.of("dijo \"hola\""),
                        List.of("linea uno\nlinea dos")));

        assertThat(csv).contains("\"Buenos Aires, Argentina\"");
        assertThat(csv).contains("\"dijo \"\"hola\"\"\"");
        assertThat(csv).contains("\"linea uno\nlinea dos\"");
    }

    /** A spreadsheet reads a leading = as a formula, which would run someone's answer as code. */
    @Test
    void cellsThatLookLikeFormulasAreDefused() {
        String csv = SubmissionCsvWriter.write(
                List.of("respuesta"),
                List.of(List.of("=1+1"), List.of("+54 11 5555"), List.of("@casa"), List.of("-3")));

        assertThat(csv).contains("'=1+1");
        assertThat(csv).contains("'+54 11 5555");
        assertThat(csv).contains("'@casa");
        assertThat(csv).contains("'-3");
    }

    @Test
    void anUnansweredFieldLeavesAnEmptyCellRatherThanAWord() {
        String csv = SubmissionCsvWriter.write(
                List.of("nombre", "edad"), List.of(Arrays.asList("Ana", null)));

        assertThat(csv).endsWith("Ana,\r\n");
    }

    /** The mark is what makes a spreadsheet read the accents in Spanish labels. */
    @Test
    void theFileOpensWithAByteOrderMark() {
        String csv = SubmissionCsvWriter.write(List.of("¿Acepta términos?"), List.of());

        assertThat(csv).startsWith(SubmissionCsvWriter.BYTE_ORDER_MARK);
        assertThat(csv).contains("¿Acepta términos?");
    }
}
