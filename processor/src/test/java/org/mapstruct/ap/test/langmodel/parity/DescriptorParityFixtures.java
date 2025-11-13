/*
 * Copyright MapStruct Authors.
 *
 * Licensed under the Apache License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package org.mapstruct.ap.test.langmodel.parity;

import java.util.ArrayList;
import java.util.List;

class MapperStub {
}

class Box<T extends Number> {

    private T value;

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }
}

class WildcardHolder {

    private List<Number> source;
    private List<Integer> sink;
    private Number[] numberArray;

    public List<Number> getSource() {
        return source;
    }

    public void setSource(List<Number> source) {
        this.source = source;
    }

    public List<Integer> getSink() {
        return sink;
    }

    public void setSink(List<Integer> sink) {
        this.sink = sink;
    }

    public Number[] getNumberArray() {
        return numberArray;
    }

    public void setNumberArray(Number[] numberArray) {
        this.numberArray = numberArray;
    }
}

class OverrideBase {

    public Number value(Number input) {
        return input;
    }

    public CharSequence label() {
        return "base";
    }
}

class OverrideChild extends OverrideBase {

    @Override
    public Integer value(Number input) {
        return input == null ? null : input.intValue();
    }

    @Override
    public String label() {
        return "child";
    }

    public void childOnly() {
        // no-op
    }
}

class MultipleConstructors {

    public MultipleConstructors() {
    }

    public MultipleConstructors(String value) {
    }

    private MultipleConstructors(int hidden) {
    }
}

class AdderHolder {

    private final List<String> items = new ArrayList<>();

    public void addItem(String item) {
        items.add( item );
    }
}

enum SampleEnum {
    FIRST,
    SECOND
}

sealed interface Shape permits Circle, Rectangle {
}

final class Circle implements Shape {

    private final double radius;

    Circle(double radius) {
        this.radius = radius;
    }

    double radius() {
        return radius;
    }
}

final class Rectangle implements Shape {

    private final double width;
    private final double height;

    Rectangle(double width, double height) {
        this.width = width;
        this.height = height;
    }

    double width() {
        return width;
    }

    double height() {
        return height;
    }
}

class MethodHolder {

    @Deprecated
    public <R extends Comparable<R> & java.io.Serializable> R identity(R value) {
        return value;
    }
}
