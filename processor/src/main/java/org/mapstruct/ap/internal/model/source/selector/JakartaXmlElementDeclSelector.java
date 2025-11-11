/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.model.source.selector;

import org.mapstruct.ap.langmodel.api.LangElements;
import org.mapstruct.ap.langmodel.api.LangTypes;

/**
 * {@link XmlElementDeclSelector} variant for {@code jakarta.xml.bind.annotation} annotations.
 */
class JakartaXmlElementDeclSelector extends XmlElementDeclSelector {

    private static final String XML_ELEMENT_DECL = "jakarta.xml.bind.annotation.XmlElementDecl";
    private static final String XML_ELEMENT_REF = "jakarta.xml.bind.annotation.XmlElementRef";

    JakartaXmlElementDeclSelector(LangElements langElements, LangTypes langTypes) {
        super( langElements, langTypes );
    }

    @Override
    protected String xmlElementDeclAnnotation() {
        return XML_ELEMENT_DECL;
    }

    @Override
    protected String xmlElementRefAnnotation() {
        return XML_ELEMENT_REF;
    }
}
