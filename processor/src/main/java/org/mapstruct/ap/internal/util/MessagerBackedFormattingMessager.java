/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.stream.IntStream;

import javax.annotation.processing.Messager;
import javax.tools.Diagnostic;

import org.mapstruct.ap.internal.langmodel.api.DescriptorUnwrapper;
import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationValueDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.ElementDescriptor;

/**
 * {@link FormattingMessager} implementation delegating to the compiler {@link Messager} while operating on descriptors.
 */
public final class MessagerBackedFormattingMessager implements FormattingMessager {

    private static final String ELEMENT_FQN = "javax" + ".lang.model.element.Element";
    private static final String ANNOTATION_MIRROR_FQN = "javax" + ".lang.model.element.AnnotationMirror";
    private static final String ANNOTATION_VALUE_FQN = "javax" + ".lang.model.element.AnnotationValue";

    private final Messager messager;
    private final boolean verbose;
    private final DescriptorUnwrapper descriptorUnwrapper;
    private boolean erroneous;

    public MessagerBackedFormattingMessager(Messager messager,
                                            boolean verbose,
                                            DescriptorUnwrapper descriptorUnwrapper) {
        this.messager = Objects.requireNonNull( messager, "messager" );
        this.verbose = verbose;
        this.descriptorUnwrapper = Objects.requireNonNull( descriptorUnwrapper, "descriptorUnwrapper" );
    }

    @Override
    public void printMessage(Message msg, Object... args) {
        emit( null, null, null, msg, args );
    }

    @Override
    public void printMessage(ElementDescriptor element, Message msg, Object... args) {
        emit( element, null, null, msg, args );
    }

    @Override
    public void printMessage(ElementDescriptor element,
                             AnnotationDescriptor annotation,
                             AnnotationValueDescriptor value,
                             Message msg,
                             Object... args) {
        emit( element, annotation, value, msg, args );
    }

    @Override
    public void note(int level, Message msg, Object... args) {
        if ( !verbose ) {
            return;
        }
        StringBuilder builder = new StringBuilder();
        IntStream.range( 0, level ).mapToObj( i -> "-" ).forEach( builder::append );
        builder.append( " MapStruct: " ).append( String.format( msg.getDescription(), args ) );
        messager.printMessage( Diagnostic.Kind.NOTE, builder.toString() );
    }

    @Override
    public boolean isErroneous() {
        return erroneous;
    }

    private void emit(ElementDescriptor element,
                      AnnotationDescriptor annotation,
                      AnnotationValueDescriptor value,
                      Message msg,
                      Object... args) {

        Object unwrappedElement = descriptorUnwrapper.element( element, Object.class ).orElse( null );
        Object unwrappedAnnotation = descriptorUnwrapper.annotation( annotation, Object.class ).orElse( null );
        Object unwrappedValue = descriptorUnwrapper.annotationValue( value, Object.class ).orElse( null );

        String payload = String.format( msg.getDescription(), args );
        try {
            if ( unwrappedElement != null && unwrappedAnnotation != null && unwrappedValue != null ) {
                invokePrintMessage( msg.getDiagnosticKind(), payload, unwrappedElement, unwrappedAnnotation,
                    unwrappedValue );
            }
            else if ( unwrappedElement != null && unwrappedAnnotation != null ) {
                invokePrintMessage( msg.getDiagnosticKind(), payload, unwrappedElement, unwrappedAnnotation );
            }
            else if ( unwrappedElement != null ) {
                invokePrintMessage( msg.getDiagnosticKind(), payload, unwrappedElement );
            }
            else {
                messager.printMessage( msg.getDiagnosticKind(), payload );
            }
        }
        catch ( ReflectiveOperationException ex ) {
            throw new IllegalStateException( "Failed to emit diagnostic via Messager", ex );
        }

        if ( msg.getDiagnosticKind() == Diagnostic.Kind.ERROR ) {
            erroneous = true;
        }
    }

    private void invokePrintMessage(Diagnostic.Kind kind, CharSequence payload, Object element)
        throws ReflectiveOperationException {
        Method method = messager.getClass()
            .getMethod( "printMessage", Diagnostic.Kind.class, CharSequence.class, loadClass( ELEMENT_FQN ) );
        method.invoke( messager, kind, payload, element );
    }

    private void invokePrintMessage(Diagnostic.Kind kind, CharSequence payload, Object element, Object annotation)
        throws ReflectiveOperationException {
        Method method = messager.getClass()
            .getMethod(
                "printMessage",
                Diagnostic.Kind.class,
                CharSequence.class,
                loadClass( ELEMENT_FQN ),
                loadClass( ANNOTATION_MIRROR_FQN )
            );
        method.invoke( messager, kind, payload, element, annotation );
    }

    private void invokePrintMessage(Diagnostic.Kind kind,
                                    CharSequence payload,
                                    Object element,
                                    Object annotation,
                                    Object value) throws ReflectiveOperationException {

        Method method = messager.getClass()
            .getMethod(
                "printMessage",
                Diagnostic.Kind.class,
                CharSequence.class,
                loadClass( ELEMENT_FQN ),
                loadClass( ANNOTATION_MIRROR_FQN ),
                loadClass( ANNOTATION_VALUE_FQN )
            );
        method.invoke( messager, kind, payload, element, annotation, value );
    }

    private static Class<?> loadClass(String fqn) throws ClassNotFoundException {
        return Class.forName( fqn );
    }
}
