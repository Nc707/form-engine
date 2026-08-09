package com.nc.formengine.submission.dataimpl.daoimpl;

import com.nc.formengine.submission.data.dao.FormSubmissionDao;
import com.nc.formengine.submission.dataimpl.PersistenceTestConfiguration;
import com.nc.formengine.submission.dataimpl.entity.FieldSubmission;
import com.nc.formengine.submission.dataimpl.entity.FormSubmission;
import com.nc.formengine.submission.dataimpl.mapper.FieldSubmissionMapper;
import com.nc.formengine.submission.dataimpl.mapper.FormSubmissionMapper;
import com.nc.formengine.submission.dataimpl.repository.FormSubmissionRepository;
import com.nc.formengine.submission.model.dto.FormSubmissionDTO;
import com.nc.formengine.submission.model.enums.SubmissionStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What a submission keeps when it is written back.
 *
 * <p>A submission owns its answers with {@code orphanRemoval}, and the DTO-to-entity mapper does not
 * carry them. Saving a detached entity built from the DTO therefore used to delete every answer the
 * submission had — which is what cancelling one does, since cancelling is nothing but a status
 * change on a submission full of answers. These tests pin down that writing a submission back leaves
 * what the caller said nothing about alone.
 */
@SpringBootTest(classes = PersistenceTestConfiguration.class)
@Import({FormSubmissionDaoImpl.class, FormSubmissionMapper.class, FieldSubmissionMapper.class})
@Transactional
class FormSubmissionPersistenceTest {

    @Autowired
    private FormSubmissionDao dao;

    @Autowired
    private FormSubmissionRepository repository;

    @PersistenceContext
    private EntityManager entityManager;

    private Long submissionId;

    @BeforeEach
    void storeSubmissionWithThreeAnswers() {
        FormSubmission submission = new FormSubmission();
        submission.setFormDefinitionId(7L);
        submission.setFormCode("ALTA");
        submission.setSubmittedBy("ana@example.com");
        submission.setSubmittedAt(LocalDateTime.now());
        submission.setStatus(SubmissionStatus.SUBMITTED);

        submission.setFieldSubmissions(new java.util.ArrayList<>(List.of(
                answer(submission, 1L, "nombre", "Ana"),
                answer(submission, 2L, "edad", "34"),
                answer(submission, 3L, "acepta", "true"))));

        submissionId = repository.save(submission).getId();
        flushAndClear();
    }

    private FieldSubmission answer(FormSubmission parent, Long fieldDefinitionId, String name, String value) {
        FieldSubmission answer = new FieldSubmission();
        answer.setFormSubmission(parent);
        answer.setFieldDefinitionId(fieldDefinitionId);
        answer.setFieldName(name);
        answer.setValue(value);
        return answer;
    }

    /** The path {@code FormSubmissionWorkflowService.cancel} takes: read, flip the status, save. */
    @Test
    void cancellingASubmissionKeepsItsAnswers() {
        FormSubmissionDTO stored = dao.findById(submissionId).orElseThrow();
        assertThat(stored.getFieldSubmissions()).hasSize(3);

        stored.setStatus(SubmissionStatus.CANCELED);
        dao.save(stored);
        flushAndClear();

        FormSubmissionDTO reread = dao.findById(submissionId).orElseThrow();
        assertThat(reread.getStatus()).isEqualTo(SubmissionStatus.CANCELED);
        assertThat(reread.getFieldSubmissions())
                .extracting(answer -> answer.getFieldName() + "=" + answer.getValue())
                .containsExactlyInAnyOrder("nombre=Ana", "edad=34", "acepta=true");
    }

    /** A DTO that carries no answers at all is the worst case: nothing hints they should survive. */
    @Test
    void savingASubmissionThatCarriesNoAnswersKeepsTheStoredOnes() {
        FormSubmissionDTO blank = FormSubmissionDTO.builder()
                .id(submissionId)
                .formDefinitionId(7L)
                .formCode("ALTA")
                .submittedBy("ana@example.com")
                .status(SubmissionStatus.DRAFT)
                .build();

        dao.save(blank);
        flushAndClear();

        assertThat(dao.findById(submissionId).orElseThrow().getFieldSubmissions()).hasSize(3);
    }

    @Test
    void aSubmissionWithoutAnIdIsStillCreated() {
        FormSubmissionDTO fresh = FormSubmissionDTO.builder()
                .formDefinitionId(9L)
                .formCode("RECLAMO")
                .submittedBy("luis@example.com")
                .status(SubmissionStatus.DRAFT)
                .build();

        FormSubmissionDTO saved = dao.save(fresh);

        assertThat(saved.getId()).isNotNull().isNotEqualTo(submissionId);
        assertThat(saved.getFormCode()).isEqualTo("RECLAMO");
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
