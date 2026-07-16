package com.interview.catalogservice;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Tag("integration") // full-context test — needs real infra; excluded from the fast CI
@SpringBootTest
class CatalogServiceApplicationTests {

    @Test
    void contextLoads() {
    }
}
