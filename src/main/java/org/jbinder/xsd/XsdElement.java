package org.jbinder.xsd;

public record XsdElement(
    String name,
    ComplexType complexType,
    SimpleType simpleType
) {}
