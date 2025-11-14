/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.processor;

import java.util.Objects;
import java.util.stream.Collectors;
import javax.tools.Diagnostic.Kind;

import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationValueDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.ElementDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.ExecutableDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.ParameterDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeElementDescriptor;
import org.mapstruct.ap.internal.util.FormattingMessager;
import org.mapstruct.ap.internal.util.Message;

/**
 * Handles redirection of errors/warnings so that they're shown on the mapper instead of hidden on a superclass.
 * <p>
 * Note: this messenger bridges MapStruct diagnostics to the JSR 269 {@link javax.annotation.processing.Messager}
 * API, keeping descriptor-level metadata aligned with compiler reporting.
 *
 * @author Ben Zegveld
 */
public class MapperAnnotatedFormattingMessenger implements FormattingMessager {

    private final FormattingMessager delegateMessager;
    private final TypeElementDescriptor mapperTypeDescriptor;

    public MapperAnnotatedFormattingMessenger(FormattingMessager delegateMessager,
                                              TypeElementDescriptor mapperTypeDescriptor) {
        this.delegateMessager = Objects.requireNonNull( delegateMessager, "delegateMessager" );
        this.mapperTypeDescriptor = mapperTypeDescriptor;
    }

    @Override
    public void printMessage(Message msg, Object... args) {
        delegateMessager.printMessage( msg, args );
    }

    @Override
    public void printMessage(ElementDescriptor element, Message msg, Object... args) {
        delegateMessager.printMessage(
            determineDelegationElement( element ),
            determineDelegationMessage( element, msg ),
            determineDelegationArguments( element, msg, args )
        );
    }

    @Override
    public void printMessage(ElementDescriptor element,
                             AnnotationDescriptor annotation,
                             AnnotationValueDescriptor value,
                             Message msg,
                             Object... args) {
        delegateMessager.printMessage(
            determineDelegationElement( element ),
            annotation,
            value,
            determineDelegationMessage( element, msg ),
            determineDelegationArguments( element, msg, args )
        );
    }

    @Override
    public void note(int level, Message log, Object... args) {
        delegateMessager.note( level, log, args );
    }

    @Override
    public boolean isErroneous() {
        return delegateMessager.isErroneous();
    }

    private Object[] determineDelegationArguments(ElementDescriptor element, Message msg, Object[] args) {
        if ( methodInMapperClass( element ) ) {
            return args;
        }
        if ( element == null || element.enclosingElement().isEmpty() ) {
            return args;
        }
        String originalMessage = String.format( msg.getDescription(), args );
        return new Object[] {
            originalMessage,
            constructMethod( element ),
            element.enclosingElement()
                .map( enclosing -> enclosing.simpleName().content() )
                .orElse( "" )
        };
    }

    /**
     * ExecutableElement.toString() has different values depending on the compiler. Constructing the method itself
     * manually will ensure that the message is always traceable to it's source.
     */
    private String constructMethod(ElementDescriptor element) {
        if ( element instanceof ExecutableDescriptor ) {
            ExecutableDescriptor executableElement = (ExecutableDescriptor) element;
            StringBuilder method = new StringBuilder();
            method.append( executableElement.returnType() != null
                ? executableElement.returnType().displayName()
                : "void" );
            method.append( ' ' );
            method.append( executableElement.simpleName().content() );
            method.append( '(' );
            method.append( executableElement.parameters()
                .stream()
                .map( this::parameterToString )
                .collect( Collectors.joining( ", " ) ) );
            method.append( ')' );
            return method.toString();
        }
        return element != null ? element.simpleName().content() : "";
    }

    private String parameterToString(ParameterDescriptor parameter) {
        String typeName = parameter.type().displayName();
        if ( parameter.isVarArgs() && typeName.endsWith( "[]" ) ) {
            typeName = typeName.substring( 0, typeName.length() - 2 ) + "...";
        }
        return typeName + " " + parameter.name();
    }

    private Message determineDelegationMessage(ElementDescriptor element, Message msg) {
        if ( methodInMapperClass( element ) ) {
            return msg;
        }
        if ( msg.getDiagnosticKind() == Kind.ERROR ) {
            return Message.MESSAGE_MOVED_TO_MAPPER_ERROR;
        }
        return Message.MESSAGE_MOVED_TO_MAPPER_WARNING;
    }

    private ElementDescriptor determineDelegationElement(ElementDescriptor element) {
        if ( methodInMapperClass( element ) ) {
            return element;
        }
        return mapperTypeDescriptor != null ? mapperTypeDescriptor : element;
    }

    private boolean methodInMapperClass(ElementDescriptor element) {
        if ( mapperTypeDescriptor == null || element == null ) {
            return true;
        }
        if ( element.id().equals( mapperTypeDescriptor.id() ) ) {
            return true;
        }
        return element.enclosingElement()
            .map( enclosing -> enclosing.id().equals( mapperTypeDescriptor.id() ) )
            .orElse( false );
    }

}
