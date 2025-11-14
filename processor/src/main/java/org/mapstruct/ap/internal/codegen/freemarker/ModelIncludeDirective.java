/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.codegen.freemarker;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.mapstruct.ap.internal.codegen.template.DefaultTemplateRenderingContext;
import org.mapstruct.ap.internal.codegen.template.TemplateRenderer;
import org.mapstruct.ap.internal.codegen.template.Writable;

import freemarker.core.Environment;
import freemarker.ext.beans.BeanModel;
import freemarker.template.Configuration;
import freemarker.template.TemplateDirectiveBody;
import freemarker.template.TemplateDirectiveModel;
import freemarker.template.TemplateException;
import freemarker.template.TemplateModel;

/**
 * FreeMarker directive used to include nested {@link Writable} models.
 */
final class ModelIncludeDirective implements TemplateDirectiveModel {

    private final Configuration configuration;
    private final TemplateRenderer renderer;

    ModelIncludeDirective(Configuration configuration, TemplateRenderer renderer) {
        this.configuration = configuration;
        this.renderer = renderer;
    }

    @Override
    public void execute(Environment env, @SuppressWarnings("rawtypes") Map params, TemplateModel[] loopVars,
                        TemplateDirectiveBody body)
        throws TemplateException, IOException {

        Writable modelElement = getModelElement( params );
        DefaultTemplateRenderingContext context = createContext( params );

        try {
            if ( modelElement != null ) {
                modelElement.write( context, env.getOut() );
            }
        }
        catch ( TemplateException | RuntimeException | IOException te ) {
            throw te;
        }
        catch ( Exception e ) {
            throw new RuntimeException( e );
        }
    }

    @SuppressWarnings("rawtypes")
    private Writable getModelElement(Map params) {
        if ( !params.containsKey( "object" ) ) {
            throw new IllegalArgumentException(
                "Object to be included must be passed to this directive via the 'object' parameter"
            );
        }

        BeanModel objectModel = (BeanModel) params.get( "object" );

        if ( objectModel == null ) {
            return null;
        }

        if ( !( objectModel.getWrappedObject() instanceof Writable ) ) {
            throw new IllegalArgumentException( "Given object isn't a Writable:" + objectModel.getWrappedObject() );
        }

        return (Writable) objectModel.getWrappedObject();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private DefaultTemplateRenderingContext createContext(Map params) {
        Map<String, Object> ext = new HashMap<>( params );
        ext.remove( "object" );

        Map<Class<?>, Object> values = new HashMap<>();
        values.put( Configuration.class, configuration );
        values.put( Map.class, ext );
        values.put( TemplateRenderer.class, renderer );

        return new DefaultTemplateRenderingContext( values );
    }
}
