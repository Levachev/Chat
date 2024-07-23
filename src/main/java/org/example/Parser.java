package org.example;

import org.example.Client.Client;
import org.example.Client.ClientException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.swing.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;

public class Parser {
    public Parser(){}

    public ArrayList<String> getValueByName(Document XMLDocument, String name, String innerElement){
        ArrayList<String> tmp = new ArrayList<>();
        NodeList elements = XMLDocument.getDocumentElement().getElementsByTagName(name);
        if(innerElement!=null) {
            for (int i = 0; i < elements.getLength(); i++) {
                Element item = (Element) elements.item(i);
                try {
                    tmp.add(item.getElementsByTagName(innerElement).item(0).getTextContent());
                } catch (NullPointerException e){
                    return null;
                }
            }
        } else{
            try {
                tmp.add(elements.item(0).getTextContent());
            } catch (NullPointerException e){
                return null;
            }
        }
        return tmp;
    }

    public String getRootName(Document document){
        return document.getDocumentElement().getNodeName();
    }

    public String getAttributeOfRoot(Document document, String attrName){
        return document.getDocumentElement().getAttributes().getNamedItem(attrName).getNodeValue();
    }

    public String getAttributeOfNode(Document document, String nodeName, String attrName){
        try {
            return document.getElementsByTagName(nodeName).item(0).getAttributes().getNamedItem(attrName).getNodeValue();
        } catch (NullPointerException e){
            return null;
        }
    }

    public String getRootChildName(Document document){
        try {
            Node child = document.getDocumentElement().getElementsByTagName("*").item(0);
            return child.getNodeName();
        } catch (NullPointerException e){
            return null;
        }
    }
}
