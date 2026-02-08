package com.nc.formengine.rest.controller;

import com.nc.formengine.business.service.FieldOptionService;
import com.nc.formengine.model.dto.FieldOptionDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/field-options")
@RequiredArgsConstructor
public class FieldOptionController {

    private final FieldOptionService fieldOptionService;

    @PostMapping
    public ResponseEntity<FieldOptionDTO> create(@Valid @RequestBody FieldOptionDTO fieldOptionDTO) {
        FieldOptionDTO created = fieldOptionService.create(fieldOptionDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FieldOptionDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody FieldOptionDTO fieldOptionDTO) {
        FieldOptionDTO updated = fieldOptionService.update(id, fieldOptionDTO);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FieldOptionDTO> findById(@PathVariable Long id) {
        return fieldOptionService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<FieldOptionDTO>> findAll() {
        List<FieldOptionDTO> options = fieldOptionService.findAll();
        return ResponseEntity.ok(options);
    }

    @GetMapping("/by-field-definition/{fieldDefinitionId}")
    public ResponseEntity<List<FieldOptionDTO>> findByFieldDefinitionId(
            @PathVariable Long fieldDefinitionId) {
        List<FieldOptionDTO> options = fieldOptionService.findByFieldDefinitionId(fieldDefinitionId);
        return ResponseEntity.ok(options);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        fieldOptionService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
