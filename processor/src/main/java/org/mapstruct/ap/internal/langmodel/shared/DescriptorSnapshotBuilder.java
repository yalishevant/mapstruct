/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.shared;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationValueDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.ElementDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.ExecutableDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.FieldDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.LangElementKind;
import org.mapstruct.ap.internal.langmodel.descriptor.LangModifier;
import org.mapstruct.ap.internal.langmodel.descriptor.ParameterDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.RecordComponentDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeElementDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.util.TypeDescriptorDisplay;
import org.mapstruct.ap.internal.langmodel.LangModelContext;
import org.mapstruct.ap.internal.langmodel.api.LangElements;

/**
 * Builds {@link DescriptorSnapshot} instances from the descriptor-oriented LangModel API.
 */
public final class DescriptorSnapshotBuilder {

    private final LangModelContext context;
    private final LangElements elements;

    public DescriptorSnapshotBuilder(LangModelContext context) {
        this.context = Objects.requireNonNull( context, "context" );
        this.elements = Objects.requireNonNull( context.elements(), "context.elements()" );
    }

    public DescriptorSnapshot snapshotForTypes(List<String> typeNames) {
        DescriptorSnapshot snapshot = new DescriptorSnapshot();
        for ( String typeName : typeNames ) {
            TypeElementDescriptor type = elements.typeElement( typeName );
            if ( type == null ) {
                throw new IllegalArgumentException( "Type not found: " + typeName );
            }
            snapshot.addType( buildTypeSnapshot( type ) );
        }
        return snapshot;
    }

    private TypeSnapshot buildTypeSnapshot(TypeElementDescriptor type) {
        List<AnnotationSnapshot> annotations = toAnnotationSnapshots( elements.annotationMirrors( type ) );
        List<FieldSnapshot> fields = buildFieldSnapshots( type, elements.enclosedFields( type ) );
        List<RecordComponentSnapshot> recordComponents = buildRecordComponentSnapshots( elements.recordComponents( type ) );
        List<ExecutableSnapshot> constructors = buildExecutableSnapshots(
            type,
            elements.constructors( type ),
            descriptor -> descriptor.kind() == LangElementKind.CONSTRUCTOR
        );
        List<ExecutableSnapshot> methods = buildExecutableSnapshots(
            type,
            elements.enclosedExecutables( type ),
            descriptor -> descriptor.kind() == LangElementKind.METHOD
        );

        List<String> permitted = type.permittedSubclasses()
            .stream()
            .map( DescriptorSnapshotBuilder::describeType )
            .sorted()
            .collect( Collectors.toUnmodifiableList() );

        String qualifiedName = type.qualifiedName();

        return new TypeSnapshot(
            qualifiedName,
            qualifiedName,
            type.kind().name(),
            normalizeTypeModifiers( type.modifiers() ),
            type.isRecord(),
            type.isSealed(),
            permitted,
            annotations,
            fields,
            recordComponents,
            constructors,
            methods
        );
    }

    private List<FieldSnapshot> buildFieldSnapshots(TypeElementDescriptor owner, List<FieldDescriptor> descriptors) {
        return descriptors.stream()
            .filter( descriptor -> isDeclaredIn( owner, descriptor ) )
            .sorted( Comparator.comparing( descriptor -> descriptor.simpleName().content() ) )
            .map( descriptor -> new FieldSnapshot(
                descriptor.simpleName().content(),
                descriptor.kind().name(),
                describeType( descriptor.fieldType() ),
                toSortedStrings( descriptor.modifiers() ),
                toAnnotationSnapshots( elements.annotationMirrors( descriptor ) )
            ) )
            .collect( Collectors.toUnmodifiableList() );
    }

    private List<RecordComponentSnapshot> buildRecordComponentSnapshots(List<RecordComponentDescriptor> descriptors) {
        return descriptors.stream()
            .sorted( Comparator.comparing( descriptor -> descriptor.simpleName().content() ) )
            .map( descriptor -> new RecordComponentSnapshot(
                descriptor.simpleName().content(),
                describeType( descriptor.componentType() ),
                toAnnotationSnapshots( elements.annotationMirrors( descriptor ) )
            ) )
            .collect( Collectors.toUnmodifiableList() );
    }

    private List<ExecutableSnapshot> buildExecutableSnapshots(TypeElementDescriptor owner,
                                                              List<ExecutableDescriptor> descriptors,
                                                              java.util.function.Predicate<ExecutableDescriptor> filter) {
        return descriptors.stream()
            .filter( filter )
            .filter( descriptor -> includeExecutable( owner, descriptor ) )
            .filter( descriptor -> isDeclaredIn( owner, descriptor ) )
            .sorted( Comparator.comparing( descriptor -> descriptor.simpleName().content() ) )
            .map( this::buildExecutableSnapshot )
            .collect( Collectors.toUnmodifiableList() );
    }

    private ExecutableSnapshot buildExecutableSnapshot(ExecutableDescriptor descriptor) {
        List<ParameterSnapshot> parameters = descriptor.parameters()
            .stream()
            .map( this::buildParameterSnapshot )
            .collect( Collectors.toCollection( ArrayList::new ) );

        // Preserve declaration order for parameters
        parameters = Collections.unmodifiableList( parameters );

        List<String> typeParameters = descriptor.typeParameters()
            .stream()
            .map( DescriptorSnapshotBuilder::describeType )
            .collect( Collectors.toUnmodifiableList() );

        List<String> thrownTypes = descriptor.thrownTypes()
            .stream()
            .map( DescriptorSnapshotBuilder::describeType )
            .sorted()
            .collect( Collectors.toUnmodifiableList() );

        return new ExecutableSnapshot(
            descriptor.simpleName().content(),
            descriptor.kind().name(),
            descriptor.kind() == LangElementKind.CONSTRUCTOR
                ? null
                : describeType( descriptor.returnType() ),
            parameters,
            typeParameters,
            thrownTypes,
            normalizeExecutableModifiers( descriptor.modifiers() ),
            descriptor.isDefault(),
            toAnnotationSnapshots( elements.annotationMirrors( descriptor ) )
        );
    }

    private ParameterSnapshot buildParameterSnapshot(ParameterDescriptor descriptor) {
        return new ParameterSnapshot(
            descriptor.simpleName().content(),
            describeType( descriptor.asType() ),
            descriptor.isVarArgs(),
            toSortedStrings( descriptor.modifiers() ),
            toAnnotationSnapshots( descriptor.annotations() )
        );
    }

    private static List<String> toSortedStrings(Set<LangModifier> modifiers) {
        return modifiers.stream()
            .map( LangModifier::name )
            .collect( Collectors.collectingAndThen( Collectors.toCollection( TreeSet::new ), List::copyOf ) );
    }

    private static List<String> normalizeTypeModifiers(Set<LangModifier> modifiers) {
        return modifiers.stream()
            .map( LangModifier::name )
            .filter( name -> !"FINAL".equals( name ) && !"PUBLIC".equals( name ) )
            .collect( Collectors.collectingAndThen( Collectors.toCollection( TreeSet::new ), List::copyOf ) );
    }

    private static List<String> normalizeExecutableModifiers(Set<LangModifier> modifiers) {
        return modifiers.stream()
            .map( LangModifier::name )
            .filter( name -> !"FINAL".equals( name ) && !"PUBLIC".equals( name ) )
            .collect( Collectors.collectingAndThen( Collectors.toCollection( TreeSet::new ), List::copyOf ) );
    }

    private static List<AnnotationSnapshot> toAnnotationSnapshots(List<AnnotationDescriptor> annotations) {
        return annotations.stream()
            .filter( DescriptorSnapshotBuilder::isRelevantAnnotation )
            .map( DescriptorSnapshotBuilder::toAnnotationSnapshot )
            .sorted( Comparator.comparing( AnnotationSnapshot::type ) )
            .collect( Collectors.toUnmodifiableList() );
    }

    private static List<AnnotationSnapshot> toAnnotationSnapshots(Set<AnnotationDescriptor> annotations) {
        return annotations.stream()
            .filter( DescriptorSnapshotBuilder::isRelevantAnnotation )
            .map( DescriptorSnapshotBuilder::toAnnotationSnapshot )
            .sorted( Comparator.comparing( AnnotationSnapshot::type ) )
            .collect( Collectors.toUnmodifiableList() );
    }

    private static AnnotationSnapshot toAnnotationSnapshot(AnnotationDescriptor annotation) {
        String typeName = Optional.ofNullable( annotation.annotationType() )
            .map( TypeElementDescriptor::qualifiedName )
            .orElse( "<unknown>" );

        Map<String, String> values = new LinkedHashMap<>();
        for ( Map.Entry<String, AnnotationValueDescriptor> entry : annotation.elementValues().entrySet() ) {
            values.put( entry.getKey(), describeAnnotationValue( entry.getValue() ) );
        }
        return new AnnotationSnapshot( typeName, Collections.unmodifiableMap( values ) );
    }

    private static String describeAnnotationValue(AnnotationValueDescriptor descriptor) {
        Object rawValue = descriptor.value();
        if ( rawValue == null ) {
            return "null";
        }
        if ( rawValue instanceof String ) {
            return '"' + rawValue.toString() + '"';
        }
        if ( rawValue instanceof Number || rawValue instanceof Boolean || rawValue instanceof Character ) {
            return rawValue.toString();
        }
        if ( rawValue instanceof Enum ) {
            Enum<?> enumValue = (Enum<?>) rawValue;
            return enumValue.getClass().getName() + "#" + enumValue.name();
        }

        TypeDescriptor typeValue = descriptor.asType();
        if ( typeValue != null ) {
            return describeType( typeValue );
        }

        List<AnnotationValueDescriptor> nestedValues = descriptor.asList();
        if ( !nestedValues.isEmpty() ) {
            return nestedValues.stream()
                .map( DescriptorSnapshotBuilder::describeAnnotationValue )
                .collect( Collectors.joining( ", ", "[", "]" ) );
        }

        AnnotationDescriptor nestedAnnotation = descriptor.asAnnotation();
        if ( nestedAnnotation != null ) {
            return toAnnotationSnapshot( nestedAnnotation ).toString();
        }

        return rawValue.toString();
    }

    private static String describeType(TypeDescriptor descriptor) {
        return TypeDescriptorDisplay.displayName( descriptor );
    }

    private static boolean isDeclaredIn(TypeElementDescriptor owner, ElementDescriptor descriptor) {
        return descriptor.enclosingElement()
            .map( enclosing -> isSameTypeElement( owner, enclosing ) )
            .orElseGet( () -> descriptor == owner );
    }

    private static boolean isSameTypeElement(TypeElementDescriptor owner, ElementDescriptor candidate) {
        if ( candidate == owner ) {
            return true;
        }
        if ( !( candidate instanceof TypeElementDescriptor ) ) {
            return false;
        }
        TypeElementDescriptor candidateType = (TypeElementDescriptor) candidate;
        return Objects.equals( owner.qualifiedName(), candidateType.qualifiedName() );
    }

    private static boolean isRelevantAnnotation(AnnotationDescriptor descriptor) {
        if ( descriptor == null ) {
            return false;
        }
        TypeElementDescriptor annotationType = descriptor.annotationType();
        if ( annotationType == null ) {
            return true;
        }
        String qualifiedName = annotationType.qualifiedName();
        return !"java.lang.Override".equals( qualifiedName );
    }

    private static boolean includeExecutable(TypeElementDescriptor owner, ExecutableDescriptor descriptor) {
        if ( owner.kind() == LangElementKind.ENUM ) {
            String name = descriptor.simpleName().content();
            if ( "values".equals( name ) || "valueOf".equals( name ) ) {
                return false;
            }
        }
        return true;
    }
}
