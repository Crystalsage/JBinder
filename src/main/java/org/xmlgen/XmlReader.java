package org.xmlgen;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.function.Supplier;

public class XmlReader {
    public void read(Supplier<FileInputStream> inputStreamSupplier) {
        try {
            readXml(inputStreamSupplier);
        } catch (XMLStreamException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void readXml(Supplier<FileInputStream> inputStreamSupplier) throws XMLStreamException, IOException {
        var xmlReader = getReader(inputStreamSupplier);
        while(xmlReader.hasNext()) {
            var element = xmlReader.nextEvent();
            if (XMLEvent.START_ELEMENT == element.getEventType()) {
                parseStartElement(element.asStartElement());
            }
        }
    }

    private void parseStartElement(StartElement element) {
        if ("element".equals(element.getName().getLocalPart())) {
            var attributes = element.asStartElement().getAttributes();
            while (attributes.hasNext()) {
                var attribute = attributes.next();
                System.out.println(attribute.getName() + ":" + attribute.getValue());
            }
        }
    }

    public XMLEventReader getReader(Supplier<FileInputStream> inputStreamSupplier) throws XMLStreamException {
        XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        return xmlInputFactory.createXMLEventReader(inputStreamSupplier.get());
    }
}
