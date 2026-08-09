package com.nc.formengine.rest.controller;

import com.nc.formengine.business.service.FormLayoutService;
import com.nc.formengine.business.service.LayoutResolutionService;
import com.nc.formengine.model.dto.FormLayoutDTO;
import com.nc.formengine.model.enums.DeviceType;
import com.nc.formengine.model.exception.FormLayoutNotFoundException;
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
@RequestMapping("/api/v1/form-layouts")
@RequiredArgsConstructor
@Tag(name = "Form layouts",
        description = "Per-device grid layouts for a form, with fallback to the generic layout.")
public class FormLayoutController {

    private final FormLayoutService formLayoutService;
    private final LayoutResolutionService layoutResolutionService;

    @PostMapping
    @Operation(summary = "Create a layout",
            description = "A null deviceType creates the generic layout other devices fall back to. "
                    + "Every referenced field must belong to the same form.")
    @ApiResponse(responseCode = "201", description = "Layout created.")
    @ApiResponse(responseCode = "422", description = "The layout references fields of another form.")
    public ResponseEntity<FormLayoutDTO> createLayout(@Valid @RequestBody FormLayoutDTO layoutDTO) {
        FormLayoutDTO created = formLayoutService.createLayout(layoutDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace a layout")
    @ApiResponse(responseCode = "422", description = "The layout references fields of another form.")
    public ResponseEntity<FormLayoutDTO> updateLayout(
            @PathVariable("id") Long id,
            @Valid @RequestBody FormLayoutDTO layoutDTO) {
        FormLayoutDTO updated = formLayoutService.updateLayout(id, layoutDTO);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Find a layout by id")
    public ResponseEntity<FormLayoutDTO> getLayoutById(@PathVariable("id") Long id) {
        return formLayoutService.getLayoutById(id)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new FormLayoutNotFoundException(id));
    }

    @GetMapping("/form/{formDefinitionId}")
    @Operation(summary = "List every layout of one form")
    public ResponseEntity<List<FormLayoutDTO>> getLayoutsByForm(
            @PathVariable("formDefinitionId") Long formDefinitionId) {
        List<FormLayoutDTO> layouts = formLayoutService.getLayoutsByFormDefinition(formDefinitionId);
        return ResponseEntity.ok(layouts);
    }

    @GetMapping("/form/{formDefinitionId}/device")
    @Operation(summary = "Get the layout stored for one device",
            description = "Exact match only, no fallback. Omit deviceType for the generic layout.")
    @ApiResponse(responseCode = "404", description = "No layout is stored for that form and device.")
    public ResponseEntity<FormLayoutDTO> getLayoutByFormAndDevice(
            @PathVariable("formDefinitionId") Long formDefinitionId,
            @RequestParam(value = "deviceType", required = false) DeviceType deviceType) {
        return formLayoutService.getLayoutByFormAndDevice(formDefinitionId, deviceType)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new FormLayoutNotFoundException(formDefinitionId, deviceType));
    }

    @GetMapping("/form/{formDefinitionId}/resolve")
    @Operation(summary = "Resolve the best layout for a device",
            description = "Falls back to the closest device type, then to the generic layout.")
    @ApiResponse(responseCode = "404", description = "The form has no layout to fall back to.")
    public ResponseEntity<FormLayoutDTO> resolveLayout(
            @PathVariable("formDefinitionId") Long formDefinitionId,
            @RequestParam(value = "deviceType", required = false) DeviceType deviceType) {
        return layoutResolutionService.resolveLayout(formDefinitionId, deviceType)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new FormLayoutNotFoundException(formDefinitionId, deviceType));
    }

    @GetMapping("/form/{formDefinitionId}/resolve-or-default")
    @Operation(summary = "Resolve a layout, generating one if none exists",
            description = "Same fallback chain as /resolve, but synthesises a single-column layout "
                    + "from the form's fields instead of returning 404.")
    public ResponseEntity<FormLayoutDTO> resolveLayoutOrDefault(
            @PathVariable("formDefinitionId") Long formDefinitionId,
            @RequestParam(value = "deviceType", required = false) DeviceType deviceType) {
        FormLayoutDTO layout = layoutResolutionService.resolveLayoutOrDefault(formDefinitionId, deviceType);
        return ResponseEntity.ok(layout);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a layout")
    @ApiResponse(responseCode = "204", description = "Layout deleted, or never existed.")
    public ResponseEntity<Void> deleteLayout(@PathVariable("id") Long id) {
        formLayoutService.deleteLayout(id);
        return ResponseEntity.noContent().build();
    }
}
