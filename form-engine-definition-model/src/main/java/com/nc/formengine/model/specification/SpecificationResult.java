package com.nc.formengine.model.specification;

import lombok.Data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Result of a specification evaluation.
 * Contains whether the specification was satisfied and any error messages.
 */
@Data
public class SpecificationResult {
    
    private final boolean satisfied;
    private final List<String> reasons;
    
    private SpecificationResult(boolean satisfied, List<String> reasons) {
        this.satisfied = satisfied;
        this.reasons = new ArrayList<>(reasons);
    }
    
    /**
     * Creates a satisfied result.
     *
     * @return a satisfied specification result
     */
    public static SpecificationResult satisfied() {
        return new SpecificationResult(true, Collections.emptyList());
    }
    
    /**
     * Creates a not satisfied result with a single reason.
     *
     * @param reason the reason why the specification was not satisfied
     * @return a not satisfied specification result
     */
    public static SpecificationResult notSatisfied(String reason) {
        return new SpecificationResult(false, Collections.singletonList(reason));
    }
    
    /**
     * Creates a not satisfied result with multiple reasons.
     *
     * @param reasons the reasons why the specification was not satisfied
     * @return a not satisfied specification result
     */
    public static SpecificationResult notSatisfied(List<String> reasons) {
        return new SpecificationResult(false, new ArrayList<>(reasons));
    }
    
    /**
     * Checks if the specification was satisfied.
     *
     * @return true if satisfied, false otherwise
     */
    public boolean isSatisfied() {
        return satisfied;
    }
    
    /**
     * Gets all error messages if not satisfied.
     *
     * @return list of error messages
     */
    public List<String> getReasons() {
        return Collections.unmodifiableList(reasons);
    }
}
