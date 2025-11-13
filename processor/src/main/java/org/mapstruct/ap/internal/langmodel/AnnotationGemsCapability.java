/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel;

/**
 * Optional capability providing {@link AnnotationGemFactory} support.
 */
public interface AnnotationGemsCapability {

    /**
     * @return annotation gem factory bound to the backing implementation.
     */
    AnnotationGemFactory annotationGems();
}
