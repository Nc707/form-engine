package com.nc.formengine.rest.controller;

import com.nc.formengine.business.service.FormDefinitionService;
import com.nc.formengine.model.dto.FormDefinitionDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/form-definitions")
@RequiredArgsConstructor
public class FormDefinitionController {

    private final FormDefinitionService formDefinitionService;

    @PostMapping
    public ResponseEntity<FormDefinitionDTO> create(@Valid @RequestBody FormDefinitionDTO formDefinitionDTO) {
        FormDefinitionDTO created = formDefinitionService.create(formDefinitionDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FormDefinitionDTO> update(
            @PathVariable("id") Long id,
            @Valid @RequestBody FormDefinitionDTO formDefinitionDTO) {
        FormDefinitionDTO updated = formDefinitionService.update(id, formDefinitionDTO);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FormDefinitionDTO> findById(@PathVariable("id") Long id) {
        return formDefinitionService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-code/{code}")
    public ResponseEntity<FormDefinitionDTO> findByCode(@PathVariable String code) {
        return formDefinitionService.findByCode(code)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<FormDefinitionDTO>> findAll() {
        List<FormDefinitionDTO> forms = formDefinitionService.findAll();
        return ResponseEntity.ok(forms);
    }

    @GetMapping("/exists-by-code/{code}")
    public ResponseEntity<Boolean> existsByCode(@PathVariable String code) {
        boolean exists = formDefinitionService.existsByCode(code);
        return ResponseEntity.ok(exists);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable("id") Long id) {
        formDefinitionService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
