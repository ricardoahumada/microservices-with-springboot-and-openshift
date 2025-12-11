package com.microcompany.productsservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microcompany.productsservice.exception.ProductNotfoundException;
import com.microcompany.productsservice.model.Product;
import com.microcompany.productsservice.persistence.ProductsRepository;
import com.microcompany.productsservice.service.ProductsService;
import com.microcompany.productsservice.util.JsonUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// TODO: uncomment and implement methods
@ExtendWith(SpringExtension.class)
@WebMvcTest(ProductServiceController.class)
public class ProductServiceControllerTest_WebMvcTest {

    @BeforeEach
    public void setUp() {

        List<Product> productsFake = List.of(
                new Product(1l, "Fake prod 1", "111-222-3333"),
                new Product(2l, "Fake prod 2", "111-222-4444"),
                new Product(3l, "Fake prod 3", "111-222-555")
        );

        Mockito.when(prodServiceMock.getProductsByText("")).thenReturn(productsFake);
        Mockito.when(prodServiceMock.getProductsByText("a")).thenThrow(new ProductNotfoundException("No hay productos mock"));

        Mockito.when(prodServiceMock.create(Mockito.any(Product.class)))
                .thenAnswer(elem -> {
                    Product ap = (Product) elem.getArguments()[0];
                    ap.setId(100L);
                    return ap;
                });

        Mockito.when(productsRepositoryMock.save(Mockito.any(Product.class)))
                .thenAnswer(elem -> {
                    Product ap = (Product) elem.getArguments()[0];
                    ap.setId(100L);
                    return ap;
                });
    }

    @Autowired
    private MockMvc mvc;

    @MockBean
    private ProductsService prodServiceMock;

    @MockBean
    private ProductsRepository productsRepositoryMock;


    @Test
    public void givenProducts_whenGetProducts_thenStatus200() throws Exception {
        mvc.perform(get("/products").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].name", is("Fake prod 1")));
    }

    @Test
    public void givenProducts_whenGetProductsBadSearch_thenStatus404() throws Exception {
        mvc.perform(get("/products?nombrewith=a").accept(MediaType.APPLICATION_JSON))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    void givenProducts_whenValidCreateProduct_thenIsCreatedAndHaveId() throws Exception {
        Product newProd = new Product(null, "New Fake product", "111-222-3333");

        mvc.perform(post("/products")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonUtil.asJsonString(newProd))
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isCreated())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id", greaterThan(0)));
    }

    @Test
    void givenProducts_whenInvalidCreateProduct_thenPreconditionFalied() throws Exception {
        Product newProd = new Product(null, "New Fake Product", "111-222-333");

        mvc.perform(post("/products")
                        .accept(MediaType.APPLICATION_JSON)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonUtil.asJsonString(newProd))
                )
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isPreconditionFailed())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.serial").exists());
    }

}