/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.validation;

import static java.util.stream.Collectors.joining;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationValueDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.BuilderDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.ElementDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.ExecutableDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.FieldDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.LangElementKind;
import org.mapstruct.ap.internal.langmodel.descriptor.LangModifier;
import org.mapstruct.ap.internal.langmodel.descriptor.LangTypeKind;
import org.mapstruct.ap.internal.langmodel.descriptor.NameDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.ParameterDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.RecordComponentDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeElementDescriptor;
import org.mapstruct.ap.internal.langmodel.AnnotationAttribute;
import org.mapstruct.ap.internal.langmodel.ExecutableSignature;
import org.mapstruct.ap.internal.langmodel.GeneratedFileAccess;
import org.mapstruct.ap.internal.langmodel.LangDescriptorFactory;
import org.mapstruct.ap.internal.langmodel.LangModelContext;
import org.mapstruct.ap.internal.langmodel.LangModelContextFactory;
import org.mapstruct.ap.internal.langmodel.LangModelElementQuery;
import org.mapstruct.ap.internal.langmodel.LangModelTypeSystem;
import org.mapstruct.ap.internal.langmodel.MapperAnnotation;
import org.mapstruct.ap.internal.langmodel.MapperConfigAnnotation;
import org.mapstruct.ap.internal.langmodel.MapperEntryPoint;
import org.mapstruct.ap.internal.langmodel.OptionalCapability;
import org.mapstruct.ap.internal.langmodel.TypeIntrospector;
import org.mapstruct.ap.internal.langmodel.api.BuilderIntrospectorContext;
import org.mapstruct.ap.internal.langmodel.api.DescriptorUnwrapper;
import org.mapstruct.ap.internal.langmodel.api.EnumMappingSupport;
import org.mapstruct.ap.internal.langmodel.api.LangElements;
import org.mapstruct.ap.internal.langmodel.api.LangTypes;
import org.mapstruct.ap.internal.langmodel.api.MappingExclusionSupport;
import org.mapstruct.ap.internal.langmodel.api.PackageDescriptor;
import org.mapstruct.ap.internal.langmodel.codegen.GeneratedFileSink;

/**
 * Guards the public surface of the thin language model API.
 *
 * <p>The snapshot keeps the minimal contracts stable so that accidental additions are caught immediately.</p>
 */
class ThinApiSurfaceTest {

    private static final List<Class<?>> THIN_API_TYPES = List.of(
        AnnotationAttribute.class,
        ExecutableSignature.class,
        GeneratedFileAccess.class,
        LangDescriptorFactory.class,
        LangModelContext.class,
        LangModelContextFactory.class,
        LangModelElementQuery.class,
        LangModelTypeSystem.class,
        MapperAnnotation.class,
        MapperConfigAnnotation.class,
        MapperEntryPoint.class,
        OptionalCapability.class,
        TypeIntrospector.class,
        TypeIntrospector.Metadata.class,
        TypeIntrospector.Metadata.Names.class,
        TypeIntrospector.Metadata.TypeFlags.class,
        BuilderIntrospectorContext.class,
        DescriptorUnwrapper.class,
        EnumMappingSupport.class,
        LangElements.class,
        LangTypes.class,
        MappingExclusionSupport.class,
        PackageDescriptor.class,
        GeneratedFileSink.class,
        AnnotationDescriptor.class,
        AnnotationValueDescriptor.class,
        BuilderDescriptor.class,
        ElementDescriptor.class,
        ExecutableDescriptor.class,
        FieldDescriptor.class,
        LangElementKind.class,
        LangModifier.class,
        LangTypeKind.class,
        NameDescriptor.class,
        ParameterDescriptor.class,
        RecordComponentDescriptor.class,
        TypeDescriptor.class,
        TypeElementDescriptor.class
    );

    @Test
    void thinApiMatchesSnapshot() throws IOException {
        Map<String, TypeSurface> surfaces = new LinkedHashMap<>();
        for ( Class<?> type : THIN_API_TYPES ) {
            surfaces.put( type.getName(), describe( type ) );
        }

        String actualJson = toJson( surfaces );
        Path snapshotPath = resolveSnapshotPath();

        if ( !Files.exists( snapshotPath ) ) {
            fail( missingSnapshotMessage( snapshotPath, actualJson ) );
        }

        String expectedJson = Files.readString( snapshotPath, StandardCharsets.UTF_8 ).replace( "\r\n", "\n" );
        assertThat( actualJson )
            .as( () -> mismatchMessage( snapshotPath, actualJson ) )
            .isEqualTo( expectedJson );
    }

    private static Path resolveSnapshotPath() {
        Path moduleRelative = Paths.get( "thin-api-surface.json" );
        if ( Files.exists( moduleRelative ) ) {
            return moduleRelative;
        }

        Path repositoryRelative = Paths.get( "..", "thin-api-surface.json" ).normalize();
        if ( Files.exists( repositoryRelative ) ) {
            return repositoryRelative;
        }

        return moduleRelative;
    }

    private static String missingSnapshotMessage(Path snapshotPath, String actualJson) {
        return "Missing thin API snapshot at " + snapshotPath.toAbsolutePath() + System.lineSeparator()
            + "Add the generated snapshot to version control:" + System.lineSeparator()
            + actualJson;
    }

    private static String mismatchMessage(Path snapshotPath, String actualJson) {
        return "Thin API surface changed. Update " + snapshotPath + " when the change is intentional."
            + System.lineSeparator()
            + "Generated snapshot:" + System.lineSeparator()
            + actualJson;
    }

    private static TypeSurface describe(Class<?> type) {
        TypeSurface surface = new TypeSurface();
        surface.kind = determineKind( type );
        surface.typeParameters = formatTypeParameters( type.getTypeParameters() );
        surface.interfaces = formatInterfaces( type.getGenericInterfaces() );

        if ( type.isInterface() ) {
            surface.methods = formatMethods( type.getDeclaredMethods() );
        }
        else if ( type.isEnum() ) {
            surface.methods = formatMethods( type.getDeclaredMethods() );
            surface.enumConstants = formatEnumConstants( type );
        }
        else {
            surface.superClass = determineSuperClass( type.getGenericSuperclass() );
            surface.methods = formatMethods( type.getDeclaredMethods() );
            surface.constructors = formatConstructors( type.getDeclaredConstructors() );
        }

        return surface;
    }

    private static String determineKind(Class<?> type) {
        if ( type.isInterface() ) {
            return "interface";
        }
        if ( type.isEnum() ) {
            return "enum";
        }
        return "class";
    }

    private static String determineSuperClass(Type superType) {
        if ( superType == null ) {
            return null;
        }

        String name = superType.getTypeName();
        if ( Objects.equals( name, "java.lang.Object" ) || Objects.equals( name, "java.lang.Enum" ) ) {
            return null;
        }
        return name;
    }

    private static List<String> formatInterfaces(Type[] interfaces) {
        if ( interfaces.length == 0 ) {
            return Collections.emptyList();
        }
        return Arrays.stream( interfaces )
            .map( Type::getTypeName )
            .sorted()
            .collect( Collectors.toList() );
    }

    private static List<String> formatTypeParameters(TypeVariable<?>[] typeParameters) {
        if ( typeParameters.length == 0 ) {
            return Collections.emptyList();
        }

        List<String> formatted = new ArrayList<>();
        for ( TypeVariable<?> typeParameter : typeParameters ) {
            StringBuilder builder = new StringBuilder();
            builder.append( typeParameter.getName() );
            Type[] bounds = typeParameter.getBounds();
            List<String> explicitBounds = Arrays.stream( bounds )
                .filter( bound -> !isImplicitObjectBound( bound ) )
                .map( Type::getTypeName )
                .collect( Collectors.toList() );
            if ( !explicitBounds.isEmpty() ) {
                builder.append( " extends " ).append( String.join( " & ", explicitBounds ) );
            }
            formatted.add( builder.toString() );
        }
        return formatted;
    }

    private static boolean isImplicitObjectBound(Type bound) {
        if ( bound instanceof Class<?> ) {
            return ( (Class<?>) bound ) == Object.class;
        }
        return false;
    }

    private static List<String> formatMethods(Method[] methods) {
        List<String> formatted = new ArrayList<>();
        for ( Method method : methods ) {
            if ( !Modifier.isPublic( method.getModifiers() ) || method.isSynthetic() ) {
                continue;
            }
            formatted.add( formatExecutableSignature( method ) );
        }
        Collections.sort( formatted );
        return formatted;
    }

    private static List<String> formatConstructors(Constructor<?>[] constructors) {
        List<String> formatted = new ArrayList<>();
        for ( Constructor<?> constructor : constructors ) {
            if ( !Modifier.isPublic( constructor.getModifiers() ) || constructor.isSynthetic() ) {
                continue;
            }
            formatted.add( formatExecutableSignature( constructor ) );
        }
        Collections.sort( formatted );
        return formatted;
    }

    private static String formatExecutableSignature(Method method) {
        StringBuilder builder = new StringBuilder();
        if ( Modifier.isStatic( method.getModifiers() ) ) {
            builder.append( "static " );
        }
        if ( method.isDefault() ) {
            builder.append( "default " );
        }
        appendTypeParameters( builder, method.getTypeParameters() );
        builder.append( method.getGenericReturnType().getTypeName() )
            .append( " " )
            .append( method.getName() )
            .append( "(" )
            .append( formatParameterTypes( method.getGenericParameterTypes(), method.isVarArgs() ) )
            .append( ")" );
        appendThrowsClause( builder, method.getGenericExceptionTypes() );
        return builder.toString();
    }

    private static String formatExecutableSignature(Constructor<?> constructor) {
        StringBuilder builder = new StringBuilder();
        appendTypeParameters( builder, constructor.getTypeParameters() );
        builder.append( constructor.getDeclaringClass().getSimpleName() )
            .append( "(" )
            .append( formatParameterTypes( constructor.getGenericParameterTypes(), constructor.isVarArgs() ) )
            .append( ")" );
        appendThrowsClause( builder, constructor.getGenericExceptionTypes() );
        return builder.toString();
    }

    private static void appendTypeParameters(StringBuilder builder, TypeVariable<?>[] typeParameters) {
        if ( typeParameters.length == 0 ) {
            return;
        }
        builder.append( "<" )
            .append( formatTypeParameters( typeParameters ).stream().collect( joining( ", " ) ) )
            .append( "> " );
    }

    private static String formatParameterTypes(Type[] parameterTypes, boolean varArgs) {
        if ( parameterTypes.length == 0 ) {
            return "";
        }
        List<String> formatted = new ArrayList<>( parameterTypes.length );
        for ( int i = 0; i < parameterTypes.length; i++ ) {
            boolean isVarArg = varArgs && i == parameterTypes.length - 1;
            formatted.add( formatParameterType( parameterTypes[i], isVarArg ) );
        }
        return String.join( ", ", formatted );
    }

    private static String formatParameterType(Type type, boolean varArg) {
        if ( !varArg ) {
            return type.getTypeName();
        }

        if ( type instanceof Class<?> ) {
            Class<?> classType = (Class<?>) type;
            if ( classType.isArray() ) {
                return classType.getComponentType().getTypeName() + "...";
            }
        }
        else if ( type instanceof GenericArrayType ) {
            GenericArrayType arrayType = (GenericArrayType) type;
            return arrayType.getGenericComponentType().getTypeName() + "...";
        }
        else if ( type instanceof ParameterizedType ) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Type raw = parameterizedType.getRawType();
            if ( raw instanceof Class<?> && ( (Class<?>) raw ).isArray() ) {
                Class<?> rawArray = (Class<?>) raw;
                return rawArray.getComponentType().getTypeName() + "...";
            }
        }

        return type.getTypeName();
    }

    private static void appendThrowsClause(StringBuilder builder, Type[] exceptionTypes) {
        if ( exceptionTypes.length == 0 ) {
            return;
        }
        builder.append( " throws " )
            .append( Arrays.stream( exceptionTypes )
                .map( Type::getTypeName )
                .collect( joining( ", " ) ) );
    }

    private static List<String> formatEnumConstants(Class<?> type) {
        Object[] constants = type.getEnumConstants();
        List<String> names = new ArrayList<>( constants.length );
        for ( Object constant : constants ) {
            names.add( ( (Enum<?>) constant ).name() );
        }
        return names;
    }

    private static String toJson(Map<String, TypeSurface> surfaces) {
        StringBuilder builder = new StringBuilder();
        builder.append( "{\n" );
        List<Map.Entry<String, TypeSurface>> entries = new ArrayList<>( surfaces.entrySet() );
        for ( int i = 0; i < entries.size(); i++ ) {
            Map.Entry<String, TypeSurface> entry = entries.get( i );
            builder.append( "  \"" )
                .append( escape( entry.getKey() ) )
                .append( "\": " );
            appendTypeSurfaceJson( builder, entry.getValue(), "  " );
            if ( i < entries.size() - 1 ) {
                builder.append( "," );
            }
            builder.append( "\n" );
        }
        builder.append( "}\n" );
        return builder.toString();
    }

    private static void appendTypeSurfaceJson(StringBuilder builder, TypeSurface surface, String indent) {
        builder.append( "{\n" );
        String childIndent = indent + "  ";
        List<String> fields = new ArrayList<>();
        fields.add( formatStringField( childIndent, "kind", surface.kind ) );
        if ( surface.superClass != null ) {
            fields.add( formatStringField( childIndent, "superClass", surface.superClass ) );
        }
        if ( !surface.typeParameters.isEmpty() ) {
            fields.add( formatArrayField( childIndent, "typeParameters", surface.typeParameters ) );
        }
        fields.add( formatArrayField( childIndent, "interfaces", surface.interfaces ) );
        fields.add( formatArrayField( childIndent, "methods", surface.methods ) );
        if ( !surface.constructors.isEmpty() ) {
            fields.add( formatArrayField( childIndent, "constructors", surface.constructors ) );
        }
        if ( !surface.enumConstants.isEmpty() ) {
            fields.add( formatArrayField( childIndent, "enumConstants", surface.enumConstants ) );
        }

        for ( int i = 0; i < fields.size(); i++ ) {
            builder.append( fields.get( i ) );
            if ( i < fields.size() - 1 ) {
                builder.append( "," );
            }
            builder.append( "\n" );
        }
        builder.append( indent ).append( "}" );
    }

    private static String formatStringField(String indent, String name, String value) {
        return indent + "\"" + escape( name ) + "\": \"" + escape( value ) + "\"";
    }

    private static String formatArrayField(String indent, String name, List<String> values) {
        if ( values.isEmpty() ) {
            return indent + "\"" + escape( name ) + "\": []";
        }

        StringBuilder builder = new StringBuilder();
        builder.append( indent )
            .append( "\"" )
            .append( escape( name ) )
            .append( "\": [\n" );
        for ( int i = 0; i < values.size(); i++ ) {
            builder.append( indent )
                .append( "  \"" )
                .append( escape( values.get( i ) ) )
                .append( "\"" );
            if ( i < values.size() - 1 ) {
                builder.append( "," );
            }
            builder.append( "\n" );
        }
        builder.append( indent ).append( "]" );
        return builder.toString();
    }

    private static String escape(String value) {
        return value
            .replace( "\\", "\\\\" )
            .replace( "\"", "\\\"" );
    }

    private static final class TypeSurface {

        private String kind;
        private String superClass;
        private List<String> typeParameters = Collections.emptyList();
        private List<String> interfaces = Collections.emptyList();
        private List<String> methods = Collections.emptyList();
        private List<String> constructors = Collections.emptyList();
        private List<String> enumConstants = Collections.emptyList();
    }
}
