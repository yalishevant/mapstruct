/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.javax;

import javax.lang.model.element.PackageElement;

import org.mapstruct.ap.internal.langmodel.api.PackageDescriptor;

final class JavaxPackageDescriptor extends JavaxElementDescriptor implements PackageDescriptor {

    private final PackageElement packageElement;

    JavaxPackageDescriptor(JavaxLangModelContext context,
                           JavaxDescriptorFactory factory,
                           PackageElement packageElement) {
        super( context, factory, packageElement );
        this.packageElement = packageElement;
    }

    @Override
    public String qualifiedName() {
        return packageElement.getQualifiedName().toString();
    }
}
