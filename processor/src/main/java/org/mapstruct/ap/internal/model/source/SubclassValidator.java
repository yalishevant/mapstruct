/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.model.source;

import java.util.ArrayList;
import java.util.List;

import org.mapstruct.ap.internal.util.FormattingMessager;
import org.mapstruct.ap.internal.util.Message;
import org.mapstruct.ap.descriptor.AnnotationDescriptor;
import org.mapstruct.ap.descriptor.ExecutableDescriptor;
import org.mapstruct.ap.langmodel.api.LangTypes;
import org.mapstruct.ap.descriptor.TypeDescriptor;

/**
 * Handles the validation of multiple @SubclassMapping annotations on the same method.
 *
 * @author Ben Zegveld
 */
public class SubclassValidator {

    private final FormattingMessager messager;
    private final List<TypeDescriptor> handledSubclasses = new ArrayList<>();
    private final LangTypes langTypes;

    public SubclassValidator(FormattingMessager messager, LangTypes langTypes) {
        this.messager = messager;
        this.langTypes = langTypes;
    }

    public boolean isValidUsage(ExecutableDescriptor executable,
                                AnnotationDescriptor annotation,
                                TypeDescriptor sourceType) {
        for ( TypeDescriptor typeDescriptor : handledSubclasses ) {
            if ( langTypes.isSameType( sourceType, typeDescriptor ) ) {
                messager
                        .printMessage(
                            executable,
                            annotation,
                            Message.SUBCLASSMAPPING_DOUBLE_SOURCE_SUBCLASS,
                            sourceType.displayName() );
                return false;
            }
            if ( langTypes.isAssignable( sourceType, typeDescriptor ) ) {
                messager
                        .printMessage(
                            executable,
                            annotation,
                            Message.SUBCLASSMAPPING_ILLOGICAL_ORDER,
                            sourceType.displayName(),
                            typeDescriptor.displayName(),
                            sourceType.displayName(),
                            typeDescriptor.displayName() );
                return false;
            }
        }
        handledSubclasses.add( sourceType );
        return true;
    }

}
