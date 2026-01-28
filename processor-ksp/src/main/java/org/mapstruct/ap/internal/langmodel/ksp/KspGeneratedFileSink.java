/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.ksp;

import com.google.devtools.ksp.processing.CodeGenerator;
import com.google.devtools.ksp.processing.Dependencies;
import com.google.devtools.ksp.symbol.KSClassDeclaration;
import com.google.devtools.ksp.symbol.KSFile;

import org.mapstruct.ap.internal.langmodel.api.DescriptorUnwrapper;
import org.mapstruct.ap.internal.langmodel.codegen.GeneratedFileSink;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeElementDescriptor;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * KSP implementation of {@link GeneratedFileSink}.
 * Writes generated Java source files using KSP's {@link CodeGenerator}.
 */
final class KspGeneratedFileSink implements GeneratedFileSink {

    private final CodeGenerator codeGenerator;
    private final DescriptorUnwrapper descriptorUnwrapper;

    KspGeneratedFileSink(CodeGenerator codeGenerator, DescriptorUnwrapper descriptorUnwrapper) {
        this.codeGenerator = Objects.requireNonNull( codeGenerator, "codeGenerator" );
        this.descriptorUnwrapper = Objects.requireNonNull( descriptorUnwrapper, "descriptorUnwrapper" );
    }

    @Override
    public Writer createJavaSourceWriter(String packageName, String simpleName, List<Object> originatingElements)
            throws IOException {
        Objects.requireNonNull( simpleName, "simpleName" );

        String pkg = packageName != null ? packageName : "";

        // Collect originating files for incremental compilation
        List<KSFile> originatingFiles = collectOriginatingFiles( originatingElements );

        Dependencies dependencies = originatingFiles.isEmpty()
            ? Dependencies.Companion.getALL_FILES()
            : new Dependencies( false, originatingFiles.toArray( new KSFile[0] ) );

        OutputStream outputStream = codeGenerator.createNewFile(
            dependencies,
            pkg,
            simpleName,
            "java"
        );

        return new OutputStreamWriter( outputStream, StandardCharsets.UTF_8 );
    }

    @Override
    public Writer createResourceWriter(String resourceName, List<Object> originatingElements) throws IOException {
        Objects.requireNonNull( resourceName, "resourceName" );

        // Collect originating files for incremental compilation
        List<KSFile> originatingFiles = collectOriginatingFiles( originatingElements );

        Dependencies dependencies = originatingFiles.isEmpty()
            ? Dependencies.Companion.getALL_FILES()
            : new Dependencies( false, originatingFiles.toArray( new KSFile[0] ) );

        // Extract package and file name from resource path
        int lastSlash = resourceName.lastIndexOf( '/' );
        String pkg = lastSlash > 0 ? resourceName.substring( 0, lastSlash ).replace( '/', '.' ) : "";
        String fileName = lastSlash >= 0 ? resourceName.substring( lastSlash + 1 ) : resourceName;

        // Extract extension
        int lastDot = fileName.lastIndexOf( '.' );
        String baseName = lastDot > 0 ? fileName.substring( 0, lastDot ) : fileName;
        String extension = lastDot > 0 ? fileName.substring( lastDot + 1 ) : "";

        OutputStream outputStream = codeGenerator.createNewFile(
            dependencies,
            pkg,
            baseName,
            extension
        );

        return new OutputStreamWriter( outputStream, StandardCharsets.UTF_8 );
    }

    private List<KSFile> collectOriginatingFiles(List<Object> originatingElements) {
        List<KSFile> originatingFiles = new ArrayList<>();
        if ( originatingElements != null ) {
            for ( Object element : originatingElements ) {
                KSFile file = extractKSFile( element );
                if ( file != null ) {
                    originatingFiles.add( file );
                }
            }
        }
        return originatingFiles;
    }

    private KSFile extractKSFile(Object element) {
        if ( element instanceof KSClassDeclaration ) {
            return ( (KSClassDeclaration) element ).getContainingFile();
        }
        if ( element instanceof TypeElementDescriptor ) {
            KSClassDeclaration classDecl = descriptorUnwrapper.type(
                (TypeElementDescriptor) element, KSClassDeclaration.class ).orElse( null );
            if ( classDecl != null ) {
                return classDecl.getContainingFile();
            }
        }
        return null;
    }
}
