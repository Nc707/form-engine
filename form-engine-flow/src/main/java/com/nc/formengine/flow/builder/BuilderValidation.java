package com.nc.formengine.flow.builder;

import com.nc.formengine.model.dto.FieldDefinitionDTO;
import com.nc.formengine.model.dto.FieldDependencyDTO;
import com.nc.formengine.model.enums.DependencyCondition;
import com.nc.formengine.model.enums.FieldType;
import com.nc.formengine.model.rules.DefinitionRules;

import java.util.List;

/**
 * What the builder refuses to save, asked before any service is called.
 *
 * <p>These rules are no longer the builder's own. They live in {@link DefinitionRules}, the business
 * layer enforces them, and this is where the editor asks the same questions early — so that a problem
 * is a message next to the field being edited rather than an exception after pressing save. The domain
 * is what makes them true; this only makes them timely.
 *
 * <p>That split matters for more than tidiness. While these lived here and nowhere else, every one of
 * them could be walked straight past through the REST API.
 */
final class BuilderValidation {

    private BuilderValidation() {
    }

    /**
     * Checks the details of a form about to be created.
     *
     * <p>The code is only checked for shape; whether it is free is a question for the service, and the
     * caller asks {@code existsByCode} separately.
     *
     * @return the problems found, empty when there are none
     */
    static List<String> validateNewForm(String code, String title) {
        return DefinitionRules.checkForm(code, title);
    }

    /**
     * Checks one field, together with the restrictions and options it carries.
     *
     * @param field    the field being saved
     * @param siblings the form's other fields, not including this one
     * @return the problems found, empty when there are none
     */
    static List<String> validateField(FieldDefinitionDTO field, List<FieldDefinitionDTO> siblings) {
        return DefinitionRules.checkField(field, siblings);
    }

    /**
     * Checks a conditional dependency against the fields it names.
     *
     * @param dependency the dependency being saved
     * @param fields     the form's fields as currently stored
     * @return the problems found, empty when there are none
     */
    static List<String> validateDependency(FieldDependencyDTO dependency,
                                           List<FieldDefinitionDTO> fields) {
        return DefinitionRules.checkDependency(dependency, fields);
    }

    /** The conditions worth offering for a trigger field of this type. */
    static List<DependencyCondition> conditionsFor(FieldType triggerType) {
        return DefinitionRules.conditionsFor(triggerType);
    }
}
