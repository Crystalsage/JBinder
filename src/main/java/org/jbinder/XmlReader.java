package org.jbinder;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

public class XmlReader {

    public List<StartElement> readXml(Supplier<FileInputStream> inputStreamSupplier) throws XMLStreamException {
        XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        var reader = xmlInputFactory.createXMLEventReader(inputStreamSupplier.get());

        return Stream.generate(() -> null)
                .takeWhile(x -> reader.hasNext())
                .map(x -> {
                    try {
                        return reader.nextEvent();
                    } catch (XMLStreamException e) {
                        throw new RuntimeException(e);
                    }
                })
                .filter(e -> e.getEventType() == XMLEvent.START_ELEMENT)
                .map(XMLEvent::asStartElement)
                .toList();
    }
}