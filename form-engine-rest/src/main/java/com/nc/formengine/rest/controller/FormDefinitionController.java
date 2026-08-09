package com.nc.formengine.rest.controller;

import com.nc.formengine.business.service.FormDefinitionService;
import com.nc.formengine.model.dto.FormDefinitionDTO;
import com.nc.formengine.model.exception.FormDefinitionNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/form-definitions")
@RequiredArgsConstructor
@Tag(name = "Form definitions", description = "The forms themselves: code, title and version.")
public class FormDefinitionController {

    private final FormDefinitionService formDefinitionService;

    @PostMapping
    @Operation(summary = "Create a form definition",
            description = "The body must not carry an id. Form codes are unique.")
    @ApiResponse(responseCode = "201", description = "Form created.")
    @ApiResponse(responseCode = "409", description = "A form already uses that code.")
    public ResponseEntity<FormDefinitionDTO> create(@Valid @RequestBody FormDefinitionDTO formDefinitionDTO) {
        FormDefinitionDTO created = formDefinitionService.create(formDefinitionDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace a form definition")
    @ApiResponse(responseCode = "200", description = "Form updated.")
    @ApiResponse(responseCode = "409", description = "Another form already uses that code.")
    public ResponseEntity<FormDefinitionDTO> update(
            @PathVariable("id") Long id,
            @Valid @RequestBody FormDefinitionDTO formDefinitionDTO) {
        FormDefinitionDTO updated = formDefinitionService.update(id, formDefinitionDTO);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Find a form definition by id")
    public ResponseEntity<FormDefinitionDTO> findById(@PathVariable("id") Long id) {
        return formDefinitionService.findById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new FormDefinitionNotFoundException(id));
    }

    @GetMapping("/by-code/{code}")
    @Operation(summary = "Find a form definition by its unique code")
    public ResponseEntity<FormDefinitionDTO> findByCode(@PathVariable String code) {
        return formDefinitionService.findByCode(code)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new FormDefinitionNotFoundException(code));
    }

    @GetMapping
    @Operation(summary = "List every form definition")
    public ResponseEntity<List<FormDefinitionDTO>> findAll() {
        List<FormDefinitionDTO> forms = formDefinitionService.findAll();
        return ResponseEntity.ok(forms);
    }

    @GetMapping("/exists-by-code/{code}")
    @Operation(summary = "Check whether a form code is taken",
            description = "Returns true or false; never 404.")
    public ResponseEntity<Boolean> existsByCode(@PathVariable String code) {
        boolean exists = formDefinitionService.existsByCode(code);
        return ResponseEntity.ok(exists);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a form definition")
    @ApiResponse(responseCode = "204", description = "Form deleted, or never existed.")
    public ResponseEntity<Void> deleteById(@PathVariable("id") Long id) {
        formDefinitionService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
