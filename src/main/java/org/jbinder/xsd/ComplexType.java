package org.jbinder.xsd;

public record ComplexType(String name) implements XsdType {
    @Override
    public String toString() {
        return name;
    }
}
