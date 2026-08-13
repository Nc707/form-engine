package com.nc.formengine.model.validation;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The format two independent readers have to agree on.
 *
 * <p>The engine reads a stored answer; the renderer writes one. When those disagreed, a multi-select
 * answer validated against the wrong values and nobody found out.
 */
class AnswerCodecTest {

    @Test
    void selectionsSurviveTheRoundTrip() {
        var chosen = new LinkedHashSet<>(List.of("java", "spring", "sql"));

        assertThat(AnswerCodec.decodeSelections(AnswerCodec.encodeSelections(chosen)))
            .containsExactly("java", "spring", "sql");
    }

    /**
     * Both shapes reach the validator: the joined string through a stored submission, the widget's own
     * collection through the live per-field check while a form is being filled in.
     */
    @Test
    void decodesTheJoinedStringAndTheRawCollectionAlike() {
        assertThat(AnswerCodec.decodeSelections("java,spring")).containsExactly("java", "spring");
        assertThat(AnswerCodec.decodeSelections(List.of("java", "spring")))
            .containsExactly("java", "spring");
        assertThat(AnswerCodec.decodeSelections(new String[] {"java", "spring"}))
            .containsExactly("java", "spring");
    }

    @Test
    void aSingleValueDecodesToItself() {
        assertThat(AnswerCodec.decodeSelections("java")).containsExactly("java");
    }

    @Test
    void blanksAndSurroundingSpaceAreNotSelections() {
        assertThat(AnswerCodec.decodeSelections(" java , , spring ,"))
            .containsExactly("java", "spring");
        assertThat(AnswerCodec.decodeSelections("")).isEmpty();
        assertThat(AnswerCodec.decodeSelections(null)).isEmpty();
        assertThat(AnswerCodec.decodeSelections(List.of())).isEmpty();
    }

    @Test
    void encodesNothingAsTheEmptyAnswer() {
        assertThat(AnswerCodec.encodeSelections(null)).isEmpty();
        assertThat(AnswerCodec.encodeSelections(List.of())).isEmpty();
    }

    @Test
    void aBooleanAnswerIsWrittenAsTrueOrFalse() {
        assertThat(AnswerCodec.encodeBoolean(true)).isEqualTo("true");
        assertThat(AnswerCodec.encodeBoolean(false)).isEqualTo("false");
    }
}
