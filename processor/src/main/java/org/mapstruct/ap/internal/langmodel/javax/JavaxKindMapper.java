/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.javax;

import javax.lang.model.element.ElementKind;
import javax.lang.model.type.TypeKind;

import org.mapstruct.ap.internal.util.IgnoreJRERequirement;
import org.mapstruct.ap.internal.langmodel.descriptor.LangElementKind;
import org.mapstruct.ap.internal.langmodel.descriptor.LangTypeKind;

@IgnoreJRERequirement
final class JavaxKindMapper {

    private JavaxKindMapper() {
    }

    @IgnoreJRERequirement
    static LangElementKind map(ElementKind kind) {
        String name = kind.name();
        switch ( name ) {
            case "ANNOTATION_TYPE":
                return LangElementKind.ANNOTATION_TYPE;
            case "CLASS":
                return LangElementKind.CLASS;
            case "CONSTRUCTOR":
                return LangElementKind.CONSTRUCTOR;
            case "ENUM":
                return LangElementKind.ENUM;
            case "ENUM_CONSTANT":
                return LangElementKind.ENUM_CONSTANT;
            case "FIELD":
                return LangElementKind.FIELD;
            case "INTERFACE":
                return LangElementKind.INTERFACE;
            case "METHOD":
                return LangElementKind.METHOD;
            case "PACKAGE":
                return LangElementKind.PACKAGE;
            case "RECORD":
                return LangElementKind.RECORD;
            case "RECORD_COMPONENT":
                return LangElementKind.RECORD_COMPONENT;
            default:
                throw new IllegalArgumentException( "Unsupported ElementKind: " + name );
        }
    }

    static LangTypeKind map(TypeKind kind) {
        switch ( kind ) {
            case ARRAY:
                return LangTypeKind.ARRAY;
            case BOOLEAN:
            case BYTE:
            case CHAR:
            case DOUBLE:
            case FLOAT:
            case INT:
            case LONG:
            case SHORT:
                return LangTypeKind.PRIMITIVE;
            case DECLARED:
                return LangTypeKind.DECLARED;
            case ERROR:
                return LangTypeKind.ERROR;
            case INTERSECTION:
                return LangTypeKind.INTERSECTION;
            case TYPEVAR:
                return LangTypeKind.TYPE_PARAMETER;
            case VOID:
                return LangTypeKind.VOID;
            case WILDCARD:
                return LangTypeKind.WILDCARD;
            default:
                throw new IllegalArgumentException( "Unsupported TypeKind: " + kind );
        }
    }
}
