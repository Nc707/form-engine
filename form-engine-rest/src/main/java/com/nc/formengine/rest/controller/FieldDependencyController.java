package com.nc.formengine.rest.controller;

import com.nc.formengine.business.service.FieldDependencyService;
import com.nc.formengine.model.dto.FieldDependencyDTO;
import com.nc.formengine.model.exception.FieldDependencyNotFoundException;
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
@RequestMapping("/api/v1/field-dependencies")
@RequiredArgsConstructor
@Tag(name = "Field dependencies",
        description = "Rules that show, hide or require one field based on another field's value.")
public class FieldDependencyController {

    private final FieldDependencyService fieldDependencyService;

    @PostMapping
    @Operation(summary = "Create a field dependency",
            description = "Both the dependent and the trigger field must exist.")
    @ApiResponse(responseCode = "201", description = "Dependency created.")
    public ResponseEntity<FieldDependencyDTO> create(@Valid @RequestBody FieldDependencyDTO fieldDependencyDTO) {
        FieldDependencyDTO created = fieldDependencyService.create(fieldDependencyDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace a field dependency")
    public ResponseEntity<FieldDependencyDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody FieldDependencyDTO fieldDependencyDTO) {
        FieldDependencyDTO updated = fieldDependencyService.update(id, fieldDependencyDTO);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Find a field dependency by id")
    public ResponseEntity<FieldDependencyDTO> findById(@PathVariable Long id) {
        return fieldDependencyService.findById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new FieldDependencyNotFoundException(id));
    }

    @GetMapping
    @Operation(summary = "List every field dependency")
    public ResponseEntity<List<FieldDependencyDTO>> findAll() {
        List<FieldDependencyDTO> dependencies = fieldDependencyService.findAll();
        return ResponseEntity.ok(dependencies);
    }

    @GetMapping("/by-trigger-field/{triggerFieldId}")
    @Operation(summary = "List the dependencies driven by one field",
            description = "Returns an empty list when nothing depends on the field.")
    public ResponseEntity<List<FieldDependencyDTO>> findByTriggerFieldId(
            @PathVariable Long triggerFieldId) {
        List<FieldDependencyDTO> dependencies = fieldDependencyService.findByTriggerFieldId(triggerFieldId);
        return ResponseEntity.ok(dependencies);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a field dependency")
    @ApiResponse(responseCode = "204", description = "Dependency deleted, or never existed.")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        fieldDependencyService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
