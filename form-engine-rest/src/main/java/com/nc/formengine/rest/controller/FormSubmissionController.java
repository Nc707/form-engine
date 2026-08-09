package com.nc.formengine.rest.controller;

import com.nc.formengine.submission.business.service.FormSubmissionService;
import com.nc.formengine.submission.model.dto.FormSubmissionDTO;
import com.nc.formengine.submission.model.exception.FormSubmissionNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Form submissions", description = "Filled-in instances of a form definition.")
public class FormSubmissionController {

    private final FormSubmissionService formSubmissionService;

    @PostMapping
    @Operation(summary = "Create a form submission")
    @ApiResponse(responseCode = "201", description = "Submission created.")
    public ResponseEntity<FormSubmissionDTO> create(@Valid @RequestBody FormSubmissionDTO formSubmissionDTO) {
        FormSubmissionDTO created = formSubmissionService.create(formSubmissionDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace a form submission")
    public ResponseEntity<FormSubmissionDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody FormSubmissionDTO formSubmissionDTO) {
        FormSubmissionDTO updated = formSubmissionService.update(id, formSubmissionDTO);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Find a form submission by id")
    public ResponseEntity<FormSubmissionDTO> findById(@PathVariable Long id) {
        return formSubmissionService.findById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new FormSubmissionNotFoundException(id));
    }

    @GetMapping
    @Operation(summary = "List every form submission")
    public ResponseEntity<List<FormSubmissionDTO>> findAll() {
        List<FormSubmissionDTO> submissions = formSubmissionService.findAll();
        return ResponseEntity.ok(submissions);
    }

    @GetMapping("/paginated")
    @Operation(summary = "List form submissions, one page at a time",
            description = "Accepts the usual page, size and sort query parameters.")
    public ResponseEntity<Page<FormSubmissionDTO>> findAllPaginated(Pageable pageable) {
        Page<FormSubmissionDTO> submissions = formSubmissionService.findAll(pageable);
        return ResponseEntity.ok(submissions);
    }

    @GetMapping("/by-form-definition/{formDefinitionId}")
    @Operation(summary = "List the submissions of one form definition")
    public ResponseEntity<List<FormSubmissionDTO>> findByFormDefinitionId(
            @PathVariable Long formDefinitionId) {
        List<FormSubmissionDTO> submissions = formSubmissionService.findByFormDefinitionId(formDefinitionId);
        return ResponseEntity.ok(submissions);
    }

    @GetMapping("/by-form-code/{formCode}")
    @Operation(summary = "List the submissions recorded against a form code")
    public ResponseEntity<List<FormSubmissionDTO>> findByFormCode(@PathVariable String formCode) {
        List<FormSubmissionDTO> submissions = formSubmissionService.findByFormCode(formCode);
        return ResponseEntity.ok(submissions);
    }

    @GetMapping("/by-submitted-by/{submittedBy}")
    @Operation(summary = "List the submissions of one user")
    public ResponseEntity<List<FormSubmissionDTO>> findBySubmittedBy(@PathVariable String submittedBy) {
        List<FormSubmissionDTO> submissions = formSubmissionService.findBySubmittedBy(submittedBy);
        return ResponseEntity.ok(submissions);
    }

    @GetMapping("/by-status/{status}")
    @Operation(summary = "List the submissions in one status",
            description = "Status must be DRAFT, SUBMITTED or CANCELED.")
    @ApiResponse(responseCode = "400", description = "Unknown status; the body lists the allowed values.")
    public ResponseEntity<List<FormSubmissionDTO>> findByStatus(@PathVariable String status) {
        List<FormSubmissionDTO> submissions = formSubmissionService.findByStatus(status);
        return ResponseEntity.ok(submissions);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a form submission")
    @ApiResponse(responseCode = "204", description = "Submission deleted, or never existed.")
    public ResponseEntity<Void> deleteById(@PathVariable Long id) {
        formSubmissionService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
