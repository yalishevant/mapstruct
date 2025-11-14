/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.mapstruct.ap.internal.langmodel.descriptor.ElementDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.ExecutableDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.FieldDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.RecordComponentDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeElementDescriptor;

/**
 * Provides higher-level type inspection utilities on top of {@link org.mapstruct.ap.internal.langmodel.api.LangTypes}
 * and {@link org.mapstruct.ap.internal.langmodel.api.LangElements}.
 * <p>
 * The goal of this contract is to supply the MapStruct processor core with language-neutral metadata about a type
 * without exposing backend-specific compiler models.
 */
public interface TypeIntrospector {

    /**
     * Describe the supplied type.
     *
     * @param descriptor descriptor of the type to be introspected (must not be {@code null})
     *
     * @return immutable snapshot of commonly requested metadata
     */
    Metadata describe(TypeDescriptor descriptor);

    /**
     * Resolve an executable in the context of a containing type.
     *
     * @param containingType type that provides the resolution scope
     * @param executable     executable descriptor to resolve
     *
     * @return resolved signature or {@code null} if it cannot be resolved
     */
    ExecutableSignature resolveExecutable(TypeDescriptor containingType, ExecutableDescriptor executable);

    /**
     * Obtain the executable signature for the supplied descriptor without any resolution against containing types.
     *
     * @param executable executable descriptor
     *
     * @return signature or {@code null} if not applicable
     */
    ExecutableSignature executable(ExecutableDescriptor executable);

    /**
     * Computes the type of {@code member} as seen from the {@code containingType}.
     *
     * @param containingType context type
     * @param member         member descriptor (method, field, record component)
     *
     * @return resolved descriptor or {@code null} if the operation is not supported for the supplied member
     */
    TypeDescriptor asMemberOf(TypeDescriptor containingType, ElementDescriptor member);

    /**
     * Retrieve executables visible on the supplied descriptor.
     *
     * @param descriptor descriptor of the type to inspect
     *
     * @return immutable list of executables (empty when none)
     */
    List<ExecutableDescriptor> enclosedExecutables(TypeDescriptor descriptor);

    /**
     * Retrieve constructors declared directly on the supplied descriptor.
     *
     * @param descriptor descriptor representing the type element
     *
     * @return immutable list of constructors (empty when none or descriptor is not a declared type)
     */
    List<ExecutableDescriptor> constructors(TypeDescriptor descriptor);

    /**
     * Retrieve fields visible on the supplied descriptor.
     *
     * @param descriptor descriptor of the type to inspect
     *
     * @return immutable list of fields (empty when none)
     */
    List<FieldDescriptor> enclosedFields(TypeDescriptor descriptor);

    /**
     * Retrieve record components declared directly on the supplied descriptor.
     *
     * @param descriptor descriptor of the type to inspect
     *
     * @return immutable list of record components (empty when the type is not a record)
     */
    List<RecordComponentDescriptor> recordComponents(TypeDescriptor descriptor);

    /**
     * Determine whether {@code overrider} overrides {@code overridden} in the scope of {@code type}.
     */
    boolean overrides(ExecutableDescriptor overrider, ExecutableDescriptor overridden, TypeDescriptor type);

    /**
     * Snapshot of metadata describing a type.
     */
    final class Metadata {

        private final TypeDescriptor descriptor;
        private final TypeElementDescriptor typeElement;
        private final TypeDescriptor componentType;
        private final TypeDescriptor baseComponentType;
        private final Names names;
        private final TypeFlags typeFlags;
        private final List<TypeDescriptor> permittedSubclasses;
        private final List<String> enumConstants;
        private final TypeDescriptor topLevelType;

        public Metadata(TypeDescriptor descriptor,
                        TypeElementDescriptor typeElement,
                        TypeDescriptor componentType,
                        TypeDescriptor baseComponentType,
                        Names names,
                        TypeFlags typeFlags,
                        List<TypeDescriptor> permittedSubclasses,
                        List<String> enumConstants,
                        TypeDescriptor topLevelType) {
            this.descriptor = Objects.requireNonNull( descriptor, "descriptor" );
            this.typeElement = typeElement;
            this.componentType = componentType;
            this.baseComponentType = baseComponentType;
            this.names = names == null ? Names.empty() : names;
            this.typeFlags = typeFlags == null ? TypeFlags.empty() : typeFlags;
            this.permittedSubclasses = permittedSubclasses == null
                ? Collections.emptyList()
                : Collections.unmodifiableList( permittedSubclasses );
            this.enumConstants = enumConstants == null
                ? Collections.emptyList()
                : Collections.unmodifiableList( enumConstants );
            this.topLevelType = topLevelType;
        }

        public TypeDescriptor descriptor() {
            return descriptor;
        }

        public Optional<TypeElementDescriptor> typeElement() {
            return Optional.ofNullable( typeElement );
        }

        public Optional<TypeDescriptor> componentType() {
            return Optional.ofNullable( componentType );
        }

        public Optional<TypeDescriptor> baseComponentType() {
            return Optional.ofNullable( baseComponentType );
        }

        public Optional<String> simpleName() {
            return Optional.ofNullable( names.simpleName );
        }

        public Optional<String> qualifiedName() {
            return Optional.ofNullable( names.qualifiedName );
        }

        public Optional<String> packageName() {
            return Optional.ofNullable( names.packageName );
        }

        public boolean isEnumType() {
            return typeFlags.enumType;
        }

        public boolean isInterfaceType() {
            return typeFlags.interfaceType;
        }

        public boolean isRecordType() {
            return typeFlags.recordType;
        }

        public boolean isSealedType() {
            return typeFlags.sealedType;
        }

        public List<TypeDescriptor> permittedSubclasses() {
            return permittedSubclasses;
        }

        public List<String> enumConstants() {
            return enumConstants;
        }

        public Optional<TypeDescriptor> topLevelType() {
            return Optional.ofNullable( topLevelType );
        }

        /**
         * Container for optional naming details of a type.
         */
        public static final class Names {

            private final String simpleName;
            private final String qualifiedName;
            private final String packageName;

            private Names(String simpleName, String qualifiedName, String packageName) {
                this.simpleName = simpleName;
                this.qualifiedName = qualifiedName;
                this.packageName = packageName;
            }

            public static Names of(String simpleName, String qualifiedName, String packageName) {
                return new Names( simpleName, qualifiedName, packageName );
            }

            private static Names empty() {
                return new Names( null, null, null );
            }
        }

        /**
         * Container for boolean flags describing specific type characteristics.
         */
        public static final class TypeFlags {

            private final boolean enumType;
            private final boolean interfaceType;
            private final boolean recordType;
            private final boolean sealedType;

            private TypeFlags(boolean enumType, boolean interfaceType, boolean recordType, boolean sealedType) {
                this.enumType = enumType;
                this.interfaceType = interfaceType;
                this.recordType = recordType;
                this.sealedType = sealedType;
            }

            public static TypeFlags of(boolean enumType,
                                       boolean interfaceType,
                                       boolean recordType,
                                       boolean sealedType) {
                return new TypeFlags( enumType, interfaceType, recordType, sealedType );
            }

            private static TypeFlags empty() {
                return new TypeFlags( false, false, false, false );
            }
        }
    }
}
