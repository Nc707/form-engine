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
 * and the one case it exists for cannot be seen at all. What it builds is that case: a form whose
 * first version collected answers and was then archived by publishing a second version that dropped
 * a field. The submissions against v1 still hold an answer to that field, which is what the detail
 * view has to show as retired.
 *
 * <p>Order matters. Submissions can only be made against a published form, so v1 has to collect them
 * before v2 is published and archives it.
 */
@Component
class ResponsesDemoData implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ResponsesDemoData.class);

    private static final String CODE = "ALTA-CLIENTE";

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
                "nombre", "Ana Pérez",
                "nacimiento", "1990-04-12",
                "pais", "AR",
                "intereses", "deportes,musica",
                "acepta", "true",
                "canal", "sucursal"));

        submit(v1Id, "luis@example.com", Map.of(
                "nombre", "Luis Gómez",
                "nacimiento", "1985-11-03",
                "pais", "UY",
                "intereses", "viajes",
                "acepta", "true",
                "canal", "telefono"));

        // A draft: still being filled in, so it is missing answers on purpose.
        draft(v1Id, "mara@example.com", Map.of(
                "nombre", "Mara Ríos",
                "pais", "BR"));

        // A draft its author gave up on. Nothing was ever sent.
        Long abandoned = draft(v1Id, "sofia@example.com", Map.of(
                "nombre", "Sofía Vera"));
        if (abandoned != null) {
            workflowService.discard(abandoned);
        }

        // A response that arrived and was then annulled. Its answers have to survive being voided.
        Long annulled = submit(v1Id, "jorge@example.com", Map.of(
                "nombre", "Jorge Díaz",
                "nacimiento", "1978-01-30",
                "pais", "AR",
                "acepta", "false",
                "canal", "web"));
        if (annulled != null) {
            workflowService.voidSubmission(annulled);
        }

        // Retire the field from v1 itself, after its submissions were made. A field dropped only
        // from v2 would prove nothing: submissions point at v1, where it would still exist. What
        // makes an answer retired is that its own definition no longer declares the field.
        retire("canal", v1Id);

        FormDefinitionDTO v2 = formDefinitionService.createNewVersion(v1Id);
        formDefinitionService.publish(v2.getId());

        submit(v2.getId(), "nadia@example.com", Map.of(
                "nombre", "Nadia Costa",
                "nacimiento", "1996-07-21",
                "pais", "BR",
                "intereses", "musica,viajes",
                "acepta", "true"));

        log.info("Demo data ready: '{}' v1 (archived) and v2 (published), with submissions on both", CODE);
    }

    // -- definition ----------------------------------------------------------

    private FormDefinitionDTO firstVersion() {
        List<FieldDefinitionDTO> fields = new ArrayList<>(List.of(
                field("nombre", "Nombre completo", FieldType.TEXT, 0, true, null),
                field("nacimiento", "Fecha de nacimiento", FieldType.DATE, 1, false, null),
                field("pais", "País", FieldType.SELECT, 2, false,
                        List.of(option("AR", "Argentina", 0), option("UY", "Uruguay", 1),
                                option("BR", "Brasil", 2))),
                field("intereses", "Intereses", FieldType.MULTI_SELECT, 3, false,
                        List.of(option("deportes", "Deportes", 0), option("musica", "Música", 1),
                                option("viajes", "Viajes", 2))),
                field("acepta", "¿Acepta los términos?", FieldType.BOOLEAN, 4, false, null),
                // Dropped in v2: this is the field the viewer has to show as retired.
                field("canal", "Canal de contacto", FieldType.SELECT, 5, false,
                        List.of(option("web", "Sitio web", 0), option("sucursal", "Sucursal", 1),
                                option("telefono", "Teléfono", 2)))));

        return FormDefinitionDTO.builder()
                .code(CODE)
                .title("Alta de cliente")
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

    /** Ids differ between versions, so the field to drop has to be looked up in that version. */
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
