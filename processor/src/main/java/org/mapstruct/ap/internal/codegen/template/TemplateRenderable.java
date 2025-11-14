/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.codegen.template;

import java.io.Writer;

/**
 * Base class for MapStruct models rendered via templating.
 * The actual rendering is delegated to a {@link TemplateRenderer} provided through the {@link Writable.Context}.
 */
public abstract class TemplateRenderable implements Writable {

    @Override
    public void write(Context context, Writer writer) throws Exception {
        TemplateRenderer renderer = context.get( TemplateRenderer.class );
        if ( renderer == null ) {
            throw new IllegalStateException( "TemplateRenderer not provided in rendering context" );
        }

        renderer.render( this, context, writer );
    }

    /**
     * Returns the name of the template to be used for this renderable.
     *
     * @return the template name. Never {@code null}.
     */
    public String getTemplateName() {
        return getTemplateNameForClass( getClass() );
    }

    /**
     * Computes the template path from the provided class by replacing dots with slashes and appending {@code .ftl}.
     *
     * @param clazz the class to derive the template name from
     * @return the template name. Never {@code null}.
     */
    protected String getTemplateNameForClass(Class<?> clazz) {
        return clazz.getName().replace( '.', '/' ) + ".ftl";
    }
}
