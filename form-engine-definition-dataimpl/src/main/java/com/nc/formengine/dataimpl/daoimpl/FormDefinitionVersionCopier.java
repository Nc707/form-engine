package com.nc.formengine.dataimpl.daoimpl;

import com.nc.formengine.dataimpl.entity.FieldDefinition;
import com.nc.formengine.dataimpl.entity.FieldDependency;
import com.nc.formengine.dataimpl.entity.FieldLayout;
import com.nc.formengine.dataimpl.entity.FieldOption;
import com.nc.formengine.dataimpl.entity.FieldRestriction;
import com.nc.formengine.dataimpl.entity.FormDefinition;
import com.nc.formengine.dataimpl.entity.FormLayout;
import com.nc.formengine.dataimpl.repository.FieldDependencyRepository;
import com.nc.formengine.dataimpl.repository.FormRepository;
import com.nc.formengine.model.enums.FormDefinitionStatus;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Copies a form definition into a fresh version of itself.
 *
 * <p>The copy has to be deep. A new version that shared rows with the old one would not be a new
 * version at all: editing it would edit the published form through the back door, which is the exact
 * thing the lifecycle exists to prevent. So every field, restriction, option, layout and dependency
 * is recreated.
 *
 * <p>The one genuinely awkward part is that dependencies and layouts refer to fields <em>by row</em>.
 * Copied naively they would keep pointing at the original version's fields, and the new version
 * would inherit conditional logic wired to a form the user is no longer editing. Both are therefore
 * translated through a map from source field id to the corresponding new field.
 */
@Component
@RequiredArgsConstructor
class FormDefinitionVersionCopier {

    private final FormRepository formRepository;
    private final FieldDependencyRepository fieldDependencyRepository;

    /**
     * Writes {@code source} back out as a {@link FormDefinitionStatus#DRAFT} at {@code newVersion}.
     *
     * <p>Persistence happens in two passes on purpose. The fields must already hold their generated
     * ids before anything can point at them, so the form and its fields are saved first, and only
     * then are the layouts and dependencies built against the saved instances. Doing it in one pass
     * would leave Hibernate to guess an insert order for rows that reference each other.
     *
     * @return the newly persisted definition
     */
    FormDefinition copy(FormDefinition source, Integer newVersion) {
        FormDefinition copy = new FormDefinition();
        copy.setCode(source.getCode());
        copy.setTitle(source.getTitle());
        copy.setDescription(source.getDescription());
        copy.setVersion(newVersion);
        copy.setStatus(FormDefinitionStatus.DRAFT);

        Map<Long, FieldDefinition> fieldsBySourceId = new HashMap<>();
        List<FieldDefinition> copiedFields = new ArrayList<>();
        for (FieldDefinition sourceField : source.getFields()) {
            FieldDefinition fieldCopy = copyField(sourceField, copy);
            copiedFields.add(fieldCopy);
            fieldsBySourceId.put(sourceField.getId(), fieldCopy);
        }
        copy.setFields(copiedFields);

        // First pass: the form, its fields, and everything the fields own by cascade.
        FormDefinition saved = formRepository.saveAndFlush(copy);

        // Second pass: the rows that reference fields, now that the fields have ids.
        copyLayouts(source, saved, fieldsBySourceId);
        copyDependencies(source, fieldsBySourceId);

        return formRepository.saveAndFlush(saved);
    }

    private FieldDefinition copyField(FieldDefinition source, FormDefinition owner) {
        FieldDefinition copy = new FieldDefinition();
        copy.setFormDefinition(owner);
        copy.setName(source.getName());
        copy.setLabel(source.getLabel());
        copy.setType(source.getType());
        copy.setOrderIndex(source.getOrderIndex());
        copy.setRequired(source.getRequired());

        List<FieldRestriction> restrictions = new ArrayList<>();
        for (FieldRestriction sourceRestriction : source.getRestrictions()) {
            FieldRestriction restrictionCopy = new FieldRestriction();
            restrictionCopy.setFieldDefinition(copy);
            restrictionCopy.setType(sourceRestriction.getType());
            restrictionCopy.setErrorMessage(sourceRestriction.getErrorMessage());
            restrictionCopy.setOrderIndex(sourceRestriction.getOrderIndex());
            // A fresh map: sharing it would make the two versions' parameters the same object.
            restrictionCopy.setParameters(new HashMap<>(sourceRestriction.getParameters()));
            restrictions.add(restrictionCopy);
        }
        copy.setRestrictions(restrictions);

        List<FieldOption> options = new ArrayList<>();
        for (FieldOption sourceOption : source.getOptions()) {
            FieldOption optionCopy = new FieldOption();
            optionCopy.setFieldDefinition(copy);
            optionCopy.setLabel(sourceOption.getLabel());
            optionCopy.setValue(sourceOption.getValue());
            optionCopy.setOrderIndex(sourceOption.getOrderIndex());
            options.add(optionCopy);
        }
        copy.setOptions(options);

        return copy;
    }

    private void copyLayouts(FormDefinition source, FormDefinition target,
                             Map<Long, FieldDefinition> fieldsBySourceId) {
        List<FormLayout> layouts = new ArrayList<>();
        for (FormLayout sourceLayout : source.getLayouts()) {
            FormLayout layoutCopy = new FormLayout();
            layoutCopy.setFormDefinition(target);
            layoutCopy.setDeviceType(sourceLayout.getDeviceType());

            List<FieldLayout> fieldLayouts = new ArrayList<>();
            for (FieldLayout sourceFieldLayout : sourceLayout.getFieldLayouts()) {
                FieldDefinition targetField = resolve(sourceFieldLayout.getFieldDefinition(), fieldsBySourceId);
                if (targetField == null) {
                    // The placed field belongs to another form; there is nothing to place it on here.
                    continue;
                }
                FieldLayout fieldLayoutCopy = new FieldLayout();
                fieldLayoutCopy.setFormLayout(layoutCopy);
                fieldLayoutCopy.setFieldDefinition(targetField);
                fieldLayoutCopy.setRow(sourceFieldLayout.getRow());
                fieldLayoutCopy.setColumn(sourceFieldLayout.getColumn());
                fieldLayoutCopy.setColspan(sourceFieldLayout.getColspan());
                fieldLayoutCopy.setRowspan(sourceFieldLayout.getRowspan());
                fieldLayouts.add(fieldLayoutCopy);
            }
            layoutCopy.setFieldLayouts(fieldLayouts);
            layouts.add(layoutCopy);
        }
        target.getLayouts().addAll(layouts);
    }

    private void copyDependencies(FormDefinition source, Map<Long, FieldDefinition> fieldsBySourceId) {
        List<FieldDependency> copies = new ArrayList<>();
        for (FieldDependency sourceDependency : fieldDependencyRepository.findByFormDefinitionId(source.getId())) {
            FieldDefinition dependent = resolve(sourceDependency.getDependentField(), fieldsBySourceId);
            FieldDefinition trigger = resolve(sourceDependency.getTriggerField(), fieldsBySourceId);
            if (dependent == null || trigger == null) {
                // Half a dependency is worse than none: it would either fail to persist or silently
                // wire the new version to the old one's fields.
                continue;
            }
            FieldDependency copy = new FieldDependency();
            copy.setDependentField(dependent);
            copy.setTriggerField(trigger);
            copy.setCondition(sourceDependency.getCondition());
            copy.setEffect(sourceDependency.getEffect());
            copy.setTriggerValue(sourceDependency.getTriggerValue());
            copies.add(copy);
        }
        fieldDependencyRepository.saveAll(copies);
    }

    private FieldDefinition resolve(FieldDefinition sourceField, Map<Long, FieldDefinition> fieldsBySourceId) {
        return sourceField == null ? null : fieldsBySourceId.get(sourceField.getId());
    }
}
