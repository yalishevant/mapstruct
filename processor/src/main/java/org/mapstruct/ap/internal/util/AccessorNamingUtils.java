/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.util;

import org.mapstruct.ap.internal.langmodel.AccessorNamingAdapter;
import org.mapstruct.ap.internal.langmodel.descriptor.ElementDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.ExecutableDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.LangModifier;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;
import org.mapstruct.ap.internal.util.accessor.Accessor;
import org.mapstruct.ap.internal.util.accessor.AccessorType;
import org.mapstruct.ap.spi.MethodType;

/**
 * Descriptor-based utilities for working with accessor naming strategies.
 *
 * <p>The interaction with {@link org.mapstruct.ap.spi.AccessorNamingStrategy} is delegated to backend-specific
 * {@link AccessorNamingAdapter} implementations, keeping this helper free from direct compiler dependencies.</p>
 *
 * @author Filip Hrisafov
 */
public final class AccessorNamingUtils {

    private final AccessorNamingAdapter adapter;

    public AccessorNamingUtils(AccessorNamingAdapter adapter) {
        this.adapter = adapter;
    }

    public boolean isGetterMethod(ExecutableDescriptor executable) {
        return executable != null
            && isPublicNotStatic( executable )
            && executable.parameters().isEmpty()
            && adapter.methodType( executable ) == MethodType.GETTER;
    }

    public boolean isPresenceCheckMethod(ExecutableDescriptor executable) {
        return executable != null
            && isPublicNotStatic( executable )
            && executable.parameters().isEmpty()
            && isBooleanType( executable.returnType() )
            && adapter.methodType( executable ) == MethodType.PRESENCE_CHECKER;
    }

    public boolean isSetterMethod(ExecutableDescriptor executable) {
        return executable != null
            && isPublicNotStatic( executable )
            && executable.parameters().size() == 1
            && adapter.methodType( executable ) == MethodType.SETTER;
    }

    public boolean isAdderMethod(ExecutableDescriptor executable) {
        return executable != null
            && isPublicNotStatic( executable )
            && executable.parameters().size() == 1
            && adapter.methodType( executable ) == MethodType.ADDER;
    }

    public String getPropertyName(ExecutableDescriptor executable) {
        return adapter.propertyName( executable );
    }

    /**
     * @param adderMethod the adder method
     *
     * @return the element name to which an adder method applies.
     */
    public String getElementNameForAdder(Accessor adderMethod) {
        if ( adderMethod.getAccessorType() == AccessorType.ADDER ) {
            ElementDescriptor element = adderMethod.getElement();
            if ( element != null ) {
                return adapter.elementName( element );
            }
        }
        return null;
    }

    private boolean isPublicNotStatic(ExecutableDescriptor executable) {
        return executable != null
            && executable.modifiers().contains( LangModifier.PUBLIC )
            && !executable.modifiers().contains( LangModifier.STATIC );
    }

    private static boolean isBooleanType(TypeDescriptor type) {
        if ( type == null ) {
            return false;
        }
        if ( type.isPrimitive() ) {
            return "boolean".equals( type.displayName() );
        }
        return type.qualifiedName().map( "java.lang.Boolean"::equals ).orElse( false );
    }
}
