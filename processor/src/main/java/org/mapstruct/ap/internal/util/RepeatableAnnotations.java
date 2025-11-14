/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.util;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.mapstruct.ap.internal.langmodel.api.LangElements;
import org.mapstruct.ap.internal.langmodel.api.PackageDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.ElementDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.LangElementKind;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeElementDescriptor;
import org.mapstruct.tools.gem.Gem;

/**
 * Descriptor-first helper for collecting repeatable annotation gems.
 *
 * @author Ben Zegveld
 */
public abstract class RepeatableAnnotations<SINGULAR extends Gem, MULTIPLE extends Gem, OPTIONS> {
    private static final String JAVA_LANG_ANNOTATION_PGK = "java.lang.annotation";
    private static final String ORG_MAPSTRUCT_PKG = "org.mapstruct";

    private final LangElements langElements;
    private final String singularFqn;
    private final String multipleFqn;

    protected RepeatableAnnotations(LangElements langElements, String singularFqn, String multipleFqn) {
        this.langElements = langElements;
        this.singularFqn = singularFqn;
        this.multipleFqn = multipleFqn;
    }

    /**
     * Creates a gem instance for the repeatable annotation found on the given element.
     *
     * @param element element hosting the annotation
     * @param annotation descriptor of the annotation
     * @return gem backing the single annotation
     */
    protected abstract SINGULAR singularInstanceOn(ElementDescriptor element, AnnotationDescriptor annotation);

    /**
     * Creates a container gem instance for the repeatable annotation found on the given element.
     *
     * @param element element hosting the annotation
     * @param annotation descriptor backing the container
     * @return gem for the container annotation
     */
    protected abstract MULTIPLE multipleInstanceOn(ElementDescriptor element, AnnotationDescriptor annotation);

    /**
     * @param gem single annotation gem being processed
     * @param annotation descriptor backing the gem
     * @param source originating element
     * @param mappings accumulator of processed options
     */
    protected abstract void addInstance(SINGULAR gem,
                                        AnnotationDescriptor annotation,
                                        ElementDescriptor source,
                                        Set<OPTIONS> mappings);

    /**
     * @param gems container gem being processed
     * @param annotation descriptor backing the container
     * @param source originating element
     * @param mappings accumulator of processed options
     */
    protected abstract void addInstances(MULTIPLE gems,
                                         AnnotationDescriptor annotation,
                                         ElementDescriptor source,
                                         Set<OPTIONS> mappings);

    /**
     * Retrieves the processed annotations for the supplied element.
     */
    public Set<OPTIONS> getProcessedAnnotations(ElementDescriptor source) {
        return getMappings( source, source, new LinkedHashSet<>(), new HashSet<>() );
    }

    private Set<OPTIONS> getMappings(ElementDescriptor source,
                                     ElementDescriptor element,
                                     LinkedHashSet<OPTIONS> mappingOptions,
                                     Set<String> handledElements) {

        for ( AnnotationDescriptor annotation : langElements.annotationMirrors( element ) ) {
            TypeElementDescriptor annotationType = annotation.annotationType();
            if ( annotationType == null ) {
                continue;
            }

            if ( isAnnotation( annotationType, singularFqn ) ) {
                SINGULAR mapping = singularInstanceOn( element, annotation );
                addInstance( mapping, annotation, source, mappingOptions );
            }
            else if ( isAnnotation( annotationType, multipleFqn ) ) {
                MULTIPLE mappings = multipleInstanceOn( element, annotation );
                addInstances( mappings, annotation, source, mappingOptions );
            }
            else if ( shouldRecurseInto( annotationType, handledElements ) ) {
                handledElements.add( annotationType.id() );
                getMappings( source, annotationType, mappingOptions, handledElements );
            }
        }

        return mappingOptions;
    }

    private boolean shouldRecurseInto(TypeElementDescriptor annotationType, Set<String> handledElements) {
        if ( annotationType.kind() != LangElementKind.ANNOTATION_TYPE ) {
            return false;
        }
        if ( handledElements.contains( annotationType.id() ) ) {
            return false;
        }

        PackageDescriptor descriptor = langElements.packageOf( annotationType );
        String packageName = descriptor != null ? descriptor.qualifiedName() : null;
        return packageName != null
            && !JAVA_LANG_ANNOTATION_PGK.equals( packageName )
            && !ORG_MAPSTRUCT_PKG.equals( packageName );
    }

    private boolean isAnnotation(TypeElementDescriptor element, String annotationFqn) {
        return annotationFqn.equals( element.qualifiedName() );
    }
}
