package com.nc.formengine.dataimpl.daoimpl;

import com.nc.formengine.data.dao.FieldDefinitionDao;
import com.nc.formengine.dataimpl.PersistenceTestConfiguration;
import com.nc.formengine.dataimpl.entity.FormDefinition;
import com.nc.formengine.dataimpl.mapper.FieldDefinitionMapper;
import com.nc.formengine.dataimpl.mapper.FieldOptionMapper;
import com.nc.formengine.dataimpl.mapper.FieldRestrictionMapper;
import com.nc.formengine.dataimpl.repository.FormRepository;
import com.nc.formengine.model.dto.FieldDefinitionDTO;
import com.nc.formengine.model.dto.FieldOptionDTO;
import com.nc.formengine.model.dto.FieldRestrictionDTO;
import com.nc.formengine.model.enums.FieldType;
import com.nc.formengine.model.enums.RestrictionType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What a field definition keeps when it goes through the database.
 *
 * <p>Restrictions used to live in five columns on the field, which could not hold a custom message,
 * an evaluation order, or any rule without a column of its own. These tests pin down that the
 * replacement keeps all of it, and that updating a field does not quietly destroy what the request
 * said nothing about.
 */
@SpringBootTest(classes = PersistenceTestConfiguration.class)
@Import({FieldDefinitionDaoImpl.class, FieldDefinitionMapper.class,
    FieldRestrictionMapper.class, FieldOptionMapper.class})
@Transactional
class FieldDefinitionPersistenceTest {

    @Autowired
    private FieldDefinitionDao dao;

    @Autowired
    private FormRepository formRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private Long formId;

    @BeforeEach
    void createForm() {
        FormDefinition form = new FormDefinition();
        form.setCode("PERSISTENCE_TEST");
        form.setTitle("Persistence test");
        formId = formRepository.save(form).getId();
    }

    @Test
    void keepsEverythingARestrictionCarries() {
        FieldDefinitionDTO saved = dao.save(field(List.of(
            restriction(RestrictionType.MIN_LENGTH, Map.of("minLength", 3), "Too short", 0),
            restriction(RestrictionType.EMAIL, Map.of(), "Write something", 1)), null));

        FieldDefinitionDTO reloaded = reload(saved.getId());

        assertThat(reloaded.getRestrictions()).hasSize(2);
        FieldRestrictionDTO minLength = reloaded.getRestrictions().get(0);
        assertThat(minLength.getId()).isNotNull();
        assertThat(minLength.getRestrictionType()).isEqualTo(RestrictionType.MIN_LENGTH);
        assertThat(minLength.getErrorMessage()).isEqualTo("Too short");
        assertThat(minLength.getOrderIndex()).isZero();
        assertThat(minLength.getFieldDefinitionId()).isEqualTo(saved.getId());
    }

    /**
     * A rule taking no parameter had no column to live in, so it could not be stored at all before.
     */
    @Test
    void storesRestrictionsThatTakeNoParameter() {
        FieldDefinitionDTO saved = dao.save(field(List.of(
            restriction(RestrictionType.EMAIL, Map.of(), null, 0)), null));

        assertThat(reload(saved.getId()).getRestrictions())
            .extracting(FieldRestrictionDTO::getRestrictionType)
            .containsExactly(RestrictionType.EMAIL);
    }

    @Test
    void keepsTheMessageForAMissingRequiredAnswer() {
        FieldDefinitionDTO field = field(null, null);
        field.setRequired(true);
        field.setRequiredMessage("We need your name");

        assertThat(reload(dao.save(field).getId()).getRequiredMessage()).isEqualTo("We need your name");
    }

    @Test
    void keepsANumericParameterNumeric() {
        FieldDefinitionDTO saved = dao.save(field(List.of(
            restriction(RestrictionType.MIN_LENGTH, Map.of("minLength", 3), null, 0)), null));

        assertThat(reload(saved.getId()).getRestrictions().get(0).getParameters())
            .containsEntry("minLength", 3);
    }

    @Test
    void keepsARegexExactlyAsItWasWritten() {
        String pattern = "^[A-Z]{2}-\\d+$";
        FieldDefinitionDTO saved = dao.save(field(List.of(
            restriction(RestrictionType.PATTERN, Map.of("pattern", pattern), null, 0)), null));

        assertThat(reload(saved.getId()).getRestrictions().get(0).getParameters())
            .containsEntry("pattern", pattern);
    }

    @Test
    void readsTheOptionsOfAFieldWithTheFieldItself() {
        FieldDefinitionDTO saved = dao.save(field(null, List.of(
            option("One", "1", 0), option("Two", "2", 1))));

        assertThat(reload(saved.getId()).getOptions())
            .extracting(FieldOptionDTO::getLabel, FieldOptionDTO::getValue)
            .containsExactly(org.assertj.core.groups.Tuple.tuple("One", "1"),
                org.assertj.core.groups.Tuple.tuple("Two", "2"));
    }

    @Test
    void ordersRestrictionsAndOptionsByTheirOrderIndex() {
        FieldDefinitionDTO saved = dao.save(field(
            List.of(restriction(RestrictionType.EMAIL, Map.of(), null, 5),
                restriction(RestrictionType.MIN_LENGTH, Map.of("minLength", 3), null, 1)),
            List.of(option("Second", "b", 9), option("First", "a", 2))));

        FieldDefinitionDTO reloaded = reload(saved.getId());

        assertThat(reloaded.getRestrictions())
            .extracting(FieldRestrictionDTO::getRestrictionType)
            .containsExactly(RestrictionType.MIN_LENGTH, RestrictionType.EMAIL);
        assertThat(reloaded.getOptions())
            .extracting(FieldOptionDTO::getLabel)
            .containsExactly("First", "Second");
    }

    @Test
    void anUpdateThatSaysNothingAboutThemKeepsTheRestrictionsAndOptions() {
        FieldDefinitionDTO saved = dao.save(field(
            List.of(restriction(RestrictionType.MIN_LENGTH, Map.of("minLength", 3), "Too short", 0)),
            List.of(option("One", "1", 0))));

        FieldDefinitionDTO update = new FieldDefinitionDTO();
        update.setId(saved.getId());
        update.setFormDefinitionId(formId);
        update.setName("nickname");
        update.setLabel("Renamed");
        update.setType(FieldType.TEXT);
        update.setRequired(true);
        dao.save(update);

        FieldDefinitionDTO reloaded = reload(saved.getId());

        assertThat(reloaded.getLabel()).isEqualTo("Renamed");
        assertThat(reloaded.getRestrictions()).hasSize(1);
        assertThat(reloaded.getRestrictions().get(0).getErrorMessage()).isEqualTo("Too short");
        assertThat(reloaded.getOptions()).hasSize(1);
    }

    @Test
    void anUpdateCarryingEmptyListsDeletesThem() {
        FieldDefinitionDTO saved = dao.save(field(
            List.of(restriction(RestrictionType.MIN_LENGTH, Map.of("minLength", 3), null, 0)),
            List.of(option("One", "1", 0))));

        FieldDefinitionDTO update = reload(saved.getId());
        update.setRestrictions(List.of());
        update.setOptions(List.of());
        dao.save(update);

        FieldDefinitionDTO reloaded = reload(saved.getId());

        assertThat(reloaded.getRestrictions()).isEmpty();
        assertThat(reloaded.getOptions()).isEmpty();
    }

    @Test
    void anUpdatedRestrictionKeepsItsIdentity() {
        FieldDefinitionDTO saved = dao.save(field(
            List.of(restriction(RestrictionType.MIN_LENGTH, Map.of("minLength", 3), "Too short", 0)), null));
        Long restrictionId = reload(saved.getId()).getRestrictions().get(0).getId();

        FieldDefinitionDTO update = reload(saved.getId());
        update.getRestrictions().get(0).setErrorMessage("Ahora al menos cinco");
        update.getRestrictions().get(0).setParameters(Map.of("minLength", 5));
        dao.save(update);

        FieldRestrictionDTO reloaded = reload(saved.getId()).getRestrictions().get(0);

        assertThat(reloaded.getId()).isEqualTo(restrictionId);
        assertThat(reloaded.getErrorMessage()).isEqualTo("Ahora al menos cinco");
        assertThat(reloaded.getParameters()).containsEntry("minLength", 5);
    }

    @Test
    void deletingAFieldTakesItsRestrictionsWithIt() {
        FieldDefinitionDTO saved = dao.save(field(
            List.of(restriction(RestrictionType.MIN_LENGTH, Map.of("minLength", 3), null, 0)), null));
        flushAndClear();

        dao.deleteById(saved.getId());
        flushAndClear();

        assertThat(dao.findById(saved.getId())).isEmpty();
        assertThat(entityManager.createQuery("select count(r) from FieldRestriction r", Long.class)
            .getSingleResult()).isZero();
    }

    /** Reads the field back from the database rather than from the persistence context. */
    private FieldDefinitionDTO reload(Long id) {
        flushAndClear();
        return dao.findById(id).orElseThrow();
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }

    private FieldDefinitionDTO field(List<FieldRestrictionDTO> restrictions, List<FieldOptionDTO> options) {
        FieldDefinitionDTO field = new FieldDefinitionDTO();
        field.setFormDefinitionId(formId);
        field.setName("nickname");
        field.setLabel("Nickname");
        field.setType(FieldType.TEXT);
        field.setRequired(true);
        field.setOrderIndex(1);
        field.setRestrictions(restrictions);
        field.setOptions(options);
        return field;
    }

    private FieldRestrictionDTO restriction(RestrictionType type, Map<String, Object> parameters,
                                            String errorMessage, Integer orderIndex) {
        return FieldRestrictionDTO.builder()
            .restrictionType(type)
            .parameters(parameters)
            .errorMessage(errorMessage)
            .orderIndex(orderIndex)
            .build();
    }

    private FieldOptionDTO option(String label, String value, Integer orderIndex) {
        return FieldOptionDTO.builder()
            .label(label)
            .value(value)
            .orderIndex(orderIndex)
            .build();
    }
}
