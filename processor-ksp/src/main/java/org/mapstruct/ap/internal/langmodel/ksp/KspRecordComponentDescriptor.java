/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.ksp;

import com.google.devtools.ksp.symbol.KSAnnotation;
import com.google.devtools.ksp.symbol.KSPropertyDeclaration;
import com.google.devtools.ksp.symbol.KSTypeReference;
import com.google.devtools.ksp.symbol.KSValueParameter;

import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.ElementDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.LangElementKind;
import org.mapstruct.ap.internal.langmodel.descriptor.LangModifier;
import org.mapstruct.ap.internal.langmodel.descriptor.NameDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.RecordComponentDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * KSP implementation of {@link RecordComponentDescriptor} for Kotlin data class primary constructor parameters.
 */
final class KspRecordComponentDescriptor implements RecordComponentDescriptor {

    private final KspDescriptorFactory factory;
    private final KSValueParameter parameter;
    private final String id;

    KspRecordComponentDescriptor(KspDescriptorFactory factory, KSValueParameter parameter) {
        this.factory = Objects.requireNonNull( factory, "factory" );
        this.parameter = Objects.requireNonNull( parameter, "parameter" );
        this.id = computeId( parameter );
    }

    KSValueParameter parameter() {
        return parameter;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public LangElementKind kind() {
        return LangElementKind.RECORD_COMPONENT;
    }

    @Override
    public NameDescriptor simpleName() {
        return new KspNameDescriptor( parameter.getName() );
    }

    @Override
    public Optional<ElementDescriptor> enclosingElement() {
        // Would need parent tracking - for now return empty
        return Optional.empty();
    }

    @Override
    public Set<LangModifier> modifiers() {
        return Collections.emptySet();
    }

    @Override
    public TypeDescriptor asType() {
        return componentType();
    }

    @Override
    public Object unwrap() {
        return parameter;
    }

    @Override
    public TypeDescriptor componentType() {
        KSTypeReference typeRef = parameter.getType();
        if ( typeRef == null ) {
            return null;
        }
        return factory.typeDescriptor( typeRef.resolve() );
    }

    @Override
    public List<AnnotationDescriptor> annotations() {
        List<KSAnnotation> annotations = new ArrayList<>();
        for ( KSAnnotation annotation : KspSequenceUtils.toIterable( parameter.getAnnotations() ) ) {
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
        if ( !( obj instanceof KspRecordComponentDescriptor ) ) {
            return false;
        }
        KspRecordComponentDescriptor other = (KspRecordComponentDescriptor) obj;
        return Objects.equals( id, other.id );
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "KspRecordComponentDescriptor[" + simpleName().content() + "]";
    }

    private String computeId(KSValueParameter param) {
        String name = param.getName() != null ? param.getName().asString() : "unnamed";
        return "recordComponent:" + name;
    }
}
