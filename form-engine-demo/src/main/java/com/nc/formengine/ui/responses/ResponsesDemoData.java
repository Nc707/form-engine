package com.nc.formengine.ui.responses;

import com.nc.formengine.business.service.FieldDefinitionService;
import com.nc.formengine.business.service.FormDefinitionService;
import com.nc.formengine.model.dto.FieldDefinitionDTO;
import com.nc.formengine.model.dto.FieldOptionDTO;
import com.nc.formengine.model.dto.FormDefinitionDTO;
import com.nc.formengine.model.enums.FieldType;
import com.nc.formengine.submission.business.service.FormSubmissionWorkflowService;
import com.nc.formengine.submission.business.service.SubmissionResult;
import com.nc.formengine.submission.model.dto.FieldSubmissionDTO;
import com.nc.formengine.submission.model.dto.FormSubmissionDTO;
import com.nc.formengine.submission.model.enums.SubmissionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Something for the response viewer to show.
 *
 * <p>The database is in-memory and recreated on every start, so without this the viewer opens empty
 * and the case it exists for cannot be seen at all. What it builds is that case: a form whose first
 * version collected answers and was then archived by a second version that no longer asks one of the
 * questions. The v1 submissions are still read against v1, so they keep the dropped field and its
 * answers; the v2 submission is read against v2 and does not have it. That is what versioning is for.
 *
 * <p>Order matters, twice over. Submissions can only be made against a published form, so v1 has to
 * collect them before v2 is published and archives it. And a field can only be dropped from a draft, so
 * dropping it happens on v2 before v2 goes live — never on the published v1, whose stored answers would
 * otherwise start being judged against rules nobody filling it in ever saw.
 */
@Component
class ResponsesDemoData implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ResponsesDemoData.class);

    private static final String CODE = "customer_onboarding";

    private final FormDefinitionService formDefinitionService;
    private final FieldDefinitionService fieldDefinitionService;
    private final FormSubmissionWorkflowService workflowService;

    ResponsesDemoData(FormDefinitionService formDefinitionService,
                      FieldDefinitionService fieldDefinitionService,
                      FormSubmissionWorkflowService workflowService) {
        this.formDefinitionService = formDefinitionService;
        this.fieldDefinitionService = fieldDefinitionService;
        this.workflowService = workflowService;
    }

    @Override
    public void run(String... args) {
        if (formDefinitionService.existsByCode(CODE)) {
            return;
        }

        FormDefinitionDTO v1 = formDefinitionService.publish(
                formDefinitionService.create(firstVersion()).getId());
        Long v1Id = v1.getId();

        submit(v1Id, "ana@example.com", Map.of(
                "full_name", "Ana Pérez",
                "birth_date", "1990-04-12",
                "country", "ar",
                "interests", "sports,music",
                "accepts_terms", "true",
                "contact_channel", "branch"));

        submit(v1Id, "luis@example.com", Map.of(
                "full_name", "Luis Gómez",
                "birth_date", "1985-11-03",
                "country", "uy",
                "interests", "travel",
                "accepts_terms", "true",
                "contact_channel", "phone"));

        // A draft: still being filled in, so it is missing answers on purpose.
        draft(v1Id, "mara@example.com", Map.of(
                "full_name", "Mara Ríos",
                "country", "br"));

        // A draft its author gave up on. Nothing was ever sent, so nothing is being retracted.
        Long abandoned = draft(v1Id, "sofia@example.com", Map.of(
                "full_name", "Sofía Vera"));
        if (abandoned != null) {
            workflowService.discard(abandoned);
        }

        // A response that arrived and was then annulled. Its answers have to survive being voided.
        Long annulled = submit(v1Id, "jorge@example.com", Map.of(
                "full_name", "Jorge Díaz",
                "birth_date", "1978-01-30",
                "country", "ar",
                "accepts_terms", "false",
                "contact_channel", "web"));
        if (annulled != null) {
            workflowService.voidSubmission(annulled);
        }

        // v2 stops asking for the contact channel. The field is dropped from the new draft, not from
        // the live v1 — v1 keeps it, which is why its submissions stay fully readable.
        FormDefinitionDTO v2 = formDefinitionService.createNewVersion(v1Id);
        retire("contact_channel", v2.getId());
        formDefinitionService.publish(v2.getId());

        submit(v2.getId(), "nadia@example.com", Map.of(
                "full_name", "Nadia Costa",
                "birth_date", "1996-07-21",
                "country", "br",
                "interests", "music,travel",
                "accepts_terms", "true"));

        log.info("Demo data ready: '{}' v1 (archived) and v2 (published), with submissions on both", CODE);
    }

    // -- definition ----------------------------------------------------------

    private FormDefinitionDTO firstVersion() {
        List<FieldDefinitionDTO> fields = new ArrayList<>(List.of(
                field("full_name", "Full name", FieldType.TEXT, 0, true, null),
                field("birth_date", "Date of birth", FieldType.DATE, 1, false, null),
                field("country", "Country", FieldType.SELECT, 2, false,
                        List.of(option("ar", "Argentina", 0), option("uy", "Uruguay", 1),
                                option("br", "Brazil", 2))),
                field("interests", "Interests", FieldType.MULTI_SELECT, 3, false,
                        List.of(option("sports", "Sports", 0), option("music", "Music", 1),
                                option("travel", "Travel", 2))),
                field("accepts_terms", "Accepts the terms", FieldType.BOOLEAN, 4, false, null),
                // v2 stops asking this. v1 keeps it, and so do the answers given under v1.
                field("contact_channel", "Preferred contact", FieldType.SELECT, 5, false,
                        List.of(option("web", "Website", 0), option("branch", "Branch", 1),
                                option("phone", "Phone", 2)))));

        return FormDefinitionDTO.builder()
                .code(CODE)
                .title("Customer onboarding")
                .description("Sample form for the response viewer")
                .version(1)
                .fields(fields)
                .build();
    }

    private FieldDefinitionDTO field(String name, String label, FieldType type, int order,
                                     boolean required, List<FieldOptionDTO> options) {
        return FieldDefinitionDTO.builder()
                .name(name).label(label).type(type)
                .orderIndex(order).required(required)
                .options(options)
                .build();
    }

    private FieldOptionDTO option(String value, String label, int order) {
        return FieldOptionDTO.builder().value(value).label(label).orderIndex(order).build();
    }

    /**
     * Drops a field from a draft version.
     *
     * <p>Ids differ between versions, so the field has to be looked up in the version being edited —
     * and that version has to be a draft, which is the whole reason this runs on v2 before it is
     * published rather than on the v1 that is already collecting answers.
     */
    private void retire(String name, Long formDefinitionId) {
        fieldDefinitionService.findByFormDefinitionId(formDefinitionId).stream()
                .filter(field -> name.equals(field.getName()))
                .findFirst()
                .ifPresent(field -> fieldDefinitionService.deleteById(field.getId()));
    }

    // -- submissions ---------------------------------------------------------

    private Long submit(Long formDefinitionId, String author, Map<String, String> answers) {
        return store(formDefinitionId, author, answers, SubmissionStatus.SUBMITTED);
    }

    private Long draft(Long formDefinitionId, String author, Map<String, String> answers) {
        return store(formDefinitionId, author, answers, SubmissionStatus.DRAFT);
    }

    private Long store(Long formDefinitionId, String author, Map<String, String> answers,
                       SubmissionStatus status) {
        FormSubmissionDTO submission = FormSubmissionDTO.builder()
                .formDefinitionId(formDefinitionId)
                .formCode(CODE)
                .author(author)
                .status(status)
                .fieldSubmissions(fieldSubmissions(formDefinitionId, answers))
                .build();

        SubmissionResult result = status == SubmissionStatus.DRAFT
                ? workflowService.saveDraft(submission)
                : workflowService.submit(submission);

        if (!result.persisted()) {
            log.warn("Demo submission by {} was rejected: {}", author, result.report());
            return null;
        }

        // A submission owns its answers and cascades into them, so they are already stored. Creating
        // them again here is what used to give every seeded submission a duplicate of every answer.
        return result.submission().getId();
    }

    private List<FieldSubmissionDTO> fieldSubmissions(Long formDefinitionId, Map<String, String> answers) {
        Map<String, FieldDefinitionDTO> byName = new LinkedHashMap<>();
        fieldDefinitionService.findByFormDefinitionId(formDefinitionId)
                .forEach(field -> byName.put(field.getName(), field));

        List<FieldSubmissionDTO> submissions = new ArrayList<>();
        byName.forEach((name, field) -> {
            String value = answers.get(name);
            if (value != null) {
                submissions.add(FieldSubmissionDTO.builder()
                        .fieldDefinitionId(field.getId())
                        .fieldName(field.getName())
                        .value(value)
                        .build());
            }
        });
        return submissions;
    }
}
