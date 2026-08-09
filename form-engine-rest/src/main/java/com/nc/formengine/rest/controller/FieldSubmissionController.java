package com.nc.formengine.rest.controller;

import com.nc.formengine.submission.business.service.FieldSubmissionService;
import com.nc.formengine.submission.model.dto.FieldSubmissionDTO;
import com.nc.formengine.submission.model.exception.FieldSubmissionNotFoundException;
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
@RequestMapping("/api/v1/field-submissions")
@RequiredArgsConstructor
@Tag(name = "Field submissions", description = "The individual answers inside a form submission.")
public class FieldSubmissionController {

    private final FieldSubmissionService fieldSubmissionService;

    @PostMapping
    @Operation(summary = "Create a field submission",
            description = "The referenced form submission must exist. fieldName is filled in "
                    + "server-side from the field definition.")
    @ApiResponse(responseCode = "201", description = "Answer recorded.")
    public ResponseEntity<FieldSubmissionDTO> create(@Valid @RequestBody FieldSubmissionDTO fieldSubmissionDTO) {
        FieldSubmissionDTO created = fieldSubmissionService.create(fieldSubmissionDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace a field submission")
    public ResponseEntity<FieldSubmissionDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody FieldSubmissionDTO fieldSubmissionDTO) {
        FieldSubmissionDTO updated = fieldSubmissionService.update(id, fieldSubmissionDTO);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Find a field submission by id")
    public ResponseEntity<FieldSubmissionDTO> findById(@PathVariable Long id) {
        return fieldSubmissionService.findById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new FieldSubmissionNotFoundException(id));
    }

    @GetMapping
    @Operation(summary = "List every field submission")
    public ResponseEntity<List<FieldSubmissionDTO>> findAll() {
        List<FieldSubmissionDTO> fieldSubmissions = fieldSubmissionService.findAll();
        return ResponseEntity.ok(fieldSubmissions);
    }

    @GetMapping("/by-form-submission/{formSubmissionId}")
    @Operation(summary = "List the answers of one form submission")
    public ResponseEntity<List<FieldSubmissionDTO>> findByFormSubmissionId(
            @PathVariable Long formSubmissionId) {
        List<FieldSubmissionDTO> fieldSubmissions = fieldSubmissionService.findByFormSubmissionId(formSubmissionId);
        return ResponseEntity.ok(fieldSubmissions);
    }

    @GetMapping("/by-field-definition/{fieldDefinitionId}")
    @Operation(summary = "List every answer given to one field, across submissions")
    public ResponseEntity<List<FieldSubmissionDTO>> findByFieldDefinitionId(
            @PathVariable Long fieldDefinitionId) {
        List<FieldSubmissionDTO> fieldSubmissions = fieldSubmissionService.findByFieldDefinitionId(fieldDefinitionId);
        return ResponseEntity.ok(fieldSubmissions);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a field submission")
    @ApiResponse(responseCode = "204", description = "Answer deleted, or never existed.")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        fieldSubmissionService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/by-form-submission/{formSubmissionId}")
    @Operation(summary = "Delete every answer of one form submission")
    @ApiResponse(responseCode = "204", description = "Answers deleted.")
    public ResponseEntity<Void> deleteByFormSubmissionId(@PathVariable Long formSubmissionId) {
        fieldSubmissionService.deleteByFormSubmissionId(formSubmissionId);
        return ResponseEntity.noContent().build();
    }
}
