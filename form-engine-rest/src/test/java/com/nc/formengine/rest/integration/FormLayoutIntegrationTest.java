package com.nc.formengine.rest.integration;

import tools.jackson.databind.ObjectMapper;
import com.nc.formengine.model.dto.FieldDefinitionDTO;
import com.nc.formengine.model.dto.FieldLayoutDTO;
import com.nc.formengine.model.dto.FormDefinitionDTO;
import com.nc.formengine.model.dto.FormLayoutDTO;
import com.nc.formengine.model.enums.DeviceType;
import com.nc.formengine.model.enums.FieldType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FormLayoutIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldCreateLayoutAndResolveByDeviceType() throws Exception {
        FormDefinitionDTO form = createTestForm();
        Long formId = form.getId();
        Long field1Id = form.getFields().get(0).getId();
        Long field2Id = form.getFields().get(1).getId();

        FormLayoutDTO mobileLayout = FormLayoutDTO.builder()
                .formDefinitionId(formId)
                .deviceType(DeviceType.MOBILE)
                .fieldLayouts(List.of(
                        FieldLayoutDTO.builder()
                                .fieldDefinitionId(field1Id)
                                .row(0).column(0).colspan(12).rowspan(1)
                                .build(),
                        FieldLayoutDTO.builder()
                                .fieldDefinitionId(field2Id)
                                .row(1).column(0).colspan(12).rowspan(1)
                                .build()
                ))
                .build();

        String layoutJson = objectMapper.writeValueAsString(mobileLayout);

        MvcResult createResult = mockMvc.perform(post("/api/v1/form-layouts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(layoutJson))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.deviceType").value("MOBILE"))
                .andExpect(jsonPath("$.fieldLayouts.length()").value(2))
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        FormLayoutDTO createdLayout = objectMapper.readValue(responseBody, FormLayoutDTO.class);
        
        mockMvc.perform(get("/api/v1/form-layouts/form/" + formId + "/resolve")
                .param("deviceType", "MOBILE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdLayout.getId()))
                .andExpect(jsonPath("$.deviceType").value("MOBILE"));
    }

    @Test
    void shouldFallbackToGenericLayoutWhenDeviceSpecificNotFound() throws Exception {
        FormDefinitionDTO form = createTestForm();
        Long formId = form.getId();
        Long field1Id = form.getFields().get(0).getId();

        FormLayoutDTO genericLayout = FormLayoutDTO.builder()
                .formDefinitionId(formId)
                .deviceType(null)
                .fieldLayouts(List.of(
                        FieldLayoutDTO.builder()
                                .fieldDefinitionId(field1Id)
                                .row(0).column(0).colspan(12).rowspan(1)
                                .build()
                ))
                .build();

        String layoutJson = objectMapper.writeValueAsString(genericLayout);

        mockMvc.perform(post("/api/v1/form-layouts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(layoutJson))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/form-layouts/form/" + formId + "/resolve")
                .param("deviceType", "MOBILE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceType").isEmpty());
    }

    /**
     * A form with no layout resolves to nothing rather than to a synthesised one. Stacking the fields
     * in order is what a consumer does with no layout anyway, so there is nothing to hand back.
     */
    @Test
    void shouldReportNoLayoutWhenNoneExists() throws Exception {
        FormDefinitionDTO form = createTestForm();
        Long formId = form.getId();

        mockMvc.perform(get("/api/v1/form-layouts/form/" + formId + "/resolve")
                .param("deviceType", "MOBILE"))
                .andExpect(status().isNotFound());
    }

    /**
     * With two device types, "closest" is simply the other one: laying a form out for the wrong screen
     * still beats handing back no layout at all.
     */
    @Test
    void shouldFallbackToTheOtherDeviceType() throws Exception {
        FormDefinitionDTO form = createTestForm();
        Long formId = form.getId();
        Long field1Id = form.getFields().get(0).getId();

        FormLayoutDTO desktopLayout = FormLayoutDTO.builder()
                .formDefinitionId(formId)
                .deviceType(DeviceType.DESKTOP)
                .fieldLayouts(List.of(
                        FieldLayoutDTO.builder()
                                .fieldDefinitionId(field1Id)
                                .row(0).column(0).colspan(6).rowspan(1)
                                .build()
                ))
                .build();

        String layoutJson = objectMapper.writeValueAsString(desktopLayout);

        mockMvc.perform(post("/api/v1/form-layouts")
                .contentType(MediaType.APPLICATION_JSON)
                .content(layoutJson))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/form-layouts/form/" + formId + "/resolve")
                .param("deviceType", "MOBILE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.deviceType").value("DESKTOP"));
    }

    private FormDefinitionDTO createTestForm() throws Exception {
        FormDefinitionDTO formToCreate = new FormDefinitionDTO();
        formToCreate.setCode("TEST_FORM_" + System.currentTimeMillis());
        formToCreate.setTitle("Test Form");
        formToCreate.setDescription("A test form for layout testing");
        formToCreate.setVersion(1);
        
        List<FieldDefinitionDTO> fields = new ArrayList<>();
        
        FieldDefinitionDTO field1 = FieldDefinitionDTO.builder()
                .name("name")
                .label("Name")
                .type(FieldType.TEXT)
                .orderIndex(0)
                .required(true)
                .build();
        fields.add(field1);
        
        FieldDefinitionDTO field2 = FieldDefinitionDTO.builder()
                .name("email")
                .label("Email")
                .type(FieldType.TEXT)
                .orderIndex(1)
                .required(true)
                .build();
        fields.add(field2);
        
        formToCreate.setFields(fields);

        String formJson = objectMapper.writeValueAsString(formToCreate);

        MvcResult createResult = mockMvc.perform(post("/api/v1/form-definitions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(formJson))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        return objectMapper.readValue(responseBody, FormDefinitionDTO.class);
    }
}
