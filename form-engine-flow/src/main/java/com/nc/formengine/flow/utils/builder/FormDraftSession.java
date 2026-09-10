package com.nc.formengine.flow;

import com.nc.formengine.business.service.FieldDependencyService;
import com.nc.formengine.business.service.FieldDefinitionService;
import com.nc.formengine.business.service.FormDefinitionService;
import com.nc.formengine.business.service.FormLayoutService;
import com.nc.formengine.model.dto.FieldDefinitionDTO;
import com.nc.formengine.model.dto.FieldDependencyDTO;
import com.nc.formengine.model.dto.FieldLayoutDTO;
import com.nc.formengine.model.dto.FormDefinitionDTO;
import com.nc.formengine.model.dto.FormLayoutDTO;
import com.nc.formengine.model.exception.FormDefinitionNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * One form being edited, and every write the builder is allowed to make to it.
 *
 * <p>This is the only class in the package that holds a service, which makes "have we forgotten a
 * permission check anywhere?" a question one search can answer. Every mutator starts with {@link
 * #requireEditable()} and ends with {@link #reload()}, so the editor is never showing anything but
 * what is actually stored.
 *
 * <p><b>Why there is no single Save.</b> {@code FormDefinitionService.update} persists the form's
 * own columns and nothing else — {@code FormDefinitionMapper.updateEntity} never looks at {@code
 * fields}. An editor that assembled a whole {@code FormDefinitionDTO} and saved it once would drop
 * every field, restriction and option change on the floor and return a DTO that looked correct.
 * Fields go through {@code FieldDefinitionService} instead, whose DAO reads the stored entity and
 * applies the DTO onto it, reconciling restrictions and options by id in a single transaction. So
 * the unit of work here is one field, not one form, and the persisted draft is the working copy.
 *
 * <p>The other half of that bargain is that every field on screen has an id, always — which is what
 * makes the dependency editor possible, since a dependency is a separate aggregate that references
 * fields by id and can never be part of a form's own save.
 */
final class FormDraftSession {

    private final FormDefinitionService formService;
    private final FieldDefinitionService fieldService;
    private final FieldDependencyService dependencyService;
    private final FormLayoutService layoutService;

    private final Long formId;

    private FormDefinitionDTO form;
    private List<FieldDefinitionDTO> fields = List.of();
    private List<FieldDependencyDTO> dependencies = List.of();
    private FormActions actions = FormActions.of(null, 0);

    /**
     * Opens the form for editing.
     *
     * @throws FormDefinitionNotFoundException when there is no such form
     */
    FormDraftSession(FormDefinitionService formService,
                     FieldDefinitionService fieldService,
                     FieldDependencyService dependencyService,
                     FormLayoutService layoutService,
                     Long formId) {
        this.formService = formService;
        this.fieldService = fieldService;
        this.dependencyService = dependencyService;
        this.layoutService = layoutService;
        this.formId = formId;
        reload();
    }

    // --- reads -------------------------------------------------------------------------------

    FormDefinitionDTO form() {
        return form;
    }

    /** The form's fields in the order they render, never null. */
    List<FieldDefinitionDTO> fields() {
        return fields;
    }

    List<FieldDependencyDTO> dependencies() {
        return dependencies;
    }

    FormActions actions() {
        return actions;
    }

    Map<Long, FieldDefinitionDTO> fieldsById() {
        Map<Long, FieldDefinitionDTO> index = new LinkedHashMap<>();
        for (FieldDefinitionDTO field : fields) {
            index.put(field.getId(), field);
        }
        return index;
    }

    /** Re-reads everything from the services, so a failed write cannot leave a stale screen. */
    void reload() {
        form = formService.findById(formId)
                .orElseThrow(() -> new FormDefinitionNotFoundException(formId));

        List<FieldDefinitionDTO> loaded = form.getFields() == null
                ? new ArrayList<>()
                : new ArrayList<>(form.getFields());
        loaded.removeIf(Objects::isNull);
        loaded.sort(OrderIndexes.byOrder());
        fields = loaded;

        dependencies = loadDependencies();
        actions = FormActions.of(form.getStatus(), fields.size());
    }

    /**
     * Every dependency of this form.
     *
     * <p>{@code FieldDependencyService} exposes no "by form" lookup — only the DAO has one — so the
     * dependencies are gathered per trigger field instead. That is complete rather than approximate:
     * the repository only ever links two fields of the same form, and the version copier preserves
     * that, so a dependency of this form necessarily has its trigger among this form's fields.
     *
     * <p>Isolated here on purpose. If the business layer ever grows {@code findByFormDefinitionId},
     * this method is the only thing that has to change.
     */
    private List<FieldDependencyDTO> loadDependencies() {
        Map<Long, FieldDependencyDTO> byId = new LinkedHashMap<>();
        for (FieldDefinitionDTO field : fields) {
            if (field.getId() == null) {
                continue;
            }
            for (FieldDependencyDTO dependency : dependencyService.findByTriggerFieldId(field.getId())) {
                if (dependency != null && dependency.getId() != null) {
                    byId.put(dependency.getId(), dependency);
                }
            }
        }
        return new ArrayList<>(byId.values());
    }

    // --- the form itself ---------------------------------------------------------------------

    /**
     * Saves the form's own details.
     *
     * <p>Title and description are all that is sent, because they are all the service would store.
     * The code is deliberately absent from the editor: {@code update} replaces whatever it is given
     * with the stored code, since renaming one would orphan the code's other versions.
     */
    void saveDetails(String title, String description) {
        requireEditable();
        formService.update(formId, FormDefinitionDTO.builder()
                .id(formId)
                .code(form.getCode())
                .title(title)
                .description(description)
                .version(form.getVersion())
                .status(form.getStatus())
                .build());
        reload();
    }

    void publish() {
        if (!actions.publishable()) {
            throw new IllegalStateException(actions.publishBlockedReason());
        }
        formService.publish(formId);
        reload();
    }

    /**
     * Branches a fresh draft off this version.
     *
     * @return the id of the new draft, to navigate to
     */
    Long createNewVersion() {
        if (!actions.versionable()) {
            throw new IllegalStateException("Only a published or archived form can be versioned.");
        }
        return formService.createNewVersion(formId).getId();
    }

    void deleteForm() {
        if (!actions.deletable()) {
            throw new IllegalStateException(actions.reason());
        }
        formService.deleteById(formId);
    }

    // --- fields ------------------------------------------------------------------------------

    /** Adds a field at the end of the form. */
    void addField(FieldDefinitionDTO field) {
        requireEditable();
        field.setId(null);
        field.setFormDefinitionId(formId);
        field.setOrderIndex(fields.size());
        // Explicitly empty rather than absent: on a new field the two mean the same thing, and being
        // explicit keeps the DTO shaped like the one an update sends.
        field.setRestrictions(field.getRestrictions() == null
                ? new ArrayList<>() : field.getRestrictions());
        field.setOptions(field.getOptions() == null
                ? new ArrayList<>() : field.getOptions());
        fieldService.create(field);
        reload();
    }

    /**
     * Saves an existing field together with its rules and options.
     *
     * <p>Both collections must be non-null here even when empty. The mapper reads null as "this
     * request says nothing about them" and keeps what is stored, so a field whose last rule was just
     * removed would keep it.
     */
    void saveField(FieldDefinitionDTO field) {
        requireEditable();
        field.setFormDefinitionId(formId);
        field.setRestrictions(field.getRestrictions() == null
                ? new ArrayList<>() : field.getRestrictions());
        field.setOptions(field.getOptions() == null
                ? new ArrayList<>() : field.getOptions());
        fieldService.update(field.getId(), field);
        reload();
    }

    /** Moves a field one place up ({@code delta} -1) or down (+1). */
    void moveField(Long fieldId, int delta) {
        requireEditable();
        List<FieldDefinitionDTO> reordered = new ArrayList<>(fields);
        int index = indexOf(reordered, fieldId);
        if (index < 0 || !OrderIndexes.swap(reordered, index, delta)) {
            return;
        }
        persistOrder(reordered);
        reload();
    }

    /**
     * Removes a field, taking its placement in any layout with it.
     *
     * <p>A field a layout places cannot simply be deleted: {@code field_layouts.field_definition_id}
     * is {@code NOT NULL} and {@code FieldDefinition} has no inverse collection, so nothing cascades
     * and the delete fails at flush. This is not a corner case — the version copier copies layouts,
     * so any draft branched off a laid-out form hits it on its first deleted field.
     *
     * <p>Writing to another view's layouts is not this session's business by rights, so it is kept to
     * this one method for whoever owns layout editing to replace with a proper hook. The
     * {@code DataIntegrityViolationException} catch stays regardless, as the backstop for a
     * placement written between the detach and the delete.
     */
    void deleteField(Long fieldId) {
        requireEditable();
        detachFromLayouts(fieldId);
        try {
            fieldService.deleteById(fieldId);
        } catch (DataIntegrityViolationException ex) {
            throw new IllegalStateException(
                    "This field is used elsewhere and could not be removed. "
                            + "Take it out of the form's layout first.", ex);
        }
        reload();
        // The hole the removal left would otherwise stay in the numbering forever.
        if (persistOrder(new ArrayList<>(fields))) {
            reload();
        }
    }

    private void detachFromLayouts(Long fieldId) {
        for (FormLayoutDTO layout : layoutService.getLayoutsByFormDefinition(formId)) {
            List<FieldLayoutDTO> placements = layout.getFieldLayouts();
            if (placements == null || placements.isEmpty()) {
                continue;
            }
            List<FieldLayoutDTO> kept = new ArrayList<>(placements);
            boolean removed = kept.removeIf(placement ->
                    placement != null && fieldId.equals(placement.getFieldDefinitionId()));
            if (removed) {
                layout.setFieldLayouts(kept);
                layoutService.updateLayout(layout.getId(), layout);
            }
        }
    }

    /**
     * Writes 0..n-1 onto the fields whose position actually changed.
     *
     * <p>Only the moved ones are sent, and each as a patch that speaks for neither collection, so
     * reordering a form can never disturb a rule or an option.
     *
     * @return whether anything needed saving
     */
    private boolean persistOrder(List<FieldDefinitionDTO> ordered) {
        boolean changed = false;
        for (int index = 0; index < ordered.size(); index++) {
            FieldDefinitionDTO field = ordered.get(index);
            if (field.getOrderIndex() != null && field.getOrderIndex() == index) {
                continue;
            }
            fieldService.update(field.getId(), OrderIndexes.reorderPatch(field, index));
            changed = true;
        }
        return changed;
    }

    private static int indexOf(List<FieldDefinitionDTO> list, Long fieldId) {
        for (int index = 0; index < list.size(); index++) {
            if (Objects.equals(list.get(index).getId(), fieldId)) {
                return index;
            }
        }
        return -1;
    }

    // --- dependencies ------------------------------------------------------------------------

    void addDependency(FieldDependencyDTO dependency) {
        requireEditable();
        dependency.setId(null);
        dependencyService.create(dependency);
        reload();
    }

    void saveDependency(FieldDependencyDTO dependency) {
        requireEditable();
        dependencyService.update(dependency.getId(), dependency);
        reload();
    }

    void deleteDependency(Long dependencyId) {
        requireEditable();
        dependencyService.deleteById(dependencyId);
        reload();
    }

    // --- the guard ---------------------------------------------------------------------------

    /**
     * Refuses every write to a form that is no longer a draft.
     *
     * <p>The views disable their controls too, but this is the check that counts. A dialog attaches
     * to the UI root rather than to the pane that was disabled, so it would not inherit that state —
     * and neither {@code FieldDefinitionService} nor {@code FieldDependencyService} looks at the
     * parent form's status, so both would carry out the write quite happily.
     */
    private void requireEditable() {
        if (!actions.editable()) {
            throw new IllegalStateException(actions.reason());
        }
    }
}
