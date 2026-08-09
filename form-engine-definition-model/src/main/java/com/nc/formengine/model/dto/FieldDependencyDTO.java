package com.nc.formengine.model.dto;

import com.nc.formengine.model.enums.DependencyCondition;
import com.nc.formengine.model.enums.DependencyEffect;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for a conditional dependency between two fields of the same form.
 * <p>
 * Reads as: when the value of {@code triggerField} satisfies {@code condition} against
 * {@code triggerValue}, apply {@code effect} to {@code dependentField}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldDependencyDTO {

    private Long id;

    /** The field the effect is applied to. */
    @NotNull
    private Long dependentFieldId;

    /** The field whose answer is compared against {@link #triggerValue}. */
    @NotNull
    private Long triggerFieldId;

    /** The comparison to apply to the trigger field's value. */
    @NotNull
    private DependencyCondition condition;

    /** What happens to the dependent field while the condition holds. */
    @NotNull
    private DependencyEffect effect;

    /**
     * The value the trigger field's answer is compared against. A blank value never satisfies a
     * condition.
     */
    private String triggerValue;
}
