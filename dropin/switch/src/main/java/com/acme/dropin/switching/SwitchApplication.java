package com.acme.dropin.switching;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The demo's money shot, as a runnable service.
 *
 * <p>Start it with {@code --pas.backend=mainframe} and the COBOL-semantics path
 * answers. Start it with {@code --pas.backend=mirror} and the Java 21
 * implementation answers. The caller's URL, headers and payload are the same in
 * both cases; nothing on the calling side is rebuilt or reconfigured.
 */
@SpringBootApplication
public class SwitchApplication {

    public static void main(String[] args) {
        SpringApplication.run(SwitchApplication.class, args);
    }
}
