package com.nc.formengine.flow.responses;

import com.nc.formengine.business.service.FieldDefinitionService;
import com.nc.formengine.model.dto.FieldDefinitionDTO;
import com.nc.formengine.submission.model.dto.FieldSubmissionDTO;
import com.nc.formengine.submission.model.dto.FormSubmissionDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Puts the labels back on a submission's answers.
 *
 * <p>A submission stores a field id and a raw string. Reading it back means going to the field
 * definitions for the name and the type — and <b>to the definition the submission points at</b>, not
 * to whatever version of that form is published today. Publishing a new version archives the old one
 * precisely so that submissions made under it stay interpretable, and resolving against the current
 * version would relabel old answers with new fields' names, or silently drop the ones that were
 * removed. So this class only ever looks up {@code submission.getFormDefinitionId()}.
 *
 * <p>That invariant is also what makes matching by name a safe fallback: within one definition a
 * name is unique. It would not be safe across versions, where the same name can be reused for a
 * field of a different type.
 *
 * <p>Ordering: the definition's fields first, in their declared order, whether or not the submission
 * answered them; then any answer whose field no longer exists, in the order it was stored. Retired
 * answers have no {@code orderIndex} to place them by, so interleaving them would only invent an
 * order.
 */
@Component
public class AnswerResolver {

    private final FieldDefinitionService fieldDefinitionService;

    public AnswerResolver(FieldDefinitionService fieldDefinitionService) {
        this.fieldDefinitionService = fieldDefinitionService;
    }

    /** The fields of the definition this submission was filled in against, in their declared order. */
    public List<FieldDefinitionDTO> definitionOf(FormSubmissionDTO submission) {
        if (submission == null || submission.getFormDefinitionId() == null) {
            return List.of();
        }
        return fieldDefinitionService.findByFormDefinitionId(submission.getFormDefinitionId());
    }

    public List<ResolvedAnswer> resolve(FormSubmissionDTO submission) {
        return resolve(submission, definitionOf(submission));
    }

    /**
     * @param fields the fields of {@code submission}'s own definition, already ordered — passed in so
     *               that a grid resolving many submissions of one form reads them once
     */
    public List<ResolvedAnswer> resolve(FormSubmissionDTO submission, List<FieldDefinitionDTO> fields) {
        List<FieldSubmissionDTO> answers = submission == null || submission.getFieldSubmissions() == null
                ? List.of()
                : submission.getFieldSubmissions();

        Map<Long, FieldSubmissionDTO> byFieldId = new LinkedHashMap<>();
        Map<String, FieldSubmissionDTO> byFieldName = new LinkedHashMap<>();
        for (FieldSubmissionDTO answer : answers) {
            if (answer == null) {
                continue;
            }
            // Nothing stops a submission from holding two answers for one field. Last one wins,
            // rather than letting a duplicate blow up the whole view.
            if (answer.getFieldDefinitionId() != null) {
                byFieldId.put(answer.getFieldDefinitionId(), answer);
            }
            if (answer.getFieldName() != null) {
                byFieldName.put(answer.getFieldName(), answer);
            }
        }

        // An answer belongs to a field that still exists if either half of its identity is known.
        // Deciding by identity rather than by which answer won the lookup keeps a duplicate from
        // being mistaken for a retired field.
        Set<Long> knownIds = new HashSet<>();
        Set<String> knownNames = new HashSet<>();
        for (FieldDefinitionDTO field : fields) {
            knownIds.add(field.getId());
            knownNames.add(field.getName());
        }

        List<ResolvedAnswer> resolved = new ArrayList<>();

        for (FieldDefinitionDTO field : fields) {
            FieldSubmissionDTO answer = byFieldId.get(field.getId());
            if (answer == null) {
                answer = byFieldName.get(field.getName());
            }

            String value = answer == null ? null : answer.getValue();
            boolean answered = value != null && !value.isEmpty();
            resolved.add(new ResolvedAnswer(
                    labelOf(field),
                    answered ? AnswerFormatter.format(field, value) : "",
                    answered,
                    false));
        }

        for (FieldSubmissionDTO answer : answers) {
            if (answer == null
                    || knownIds.contains(answer.getFieldDefinitionId())
                    || knownNames.contains(answer.getFieldName())) {
                continue;
            }
            String value = answer.getValue();
            resolved.add(new ResolvedAnswer(
                    retiredLabelOf(answer),
                    value == null ? "" : value,
                    value != null && !value.isEmpty(),
                    true));
        }

        return resolved;
    }

    private String labelOf(FieldDefinitionDTO field) {
        if (field.getLabel() != null && !field.getLabel().isBlank()) {
            return field.getLabel();
        }
        return field.getName() != null ? field.getName() : "Field #" + field.getId();
    }

    /**
     * A retired field has no definition left to name it, so the snapshot the submission kept is all
     * there is — and even that is optional, since the submission modules are decoupled from the
     * definition ones and a caller may never have supplied it.
     */
    private String retiredLabelOf(FieldSubmissionDTO answer) {
        if (answer.getFieldName() != null && !answer.getFieldName().isBlank()) {
            return answer.getFieldName();
        }
        return "Field #" + answer.getFieldDefinitionId();
    }
}
