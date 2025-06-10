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
            if (xmlEvent.getEventType() == XMLEvent.START_ELEMENT) {
                var element = xmlEvent.asStartElement();
                if ("complexType".equals(Util.tagName(element))) {
                    var complexType = (ComplexType) types.get(Util.attributeValue(element, "name").get());
                    // Special case
                    if (complexType.name().equals("Document")) {
                        typeDAG.saveRoot(new XsdElement(complexType.name(), complexType));
                    }

                    List<XMLEvent> complexTypeBlock = capture(xmlEventsIterator, "complexType");
                    for (XMLEvent event : complexTypeBlock) {
                        XsdElement target;
                        var isSequence = "sequence".equals(Util.tagName(complexTypeBlock.getFirst()));
                        if ("element".equals(Util.tagName(event))) {
                            var dependeeType = types.get(Util.attributeValue(event, "type").get());
                            var source = new XsdElement(complexType.name(), complexType);
                            target = switch (dependeeType) {
                                case ComplexType c -> new XsdElement(c.name(),  c);
                                case SimpleType s -> new XsdElement(s.name(), s);
                            };
                            typeDAG.put(source, target);
                        }
                    }
                }
            }
        }

        System.out.println(typeDAG.sort());
    }

    private static List<XMLEvent> capture(Iterator<XMLEvent> xmlEventIterator, String name) {
        var block = new ArrayList<XMLEvent>(10);
        while (xmlEventIterator.hasNext()) {
            var nextEvent = xmlEventIterator.next();
            if (nextEvent.isEndElement() && nextEvent.asEndElement().getName().getLocalPart().equals(name)) {
                block.add(nextEvent);
                return block;
            }

            if (nextEvent.isStartElement()) {
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
