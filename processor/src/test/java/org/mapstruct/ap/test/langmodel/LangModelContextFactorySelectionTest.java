/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.test.langmodel;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic.Kind;

import org.junit.jupiter.api.Test;
import org.mapstruct.ap.MappingProcessor;
import org.mapstruct.ap.internal.option.MappingOption;
import org.mapstruct.ap.internal.option.Options;
import org.mapstruct.ap.langmodel.LangModelContextFactory;
import org.mapstruct.ap.langmodel.javax.JavaxLangModelContextFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LangModelContextFactorySelectionTest {

    @Test
    void selectsDefaultBackendWhenNotConfigured() throws Exception {
        MappingProcessor processor = new MappingProcessor();
        setOptions( processor, new Options( Collections.emptyMap() ) );

        RecordingMessager messager = new RecordingMessager();
        LangModelContextFactory factory = invokeResolve( processor, new StubProcessingEnvironment( messager ) );

        assertThat( factory ).isInstanceOf( JavaxLangModelContextFactory.class );
        assertThat( messager.messages ).isEmpty();
    }

    @Test
    void selectsBackendByAliasIgnoringCase() throws Exception {
        MappingProcessor processor = new MappingProcessor();
        Map<String, String> optionValues = Map.of(
            MappingOption.LANG_MODEL_BACKEND.getOptionName(),
            "APT"
        );
        setOptions( processor, new Options( optionValues ) );

        RecordingMessager messager = new RecordingMessager();
        LangModelContextFactory factory = invokeResolve( processor, new StubProcessingEnvironment( messager ) );

        assertThat( factory ).isInstanceOf( JavaxLangModelContextFactory.class );
        assertThat( messager.messages ).isEmpty();
    }

    @Test
    void reportsErrorForUnknownBackend() throws Exception {
        MappingProcessor processor = new MappingProcessor();
        Map<String, String> optionValues = Map.of(
            MappingOption.LANG_MODEL_BACKEND.getOptionName(),
            "does-not-exist"
        );
        setOptions( processor, new Options( optionValues ) );

        RecordingMessager messager = new RecordingMessager();
        assertThatThrownBy( () -> invokeResolve( processor, new StubProcessingEnvironment( messager ) ) )
            .isInstanceOf( IllegalStateException.class )
            .hasMessageContaining( "does-not-exist" );

        assertThat( messager.messages )
            .hasSize( 1 )
            .first()
            .asString()
            .contains( "Unknown MapStruct lang model backend" );
        assertThat( messager.kinds )
            .containsExactly( Kind.ERROR );
    }

    private static void setOptions(MappingProcessor processor, Options options) throws Exception {
        Field optionsField = MappingProcessor.class.getDeclaredField( "options" );
        optionsField.setAccessible( true );
        optionsField.set( processor, options );
    }

    private static LangModelContextFactory invokeResolve(MappingProcessor processor,
                                                         ProcessingEnvironment processingEnvironment)
        throws Exception {

        Method method = MappingProcessor.class.getDeclaredMethod(
            "resolveLangModelContextFactory",
            ProcessingEnvironment.class
        );
        method.setAccessible( true );
        try {
            return (LangModelContextFactory) method.invoke( processor, processingEnvironment );
        }
        catch ( InvocationTargetException ex ) {
            Throwable cause = ex.getCause();
            if ( cause instanceof Exception ) {
                throw (Exception) cause;
            }
            if ( cause instanceof Error ) {
                throw (Error) cause;
            }
            throw ex;
        }
    }

    private static final class StubProcessingEnvironment implements ProcessingEnvironment {

        private final RecordingMessager messager;

        private StubProcessingEnvironment(RecordingMessager messager) {
            this.messager = messager;
        }

        @Override
        public Map<String, String> getOptions() {
            return Collections.emptyMap();
        }

        @Override
        public Messager getMessager() {
            return messager;
        }

        @Override
        public Filer getFiler() {
            throw new UnsupportedOperationException( "Not required for test" );
        }

        @Override
        public Elements getElementUtils() {
            throw new UnsupportedOperationException( "Not required for test" );
        }

        @Override
        public Types getTypeUtils() {
            throw new UnsupportedOperationException( "Not required for test" );
        }

        @Override
        public SourceVersion getSourceVersion() {
            return SourceVersion.latestSupported();
        }

        @Override
        public Locale getLocale() {
            return Locale.getDefault();
        }
    }

    private static final class RecordingMessager implements Messager {

        private final List<String> messages = new ArrayList<>();
        private final List<Kind> kinds = new ArrayList<>();

        @Override
        public void printMessage(Kind kind, CharSequence msg) {
            kinds.add( kind );
            messages.add( msg == null ? null : msg.toString() );
        }

        @Override
        public void printMessage(Kind kind, CharSequence msg, Element element) {
            printMessage( kind, msg );
        }

        @Override
        public void printMessage(Kind kind, CharSequence msg, Element element, AnnotationMirror annotation) {
            printMessage( kind, msg );
        }

        @Override
        public void printMessage(Kind kind, CharSequence msg, Element element, AnnotationMirror annotation,
                                 AnnotationValue value) {
            printMessage( kind, msg );
        }
    }
}
