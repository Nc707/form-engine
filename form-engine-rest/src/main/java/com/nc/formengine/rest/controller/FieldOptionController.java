package com.nc.formengine.rest.controller;

import com.nc.formengine.business.service.FieldOptionService;
import com.nc.formengine.model.dto.FieldOptionDTO;
import com.nc.formengine.model.exception.FieldOptionNotFoundException;
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
@RequestMapping("/api/v1/field-options")
@RequiredArgsConstructor
@Tag(name = "Field options", description = "The selectable options of SELECT and MULTI_SELECT fields.")
public class FieldOptionController {

    private final FieldOptionService fieldOptionService;

    @PostMapping
    @Operation(summary = "Create a field option",
            description = "The referenced field definition must exist.")
    @ApiResponse(responseCode = "201", description = "Option created.")
    public ResponseEntity<FieldOptionDTO> create(@Valid @RequestBody FieldOptionDTO fieldOptionDTO) {
        FieldOptionDTO created = fieldOptionService.create(fieldOptionDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace a field option")
    public ResponseEntity<FieldOptionDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody FieldOptionDTO fieldOptionDTO) {
        FieldOptionDTO updated = fieldOptionService.update(id, fieldOptionDTO);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Find a field option by id")
    public ResponseEntity<FieldOptionDTO> findById(@PathVariable Long id) {
        return fieldOptionService.findById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new FieldOptionNotFoundException(id));
    }

    @GetMapping
    @Operation(summary = "List every field option")
    public ResponseEntity<List<FieldOptionDTO>> findAll() {
        List<FieldOptionDTO> options = fieldOptionService.findAll();
        return ResponseEntity.ok(options);
    }

    @GetMapping("/by-field-definition/{fieldDefinitionId}")
    @Operation(summary = "List the options of one field",
            description = "Returns an empty list when the field has no options or does not exist.")
    public ResponseEntity<List<FieldOptionDTO>> findByFieldDefinitionId(
            @PathVariable Long fieldDefinitionId) {
        List<FieldOptionDTO> options = fieldOptionService.findByFieldDefinitionId(fieldDefinitionId);
        return ResponseEntity.ok(options);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a field option")
    @ApiResponse(responseCode = "204", description = "Option deleted, or never existed.")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        fieldOptionService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
