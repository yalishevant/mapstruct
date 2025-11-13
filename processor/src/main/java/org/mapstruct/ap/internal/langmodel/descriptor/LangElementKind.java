/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.descriptor;

/**
 * Minimal set of element kinds required by MapStruct's processor pipeline.
 */
public enum LangElementKind {
    ANNOTATION_TYPE,
    CLASS,
    CONSTRUCTOR,
    ENUM,
    ENUM_CONSTANT,
    FIELD,
    INTERFACE,
    METHOD,
    PARAMETER,
    PACKAGE,
    RECORD,
    RECORD_COMPONENT;
}
