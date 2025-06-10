package org.jbinder.xsd;

import javax.xml.namespace.QName;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.util.Optional;

public class Util {
    public static String tagName(XMLEvent xmlEvent) {
        if (xmlEvent.isStartElement()) {
            return xmlEvent.asStartElement().getName().getLocalPart();
        } else if (xmlEvent.isEndElement()) {
            return xmlEvent.asEndElement().getName().getLocalPart();
        }

        throw new IllegalArgumentException("WRONG TAGNAME");
    }

    public static Optional<String> attributeValue(XMLEvent element, String attributeName) {
        return Optional.ofNullable(element.asStartElement().getAttributeByName(new QName(attributeName)))
            .map(Attribute::getValue);
    }
}
