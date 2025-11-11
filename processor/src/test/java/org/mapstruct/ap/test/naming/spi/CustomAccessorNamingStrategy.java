/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.test.naming.spi;

import org.mapstruct.ap.descriptor.ExecutableDescriptor;
import org.mapstruct.ap.spi.AccessorNamingStrategy;
import org.mapstruct.ap.spi.DefaultAccessorNamingStrategy;
import org.mapstruct.ap.spi.MethodType;
import org.mapstruct.ap.spi.util.IntrospectorUtils;

/**
 * A custom {@link AccessorNamingStrategy} recognizing getters in the form of {@code property()} and setters in the
 * form of {@code withProperty(value)}.
 *
 * @author Gunnar Morling
 */
public class CustomAccessorNamingStrategy extends DefaultAccessorNamingStrategy implements AccessorNamingStrategy {

    @Override
    public MethodType getMethodType(ExecutableDescriptor method) {
        if ( method == null ) {
            return MethodType.OTHER;
        }

        String methodName = methodName( method );
        if ( method.parameters().isEmpty() && !isVoid( method.returnType() ) ) {
            return MethodType.GETTER;
        }

        if ( methodName.startsWith( "with" ) && methodName.length() > 4 ) {
            return MethodType.SETTER;
        }

        if ( methodName.startsWith( "add" ) && methodName.length() > 3 ) {
            return MethodType.ADDER;
        }

        return MethodType.OTHER;
    }

    @Override
    public String getPropertyName(ExecutableDescriptor getterOrSetterMethod) {
        String methodName = methodName( getterOrSetterMethod );
        return IntrospectorUtils.decapitalize(
            methodName.startsWith( "with" ) ? methodName.substring( 4 ) : methodName
        );
    }

    @Override
    public String getElementName(ExecutableDescriptor adderMethod) {
        String methodName = methodName( adderMethod );
        return IntrospectorUtils.decapitalize( methodName.substring( 3 ) );
    }
}
