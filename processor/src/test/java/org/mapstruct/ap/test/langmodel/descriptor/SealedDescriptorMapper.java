/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.test.langmodel.descriptor;

import org.mapstruct.Mapper;
import org.mapstruct.SubclassMapping;

@Mapper
public interface SealedDescriptorMapper {

    @SubclassMapping( source = Circle.class, target = CircleDto.class )
    @SubclassMapping( source = Rectangle.class, target = RectangleDto.class )
    ShapeDto map(Shape shape);
}
