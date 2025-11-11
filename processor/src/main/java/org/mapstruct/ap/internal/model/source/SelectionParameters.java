/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.model.source;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.mapstruct.ap.internal.model.common.SourceRHS;
import org.mapstruct.ap.descriptor.TypeDescriptor;

/**
 * Holding parameters common to the selection process, common to IterableMapping, BeanMapping, PropertyMapping and
 * MapMapping
 *
 * @author Sjaak Derksen
 */
public class SelectionParameters {

    private static final SelectionParameters EMPTY = new SelectionParameters(
        Collections.emptyList(),
        Collections.emptyList(),
        Collections.emptyList(),
        Collections.emptyList(),
        null,
        null
    );

    private final List<TypeDescriptor> qualifiers;
    private final List<String> qualifyingNames;
    private final List<TypeDescriptor> conditionQualifiers;
    private final List<String> conditionQualifyingNames;
    private final TypeDescriptor resultType;
    private final SourceRHS sourceRHS;

    /**
     * Returns new selection parameters
     *
     * ResultType is not inherited.
     *
     * @param selectionParameters the selection parameters that need to be copied
     *
     * @return the selection parameters based on the given ones
     */
    public static SelectionParameters forInheritance(SelectionParameters selectionParameters) {
        return withoutResultType( selectionParameters );
    }

    public static SelectionParameters withoutResultType(SelectionParameters selectionParameters) {
        return new SelectionParameters(
            selectionParameters.qualifiers,
            selectionParameters.qualifyingNames,
            selectionParameters.conditionQualifiers,
            selectionParameters.conditionQualifyingNames,
            null
        );
    }

    public SelectionParameters(List<TypeDescriptor> qualifiers, List<String> qualifyingNames,
                               TypeDescriptor resultType) {
        this(
            qualifiers,
            qualifyingNames,
            Collections.emptyList(),
            Collections.emptyList(),
            resultType,
            null,
            false
        );
    }

    public SelectionParameters(List<TypeDescriptor> qualifiers, List<String> qualifyingNames,
                               List<TypeDescriptor> conditionQualifiers, List<String> conditionQualifyingNames,
                               TypeDescriptor resultType) {
        this(
            qualifiers,
            qualifyingNames,
            conditionQualifiers,
            conditionQualifyingNames,
            resultType,
            null,
            false
        );
    }

    public SelectionParameters(List<TypeDescriptor> qualifiers, List<String> qualifyingNames,
                               List<TypeDescriptor> conditionQualifiers, List<String> conditionQualifyingNames,
                               TypeDescriptor resultType,
                               SourceRHS sourceRHS) {
        this(
            qualifiers,
            qualifyingNames,
            conditionQualifiers,
            conditionQualifyingNames,
            resultType,
            sourceRHS,
            false
        );
    }

    private SelectionParameters(List<TypeDescriptor> qualifiers, List<String> qualifyingNames,
                                List<TypeDescriptor> conditionQualifiers, List<String> conditionQualifyingNames,
                                TypeDescriptor resultType,
                                SourceRHS sourceRHS,
                                boolean unused) {
        this.qualifiers = qualifiers;
        this.qualifyingNames = qualifyingNames;
        this.conditionQualifiers = conditionQualifiers;
        this.conditionQualifyingNames = conditionQualifyingNames;
        this.resultType = resultType;
        this.sourceRHS = sourceRHS;
    }

    /**
     *
     * @return qualifiers used for further select the appropriate mapping method based on class and name
     */
    public List<TypeDescriptor> getQualifiers() {
        return qualifiers;
    }

    /**
     *
     * @return qualifyingNames see qualifiers, used in combination with with @Named
     */
    public List<String> getQualifyingNames() {
        return qualifyingNames;
    }

    /**
     * @return qualifiers used for further select the appropriate presence check method based on class and name
     */
    public List<TypeDescriptor> getConditionQualifiers() {
        return conditionQualifiers;
    }

    /**
     * @return qualifyingNames, used in combination with with @Named
     * @see #getConditionQualifiers()
     */
    public List<String> getConditionQualifyingNames() {
        return conditionQualifyingNames;
    }

    /**
     *
     * @return resultType used for further select the appropriate mapping method based on resultType (bean mapping)
     * targetType (Iterable- and MapMapping)
     */
    public TypeDescriptor getResultType() {
        return resultType;
    }

    /**
     * @return sourceRHS used for further selection of an appropriate factory method
     */
    public SourceRHS getSourceRHS() {
        return sourceRHS;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 97 * hash + (this.qualifyingNames != null ? this.qualifyingNames.hashCode() : 0);
        hash = 97 * hash + (this.resultType != null ? this.resultType.id().hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if ( this == obj ) {
            return true;
        }
        if ( obj == null ) {
            return false;
        }
        if ( getClass() != obj.getClass() ) {
            return false;
        }
        final SelectionParameters other = (SelectionParameters) obj;

        if ( !equals( this.qualifiers, other.qualifiers ) ) {
            return false;
        }

        if ( !Objects.equals( this.qualifyingNames, other.qualifyingNames ) ) {
            return false;
        }

        if ( !equals( this.conditionQualifiers, other.conditionQualifiers ) ) {
            return false;
        }

        if ( !Objects.equals( this.conditionQualifyingNames, other.conditionQualifyingNames ) ) {
            return false;
        }

        if ( !Objects.equals( this.sourceRHS, other.sourceRHS ) ) {
            return false;
        }

        return equals( this.resultType, other.resultType );
    }

    private boolean equals(List<TypeDescriptor> descriptors1, List<TypeDescriptor> descriptors2) {
        if ( descriptors1 == null ) {
            return descriptors2 == null;
        }
        else if ( descriptors2 == null || descriptors1.size() != descriptors2.size() ) {
            return false;
        }

        for ( int i = 0; i < descriptors1.size(); i++ ) {
            if ( !equals( descriptors1.get( i ), descriptors2.get( i ) ) ) {
                return false;
            }
        }

        return true;
    }

    private boolean equals(TypeDescriptor descriptor1, TypeDescriptor descriptor2) {
        if ( descriptor1 == descriptor2 ) {
            return true;
        }
        if ( descriptor1 == null || descriptor2 == null ) {
            return false;
        }
        return Objects.equals( descriptor1.id(), descriptor2.id() );
    }

    public SelectionParameters withSourceRHS(SourceRHS sourceRHS) {
        return new SelectionParameters(
            this.qualifiers,
            this.qualifyingNames,
            this.conditionQualifiers,
            this.conditionQualifyingNames,
            null,
            sourceRHS,
            false
        );
    }

    public static SelectionParameters forSourceRHS(SourceRHS sourceRHS) {
        return new SelectionParameters(
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            null,
            sourceRHS,
            false
        );
    }

    public static SelectionParameters empty() {
        return EMPTY;
    }

}
