package com.norkts.icros;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.metrics.buffering.BufferingApplicationStartup;

@SpringBootApplication(scanBasePackages = {
        "com.norkts.icros"})
public class ProxyApplication {
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(ProxyApplication.class);
        app.setApplicationStartup(new BufferingApplicationStartup(2048));
        app.run(args);
    }
}
