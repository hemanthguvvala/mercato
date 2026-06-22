package com.interview.catalogservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * Catalog service entry point.
 *
 * This is the second microservice in the platform — it will own everything about Products
 * (data + read/write API). order-service will call it over Feign instead of touching the
 * Product table directly.
 */
@SpringBootApplication
@EnableCaching
public class CatalogServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CatalogServiceApplication.class, args);
    }
}
