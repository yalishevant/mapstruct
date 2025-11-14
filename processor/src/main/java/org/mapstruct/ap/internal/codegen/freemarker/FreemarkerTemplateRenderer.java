/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.codegen.freemarker;

import java.io.Writer;
import java.util.Map;

import org.mapstruct.ap.internal.codegen.template.TemplateRenderable;
import org.mapstruct.ap.internal.codegen.template.TemplateRenderer;
import org.mapstruct.ap.internal.codegen.template.Writable;

import freemarker.cache.StrongCacheStorage;
import freemarker.cache.TemplateLoader;
import freemarker.ext.beans.BeanModel;
import freemarker.ext.beans.BeansWrapper;
import freemarker.ext.beans.SimpleMapModel;
import freemarker.log.Logger;
import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/**
 * {@link TemplateRenderer} implementation backed by FreeMarker.
 */
public final class FreemarkerTemplateRenderer implements TemplateRenderer {

    private static final Configuration CONFIGURATION;
    private static final Object DIRECTIVE_LOCK = new Object();

    static {
        try {
            Logger.selectLoggerLibrary( Logger.LIBRARY_NONE );
        }
        catch ( ClassNotFoundException e ) {
            throw new RuntimeException( e );
        }

        CONFIGURATION = new Configuration( Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS );
        CONFIGURATION.setTemplateLoader( new SimpleClasspathLoader() );
        CONFIGURATION.setObjectWrapper(
            new DefaultObjectWrapper( Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS )
        );
        CONFIGURATION.setCacheStorage( new StrongCacheStorage() );
        CONFIGURATION.setTemplateUpdateDelay( Integer.MAX_VALUE );
        CONFIGURATION.setLocalizedLookup( false );
    }

    private final BeansWrapper beansWrapper = BeansWrapper.getDefaultInstance();

    public FreemarkerTemplateRenderer() {
        registerIncludeDirective();
    }

    public static Configuration configuration() {
        return CONFIGURATION;
    }

    private void registerIncludeDirective() {
        synchronized ( DIRECTIVE_LOCK ) {
            CONFIGURATION.setSharedVariable(
                "includeModel",
                new ModelIncludeDirective( CONFIGURATION, this )
            );
        }
    }

    @Override
    public void render(TemplateRenderable renderable, Writable.Context context, Writer writer) throws Exception {
        Template template = CONFIGURATION.getTemplate( renderable.getTemplateName() );

        Map<?, ?> extParams = context.get( Map.class );
        if ( extParams == null ) {
            throw new IllegalStateException( "Rendering context does not provide an attribute map" );
        }

        template.process(
            new ExternalParamsTemplateModel(
                new BeanModel( renderable, beansWrapper ),
                new SimpleMapModel( extParams, beansWrapper )
            ),
            writer
        );
    }

    private static class ExternalParamsTemplateModel implements TemplateHashModel {

        private final BeanModel object;
        private final SimpleMapModel extParams;

        ExternalParamsTemplateModel(BeanModel object, SimpleMapModel extParams) {
            this.object = object;
            this.extParams = extParams;
        }

        @Override
        public TemplateModel get(String key) throws TemplateModelException {
            if ( "ext".equals( key ) ) {
                return extParams;
            }
            return object.get( key );
        }

        @Override
        public boolean isEmpty() throws TemplateModelException {
            return object.isEmpty() && extParams.isEmpty();
        }
    }

    private static final class SimpleClasspathLoader implements TemplateLoader {
        @Override
        public java.io.Reader getReader(Object name, String encoding) throws java.io.IOException {
            java.net.URL url = getClass().getClassLoader().getResource( String.valueOf( name ) );
            if ( url == null ) {
                throw new IllegalStateException( name + " not found on classpath" );
            }
            java.net.URLConnection connection = url.openConnection();
            connection.setUseCaches( false );
            java.io.InputStream is = connection.getInputStream();
            return new java.io.InputStreamReader( is, java.nio.charset.StandardCharsets.UTF_8 );
        }

        @Override
        public long getLastModified(Object templateSource) {
            return 0;
        }

        @Override
        public Object findTemplateSource(String name) throws java.io.IOException {
            return name;
        }

        @Override
        public void closeTemplateSource(Object templateSource) throws java.io.IOException {
            // No-op
        }
    }
}
