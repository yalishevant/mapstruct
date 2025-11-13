/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.javax;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.jar.Manifest;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.SourceVersion;

import org.mapstruct.ap.internal.version.VersionInformation;

/**
 * Provides information about the processor version and the processor context implementation version.
 * <p>
 * Separated into an interface and this implementation to avoid cyclic dependencies between the processor package and
 * the model package.
 *
 * @author Andreas Gudian
 */
public class DefaultVersionInformation implements VersionInformation {
    private static final String JAVAC_PE_CLASS = "com.sun.tools.javac.processing.JavacProcessingEnvironment";
    private static final String COMPILER_NAME_JAVAC = "javac";

    private static final String JDT_IDE_PE_CLASS =
        "org.eclipse.jdt.internal.apt.pluggable.core.dispatch.IdeBuildProcessingEnvImpl";
    private static final String JDT_BATCH_PE_CLASS =
        "org.eclipse.jdt.internal.compiler.apt.dispatch.BatchProcessingEnvImpl";
    private static final String COMPILER_NAME_ECLIPSE_JDT = "Eclipse JDT";

    private static final String MAP_STRUCT_VERSION = initMapStructVersion();

    private final String runtimeVersion;
    private final String runtimeVendor;
    private final String compiler;
    private final boolean sourceVersionAtLeast9;
    private final boolean sourceVersionAtLeast19;
    private final boolean eclipseJDT;
    private final boolean javac;

    DefaultVersionInformation(String runtimeVersion, String runtimeVendor, String compiler,
        SourceVersion sourceVersion) {
        this.runtimeVersion = runtimeVersion;
        this.runtimeVendor = runtimeVendor;
        this.compiler = compiler;
        this.eclipseJDT = compiler.startsWith( COMPILER_NAME_ECLIPSE_JDT );
        this.javac = compiler.startsWith( COMPILER_NAME_JAVAC );
        // If the difference between the source version and RELEASE_6 is more that 2 than we are at least on 9
        this.sourceVersionAtLeast9 = sourceVersion.compareTo( SourceVersion.RELEASE_6 ) > 2;
        this.sourceVersionAtLeast19 = sourceVersion.compareTo( SourceVersion.RELEASE_6 ) > 12;
    }

    @Override
    public String getRuntimeVersion() {
        return this.runtimeVersion;
    }

    @Override
    public String getRuntimeVendor() {
        return this.runtimeVendor;
    }

    @Override
    public String getMapStructVersion() {
        return MAP_STRUCT_VERSION;
    }

    @Override
    public String getCompiler() {
        return this.compiler;
    }

    @Override
    public boolean isSourceVersionAtLeast9() {
        return sourceVersionAtLeast9;
    }

    @Override
    public boolean isSourceVersionAtLeast19() {
        return sourceVersionAtLeast19;
    }

    @Override
    public boolean isEclipseJDTCompiler() {
        return eclipseJDT;
    }

    @Override
    public boolean isJavacCompiler() {
        return javac;
    }

    public static DefaultVersionInformation fromProcessingEnvironment(ProcessingEnvironment processingEnv) {
        String runtimeVersion = System.getProperty( "java.version" );
        String runtimeVendor = System.getProperty( "java.vendor" );

        String compiler = getCompiler( processingEnv );

        return new DefaultVersionInformation(
            runtimeVersion,
            runtimeVendor,
            compiler,
            processingEnv.getSourceVersion()
        );
    }

    private static String getCompiler(ProcessingEnvironment processingEnv) {
        String className;
        if ( Proxy.isProxyClass( processingEnv.getClass() ) ) {
            // IntelliJ IDEA wraps the ProcessingEnvironment in a Proxy.
            // Therefore we need smarter logic to determine the type  of the compiler
            String processingEnvToString = processingEnv.toString();
            if ( processingEnvToString.contains( COMPILER_NAME_JAVAC ) ) {
                // The toString of the javac ProcessingEnvironment is "javac ProcessingEnvironment"
                className = JAVAC_PE_CLASS;
            }
            else if ( processingEnvToString.startsWith( JDT_BATCH_PE_CLASS ) ) {
                // The toString of the JDT Batch is from Object#toString so it contains the class name
                className = JDT_BATCH_PE_CLASS;
            }
            else {
                InvocationHandler invocationHandler = Proxy.getInvocationHandler( processingEnv );
                return "Proxy handler " + invocationHandler.getClass() + " from " +
                    getLibraryName( invocationHandler.getClass(), false );
            }
        }
        else {
            className = processingEnv.getClass().getName();
        }

        if ( className.equals( JAVAC_PE_CLASS ) ) {
            return COMPILER_NAME_JAVAC;
        }

        if ( className.equals( JDT_IDE_PE_CLASS ) ) {
            // the processing environment for the IDE integrated APT is in a different bundle than the APT classes
            return COMPILER_NAME_ECLIPSE_JDT + " (IDE) "
                + getLibraryName( processingEnv.getTypeUtils().getClass(), true );
        }

        if ( className.equals( JDT_BATCH_PE_CLASS ) ) {
            return COMPILER_NAME_ECLIPSE_JDT + " (Batch) " + getLibraryName( processingEnv.getClass(), true );
        }

        return processingEnv.getClass().getSimpleName() + " from " + getLibraryName( processingEnv.getClass(), false );
    }

    private static String getLibraryName(Class<?> clazz, boolean preferVersionOnly) {
        String classFileName = asClassFileName( clazz.getName() );
        URL resource = clazz.getClassLoader().getResource( classFileName );

        Manifest manifest = openManifest( classFileName, resource );
        if ( preferVersionOnly && manifest != null ) {
            String version = manifest.getMainAttributes().getValue( "Implementation-Version" );
            if ( version != null ) {
                return version;
            }
        }

        if ( resource == null ) {
            return clazz.getName();
        }
        try {
            return new URL( resource.toExternalForm().replace( classFileName, "" ) ).toExternalForm();
        }
        catch ( MalformedURLException e ) {
            return resource.toExternalForm();
        }
    }

    private static Manifest openManifest(String classFileName, URL resource) {
        if ( resource == null ) {
            return null;
        }

        try {
            if ( "jar".equals( resource.getProtocol() ) ) {
                String path = resource.getPath();
                URL manifestUrl = new URL( path.substring( 0, path.lastIndexOf( '!' ) + 2 ) + "META-INF/MANIFEST.MF" );
                try ( java.io.InputStream stream = manifestUrl.openStream() ) {
                    return new Manifest( stream );
                }
            }
        }
        catch ( IOException e ) {
            // fall-through
        }
        return null;
    }

    private static String asClassFileName(String className) {
        return className.replace( '.', '/' ) + ".class";
    }

    private static String initMapStructVersion() {
        Manifest manifest = openManifest(
            asClassFileName( DefaultVersionInformation.class.getName() ),
            DefaultVersionInformation.class.getClassLoader().getResource(
                asClassFileName( DefaultVersionInformation.class.getName() )
            )
        );
        if ( manifest != null ) {
            String version = manifest.getMainAttributes().getValue( "Implementation-Version" );
            if ( version != null ) {
                return version;
            }
        }
        Package pack = DefaultVersionInformation.class.getPackage();
        return pack != null ? pack.getImplementationVersion() : null;
    }
}
