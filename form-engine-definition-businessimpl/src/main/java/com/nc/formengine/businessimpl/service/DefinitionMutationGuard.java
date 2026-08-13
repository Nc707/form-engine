package com.nc.formengine.businessimpl.service;

import com.nc.formengine.data.dao.FieldDefinitionDao;
import com.nc.formengine.data.dao.FormDao;
import com.nc.formengine.model.dto.FieldDefinitionDTO;
import com.nc.formengine.model.dto.FormDefinitionDTO;
import com.nc.formengine.model.enums.FormDefinitionStatus;
import com.nc.formengine.model.exception.FieldDefinitionNotFoundException;
import com.nc.formengine.model.exception.FormDefinitionNotEditableException;
import com.nc.formengine.model.exception.FormDefinitionNotFoundException;
import com.nc.formengine.model.exception.ValidationFailedException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Refuses to change a definition that is no longer a draft.
 *
 * <p>{@code FormDefinitionStatus} exists so that stored answers keep being judged by the rules they
 * were shown. That only holds if <em>every</em> part of a definition freezes when it is published, and
 * for a long time only the form's own row did: its fields, their options, their dependencies and its
 * layouts could all be rewritten afterwards, and all four were exposed over REST. Adding a required
 * field to a published form was enough to make every submission ever made against it retroactively
 * invalid — the exact scenario the lifecycle is documented as preventing.
 *
 * <p>One collaborator rather than the same check copied into five services, so there is one place to
 * read and one place to get it wrong.
 */
@Component
@RequiredArgsConstructor
class DefinitionMutationGuard {

    private final FormDao formDao;
    private final FieldDefinitionDao fieldDefinitionDao;

    /**
     * Allows a change to a form only while it is a draft.
     *
     * @param formDefinitionId the form about to be changed
     * @return the form, for callers that need the rest of it
     * @throws FormDefinitionNotFoundException  if there is no such form
     * @throws FormDefinitionNotEditableException if it is published or archived
     */
    FormDefinitionDTO requireDraft(Long formDefinitionId) {
        if (formDefinitionId == null) {
            throw new ValidationFailedException("The change does not say which form it belongs to",
                List.of("formDefinitionId is required"));
        }
        FormDefinitionDTO form = formDao.findById(formDefinitionId)
            .orElseThrow(() -> new FormDefinitionNotFoundException(formDefinitionId));
        if (form.getStatus() != FormDefinitionStatus.DRAFT) {
            throw new FormDefinitionNotEditableException(formDefinitionId, form.getStatus());
        }
        return form;
    }

    /**
     * Allows a change to something that belongs to a field, by checking the field's form.
     *
     * @param fieldDefinitionId the field whose options, rules or placement are being changed
     * @return the field
     * @throws FieldDefinitionNotFoundException if there is no such field
     */
    FieldDefinitionDTO requireDraftOfField(Long fieldDefinitionId) {
        if (fieldDefinitionId == null) {
            throw new ValidationFailedException("The change does not say which field it belongs to",
                List.of("fieldDefinitionId is required"));
        }
        FieldDefinitionDTO field = fieldDefinitionDao.findById(fieldDefinitionId)
            .orElseThrow(() -> new FieldDefinitionNotFoundException(fieldDefinitionId));
        requireDraft(field.getFormDefinitionId());
        return field;
    }

    /** Turns broken domain rules into the 422 the error model reserves for them. */
    void requireNoProblems(String what, List<String> problems) {
        if (!problems.isEmpty()) {
            throw new ValidationFailedException(what + " breaks "
                + (problems.size() == 1 ? "a rule" : problems.size() + " rules"), problems);
        }
    }
}
