/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.descriptor;

/**
 * Language-neutral classification of type kinds. Mirrors the concepts provided by javax.lang.model
 * while remaining detached from a particular compiler API.
 */
public enum LangTypeKind {

    ARRAY,
    DECLARED,
    ERROR,
    INTERSECTION,
    PRIMITIVE,
    TYPE_PARAMETER,
    VOID,
    WILDCARD;
}
