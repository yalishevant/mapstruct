/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.ksp;

import com.google.devtools.ksp.symbol.KSAnnotation;
import com.google.devtools.ksp.symbol.KSDeclaration;
import com.google.devtools.ksp.symbol.KSPropertyDeclaration;
import com.google.devtools.ksp.symbol.KSType;
import com.google.devtools.ksp.symbol.KSTypeReference;
import com.google.devtools.ksp.symbol.Modifier;

import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.ElementDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.FieldDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.LangElementKind;
import org.mapstruct.ap.internal.langmodel.descriptor.LangModifier;
import org.mapstruct.ap.internal.langmodel.descriptor.NameDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * KSP implementation of {@link FieldDescriptor} for Kotlin properties.
 */
final class KspFieldDescriptor implements FieldDescriptor {

    private final KspDescriptorFactory factory;
    private final KSPropertyDeclaration property;
    private final String id;
    private final Set<LangModifier> modifiers;

    KspFieldDescriptor(KspDescriptorFactory factory, KSPropertyDeclaration property) {
        this.factory = Objects.requireNonNull( factory, "factory" );
        this.property = Objects.requireNonNull( property, "property" );
        this.id = computeId( property );
        this.modifiers = KspKindMapper.mapModifiers( property.getModifiers() );
    }

    KSPropertyDeclaration property() {
        return property;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public LangElementKind kind() {
        return LangElementKind.FIELD;
    }

    @Override
    public NameDescriptor simpleName() {
        return new KspNameDescriptor( property.getSimpleName() );
    }

    @Override
    public Optional<ElementDescriptor> enclosingElement() {
        KSDeclaration parent = property.getParentDeclaration();
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
        return fieldType();
    }

    @Override
    public Object unwrap() {
        return property;
    }

    @Override
    public TypeDescriptor fieldType() {
        KSTypeReference typeRef = property.getType();
        if ( typeRef == null ) {
            return null;
        }
        return factory.typeDescriptor( typeRef.resolve() );
    }

    @Override
    public boolean isStatic() {
        // In Kotlin, properties in companion objects or top-level are similar to static
        // Check if it's a top-level property or in a companion object
        KSDeclaration parent = property.getParentDeclaration();
        if ( parent == null ) {
            // Top-level property - similar to static
            return true;
        }
        // Check for @JvmStatic annotation or companion object context
        for ( KSAnnotation annotation : KspSequenceUtils.toIterable( property.getAnnotations() ) ) {
            if ( annotation.getAnnotationType().resolve().getDeclaration() != null ) {
                String annotationName = annotation.getAnnotationType().resolve()
                    .getDeclaration().getQualifiedName() != null
                    ? annotation.getAnnotationType().resolve().getDeclaration().getQualifiedName().asString()
                    : "";
                if ( "kotlin.jvm.JvmStatic".equals( annotationName ) ) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean isFinal() {
        // In Kotlin, val is final, var is not
        return !property.isMutable();
    }

    @Override
    public List<AnnotationDescriptor> annotations() {
        List<KSAnnotation> annotations = new ArrayList<>();
        for ( KSAnnotation annotation : KspSequenceUtils.toIterable( property.getAnnotations() ) ) {
            annotations.add( annotation );
        }

        if ( annotations.isEmpty() ) {
            return Collections.emptyList();
        }

        List<AnnotationDescriptor> result = new ArrayList<>( annotations.size() );
        for ( KSAnnotation annotation : annotations ) {
            AnnotationDescriptor descriptor = factory.annotationDescriptor( annotation );
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
        if ( !( obj instanceof KspFieldDescriptor ) ) {
            return false;
        }
        KspFieldDescriptor other = (KspFieldDescriptor) obj;
        return Objects.equals( id, other.id );
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "KspFieldDescriptor[" + simpleName().content() + "]";
    }

    private String computeId(KSPropertyDeclaration prop) {
        StringBuilder builder = new StringBuilder( "field:" );

        KSDeclaration parent = prop.getParentDeclaration();
        if ( parent != null && parent.getQualifiedName() != null ) {
            builder.append( parent.getQualifiedName().asString() ).append( "#" );
        }

        builder.append( prop.getSimpleName().asString() );
        return builder.toString();
    }
}
