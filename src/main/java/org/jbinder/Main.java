package org.jbinder;

import org.jbinder.xsd.XsdParser;

import java.io.FileInputStream;

public class Main {
    private static final String FILE_PATH = "mincamt.053.001.13.xsd";

    public static void main(String[] args) throws Exception {
        var xmlReader = new XmlReader();
        var elements = xmlReader.readXml(new FileInputStream(FILE_PATH));
        System.out.println(XsdParser.parse(elements));
    }
}