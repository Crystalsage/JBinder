package org.jbinder.xsd;

import org.jbinder.graph.TypeDAG;
import org.jbinder.xsd.types.XsdStringType;

import javax.xml.namespace.QName;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class XsdParser {
    static TypeDAG typeDAG = new TypeDAG();

    public static void parse(List<XMLEvent> elements) {
        List<XsdElement> xsdElements = new ArrayList<>();

        var xmlEventsIterator = elements.iterator();
        XMLEvent xmlEvent;

        var complexTypes = new ArrayList<ComplexType>();
        var simpleTypes = new ArrayList<SimpleType>();

        while (xmlEventsIterator.hasNext()) {
            xmlEvent = xmlEventsIterator.next();
            if (xmlEvent.getEventType() == XMLEvent.START_ELEMENT) {
                handleStartElement(xmlEvent, complexTypes, simpleTypes, xmlEventsIterator);
            }
        }

    }

    private static void handleStartElement(XMLEvent xmlEvent, ArrayList<ComplexType> complexTypes, ArrayList<SimpleType> simpleTypes, Iterator<XMLEvent> xmlEventsIterator) {
        var startElement = xmlEvent.asStartElement();
        switch (startElement.getName().getLocalPart()) {
            case "complexType" -> complexTypes.add(handleComplexType(startElement));
            case "simpleType" -> simpleTypes.add(handleSimpleType(startElement, xmlEventsIterator));
        }
    }

    private static SimpleType handleSimpleType(StartElement startElement, Iterator<XMLEvent> xmlEventsIterator) {
        SimpleType simpleType;

        var simpleTypeName = getAttributeValue(startElement, "name");

        while (xmlEventsIterator.hasNext()) {
            var nextEvent = xmlEventsIterator.next();

            // Skip over character types, mostly whitespace
            if (nextEvent.isCharacters()) {
                continue;
            }

            if (nextEvent.isStartElement()) {
                if (nextEvent.asStartElement().getName().getLocalPart().equals("restriction")) {
                    switch (getAttributeValue(nextEvent.asStartElement(), "base")) {
                        case "xs:string" -> simpleType = getStringSimpleType(xmlEventsIterator, simpleTypeName);
                        default ->
                            throw new IllegalStateException("Unexpected value: " + getAttributeValue(nextEvent.asStartElement(), "base"));
                    }
                }
            }
        }

        return simpleType;
    }

    // TODO: Maybe these handlers should get entire tag chunks instead of iterator
    private static SimpleType getStringSimpleType(Iterator<XMLEvent> xmlEventsIterator, String simpleTypeName) {
        var xsdStringType = new XsdStringType();

        // Until we encounter the end of simpleType definition, collect the restrictions
        while (xmlEventsIterator.hasNext()) {
            var nextElement = getNext(xmlEventsIterator);

            if (nextElement.isEndElement() && "simpleType".equals(nextElement.asEndElement().getName().getLocalPart())) {
                break;
            }

            if (nextElement.isStartElement()) {
                var restriction = getAttributeValue(nextElement.asStartElement(), "value");
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

    private static ComplexType handleComplexType(StartElement complexTypeElement) {
        return new ComplexType(getAttributeValue(complexTypeElement, "name"));
    }

    private static String getAttributeValue(StartElement element, String attributeName) {
        return element.getAttributeByName(new QName(attributeName)).getValue();
    }

    private static boolean isEndComplexType(XMLEvent xmlEvent) {
        return xmlEvent.isEndElement() && "complexType".equals(xmlEvent.asEndElement().getName().getLocalPart());
    }
}
