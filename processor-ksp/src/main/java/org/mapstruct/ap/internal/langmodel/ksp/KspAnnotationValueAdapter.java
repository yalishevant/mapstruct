/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.ksp;

import com.google.devtools.ksp.symbol.KSAnnotation;
import com.google.devtools.ksp.symbol.KSClassDeclaration;
import com.google.devtools.ksp.symbol.KSType;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.AnnotationValueVisitor;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.List;

/**
 * Adapter that wraps a KSP annotation value as a javax {@link AnnotationValue}.
 */
final class KspAnnotationValueAdapter implements AnnotationValue {

    private final Object value;
    private final KspTypeAdapterFactory adapterFactory;

    KspAnnotationValueAdapter(Object value, KspTypeAdapterFactory adapterFactory) {
        this.value = value;
        this.adapterFactory = adapterFactory;
    }

    @Override
    public Object getValue() {
        return convertValue( value );
    }

    @Override
    public <R, P> R accept(AnnotationValueVisitor<R, P> v, P p) {
        Object converted = convertValue( value );

        if ( converted == null ) {
            return v.visitUnknown( this, p );
        }
        if ( converted instanceof Boolean ) {
            return v.visitBoolean( (Boolean) converted, p );
        }
        if ( converted instanceof Byte ) {
            return v.visitByte( (Byte) converted, p );
        }
        if ( converted instanceof Character ) {
            return v.visitChar( (Character) converted, p );
        }
        if ( converted instanceof Double ) {
            return v.visitDouble( (Double) converted, p );
        }
        if ( converted instanceof Float ) {
            return v.visitFloat( (Float) converted, p );
        }
        if ( converted instanceof Integer ) {
            return v.visitInt( (Integer) converted, p );
        }
        if ( converted instanceof Long ) {
            return v.visitLong( (Long) converted, p );
        }
        if ( converted instanceof Short ) {
            return v.visitShort( (Short) converted, p );
        }
        if ( converted instanceof String ) {
            return v.visitString( (String) converted, p );
        }
        if ( converted instanceof TypeMirror ) {
            return v.visitType( (TypeMirror) converted, p );
        }
        if ( converted instanceof VariableElement ) {
            return v.visitEnumConstant( (VariableElement) converted, p );
        }
        if ( converted instanceof AnnotationMirror ) {
            return v.visitAnnotation( (AnnotationMirror) converted, p );
        }
        if ( converted instanceof List ) {
            @SuppressWarnings( "unchecked" )
            List<? extends AnnotationValue> list = (List<? extends AnnotationValue>) converted;
            return v.visitArray( list, p );
        }

        return v.visitUnknown( this, p );
    }

    private Object convertValue(Object val) {
        if ( val == null ) {
            return null;
        }

        // Primitive types and String - return as-is
        if ( val instanceof Boolean || val instanceof Byte || val instanceof Character ||
             val instanceof Double || val instanceof Float || val instanceof Integer ||
             val instanceof Long || val instanceof Short || val instanceof String ) {
            return val;
        }

        // KSType -> TypeMirror
        if ( val instanceof KSType ) {
            return adapterFactory.typeMirror( (KSType) val );
        }

        // KSAnnotation -> AnnotationMirror
        if ( val instanceof KSAnnotation ) {
            return adapterFactory.annotationMirror( (KSAnnotation) val );
        }

        // KSClassDeclaration (enum constant) -> VariableElement
        if ( val instanceof KSClassDeclaration ) {
            KSClassDeclaration classDecl = (KSClassDeclaration) val;
            return new KspEnumConstantAdapter( classDecl, adapterFactory );
        }

        // List -> List<AnnotationValue>
        if ( val instanceof List ) {
            List<?> list = (List<?>) val;
            List<AnnotationValue> result = new ArrayList<>( list.size() );
            for ( Object item : list ) {
                result.add( new KspAnnotationValueAdapter( item, adapterFactory ) );
            }
            return result;
        }

        // ArrayList (KSP uses ArrayList for arrays) -> List<AnnotationValue>
        if ( val instanceof ArrayList ) {
            ArrayList<?> list = (ArrayList<?>) val;
            List<AnnotationValue> result = new ArrayList<>( list.size() );
            for ( Object item : list ) {
                result.add( new KspAnnotationValueAdapter( item, adapterFactory ) );
            }
            return result;
        }

        // Unknown - return as string
        return val.toString();
    }

    @Override
    public String toString() {
        return String.valueOf( getValue() );
    }
}
