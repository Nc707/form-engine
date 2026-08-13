package com.nc.formengine.model.specification;

import com.nc.formengine.model.enums.FieldType;
import com.nc.formengine.model.enums.RestrictionType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Registry that maps restriction types to their applicable field types.
 * Used for filtering and validating which restrictions can be applied to which fields.
 *
 * <p>Nothing here is universal, and that is the point: a restriction judges the shape of an answer, and
 * shape is what a field type is. DATE, BOOLEAN, SELECT and MULTI_SELECT have no configurable rule at
 * all — everything true of their answers is already decided by the type itself, by the {@code required}
 * flag, or by the choices the field offers.
 */
public final class RestrictionTypeRegistry {

    private static final Map<RestrictionType, Set<FieldType>> APPLICABILITY_MAP;

    static {
        Map<RestrictionType, Set<FieldType>> map = new HashMap<>();

        // Text-specific restrictions
        map.put(RestrictionType.MIN_LENGTH, Set.of(FieldType.TEXT));
        map.put(RestrictionType.MAX_LENGTH, Set.of(FieldType.TEXT));
        map.put(RestrictionType.PATTERN, Set.of(FieldType.TEXT));
        map.put(RestrictionType.EMAIL, Set.of(FieldType.TEXT));
        
        // Number-specific restrictions
        map.put(RestrictionType.MIN_VALUE, Set.of(FieldType.NUMBER));
        map.put(RestrictionType.MAX_VALUE, Set.of(FieldType.NUMBER));
        
        APPLICABILITY_MAP = Collections.unmodifiableMap(map);
    }
    
    private RestrictionTypeRegistry() {
        // Utility class, prevent instantiation
    }
    
    /**
     * Gets the field types that a restriction type can be applied to.
     *
     * @param restrictionType the restriction type
     * @return set of applicable field types, or empty set if none
     */
    public static Set<FieldType> getApplicableFieldTypes(RestrictionType restrictionType) {
        return APPLICABILITY_MAP.getOrDefault(restrictionType, Collections.emptySet());
    }
    
    /**
     * Checks if a restriction type is applicable to a field type.
     *
     * @param restrictionType the restriction type to check
     * @param fieldType       the field type to check against
     * @return true if the restriction can be applied to the field type
     */
    public static boolean isApplicable(RestrictionType restrictionType, FieldType fieldType) {
        Set<FieldType> applicableTypes = APPLICABILITY_MAP.get(restrictionType);
        return applicableTypes != null && applicableTypes.contains(fieldType);
    }
    
    /**
     * Filters a set of restriction types to only those applicable to a given field type.
     *
     * @param fieldType the field type to filter for
     * @return set of restriction types applicable to the field type
     */
    public static Set<RestrictionType> getApplicableRestrictionTypes(FieldType fieldType) {
        return APPLICABILITY_MAP.entrySet().stream()
            .filter(entry -> entry.getValue().contains(fieldType))
            .map(Map.Entry::getKey)
            .collect(java.util.stream.Collectors.toSet());
    }
}
