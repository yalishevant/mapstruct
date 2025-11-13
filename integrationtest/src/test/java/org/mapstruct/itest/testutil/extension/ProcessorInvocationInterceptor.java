/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.itest.testutil.extension;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.maven.it.Verifier;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.junit.platform.commons.util.ReflectionUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import static org.apache.maven.shared.utils.io.FileUtils.deleteDirectory;
import static org.mapstruct.itest.testutil.extension.ProcessorTestTemplateInvocationContext.CURRENT_VERSION;

/**
 * @author Filip Hrisafov
 * @author Andreas Gudian
 */
public class ProcessorInvocationInterceptor implements InvocationInterceptor {

    /**
     * System property to enable remote debugging of the processor execution in the integration test
     */
    public static final String SYS_PROP_DEBUG = "processorIntegrationTest.debug";

    private final ProcessorTestContext processorTestContext;

    public ProcessorInvocationInterceptor(ProcessorTestContext processorTestContext) {
        this.processorTestContext = processorTestContext;
    }

    @Override
    public void interceptTestTemplateMethod(Invocation<Void> invocation,
        ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {
        try {
            doExecute( extensionContext );
            invocation.proceed();
        }
        catch ( Exception e ) {
            invocation.skip();
            throw e;
        }
    }

    private void doExecute(ExtensionContext extensionContext) throws Exception {
        File destination = extractTest( extensionContext );
        PrintStream originalOut = System.out;

        final Verifier verifier;
        if ( Boolean.getBoolean( SYS_PROP_DEBUG ) ) {
            // the compiler is executed within the Maven JVM. So make
            // sure we fork a new JVM for that, and let that new JVM use the command 'mvnDebug' instead of 'mvn'
            verifier = new Verifier( destination.getCanonicalPath(), null, true, true );
            verifier.setDebugJvm( true );
        }
        else {
            verifier = new Verifier( destination.getCanonicalPath() );
            if ( processorTestContext.isForkJvm() ) {
                verifier.setForkJvm( true );
            }
        }

        List<String> goals = new ArrayList<>( 3 );

        goals.add( "clean" );

        try {
            configureProcessor( verifier );

            String mapStructVersion = resolveMapStructVersion();
            if ( hasText( mapStructVersion ) ) {
                verifier.setSystemProperty( "mapstruct.version", mapStructVersion );
            }

            verifier.setSystemProperty( "compiler-source-target-version", sourceTargetVersion() );

            if ( Boolean.getBoolean( SYS_PROP_DEBUG ) ) {
                originalOut.print( "Processor Integration Test: " );
                originalOut.println( "Listening for transport dt_socket at address: 8000 (in some seconds)" );
            }

            goals.add( "test" );

            addAdditionalCliArguments( verifier );

            originalOut.println( extensionContext.getRequiredTestClass().getSimpleName() + "." +
                extensionContext.getRequiredTestMethod().getName() + " executing " +
                processorTestContext.getProcessor().name().toLowerCase() );

            verifier.executeGoals( goals );
            verifier.verifyErrorFreeLog();
        }
        finally {
            verifier.resetStreams();
        }
    }

    private void addAdditionalCliArguments(Verifier verifier)
        throws Exception {
        Class<? extends ProcessorTest.CommandLineEnhancer> cliEnhancerClass =
            processorTestContext.getCliEnhancerClass();

        Constructor<? extends ProcessorTest.CommandLineEnhancer> cliEnhancerConstructor = null;
        if ( cliEnhancerClass != ProcessorTest.CommandLineEnhancer.class ) {
            try {
                cliEnhancerConstructor = cliEnhancerClass.getConstructor();
                ProcessorTest.CommandLineEnhancer enhancer = cliEnhancerConstructor.newInstance();
                Collection<String> additionalArgs = enhancer.getAdditionalCommandLineArguments(
                    processorTestContext.getProcessor(), CURRENT_VERSION );

                for ( String arg : additionalArgs ) {
                    verifier.addCliOption( arg );
                }

            }
            catch ( NoSuchMethodException e ) {
                throw new RuntimeException( cliEnhancerClass + " does not have a default constructor." );
            }
            catch ( SecurityException e ) {
                throw new RuntimeException( e );
            }
        }
    }

    private void configureProcessor(Verifier verifier) {
        ProcessorTest.ProcessorType processor = processorTestContext.getProcessor();
        String compilerId = processor.getCompilerId();
        if ( compilerId != null ) {
            String profile = processor.getProfile();
            if ( profile == null ) {
                profile = "generate-via-compiler-plugin";
            }
            verifier.addCliOption( "-P" + profile );
            verifier.setSystemProperty( "compiler-id", compilerId );
            if ( processor == ProcessorTest.ProcessorType.JAVAC ) {
                if ( CURRENT_VERSION.ordinal() >= JRE.JAVA_21.ordinal() ) {
                    verifier.setSystemProperty( "maven.compiler.proc", "full" );
                }
            }
        }
        else {
            verifier.addCliOption( "-Pgenerate-via-processor-plugin" );
        }
    }

    private File extractTest(ExtensionContext extensionContext) throws IOException {
        String tmpDir = getTmpDir();

        String tempDirName = extensionContext.getRequiredTestClass().getPackage().getName() + "." +
            extensionContext.getRequiredTestMethod().getName();
        File tempDirBase = new File( tmpDir, tempDirName ).getCanonicalFile();

        if ( !tempDirBase.exists() ) {
            tempDirBase.mkdirs();
        }

        File parentPom = new File( tempDirBase, "pom.xml" );
        copyResourceFile( "pom.xml", parentPom );

        ProcessorTest.ProcessorType processorType = processorTestContext.getProcessor();
        File tempDir = new File( tempDirBase, processorType.name().toLowerCase() );
        deleteDirectory( tempDir );
        Files.createDirectories( tempDir.toPath() );

        Path sourceDir = resolveResourcePath( processorTestContext.getBaseDir() );
        copyResourceDirectory( sourceDir, tempDir );
        Path parentPomPath = tempDirBase.toPath().resolve( "pom.xml" );
        if ( Files.exists( parentPomPath ) ) {
            Files.copy(
                parentPomPath,
                tempDir.toPath().resolve( "pom.xml" ),
                StandardCopyOption.REPLACE_EXISTING
            );
            Path parentDir = tempDirBase.toPath().resolve( "parent" );
            if ( Files.exists( parentDir.resolve( "pom.xml" ) ) ) {
                Path targetParentDir = tempDir.toPath().resolve( "parent" );
                Files.createDirectories( targetParentDir );
                Files.copy(
                    parentDir.resolve( "pom.xml" ),
                    targetParentDir.resolve( "pom.xml" ),
                    StandardCopyOption.REPLACE_EXISTING
                );
            }
        }
        return tempDir;
    }

    private void copyResourceFile(String resourceName, File destination) throws IOException {
        Path source = resolveResourcePath( resourceName );
        Files.createDirectories( destination.getParentFile().toPath() );
        Files.copy( source, destination.toPath(), StandardCopyOption.REPLACE_EXISTING );

        if ( "pom.xml".equals( resourceName ) ) {
            Path tempDirBase = destination.getParentFile().toPath();
            copyParentPom( tempDirBase );
            rewriteParentReference( tempDirBase, destination.toPath() );
            unescapePlaceholders( tempDirBase );
            unescapePlaceholders( destination.toPath() );
        }
    }

    private void copyResourceDirectory(Path source, File destination) throws IOException {
        if ( Files.isDirectory( source ) ) {
            Path destinationPath = destination.toPath();
            Files.walk( source )
                .forEach( path -> {
                    try {
                        Path relative = source.relativize( path );
                        Path target = destinationPath.resolve( relative );
                        if ( Files.isDirectory( path ) ) {
                            Files.createDirectories( target );
                        }
                        else {
                            Files.createDirectories( target.getParent() );
                            Files.copy( path, target, StandardCopyOption.REPLACE_EXISTING );
                        }
                    }
                    catch ( IOException e ) {
                        throw new UncheckedIOException( e );
                    }
                } );
            unescapePlaceholders( destinationPath );
        }
        else {
            Files.createDirectories( destination.toPath() );
            Path targetPath = destination.toPath().resolve( source.getFileName() );
            Files.copy( source, targetPath, StandardCopyOption.REPLACE_EXISTING );
            unescapePlaceholders( targetPath );
        }
    }

    private void unescapePlaceholders(Path directory) throws IOException {
        if ( directory == null || !Files.exists( directory ) ) {
            return;
        }

        if ( Files.isDirectory( directory ) ) {
            Files.walk( directory )
                .filter( Files::isRegularFile )
                .filter( path -> "pom.xml".equals( path.getFileName().toString() ) )
                .forEach( this::unescapePlaceholder );
        }
        else if ( Files.isRegularFile( directory ) && "pom.xml".equals( directory.getFileName().toString() ) ) {
            unescapePlaceholder( directory );
        }
    }

    private void unescapePlaceholder(Path path) {
        try {
            String content = new String( Files.readAllBytes( path ), StandardCharsets.UTF_8 );
            String updated = content
                .replace( "\\${compiler-source-target-version}", "${compiler-source-target-version}" )
                .replace( "\\${compiler-id}", "${compiler-id}" )
                .replace( "\\${project.build.directory}", "${project.build.directory}" )
                .replace( "\\${project.build.resources[0].directory}", "${project.build.resources[0].directory}" );
            if ( !content.equals( updated ) ) {
                Files.write( path, updated.getBytes( StandardCharsets.UTF_8 ) );
            }
        }
        catch ( IOException e ) {
            throw new RuntimeException( "Failed to unescape placeholders in " + path, e );
        }
    }

    private Path resolveResourcePath(String resourceName) {
        String normalized = resourceName.startsWith( "/" ) ? resourceName.substring( 1 ) : resourceName;
        String relativePath = normalized.replace( '/', File.separatorChar );

        Path[] baseDirectories = new Path[] {
            Paths.get( "integrationtest", "target", "test-classes" ),
            Paths.get( "target", "test-classes" ),
            Paths.get( "integrationtest", "src", "test", "resources" ),
            Paths.get( "src", "test", "resources" ),
            Paths.get( "..", "integrationtest", "target", "test-classes" ),
            Paths.get( "..", "integrationtest", "src", "test", "resources" )
        };

        for ( Path baseDir : baseDirectories ) {
            Path candidate = baseDir.resolve( relativePath );
            if ( Files.exists( candidate ) ) {
                return candidate.toAbsolutePath().normalize();
            }
        }

        throw new IllegalArgumentException( "Unable to locate test resource '" + resourceName + "'" );
    }

    private void rewriteParentReference(Path tempDirBase, Path pomPath) throws IOException {
        String content = new String( Files.readAllBytes( pomPath ), StandardCharsets.UTF_8 );
        content = content.replace(
            "<relativePath>../../../../parent/pom.xml</relativePath>",
            "<relativePath>parent/pom.xml</relativePath>"
        );
        Files.write( pomPath, content.getBytes( StandardCharsets.UTF_8 ) );
    }

    private void copyParentPom(Path tempDirBase) throws IOException {
        Path targetDir = tempDirBase.resolve( "parent" );
        Files.createDirectories( targetDir );
        Path sourcePom = Paths.get( "parent", "pom.xml" );
        Files.copy( sourcePom, targetDir.resolve( "pom.xml" ), StandardCopyOption.REPLACE_EXISTING );
    }

    private String getTmpDir() {
        if ( CURRENT_VERSION == JRE.JAVA_8 ) {
            // On Java 8 the tmp dir is always
            // no matter we run from the aggregator or not
            return "target/tmp";
        }

        // On Java 11+ we need to do it base on the location relative
        String tmpDir;
        if ( Files.exists( Paths.get( "integrationtest" ) ) ) {
            // If it exists then we are running from the main aggregator
            tmpDir = "integrationtest/target/tmp";
        }
        else {
            tmpDir = "target/tmp";
        }
        return tmpDir;
    }

    private String sourceTargetVersion() {
        if ( CURRENT_VERSION == JRE.JAVA_8 ) {
            return "1.8";
        }
        else if ( CURRENT_VERSION == JRE.OTHER ) {
            try {
                // Extracting the major version is done with code from
                // org.junit.jupiter.api.condition.JRE when determining the current version

                // java.lang.Runtime.version() is a static method available on Java 9+
                // that returns an instance of java.lang.Runtime.Version which has the
                // following method: public int major()
                Method versionMethod = null;
                versionMethod = Runtime.class.getMethod( "version" );
                Object version = ReflectionUtils.invokeMethod( versionMethod, null );
                Method majorMethod = version.getClass().getMethod( "major" );
                return String.valueOf( (int) ReflectionUtils.invokeMethod( majorMethod, version ) );
            }
            catch ( NoSuchMethodException e ) {
                throw new RuntimeException( "Failed to get Java Version" );
            }
        }
        else {
            return CURRENT_VERSION.name().substring( 5 );
        }
    }

    private String resolveMapStructVersion() {
        String systemProperty = System.getProperty( "mapstruct.version" );
        if ( hasText( systemProperty ) ) {
            return systemProperty;
        }

        String versionFromMetadata = resolveVersionFromMetadata();
        if ( versionFromMetadata != null ) {
            return versionFromMetadata;
        }

        String versionFromPom = resolveVersionFromPomHierarchy();
        if ( versionFromPom != null ) {
            return versionFromPom;
        }

        throw new IllegalStateException(
            "Unable to resolve mapstruct.version system property; please ensure the build sets it explicitly." );
    }

    private String resolveVersionFromMetadata() {
        try ( InputStream pomProperties = getClass().getClassLoader().getResourceAsStream(
            "META-INF/maven/org.mapstruct/mapstruct-integrationtest/pom.properties" ) ) {
            if ( pomProperties == null ) {
                return null;
            }
            Properties properties = new Properties();
            properties.load( pomProperties );
            String version = properties.getProperty( "version" );
            return hasText( version ) ? version : null;
        }
        catch ( IOException e ) {
            throw new IllegalStateException( "Failed to resolve mapstruct.version from pom.properties", e );
        }
    }

    private String resolveVersionFromPomHierarchy() {
        for ( Path candidate : candidatePomPaths() ) {
            String version = resolveVersionFromPom( candidate );
            if ( hasText( version ) ) {
                return version;
            }
        }
        return null;
    }

    private List<Path> candidatePomPaths() {
        Set<Path> candidates = new LinkedHashSet<>();
        Path current = Paths.get( "" ).toAbsolutePath().normalize();
        for ( int depth = 0; depth < 6 && current != null; depth++ ) {
            candidates.add( current.resolve( "parent" ).resolve( "pom.xml" ) );
            candidates.add( current.resolve( "pom.xml" ) );
            current = current.getParent();
        }
        List<Path> ordered = new ArrayList<>( candidates.size() );
        for ( Path candidate : candidates ) {
            if ( Files.exists( candidate ) ) {
                ordered.add( candidate );
            }
        }
        return ordered;
    }

    private String resolveVersionFromPom(Path pomPath) {
        if ( pomPath == null || !Files.exists( pomPath ) ) {
            return null;
        }
        try ( InputStream inputStream = Files.newInputStream( pomPath ) ) {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware( true );
            enableSecureProcessing( factory );
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse( inputStream );
            Element project = document.getDocumentElement();
            if ( project == null ) {
                return null;
            }

            String projectVersion = childTextContent( project, "version" );
            if ( projectVersion != null ) {
                return projectVersion;
            }

            Element parent = childElement( project, "parent" );
            if ( parent != null ) {
                String parentVersion = childTextContent( parent, "version" );
                if ( parentVersion != null ) {
                    return parentVersion;
                }
            }
            return null;
        }
        catch ( ParserConfigurationException | SAXException | IOException e ) {
            throw new IllegalStateException( "Failed to parse " + pomPath + " when resolving mapstruct.version", e );
        }
    }

    private void enableSecureProcessing(DocumentBuilderFactory factory) throws ParserConfigurationException {
        factory.setFeature( XMLConstants.FEATURE_SECURE_PROCESSING, true );
        factory.setFeature( "http://apache.org/xml/features/disallow-doctype-decl", true );
        factory.setFeature( "http://xml.org/sax/features/external-general-entities", false );
        factory.setFeature( "http://xml.org/sax/features/external-parameter-entities", false );
        factory.setExpandEntityReferences( false );
    }

    private Element childElement(Element parent, String name) {
        NodeList children = parent.getChildNodes();
        for ( int i = 0; i < children.getLength(); i++ ) {
            Node node = children.item( i );
            if ( node instanceof Element && name.equals( node.getNodeName() ) ) {
                return (Element) node;
            }
        }
        return null;
    }

    private String childTextContent(Element parent, String name) {
        Element child = childElement( parent, name );
        if ( child == null ) {
            return null;
        }
        String textContent = child.getTextContent();
        if ( textContent == null ) {
            return null;
        }
        String value = textContent.trim();
        return value.isEmpty() ? null : value;
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
