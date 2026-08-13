package com.nc.formengine.businessimpl.service;

import com.nc.formengine.business.service.FieldOptionService;
import com.nc.formengine.data.dao.FieldOptionDao;
import com.nc.formengine.model.dto.FieldOptionDTO;
import com.nc.formengine.model.exception.DuplicateResourceException;
import com.nc.formengine.model.exception.FieldOptionNotFoundException;
import com.nc.formengine.model.exception.ValidationFailedException;
import com.nc.formengine.model.validation.AnswerCodec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The choices a select field offers.
 *
 * <p>These are what the engine checks a select answer against, so they are not free-form: two options
 * sharing a value make one of them unreachable, and a value containing the multi-select separator could
 * never be told apart from two values.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class FieldOptionServiceImpl implements FieldOptionService {

    private final FieldOptionDao fieldOptionDao;
    private final DefinitionMutationGuard guard;

    @Override
    public FieldOptionDTO create(FieldOptionDTO fieldOptionDTO) {
        if (fieldOptionDTO.getId() != null) {
            throw new IllegalArgumentException("New field option should not have an ID");
        }
        guard.requireDraftOfField(fieldOptionDTO.getFieldDefinitionId());
        requireUsableValue(fieldOptionDTO);
        return fieldOptionDao.save(fieldOptionDTO);
    }

    @Override
    public FieldOptionDTO update(Long id, FieldOptionDTO fieldOptionDTO) {
        FieldOptionDTO stored = fieldOptionDao.findById(id)
                .orElseThrow(() -> new FieldOptionNotFoundException(id));
        guard.requireDraftOfField(stored.getFieldDefinitionId());

        fieldOptionDTO.setId(id);
        // An option belongs to the field whose answers reference it, and cannot be moved.
        fieldOptionDTO.setFieldDefinitionId(stored.getFieldDefinitionId());
        requireUsableValue(fieldOptionDTO);
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
        fieldOptionDao.findById(id)
                .ifPresent(stored -> guard.requireDraftOfField(stored.getFieldDefinitionId()));
        fieldOptionDao.deleteById(id);
    }

    private void requireUsableValue(FieldOptionDTO option) {
        String value = option.getValue();
        if (value != null && value.contains(AnswerCodec.SELECTION_SEPARATOR)) {
            throw new ValidationFailedException("The option value is not usable as an answer",
                    "An option value cannot contain '" + AnswerCodec.SELECTION_SEPARATOR
                            + "': it is what separates the answers of a multi-select.");
        }

        // Only a genuinely different row counts. Comparing ids straight would treat two nulls as equal
        // and let a create walk past the option it collides with.
        boolean valueTaken = value != null
                && fieldOptionDao.findByFieldDefinitionId(option.getFieldDefinitionId()).stream()
                        .filter(other -> option.getId() == null
                                || !Objects.equals(other.getId(), option.getId()))
                        .anyMatch(other -> value.equals(other.getValue()));
        if (valueTaken) {
            // The renderer resolves an option by value and takes the first match, so the second one
            // could never be chosen or read back.
            throw new DuplicateResourceException("Field option", "value", value);
        }
    }
}
