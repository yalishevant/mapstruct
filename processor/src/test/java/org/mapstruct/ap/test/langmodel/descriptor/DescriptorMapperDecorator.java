/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.test.langmodel.descriptor;

public abstract class DescriptorMapperDecorator implements DecoratorDescriptorMapper {

    private final DecoratorDescriptorMapper delegate;

    protected DescriptorMapperDecorator(DecoratorDescriptorMapper delegate) {
        this.delegate = delegate;
    }

    @Override
    public PersonDto map(Person source) {
        PersonDto target = delegate.map( source );
        if ( target != null && target.getName() != null ) {
            target.setName( target.getName() + " (decorated)" );
        }
        return target;
    }
}
