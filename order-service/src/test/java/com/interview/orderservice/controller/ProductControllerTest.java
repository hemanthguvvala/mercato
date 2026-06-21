package com.interview.orderservice.controller;

import com.interview.orderservice.service.ProductBatchService;
import com.interview.orderservice.service.ProductService;
import com.interview.orderservice.web.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)   // loads ONLY the web layer for this controller — fast slice
class ProductControllerTest {

    @Autowired
    MockMvc mockMvc;                   // simulates HTTP requests — no real server/port

    @MockBean
    ProductService service;            // a Mockito mock replaces the real service in this slice

    @MockBean
    ProductBatchService batchService;  // controller also needs this bean now (/batch endpoint)

    @Test
    void getProduct_found_returns200() throws Exception {
        when(service.findById(1L)).thenReturn(Optional.of(new Product(1L, "Phone", 499.0, 0L)));

        mockMvc.perform(get("/product/1"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$.name").value("Phone"))
               .andExpect(jsonPath("$.price").value(499.0));
    }

    @Test
    void getProduct_missing_returns404() throws Exception {
        when(service.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/product/999"))
               .andExpect(status().isNotFound());
    }

    @Test
    void create_invalid_returns400() throws Exception {
        mockMvc.perform(post("/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\",\"price\":-6}"))
               .andExpect(status().isBadRequest())
               .andExpect(jsonPath("$.errors.name").exists());   // proves validation + advice work
    }
}
