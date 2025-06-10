package org.jbinder.xsd;

import org.jbinder.graph.TypeDAG;
import org.jbinder.xsd.types.XsdStringType;

import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.util.*;

public class XsdParser {

    public static void parse(List<XMLEvent> elements) {
        Map<String, XsdType> types = new HashMap<>();
        TypeDAG typeDAG = new TypeDAG();

        var xmlEventsIterator = elements.iterator();

        while (xmlEventsIterator.hasNext()) {
            XMLEvent xmlEvent = xmlEventsIterator.next();
            if (xmlEvent.getEventType() == XMLEvent.START_ELEMENT) {
                handleStartElement(xmlEvent, types, xmlEventsIterator);
            }
        }

        // Refresh iterator
        xmlEventsIterator = elements.iterator();
        while (xmlEventsIterator.hasNext()) {
            XMLEvent xmlEvent = xmlEventsIterator.next();
            if (xmlEvent.getEventType() != XMLEvent.START_ELEMENT || !"complexType".equals(Util.tagName(xmlEvent))) {
                continue;
            }

            // Get current complex type
            var complexType = (ComplexType) types.get(Util.attributeValue(xmlEvent, "name").get());

            // Save root
            if (complexType.name().equals("Document")) {
                typeDAG.saveRoot(new XsdElement(complexType.name(), complexType));
            }

            List<XMLEvent> complexTypeBlock = capture(xmlEventsIterator, "complexType");
            for (XMLEvent targetElement : complexTypeBlock) {
                var source = new XsdElement(complexType.name(), complexType);
                var target = getTarget(targetElement, types);
                typeDAG.put(source, target);
            }
        }

        var sorted = typeDAG.sort();
        StringBuilder out = new StringBuilder();
        out.append(sorted.getFirst().name());
        for (XsdElement xsdElement : sorted.subList(1, sorted.size())) {
            out.append(" --> ").append(xsdElement.name());
        }
        System.out.println(out);
    }

    private static XsdElement getTarget(XMLEvent targetElement, Map<String, XsdType> types) {
        var dependeeType = types.get(Util.attributeValue(targetElement, "type").get());
        return switch (dependeeType) {
            case ComplexType c -> new XsdElement(c.name(), c);
            case SimpleType s -> new XsdElement(s.name(), s);
        };
    }

    private static List<XMLEvent> capture(Iterator<XMLEvent> xmlEventIterator, String name) {
        var block = new ArrayList<XMLEvent>(10);
        while (xmlEventIterator.hasNext()) {
            var nextEvent = xmlEventIterator.next();
            if (nextEvent.isEndElement() && nextEvent.asEndElement().getName().getLocalPart().equals(name)) {
                return block;
            }

            if (nextEvent.isStartElement() && "element".equals(Util.tagName(nextEvent))) {
                block.add(nextEvent);
            }
        }

        return block;
    }

    private static void handleStartElement(XMLEvent xmlEvent, Map<String, XsdType> xsdTypes, Iterator<XMLEvent> xmlEventsIterator) {
        var startElement = xmlEvent.asStartElement();
        switch (Util.tagName(startElement)) {
            case "complexType" -> handleComplexType(startElement).map(c -> xsdTypes.putIfAbsent(c.name(), c));
            case "simpleType" -> handleSimpleType(startElement, xmlEventsIterator).map(s -> xsdTypes.put(s.name(), s));
        }
    }

    private static Optional<SimpleType> handleSimpleType(StartElement startElement, Iterator<XMLEvent> xmlEventsIterator) {
        var simpleTypeName = Util.attributeValue(startElement, "name").orElse(null);
        if (simpleTypeName == null) {
            return Optional.empty();
        }

        while (xmlEventsIterator.hasNext()) {
            var nextEvent = getNext(xmlEventsIterator);

            if (isEndElement(nextEvent, "simpleType")) {
                break;
            }

            if (nextEvent.isStartElement() && startElementEquals(nextEvent.asStartElement(), "restriction")) {
                var baseType = Util.attributeValue(nextEvent.asStartElement(), "base").orElse("");
                return switch (baseType) {
                    case "xs:string" -> Optional.of(parseStringSimpleType(xmlEventsIterator, simpleTypeName));
                    default ->
                        throw new IllegalStateException("Unexpected value: " + Util.attributeValue(nextEvent.asStartElement(), "base"));
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
                var restriction = Util.attributeValue(nextElement.asStartElement(), "value").orElse("");
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
        var complexTypeName = Util.attributeValue(complexTypeElement, "name");
        return complexTypeName.map(ComplexType::new);
    }


    public record ParseResult(List<ComplexType> complexTypes, List<SimpleType> simpleTypes) {
    }
}
