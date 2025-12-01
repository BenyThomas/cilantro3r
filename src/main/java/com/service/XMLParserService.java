/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.service;

import java.io.ByteArrayInputStream;

import com.DTO.ReqToRubikon.Request;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.xmlbeans.impl.soap.MessageFactory;
import org.apache.xmlbeans.impl.soap.MimeHeaders;
import org.apache.xmlbeans.impl.soap.SOAPException;
import org.apache.xmlbeans.impl.soap.SOAPMessage;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import org.apache.cxf.staxutils.StaxUtils;
import org.springframework.stereotype.Service;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import philae.api.LoginUser;
import philae.api.ObjectFactory;

/**
 * @author MELLEJI
 */
@Service
public class XMLParserService {

    private static final Logger LOGGER = LoggerFactory.getLogger(XMLParserService.class);

    public static Document initDomXML(String xmlStr) throws SAXException, IOException, ParserConfigurationException {
        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document = documentBuilder.parse(new InputSource(new StringReader(xmlStr)));
        document.getDocumentElement().normalize();
        return document;
    }

    public static String getDomTagText(String tagName, String xmlStr) {
        String returnValue;
        String res = "-1";
        try {
            NodeList nodeList;
            try {
                nodeList = initDomXML(xmlStr).getElementsByTagName(tagName);
                for (int i = 0; i < nodeList.getLength(); i++) {
                    returnValue = nodeList.item(i).getTextContent();
                    // System.out.println("tagName: - "+returnValue);
                    res = returnValue;
                }
            } catch (IOException | ParserConfigurationException ex) {
                LOGGER.error(null, ex);
            }
            return res.trim();
        } catch (SAXException ex) {
            LOGGER.error(null, ex);
        }
        return res;
    }

    public static String getDomTagTextex(String st) throws ParserConfigurationException, SAXException, IOException {
        //still working on progress
        DocumentBuilder db = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        InputSource is = new InputSource();
        is.setCharacterStream(new StringReader(st));
        String xmlRecords = "";
        Document doc = db.parse(is);
        NodeList nodes = doc.getElementsByTagName("gen:getGenericResult");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element element = (Element) nodes.item(i);
            NodeList nodes2 = doc.getElementsByTagName("Request");
            for (int k = 0; k < nodes2.getLength(); k++) {
                NodeList nodes3 = doc.getElementsByTagName("dataItem");
                for (int l = 0; l < nodes3.getLength(); l++) {
                    NodeList name = element.getElementsByTagName("name");
                    Element line = (Element) name.item(l);
                    xmlRecords = String.valueOf(xmlRecords) + "&" + getCharacterDataFromElement(line);
                    NodeList value = element.getElementsByTagName("value");
                    line = (Element) value.item(l);
                    if (line != null) {
                        xmlRecords = String.valueOf(xmlRecords) + "=" + getCharacterDataFromElement(line);
                    } else {
                        xmlRecords = String.valueOf(xmlRecords) + "=null";
                    }
                }
            }
        }
        return xmlRecords;
    }

    public static String getCharacterDataFromElement(Element e) {
        Node child = e.getFirstChild();
        if (child instanceof CharacterData) {
            CharacterData cd = (CharacterData) child;
            return cd.getData();
        }
        return "";
    }

    public static List getDomTagList(String tagName, String xmlStr) throws SAXException, IOException, ParserConfigurationException {
        List<String> list = new ArrayList<>();
        NodeList nodeList = initDomXML(xmlStr).getElementsByTagName(tagName);
        for (int i = 0; i < nodeList.getLength(); i++) {
            list.add(nodeList.item(i).getTextContent().trim());
        }
        return list;
    }

    public static String xmlToStringPretty(String input, int indent) {
        //xml make look good...
        try {
            Source xmlInput = new StreamSource(new StringReader(input));
            StringWriter stringWriter = new StringWriter();
            StreamResult xmlOutput = new StreamResult(stringWriter);
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setAttribute("indent-number", indent);
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(xmlInput, xmlOutput);
            return xmlOutput.getWriter().toString();
        } catch (IllegalArgumentException | TransformerException e) {
            LOGGER.error(null, e);
            return "-1";
        }
    }

    public static String xmlsrToString(XMLStreamReader xmlr) {
        String result = "-1";
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            StringWriter stringWriter = new StringWriter();
            if (xmlr != null) {
                try {
                    transformer.transform(new StAXSource(xmlr), new StreamResult(stringWriter));
                    result = stringWriter.toString();
                } catch (TransformerException ex) {
                    LOGGER.error(null, ex);
                }
            }
            return result;
        } catch (TransformerConfigurationException ex) {
            LOGGER.error(null, ex);
            return result;
        }
    }

    public static String xmlsrToStringPretty(XMLStreamReader xmlr) {
        String result = "-1";
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            StringWriter stringWriter = new StringWriter();
            if (xmlr != null) {
                try {
                    transformer.transform(new StAXSource(xmlr), new StreamResult(stringWriter));
                    result = stringWriter.toString();
                } catch (TransformerException ex) {
                    LOGGER.error(null, ex);
                }
            }
            return result;
        } catch (TransformerConfigurationException ex) {
            LOGGER.error(null, ex);
            return result;
        }
    }

    public static XMLStreamReader createXMLStreamReaderFromSOAPMessage(String soapResponse, String soapPart, String startTag) {
        XMLStreamReader xsr = null;
        DOMSource bodySource;

//        System.out.println("=====tags here" + soapResponse + soapPart + startTag);
        try {
            if (soapResponse.contains(startTag)) {
                //generate soap message from the string
                final SOAPMessage soapMessage = MessageFactory.newInstance().createMessage(new MimeHeaders(), new ByteArrayInputStream(soapResponse.getBytes()));
                if (soapPart.equalsIgnoreCase("header")) {
                    bodySource = new DOMSource(soapMessage.getSOAPPart().getEnvelope().getHeader());
                } else {
                    bodySource = new DOMSource(soapMessage.getSOAPPart().getEnvelope().getBody());
                }
                xsr = StaxUtils.createXMLStreamReader(bodySource);
//                LOGGER.trace("xsr:" + xsr.getLocalName());// Advance to Envelope tag
                while (!xsr.getLocalName().equals(startTag)) {
                    xsr.nextTag();
                    LOGGER.trace("xsr:" + xsr.getLocalName());// Advance to Envelope tag
                }
            }
        } catch (SOAPException | XMLStreamException | IOException e) {
            LOGGER.error(null, e);
        }
        return xsr;
    }

    public static void iterateXmlBody(Node node) {

        // get all child nodes
        NodeList list = node.getChildNodes();

        for (int i = 0; i < list.getLength(); i++) {

            // get child node
            Node childNode = list.item(i);
            if (childNode.getNodeName().equals("string")) {
                System.out.println("Found Node: " + childNode.getNodeName() + " - with value: " + childNode.getTextContent());
            } else if (childNode.getNodeName().equals("startIndex")) {
                System.out.println("Found Node: " + childNode.getNodeName() + " - with value: " + childNode.getTextContent());
            } else if (childNode.getNodeName().equals("name")) {
                System.out.println("Found Node: " + childNode.getNodeName() + " - with value: " + childNode.getTextContent());
            }

            // visit child node
            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                Element element = (Element) childNode;
                iterateXmlBody(element);
            }
        }

    }

    public static <T> T jaxbXMLToObject(String xmlData, Class<T> clazz) {   //I've passed the xmlString to unmarshaller by StringReader
        try {
            T result;
            JAXBContext context = JAXBContext.newInstance(clazz);
            Unmarshaller unmarshaller = context.createUnmarshaller();
            StreamSource source = new StreamSource(new StringReader(xmlData));
            result = unmarshaller.unmarshal(source, clazz).getValue();
            return result; // Of course this is after conversion
        } catch (JAXBException e) {
            LOGGER.error(null, e);
            //How to use it 
            //methodCallDTO dto = XMLParserService.jaxbXMLToObject(xmlMsg, methodCallDTO.class);

            // TODO - Handle Exception
        }
        return null;
    }

    public static <T> String objectToJaxbXML(LoginUser documentType, Class<T> clazz, String location) {   //I've passed the xmlString to unmarshaller by StringReader
        try {
            JAXBContext context = JAXBContext.newInstance(clazz);
            Marshaller marshaller = context.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
//            marshaller.setProperty(Marshaller.JAXB_NO_NAMESPACE_SCHEMA_LOCATION, location);
            JAXBElement<LoginUser> documentTypeJAXBElement = (new ObjectFactory()).createLoginUser(documentType);
            StringWriter sw = new StringWriter();
            marshaller.marshal(documentTypeJAXBElement, sw);
            return sw.toString();
        } catch (JAXBException e) {
            LOGGER.error(null, e);
        }
        return null;
    }

    public static String jaxbObjectToXML(LoginUser reqMsg, Boolean formattedOut, Boolean rmXMLHdr) {
        try {
            //Create JAXB Context
            JAXBContext jaxbContext = JAXBContext.newInstance(LoginUser.class);

            //Create Marshaller
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

            //Required formatting??           
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, formattedOut);
            //disable xml decoration <?xml version="1.0" encoding="UTF-8"?>7
            jaxbMarshaller.setProperty(Marshaller.JAXB_FRAGMENT, rmXMLHdr);
            //custom xml decoration
            //jaxbMarshaller.setProperty("com.sun.xml.bind.xmlHeaders",      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            //Print XML String to Console
            StringWriter sw = new StringWriter();
            //If we have JAXB root annotated class
            //Write XML to StringWriter
            JAXBElement<LoginUser> jaxbElement = new JAXBElement<>(new QName("api:loginUser"), LoginUser.class, reqMsg);
            jaxbMarshaller.marshal(jaxbElement, sw);
            //Verify XML Content
            String xmlContent = sw.toString();
            return xmlContent;
            //If we have JAXB annotated class
            //jaxbMarshaller.marshal(employeeObj, System.out);  
        } catch (JAXBException e) {
            LOGGER.error(null, e);
        }
        return null;
    }

    public static String jaxbGenericObjToXML(Object clazz, Boolean formattedOut, Boolean rmXMLHdr) {   //I've passed the xmlString to unmarshaller by StringReader
        try {
            //Create JAXB Context
            JAXBContext jaxbContext = JAXBContext.newInstance(clazz.getClass());

            //Create Marshaller
            Marshaller jaxbMarshaller = jaxbContext.createMarshaller();

            //Required formatting??           
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, formattedOut);
            //disable xml decoration <?xml version="1.0" encoding="UTF-8"?>7
            jaxbMarshaller.setProperty(Marshaller.JAXB_FRAGMENT, rmXMLHdr);
            //beautify the xml
            jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            //custom xml decoration
            //jaxbMarshaller.setProperty("com.sun.xml.bind.xmlHeaders",      "<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            //Print XML String to Console
            StringWriter sw = new StringWriter();
            //If we DO NOT have JAXB annotated class
            //If we DO NOT have JAXB annotated class
            //JAXBElement<Employee> jaxbElement =  new JAXBElement<Employee>( new QName("", "employee"),Employee.class,employeeObj);

            //jaxbMarshaller.marshal(jaxbElement, System.out);
            //If we have JAXB root annotated class
            //Write XML to StringWriter
            jaxbMarshaller.marshal(clazz, sw);
            //Verify XML Content
            String xmlContent = sw.toString().replace("<ns2:request xmlns:ns2=\"http://api.PHilae/\">", "").replace("</ns2:request>", "").replace("<ns2:request xmlns:ns2=\"http://ach.PHilae/\">", "").replace("<ns2:transfer xmlns:ns2=\"http://ach.PHilae/\">", "").replace("</ns2:transfer>", "");

            return xmlContent; // Of course this is after conversion
        } catch (JAXBException e) {
            LOGGER.error(null, e);
            //How to use it 
            //methodCallDTO dto = XMLParserService.jaxbXMLToObject(xmlMsg, methodCallDTO.class);

            // TODO - Handle Exception
        }
        return null;
    }

    public static String objectToXmlPretty(Object obj) {
        String xml = null;
        // Converting a Java class object to XML
        XmlMapper xmlMapper = new XmlMapper();
        xmlMapper.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
        xmlMapper.enable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        xmlMapper.disable(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT);
        xmlMapper.disable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
        //xmlMapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
        try {
            xml = xmlMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
            // LOGGER.info("{}", xml);
        } catch (Exception e) {
            LOGGER.info("{}", xml);
        }
        return xml;
    }

    public static String getRootTagText(String tagName, String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

            NodeList nodes = doc.getDocumentElement().getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
                Node current = nodes.item(i);
                if (current.getNodeType() == Node.ELEMENT_NODE && current.getNodeName().equalsIgnoreCase(tagName)) {
                    return current.getTextContent();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static List<Map<String, String>> getNestedTags(String xml, String parentTag, List<String> childTags) {
        List<Map<String, String>> results = new ArrayList<>();
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

            NodeList parentList = doc.getElementsByTagName(parentTag);

            for (int i = 0; i < parentList.getLength(); i++) {
                Node node = parentList.item(i);
                Map<String, String> childData = new HashMap<>();
                NodeList children = node.getChildNodes();

                for (int j = 0; j < children.getLength(); j++) {
                    Node child = children.item(j);
                    if (child.getNodeType() == Node.ELEMENT_NODE && childTags.contains(child.getNodeName())) {
                        childData.put(child.getNodeName(), child.getTextContent());
                    }
                }

                if (!childData.isEmpty()) {
                    results.add(childData);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return results;
    }


}
