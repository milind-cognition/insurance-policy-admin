package com.acme.insurance.pas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * PAS REST Facade Application.
 *
 * Read-only REST API facade over the COBOL Policy Administration System.
 * Added in 2022 to provide modern HTTP/JSON access to policy data stored
 * in DB2 on z/OS. This facade does NOT write to the database - all writes
 * go through CICS transactions on the mainframe.
 *
 * WARNING: This is a read-only facade. Do not add write endpoints.
 * All policy mutations must go through CICS (PNEW, PRWL, PEND, PUWR).
 *
 * @author T. Nguyen (2022)
 */
@SpringBootApplication
public class PasFacadeApplication {

    public static void main(String[] args) {
        SpringApplication.run(PasFacadeApplication.class, args);
    }
}
