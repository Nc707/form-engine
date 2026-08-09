package com.nc.formengine.businessimpl.service;

import com.nc.formengine.business.service.FieldDependencyService;
import com.nc.formengine.data.dao.FieldDependencyDao;
import com.nc.formengine.model.dto.FieldDependencyDTO;
import com.nc.formengine.model.exception.FieldDependencyNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class FieldDependencyServiceImpl implements FieldDependencyService {

    private final FieldDependencyDao fieldDependencyDao;

    @Override
    public FieldDependencyDTO create(FieldDependencyDTO fieldDependencyDTO) {
        if (fieldDependencyDTO.getId() != null) {
            throw new IllegalArgumentException("New field dependency should not have an ID");
        }
        return fieldDependencyDao.save(fieldDependencyDTO);
    }

    @Override
    public FieldDependencyDTO update(Long id, FieldDependencyDTO fieldDependencyDTO) {
        // Existence check: throws if the id is unknown.
        fieldDependencyDao.findById(id)
                .orElseThrow(() -> new FieldDependencyNotFoundException(id));
        
        fieldDependencyDTO.setId(id);
        return fieldDependencyDao.save(fieldDependencyDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FieldDependencyDTO> findById(Long id) {
        return fieldDependencyDao.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FieldDependencyDTO> findByTriggerFieldId(Long triggerFieldId) {
        return fieldDependencyDao.findByTriggerFieldId(triggerFieldId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FieldDependencyDTO> findAll() {
        return fieldDependencyDao.findAll();
    }

    @Override
    public void deleteById(Long id) {
        fieldDependencyDao.deleteById(id);
    }
}
