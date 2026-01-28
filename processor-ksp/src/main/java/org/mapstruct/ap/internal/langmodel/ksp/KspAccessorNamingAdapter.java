/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.ksp;

import com.google.devtools.ksp.symbol.KSFunctionDeclaration;
import com.google.devtools.ksp.symbol.KSPropertyDeclaration;
import com.google.devtools.ksp.symbol.Modifier;

import org.mapstruct.ap.internal.langmodel.AccessorNamingAdapter;
import org.mapstruct.ap.internal.langmodel.descriptor.ElementDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.ExecutableDescriptor;
import org.mapstruct.ap.spi.AccessorNamingStrategy;
import org.mapstruct.ap.spi.MethodType;

import java.util.Set;

/**
 * Bridges descriptor-based accessor naming calls for KSP.
 * <p>
 * Note: The standard {@link AccessorNamingStrategy} SPI expects javax.lang.model elements,
 * which are not available in KSP. This adapter provides Kotlin-aware accessor naming
 * that handles Kotlin properties and their synthetic accessors correctly.
 */
final class KspAccessorNamingAdapter implements AccessorNamingAdapter {

    private final AccessorNamingStrategy delegate;

    KspAccessorNamingAdapter(AccessorNamingStrategy delegate) {
        this.delegate = delegate;
    }

    @Override
    public MethodType methodType(ExecutableDescriptor executable) {
        if ( executable == null ) {
            return null;
        }

        Object unwrapped = executable.unwrap();
        if ( !( unwrapped instanceof KSFunctionDeclaration ) ) {
            return null;
        }

        KSFunctionDeclaration function = (KSFunctionDeclaration) unwrapped;
        String name = function.getSimpleName().asString();

        // Handle Kotlin property accessors
        if ( isGetter( function, name ) ) {
            return MethodType.GETTER;
        }
        if ( isSetter( function, name ) ) {
            return MethodType.SETTER;
        }
        if ( isAdder( name ) ) {
            return MethodType.ADDER;
        }
        if ( isPresenceChecker( name ) ) {
            return MethodType.PRESENCE_CHECKER;
        }

        return MethodType.OTHER;
    }

    @Override
    public String propertyName(ExecutableDescriptor executable) {
        if ( executable == null ) {
            return null;
        }

        Object unwrapped = executable.unwrap();
        if ( !( unwrapped instanceof KSFunctionDeclaration ) ) {
            return null;
        }

        KSFunctionDeclaration function = (KSFunctionDeclaration) unwrapped;
        String name = function.getSimpleName().asString();

        // Handle getter patterns
        if ( name.startsWith( "get" ) && name.length() > 3 ) {
            return decapitalize( name.substring( 3 ) );
        }
        if ( name.startsWith( "is" ) && name.length() > 2 ) {
            return decapitalize( name.substring( 2 ) );
        }

        // Handle setter patterns
        if ( name.startsWith( "set" ) && name.length() > 3 ) {
            return decapitalize( name.substring( 3 ) );
        }

        // Handle adder patterns
        if ( name.startsWith( "add" ) && name.length() > 3 ) {
            return decapitalize( name.substring( 3 ) );
        }

        // Handle presence checker patterns
        if ( name.startsWith( "has" ) && name.length() > 3 ) {
            return decapitalize( name.substring( 3 ) );
        }

        return name;
    }

    @Override
    public String elementName(ElementDescriptor element) {
        if ( element == null ) {
            return null;
        }

        // For Kotlin properties, use the property name directly
        Object unwrapped = element.unwrap();
        if ( unwrapped instanceof KSPropertyDeclaration ) {
            return ( (KSPropertyDeclaration) unwrapped ).getSimpleName().asString();
        }

        if ( element instanceof ExecutableDescriptor ) {
            return propertyName( (ExecutableDescriptor) element );
        }

        return element.simpleName().content();
    }

    private boolean isGetter(KSFunctionDeclaration function, String name) {
        // No parameters, has return type
        if ( !function.getParameters().isEmpty() ) {
            return false;
        }
        if ( function.getReturnType() == null ) {
            return false;
        }

        // Check for void return type
        String returnTypeName = function.getReturnType().resolve().getDeclaration().getQualifiedName() != null
            ? function.getReturnType().resolve().getDeclaration().getQualifiedName().asString()
            : "";
        if ( "kotlin.Unit".equals( returnTypeName ) || "void".equals( returnTypeName ) ) {
            return false;
        }

        // Standard getter patterns
        if ( name.startsWith( "get" ) && name.length() > 3 ) {
            return true;
        }
        if ( name.startsWith( "is" ) && name.length() > 2 ) {
            // Check if return type is boolean
            return "kotlin.Boolean".equals( returnTypeName ) || "boolean".equals( returnTypeName );
        }

        return false;
    }

    private boolean isSetter(KSFunctionDeclaration function, String name) {
        // Exactly one parameter
        if ( function.getParameters().size() != 1 ) {
            return false;
        }

        // Check for void return type or Unit
        if ( function.getReturnType() != null ) {
            String returnTypeName = function.getReturnType().resolve().getDeclaration().getQualifiedName() != null
                ? function.getReturnType().resolve().getDeclaration().getQualifiedName().asString()
                : "";
            if ( !"kotlin.Unit".equals( returnTypeName ) && !"void".equals( returnTypeName ) ) {
                return false;
            }
        }

        return name.startsWith( "set" ) && name.length() > 3;
    }

    private boolean isAdder(String name) {
        return name.startsWith( "add" ) && name.length() > 3;
    }

    private boolean isPresenceChecker(String name) {
        return name.startsWith( "has" ) && name.length() > 3;
    }

    private String decapitalize(String name) {
        if ( name == null || name.isEmpty() ) {
            return name;
        }

        // Handle cases like "URL" -> "URL" (all caps)
        if ( name.length() > 1 && Character.isUpperCase( name.charAt( 0 ) )
            && Character.isUpperCase( name.charAt( 1 ) ) ) {
            return name;
        }

        char[] chars = name.toCharArray();
        chars[0] = Character.toLowerCase( chars[0] );
        return new String( chars );
    }
}
