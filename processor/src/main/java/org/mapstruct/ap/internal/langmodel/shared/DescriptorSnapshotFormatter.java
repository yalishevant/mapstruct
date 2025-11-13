/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.shared;

import java.util.Map;
import java.util.StringJoiner;

/**
 * Renders {@link DescriptorSnapshot} structures as deterministic text blocks suitable for golden tests.
 */
public final class DescriptorSnapshotFormatter {

    private DescriptorSnapshotFormatter() {
    }

    public static String format(DescriptorSnapshot snapshot) {
        StringBuilder builder = new StringBuilder();
        snapshot.types().values().forEach( type -> {
            builder.append( "TYPE " ).append( type.qualifiedName() ).append( System.lineSeparator() );
            appendLine( builder, "id", type.id() );
            appendLine( builder, "kind", type.kind() );
            appendLine( builder, "modifiers", type.modifiers() );
            appendLine( builder, "record", type.recordType() );
            appendLine( builder, "sealed", type.sealedType() );
            appendLine( builder, "permittedSubclasses", type.permittedSubclasses() );
            appendAnnotations( builder, "annotations", type.annotations() );
            appendFields( builder, type.fields() );
            appendRecordComponents( builder, type.recordComponents() );
            appendExecutables( builder, "constructors", type.constructors() );
            appendExecutables( builder, "methods", type.methods() );
            builder.append( System.lineSeparator() );
        } );
        return builder.toString().trim();
    }

    private static void appendLine(StringBuilder builder, String label, Object value) {
        builder.append( "  " ).append( label ).append( ": " ).append( value ).append( System.lineSeparator() );
    }

    private static void appendLine(StringBuilder builder, String label, Iterable<?> values) {
        StringJoiner joiner = new StringJoiner( ", ", "[", "]" );
        for ( Object value : values ) {
            joiner.add( String.valueOf( value ) );
        }
        builder.append( "  " ).append( label ).append( ": " ).append( joiner ).append( System.lineSeparator() );
    }

    private static void appendAnnotations(StringBuilder builder, String label, Iterable<AnnotationSnapshot> annotations) {
        builder.append( "  " ).append( label ).append( ":" ).append( System.lineSeparator() );
        for ( AnnotationSnapshot annotation : annotations ) {
            builder.append( "    - " ).append( renderAnnotation( annotation ) ).append( System.lineSeparator() );
        }
    }

    private static void appendFields(StringBuilder builder, Iterable<FieldSnapshot> fields) {
        builder.append( "  fields:" ).append( System.lineSeparator() );
        for ( FieldSnapshot field : fields ) {
            builder.append( "    - " )
                .append( field.name() )
                .append( " : " )
                .append( field.type() )
                .append( " (" )
                .append( field.kind() )
                .append( ")" )
                .append( " modifiers=" )
                .append( field.modifiers() )
                .append( System.lineSeparator() );
            appendAnnotations( builder, "      annotations", field.annotations() );
        }
    }

    private static void appendRecordComponents(StringBuilder builder, Iterable<RecordComponentSnapshot> components) {
        builder.append( "  recordComponents:" ).append( System.lineSeparator() );
        for ( RecordComponentSnapshot component : components ) {
            builder.append( "    - " )
                .append( component.name() )
                .append( " : " )
                .append( component.type() )
                .append( System.lineSeparator() );
            appendAnnotations( builder, "      annotations", component.annotations() );
        }
    }

    private static void appendExecutables(StringBuilder builder, String label, Iterable<ExecutableSnapshot> executables) {
        builder.append( "  " ).append( label ).append( ":" ).append( System.lineSeparator() );
        for ( ExecutableSnapshot executable : executables ) {
            builder.append( "    - " ).append( executable.name() ).append( "(" );
            StringJoiner parametersJoiner = new StringJoiner( ", " );
            for ( ParameterSnapshot parameter : executable.parameters() ) {
                StringBuilder parameterBuilder = new StringBuilder();
                parameterBuilder.append( parameter.type() );
                if ( parameter.varArgs() ) {
                    parameterBuilder.append( " (varargs)" );
                }
                if ( !parameter.annotations().isEmpty() ) {
                    parameterBuilder.append( " annotations=" )
                        .append( formatAnnotationsInline( parameter.annotations() ) );
                }
                if ( !parameter.modifiers().isEmpty() ) {
                    parameterBuilder.append( " modifiers=" ).append( parameter.modifiers() );
                }
                parametersJoiner.add( parameterBuilder.toString() );
            }
            builder.append( parametersJoiner.toString() ).append( ")" );
            if ( executable.returnType() != null ) {
                builder.append( " -> " ).append( executable.returnType() );
            }
            builder.append( " modifiers=" ).append( executable.modifiers() );
            if ( executable.defaultMethod() ) {
                builder.append( " [default]" );
            }
            if ( !executable.typeParameters().isEmpty() ) {
                builder.append( " typeParameters=" ).append( executable.typeParameters() );
            }
            if ( !executable.thrownTypes().isEmpty() ) {
                builder.append( " throws=" ).append( executable.thrownTypes() );
            }
            builder.append( System.lineSeparator() );
            appendAnnotations( builder, "      annotations", executable.annotations() );
        }
    }

    private static String renderAnnotation(AnnotationSnapshot annotation) {
        StringBuilder builder = new StringBuilder( "@" ).append( annotation.type() );
        if ( !annotation.values().isEmpty() ) {
            builder.append( annotation.values() );
        }
        return builder.toString();
    }

    private static String formatAnnotationsInline(Iterable<AnnotationSnapshot> annotations) {
        StringJoiner joiner = new StringJoiner( ", ", "[", "]" );
        for ( AnnotationSnapshot annotation : annotations ) {
            joiner.add( renderAnnotation( annotation ) );
        }
        return joiner.toString();
    }
}
