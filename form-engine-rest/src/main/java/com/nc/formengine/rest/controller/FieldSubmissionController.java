package com.nc.formengine.rest.controller;

import com.nc.formengine.submission.business.service.FieldSubmissionService;
import com.nc.formengine.submission.model.dto.FieldSubmissionDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/field-submissions")
@RequiredArgsConstructor
public class FieldSubmissionController {

    private final FieldSubmissionService fieldSubmissionService;

    @PostMapping
    public ResponseEntity<FieldSubmissionDTO> create(@Valid @RequestBody FieldSubmissionDTO fieldSubmissionDTO) {
        FieldSubmissionDTO created = fieldSubmissionService.create(fieldSubmissionDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FieldSubmissionDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody FieldSubmissionDTO fieldSubmissionDTO) {
        FieldSubmissionDTO updated = fieldSubmissionService.update(id, fieldSubmissionDTO);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FieldSubmissionDTO> findById(@PathVariable Long id) {
        return fieldSubmissionService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<FieldSubmissionDTO>> findAll() {
        List<FieldSubmissionDTO> fieldSubmissions = fieldSubmissionService.findAll();
        return ResponseEntity.ok(fieldSubmissions);
    }

    @GetMapping("/by-form-submission/{formSubmissionId}")
    public ResponseEntity<List<FieldSubmissionDTO>> findByFormSubmissionId(
            @PathVariable Long formSubmissionId) {
        List<FieldSubmissionDTO> fieldSubmissions = fieldSubmissionService.findByFormSubmissionId(formSubmissionId);
        return ResponseEntity.ok(fieldSubmissions);
    }

    @GetMapping("/by-field-definition/{fieldDefinitionId}")
    public ResponseEntity<List<FieldSubmissionDTO>> findByFieldDefinitionId(
            @PathVariable Long fieldDefinitionId) {
        List<FieldSubmissionDTO> fieldSubmissions = fieldSubmissionService.findByFieldDefinitionId(fieldDefinitionId);
        return ResponseEntity.ok(fieldSubmissions);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        fieldSubmissionService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/by-form-submission/{formSubmissionId}")
    public ResponseEntity<Void> deleteByFormSubmissionId(@PathVariable Long formSubmissionId) {
        fieldSubmissionService.deleteByFormSubmissionId(formSubmissionId);
        return ResponseEntity.noContent().build();
    }
}
