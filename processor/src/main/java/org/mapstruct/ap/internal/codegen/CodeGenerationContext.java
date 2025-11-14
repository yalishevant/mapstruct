/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.codegen;

import org.mapstruct.ap.internal.codegen.template.TemplateRenderer;
import org.mapstruct.ap.internal.langmodel.api.DescriptorUnwrapper;
import org.mapstruct.ap.internal.langmodel.codegen.GeneratedFileSink;

/**
 * Context shared between the core mapper pipeline and the code generation layer.
 */
public interface CodeGenerationContext {

    /**
     * Provides access to the sink used for creating generated files.
     *
     * @return generated file sink
     */
    GeneratedFileSink getGeneratedFileSink();

    /**
     * Provides the templating engine used to render models.
     *
     * @return the template renderer
     */
    TemplateRenderer getTemplateRenderer();

    /**
     * Provides access to backend-specific descriptor conversions.
     *
     * @return descriptor unwrapper
     */
    DescriptorUnwrapper getDescriptorUnwrapper();
}
