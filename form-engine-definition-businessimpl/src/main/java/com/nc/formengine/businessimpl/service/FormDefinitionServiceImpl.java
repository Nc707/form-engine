package com.nc.formengine.businessimpl.service;

import com.nc.formengine.business.service.FormDefinitionService;
import com.nc.formengine.data.dao.FormDao;
import com.nc.formengine.model.dto.FormDefinitionDTO;
import com.nc.formengine.model.exception.DuplicateResourceException;
import com.nc.formengine.model.exception.FormDefinitionNotFoundException;
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
        requireCodeIsFree(formDefinitionDTO.getCode(), null);
        return formDao.save(formDefinitionDTO);
    }

    @Override
    public FormDefinitionDTO update(Long id, FormDefinitionDTO formDefinitionDTO) {
        // Existence check: throws if the id is unknown.
        formDao.findById(id)
                .orElseThrow(() -> new FormDefinitionNotFoundException(id));

        requireCodeIsFree(formDefinitionDTO.getCode(), id);

        formDefinitionDTO.setId(id);
        return formDao.save(formDefinitionDTO);
    }

    /**
     * Form codes are the public handle for a form, so they have to stay unique. On update the form
     * being edited is allowed to keep its own code, hence {@code selfId}.
     */
    private void requireCodeIsFree(String code, Long selfId) {
        if (code == null) {
            return;
        }
        formDao.findByCode(code)
                .filter(existing -> !existing.getId().equals(selfId))
                .ifPresent(existing -> {
                    throw new DuplicateResourceException("Form", "code", code);
                });
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
    public List<FormDefinitionDTO> findAll() {
        return formDao.findAll();
    }

    @Override
    public void deleteById(Long id) {
        formDao.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByCode(String code) {
        return formDao.existsByCode(code);
    }
}
