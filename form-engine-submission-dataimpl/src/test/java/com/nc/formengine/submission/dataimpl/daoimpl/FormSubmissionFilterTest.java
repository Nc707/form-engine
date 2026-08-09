package com.nc.formengine.submission.dataimpl.daoimpl;

import com.nc.formengine.submission.data.dao.FormSubmissionDao;
import com.nc.formengine.submission.dataimpl.PersistenceTestConfiguration;
import com.nc.formengine.submission.dataimpl.entity.FormSubmission;
import com.nc.formengine.submission.dataimpl.mapper.FieldSubmissionMapper;
import com.nc.formengine.submission.dataimpl.mapper.FormSubmissionMapper;
import com.nc.formengine.submission.dataimpl.repository.FormSubmissionRepository;
import com.nc.formengine.submission.model.dto.FormSubmissionDTO;
import com.nc.formengine.submission.model.dto.SubmissionFilter;
import com.nc.formengine.submission.model.enums.SubmissionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** How a {@link SubmissionFilter} narrows a page of submissions, and what it counts. */
@SpringBootTest(classes = PersistenceTestConfiguration.class)
@Import({FormSubmissionDaoImpl.class, FormSubmissionMapper.class, FieldSubmissionMapper.class})
@Transactional
class FormSubmissionFilterTest {

    private static final Long ALTA = 1L;
    private static final Long RECLAMO = 2L;

    @Autowired
    private FormSubmissionDao dao;

    @Autowired
    private FormSubmissionRepository repository;

    @BeforeEach
    void storeSubmissions() {
        store(ALTA, "Ana@Example.com", SubmissionStatus.SUBMITTED);
        store(ALTA, "luis@example.com", SubmissionStatus.SUBMITTED);
        store(ALTA, "luis@example.com", SubmissionStatus.DRAFT);
        store(ALTA, "mara@example.com", SubmissionStatus.CANCELED);
        store(RECLAMO, "ana@example.com", SubmissionStatus.SUBMITTED);
    }

    @Test
    void aFilterWithNothingSetMatchesEverything() {
        assertThat(page(new SubmissionFilter(null, null, null))).hasSize(5);
    }

    @Test
    void componentsOfTheFilterNarrowTogether() {
        assertThat(page(new SubmissionFilter(ALTA, null, null))).hasSize(4);
        assertThat(page(new SubmissionFilter(ALTA, SubmissionStatus.SUBMITTED, null))).hasSize(2);
        assertThat(page(new SubmissionFilter(ALTA, SubmissionStatus.SUBMITTED, "luis"))).hasSize(1);
    }

    @Test
    void theAuthorIsMatchedAsACaseInsensitiveSubstring() {
        assertThat(page(new SubmissionFilter(null, null, "ANA@"))).hasSize(2);
        assertThat(page(new SubmissionFilter(null, null, "example.com"))).hasSize(5);
        assertThat(page(new SubmissionFilter(null, null, "  luis  "))).hasSize(2);
    }

    @Test
    void aBlankAuthorDoesNotNarrowAnything() {
        assertThat(page(new SubmissionFilter(null, null, "   "))).hasSize(5);
    }

    @Test
    void countingNamesEveryStateIncludingTheEmptyOnes() {
        Map<SubmissionStatus, Long> counts = dao.countByStatus(new SubmissionFilter(ALTA, null, null));

        assertThat(counts).containsOnlyKeys(SubmissionStatus.values());
        assertThat(counts.get(SubmissionStatus.SUBMITTED)).isEqualTo(2L);
        assertThat(counts.get(SubmissionStatus.DRAFT)).isEqualTo(1L);
        assertThat(counts.get(SubmissionStatus.CANCELED)).isEqualTo(1L);

        assertThat(dao.countByStatus(new SubmissionFilter(RECLAMO, null, null)))
                .containsEntry(SubmissionStatus.DRAFT, 0L);
    }

    @Test
    void theFilteredPageIsStillPagedAndSorted() {
        var firstPage = dao.findAll(new SubmissionFilter(ALTA, null, null),
                PageRequest.of(0, 2, Sort.by(Sort.Direction.ASC, "submittedBy")));

        assertThat(firstPage.getTotalElements()).isEqualTo(4);
        assertThat(firstPage.getTotalPages()).isEqualTo(2);
        assertThat(firstPage.getContent())
                .extracting(FormSubmissionDTO::getSubmittedBy)
                .containsExactly("Ana@Example.com", "luis@example.com");
    }

    private java.util.List<FormSubmissionDTO> page(SubmissionFilter filter) {
        return dao.findAll(filter, PageRequest.of(0, 50)).getContent();
    }

    private void store(Long formDefinitionId, String submittedBy, SubmissionStatus status) {
        FormSubmission submission = new FormSubmission();
        submission.setFormDefinitionId(formDefinitionId);
        submission.setFormCode(formDefinitionId.equals(ALTA) ? "ALTA" : "RECLAMO");
        submission.setSubmittedBy(submittedBy);
        submission.setSubmittedAt(LocalDateTime.now());
        submission.setStatus(status);
        repository.save(submission);
    }
}
