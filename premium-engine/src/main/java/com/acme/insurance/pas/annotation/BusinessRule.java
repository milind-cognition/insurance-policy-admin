package com.acme.insurance.pas.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method or field as implementing a business rule from the legacy
 * COBOL system. Preserved for traceability and compliance audit during
 * mainframe modernization.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.TYPE})
public @interface BusinessRule {

    /** Short identifier for the rule (e.g. "BASE-RATE-AUT"). */
    String id();

    /** Human-readable description of the business rule. */
    String description();

    /** Originating COBOL program (e.g. "PREMBAT", "UNDWRT"). */
    String sourceProgram() default "";

    /** Originating COBOL paragraph (e.g. "3200-CALCULATE-PREMIUM"). */
    String sourceParagraph() default "";

    /** Date the rule was last modified in the COBOL source. */
    String lastModified() default "";
}
