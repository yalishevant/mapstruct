/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.javax;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;

import org.mapstruct.ap.internal.langmodel.descriptor.ElementDescriptor;
import org.mapstruct.ap.internal.langmodel.api.LangTypes;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeElementDescriptor;

final class JavaxLangTypes implements LangTypes {

    private final JavaxLangModelContext context;
    private final JavaxDescriptorFactory factory;

    JavaxLangTypes(JavaxLangModelContext context,
                   JavaxDescriptorFactory factory) {
        this.context = Objects.requireNonNull( context );
        this.factory = Objects.requireNonNull( factory );
    }

    @Override
    public TypeDescriptor declaredType(TypeElementDescriptor element, List<TypeDescriptor> arguments) {
        TypeElement typeElement = unwrapTypeElement( element );
        List<TypeMirror> typeMirrors = new ArrayList<>( arguments.size() );
        for ( TypeDescriptor argument : arguments ) {
            typeMirrors.add( unwrap( argument ) );
        }
        DeclaredType declaredType = context.delegateTypeUtils().getDeclaredType( typeElement,
            typeMirrors.toArray( new TypeMirror[0] ) );
        return factory.typeDescriptor( declaredType );
    }

    @Override
    public TypeDescriptor erasure(TypeDescriptor type) {
        TypeMirror mirror = context.delegateTypeUtils().erasure( unwrap( type ) );
        return factory.typeDescriptor( mirror );
    }

    @Override
    public TypeDescriptor boxed(TypeDescriptor type) {
        TypeMirror mirror = unwrap( type );
        if ( mirror.getKind().isPrimitive() ) {
            TypeMirror boxed = context.delegateTypeUtils().boxedClass( (javax.lang.model.type.PrimitiveType) mirror )
                .asType();
            return factory.typeDescriptor( boxed );
        }
        return type;
    }

    @Override
    public TypeDescriptor unboxed(TypeDescriptor type) {
        TypeMirror mirror = unwrap( type );
        try {
            TypeMirror unboxed = context.delegateTypeUtils().unboxedType( mirror );
            return factory.typeDescriptor( unboxed );
        }
        catch ( IllegalArgumentException ex ) {
            return type;
        }
    }

    @Override
    public boolean isAssignable(TypeDescriptor source, TypeDescriptor target) {
        return context.delegateTypeUtils().isAssignable( unwrap( source ), unwrap( target ) );
    }

    @Override
    public boolean isSameType(TypeDescriptor a, TypeDescriptor b) {
        return context.delegateTypeUtils().isSameType( unwrap( a ), unwrap( b ) );
    }

    @Override
    public boolean isSubtype(TypeDescriptor sub, TypeDescriptor sup) {
        return context.delegateTypeUtils().isSubtype( unwrap( sub ), unwrap( sup ) );
    }

    @Override
    public boolean isSubtypeErased(TypeDescriptor sub, TypeDescriptor rawSuper) {
        return context.delegateTypeUtils().isSubtypeErased( unwrap( sub ), unwrap( rawSuper ) );
    }

    @Override
    public boolean contains(TypeDescriptor container, TypeDescriptor contained) {
        return context.delegateTypeUtils().contains( unwrap( container ), unwrap( contained ) );
    }

    @Override
    public List<TypeDescriptor> directSupertypes(TypeDescriptor type) {
        List<? extends TypeMirror> supertypes = context.delegateTypeUtils().directSupertypes( unwrap( type ) );
        if ( supertypes.isEmpty() ) {
            return Collections.emptyList();
        }
        List<TypeDescriptor> result = new ArrayList<>( supertypes.size() );
        for ( TypeMirror supertype : supertypes ) {
            TypeDescriptor descriptor = factory.typeDescriptor( supertype );
            if ( descriptor != null ) {
                result.add( descriptor );
            }
        }
        return Collections.unmodifiableList( result );
    }

    @Override
    public TypeDescriptor asMemberOf(TypeDescriptor containingType, ElementDescriptor member) {
        TypeMirror containingMirror = unwrap( containingType );
        javax.lang.model.element.Element element = JavaxElementUnwrapper.unwrapElement( member );
        TypeMirror result = context.delegateTypeUtils().asMemberOf( (DeclaredType) containingMirror, element );
        return factory.typeDescriptor( result );
    }

    @Override
    public TypeElementDescriptor asElement(TypeDescriptor type) {
        TypeMirror mirror = unwrap( type );
        javax.lang.model.element.Element element = context.delegateTypeUtils().asElement( mirror );
        if ( !( element instanceof TypeElement ) ) {
            return null;
        }
        return factory.typeElementDescriptor( element );
    }

    @Override
    public TypeDescriptor primitive(String primitiveName) {
        if ( "void".equals( primitiveName ) ) {
            return voidType();
        }
        javax.lang.model.type.TypeKind kind = javax.lang.model.type.TypeKind.valueOf( primitiveName.toUpperCase() );
        TypeMirror mirror = context.delegateTypeUtils().getPrimitiveType( kind );
        return factory.typeDescriptor( mirror );
    }

    @Override
    public TypeDescriptor voidType() {
        return factory.typeDescriptor( context.delegateTypeUtils().getNoType( javax.lang.model.type.TypeKind.VOID ) );
    }

    private TypeMirror unwrap(TypeDescriptor descriptor) {
        if ( descriptor instanceof JavaxTypeDescriptor ) {
            return ( (JavaxTypeDescriptor) descriptor ).mirror();
        }
        throw new IllegalArgumentException( "Unsupported TypeDescriptor implementation for javax backend" );
    }

    private TypeElement unwrapTypeElement(TypeElementDescriptor descriptor) {
        if ( descriptor instanceof JavaxTypeElementDescriptor ) {
            return ( (JavaxTypeElementDescriptor) descriptor ).element();
        }
        throw new IllegalArgumentException(
            "Unsupported TypeElementDescriptor implementation for javax backend" );
    }
}
