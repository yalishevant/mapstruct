/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.descriptor.util;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.mapstruct.ap.internal.langmodel.descriptor.LangTypeKind;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeElementDescriptor;

/**
 * Utility for computing human-readable representations of {@link TypeDescriptor} instances without
 * relying on backend-specific mirrors.
 */
public final class TypeDescriptorDisplay {

    private TypeDescriptorDisplay() {
    }

    /**
     * Returns a language-neutral string representation of the given descriptor suitable for diagnostics
     * and logging. The result mirrors the output we'd obtain from {@code TypeMirror#toString()} for javac.
     *
     * @param descriptor descriptor to format
     * @return human-readable type representation
     */
    public static String displayName(TypeDescriptor descriptor) {
        return displayNameInternal( descriptor, 0 );
    }

    private static String displayNameInternal(TypeDescriptor descriptor, int depth) {
        if ( descriptor == null ) {
            return "<null>";
        }

        if ( descriptor.isVoid() || descriptor.kind() == LangTypeKind.VOID ) {
            return "void";
        }

        LangTypeKind kind = descriptor.kind();
        switch ( kind ) {
            case PRIMITIVE:
                return primitiveName( descriptor );
            case ARRAY:
                return arrayName( descriptor, depth );
            case DECLARED:
            case ERROR:
                return declaredName( descriptor, depth );
            case TYPE_PARAMETER:
                return typeParameterName( descriptor );
            case WILDCARD:
                return wildcardName( descriptor, depth );
            case INTERSECTION:
                return intersectionName( descriptor, depth );
            default:
                return fallbackName( descriptor );
        }
    }

    private static String primitiveName(TypeDescriptor descriptor) {
        return stripPrefix( descriptor.id() );
    }

    private static String arrayName(TypeDescriptor descriptor, int depth) {
        Optional<TypeDescriptor> component = descriptor.componentType();
        if ( component.isEmpty() ) {
            return fallbackName( descriptor );
        }
        return displayNameInternal( component.get(), depth + 1 ) + "[]";
    }

    private static String declaredName(TypeDescriptor descriptor, int depth) {
        String baseName = descriptor.qualifiedName()
            .or( () -> descriptor.typeElement().map( TypeDescriptorDisplay::elementQualifiedName ) )
            .orElseGet( () -> stripPrefix( descriptor.id() ) );

        List<TypeDescriptor> arguments = descriptor.typeArguments();
        if ( arguments == null || arguments.isEmpty() ) {
            return baseName;
        }

        String generics = arguments.stream()
            .map( argument -> displayNameInternal( argument, depth + 1 ) )
            .collect( Collectors.joining( ", " ) );
        return baseName + "<" + generics + ">";
    }

    private static String typeParameterName(TypeDescriptor descriptor) {
        return descriptor.typeVariableName()
            .filter( name -> !name.isBlank() )
            .orElseGet( () -> fallbackName( descriptor ) );
    }

    private static String wildcardName(TypeDescriptor descriptor, int depth) {
        Optional<TypeDescriptor> extendsBound = descriptor.wildcardExtendsBound();
        if ( extendsBound.isPresent() ) {
            return "? extends " + displayNameInternal( extendsBound.get(), depth + 1 );
        }

        Optional<TypeDescriptor> superBound = descriptor.wildcardSuperBound();
        if ( superBound.isPresent() ) {
            return "? super " + displayNameInternal( superBound.get(), depth + 1 );
        }

        return "?";
    }

    private static String intersectionName(TypeDescriptor descriptor, int depth) {
        List<TypeDescriptor> bounds = descriptor.typeVariableBounds();
        if ( bounds == null || bounds.isEmpty() ) {
            return fallbackName( descriptor );
        }

        return bounds.stream()
            .map( bound -> displayNameInternal( bound, depth + 1 ) )
            .collect( Collectors.joining( " & " ) );
    }

    private static String fallbackName(TypeDescriptor descriptor) {
        return sanitizeSegments( stripPrefix( Objects.toString( descriptor.id(), "" ) ) );
    }

    private static String stripPrefix(String id) {
        if ( id == null || id.isEmpty() ) {
            return "";
        }
        int separator = id.indexOf( ':' );
        String result = separator >= 0 && separator + 1 < id.length() ? id.substring( separator + 1 ) : id;
        return sanitizeSegments( result );
    }

    private static String sanitizeSegments(String value) {
        if ( value == null || value.isEmpty() ) {
            return value;
        }
        String sanitized = value
            .replace( "declared:", "" )
            .replace( "primitive:", "" )
            .replace( "array:", "" )
            .replace( "wildcard:", "" )
            .replace( "extends=", "extends " )
            .replace( "super=", "super " )
            .replace( "intersection:", "" )
            .replace( "typevar:", "" );
        return sanitized.trim();
    }

    private static String elementQualifiedName(TypeElementDescriptor descriptor) {
        if ( descriptor == null ) {
            return "";
        }
        String qualified = descriptor.qualifiedName();
        return qualified != null && !qualified.isEmpty()
            ? qualified
            : descriptor.simpleName().content();
    }
}
