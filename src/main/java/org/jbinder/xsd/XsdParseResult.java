package org.jbinder.xsd;

import java.util.List;

public record XsdParseResult(
    List<ComplexType> complexTypes,
    List<SimpleType> simpleTypes
) { }
