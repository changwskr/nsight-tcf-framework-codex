package com.nh.nsight.marketing.oc.support;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

/**
 * 업로드·classpath XML 파싱 시 XXE/외부 DTD 로딩을 제한합니다.
 */
public final class SecureXmlDocuments {

    private SecureXmlDocuments() {
    }

    public static Document parseUtf8(String text) throws Exception {
        byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
        try (InputStream in = new java.io.ByteArrayInputStream(bytes)) {
            return parse(in);
        }
    }

    public static Document parse(InputStream in) throws Exception {
        return newDocumentBuilder().parse(new InputSource(in));
    }

    public static DocumentBuilder newDocumentBuilder() throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        factory.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
        factory.setValidating(false);
        factory.setNamespaceAware(true);
        DocumentBuilder builder = factory.newDocumentBuilder();
        builder.setEntityResolver((publicId, systemId) -> new InputSource(new StringReader("")));
        return builder;
    }
}
