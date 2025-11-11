/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.test.langmodel.contract;

import java.util.List;
import java.util.stream.Stream;

/**
 * Test fixture types used by {@link LangModelContractProcessor} to validate the descriptor contract.
 */
public class LangModelContractTypes {

    public static class MapperStub {
    }

    public static class Box<T extends Number & Comparable<T>> {

        private T value;

        public T getValue() {
            return value;
        }
    }

    public static class WildcardHolder {

        List<? extends Number> source;
        List<? super Integer> sink;
        Number[] numberArray;
    }

    public static class OverrideBase {

        public Number value(Number input) {
            return input;
        }

        public CharSequence label() {
            return "base";
        }
    }

    public static class OverrideChild extends OverrideBase {

        @Override
        public Integer value(Number input) {
            return input == null ? null : input.intValue();
        }

        @Override
        public String label() {
            return "child";
        }

        public void childOnly() {
            // intentional no-op
        }
    }

    public static class MultipleConstructors {

        public MultipleConstructors() {
        }

        public MultipleConstructors(String value) {
        }

        private MultipleConstructors(int hidden) {
        }
    }

    public static class AdderHolder {

        private List<String> items;

        public void addItem(String item) {
            if ( items == null ) {
                items = new java.util.ArrayList<>();
            }
            items.add( item );
        }
    }

    public enum SampleEnum {
        FIRST,
        SECOND
    }

    public sealed interface Shape permits Circle, Rectangle {
    }

    public static final class Circle implements Shape {

        private final double radius;

        public Circle(double radius) {
            this.radius = radius;
        }

        public double radius() {
            return radius;
        }
    }

    public static final class Rectangle implements Shape {

        private final double width;
        private final double height;

        public Rectangle(double width, double height) {
            this.width = width;
            this.height = height;
        }

        public double width() {
            return width;
        }

        public double height() {
            return height;
        }
    }

    public record RecordType(String name, int age) {
    }

    public interface StreamSource {

        Stream<String> values();
    }

    public static class MethodHolder {

        public <R extends Comparable<R> & java.io.Serializable> R identity(R value) {
            return value;
        }
    }
}
