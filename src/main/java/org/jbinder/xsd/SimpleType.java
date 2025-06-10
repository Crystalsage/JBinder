package org.jbinder.xsd;

import org.jbinder.xsd.types.XsdTypeInfo;

public record SimpleType(String name, XsdTypeInfo typeInfo) implements XsdType {
    @Override
    public String toString() {
        return name;
    }
}
