/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.ksp;

import com.google.devtools.ksp.symbol.ClassKind;
import com.google.devtools.ksp.symbol.KSClassDeclaration;
import com.google.devtools.ksp.symbol.KSDeclaration;
import com.google.devtools.ksp.symbol.KSType;
import com.google.devtools.ksp.symbol.KSTypeReference;
import com.google.devtools.ksp.symbol.Modifier;

import org.mapstruct.ap.internal.langmodel.descriptor.ElementDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.LangElementKind;
import org.mapstruct.ap.internal.langmodel.descriptor.LangModifier;
import org.mapstruct.ap.internal.langmodel.descriptor.NameDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeElementDescriptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * KSP implementation of {@link TypeElementDescriptor}.
 */
final class KspTypeElementDescriptor implements TypeElementDescriptor {

    private final KspDescriptorFactory factory;
    private final KSClassDeclaration declaration;
    private final String id;
    private final LangElementKind kind;
    private final Set<LangModifier> modifiers;

    KspTypeElementDescriptor(KspDescriptorFactory factory, KSClassDeclaration declaration) {
        this.factory = Objects.requireNonNull( factory, "factory" );
        this.declaration = Objects.requireNonNull( declaration, "declaration" );
        this.id = computeId( declaration );
        this.kind = KspKindMapper.mapElementKind( declaration );
        this.modifiers = KspKindMapper.mapModifiers( declaration.getModifiers() );
    }

    KSClassDeclaration declaration() {
        return declaration;
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
        return new KspNameDescriptor( declaration.getSimpleName() );
    }

    @Override
    public Optional<ElementDescriptor> enclosingElement() {
        KSDeclaration parent = declaration.getParentDeclaration();
        if ( parent == null ) {
            // Return package descriptor
            return Optional.ofNullable( factory.packageDescriptor( declaration ) );
        }
        return Optional.ofNullable( factory.elementDescriptor( parent ) );
    }

    @Override
    public Set<LangModifier> modifiers() {
        return modifiers;
    }

    @Override
    public TypeDescriptor asType() {
        KSType type = declaration.asStarProjectedType();
        // For a proper declared type with type parameters, we need to preserve them
        if ( !declaration.getTypeParameters().isEmpty() ) {
            type = declaration.asType( Collections.emptyList() );
        }
        return factory.typeDescriptor( type );
    }

    @Override
    public Object unwrap() {
        return declaration;
    }

    @Override
    public String qualifiedName() {
        if ( declaration.getQualifiedName() == null ) {
            return declaration.getSimpleName().asString();
        }
        return declaration.getQualifiedName().asString();
    }

    @Override
    public boolean isRecord() {
        // In Kotlin, data classes are similar to records
        return declaration.getModifiers().contains( Modifier.DATA );
    }

    @Override
    public boolean isSealed() {
        return declaration.getModifiers().contains( Modifier.SEALED );
    }

    @Override
    public List<TypeDescriptor> permittedSubclasses() {
        if ( !isSealed() ) {
            return Collections.emptyList();
        }

        List<KSClassDeclaration> sealedSubclasses = new ArrayList<>();
        for ( KSClassDeclaration subclass : KspSequenceUtils.toIterable( declaration.getSealedSubclasses() ) ) {
            sealedSubclasses.add( subclass );
        }

        if ( sealedSubclasses.isEmpty() ) {
            return Collections.emptyList();
        }

        List<TypeDescriptor> result = new ArrayList<>( sealedSubclasses.size() );
        for ( KSClassDeclaration subclass : sealedSubclasses ) {
            TypeDescriptor descriptor = factory.typeDescriptor( subclass.asStarProjectedType() );
            if ( descriptor != null ) {
                result.add( descriptor );
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
        if ( !( obj instanceof KspTypeElementDescriptor ) ) {
            return false;
        }
        KspTypeElementDescriptor other = (KspTypeElementDescriptor) obj;
        return Objects.equals( id, other.id );
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "KspTypeElementDescriptor[" + qualifiedName() + "]";
    }

    private String computeId(KSClassDeclaration decl) {
        if ( decl.getQualifiedName() != null ) {
            return "element:" + decl.getQualifiedName().asString();
        }
        return "element:" + decl.getSimpleName().asString();
    }
}
