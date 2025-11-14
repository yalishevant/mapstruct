/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.model;

import java.util.Collection;

import org.mapstruct.ap.internal.model.common.BuilderType;
import org.mapstruct.ap.internal.model.source.Method;
import org.mapstruct.ap.internal.gem.BuilderGem;
import org.mapstruct.ap.internal.util.Extractor;
import org.mapstruct.ap.internal.util.Message;
import org.mapstruct.ap.internal.util.Strings;
import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.ExecutableDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.ParameterDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;

import static org.mapstruct.ap.internal.util.Collections.first;

/**
 * @author Filip Hrisafov
 */
public class BuilderFinisherMethodResolver {

    private static final String DEFAULT_BUILD_METHOD_NAME = "build";

    private static final Extractor<ExecutableDescriptor, String> EXECUTABLE_DESCRIPTOR_NAME_EXTRACTOR =
        executable -> {
            StringBuilder sb = new StringBuilder( executable.simpleName().content() );

            sb.append( '(' );
            boolean firstParameter = true;
            for ( ParameterDescriptor parameter : executable.parameters() ) {
                if ( !firstParameter ) {
                    sb.append( ", " );
                }
                TypeDescriptor parameterType = parameter.asType();
                String typeName = parameterType != null ? parameterType.displayName() : "?";
                sb.append( typeName ).append( ' ' ).append( parameter.simpleName().content() );
                firstParameter = false;
            }

            sb.append( ')' );
            return sb.toString();
        };

    private BuilderFinisherMethodResolver() {
    }

    public static MethodReference getBuilderFinisherMethod(Method method, BuilderType builderType,
        MappingBuilderContext ctx) {
        Collection<ExecutableDescriptor> buildMethods = builderType.getBuildMethods();
        if ( buildMethods.isEmpty() ) {
            //If we reach this method this should never happen
            return null;
        }

        BuilderGem builder = method.getOptions().getBeanMapping().getBuilder();
        if ( builder == null && buildMethods.size() == 1 ) {
            return MethodReference.forMethodCall( first( buildMethods ).simpleName().content() );
        }
        else {
            String buildMethodPattern = DEFAULT_BUILD_METHOD_NAME;
            if ( builder != null ) {
                buildMethodPattern = builder.buildMethod().get();
            }
            for ( ExecutableDescriptor buildMethod : buildMethods ) {
                String methodName = buildMethod.simpleName().content();
                if ( methodName.matches( buildMethodPattern ) ) {
                    return MethodReference.forMethodCall( methodName );
                }
            }

            if ( builder == null ) {
                ctx.getMessager().printMessage(
                    method.getExecutable(),
                    Message.BUILDER_NO_BUILD_METHOD_FOUND_DEFAULT,
                    buildMethodPattern,
                    builderType.getBuilder(),
                    builderType.getBuildingType(),
                    Strings.join( buildMethods, ", ", EXECUTABLE_DESCRIPTOR_NAME_EXTRACTOR )
                );
            }
            else {
                AnnotationDescriptor builderAnnotation = ctx.getTypeFactory().getDescriptorFactory()
                    .annotationDescriptor( builder.mirror() );
                ctx.getMessager().printMessage(
                    method.getExecutable(),
                    builderAnnotation,
                    Message.BUILDER_NO_BUILD_METHOD_FOUND,
                    buildMethodPattern,
                    builderType.getBuilder(),
                    builderType.getBuildingType(),
                    Strings.join( buildMethods, ", ", EXECUTABLE_DESCRIPTOR_NAME_EXTRACTOR )
                );
            }
        }

        return null;
    }

}
