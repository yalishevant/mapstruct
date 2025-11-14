/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.util;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.mapstruct.ap.internal.langmodel.descriptor.ElementDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.ExecutableDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.FieldDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.LangModifier;
import org.mapstruct.ap.internal.langmodel.descriptor.RecordComponentDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;
import org.mapstruct.ap.internal.util.accessor.Accessor;
import org.mapstruct.ap.internal.util.accessor.ElementAccessor;
import org.mapstruct.ap.internal.util.accessor.ReadAccessor;

import static org.mapstruct.ap.internal.util.accessor.AccessorType.ADDER;
import static org.mapstruct.ap.internal.util.accessor.AccessorType.SETTER;

/**
 * Filter helpers operating on descriptor collections.
 *
 * @author Gunnar Morling
 * @author Filip Hrisafov
 * @author Anton Yalyshev
 */
public class Filters {

    private final AccessorNamingUtils accessorNaming;
    private final Function<ExecutableDescriptor, TypeDescriptor> returnTypeResolver;
    private final Function<FieldDescriptor, TypeDescriptor> fieldTypeResolver;
    private final Function<RecordComponentDescriptor, TypeDescriptor> recordComponentTypeResolver;
    private final Function<ExecutableDescriptor, TypeDescriptor> singleParameterResolver;

    public Filters(AccessorNamingUtils accessorNaming,
                   Function<ExecutableDescriptor, TypeDescriptor> returnTypeResolver,
                   Function<FieldDescriptor, TypeDescriptor> fieldTypeResolver,
                   Function<RecordComponentDescriptor, TypeDescriptor> recordComponentTypeResolver,
                   Function<ExecutableDescriptor, TypeDescriptor> singleParameterResolver) {
        this.accessorNaming = accessorNaming;
        this.returnTypeResolver = returnTypeResolver;
        this.fieldTypeResolver = fieldTypeResolver;
        this.recordComponentTypeResolver = recordComponentTypeResolver;
        this.singleParameterResolver = singleParameterResolver;
    }

    public List<ReadAccessor> getterMethodsIn(List<ExecutableDescriptor> elements) {
        if ( elements.isEmpty() ) {
            return Collections.emptyList();
        }
        return elements.stream()
            .filter( accessorNaming::isGetterMethod )
            .map( method -> ReadAccessor.fromGetter(
                method,
                resolveReturnType( method )
            ) )
            .collect( Collectors.toCollection( LinkedList::new ) );
    }

    public Map<String, ReadAccessor> recordAccessorsIn(Collection<RecordComponentDescriptor> recordComponents) {
        if ( recordComponents.isEmpty() ) {
            return Collections.emptyMap();
        }
        Map<String, ReadAccessor> recordAccessors = new LinkedHashMap<>();
        for ( RecordComponentDescriptor recordComponent : recordComponents ) {
            recordAccessors.put(
                recordComponent.simpleName().content(),
                ReadAccessor.fromRecordComponent(
                    recordComponent,
                    resolveRecordComponentType( recordComponent )
                )
            );
        }

        return recordAccessors;
    }

    public <T> List<T> fieldsIn(List<FieldDescriptor> accessors,
                                BiFunction<ElementDescriptor, TypeDescriptor, T> creator) {
        if ( accessors.isEmpty() ) {
            return Collections.emptyList();
        }
        return accessors.stream()
            .filter( Filters::isFieldAccessor )
            .map( field -> creator.apply( field, resolveFieldType( field ) ) )
            .collect( Collectors.toCollection( LinkedList::new ) );
    }

    public List<ExecutableDescriptor> presenceCheckMethodsIn(List<ExecutableDescriptor> elements) {
        if ( elements.isEmpty() ) {
            return Collections.emptyList();
        }
        return elements.stream()
            .filter( accessorNaming::isPresenceCheckMethod )
            .collect( Collectors.toCollection( LinkedList::new ) );
    }

    public List<Accessor> setterMethodsIn(List<ExecutableDescriptor> elements) {
        if ( elements.isEmpty() ) {
            return Collections.emptyList();
        }
        return elements.stream()
            .filter( accessorNaming::isSetterMethod )
            .map( method -> new ElementAccessor(
                method,
                resolveSingleParameterType( method ),
                SETTER
            ) )
            .collect( Collectors.toCollection( LinkedList::new ) );
    }

    public List<Accessor> adderMethodsIn(List<ExecutableDescriptor> elements) {
        if ( elements.isEmpty() ) {
            return Collections.emptyList();
        }
        return elements.stream()
            .filter( accessorNaming::isAdderMethod )
            .map( method -> new ElementAccessor(
                method,
                resolveSingleParameterType( method ),
                ADDER
            ) )
            .collect( Collectors.toCollection( LinkedList::new ) );
    }

    private static boolean isFieldAccessor(FieldDescriptor field) {
        return field != null
            && field.modifiers().contains( LangModifier.PUBLIC )
            && !field.isStatic();
    }

    private TypeDescriptor resolveReturnType(ExecutableDescriptor method) {
        if ( returnTypeResolver != null ) {
            TypeDescriptor resolved = returnTypeResolver.apply( method );
            if ( resolved != null ) {
                return resolved;
            }
        }
        return method.returnType();
    }

    private TypeDescriptor resolveRecordComponentType(RecordComponentDescriptor recordComponent) {
        if ( recordComponentTypeResolver != null ) {
            TypeDescriptor resolved = recordComponentTypeResolver.apply( recordComponent );
            if ( resolved != null ) {
                return resolved;
            }
        }
        TypeDescriptor fallback = recordComponent.componentType();
        return fallback != null ? fallback : recordComponent.asType();
    }

    private TypeDescriptor resolveFieldType(FieldDescriptor field) {
        if ( fieldTypeResolver != null ) {
            TypeDescriptor resolved = fieldTypeResolver.apply( field );
            if ( resolved != null ) {
                return resolved;
            }
        }
        return field.fieldType();
    }

    private TypeDescriptor resolveSingleParameterType(ExecutableDescriptor method) {
        if ( singleParameterResolver != null ) {
            TypeDescriptor resolved = singleParameterResolver.apply( method );
            if ( resolved != null ) {
                return resolved;
            }
        }
        return method.parameters().isEmpty()
            ? null
            : method.parameters().get( 0 ).type();
    }
}
