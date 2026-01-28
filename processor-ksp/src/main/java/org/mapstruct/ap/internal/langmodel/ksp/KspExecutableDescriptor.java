/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.ksp;

import com.google.devtools.ksp.symbol.KSDeclaration;
import com.google.devtools.ksp.symbol.KSFunctionDeclaration;
import com.google.devtools.ksp.symbol.KSType;
import com.google.devtools.ksp.symbol.KSTypeParameter;
import com.google.devtools.ksp.symbol.KSTypeReference;
import com.google.devtools.ksp.symbol.KSValueParameter;

import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationValueDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.ElementDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.ExecutableDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.LangElementKind;
import org.mapstruct.ap.internal.langmodel.descriptor.LangModifier;
import org.mapstruct.ap.internal.langmodel.descriptor.NameDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.ParameterDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * KSP implementation of {@link ExecutableDescriptor}.
 */
final class KspExecutableDescriptor implements ExecutableDescriptor {

    private final KspDescriptorFactory factory;
    private final KSFunctionDeclaration function;
    private final String id;
    private final LangElementKind kind;
    private final Set<LangModifier> modifiers;

    KspExecutableDescriptor(KspDescriptorFactory factory, KSFunctionDeclaration function) {
        this.factory = Objects.requireNonNull( factory, "factory" );
        this.function = Objects.requireNonNull( function, "function" );
        this.id = computeId( function );
        this.kind = KspKindMapper.mapElementKind( function );
        // Use function-specific modifier mapping to handle implicit abstract/public in interfaces
        this.modifiers = KspKindMapper.mapModifiers( function );
    }

    KSFunctionDeclaration function() {
        return function;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public LangElementKind kind() {
        return kind;
    }

    @Override
    public NameDescriptor simpleName() {
        return new KspNameDescriptor( function.getSimpleName() );
    }

    @Override
    public Optional<ElementDescriptor> enclosingElement() {
        KSDeclaration parent = function.getParentDeclaration();
        if ( parent == null ) {
            return Optional.empty();
        }
        return Optional.ofNullable( factory.elementDescriptor( parent ) );
    }

    @Override
    public Set<LangModifier> modifiers() {
        return modifiers;
    }

    @Override
    public TypeDescriptor asType() {
        // Return the function's return type as the "type" of this executable
        KSTypeReference returnTypeRef = function.getReturnType();
        if ( returnTypeRef == null ) {
            return null;
        }
        return factory.typeDescriptor( returnTypeRef.resolve() );
    }

    @Override
    public Object unwrap() {
        return function;
    }

    @Override
    public List<ParameterDescriptor> parameters() {
        List<KSValueParameter> params = function.getParameters();
        if ( params.isEmpty() ) {
            return Collections.emptyList();
        }

        List<ParameterDescriptor> result = new ArrayList<>( params.size() );
        for ( KSValueParameter param : params ) {
            result.add( new KspParameterDescriptor( factory, param ) );
        }
        return Collections.unmodifiableList( result );
    }

    @Override
    public TypeDescriptor returnType() {
        KSTypeReference returnTypeRef = function.getReturnType();
        if ( returnTypeRef == null ) {
            // Constructors don't have an explicit return type
            KSDeclaration parent = function.getParentDeclaration();
            if ( parent != null ) {
                return factory.typeDescriptor( parent );
            }
            return null;
        }
        return factory.typeDescriptor( returnTypeRef.resolve() );
    }

    @Override
    public List<TypeDescriptor> thrownTypes() {
        // Kotlin doesn't have checked exceptions, but @Throws annotation can be used
        // For now, return empty list - can be enhanced to parse @Throws annotations
        return Collections.emptyList();
    }

    @Override
    public boolean isDefault() {
        return KspKindMapper.isDefaultMethod( function );
    }

    @Override
    public boolean isVarArgs() {
        List<KSValueParameter> params = function.getParameters();
        if ( params.isEmpty() ) {
            return false;
        }
        // Check if the last parameter is vararg
        KSValueParameter lastParam = params.get( params.size() - 1 );
        return lastParam.isVararg();
    }

    @Override
    public AnnotationValueDescriptor defaultValue() {
        // Kotlin doesn't have annotation method default values in the same way as Java
        // This is primarily used for annotation elements
        return null;
    }

    @Override
    public List<TypeDescriptor> typeParameters() {
        List<KSTypeParameter> typeParams = function.getTypeParameters();
        if ( typeParams.isEmpty() ) {
            return Collections.emptyList();
        }

        List<TypeDescriptor> result = new ArrayList<>( typeParams.size() );
        for ( KSTypeParameter typeParam : typeParams ) {
            // Type parameters are represented as types
            KSTypeReference firstBound = KspSequenceUtils.firstOrNull( typeParam.getBounds() );
            KSType paramType = firstBound != null ? firstBound.resolve() : null;
            if ( paramType != null ) {
                TypeDescriptor descriptor = factory.typeDescriptor( paramType );
                if ( descriptor != null ) {
                    result.add( descriptor );
                }
            }
        }
        return Collections.unmodifiableList( result );
    }

    @Override
    public int compareTo(ElementDescriptor other) {
        return simpleName().content().compareTo( other.simpleName().content() );
    }

    @Override
    public boolean equals(Object obj) {
        if ( this == obj ) {
            return true;
        }
        if ( !( obj instanceof KspExecutableDescriptor ) ) {
            return false;
        }
        KspExecutableDescriptor other = (KspExecutableDescriptor) obj;
        return Objects.equals( id, other.id );
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "KspExecutableDescriptor[" + simpleName().content() + "]";
    }

    private String computeId(KSFunctionDeclaration func) {
        StringBuilder builder = new StringBuilder( "method:" );

        // Include enclosing type
        KSDeclaration parent = func.getParentDeclaration();
        if ( parent != null && parent.getQualifiedName() != null ) {
            builder.append( parent.getQualifiedName().asString() ).append( "#" );
        }

        builder.append( func.getSimpleName().asString() );

        // Include parameter types for overload distinction
        builder.append( "(" );
        List<KSValueParameter> params = func.getParameters();
        for ( int i = 0; i < params.size(); i++ ) {
            if ( i > 0 ) {
                builder.append( "," );
            }
            KSTypeReference typeRef = params.get( i ).getType();
            if ( typeRef != null ) {
                KSType type = typeRef.resolve();
                if ( type.getDeclaration() != null && type.getDeclaration().getQualifiedName() != null ) {
                    builder.append( type.getDeclaration().getQualifiedName().asString() );
                }
                else {
                    builder.append( type.toString() );
                }
            }
        }
        builder.append( ")" );

        return builder.toString();
    }
}
