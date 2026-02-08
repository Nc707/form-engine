package com.nc.formengine.rest.controller;

import com.nc.formengine.business.service.FormLayoutService;
import com.nc.formengine.business.service.LayoutResolutionService;
import com.nc.formengine.model.dto.FormLayoutDTO;
import com.nc.formengine.model.enums.DeviceType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/form-layouts")
@RequiredArgsConstructor
public class FormLayoutController {

    private final FormLayoutService formLayoutService;
    private final LayoutResolutionService layoutResolutionService;

    @PostMapping
    public ResponseEntity<FormLayoutDTO> createLayout(@Valid @RequestBody FormLayoutDTO layoutDTO) {
        FormLayoutDTO created = formLayoutService.createLayout(layoutDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<FormLayoutDTO> updateLayout(
            @PathVariable("id") Long id,
            @Valid @RequestBody FormLayoutDTO layoutDTO) {
        FormLayoutDTO updated = formLayoutService.updateLayout(id, layoutDTO);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FormLayoutDTO> getLayoutById(@PathVariable("id") Long id) {
        return formLayoutService.getLayoutById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/form/{formDefinitionId}")
    public ResponseEntity<List<FormLayoutDTO>> getLayoutsByForm(
            @PathVariable("formDefinitionId") Long formDefinitionId) {
        List<FormLayoutDTO> layouts = formLayoutService.getLayoutsByFormDefinition(formDefinitionId);
        return ResponseEntity.ok(layouts);
    }

    @GetMapping("/form/{formDefinitionId}/device")
    public ResponseEntity<FormLayoutDTO> getLayoutByFormAndDevice(
            @PathVariable("formDefinitionId") Long formDefinitionId,
            @RequestParam(value = "deviceType", required = false) DeviceType deviceType) {
        return formLayoutService.getLayoutByFormAndDevice(formDefinitionId, deviceType)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/form/{formDefinitionId}/resolve")
    public ResponseEntity<FormLayoutDTO> resolveLayout(
            @PathVariable("formDefinitionId") Long formDefinitionId,
            @RequestParam(value = "deviceType", required = false) DeviceType deviceType) {
        return layoutResolutionService.resolveLayout(formDefinitionId, deviceType)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/form/{formDefinitionId}/resolve-or-default")
    public ResponseEntity<FormLayoutDTO> resolveLayoutOrDefault(
            @PathVariable("formDefinitionId") Long formDefinitionId,
            @RequestParam(value = "deviceType", required = false) DeviceType deviceType) {
        FormLayoutDTO layout = layoutResolutionService.resolveLayoutOrDefault(formDefinitionId, deviceType);
        return ResponseEntity.ok(layout);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLayout(@PathVariable("id") Long id) {
        formLayoutService.deleteLayout(id);
        return ResponseEntity.noContent().build();
    }
}
