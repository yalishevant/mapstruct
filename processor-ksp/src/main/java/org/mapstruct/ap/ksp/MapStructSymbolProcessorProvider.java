/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.ksp;

import com.google.devtools.ksp.processing.SymbolProcessor;
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment;
import com.google.devtools.ksp.processing.SymbolProcessorProvider;

/**
 * Provider for the MapStruct KSP symbol processor.
 * <p>
 * This class is discovered via the KSP service loader mechanism.
 */
public class MapStructSymbolProcessorProvider implements SymbolProcessorProvider {

    @Override
    public SymbolProcessor create(SymbolProcessorEnvironment environment) {
        return new MapStructSymbolProcessor(
            environment.getLogger(),
            environment.getCodeGenerator(),
            environment.getOptions()
        );
    }
}
