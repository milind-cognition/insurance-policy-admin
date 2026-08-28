package com.acme.dropin.contract;

import org.junit.jupiter.api.Test;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The XML projection carries the same fields, in the same order, with the same
 * values as the JSON one - which is what "XML or JSON over the CICS web
 * service" has to mean if a C# caller and a Java caller are to agree.
 */
class ContractXmlTest {

    private final PolicyView policy = GoldenFixtures.policy("POL-00000001");

    @Test
    void policyElementsMatchTheJsonPropertiesInOrder() throws Exception {
        List<String> xmlElements = childNames(ContractXml.write(policy));
        List<String> jsonProperties = new ArrayList<>();
        ContractJson.mapper().readTree(GoldenFixtures.policyJson("POL-00000001"))
                .fieldNames().forEachRemaining(jsonProperties::add);
        assertEquals(jsonProperties, xmlElements);
    }

    @Test
    void moneyKeepsItsTwoDecimalsInXml() {
        assertTrue(ContractXml.write(policy).contains("<totalPremium>1250.00</totalPremium>"),
                "premium must not lose its scale in the XML projection");
        assertTrue(ContractXml.write(policy).contains("<effectiveDate>2025-01-01</effectiveDate>"));
    }

    @Test
    void coveragesAreWrappedAndOrdered() throws Exception {
        String xml = ContractXml.writeCoverages(GoldenFixtures.coverages("POL-00000001"));
        assertEquals(List.of("coverage", "coverage"), childNames(xml));
    }

    private static List<String> childNames(String xml) throws Exception {
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        NodeList children = document.getDocumentElement().getChildNodes();
        List<String> names = new ArrayList<>();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                names.add(child.getNodeName());
            }
        }
        return names;
    }
}
