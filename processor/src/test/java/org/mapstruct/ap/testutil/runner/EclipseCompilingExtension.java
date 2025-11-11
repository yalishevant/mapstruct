/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.testutil.runner;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.processing.Processor;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileManager.Location;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;

import org.eclipse.jdt.internal.compiler.tool.EclipseCompiler;
import org.mapstruct.ap.MappingProcessor;
import org.mapstruct.ap.testutil.compilation.model.CompilationOutcomeDescriptor;

/**
 * Extension that uses the Eclipse compiler (ECJ) to compile sources.
 *
 * @author MapStruct contributors
 */
class EclipseCompilingExtension extends CompilingExtension {

    private static final List<File> COMPILER_CLASSPATH_FILES = asFiles( TEST_COMPILATION_CLASSPATH );

    private static final ClassLoader DEFAULT_PROCESSOR_CLASSLOADER =
        new ModifiableURLClassLoader( new FilteringParentClassLoader( "org.mapstruct." )
            .allowingPackage( "org.mapstruct.ap.internal.version." ) )
                .withPaths( PROCESSOR_CLASSPATH );

    EclipseCompilingExtension() {
        super( Compiler.ECLIPSE );
    }

    @Override
    protected CompilationOutcomeDescriptor compileWithSpecificCompiler(CompilationRequest compilationRequest,
                                                                       String sourceOutputDir,
                                                                       String classOutputDir,
                                                                       String additionalCompilerClasspath) {
        JavaCompiler compiler = new EclipseCompiler();
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager( null, null, StandardCharsets.UTF_8 );

        String release = determineReleaseVersion();

        Iterable<? extends JavaFileObject> compilationUnits =
            fileManager.getJavaFileObjectsFromFiles( getSourceFiles( compilationRequest.getSourceClasses() ) );

        boolean isEclipseCompiler = fileManager.getClass().getName().contains( "org.eclipse.jdt" );
        ModuleSetup moduleSetup;
        try {
            fileManager.setLocation( StandardLocation.CLASS_PATH, getCompilerClasspathFiles( compilationRequest ) );
            fileManager.setLocation( StandardLocation.CLASS_OUTPUT, Arrays.asList( new File( classOutputDir ) ) );
            fileManager.setLocation( StandardLocation.SOURCE_OUTPUT, Arrays.asList( new File( sourceOutputDir ) ) );
            moduleSetup = configureJavaRuntime( fileManager, release, isEclipseCompiler );
        }
        catch ( IOException e ) {
            throw new RuntimeException( e );
        }

        ClassLoader processorClassloader;
        if ( additionalCompilerClasspath == null ) {
            processorClassloader = DEFAULT_PROCESSOR_CLASSLOADER;
        }
        else {
            processorClassloader = new ModifiableURLClassLoader(
                new FilteringParentClassLoader( "org.mapstruct." )
                    .allowingPackage( "org.mapstruct.ap.internal.version." ) )
                    .withPaths( PROCESSOR_CLASSPATH )
                    .withPath( additionalCompilerClasspath )
                    .withOriginsOf( compilationRequest.getServices().values() );
        }

        List<String> options = new ArrayList<>( compilationRequest.getProcessorOptions() );
        options.add( "-d" );
        options.add( classOutputDir );
        options.add( "-s" );
        options.add( sourceOutputDir );
        options.add( "-warn:-raw,-unchecked,-serial,-removal,-deprecation,-unused,-warningToken" );
        options.add( "--release" );
        options.add( release );
        if ( !moduleSetup.modulePaths.isEmpty() ) {
            options.add( "--module-path" );
            options.add( joinPaths( moduleSetup.modulePaths ) );
        }
        if ( moduleSetup.systemRoot != null ) {
            options.add( "--system" );
            options.add( moduleSetup.systemRoot.toString() );
        }

        if ( Boolean.getBoolean( "mapstruct.debug.compiler" ) ) {
            System.out.println( "[" + compiler + "] options " + options );
        }

        CompilationTask task =
            compiler.getTask(
                null,
                fileManager,
                diagnostics,
                options,
                null,
                compilationUnits );

        task.setProcessors(
            Arrays.asList( (Processor) loadAndInstantiate( processorClassloader, MappingProcessor.class ) ) );

        boolean compilationSuccessful = task.call();

        if ( !compilationSuccessful && Boolean.getBoolean( "mapstruct.debug.compiler" ) ) {
            diagnostics.getDiagnostics().forEach( d -> System.out.println( "[ECJ diagnostic] " + d ) );
        }

        return CompilationOutcomeDescriptor.forResult(
            SOURCE_DIR,
            compilationSuccessful,
            diagnostics.getDiagnostics() );
    }

    private static List<File> getCompilerClasspathFiles(CompilationRequest request) {
        Collection<String> testDependencies = request.getTestDependencies();
        if ( testDependencies.isEmpty() ) {
            return COMPILER_CLASSPATH_FILES;
        }

        List<File> compilerClasspathFiles = new ArrayList<>(
            COMPILER_CLASSPATH_FILES.size() + testDependencies.size() );

        compilerClasspathFiles.addAll( COMPILER_CLASSPATH_FILES );
        for ( String testDependencyPath : filterBootClassPath( testDependencies ) ) {
            compilerClasspathFiles.add( new File( testDependencyPath ) );
        }

        return compilerClasspathFiles;
    }

    private static List<File> asFiles(List<String> paths) {
        List<File> classpath = new ArrayList<>();
        for ( String path : paths ) {
            classpath.add( new File( path ) );
        }

        return classpath;
    }

    private static String determineReleaseVersion() {
        String configured = System.getProperty( "maven.compiler.release" );
        if ( configured != null && !configured.isEmpty() ) {
            return configured;
        }

        String specVersion = System.getProperty( "java.specification.version" );
        int feature = 8;
        if ( specVersion != null && !specVersion.isEmpty() ) {
            if ( specVersion.contains( "." ) ) {
                String[] parts = specVersion.split( "\\." );
                feature = Integer.parseInt( parts[parts.length - 1] );
            }
            else {
                feature = Integer.parseInt( specVersion );
            }
        }

        if ( feature >= 21 ) {
            return "21";
        }
        return Integer.toString( feature );
    }

    private static ModuleSetup configureJavaRuntime(StandardJavaFileManager fileManager, String release,
                                                   boolean configureSystemModules) throws IOException {
        int releaseVersion = parseRelease( release );

        if ( configureSystemModules && releaseVersion >= 9 ) {
            List<Path> systemModulePaths = detectSystemModulePaths();
            Location systemModulesLocation = resolveSystemModulesLocation();
            if ( systemModulesLocation != null && !systemModulePaths.isEmpty() ) {
                try {
                    fileManager.setLocation(
                        systemModulesLocation,
                        toFiles( systemModulePaths )
                    );
                    return new ModuleSetup( systemModulePaths, Paths.get( System.getProperty( "java.home" ) ) );
                }
                catch ( UnsupportedOperationException | IllegalArgumentException ex ) {
                    // Fallback to legacy configuration below when the compiler does not support module locations.
                }
            }
        }

        return ModuleSetup.none();
    }

    private static int parseRelease(String release) {
        try {
            return Integer.parseInt( release );
        }
        catch ( NumberFormatException ex ) {
            return 8;
        }
    }

    private static List<Path> detectSystemModulePaths() {
        Path javaHome = Paths.get( System.getProperty( "java.home" ) );
        List<Path> result = new ArrayList<>();

        addIfExists( result, javaHome.resolve( "lib" ).resolve( "modules" ) );

        Path parent = javaHome.getParent();
        if ( parent != null ) {
            addIfExists( result, parent.resolve( "lib" ).resolve( "modules" ) );
        }

        return result;
    }

    private static List<File> toFiles(List<Path> paths) {
        List<File> files = new ArrayList<>( paths.size() );
        for ( Path path : paths ) {
            files.add( path.toFile() );
        }
        return files;
    }

    private static String joinPaths(List<Path> paths) {
        return paths.stream()
            .map( Path::toString )
            .collect( Collectors.joining( File.pathSeparator ) );
    }

    private static Location resolveSystemModulesLocation() {
        try {
            return StandardLocation.valueOf( "SYSTEM_MODULES" );
        }
        catch ( IllegalArgumentException ex ) {
            return null;
        }
    }

    private static List<File> detectLegacyRuntimeClasspath() {
        Path javaHome = Paths.get( System.getProperty( "java.home" ) );
        List<File> result = new ArrayList<>();

        addIfFile( result, javaHome.resolve( "lib" ).resolve( "rt.jar" ) );
        addIfFile( result, javaHome.resolve( "lib" ).resolve( "jce.jar" ) );

        Path parent = javaHome.getParent();
        if ( parent != null ) {
            addIfFile( result, parent.resolve( "lib" ).resolve( "rt.jar" ) );
            addIfFile( result, parent.resolve( "lib" ).resolve( "jce.jar" ) );
            addIfFile( result, parent.resolve( "Classes" ).resolve( "classes.jar" ) );
        }

        return result;
    }

    private static void addIfDirectory(List<Path> targets, Path candidate) {
        if ( candidate != null && Files.isDirectory( candidate ) ) {
            targets.add( candidate );
        }
    }

    private static void addIfExists(List<Path> targets, Path candidate) {
        if ( candidate != null && Files.exists( candidate ) ) {
            targets.add( candidate );
        }
    }

    private static void addIfFile(List<File> targets, Path candidate) {
        if ( candidate != null && Files.isRegularFile( candidate ) ) {
            targets.add( candidate.toFile() );
        }
    }

    private static final class ModuleSetup {
        private final List<Path> modulePaths;
        private final Path systemRoot;

        private ModuleSetup(List<Path> modulePaths, Path systemRoot) {
            this.modulePaths = modulePaths;
            this.systemRoot = systemRoot;
        }

        private static ModuleSetup none() {
            return new ModuleSetup( Collections.emptyList(), null );
        }
    }
}
