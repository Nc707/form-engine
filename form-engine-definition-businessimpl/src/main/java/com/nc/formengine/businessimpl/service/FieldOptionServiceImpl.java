package com.nc.formengine.businessimpl.service;

import com.nc.formengine.business.service.FieldOptionService;
import com.nc.formengine.data.dao.FieldOptionDao;
import com.nc.formengine.model.dto.FieldOptionDTO;
import com.nc.formengine.model.exception.FieldOptionNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class FieldOptionServiceImpl implements FieldOptionService {

    private final FieldOptionDao fieldOptionDao;

    @Override
    public FieldOptionDTO create(FieldOptionDTO fieldOptionDTO) {
        if (fieldOptionDTO.getId() != null) {
            throw new IllegalArgumentException("New field option should not have an ID");
        }
        return fieldOptionDao.save(fieldOptionDTO);
    }

    @Override
    public FieldOptionDTO update(Long id, FieldOptionDTO fieldOptionDTO) {
        // Existence check: throws if the id is unknown.
        fieldOptionDao.findById(id)
                .orElseThrow(() -> new FieldOptionNotFoundException(id));
        
        fieldOptionDTO.setId(id);
        return fieldOptionDao.save(fieldOptionDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<FieldOptionDTO> findById(Long id) {
        return fieldOptionDao.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FieldOptionDTO> findByFieldDefinitionId(Long fieldDefinitionId) {
        return fieldOptionDao.findByFieldDefinitionId(fieldDefinitionId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FieldOptionDTO> findAll() {
        return fieldOptionDao.findAll();
    }

    @Override
    public void deleteById(Long id) {
        fieldOptionDao.deleteById(id);
    }
}
