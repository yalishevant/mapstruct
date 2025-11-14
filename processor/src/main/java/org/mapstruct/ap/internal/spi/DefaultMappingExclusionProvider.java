/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.spi;

import java.util.regex.Pattern;
import javax.lang.model.element.TypeElement;

import org.mapstruct.ap.spi.MappingExclusionProvider;

/**
 * The default implementation of the {@link MappingExclusionProvider} service provider interface.
 *
 * <p>With the default implementation, MapStruct will not consider classes in the {@code java} and {@code javax}
 * packages as source / target for an automatic sub-mapping. The only exception is the {@link java.util.Collection},
 * {@link java.util.Map} and {@link java.util.stream.Stream} types.</p>
 *
 * @author Filip Hrisafov
 */
public class DefaultMappingExclusionProvider implements MappingExclusionProvider {

    private static final Pattern JAVA_JAVAX_PACKAGE = Pattern.compile( "^javax?\\..*" );

    @Override
    public boolean isExcluded(TypeElement typeElement) {
        if ( typeElement == null ) {
            return false;
        }
        String qualifiedName = typeElement.getQualifiedName().toString();
        return !qualifiedName.isEmpty() && isFullyQualifiedNameExcluded( qualifiedName );
    }

    protected boolean isFullyQualifiedNameExcluded(String qualifiedName) {
        return JAVA_JAVAX_PACKAGE.matcher( qualifiedName ).matches();
    }
}
