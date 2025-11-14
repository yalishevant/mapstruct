/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.codegen;

/**
 * High-level entry point for emitting generated files (Java sources, resources, etc).
 */
public interface CodeGenerator {

    /**
     * Generates the file described by the given {@link GeneratedFile}.
     *
     * @param fileDescriptor metadata about the file to be generated
     * @param context shared generation context originating from the processor
     */
    void generate(GeneratedFile fileDescriptor, CodeGenerationContext context);
}
