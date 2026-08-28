package com.acme.dropin.contract;

import java.lang.reflect.RecordComponent;
import java.util.List;

/**
 * The XML projection of the same contract.
 *
 * <p>Progressive's estate fronts CICS with Web Services exchanging XML as well
 * as JSON, so the contract has to be true in both projections. Element names,
 * order and value formatting are taken from the same record definitions the
 * JSON projection uses, which is what makes "the same fields in the same
 * order" a fact rather than a promise.
 */
public final class ContractXml {

    private ContractXml() {
    }

    public static String write(PolicyView policy) {
        return element("policy", policy);
    }

    public static String writeCoverages(List<CoverageView> coverages) {
        StringBuilder sb = new StringBuilder("<coverages>");
        for (CoverageView coverage : coverages) {
            sb.append(element("coverage", coverage));
        }
        return sb.append("</coverages>").toString();
    }

    private static String element(String name, Record value) {
        StringBuilder sb = new StringBuilder("<").append(name).append('>');
        for (RecordComponent component : value.getClass().getRecordComponents()) {
            Object v = read(component, value);
            sb.append('<').append(component.getName()).append('>');
            sb.append(v == null ? "" : escape(String.valueOf(v)));
            sb.append("</").append(component.getName()).append('>');
        }
        return sb.append("</").append(name).append('>').toString();
    }

    private static Object read(RecordComponent component, Record value) {
        try {
            return component.getAccessor().invoke(value);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot read " + component.getName(), e);
        }
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
