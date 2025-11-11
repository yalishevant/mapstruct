/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.processor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.mapstruct.ap.internal.gem.MappingConstantsGem;
import org.mapstruct.ap.internal.model.Annotation;
import org.mapstruct.ap.internal.model.Decorator;
import org.mapstruct.ap.internal.model.Mapper;
import org.mapstruct.ap.internal.model.common.Type;
import org.mapstruct.ap.internal.model.annotation.AnnotationElement;
import org.mapstruct.ap.internal.model.annotation.AnnotationElement.AnnotationElementType;
import org.mapstruct.ap.descriptor.AnnotationDescriptor;
import org.mapstruct.ap.langmodel.api.LangElements;
import org.mapstruct.ap.langmodel.api.PackageDescriptor;
import org.mapstruct.ap.descriptor.TypeElementDescriptor;

/**
 * A {@link ModelElementProcessor} which converts the given {@link Mapper}
 * object into a Spring bean in case Spring is configured as the
 * target component model for this mapper.
 *
 * @author Gunnar Morling
 * @author Andreas Gudian
 */
public class SpringComponentProcessor extends AnnotationBasedComponentModelProcessor {

    private static final String SPRING_COMPONENT_ANNOTATION = "org.springframework.stereotype.Component";
    private static final String SPRING_PRIMARY_ANNOTATION = "org.springframework.context.annotation.Primary";

    @Override
    protected String getComponentModelIdentifier() {
        return MappingConstantsGem.ComponentModelGem.SPRING;
    }

    @Override
    protected List<Annotation> getTypeAnnotations(Mapper mapper) {
        List<Annotation> typeAnnotations = new ArrayList<>();
        if ( !isAlreadyAnnotatedAsSpringStereotype( mapper ) ) {
            typeAnnotations.add( component() );
        }

        if ( mapper.getDecorator() != null ) {
            typeAnnotations.add( qualifierDelegate() );
        }

        return typeAnnotations;
    }

    /**
     * Returns the annotations that need to be added to the generated decorator, filtering out any annotations
     * that are already present or represented as meta-annotations.
     *
     * @param decorator the decorator to process
     * @return A list of annotations that should be added to the generated decorator.
     */
    @Override
    protected List<Annotation> getDecoratorAnnotations(Decorator decorator) {
        Set<String> desiredAnnotationNames = new LinkedHashSet<>();
        desiredAnnotationNames.add( SPRING_COMPONENT_ANNOTATION );
        desiredAnnotationNames.add( SPRING_PRIMARY_ANNOTATION );
        List<Annotation> decoratorAnnotations = decorator.getAnnotations();
        if ( !decoratorAnnotations.isEmpty() ) {
            Set<String> handledAnnotationTypes = new HashSet<>();
            for ( Annotation annotation : decoratorAnnotations ) {
                removeAnnotationsPresentOnElement(
                    annotation.getType(),
                    desiredAnnotationNames,
                    handledAnnotationTypes
                );
                if ( desiredAnnotationNames.isEmpty() ) {
                    // If all annotations are removed, we can stop further processing
                    return Collections.emptyList();
                }
            }
        }

        return desiredAnnotationNames.stream()
            .map( this::createAnnotation )
            .collect( Collectors.toList() );
    }

    @Override
    protected List<Annotation> getMapperReferenceAnnotations() {
        return Collections.singletonList(
            autowired()
        );
    }

    @Override
    protected List<Annotation> getDelegatorReferenceAnnotations(Mapper mapper) {
        return Arrays.asList(
            autowired(),
            qualifierDelegate()
        );
    }

    @Override
    protected boolean requiresGenerationOfDecoratorClass() {
        return true;
    }

    private Annotation createAnnotation(String canonicalName) {
        return new Annotation( getTypeFactory().getType( canonicalName ) );
    }

    private Annotation autowired() {
        return createAnnotation( "org.springframework.beans.factory.annotation.Autowired" );
    }

    private Annotation qualifierDelegate() {
        return new Annotation(
            getTypeFactory().getType( "org.springframework.beans.factory.annotation.Qualifier" ),
            Collections.singletonList(
                           new AnnotationElement(
                               AnnotationElementType.STRING,
                               Collections.singletonList( "delegate" )
                           ) ) );
    }

    private Annotation component() {
        return createAnnotation( SPRING_COMPONENT_ANNOTATION );
    }

    private boolean isAlreadyAnnotatedAsSpringStereotype(Mapper mapper) {
        Set<String> desiredAnnotationNames = new LinkedHashSet<>();
        desiredAnnotationNames.add( SPRING_COMPONENT_ANNOTATION );

        List<Annotation> mapperAnnotations = mapper.getAnnotations();
        if ( !mapperAnnotations.isEmpty() ) {
            Set<String> handledAnnotationTypes = new HashSet<>();
            for ( Annotation annotation : mapperAnnotations ) {
                removeAnnotationsPresentOnElement(
                    annotation.getType(),
                    desiredAnnotationNames,
                    handledAnnotationTypes
                );
                if ( desiredAnnotationNames.isEmpty() ) {
                    // If all annotations are removed, we can stop further processing
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Removes all the annotations and meta-annotations from {@code annotations} which are present on the given
     * annotation type.
     *
     * @param type            the annotation type to inspect
     * @param annotations             the annotations to check for
     * @param handledAnnotationTypes  set of already handled annotation type names to avoid infinite recursion
     */
    private void removeAnnotationsPresentOnElement(Type type, Set<String> annotations,
                                                   Set<String> handledAnnotationTypes) {
        if ( annotations.isEmpty() || type == null ) {
            return;
        }
        TypeElementDescriptor annotationType = type.getTypeElementDescriptor();
        if ( annotationType == null ) {
            return;
        }

        removeAnnotationsPresentOnDescriptor( annotationType, annotations, handledAnnotationTypes );
    }

    private void removeAnnotationsPresentOnDescriptor(TypeElementDescriptor annotationType,
                                                      Set<String> annotations,
                                                      Set<String> handledAnnotationTypes) {
        if ( annotations.isEmpty() || annotationType == null ) {
            return;
        }
        String qualifiedName = annotationType.qualifiedName();
        if ( qualifiedName == null || !handledAnnotationTypes.add( qualifiedName ) ) {
            return;
        }

        if ( annotations.remove( qualifiedName ) && annotations.isEmpty() ) {
            return;
        }

        LangElements elements = langElements();
        for ( AnnotationDescriptor metaAnnotation : elements.annotationMirrors( annotationType ) ) {
            TypeElementDescriptor metaType = metaAnnotation.annotationType();
            if ( metaType == null || isAnnotationInPackage( metaType, "java.lang.annotation" ) ) {
                continue;
            }
            removeAnnotationsPresentOnDescriptor( metaType, annotations, handledAnnotationTypes );
            if ( annotations.isEmpty() ) {
                return;
            }
        }
    }

    private boolean isAnnotationInPackage(TypeElementDescriptor annotationType, String packageFQN) {
        PackageDescriptor packageDescriptor = langElements().packageOf( annotationType );
        return packageDescriptor != null && packageFQN.equals( packageDescriptor.qualifiedName() );
    }

    private LangElements langElements() {
        return getProcessorContext().getLangModelContext().elementQuery().elements();
    }
}
