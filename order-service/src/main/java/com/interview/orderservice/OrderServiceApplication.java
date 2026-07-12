package com.interview.orderservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * The single entry point of the app.
 *
 * @SpringBootApplication is THREE annotations in one (a [MUST] interview question — Step 7):
 *   - @Configuration        : this class can define beans
 *   - @EnableAutoConfiguration : Boot auto-wires Tomcat, Jackson, DispatcherServlet, etc.
 *   - @ComponentScan        : scans THIS package and everything below it for @Component/@Controller/...
 *
 * SpringApplication.run() boots the whole thing: creates the ApplicationContext (the IoC
 * container you already know), starts embedded Tomcat, and registers the DispatcherServlet.
 */
@SpringBootApplication
@EnableFeignClients
@EnableScheduling
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
