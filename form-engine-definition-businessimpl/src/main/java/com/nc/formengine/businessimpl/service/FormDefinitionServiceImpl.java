package com.nc.formengine.businessimpl.service;

import com.nc.formengine.business.service.FormDefinitionService;
import com.nc.formengine.data.dao.FormDao;
import com.nc.formengine.model.dto.FormDefinitionDTO;
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
        return formDao.save(formDefinitionDTO);
    }

    @Override
    public FormDefinitionDTO update(Long id, FormDefinitionDTO formDefinitionDTO) {
        FormDefinitionDTO existing = formDao.findById(id)
                .orElseThrow(() -> new RuntimeException("Form not found with id: " + id));
        
        formDefinitionDTO.setId(id);
        return formDao.save(formDefinitionDTO);
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
