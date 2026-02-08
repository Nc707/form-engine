package com.nc.formengine.rest.controller;

import com.nc.formengine.submission.business.service.FormSubmissionService;
import com.nc.formengine.submission.model.dto.FormSubmissionDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/form-submissions")
@RequiredArgsConstructor
public class FormSubmissionController {

    private final FormSubmissionService formSubmissionService;

    @PostMapping
    public ResponseEntity<FormSubmissionDTO> create(@Valid @RequestBody FormSubmissionDTO formSubmissionDTO) {
        FormSubmissionDTO created = formSubmissionService.create(formSubmissionDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FormSubmissionDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody FormSubmissionDTO formSubmissionDTO) {
        FormSubmissionDTO updated = formSubmissionService.update(id, formSubmissionDTO);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FormSubmissionDTO> findById(@PathVariable Long id) {
        return formSubmissionService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<FormSubmissionDTO>> findAll() {
        List<FormSubmissionDTO> submissions = formSubmissionService.findAll();
        return ResponseEntity.ok(submissions);
    }

    @GetMapping("/paginated")
    public ResponseEntity<Page<FormSubmissionDTO>> findAllPaginated(Pageable pageable) {
        Page<FormSubmissionDTO> submissions = formSubmissionService.findAll(pageable);
        return ResponseEntity.ok(submissions);
    }

    @GetMapping("/by-form-definition/{formDefinitionId}")
    public ResponseEntity<List<FormSubmissionDTO>> findByFormDefinitionId(
            @PathVariable Long formDefinitionId) {
        List<FormSubmissionDTO> submissions = formSubmissionService.findByFormDefinitionId(formDefinitionId);
        return ResponseEntity.ok(submissions);
    }

    @GetMapping("/by-form-code/{formCode}")
    public ResponseEntity<List<FormSubmissionDTO>> findByFormCode(@PathVariable String formCode) {
        List<FormSubmissionDTO> submissions = formSubmissionService.findByFormCode(formCode);
        return ResponseEntity.ok(submissions);
    }

    @GetMapping("/by-submitted-by/{submittedBy}")
    public ResponseEntity<List<FormSubmissionDTO>> findBySubmittedBy(@PathVariable String submittedBy) {
        List<FormSubmissionDTO> submissions = formSubmissionService.findBySubmittedBy(submittedBy);
        return ResponseEntity.ok(submissions);
    }

    @GetMapping("/by-status/{status}")
    public ResponseEntity<List<FormSubmissionDTO>> findByStatus(@PathVariable String status) {
        List<FormSubmissionDTO> submissions = formSubmissionService.findByStatus(status);
        return ResponseEntity.ok(submissions);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        formSubmissionService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
