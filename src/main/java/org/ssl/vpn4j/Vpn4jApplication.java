package org.ssl.vpn4j;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "org.ssl.**")
public class Vpn4jApplication {

    public static void main(String[] args) {
        SpringApplication.run(Vpn4jApplication.class, args);
    }

}
