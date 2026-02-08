package com.nc.formengine.rest.controller;

import com.nc.formengine.business.service.FieldDefinitionService;
import com.nc.formengine.model.dto.FieldDefinitionDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/field-definitions")
@RequiredArgsConstructor
public class FieldDefinitionController {

    private final FieldDefinitionService fieldDefinitionService;

    @PostMapping
    public ResponseEntity<FieldDefinitionDTO> create(@Valid @RequestBody FieldDefinitionDTO fieldDefinitionDTO) {
        FieldDefinitionDTO created = fieldDefinitionService.create(fieldDefinitionDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FieldDefinitionDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody FieldDefinitionDTO fieldDefinitionDTO) {
        FieldDefinitionDTO updated = fieldDefinitionService.update(id, fieldDefinitionDTO);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FieldDefinitionDTO> findById(@PathVariable Long id) {
        return fieldDefinitionService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<FieldDefinitionDTO>> findAll() {
        List<FieldDefinitionDTO> fields = fieldDefinitionService.findAll();
        return ResponseEntity.ok(fields);
    }

    @GetMapping("/by-form-definition/{formDefinitionId}")
    public ResponseEntity<List<FieldDefinitionDTO>> findByFormDefinitionId(
            @PathVariable Long formDefinitionId) {
        List<FieldDefinitionDTO> fields = fieldDefinitionService.findByFormDefinitionId(formDefinitionId);
        return ResponseEntity.ok(fields);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        fieldDefinitionService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/by-form-definition/{formDefinitionId}")
    public ResponseEntity<Void> deleteByFormDefinitionId(@PathVariable Long formDefinitionId) {
        fieldDefinitionService.deleteByFormDefinitionId(formDefinitionId);
        return ResponseEntity.noContent().build();
    }
}
