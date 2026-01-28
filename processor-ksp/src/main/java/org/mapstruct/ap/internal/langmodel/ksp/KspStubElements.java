/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.internal.langmodel.ksp;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Stub implementation of {@link Elements} for KSP.
 * <p>
 * All methods throw {@link UnsupportedOperationException} as KSP does not use
 * the javax.lang.model API. This implementation exists only to satisfy the
 * {@link org.mapstruct.ap.spi.MapStructProcessingEnvironment} contract for SPIs
 * that may not require actual element utilities.
 */
final class KspStubElements implements Elements {

    private static final String NOT_SUPPORTED_MSG =
        "javax.lang.model Elements API is not supported in KSP. Use KSP's native API instead.";

    @Override
    public PackageElement getPackageElement(CharSequence name) {
        throw new UnsupportedOperationException( NOT_SUPPORTED_MSG );
    }

    @Override
    public PackageElement getPackageElement(ModuleElement module, CharSequence name) {
        throw new UnsupportedOperationException( NOT_SUPPORTED_MSG );
    }

    @Override
    public Set<? extends PackageElement> getAllPackageElements(CharSequence name) {
        throw new UnsupportedOperationException( NOT_SUPPORTED_MSG );
    }

    @Override
    public TypeElement getTypeElement(CharSequence name) {
        throw new UnsupportedOperationException( NOT_SUPPORTED_MSG );
    }

    @Override
    public TypeElement getTypeElement(ModuleElement module, CharSequence name) {
        throw new UnsupportedOperationException( NOT_SUPPORTED_MSG );
    }

    @Override
    public Set<? extends TypeElement> getAllTypeElements(CharSequence name) {
        throw new UnsupportedOperationException( NOT_SUPPORTED_MSG );
    }

    @Override
    public ModuleElement getModuleElement(CharSequence name) {
        throw new UnsupportedOperationException( NOT_SUPPORTED_MSG );
    }

    @Override
    public Set<? extends ModuleElement> getAllModuleElements() {
        throw new UnsupportedOperationException( NOT_SUPPORTED_MSG );
    }

    @Override
    public Map<? extends ExecutableElement, ? extends AnnotationValue> getElementValuesWithDefaults(
        AnnotationMirror a) {
        throw new UnsupportedOperationException( NOT_SUPPORTED_MSG );
    }

    @Override
    public String getDocComment(Element e) {
        throw new UnsupportedOperationException( NOT_SUPPORTED_MSG );
    }

    @Override
    public boolean isDeprecated(Element e) {
        throw new UnsupportedOperationException( NOT_SUPPORTED_MSG );
    }

    @Override
    public Name getBinaryName(TypeElement type) {
        throw new UnsupportedOperationException( NOT_SUPPORTED_MSG );
    }

    @Override
    public PackageElement getPackageOf(Element type) {
        throw new UnsupportedOperationException( NOT_SUPPORTED_MSG );
    }

    @Override
    public ModuleElement getModuleOf(Element type) {
        throw new UnsupportedOperationException( NOT_SUPPORTED_MSG );
    }

    @Override
    public List<? extends Element> getAllMembers(TypeElement type) {
        throw new UnsupportedOperationException( NOT_SUPPORTED_MSG );
    }

    @Override
    public List<? extends AnnotationMirror> getAllAnnotationMirrors(Element e) {
        throw new UnsupportedOperationException( NOT_SUPPORTED_MSG );
    }

    @Override
    public boolean hides(Element hider, Element hidden) {
        throw new UnsupportedOperationException( NOT_SUPPORTED_MSG );
    }

    @Override
    public boolean overrides(ExecutableElement overrider, ExecutableElement overridden, TypeElement type) {
        throw new UnsupportedOperationException( NOT_SUPPORTED_MSG );
    }

    @Override
    public String getConstantExpression(Object value) {
        throw new UnsupportedOperationException( NOT_SUPPORTED_MSG );
    }

    @Override
    public void printElements(Writer w, Element... elements) {
        throw new UnsupportedOperationException( NOT_SUPPORTED_MSG );
    }

    @Override
    public Name getName(CharSequence cs) {
        throw new UnsupportedOperationException( NOT_SUPPORTED_MSG );
    }

    @Override
    public boolean isFunctionalInterface(TypeElement type) {
        throw new UnsupportedOperationException( NOT_SUPPORTED_MSG );
    }

    @Override
    public boolean isBridge(ExecutableElement e) {
        throw new UnsupportedOperationException( NOT_SUPPORTED_MSG );
    }
}
