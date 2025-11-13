/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.api;

import org.mapstruct.ap.internal.langmodel.descriptor.ElementDescriptor;

/**
 * Descriptor for packages.
 */
public interface PackageDescriptor extends ElementDescriptor {

    String qualifiedName();
}
