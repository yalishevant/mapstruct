/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.codegen.template;

import java.io.Writer;

/**
 * Abstraction over the concrete templating engine used to render {@link TemplateRenderable} models.
 */
public interface TemplateRenderer {

    /**
     * Renders the provided {@link TemplateRenderable} into the supplied {@link Writer}.
     *
     * @param renderable the model element to render
     * @param context the rendering context shared across nested template invocations
     * @param writer the writer to emit the rendered content to
     * @throws Exception if rendering fails
     */
    void render(TemplateRenderable renderable, Writable.Context context, Writer writer) throws Exception;
}
