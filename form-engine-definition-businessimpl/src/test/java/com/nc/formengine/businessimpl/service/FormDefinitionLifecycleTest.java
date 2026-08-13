package com.nc.formengine.businessimpl.service;

import com.nc.formengine.data.dao.FieldDefinitionDao;
import com.nc.formengine.data.dao.FormDao;
import com.nc.formengine.model.dto.FieldDefinitionDTO;
import com.nc.formengine.model.dto.FormDefinitionDTO;
import com.nc.formengine.model.enums.FormDefinitionStatus;
import com.nc.formengine.model.exception.DuplicateResourceException;
import com.nc.formengine.model.exception.FormDefinitionNotEditableException;
import com.nc.formengine.model.exception.FormDefinitionNotFoundException;
import com.nc.formengine.model.exception.InvalidFormDefinitionTransitionException;
import com.nc.formengine.model.exception.ValidationFailedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The rules that keep a published form from changing under the submissions that reference it.
 *
 * <p>The copying itself is pinned down against a real database in {@code FormDefinitionVersioningTest};
 * what is checked here is the part this service decides: who may be edited, what publishing does to
 * the version it replaces, and which fields a caller is not allowed to set.
 */
class FormDefinitionLifecycleTest {

    private static final Long FORM_ID = 1L;

    private FormDao formDao;
    private FieldDefinitionDao fieldDefinitionDao;
    private FormDefinitionServiceImpl service;

    @BeforeEach
    void setUp() {
        formDao = mock(FormDao.class);
        fieldDefinitionDao = mock(FieldDefinitionDao.class);
        // The real guard rather than a mock: what it refuses is part of what these tests are about.
        service = new FormDefinitionServiceImpl(formDao, fieldDefinitionDao,
                new DefinitionMutationGuard(formDao, fieldDefinitionDao));
        when(formDao.save(any(FormDefinitionDTO.class))).thenAnswer(call -> call.getArgument(0));
        // A form with something to fill in, which is what publishing needs. Tests about the empty case
        // override this.
        when(fieldDefinitionDao.findByFormDefinitionId(anyLong()))
                .thenReturn(List.of(FieldDefinitionDTO.builder().id(50L).name("nickname").build()));
    }

    @Test
    void aNewFormIsVersionOneAndADraft() {
        when(formDao.existsByCode("new_form")).thenReturn(false);

        service.create(FormDefinitionDTO.builder().code("new_form").title("New").build());

        FormDefinitionDTO saved = captureSaved();
        assertThat(saved.getVersion()).isEqualTo(1);
        assertThat(saved.getStatus()).isEqualTo(FormDefinitionStatus.DRAFT);
    }

    /** A caller cannot open a form straight into PUBLISHED and skip the draft stage. */
    @Test
    void aNewFormIgnoresTheStatusAndVersionTheCallerAsksFor() {
        when(formDao.existsByCode("new_form")).thenReturn(false);

        service.create(FormDefinitionDTO.builder()
                .code("new_form").title("New").version(7).status(FormDefinitionStatus.PUBLISHED).build());

        FormDefinitionDTO saved = captureSaved();
        assertThat(saved.getVersion()).isEqualTo(1);
        assertThat(saved.getStatus()).isEqualTo(FormDefinitionStatus.DRAFT);
    }

    @Test
    void aCodeMayOnlyBeOpenedOnce() {
        when(formDao.existsByCode("taken")).thenReturn(true);

        assertThatThrownBy(() -> service.create(
                FormDefinitionDTO.builder().code("taken").title("Another").build()))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void aDraftIsEditable() {
        stored(FormDefinitionStatus.DRAFT);

        service.update(FORM_ID, FormDefinitionDTO.builder().title("Edited").build());

        assertThat(captureSaved().getTitle()).isEqualTo("Edited");
    }

    @Test
    void aPublishedFormIsNotEditable() {
        stored(FormDefinitionStatus.PUBLISHED);

        assertThatThrownBy(() -> service.update(FORM_ID, FormDefinitionDTO.builder().title("Edited").build()))
                .isInstanceOf(FormDefinitionNotEditableException.class);

        verify(formDao, never()).save(any());
    }

    @Test
    void anArchivedFormIsNotEditable() {
        stored(FormDefinitionStatus.ARCHIVED);

        assertThatThrownBy(() -> service.update(FORM_ID, FormDefinitionDTO.builder().title("Edited").build()))
                .isInstanceOf(FormDefinitionNotEditableException.class);
    }

    /** Renaming a code would orphan the other versions that share it, and renumbering would collide. */
    @Test
    void anUpdateCannotRewriteTheCodeOrTheVersion() {
        stored(FormDefinitionStatus.DRAFT);

        service.update(FORM_ID, FormDefinitionDTO.builder()
                .code("something_else").version(9).title("Edited").build());

        FormDefinitionDTO saved = captureSaved();
        assertThat(saved.getCode()).isEqualTo("form");
        assertThat(saved.getVersion()).isEqualTo(1);
    }

    @Test
    void publishingMakesADraftLive() {
        stored(FormDefinitionStatus.DRAFT);
        when(formDao.findLatestPublishedByCode("form")).thenReturn(Optional.empty());

        service.publish(FORM_ID);

        assertThat(captureSaved().getStatus()).isEqualTo(FormDefinitionStatus.PUBLISHED);
    }

    @Test
    void publishingArchivesTheVersionItReplaces() {
        stored(FormDefinitionStatus.DRAFT);
        FormDefinitionDTO previous = FormDefinitionDTO.builder()
                .id(99L).code("form").title("Form").version(1)
                .status(FormDefinitionStatus.PUBLISHED).build();
        when(formDao.findLatestPublishedByCode("form")).thenReturn(Optional.of(previous));

        service.publish(FORM_ID);

        ArgumentCaptor<FormDefinitionDTO> captor = ArgumentCaptor.forClass(FormDefinitionDTO.class);
        verify(formDao, org.mockito.Mockito.times(2)).save(captor.capture());
        List<FormDefinitionDTO> saved = captor.getAllValues();

        assertThat(saved.get(0).getId()).isEqualTo(99L);
        assertThat(saved.get(0).getStatus()).isEqualTo(FormDefinitionStatus.ARCHIVED);
        assertThat(saved.get(1).getId()).isEqualTo(FORM_ID);
        assertThat(saved.get(1).getStatus()).isEqualTo(FormDefinitionStatus.PUBLISHED);
    }

    /** Republishing the live version must not archive it on its way back in. */
    @Test
    void publishingSomethingAlreadyPublishedIsRefused() {
        stored(FormDefinitionStatus.PUBLISHED);

        assertThatThrownBy(() -> service.publish(FORM_ID))
                .isInstanceOf(InvalidFormDefinitionTransitionException.class);

        verify(formDao, never()).save(any());
    }

    @Test
    void publishingAnArchivedVersionIsRefused() {
        stored(FormDefinitionStatus.ARCHIVED);

        assertThatThrownBy(() -> service.publish(FORM_ID))
                .isInstanceOf(InvalidFormDefinitionTransitionException.class);
    }

    @Test
    void publishingSomethingThatIsNotThereIsRefused() {
        when(formDao.findById(anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.publish(FORM_ID))
                .isInstanceOf(FormDefinitionNotFoundException.class);
    }

    /**
     * A live form with nothing in it renders nothing, validates nothing, and calls an empty submission
     * valid. This rule used to exist only in the builder's button-enabling logic.
     */
    @Test
    void anEmptyFormCannotBePublished() {
        stored(FormDefinitionStatus.DRAFT);
        when(fieldDefinitionDao.findByFormDefinitionId(anyLong())).thenReturn(List.of());

        assertThatThrownBy(() -> service.publish(FORM_ID))
                .isInstanceOf(ValidationFailedException.class);

        verify(formDao, never()).save(any());
    }

    /** The shape of a code is a domain rule, not a nicety the editor happens to check. */
    @Test
    void aCodeThatCouldNotBeUsedAsAKeyIsRefused() {
        assertThatThrownBy(() -> service.create(
                FormDefinitionDTO.builder().code("ALTA-CLIENTE").title("Customers").build()))
                .isInstanceOf(ValidationFailedException.class);

        verify(formDao, never()).save(any());
    }

    @Test
    void aPublishedFormCanBeVersioned() {
        stored(FormDefinitionStatus.PUBLISHED);
        FormDefinitionDTO copy = FormDefinitionDTO.builder()
                .id(2L).code("form").version(2).status(FormDefinitionStatus.DRAFT).build();
        when(formDao.copyAsNewVersion(FORM_ID)).thenReturn(Optional.of(copy));

        assertThat(service.createNewVersion(FORM_ID)).isEqualTo(copy);
    }

    @Test
    void anArchivedFormCanBeVersioned() {
        stored(FormDefinitionStatus.ARCHIVED);
        FormDefinitionDTO copy = FormDefinitionDTO.builder()
                .id(2L).code("form").version(2).status(FormDefinitionStatus.DRAFT).build();
        when(formDao.copyAsNewVersion(FORM_ID)).thenReturn(Optional.of(copy));

        assertThat(service.createNewVersion(FORM_ID)).isEqualTo(copy);
    }

    /**
     * Branching a draft would leave two concurrent drafts of one code. Publishing the older afterwards
     * makes findByCode ("newest") and findLatestPublishedByCode ("live") disagree about what the code
     * means, which is a form with two answers to "which version is this?".
     */
    @Test
    void aDraftCannotBeVersioned() {
        stored(FormDefinitionStatus.DRAFT);

        assertThatThrownBy(() -> service.createNewVersion(FORM_ID))
                .isInstanceOf(InvalidFormDefinitionTransitionException.class);

        verify(formDao, never()).copyAsNewVersion(anyLong());
    }

    @Test
    void aPublishedFormCannotBeDeleted() {
        stored(FormDefinitionStatus.PUBLISHED);

        assertThatThrownBy(() -> service.deleteById(FORM_ID))
                .isInstanceOf(FormDefinitionNotEditableException.class);

        verify(formDao, never()).deleteById(anyLong());
    }

    @Test
    void aDraftCanBeDeleted() {
        stored(FormDefinitionStatus.DRAFT);

        service.deleteById(FORM_ID);

        verify(formDao).deleteById(FORM_ID);
    }

    private void stored(FormDefinitionStatus status) {
        when(formDao.findById(FORM_ID)).thenReturn(Optional.of(FormDefinitionDTO.builder()
                .id(FORM_ID)
                .code("form")
                .title("Form")
                .version(1)
                .status(status)
                .build()));
    }

    private FormDefinitionDTO captureSaved() {
        ArgumentCaptor<FormDefinitionDTO> captor = ArgumentCaptor.forClass(FormDefinitionDTO.class);
        verify(formDao, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        return captor.getValue();
    }
}
