package com.nc.formengine.businessimpl.service;

import com.nc.formengine.business.service.FormDefinitionService;
import com.nc.formengine.data.dao.FormDao;
import com.nc.formengine.model.dto.FormDefinitionDTO;
import com.nc.formengine.model.enums.FormDefinitionStatus;
import com.nc.formengine.model.exception.DuplicateResourceException;
import com.nc.formengine.model.exception.FormDefinitionNotEditableException;
import com.nc.formengine.model.exception.FormDefinitionNotFoundException;
import com.nc.formengine.model.exception.InvalidFormDefinitionTransitionException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class FormDefinitionServiceImpl implements FormDefinitionService {

    private final FormDao formDao;

    @Override
    public FormDefinitionDTO create(FormDefinitionDTO formDefinitionDTO) {
        if (formDefinitionDTO.getId() != null) {
            throw new IllegalArgumentException("New form should not have an ID");
        }
        requireCodeIsFree(formDefinitionDTO.getCode());

        // The lifecycle owns these two, not the caller: a form is born as version 1 of a draft.
        formDefinitionDTO.setVersion(1);
        formDefinitionDTO.setStatus(FormDefinitionStatus.DRAFT);
        return formDao.save(formDefinitionDTO);
    }

    @Override
    public FormDefinitionDTO update(Long id, FormDefinitionDTO formDefinitionDTO) {
        FormDefinitionDTO stored = requireDraft(id);

        // The identity of a version is (code, version), and neither is the caller's to rewrite here:
        // renaming a code would orphan its other versions, and renumbering would collide with them.
        formDefinitionDTO.setId(id);
        formDefinitionDTO.setCode(stored.getCode());
        formDefinitionDTO.setVersion(stored.getVersion());
        formDefinitionDTO.setStatus(stored.getStatus());
        return formDao.save(formDefinitionDTO);
    }

    @Override
    public FormDefinitionDTO publish(Long id) {
        FormDefinitionDTO definition = requireExisting(id);
        if (definition.getStatus() != FormDefinitionStatus.DRAFT) {
            throw new InvalidFormDefinitionTransitionException(
                    id, definition.getStatus(), FormDefinitionStatus.PUBLISHED);
        }

        // Only one version of a code may be live, so whatever was published before steps aside.
        // Archived rather than deleted: submissions made under it still have to be interpretable.
        formDao.findLatestPublishedByCode(definition.getCode())
                .filter(previous -> !previous.getId().equals(id))
                .ifPresent(previous -> {
                    previous.setStatus(FormDefinitionStatus.ARCHIVED);
                    formDao.save(previous);
                });

        definition.setStatus(FormDefinitionStatus.PUBLISHED);
        return formDao.save(definition);
    }

    @Override
    public FormDefinitionDTO createNewVersion(Long id) {
        requireExisting(id);
        return formDao.copyAsNewVersion(id)
                .orElseThrow(() -> new FormDefinitionNotFoundException(id));
    }

    @Override
    public void deleteById(Long id) {
        requireDraft(id);
        formDao.deleteById(id);
    }

    /**
     * A code is claimed by its whole version history, so {@code create} may only open a code nobody
     * has used. Adding to an existing code is {@link #createNewVersion(Long)}.
     */
    private void requireCodeIsFree(String code) {
        if (code != null && formDao.existsByCode(code)) {
            throw new DuplicateResourceException("Form", "code", code);
        }
    }

    private FormDefinitionDTO requireExisting(Long id) {
        return formDao.findById(id)
                .orElseThrow(() -> new FormDefinitionNotFoundException(id));
    }

    private FormDefinitionDTO requireDraft(Long id) {
        FormDefinitionDTO definition = requireExisting(id);
        if (definition.getStatus() != FormDefinitionStatus.DRAFT) {
            throw new FormDefinitionNotEditableException(id, definition.getStatus());
        }
        return definition;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FormDefinitionDTO> findById(Long id) {
        return formDao.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FormDefinitionDTO> findByCode(String code) {
        return formDao.findByCode(code);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FormDefinitionDTO> findByCodeAndVersion(String code, Integer version) {
        return formDao.findByCodeAndVersion(code, version);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FormDefinitionDTO> findLatestPublishedByCode(String code) {
        return formDao.findLatestPublishedByCode(code);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FormDefinitionDTO> findAllVersionsByCode(String code) {
        return formDao.findAllVersionsByCode(code);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FormDefinitionDTO> findByStatus(FormDefinitionStatus status) {
        return formDao.findByStatus(status);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FormDefinitionDTO> findAll() {
        return formDao.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByCode(String code) {
        return formDao.existsByCode(code);
    }
}
