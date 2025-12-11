package com.microcompany.productsservice.service;


import com.microcompany.productsservice.exception.ProductNotfoundException;
import com.microcompany.productsservice.model.Product;
import com.microcompany.productsservice.persistence.ProductsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@SpringBootTest
@Sql(value = "classpath:testing.sql")
public class ProductsServiceTest {

    @Autowired
    ProductsService prodService;

    @Test
    void givenProductsWhenSearchByTextThenIsNotNull() {
        List<Product> products = prodService.getProductsByText("Test");
        assertThat(products).isNotNull();
        assertThat(products.size()).isGreaterThan(0);
    }

    @Test
    void givenProductsWhenSearchByTextNoExistThenException() {
        assertThatExceptionOfType(ProductNotfoundException.class).isThrownBy(() -> {
            List<Product> products = prodService.getProductsByText("a");
        });

    }

    @Test
    void givenValidProduct_WhenCreate_ThenThenIsNotNull() {
    }
}