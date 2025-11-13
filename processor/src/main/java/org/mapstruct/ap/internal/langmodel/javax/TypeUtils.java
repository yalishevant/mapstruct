/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.javax;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;

import org.mapstruct.ap.internal.version.VersionInformation;

public interface TypeUtils extends Types {

     static TypeUtils create(ProcessingEnvironment processingEnvironment, VersionInformation info ) {
        if ( info.isEclipseJDTCompiler() ) {
            return new EclipseTypeUtilsDecorator( processingEnvironment );
        }
        else {
            return new JavacTypeUtilsDecorator( processingEnvironment );
        }
    }

    /**
     * Determines whether the first type is a subtype of the second type when considering only the erased signatures.
     *
     * @param t1 candidate subtype
     * @param t2 candidate super type
     * @return {@code true} if {@code t1} is a subtype of {@code t2} after erasure
     */
    boolean isSubtypeErased(TypeMirror t1, TypeMirror t2);
}
