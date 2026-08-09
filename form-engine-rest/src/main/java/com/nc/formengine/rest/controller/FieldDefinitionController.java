package com.nc.formengine.rest.controller;

import com.nc.formengine.business.service.FieldDefinitionService;
import com.nc.formengine.model.dto.FieldDefinitionDTO;
import com.nc.formengine.model.exception.FieldDefinitionNotFoundException;
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
@RequestMapping("/api/v1/field-definitions")
@RequiredArgsConstructor
@Tag(name = "Field definitions", description = "The fields belonging to a form definition.")
public class FieldDefinitionController {

    private final FieldDefinitionService fieldDefinitionService;

    @PostMapping
    @Operation(summary = "Create a field definition",
            description = "The referenced form definition must exist.")
    @ApiResponse(responseCode = "201", description = "Field created.")
    public ResponseEntity<FieldDefinitionDTO> create(@Valid @RequestBody FieldDefinitionDTO fieldDefinitionDTO) {
        FieldDefinitionDTO created = fieldDefinitionService.create(fieldDefinitionDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace a field definition")
    public ResponseEntity<FieldDefinitionDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody FieldDefinitionDTO fieldDefinitionDTO) {
        FieldDefinitionDTO updated = fieldDefinitionService.update(id, fieldDefinitionDTO);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Find a field definition by id")
    public ResponseEntity<FieldDefinitionDTO> findById(@PathVariable Long id) {
        return fieldDefinitionService.findById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new FieldDefinitionNotFoundException(id));
    }

    @GetMapping
    @Operation(summary = "List every field definition")
    public ResponseEntity<List<FieldDefinitionDTO>> findAll() {
        List<FieldDefinitionDTO> fields = fieldDefinitionService.findAll();
        return ResponseEntity.ok(fields);
    }

    @GetMapping("/by-form-definition/{formDefinitionId}")
    @Operation(summary = "List the fields of one form",
            description = "Returns an empty list when the form has no fields or does not exist.")
    public ResponseEntity<List<FieldDefinitionDTO>> findByFormDefinitionId(
            @PathVariable Long formDefinitionId) {
        List<FieldDefinitionDTO> fields = fieldDefinitionService.findByFormDefinitionId(formDefinitionId);
        return ResponseEntity.ok(fields);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a field definition")
    @ApiResponse(responseCode = "204", description = "Field deleted, or never existed.")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        fieldDefinitionService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/by-form-definition/{formDefinitionId}")
    @Operation(summary = "Delete every field of one form")
    @ApiResponse(responseCode = "204", description = "Fields deleted.")
    public ResponseEntity<Void> deleteByFormDefinitionId(@PathVariable Long formDefinitionId) {
        fieldDefinitionService.deleteByFormDefinitionId(formDefinitionId);
        return ResponseEntity.noContent().build();
    }
}
