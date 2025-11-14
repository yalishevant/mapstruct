/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.javax;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

import org.mapstruct.ap.internal.gem.BuilderGem;
import org.mapstruct.ap.internal.langmodel.AnnotationAttribute;
import org.mapstruct.ap.internal.util.AnnotationValueUtils;
import org.mapstruct.ap.internal.util.IgnoreJRERequirement;
import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationValueDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;
import org.mapstruct.ap.spi.TypeHierarchyErroneousException;

import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;

final class JavaxAnnotationAttributeFactory {

    private JavaxAnnotationAttributeFactory() {
    }

    static AnnotationAttribute<String> stringAttribute(AnnotationValueDescriptor descriptor, String defaultValue) {
        if ( descriptor == null ) {
            return AnnotationAttribute.absent( defaultValue );
        }
        String value = AnnotationValueUtils.asString( descriptor );
        return AnnotationAttribute.present( value, defaultValue );
    }

    static AnnotationAttribute<Boolean> booleanAttribute(AnnotationValueDescriptor descriptor, boolean defaultValue) {
        if ( descriptor == null ) {
            return AnnotationAttribute.absent( defaultValue );
        }
        boolean value = AnnotationValueUtils.asBoolean( descriptor, defaultValue );
        return AnnotationAttribute.present( value, defaultValue );
    }

    static AnnotationAttribute<List<TypeDescriptor>> typeListAttribute(JavaxLangModelContext context,
                                                                       TypeElement annotationTarget,
                                                                       AnnotationDescriptor annotation,
                                                                       AnnotationValueDescriptor descriptor,
                                                                       boolean declaredExplicitly) {
        if ( descriptor == null ) {
            return AnnotationAttribute.absent( Collections.emptyList() );
        }

        List<AnnotationValueDescriptor> rawValues = descriptor.asList();
        List<TypeDescriptor> values = AnnotationValueUtils.asTypeList( descriptor );
        boolean requiresFallback = declaredExplicitly
            && ((rawValues.isEmpty() && values.isEmpty())
            || (!rawValues.isEmpty() && values.size() != rawValues.size()));

        if ( requiresFallback ) {
            AnnotationMirror annotationMirror = annotation instanceof JavaxAnnotationDescriptor
                ? ( (JavaxAnnotationDescriptor) annotation ).mirror()
                : null;
            if ( annotationMirror == null ) {
                throw new TypeHierarchyErroneousException();
            }
            values = resolveUsingTrees(
                context,
                annotationTarget,
                annotationMirror,
                rawValues.isEmpty() ? Collections.singletonList( descriptor ) : rawValues
            );
        }

        if ( declaredExplicitly && values.isEmpty() ) {
            throw new TypeHierarchyErroneousException();
        }

        return declaredExplicitly
            ? AnnotationAttribute.present( values, Collections.emptyList() )
            : AnnotationAttribute.absent( Collections.emptyList() );
    }

    @IgnoreJRERequirement
    private static List<TypeDescriptor> resolveUsingTrees(JavaxLangModelContext context,
                                                          TypeElement annotationTarget,
                                                          AnnotationMirror annotationMirror,
                                                          List<AnnotationValueDescriptor> descriptors) {
        Trees trees = context.trees();
        if ( trees == null ) {
            throw new TypeHierarchyErroneousException();
        }

        List<TypeDescriptor> resolved = new ArrayList<>( descriptors.size() );
        boolean unresolved = false;

        for ( AnnotationValueDescriptor valueDescriptor : descriptors ) {
            TypeDescriptor direct = AnnotationValueUtils.asType( valueDescriptor );
            if ( direct != null ) {
                resolved.add( direct );
                continue;
            }

            AnnotationValue annotationValue = valueDescriptor instanceof JavaxAnnotationValueDescriptor
                ? ( (JavaxAnnotationValueDescriptor) valueDescriptor ).annotationValue()
                : null;
            if ( annotationValue == null ) {
                unresolved = true;
                continue;
            }
            TreePath path = trees.getPath( annotationTarget, annotationMirror, annotationValue );
            if ( path == null ) {
                unresolved = true;
                continue;
            }

            Element element = trees.getElement( path );
            if ( !( element instanceof TypeElement ) ) {
                unresolved = true;
                continue;
            }

            TypeElement typeElement = (TypeElement) element;
            TypeDescriptor descriptor = context.descriptorFactory().typeDescriptor( typeElement.asType() );
            resolved.add( descriptor );
        }

        if ( unresolved || resolved.size() != descriptors.size() ) {
            throw new TypeHierarchyErroneousException();
        }

        return Collections.unmodifiableList( resolved );
    }

    static AnnotationAttribute<TypeDescriptor> typeAttribute(AnnotationValueDescriptor descriptor,
                                                             TypeDescriptor defaultValue) {
        if ( descriptor == null ) {
            return AnnotationAttribute.absent( defaultValue );
        }
        TypeDescriptor value = AnnotationValueUtils.asType( descriptor );
        if ( value == null ) {
            Object raw = descriptor.value();
            throw new TypeHierarchyErroneousException();
        }
        return AnnotationAttribute.present( value, defaultValue );
    }

    static AnnotationAttribute<BuilderGem> builderAttribute(JavaxLangModelContext context,
                                                            AnnotationValueDescriptor descriptor) {
        if ( descriptor == null ) {
            return AnnotationAttribute.absent( null );
        }
        AnnotationDescriptor builderDescriptor = descriptor.asAnnotation();
        BuilderGem builderGem = context.annotationGemFactory().builder( builderDescriptor );
        return AnnotationAttribute.present( builderGem, null );
    }
}
