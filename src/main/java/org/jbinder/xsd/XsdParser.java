package org.jbinder.xsd;

import org.jbinder.xsd.types.XsdStringType;

import javax.xml.namespace.QName;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

public class XsdParser {

    public static ParseResult parse(List<XMLEvent> elements) {
        List<ComplexType> complexTypes = new ArrayList<>();
        List<SimpleType> simpleTypes = new ArrayList<>();

        var xmlEventsIterator = elements.iterator();

        while (xmlEventsIterator.hasNext()) {
            XMLEvent xmlEvent = xmlEventsIterator.next();
            if (xmlEvent.getEventType() == XMLEvent.START_ELEMENT) {
                handleStartElement(xmlEvent, complexTypes, simpleTypes, xmlEventsIterator);
            }
        }

        return new ParseResult(complexTypes, simpleTypes);
    }

    private static void handleStartElement(XMLEvent xmlEvent, List<ComplexType> complexTypes, List<SimpleType> simpleTypes, Iterator<XMLEvent> xmlEventsIterator) {
        var startElement = xmlEvent.asStartElement();
        switch (startElement.getName().getLocalPart()) {
            case "complexType" -> handleComplexType(startElement).map(complexTypes::add);
            case "simpleType" -> handleSimpleType(startElement, xmlEventsIterator).map(simpleTypes::add);
        }
    }

    private static Optional<SimpleType> handleSimpleType(StartElement startElement, Iterator<XMLEvent> xmlEventsIterator) {
        var simpleTypeName = getAttributeValue(startElement, "name").orElse(null);
        if (simpleTypeName == null) {
            return Optional.empty();
        }

        while (xmlEventsIterator.hasNext()) {
            var nextEvent = getNext(xmlEventsIterator);

            if (isEndElement(nextEvent, "simpleType")) {
                break;
            }

            if (nextEvent.isStartElement() && startElementEquals(nextEvent.asStartElement(), "restriction")) {
                var baseType = getAttributeValue(nextEvent.asStartElement(), "base").orElse("");
                return switch (baseType) {
                    case "xs:string" -> Optional.of(parseStringSimpleType(xmlEventsIterator, simpleTypeName));
                    default -> throw new IllegalStateException("Unexpected value: " + getAttributeValue(nextEvent.asStartElement(), "base"));
                };
            }
        }

        return Optional.empty();
    }

    private static boolean startElementEquals(StartElement startElement, String expectedName) {
        return startElement.getName().getLocalPart().equals(expectedName);
    }

    private static boolean isEndElement(XMLEvent nextEvent, String elementName) {
        return nextEvent.isEndElement() &&
            nextEvent.asEndElement().getName().getLocalPart().equals(elementName);
    }

    // TODO: Maybe these handlers should get entire tag chunks instead of iterator
    private static SimpleType parseStringSimpleType(Iterator<XMLEvent> xmlEventsIterator, String simpleTypeName) {
        var xsdStringType = new XsdStringType();

        // Until we encounter the end of simpleType definition, collect the restrictions
        while (xmlEventsIterator.hasNext()) {
            var nextElement = getNext(xmlEventsIterator);

            if (isEndElement(nextElement, "simpleType")) {
                break;
            }

            // Process restrictions on string
            if (nextElement.isStartElement()) {
                var restriction = getAttributeValue(nextElement.asStartElement(), "value").orElse("");
                switch (nextElement.asStartElement().getName().getLocalPart()) {
                    case "pattern" -> xsdStringType.setPattern(restriction);
                    case "maxLength" -> xsdStringType.setMaxLength(Integer.parseInt(restriction));
                    case "minLength" -> xsdStringType.setMinLength(Integer.parseInt(restriction));
                }
            }
        }
        return new SimpleType(simpleTypeName, xsdStringType);
    }

    private static XMLEvent getNext(Iterator<XMLEvent> xmlEventIterator) {
        var nextEvent = xmlEventIterator.next();
        // Skip over empty characters
        while (nextEvent.getEventType() == XMLEvent.CHARACTERS) {
            nextEvent = xmlEventIterator.next();
        }
        return nextEvent;
    }

    private static Optional<ComplexType> handleComplexType(StartElement complexTypeElement) {
        var complexTypeName = getAttributeValue(complexTypeElement, "name");
        return complexTypeName.map(ComplexType::new);
    }

    private static Optional<String> getAttributeValue(StartElement element, String attributeName) {
        return Optional.ofNullable(element.getAttributeByName(new QName(attributeName)))
            .map(Attribute::getValue);
    }

    public record ParseResult(List<ComplexType> complexTypes, List<SimpleType> simpleTypes) {
    }
}
