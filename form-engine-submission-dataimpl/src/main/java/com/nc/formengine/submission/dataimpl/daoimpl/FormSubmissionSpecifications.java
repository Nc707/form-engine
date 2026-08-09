package com.nc.formengine.submission.dataimpl.daoimpl;

import com.nc.formengine.submission.dataimpl.entity.FormSubmission;
import com.nc.formengine.submission.model.dto.SubmissionFilter;
import com.nc.formengine.submission.model.enums.SubmissionStatus;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns a {@link SubmissionFilter} into a query.
 *
 * <p>Only the components the filter actually speaks for become predicates, so an empty filter turns
 * into {@link Specification#unrestricted()} and matches everything. Note that the predicates are
 * collected and combined at the end rather than folded into a running {@code null}: since Spring
 * Data JPA 4, {@code Specification.and} rejects a null receiver.
 */
final class FormSubmissionSpecifications {

    private FormSubmissionSpecifications() {
    }

    static Specification<FormSubmission> matching(SubmissionFilter filter) {
        if (filter == null) {
            return Specification.unrestricted();
        }

        List<Specification<FormSubmission>> predicates = new ArrayList<>();
        if (filter.formDefinitionId() != null) {
            predicates.add(hasFormDefinitionId(filter.formDefinitionId()));
        }
        if (filter.status() != null) {
            predicates.add(hasStatus(filter.status()));
        }
        if (filter.submittedBy() != null && !filter.submittedBy().isBlank()) {
            predicates.add(submittedByContains(filter.submittedBy()));
        }

        return predicates.isEmpty() ? Specification.unrestricted() : Specification.allOf(predicates);
    }

    static Specification<FormSubmission> hasStatus(SubmissionStatus status) {
        return (root, query, builder) -> builder.equal(root.get("status"), status);
    }

    private static Specification<FormSubmission> hasFormDefinitionId(Long formDefinitionId) {
        return (root, query, builder) -> builder.equal(root.get("formDefinitionId"), formDefinitionId);
    }

    /** Case-insensitive substring: this backs a search box, not a lookup by exact address. */
    private static Specification<FormSubmission> submittedByContains(String fragment) {
        String pattern = "%" + fragment.strip().toLowerCase() + "%";
        return (root, query, builder) -> builder.like(builder.lower(root.get("submittedBy")), pattern);
    }
}
