package org.jbinder.xsd;

public record XsdElement( String name, ComplexType complexType, SimpleType simpleType) {
    public XsdElement(String name, ComplexType complexType) {
        this(name, complexType, null);
    }
    public XsdElement(String name, SimpleType simpleType) {
        this(name, null, simpleType);
    }
}