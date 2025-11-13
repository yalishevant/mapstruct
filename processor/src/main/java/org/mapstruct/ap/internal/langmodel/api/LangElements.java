/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.api;

import java.util.List;

import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.ElementDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.ExecutableDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.FieldDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.RecordComponentDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeElementDescriptor;

/**
 * Abstraction over compiler-specific element utilities.
 * <p>
 * The contract mirrors the behaviour MapStruct relies on today when interacting with {@code javax.lang.model}.
 * Each method is intentionally phrased in terms of descriptors so that back ends such as KSP/XProcessing can
 * implement the same semantics without leaking their native models.
 */
public interface LangElements {

    /**
     * Resolves a top-level or nested type by its canonical name.
     *
     * @param canonicalName fully qualified binary name
     * @return a descriptor for the type element or {@code null} if it cannot be located
     */
    TypeElementDescriptor typeElement(String canonicalName);

    /**
     * Determines the package descriptor that owns the supplied element.
     *
     * @param element descriptor of any element in the compilation model
     * @return package descriptor containing the element (never {@code null})
     */
    PackageDescriptor packageOf(ElementDescriptor element);

    /**
     * Checks whether {@code overrider} overrides {@code overridden} in the context of {@code type}.
     * The semantics must match {@link javax.lang.model.util.Elements#overrides} (i.e. visibility rules apply and
     * bridge methods are considered equivalent).
     */
    boolean overrides(ExecutableDescriptor overrider, ExecutableDescriptor overridden,
                      TypeElementDescriptor type);

    /**
     * Returns all annotation mirrors associated with the supplied element, including inherited annotations as defined
     * by the backing compiler.
     */
    List<AnnotationDescriptor> annotationMirrors(ElementDescriptor element);

    /**
     * Returns all executable members visible on the supplied type, including methods declared in super types and
     * implemented interfaces. Methods declared on {@link Object} and private members must be filtered out to match
     * the behaviour of MapStruct's existing {@code ElementUtils#getAllEnclosedExecutableElements} helper.
     */
    List<ExecutableDescriptor> enclosedExecutables(TypeElementDescriptor type);

    /**
     * Returns the constructors declared directly on the supplied type (super-type constructors are not included).
     */
    List<ExecutableDescriptor> constructors(TypeElementDescriptor type);

    /**
     * Returns field descriptors visible on the supplied type, honouring the same inheritance rules as
     * {@link #enclosedExecutables(TypeElementDescriptor)}.
     */
    List<FieldDescriptor> enclosedFields(TypeElementDescriptor type);

    /**
     * Returns record components declared directly on the supplied type element.
     */
    List<RecordComponentDescriptor> recordComponents(TypeElementDescriptor type);

}
