/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.model.common;

import java.util.Collection;
import java.util.Collections;

import org.mapstruct.ap.internal.langmodel.descriptor.BuilderDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.ExecutableDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.LangElementKind;
import org.mapstruct.ap.internal.langmodel.api.LangTypes;
import org.mapstruct.ap.internal.langmodel.descriptor.ElementDescriptor;
import org.mapstruct.ap.internal.langmodel.descriptor.TypeDescriptor;

/**
 * @author Filip Hrisafov
 */
public class BuilderType {

    private final Type builder;
    private final Type owningType;
    private final Type buildingType;
    private final ExecutableDescriptor builderCreationMethod;
    private final Collection<ExecutableDescriptor> buildMethods;

    private BuilderType(
        Type builder,
        Type owningType,
        Type buildingType,
        ExecutableDescriptor builderCreationMethod,
        Collection<ExecutableDescriptor> buildMethods
    ) {
        this.builder = builder;
        this.owningType = owningType;
        this.buildingType = buildingType;
        this.builderCreationMethod = builderCreationMethod;
        this.buildMethods = Collections.unmodifiableCollection( buildMethods );
    }

    /**
     * The type of the builder itself.
     *
     * @return the type for the builder
     */
    public Type getBuilder() {
        return builder;
    }

    /**
     * The owning type of the builder, this can be the builder itself, the type that is build by the builder or some
     * other type.
     *
     * @return the owning type
     */
    public Type getOwningType() {
        return owningType;
    }

    /**
     * The type that is being built by the builder.
     *
     * @return the type that is being built
     */
    public Type getBuildingType() {
        return buildingType;
    }

    /**
     * The creation method for the builder.
     *
     * @return the creation method for the builder
     */
    public ExecutableDescriptor getBuilderCreationMethod() {
        return builderCreationMethod;
    }

    /**
     * The build methods that can be invoked to create the type being built.
     * @return the build methods that can be invoked to create the type being built
     */
    public Collection<ExecutableDescriptor> getBuildMethods() {
        return buildMethods;
    }

    public static BuilderType create(BuilderDescriptor builderDescriptor,
                                     Type typeToBuild,
                                     TypeFactory typeFactory,
                                     LangTypes langTypes) {
        if ( builderDescriptor == null || builderDescriptor.creationMethod() == null ) {
            return null;
        }

        ExecutableDescriptor creationMethod = builderDescriptor.creationMethod();
        TypeDescriptor creationOwnerDescriptor = creationMethod.enclosingElement()
            .map( ElementDescriptor::asType )
            .orElse( null );

        Type owner = creationOwnerDescriptor != null ? typeFactory.getType( creationOwnerDescriptor ) : null;

        Type builder = null;
        if ( creationMethod.kind() != LangElementKind.CONSTRUCTOR ) {
            builder = typeFactory.getType( creationMethod.returnType() );
        }

        if ( typeToBuild != null && creationOwnerDescriptor != null
            && langTypes.isSameType( creationOwnerDescriptor, typeToBuild.getTypeDescriptor() ) ) {
            owner = typeToBuild;
        }
        else if ( builder != null && creationOwnerDescriptor != null
            && langTypes.isSameType( creationOwnerDescriptor, builder.getTypeDescriptor() ) ) {
            owner = builder;
        }

        if ( creationMethod.kind() == LangElementKind.CONSTRUCTOR ) {
            builder = owner;
        }

        return new BuilderType(
            builder,
            owner,
            typeToBuild,
            creationMethod,
            builderDescriptor.buildMethods()
        );
    }
}
