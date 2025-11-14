/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.spi;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;

/**
 * Indicates a type was visited whose hierarchy was erroneous, because it has a non-existing super-type.
 * <p>
 * This exception can be used to signal the MapStruct processor to postpone the generation of the mappers to the next
 * round
 *
 * @author Gunnar Morling
 */
public class TypeHierarchyErroneousException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final TypeDescriptor type;
    private final TypeMirror typeMirror;

    public TypeHierarchyErroneousException() {
        this( (TypeMirror) null );
    }

    public TypeHierarchyErroneousException(TypeDescriptor type) {
        this.type = type;
        this.typeMirror = null;
    }

    public TypeHierarchyErroneousException(TypeElement element) {
        this( element != null ? element.asType() : null );
    }

    public TypeHierarchyErroneousException(TypeMirror typeMirror) {
        this.type = null;
        this.typeMirror = typeMirror;
    }

    public TypeDescriptor getType() {
        return type;
    }

    public TypeMirror getTypeMirror() {
        return typeMirror;
    }
}
