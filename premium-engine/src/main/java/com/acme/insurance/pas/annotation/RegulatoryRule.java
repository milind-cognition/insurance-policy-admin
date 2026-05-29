package com.acme.insurance.pas.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method or field as implementing a regulatory rule that is subject
 * to compliance audit. Migrated from hardcoded COBOL business logic in
 * PREMBAT and UNDWRT programs.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.TYPE})
public @interface RegulatoryRule {

    /** Short identifier for the rule (e.g. "TAX-3.5-PCT"). */
    String id();

    /** Human-readable description of the regulatory requirement. */
    String description();

    /** Originating COBOL program (e.g. "PREMBAT", "UNDWRT"). */
    String sourceProgram() default "";

    /** Originating COBOL paragraph (e.g. "3200-CALCULATE-PREMIUM"). */
    String sourceParagraph() default "";

    /** Regulatory jurisdiction or authority (e.g. "STATE-ALL", "FEDERAL"). */
    String jurisdiction() default "";
}
