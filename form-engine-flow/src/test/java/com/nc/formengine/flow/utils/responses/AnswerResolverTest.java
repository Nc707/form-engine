package com.nc.formengine.flow.utils.responses;

import com.nc.formengine.business.service.FieldDefinitionService;
import com.nc.formengine.flow.utils.responses.AnswerResolver;
import com.nc.formengine.flow.utils.responses.ResolvedAnswer;
import com.nc.formengine.model.dto.FieldDefinitionDTO;
import com.nc.formengine.model.enums.FieldType;
import com.nc.formengine.submission.model.dto.FieldSubmissionDTO;
import com.nc.formengine.submission.model.dto.FormSubmissionDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

/** What a submission's answers look like once the labels are back on them. */
class AnswerResolverTest {

    private static final Long ARCHIVED_DEFINITION = 11L;

    private FieldDefinitionService fieldDefinitionService;
    private AnswerResolver resolver;

    @BeforeEach
    void setUp() {
        fieldDefinitionService = mock(FieldDefinitionService.class);
        resolver = new AnswerResolver(fieldDefinitionService);
    }

    /**
     * The case the viewer exists for: a submission against a definition that has since been
     * archived, holding an answer to a field the newer version dropped.
     */
    @Test
    void everythingAtOnce() {
        List<FieldDefinitionDTO> fields = List.of(
                field(1L, "full_name", "Full name", FieldType.TEXT),
                field(2L, "age", "Age", FieldType.NUMBER),
                field(3L, "accepts_terms", "Accepts the terms", FieldType.BOOLEAN));

        FormSubmissionDTO submission = submission(
                answer(1L, "full_name", "Ana"),
                // Answered twice: the later answer is the one that counts.
                answer(2L, "age", "30"),
                answer(2L, "age", "34"),
                // The field this belonged to is gone from the definition.
                answer(99L, "motivo", "mudanza"),
                // Gone, and without even a name snapshot to fall back on.
                answer(98L, null, "x"));
        // "accepts_terms" is declared but never answered.

        List<ResolvedAnswer> resolved = resolver.resolve(submission, fields);

        assertThat(resolved).containsExactly(
                new ResolvedAnswer("Full name", "Ana", true, false),
                new ResolvedAnswer("Age", "34", true, false),
                new ResolvedAnswer("Accepts the terms", "", false, false),
                new ResolvedAnswer("motivo", "mudanza", true, true),
                new ResolvedAnswer("Field #98", "x", true, true));
    }

    @Test
    void labelsAreResolvedAgainstTheDefinitionTheSubmissionPointsAt() {
        when(fieldDefinitionService.findByFormDefinitionId(ARCHIVED_DEFINITION))
                .thenReturn(List.of(field(1L, "full_name", "Full name", FieldType.TEXT)));

        FormSubmissionDTO submission = submission(answer(1L, "full_name", "Ana"));

        assertThat(resolver.resolve(submission))
                .extracting(ResolvedAnswer::label)
                .containsExactly("Full name");
        verify(fieldDefinitionService).findByFormDefinitionId(ARCHIVED_DEFINITION);
    }

    /** Ids differ between versions, so a name snapshot is what rescues an answer whose id moved. */
    @Test
    void anAnswerWhoseIdIsUnknownIsStillMatchedByName() {
        List<FieldDefinitionDTO> fields = List.of(field(42L, "full_name", "Full name", FieldType.TEXT));

        List<ResolvedAnswer> resolved = resolver.resolve(submission(answer(1L, "full_name", "Ana")), fields);

        assertThat(resolved).containsExactly(
                new ResolvedAnswer("Full name", "Ana", true, false));
    }

    /** The definition is gone entirely: every answer is orphaned, none is silently dropped. */
    @Test
    void aSubmissionWhoseDefinitionIsGoneKeepsAllItsAnswers() {
        List<ResolvedAnswer> resolved = resolver.resolve(
                submission(answer(1L, "full_name", "Ana"), answer(2L, "age", "34")), List.of());

        assertThat(resolved).allMatch(ResolvedAnswer::retired);
        assertThat(resolved).extracting(ResolvedAnswer::label).containsExactly("full_name", "age");
    }

    @Test
    void aSubmissionWithoutADefinitionIdAsksForNothing() {
        FormSubmissionDTO orphan = FormSubmissionDTO.builder().id(1L).build();

        assertThat(resolver.definitionOf(orphan)).isEmpty();
        verify(fieldDefinitionService, never()).findByFormDefinitionId(anyLong());
    }

    private FormSubmissionDTO submission(FieldSubmissionDTO... answers) {
        return FormSubmissionDTO.builder()
                .id(500L)
                .formDefinitionId(ARCHIVED_DEFINITION)
                .fieldSubmissions(new java.util.ArrayList<>(List.of(answers)))
                .build();
    }

    private FieldSubmissionDTO answer(Long fieldDefinitionId, String fieldName, String value) {
        return FieldSubmissionDTO.builder()
                .fieldDefinitionId(fieldDefinitionId)
                .fieldName(fieldName)
                .value(value)
                .build();
    }

    private FieldDefinitionDTO field(Long id, String name, String label, FieldType type) {
        return FieldDefinitionDTO.builder().id(id).name(name).label(label).type(type).build();
    }
}
