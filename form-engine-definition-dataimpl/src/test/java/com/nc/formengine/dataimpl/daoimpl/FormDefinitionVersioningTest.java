package com.nc.formengine.dataimpl.daoimpl;

import com.nc.formengine.data.dao.FormDao;
import com.nc.formengine.dataimpl.PersistenceTestConfiguration;
import com.nc.formengine.dataimpl.entity.FieldDefinition;
import com.nc.formengine.dataimpl.entity.FieldDependency;
import com.nc.formengine.dataimpl.entity.FieldLayout;
import com.nc.formengine.dataimpl.entity.FieldOption;
import com.nc.formengine.dataimpl.entity.FieldRestriction;
import com.nc.formengine.dataimpl.entity.FormDefinition;
import com.nc.formengine.dataimpl.entity.FormLayout;
import com.nc.formengine.dataimpl.mapper.FieldDefinitionMapper;
import com.nc.formengine.dataimpl.mapper.FieldOptionMapper;
import com.nc.formengine.dataimpl.mapper.FieldRestrictionMapper;
import com.nc.formengine.dataimpl.mapper.FieldLayoutMapper;
import com.nc.formengine.dataimpl.mapper.FormDefinitionMapper;
import com.nc.formengine.dataimpl.mapper.FormLayoutMapper;
import com.nc.formengine.dataimpl.repository.FieldDependencyRepository;
import com.nc.formengine.dataimpl.repository.FormRepository;
import com.nc.formengine.model.dto.FormDefinitionDTO;
import com.nc.formengine.model.enums.DependencyCondition;
import com.nc.formengine.model.enums.DependencyEffect;
import com.nc.formengine.model.enums.DeviceType;
import com.nc.formengine.model.enums.FieldType;
import com.nc.formengine.model.enums.FormDefinitionStatus;
import com.nc.formengine.model.enums.RestrictionType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * What survives, and what must not be shared, when a form definition is versioned.
 *
 * <p>The interesting failure mode is not losing data but silently sharing it: a new version that
 * still points at the old version's fields would let an edit to a draft change a published form.
 * These tests pin down that every row is genuinely new and that the references between them were
 * repointed.
 */
@SpringBootTest(classes = PersistenceTestConfiguration.class)
@Import({FormDaoImpl.class, FormDefinitionVersionCopier.class, FormDefinitionMapper.class,
        FieldDefinitionMapper.class, FieldRestrictionMapper.class, FieldOptionMapper.class,
        FormLayoutMapper.class, FieldLayoutMapper.class})
@Transactional
class FormDefinitionVersioningTest {

    @Autowired
    private FormDao dao;

    @Autowired
    private FormRepository formRepository;

    @Autowired
    private FieldDependencyRepository fieldDependencyRepository;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void twoVersionsMayShareACode() {
        formRepository.save(form("SHARED", 1));
        formRepository.save(form("SHARED", 2));
        entityManager.flush();

        assertThat(formRepository.findByCodeOrderByVersionAsc("SHARED"))
                .extracting(FormDefinition::getVersion)
                .containsExactly(1, 2);
    }

    /**
     * Runs outside the test transaction on purpose. A constraint violation leaves the Hibernate
     * session unusable, so raising one inside the shared transaction breaks the rollback at teardown
     * rather than the assertion here. Each save gets its own transaction instead, and the surviving
     * row is cleaned up by hand.
     */
    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void theSameVersionOfACodeMayNotBeStoredTwice() {
        Long firstId = formRepository.save(form("DUPLICATE", 1)).getId();
        try {
            assertThatThrownBy(() -> formRepository.save(form("DUPLICATE", 1)))
                    .isInstanceOf(DataIntegrityViolationException.class);
        } finally {
            formRepository.deleteById(firstId);
        }
    }

    @Test
    void aNewVersionCopiesTheFieldsRatherThanSharingThem() {
        Long sourceId = seedFormWithOneField("COPY_FIELDS");

        FormDefinitionDTO copy = dao.copyAsNewVersion(sourceId).orElseThrow();
        entityManager.flush();
        entityManager.clear();

        FormDefinition source = formRepository.findById(sourceId).orElseThrow();
        FormDefinition copied = formRepository.findById(copy.getId()).orElseThrow();

        assertThat(copied.getVersion()).isEqualTo(2);
        assertThat(copied.getStatus()).isEqualTo(FormDefinitionStatus.DRAFT);
        assertThat(copied.getCode()).isEqualTo(source.getCode());

        FieldDefinition sourceField = source.getFields().get(0);
        FieldDefinition copiedField = copied.getFields().get(0);
        assertThat(copiedField.getId()).isNotEqualTo(sourceField.getId());
        assertThat(copiedField.getName()).isEqualTo(sourceField.getName());
        assertThat(copiedField.getLabel()).isEqualTo(sourceField.getLabel());
        assertThat(copiedField.getType()).isEqualTo(sourceField.getType());
        assertThat(copiedField.getRequired()).isEqualTo(sourceField.getRequired());
    }

    @Test
    void aNewVersionCopiesRestrictionsWithTheirParameters() {
        Long sourceId = seedFormWithOneField("COPY_RESTRICTIONS");

        FormDefinitionDTO copy = dao.copyAsNewVersion(sourceId).orElseThrow();
        entityManager.flush();
        entityManager.clear();

        FieldDefinition copiedField = formRepository.findById(copy.getId()).orElseThrow()
                .getFields().get(0);

        assertThat(copiedField.getRestrictions()).hasSize(1);
        FieldRestriction restriction = copiedField.getRestrictions().get(0);
        assertThat(restriction.getType()).isEqualTo(RestrictionType.MIN_LENGTH);
        assertThat(restriction.getErrorMessage()).isEqualTo("Too short");
        assertThat(restriction.getParameters()).containsEntry("value", "3");
    }

    @Test
    void aNewVersionCopiesOptions() {
        Long sourceId = seedFormWithOneField("COPY_OPTIONS");

        FormDefinitionDTO copy = dao.copyAsNewVersion(sourceId).orElseThrow();
        entityManager.flush();
        entityManager.clear();

        FieldDefinition copiedField = formRepository.findById(copy.getId()).orElseThrow()
                .getFields().get(0);

        assertThat(copiedField.getOptions())
                .extracting(FieldOption::getValue)
                .containsExactly("yes");
    }

    /**
     * The one that would break silently. A copied dependency keeping the source's field rows would
     * make the new version's conditional logic read and write the old version's fields.
     */
    @Test
    void aNewVersionRewiresDependenciesToItsOwnFields() {
        Long sourceId = seedFormWithDependency("COPY_DEPENDENCIES");

        FormDefinitionDTO copy = dao.copyAsNewVersion(sourceId).orElseThrow();
        entityManager.flush();
        entityManager.clear();

        FormDefinition copied = formRepository.findById(copy.getId()).orElseThrow();
        List<Long> copiedFieldIds = copied.getFields().stream().map(FieldDefinition::getId).toList();

        List<FieldDependency> dependencies = fieldDependencyRepository.findByFormDefinitionId(copy.getId());
        assertThat(dependencies).hasSize(1);

        FieldDependency dependency = dependencies.get(0);
        assertThat(dependency.getDependentField().getId()).isIn(copiedFieldIds);
        assertThat(dependency.getTriggerField().getId()).isIn(copiedFieldIds);
        assertThat(dependency.getCondition()).isEqualTo(DependencyCondition.EQUALS);
        assertThat(dependency.getEffect()).isEqualTo(DependencyEffect.SHOW);
        assertThat(dependency.getTriggerValue()).isEqualTo("yes");

        // And the source keeps exactly its own, so copying did not move anything.
        assertThat(fieldDependencyRepository.findByFormDefinitionId(sourceId)).hasSize(1);
    }

    @Test
    void aNewVersionRewiresLayoutsToItsOwnFields() {
        Long sourceId = seedFormWithLayout("COPY_LAYOUTS");

        FormDefinitionDTO copy = dao.copyAsNewVersion(sourceId).orElseThrow();
        entityManager.flush();
        entityManager.clear();

        FormDefinition copied = formRepository.findById(copy.getId()).orElseThrow();
        Long copiedFieldId = copied.getFields().get(0).getId();

        assertThat(copied.getLayouts()).hasSize(1);
        FormLayout layout = copied.getLayouts().get(0);
        assertThat(layout.getDeviceType()).isEqualTo(DeviceType.DESKTOP);
        assertThat(layout.getFieldLayouts()).hasSize(1);

        FieldLayout fieldLayout = layout.getFieldLayouts().get(0);
        assertThat(fieldLayout.getFieldDefinition().getId()).isEqualTo(copiedFieldId);
        assertThat(fieldLayout.getColspan()).isEqualTo(6);
        assertThat(fieldLayout.getCustomProperties()).containsEntry("theme", "compact");
    }

    @Test
    void editingACopyLeavesTheSourceAlone() {
        Long sourceId = seedFormWithOneField("ISOLATED");

        FormDefinitionDTO copy = dao.copyAsNewVersion(sourceId).orElseThrow();
        entityManager.flush();
        entityManager.clear();

        FormDefinition copied = formRepository.findById(copy.getId()).orElseThrow();
        copied.getFields().get(0).setLabel("Renamed on the new version");
        formRepository.saveAndFlush(copied);
        entityManager.clear();

        assertThat(formRepository.findById(sourceId).orElseThrow().getFields().get(0).getLabel())
                .isEqualTo("Full name");
    }

    @Test
    void versionsAreNumberedPastTheHighestOneTheCodeHas() {
        Long sourceId = seedFormWithOneField("NUMBERING");

        Long secondId = dao.copyAsNewVersion(sourceId).orElseThrow().getId();
        entityManager.flush();

        // Branching off version 1 again must not collide with the version 2 that already exists.
        FormDefinitionDTO third = dao.copyAsNewVersion(sourceId).orElseThrow();
        entityManager.flush();

        assertThat(third.getVersion()).isEqualTo(3);
        assertThat(secondId).isNotEqualTo(third.getId());
    }

    @Test
    void copyingSomethingThatIsNotThereReportsNothing() {
        assertThat(dao.copyAsNewVersion(-1L)).isEmpty();
    }

    private FormDefinition form(String code, int version) {
        FormDefinition form = new FormDefinition();
        form.setCode(code);
        form.setTitle("Form " + code);
        form.setVersion(version);
        return form;
    }

    private Long seedFormWithOneField(String code) {
        FormDefinition form = form(code, 1);

        FieldDefinition field = new FieldDefinition();
        field.setFormDefinition(form);
        field.setName("full_name");
        field.setLabel("Full name");
        field.setType(FieldType.TEXT);
        field.setOrderIndex(0);
        field.setRequired(true);

        FieldRestriction restriction = new FieldRestriction();
        restriction.setFieldDefinition(field);
        restriction.setType(RestrictionType.MIN_LENGTH);
        restriction.setErrorMessage("Too short");
        restriction.setOrderIndex(0);
        restriction.setParameters(Map.of("value", "3"));
        field.setRestrictions(new ArrayList<>(List.of(restriction)));

        FieldOption option = new FieldOption();
        option.setFieldDefinition(field);
        option.setLabel("Yes");
        option.setValue("yes");
        option.setOrderIndex(0);
        field.setOptions(new ArrayList<>(List.of(option)));

        form.setFields(new ArrayList<>(List.of(field)));

        FormDefinition saved = formRepository.saveAndFlush(form);
        entityManager.clear();
        return saved.getId();
    }

    private Long seedFormWithDependency(String code) {
        FormDefinition form = form(code, 1);
        FieldDefinition trigger = plainField(form, "has_pet", 0);
        FieldDefinition dependent = plainField(form, "pet_name", 1);
        form.setFields(new ArrayList<>(List.of(trigger, dependent)));
        FormDefinition saved = formRepository.saveAndFlush(form);

        FieldDependency dependency = new FieldDependency();
        dependency.setTriggerField(saved.getFields().get(0));
        dependency.setDependentField(saved.getFields().get(1));
        dependency.setCondition(DependencyCondition.EQUALS);
        dependency.setEffect(DependencyEffect.SHOW);
        dependency.setTriggerValue("yes");
        fieldDependencyRepository.saveAndFlush(dependency);

        entityManager.clear();
        return saved.getId();
    }

    private Long seedFormWithLayout(String code) {
        FormDefinition form = form(code, 1);
        FieldDefinition field = plainField(form, "full_name", 0);
        form.setFields(new ArrayList<>(List.of(field)));

        FormLayout layout = new FormLayout();
        layout.setFormDefinition(form);
        layout.setDeviceType(DeviceType.DESKTOP);

        FieldLayout fieldLayout = new FieldLayout();
        fieldLayout.setFormLayout(layout);
        fieldLayout.setFieldDefinition(field);
        fieldLayout.setRow(0);
        fieldLayout.setColumn(0);
        fieldLayout.setColspan(6);
        fieldLayout.setVisible(true);
        fieldLayout.setCustomProperties(Map.of("theme", "compact"));
        layout.setFieldLayouts(new ArrayList<>(List.of(fieldLayout)));

        form.setLayouts(new ArrayList<>(List.of(layout)));

        FormDefinition saved = formRepository.saveAndFlush(form);
        entityManager.clear();
        return saved.getId();
    }

    private FieldDefinition plainField(FormDefinition form, String name, int orderIndex) {
        FieldDefinition field = new FieldDefinition();
        field.setFormDefinition(form);
        field.setName(name);
        field.setLabel(name);
        field.setType(FieldType.TEXT);
        field.setOrderIndex(orderIndex);
        field.setRequired(false);
        return field;
    }
}
