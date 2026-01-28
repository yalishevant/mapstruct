/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.ksp;

import com.google.devtools.ksp.symbol.KSAnnotation;
import com.google.devtools.ksp.symbol.KSClassDeclaration;
import com.google.devtools.ksp.symbol.KSDeclaration;
import com.google.devtools.ksp.symbol.KSType;
import com.google.devtools.ksp.symbol.KSValueArgument;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.type.DeclaredType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Adapter that wraps a KSP {@link KSAnnotation} as a javax {@link AnnotationMirror}.
 * This allows KSP annotations to be used with MapStruct's gem framework.
 */
final class KspAnnotationMirrorAdapter implements AnnotationMirror {

    private final KSAnnotation annotation;
    private final KspTypeAdapterFactory adapterFactory;

    KspAnnotationMirrorAdapter(KSAnnotation annotation, KspTypeAdapterFactory adapterFactory) {
        this.annotation = annotation;
        this.adapterFactory = adapterFactory;
    }

    @Override
    public DeclaredType getAnnotationType() {
        KSType type = annotation.getAnnotationType().resolve();
        return new KspDeclaredTypeAdapter( type, adapterFactory );
    }

    @Override
    public Map<? extends ExecutableElement, ? extends AnnotationValue> getElementValues() {
        Map<ExecutableElement, AnnotationValue> result = new HashMap<>();

        List<KSValueArgument> arguments = annotation.getArguments();
        for ( KSValueArgument arg : arguments ) {
            String name = arg.getName() != null ? arg.getName().asString() : "";
            Object value = arg.getValue();

            ExecutableElement key = new KspAnnotationMethodAdapter( name, annotation, adapterFactory );
            AnnotationValue annotationValue = new KspAnnotationValueAdapter( value, adapterFactory );
            result.put( key, annotationValue );
        }

        return result;
    }

    KSAnnotation unwrap() {
        return annotation;
    }
}
