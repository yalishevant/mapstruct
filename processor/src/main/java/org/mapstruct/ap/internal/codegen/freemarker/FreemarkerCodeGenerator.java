/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.codegen.freemarker;

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.mapstruct.ap.internal.codegen.CodeGenerationContext;
import org.mapstruct.ap.internal.codegen.CodeGenerator;
import org.mapstruct.ap.internal.codegen.GeneratedFile;
import org.mapstruct.ap.internal.codegen.template.DefaultTemplateRenderingContext;
import org.mapstruct.ap.internal.codegen.template.TemplateRenderable;
import org.mapstruct.ap.internal.codegen.template.TemplateRenderer;
import org.mapstruct.ap.internal.codegen.template.Writable;
import org.mapstruct.ap.internal.model.common.ModelElement;
import org.mapstruct.ap.internal.model.common.Type;
import org.mapstruct.ap.internal.langmodel.codegen.GeneratedFileSink;

/**
 * {@link CodeGenerator} that renders {@link TemplateRenderable} models using FreeMarker templates.
 */
public final class FreemarkerCodeGenerator implements CodeGenerator {

    @Override
    public void generate(GeneratedFile fileDescriptor, CodeGenerationContext context) {
        TemplateRenderable template = fileDescriptor.getTemplate();
        TemplateRenderer renderer = context.getTemplateRenderer();

        Map<String, Object> attributes = new HashMap<>( fileDescriptor.getAttributes() );
        if ( template instanceof ModelElement ) {
            attributes.putIfAbsent( "imports", collectImportTypeNames( (ModelElement) template ) );
        }

        Map<Class<?>, Object> contextValues = new HashMap<>();
        contextValues.put( TemplateRenderer.class, renderer );
        contextValues.put( Map.class, attributes );
        contextValues.put( CodeGenerationContext.class, context );

        Writable.Context renderingContext = new DefaultTemplateRenderingContext( contextValues );

        GeneratedFileSink generatedFileSink = context.getGeneratedFileSink();

        switch ( fileDescriptor.getKind() ) {
            case JAVA_SOURCE:
                writeJavaSource( fileDescriptor, generatedFileSink, renderingContext );
                break;
            case RESOURCE:
                writeResource( fileDescriptor, generatedFileSink, renderingContext );
                break;
            default:
                throw new IllegalStateException( "Unsupported generated file kind: " + fileDescriptor.getKind() );
        }
    }

    private void writeJavaSource(GeneratedFile file, GeneratedFileSink generatedFileSink,
                                 Writable.Context renderingContext) {
        String qualifiedName = file.getPackageName() == null || file.getPackageName().isEmpty()
            ? file.getSimpleName()
            : file.getPackageName() + "." + file.getSimpleName();

        try ( Writer sinkWriter = generatedFileSink.createJavaSourceWriter(
            file.getPackageName(),
            file.getSimpleName(),
            file.getOriginatingElements()
        );
              Writer writer = new IndentationCorrectingWriter( sinkWriter ) ) {
            file.getTemplate().write( renderingContext, writer );
        }
        catch ( IOException e ) {
            throw new RuntimeException( e );
        }
        catch ( Exception e ) {
            throw new RuntimeException( "Failed to render Java source " + qualifiedName, e );
        }
    }

    private void writeResource(GeneratedFile file, GeneratedFileSink generatedFileSink,
                               Writable.Context renderingContext) {
        try ( Writer writer = generatedFileSink.createResourceWriter(
            file.getResourceName(),
            file.getOriginatingElements()
        ) ) {
            file.getTemplate().write( renderingContext, writer );
        }
        catch ( IOException e ) {
            throw new RuntimeException( e );
        }
        catch ( Exception e ) {
            throw new RuntimeException( "Failed to render resource " + file.getResourceName(), e );
        }
    }

    private SortedSet<String> collectImportTypeNames(ModelElement element) {
        SortedSet<String> importTypeNames = new TreeSet<>();
        for ( Type type : element.getImportTypes() ) {
            importTypeNames.add( type.getImportName() );
        }
        return importTypeNames;
    }
}
