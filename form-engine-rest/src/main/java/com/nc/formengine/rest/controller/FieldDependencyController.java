package com.nc.formengine.rest.controller;

import com.nc.formengine.business.service.FieldDependencyService;
import com.nc.formengine.model.dto.FieldDependencyDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/field-dependencies")
@RequiredArgsConstructor
public class FieldDependencyController {

    private final FieldDependencyService fieldDependencyService;

    @PostMapping
    public ResponseEntity<FieldDependencyDTO> create(@Valid @RequestBody FieldDependencyDTO fieldDependencyDTO) {
        FieldDependencyDTO created = fieldDependencyService.create(fieldDependencyDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FieldDependencyDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody FieldDependencyDTO fieldDependencyDTO) {
        FieldDependencyDTO updated = fieldDependencyService.update(id, fieldDependencyDTO);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FieldDependencyDTO> findById(@PathVariable Long id) {
        return fieldDependencyService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<FieldDependencyDTO>> findAll() {
        List<FieldDependencyDTO> dependencies = fieldDependencyService.findAll();
        return ResponseEntity.ok(dependencies);
    }

    @GetMapping("/by-trigger-field/{triggerFieldId}")
    public ResponseEntity<List<FieldDependencyDTO>> findByTriggerFieldId(
            @PathVariable Long triggerFieldId) {
        List<FieldDependencyDTO> dependencies = fieldDependencyService.findByTriggerFieldId(triggerFieldId);
        return ResponseEntity.ok(dependencies);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        fieldDependencyService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
