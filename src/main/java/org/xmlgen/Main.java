package org.xmlgen;

import javax.xml.stream.XMLStreamException;
import java.io.*;

public class Main {
    private static final String FILE_PATH = "camt.053.001.13.xsd";

    public static void main(String[] args) throws IOException {
        File file = new File(FILE_PATH);

        XmlReader xmlReader = new XmlReader();
        try {
            var inputStream = new FileInputStream(file);
            xmlReader.readXml(() -> inputStream);
        } catch (XMLStreamException | IOException e) {
            throw new RuntimeException(e);
        }
    }
}