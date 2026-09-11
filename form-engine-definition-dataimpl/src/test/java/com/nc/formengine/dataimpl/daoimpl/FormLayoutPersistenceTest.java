package com.nc.formengine.dataimpl.daoimpl;

import com.nc.formengine.data.dao.FormLayoutDao;
import com.nc.formengine.dataimpl.PersistenceTestConfiguration;
import com.nc.formengine.dataimpl.entity.FieldDefinition;
import com.nc.formengine.dataimpl.entity.FormDefinition;
import com.nc.formengine.dataimpl.mapper.FieldLayoutMapper;
import com.nc.formengine.dataimpl.mapper.FormLayoutMapper;
import com.nc.formengine.dataimpl.repository.FieldRepository;
import com.nc.formengine.dataimpl.repository.FormRepository;
import com.nc.formengine.model.dto.FieldLayoutDTO;
import com.nc.formengine.model.dto.FormLayoutDTO;
import com.nc.formengine.model.enums.DeviceType;
import com.nc.formengine.model.enums.FieldType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What a form layout keeps when it goes through the database.
 *
 * <p>Unlike a field's restrictions and options, a layout's field placements are never partial: the
 * DTO always speaks for the complete set, so every save replaces the whole collection rather than
 * reconciling it by id.
 */
@SpringBootTest(classes = PersistenceTestConfiguration.class)
@Import({FormLayoutDaoImpl.class, FormLayoutMapper.class, FieldLayoutMapper.class})
@Transactional
class FormLayoutPersistenceTest {

    @Autowired
    private FormLayoutDao dao;

    @Autowired
    private FormRepository formRepository;

    @Autowired
    private FieldRepository fieldRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private Long formId;
    private Long field1Id;
    private Long field2Id;

    @BeforeEach
    void createFormWithTwoFields() {
        FormDefinition form = new FormDefinition();
        form.setCode("LAYOUT_PERSISTENCE_TEST");
        form.setTitle("Layout persistence test");
        formId = formRepository.save(form).getId();

        field1Id = saveField(form, "first_name");
        field2Id = saveField(form, "last_name");
    }

    @Test
    void anUpdateReplacesThePlacementsAndKeepsTheForm() {
        FormLayoutDTO saved = reload(dao.save(layout(DeviceType.MOBILE, placement(field1Id, 0, 0))).getId());

        FormLayoutDTO update = new FormLayoutDTO();
        update.setId(saved.getId());
        update.setFormDefinitionId(formId);
        update.setDeviceType(DeviceType.MOBILE);
        update.setFieldLayouts(List.of(placement(field1Id, 0, 0), placement(field2Id, 1, 0)));
        dao.save(update);

        FormLayoutDTO reloaded = reload(saved.getId());

        assertThat(reloaded.getFormDefinitionId()).isEqualTo(formId);
        assertThat(reloaded.getFieldLayouts())
            .extracting(FieldLayoutDTO::getFieldDefinitionId)
            .containsExactlyInAnyOrder(field1Id, field2Id);
    }

    @Test
    void theGenericLayoutRoundTripsWithANullDeviceType() {
        FormLayoutDTO saved = dao.save(layout(null, placement(field1Id, 0, 0)));

        FormLayoutDTO found = reloadByDevice(null);

        assertThat(found.getId()).isEqualTo(saved.getId());
        assertThat(found.getDeviceType()).isNull();
    }

    /**
     * {@code FormDefinition} owns its layouts with {@code cascade = ALL}: deleting the layout row
     * while a loaded parent still lists it makes the cascade write the layout straight back at
     * flush, so the delete has to take it out of that collection too.
     */
    @Test
    void deletingALayoutRemovesItEvenWithItsFormsCollectionLoaded() {
        FormLayoutDTO saved = dao.save(layout(DeviceType.MOBILE, placement(field1Id, 0, 0)));
        flushAndClear();

        // Forces the parent's layouts collection into the persistence context, the way a service
        // that loaded the form for some other reason in the same transaction would.
        formRepository.findById(formId).orElseThrow().getLayouts().size();

        dao.deleteById(saved.getId());
        flushAndClear();

        assertThat(dao.findById(saved.getId())).isEmpty();
        assertThat(entityManager.createQuery("select count(l) from FieldLayout l", Long.class)
            .getSingleResult()).isZero();
    }

    private Long saveField(FormDefinition form, String name) {
        FieldDefinition field = new FieldDefinition();
        field.setFormDefinition(form);
        field.setName(name);
        field.setLabel(name);
        field.setType(FieldType.TEXT);
        return fieldRepository.save(field).getId();
    }

    /** Reads the layout back from the database rather than from the persistence context. */
    private FormLayoutDTO reload(Long id) {
        flushAndClear();
        return dao.findById(id).orElseThrow();
    }

    private FormLayoutDTO reloadByDevice(DeviceType deviceType) {
        flushAndClear();
        return dao.findByFormDefinitionIdAndDeviceType(formId, deviceType).orElseThrow();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private FormLayoutDTO layout(DeviceType deviceType, FieldLayoutDTO... placements) {
        FormLayoutDTO layout = new FormLayoutDTO();
        layout.setFormDefinitionId(formId);
        layout.setDeviceType(deviceType);
        layout.setFieldLayouts(List.of(placements));
        return layout;
    }

    private FieldLayoutDTO placement(Long fieldDefinitionId, int row, int column) {
        return FieldLayoutDTO.builder()
            .fieldDefinitionId(fieldDefinitionId)
            .row(row)
            .column(column)
            .colspan(1)
            .rowspan(1)
            .build();
    }
}
