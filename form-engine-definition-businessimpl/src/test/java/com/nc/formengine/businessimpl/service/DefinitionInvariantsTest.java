package com.nc.formengine.businessimpl.service;

import com.nc.formengine.data.dao.FieldDefinitionDao;
import com.nc.formengine.data.dao.FieldOptionDao;
import com.nc.formengine.data.dao.FormDao;
import com.nc.formengine.model.dto.FieldDefinitionDTO;
import com.nc.formengine.model.dto.FieldOptionDTO;
import com.nc.formengine.model.dto.FieldRestrictionDTO;
import com.nc.formengine.model.dto.FormDefinitionDTO;
import com.nc.formengine.model.enums.FieldType;
import com.nc.formengine.model.enums.FormDefinitionStatus;
import com.nc.formengine.model.enums.RestrictionType;
import com.nc.formengine.model.exception.DuplicateResourceException;
import com.nc.formengine.model.exception.FormDefinitionNotEditableException;
import com.nc.formengine.model.exception.ValidationFailedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The rules a form definition has to satisfy, checked where the model owns them.
 *
 * <p>Every one of these used to live only in {@code BuilderValidation}, in the Vaadin module. They were
 * therefore not rules at all but suggestions to whoever went through the REST API: a form could be
 * stored with two fields of one name, a select with nothing to select, or a restriction whose parameter
 * was missing and which consequently validated nothing. These tests go through the services, so they
 * fail if the enforcement ever drifts back up into a consumer.
 */
class DefinitionInvariantsTest {

    private static final Long FORM_ID = 1L;
    private static final Long FIELD_ID = 10L;

    private FormDao formDao;
    private FieldDefinitionDao fieldDefinitionDao;
    private FieldOptionDao fieldOptionDao;
    private FieldDefinitionServiceImpl fieldService;
    private FieldOptionServiceImpl optionService;

    @BeforeEach
    void setUp() {
        formDao = mock(FormDao.class);
        fieldDefinitionDao = mock(FieldDefinitionDao.class);
        fieldOptionDao = mock(FieldOptionDao.class);
        DefinitionMutationGuard guard = new DefinitionMutationGuard(formDao, fieldDefinitionDao);
        fieldService = new FieldDefinitionServiceImpl(fieldDefinitionDao, guard);
        optionService = new FieldOptionServiceImpl(fieldOptionDao, guard);

        formWith(FormDefinitionStatus.DRAFT);
        when(fieldDefinitionDao.findByFormDefinitionId(FORM_ID)).thenReturn(List.of());
        when(fieldDefinitionDao.save(any())).thenAnswer(call -> call.getArgument(0));
        when(fieldOptionDao.save(any())).thenAnswer(call -> call.getArgument(0));
        when(fieldOptionDao.findByFieldDefinitionId(anyLong())).thenReturn(List.of());
    }

    // --- publishing freezes the whole definition, not just its own row ----------------------------

    @Test
    void aFieldCannotBeAddedToAPublishedForm() {
        formWith(FormDefinitionStatus.PUBLISHED);

        assertThatThrownBy(() -> fieldService.create(text("nickname")))
                .isInstanceOf(FormDefinitionNotEditableException.class);

        verify(fieldDefinitionDao, never()).save(any());
    }

    @Test
    void aFieldCannotBeChangedOnAPublishedForm() {
        formWith(FormDefinitionStatus.PUBLISHED);
        when(fieldDefinitionDao.findById(FIELD_ID)).thenReturn(Optional.of(stored("nickname")));

        assertThatThrownBy(() -> fieldService.update(FIELD_ID, text("nickname")))
                .isInstanceOf(FormDefinitionNotEditableException.class);

        verify(fieldDefinitionDao, never()).save(any());
    }

    /**
     * The sharpest instance: deleting the field an archived submission answered is what the lifecycle is
     * documented as preventing, and it was the one path the demo seeder itself relied on.
     */
    @Test
    void aFieldCannotBeDeletedFromAPublishedForm() {
        formWith(FormDefinitionStatus.PUBLISHED);
        when(fieldDefinitionDao.findById(FIELD_ID)).thenReturn(Optional.of(stored("nickname")));

        assertThatThrownBy(() -> fieldService.deleteById(FIELD_ID))
                .isInstanceOf(FormDefinitionNotEditableException.class);

        verify(fieldDefinitionDao, never()).deleteById(anyLong());
    }

    @Test
    void anOptionCannotBeAddedToAFieldOfAPublishedForm() {
        formWith(FormDefinitionStatus.PUBLISHED);
        when(fieldDefinitionDao.findById(FIELD_ID)).thenReturn(Optional.of(stored("country")));

        assertThatThrownBy(() -> optionService.create(option("ar", "Argentina")))
                .isInstanceOf(FormDefinitionNotEditableException.class);

        verify(fieldOptionDao, never()).save(any());
    }

    @Test
    void anArchivedFormIsFrozenTheSameWay() {
        formWith(FormDefinitionStatus.ARCHIVED);

        assertThatThrownBy(() -> fieldService.create(text("nickname")))
                .isInstanceOf(FormDefinitionNotEditableException.class);
    }

    // --- what makes a field coherent --------------------------------------------------------------

    /**
     * Two fields of one name render one input for both and validate only one of them. The dependency
     * evaluator resolves fields by name, so the other simply disappears.
     */
    @Test
    void twoFieldsOfOneFormCannotShareAName() {
        when(fieldDefinitionDao.findByFormDefinitionId(FORM_ID))
                .thenReturn(List.of(stored("nickname")));

        assertThatThrownBy(() -> fieldService.create(text("nickname")))
                .isInstanceOf(DuplicateResourceException.class);

        verify(fieldDefinitionDao, never()).save(any());
    }

    @Test
    void aNameThatCouldNotBeUsedAsAKeyIsRefused() {
        assertThatThrownBy(() -> fieldService.create(text("not a key")))
                .isInstanceOf(ValidationFailedException.class);
    }

    @Test
    void aSelectWithNothingToSelectIsRefused() {
        FieldDefinitionDTO select = text("country");
        select.setType(FieldType.SELECT);

        assertThatThrownBy(() -> fieldService.create(select))
                .isInstanceOf(ValidationFailedException.class);
    }

    /**
     * Evaluation is permissive on purpose — a rule nobody can satisfy would leave a form unsubmittable
     * — so a rule with no parameter accepts every answer. Refusing to store one is what keeps that from
     * meaning "looks configured, does nothing".
     */
    @Test
    void aRestrictionWithoutItsParameterIsRefused() {
        FieldDefinitionDTO field = text("nickname");
        field.setRestrictions(List.of(FieldRestrictionDTO.builder()
                .restrictionType(RestrictionType.MIN_LENGTH)
                .build()));

        assertThatThrownBy(() -> fieldService.create(field))
                .isInstanceOf(ValidationFailedException.class);
    }

    /** Stored, echoed back, and inert for ever: the specification reports itself satisfied. */
    @Test
    void aRestrictionThatDoesNotApplyToTheFieldTypeIsRefused() {
        FieldDefinitionDTO field = text("nickname");
        field.setRestrictions(List.of(FieldRestrictionDTO.builder()
                .restrictionType(RestrictionType.MIN_VALUE)
                .parameters(Map.of("minValue", 3))
                .build()));

        assertThatThrownBy(() -> fieldService.create(field))
                .isInstanceOf(ValidationFailedException.class);
    }

    @Test
    void aMinimumGreaterThanItsMaximumIsRefused() {
        FieldDefinitionDTO field = text("nickname");
        field.setRestrictions(List.of(
                FieldRestrictionDTO.builder().restrictionType(RestrictionType.MIN_LENGTH)
                        .parameters(Map.of("minLength", 10)).build(),
                FieldRestrictionDTO.builder().restrictionType(RestrictionType.MAX_LENGTH)
                        .parameters(Map.of("maxLength", 3)).build()));

        assertThatThrownBy(() -> fieldService.create(field))
                .isInstanceOf(ValidationFailedException.class);
    }

    @Test
    void aCoherentFieldIsStored() {
        FieldDefinitionDTO field = text("nickname");
        field.setRestrictions(List.of(FieldRestrictionDTO.builder()
                .restrictionType(RestrictionType.MIN_LENGTH)
                .parameters(Map.of("minLength", 3))
                .build()));

        assertThatCode(() -> fieldService.create(field)).doesNotThrowAnyException();
        verify(fieldDefinitionDao).save(any());
    }

    // --- what makes an option usable --------------------------------------------------------------

    /** The renderer resolves an option by value and takes the first match. */
    @Test
    void twoOptionsOfOneFieldCannotShareAValue() {
        when(fieldDefinitionDao.findById(FIELD_ID)).thenReturn(Optional.of(stored("country")));
        FieldOptionDTO existing = option("ar", "Argentina");
        existing.setId(77L);
        when(fieldOptionDao.findByFieldDefinitionId(FIELD_ID)).thenReturn(List.of(existing));

        assertThatThrownBy(() -> optionService.create(option("ar", "Argentina again")))
                .isInstanceOf(DuplicateResourceException.class);
    }

    /**
     * A multi-select answer joins its values with a comma and does not escape it, so an option value
     * containing one could never be told apart from two others.
     */
    @Test
    void anOptionValueCannotContainTheSelectionSeparator() {
        when(fieldDefinitionDao.findById(FIELD_ID)).thenReturn(Optional.of(stored("country")));

        assertThatThrownBy(() -> optionService.create(option("ar,uy", "Both")))
                .isInstanceOf(ValidationFailedException.class);

        verify(fieldOptionDao, never()).save(any());
    }

    // --- fixtures ---------------------------------------------------------------------------------

    private void formWith(FormDefinitionStatus status) {
        when(formDao.findById(FORM_ID)).thenReturn(Optional.of(FormDefinitionDTO.builder()
                .id(FORM_ID).code("form").title("Form").version(1).status(status).build()));
    }

    private FieldDefinitionDTO text(String name) {
        return FieldDefinitionDTO.builder()
                .formDefinitionId(FORM_ID)
                .name(name)
                .label(name)
                .type(FieldType.TEXT)
                .orderIndex(0)
                .required(false)
                .build();
    }

    private FieldDefinitionDTO stored(String name) {
        FieldDefinitionDTO field = text(name);
        field.setId(FIELD_ID);
        return field;
    }

    private FieldOptionDTO option(String value, String label) {
        return FieldOptionDTO.builder()
                .fieldDefinitionId(FIELD_ID)
                .value(value)
                .label(label)
                .orderIndex(0)
                .build();
    }
}
