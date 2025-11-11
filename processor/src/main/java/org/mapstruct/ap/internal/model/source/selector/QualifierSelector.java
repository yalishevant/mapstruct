/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.model.source.selector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.mapstruct.ap.internal.model.common.Type;
import org.mapstruct.ap.internal.model.common.TypeFactory;
import org.mapstruct.ap.internal.model.source.Method;
import org.mapstruct.ap.internal.model.source.SourceMethod;
import org.mapstruct.ap.internal.util.AnnotationDescriptorUtils;
import org.mapstruct.ap.internal.util.AnnotationValueUtils;
import org.mapstruct.ap.descriptor.AnnotationDescriptor;
import org.mapstruct.ap.langmodel.api.LangElements;
import org.mapstruct.ap.langmodel.api.LangTypes;
import org.mapstruct.ap.descriptor.TypeDescriptor;
import org.mapstruct.ap.descriptor.TypeElementDescriptor;

/**
 * This selector selects a best match based on qualifier annotations.
 * <p>
 * A method is said to be marked with a qualifier annotation if the class in which it resides is annotated with a
 * qualifier annotation or if the method itself is annotated with a qualifier annotation or both.
 * <p>
 * Rules:
 * <ol>
 * <li>If no qualifiers are requested in the selection criteria, then only candidate methods without any qualifier
 * annotations remain in the list of potential candidates</li>
 * <li>If multiple qualifiers (qualifedBy) are specified, then all of them need to be present at a candidate for it to
 * match.</li>
 * <li>If no candidate matches the required qualifiers, then all candidates are returned.</li>
 * </ol>
 *
 * @author Sjaak Derksen
 */
public class QualifierSelector implements MethodSelector {

    private static final String MAPSTRUCT_QUALIFIER = "org.mapstruct.Qualifier";

    private final LangTypes langTypes;
    private final LangElements langElements;
    private final TypeFactory typeFactory;
    private final TypeDescriptor namedAnnotationType;

    public QualifierSelector(TypeFactory typeFactory) {
        this.langTypes = typeFactory.langTypes();
        this.langElements = typeFactory.langElements();
        this.typeFactory = typeFactory;
        this.namedAnnotationType = resolveNamedAnnotationType();
    }

    @Override
    public <T extends Method> List<SelectedMethod<T>> getMatchingMethods(List<SelectedMethod<T>> methods,
                                                                         SelectionContext context) {
        SelectionCriteria criteria = context.getSelectionCriteria();

        int numberOfQualifiersToMatch = 0;

        List<TypeDescriptor> qualifierTypes = new ArrayList<>();
        if ( criteria.getQualifiers() != null ) {
            qualifierTypes.addAll( criteria.getQualifiers() );
            numberOfQualifiersToMatch += criteria.getQualifiers().size();
        }

        List<String> qualifiedByNames = new ArrayList<>();
        if ( criteria.getQualifiedByNames() != null ) {
            qualifiedByNames.addAll( criteria.getQualifiedByNames() );
            numberOfQualifiersToMatch += criteria.getQualifiedByNames().size();
        }

        if ( !qualifiedByNames.isEmpty() && namedAnnotationType != null ) {
            qualifierTypes.add( namedAnnotationType );
        }

        if ( qualifierTypes.isEmpty() ) {
            List<SelectedMethod<T>> nonQualifierAnnotatedMethods = new ArrayList<>( methods.size() );
            for ( SelectedMethod<T> candidate : methods ) {

                if ( candidate.getMethod() instanceof SourceMethod ) {
                    Set<AnnotationDescriptor> qualifierAnnotations = getQualifierAnnotations( candidate.getMethod() );
                    if ( qualifierAnnotations.isEmpty() ) {
                        nonQualifierAnnotatedMethods.add( candidate );
                    }
                }
                else {
                    nonQualifierAnnotatedMethods.add( candidate );
                }

            }
            return nonQualifierAnnotatedMethods;
        }
        else {
            List<SelectedMethod<T>> matches = new ArrayList<>( methods.size() );
            for ( SelectedMethod<T> candidate : methods ) {

                if ( !( candidate.getMethod() instanceof SourceMethod ) ) {
                    continue;
                }

                Set<AnnotationDescriptor> qualifierAnnotations = getQualifierAnnotations( candidate.getMethod() );
                int matchingQualifierCounter = 0;

                for ( AnnotationDescriptor qualifierAnnotation : qualifierAnnotations ) {
                    TypeDescriptor annotationType = qualifierAnnotation.annotationType().asType();
                    for ( TypeDescriptor qualifierType : qualifierTypes ) {
                        if ( isSameType( qualifierType, annotationType ) ) {
                            if ( isNamedAnnotation( annotationType ) ) {
                                String namedValue = namedValue( qualifierAnnotation );
                                if ( namedValue != null && qualifiedByNames.contains( namedValue ) ) {
                                    matchingQualifierCounter++;
                                }
                            }
                            else {
                                matchingQualifierCounter++;
                            }
                            break;
                        }
                    }
                }

                if ( matchingQualifierCounter == numberOfQualifiersToMatch ) {
                    matches.add( candidate );
                }
            }
            return matches;
        }
    }

    private boolean isSameType(TypeDescriptor left, TypeDescriptor right) {
        if ( left == null || right == null ) {
            return false;
        }
        return langTypes.isSameType( left, right );
    }

    private boolean isNamedAnnotation(TypeDescriptor annotationType) {
        return namedAnnotationType != null && isSameType( namedAnnotationType, annotationType );
    }

    private String namedValue(AnnotationDescriptor annotation) {
        return AnnotationValueUtils.asString( AnnotationDescriptorUtils.getValue( annotation, "value" ) );
    }

    private Set<AnnotationDescriptor> getQualifierAnnotations(Method candidate) {
        Set<AnnotationDescriptor> qualifierAnnotations = new HashSet<>();

        if ( !( candidate instanceof SourceMethod ) ) {
            return qualifierAnnotations;
        }

        SourceMethod sourceMethod = (SourceMethod) candidate;
        if ( langElements != null ) {
            for ( AnnotationDescriptor methodAnnotation :
                langElements.annotationMirrors( sourceMethod.getExecutableDescriptor() ) ) {
                addOnlyWhenQualifier( qualifierAnnotations, methodAnnotation );
            }
        }

        Type mapper = candidate.getDeclaringMapper();
        if ( mapper != null ) {
            TypeDescriptor mapperDescriptor = mapper.getTypeDescriptor();
            if ( mapperDescriptor != null ) {
                mapperDescriptor.typeElement().ifPresent( typeElementDescriptor -> {
                    if ( langElements != null ) {
                        for ( AnnotationDescriptor mapperAnnotation :
                            langElements.annotationMirrors( typeElementDescriptor ) ) {
                            addOnlyWhenQualifier( qualifierAnnotations, mapperAnnotation );
                        }
                    }
                } );
            }
        }

        return qualifierAnnotations;
    }

    private void addOnlyWhenQualifier(Set<AnnotationDescriptor> annotationSet, AnnotationDescriptor candidate) {
        if ( candidate == null ) {
            return;
        }
        if ( isQualifierAnnotation( candidate ) ) {
            annotationSet.add( candidate );
        }
    }

    private boolean isQualifierAnnotation(AnnotationDescriptor candidate) {
        if ( candidate == null || langElements == null ) {
            return false;
        }
        TypeElementDescriptor annotationType = candidate.annotationType();
        if ( annotationType == null ) {
            return false;
        }
        for ( AnnotationDescriptor meta : langElements.annotationMirrors( annotationType ) ) {
            if ( AnnotationDescriptorUtils.hasQualifiedName( meta, MAPSTRUCT_QUALIFIER ) ) {
                return true;
            }
        }
        return false;
    }

    private TypeDescriptor resolveNamedAnnotationType() {
        if ( langElements == null ) {
            return resolveNamedAnnotationViaTypeFactory();
        }
        TypeElementDescriptor namedElement = langElements.typeElement( "org.mapstruct.Named" );
        if ( namedElement != null ) {
            return namedElement.asType();
        }
        return resolveNamedAnnotationViaTypeFactory();
    }

    private TypeDescriptor resolveNamedAnnotationViaTypeFactory() {
        if ( typeFactory == null ) {
            return null;
        }
        Type namedType = typeFactory.getType( "org.mapstruct.Named" );
        return namedType != null ? namedType.getTypeDescriptor() : null;
    }
}
