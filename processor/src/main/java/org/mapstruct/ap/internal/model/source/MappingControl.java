/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.model.source;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.mapstruct.ap.internal.gem.MappingControlUseGem;
import org.mapstruct.ap.internal.util.AnnotationDescriptorUtils;
import org.mapstruct.ap.internal.util.AnnotationValueUtils;
import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.AnnotationValueDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.LangElementKind;
import org.mapstruct.ap.internal.langmodel.api.LangElements;
import org.mapstruct.ap.internal.langmodel.descriptor.LangTypeKind;
import org.mapstruct.ap.internal.langmodel.api.PackageDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeElementDescriptor;

public class MappingControl {

    private static final String JAVA_LANG_ANNOTATION_PGK = "java.lang.annotation";
    private static final String ORG_MAPSTRUCT_PKG = "org.mapstruct";
    private static final String MAPPING_CONTROL_FQN = "org.mapstruct.control.MappingControl";
    private static final String MAPPING_CONTROLS_FQN = "org.mapstruct.control.MappingControls";

    private boolean allowDirect;
    private boolean allowTypeConversion;
    private boolean allowMappingMethod;
    private boolean allow2Steps;

    public static MappingControl fromTypeDescriptor(TypeDescriptor descriptor, LangElements langElements) {
        if ( descriptor == null || langElements == null ) {
            return enabledByDefault();
        }

        if ( descriptor.kind() != LangTypeKind.DECLARED ) {
            return enabledByDefault();
        }

        TypeElementDescriptor annotationType = descriptor.typeElement().orElse( null );
        if ( annotationType == null ) {
            return enabledByDefault();
        }

        MappingControl mappingControl = disabled();
        resolveControls( mappingControl, annotationType, new HashSet<>(), langElements );
        return mappingControl;
    }

    private MappingControl(boolean allowDirect, boolean allowTypeConversion, boolean allowMappingMethod,
                           boolean allow2Steps) {
        this.allowDirect = allowDirect;
        this.allowTypeConversion = allowTypeConversion;
        this.allowMappingMethod = allowMappingMethod;
        this.allow2Steps = allow2Steps;
    }

    private static MappingControl enabledByDefault() {
        return new MappingControl( true, true, true, true );
    }

    private static MappingControl disabled() {
        return new MappingControl( false, false, false, false );
    }

    public boolean allowDirect() {
        return allowDirect;
    }

    public boolean allowTypeConversion() {
        return allowTypeConversion;
    }

    public boolean allowMappingMethod() {
        return allowMappingMethod;
    }

    public boolean allowBy2Steps() {
        return allow2Steps;
    }

    private static void resolveControls(MappingControl control,
                                        TypeElementDescriptor element,
                                        Set<String> handledElements,
                                        LangElements langElements) {
        List<AnnotationDescriptor> annotations = langElements.annotationMirrors( element );
        for ( AnnotationDescriptor annotation : annotations ) {
            TypeElementDescriptor annotationType = annotation.annotationType();
            if ( annotationType == null ) {
                continue;
            }

            String qualifiedName = annotationType.qualifiedName();
            if ( Objects.equals( qualifiedName, MAPPING_CONTROL_FQN ) ) {
                applyMappingControl( control, annotation );
            }
            else if ( Objects.equals( qualifiedName, MAPPING_CONTROLS_FQN ) ) {
                for ( AnnotationDescriptor nested :
                    AnnotationDescriptorUtils.getAnnotationList( annotation, "value" ) ) {
                    applyMappingControl( control, nested );
                }
            }
            else if ( shouldRecurse( annotationType, handledElements, langElements ) ) {
                handledElements.add( annotationType.id() );
                resolveControls( control, annotationType, handledElements, langElements );
            }
        }
    }

    private static boolean shouldRecurse(TypeElementDescriptor annotationType,
                                         Set<String> handledElements,
                                         LangElements langElements) {
        if ( annotationType.kind() != LangElementKind.ANNOTATION_TYPE ) {
            return false;
        }
        if ( handledElements.contains( annotationType.id() ) ) {
            return false;
        }
        PackageDescriptor pkg = langElements.packageOf( annotationType );
        String packageName = pkg != null ? pkg.qualifiedName() : "";
        if ( Objects.equals( packageName, JAVA_LANG_ANNOTATION_PGK ) ) {
            return false;
        }
        if ( Objects.equals( packageName, ORG_MAPSTRUCT_PKG ) ) {
            return false;
        }
        return true;
    }

    private static void applyMappingControl(MappingControl control, AnnotationDescriptor annotation) {
        AnnotationValueDescriptor valueDescriptor = AnnotationDescriptorUtils.getValue( annotation, "value" );
        String enumConstant = enumConstantName( valueDescriptor );
        if ( enumConstant == null ) {
            return;
        }

        MappingControlUseGem use = MappingControlUseGem.valueOf( enumConstant );
        switch ( use ) {
            case DIRECT:
                control.allowDirect = true;
                break;
            case MAPPING_METHOD:
                control.allowMappingMethod = true;
                break;
            case BUILT_IN_CONVERSION:
                control.allowTypeConversion = true;
                break;
            case COMPLEX_MAPPING:
                control.allow2Steps = true;
                break;
            default:
        }
    }

    private static String enumConstantName(AnnotationValueDescriptor descriptor) {
        if ( descriptor == null ) {
            return null;
        }
        String representation = AnnotationValueUtils.asString( descriptor );
        if ( representation == null ) {
            return null;
        }
        int lastDot = representation.lastIndexOf( '.' );
        if ( lastDot >= 0 && lastDot + 1 < representation.length() ) {
            return representation.substring( lastDot + 1 );
        }
        return representation;
    }

}
