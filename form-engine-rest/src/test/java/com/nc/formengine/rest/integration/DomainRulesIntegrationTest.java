package com.nc.formengine.rest.integration;

import com.nc.formengine.business.service.FormDefinitionService;
import com.nc.formengine.business.service.FormLayoutService;
import com.nc.formengine.model.dto.FieldDefinitionDTO;
import com.nc.formengine.model.dto.FieldLayoutDTO;
import com.nc.formengine.model.dto.FieldOptionDTO;
import com.nc.formengine.model.dto.FieldRestrictionDTO;
import com.nc.formengine.model.dto.FormDefinitionDTO;
import com.nc.formengine.model.dto.FormLayoutDTO;
import com.nc.formengine.model.enums.FieldType;
import com.nc.formengine.model.enums.RestrictionType;
import com.nc.formengine.submission.business.service.FormSubmissionWorkflowService;
import com.nc.formengine.submission.model.dto.FieldSubmissionDTO;
import com.nc.formengine.submission.model.dto.FormSubmissionDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The API is held to the engine's rules, not merely to its own request shapes.
 *
 * <p>Each rule here is unit-tested against its service as well. Asserting them again through HTTP is
 * the point rather than duplication: every one of them used to live only in the Vaadin builder, so a
 * client going through the API walked straight past all of them, and a service-level test cannot show
 * that the controller path is guarded.
 *
 * <p>Setup that moves a definition or a submission through its lifecycle goes through the services,
 * because those operations are deliberately not exposed over REST. What is asserted is always the HTTP
 * write.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DomainRulesIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FormDefinitionService formDefinitionService;

    @Autowired
    private FormSubmissionWorkflowService workflowService;

    @Autowired
    private FormLayoutService formLayoutService;

    // --- a published definition is frozen whole ---------------------------------------------------

    @Test
    void addingAFieldToAPublishedFormIsRefused() throws Exception {
        FormDefinitionDTO form = publishedForm("frozen_add");

        send(post("/api/v1/field-definitions"), field(form.getId(), "extra", FieldType.TEXT))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://form-engine/errors/illegal-state"));
    }

    /** The sharpest one: this is how an archived submission's field used to be deletable. */
    @Test
    void deletingAFieldOfAPublishedFormIsRefused() throws Exception {
        FormDefinitionDTO form = publishedForm("frozen_delete");

        mockMvc.perform(delete("/api/v1/field-definitions/{id}", onlyFieldOf(form).getId()))
                .andExpect(status().isConflict());
    }

    @Test
    void addingAnOptionToAFieldOfAPublishedFormIsRefused() throws Exception {
        FormDefinitionDTO form = publishedForm("frozen_option");

        send(post("/api/v1/field-options"), FieldOptionDTO.builder()
                .fieldDefinitionId(onlyFieldOf(form).getId())
                .label("Extra").value("extra").orderIndex(0).build())
                .andExpect(status().isConflict());
    }

    // --- a field has to be one the engine can work with -------------------------------------------

    @Test
    void aSecondFieldOfTheSameNameIsRefused() throws Exception {
        Long formId = draftForm("duplicate_name").getId();
        send(post("/api/v1/field-definitions"), field(formId, "nickname", FieldType.TEXT))
                .andExpect(status().isCreated());

        send(post("/api/v1/field-definitions"), field(formId, "nickname", FieldType.TEXT))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://form-engine/errors/duplicate-resource"));
    }

    @Test
    void aNameThatCouldNotBeUsedAsAKeyIsRefused() throws Exception {
        Long formId = draftForm("bad_name").getId();

        send(post("/api/v1/field-definitions"), field(formId, "not a key", FieldType.TEXT))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void aSelectWithNothingToSelectIsRefused() throws Exception {
        Long formId = draftForm("empty_select").getId();

        send(post("/api/v1/field-definitions"), field(formId, "country", FieldType.SELECT))
                .andExpect(status().isUnprocessableEntity());
    }

    /**
     * Both of these used to be stored, echoed back, and then enforce nothing at all — the failure mode
     * the builder's own comment said it existed to prevent, on the one path the builder could not see.
     */
    @Test
    void aRuleThatDoesNotApplyToTheFieldTypeIsRefused() throws Exception {
        Long formId = draftForm("wrong_rule").getId();
        FieldDefinitionDTO field = field(formId, "nickname", FieldType.TEXT);
        field.setRestrictions(List.of(FieldRestrictionDTO.builder()
                .restrictionType(RestrictionType.MIN_VALUE)
                .parameters(Map.of("minValue", 3))
                .build()));

        send(post("/api/v1/field-definitions"), field)
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void aRuleWithoutItsParameterIsRefused() throws Exception {
        Long formId = draftForm("bare_rule").getId();
        FieldDefinitionDTO field = field(formId, "nickname", FieldType.TEXT);
        field.setRestrictions(List.of(FieldRestrictionDTO.builder()
                .restrictionType(RestrictionType.MIN_LENGTH)
                .build()));

        send(post("/api/v1/field-definitions"), field)
                .andExpect(status().isUnprocessableEntity());
    }

    // --- a stored layout cannot be hijacked by naming its id ---------------------------------------

    /**
     * The DAO's save() treats a non-null id as "update that row" — the same trick every DAO in this
     * layer plays to preserve an orphanRemoval collection across a real update. Nothing but the
     * service stood between a POST and reusing someone else's id, including the layout of a form
     * that was already published: this used to overwrite it in place, rewriting a frozen form's
     * layout onto a different, unrelated draft.
     */
    @Test
    void creatingALayoutThatNamesAnExistingIdIsRefused() throws Exception {
        FormDefinitionDTO formA = formWithOneField("layout_hijack_target");
        FormLayoutDTO formALayout = formLayoutService.createLayout(layout(formA.getId(), formA.getFields().get(0).getId()));
        formDefinitionService.publish(formA.getId());

        FormDefinitionDTO formB = formWithOneField("layout_hijack_source");

        FormLayoutDTO hijack = layout(formB.getId(), formB.getFields().get(0).getId());
        hijack.setId(formALayout.getId());

        send(post("/api/v1/form-layouts"), hijack)
                .andExpect(status().isBadRequest());

        FormLayoutDTO stillFormAs = formLayoutService.getLayoutById(formALayout.getId()).orElseThrow();
        assertThat(stillFormAs.getFormDefinitionId()).isEqualTo(formA.getId());
    }

    /**
     * Validating the request's own {@code formDefinitionId} instead of the layout's actual one used
     * to pass this straight through: form B's fields are perfectly valid for form B, so the 422 this
     * asserts never fired, and the layout was saved with form B's placements while staying addressed
     * under form A's id — the exact cross-form layout the rule is meant to refuse, reached by the one
     * path that wasn't checking against the right form.
     */
    @Test
    void updatingALayoutWithAnotherFormsFieldsIsRefused() throws Exception {
        FormDefinitionDTO formA = formWithOneField("layout_move_target");
        FormLayoutDTO formALayout = formLayoutService.createLayout(layout(formA.getId(), formA.getFields().get(0).getId()));

        FormDefinitionDTO formB = formWithOneField("layout_move_source");
        FormLayoutDTO moved = layout(formB.getId(), formB.getFields().get(0).getId());

        send(put("/api/v1/form-layouts/{id}", formALayout.getId()), moved)
                .andExpect(status().isUnprocessableEntity());

        FormLayoutDTO stillFormAs = formLayoutService.getLayoutById(formALayout.getId()).orElseThrow();
        assertThat(stillFormAs.getFormDefinitionId()).isEqualTo(formA.getId());
        assertThat(stillFormAs.getFieldLayouts())
                .extracting(FieldLayoutDTO::getFieldDefinitionId)
                .containsExactly(formA.getFields().get(0).getId());
    }

    // --- answers belong to the submission they were given in --------------------------------------

    /**
     * {@code SUBMITTED} means "validated as its definition would accept it". That only holds if the
     * answers stop being editable, which they did not: validation ran in the transaction that wrote the
     * submission, and every answer was a CRUD resource of its own afterwards.
     */
    @Test
    void writingAnAnswerToASubmissionThatWasAlreadySentIsRefused() throws Exception {
        FormDefinitionDTO form = publishedForm("sent_answers");
        FieldDefinitionDTO stored = onlyFieldOf(form);

        FormSubmissionDTO submitted = workflowService.submit(FormSubmissionDTO.builder()
                .formDefinitionId(form.getId())
                .formCode(form.getCode())
                .author("user@example.com")
                .fieldSubmissions(new ArrayList<>(List.of(FieldSubmissionDTO.builder()
                        .fieldDefinitionId(stored.getId())
                        .fieldName(stored.getName())
                        .value("before")
                        .build())))
                .build()).submission();

        Long answerId = submitted.getFieldSubmissions().get(0).getId();

        send(put("/api/v1/field-submissions/{id}", answerId), FieldSubmissionDTO.builder()
                .formSubmissionId(submitted.getId())
                .fieldDefinitionId(stored.getId())
                .fieldName(stored.getName())
                .value("after")
                .build())
                .andExpect(status().isConflict());

        mockMvc.perform(delete("/api/v1/field-submissions/{id}", answerId))
                .andExpect(status().isConflict());
    }

    // --- fixtures ---------------------------------------------------------------------------------

    private ResultActions send(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder
                                       request, Object body) throws Exception {
        return mockMvc.perform(request
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    private FormDefinitionDTO draftForm(String code) throws Exception {
        FormDefinitionDTO form = new FormDefinitionDTO();
        form.setCode(code);
        form.setTitle("Form " + code);
        form.setVersion(1);

        MvcResult result = send(post("/api/v1/form-definitions"), form)
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(
                result.getResponse().getContentAsString(), FormDefinitionDTO.class);
    }

    /** Publishing is not a REST operation, so the service does it — as the builder's button would. */
    private FormDefinitionDTO publishedForm(String code) throws Exception {
        FormDefinitionDTO form = draftForm(code);
        send(post("/api/v1/field-definitions"), field(form.getId(), "nickname", FieldType.TEXT))
                .andExpect(status().isCreated());
        return formDefinitionService.publish(form.getId());
    }

    private FieldDefinitionDTO onlyFieldOf(FormDefinitionDTO form) throws Exception {
        MvcResult result = mockMvc.perform(get(
                        "/api/v1/field-definitions/by-form-definition/{id}", form.getId()))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readValue(
                result.getResponse().getContentAsString(), FieldDefinitionDTO[].class)[0];
    }

    /**
     * A field created inline with its form, so the form's {@code fields} collection is accurate the
     * first time anything in this transaction reads it — unlike a field added by a later, separate
     * request against a form already touched once (Hibernate's first-level cache does not notice a
     * sibling collection changing out from under an entity it has already loaded).
     */
    private FormDefinitionDTO formWithOneField(String code) throws Exception {
        FormDefinitionDTO form = new FormDefinitionDTO();
        form.setCode(code);
        form.setTitle("Form " + code);
        form.setVersion(1);
        form.setFields(List.of(FieldDefinitionDTO.builder()
                .name("name").label("Name").type(FieldType.TEXT).orderIndex(0).build()));

        MvcResult result = send(post("/api/v1/form-definitions"), form)
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readValue(
                result.getResponse().getContentAsString(), FormDefinitionDTO.class);
    }

    private FormLayoutDTO layout(Long formId, Long fieldId) {
        return FormLayoutDTO.builder()
                .formDefinitionId(formId)
                .fieldLayouts(List.of(FieldLayoutDTO.builder()
                        .fieldDefinitionId(fieldId)
                        .row(0).column(0).colspan(12).rowspan(1)
                        .build()))
                .build();
    }

    private FieldDefinitionDTO field(Long formId, String name, FieldType type) {
        return FieldDefinitionDTO.builder()
                .formDefinitionId(formId)
                .name(name)
                .label(name)
                .type(type)
                .orderIndex(0)
                .required(false)
                .build();
    }
}
