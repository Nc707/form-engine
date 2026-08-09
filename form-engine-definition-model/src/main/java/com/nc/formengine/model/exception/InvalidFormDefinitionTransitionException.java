package com.nc.formengine.model.exception;

import com.nc.formengine.model.enums.FormDefinitionStatus;

/**
 * A lifecycle move that the {@link FormDefinitionStatus} graph does not allow, such as publishing a
 * definition that is already published. Reported as HTTP 409.
 */
public class InvalidFormDefinitionTransitionException extends FormEngineException {

    private final FormDefinitionStatus from;
    private final FormDefinitionStatus to;

    public InvalidFormDefinitionTransitionException(Long formDefinitionId,
                                                    FormDefinitionStatus from,
                                                    FormDefinitionStatus to) {
        super("Form definition " + formDefinitionId + " cannot go from " + from + " to " + to + ".");
        this.from = from;
        this.to = to;
    }

    public FormDefinitionStatus getFrom() {
        return from;
    }

    public FormDefinitionStatus getTo() {
        return to;
    }
}
