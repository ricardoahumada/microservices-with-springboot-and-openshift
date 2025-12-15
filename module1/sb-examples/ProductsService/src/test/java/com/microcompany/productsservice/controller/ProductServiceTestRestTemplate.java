package com.microcompany.productsservice.controller;

import com.microcompany.productsservice.model.Product;
import com.microcompany.productsservice.model.StatusMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.jdbc.Sql;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// TODO: uncomment and implement methods
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Sql("classpath:testing.sql")
public class ProductServiceTestRestTemplate {
    @Value(value = "${local.server.port}")
//    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void givenUrl_whenGetProducts_thenAStringExists() throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity("http://localhost:" + port + "/products", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        System.out.println(response.getBody());

        assertThat(response.getBody()).contains("Prod Test");
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);

    }

    @Test
    public void givenUrl_whenGetProducts_thenAProductExists() throws Exception {
        HttpHeaders header = new HttpHeaders();
        header.set("ACCEPT", MediaType.APPLICATION_JSON_VALUE);
        ResponseEntity<Product[]> response = restTemplate.getForEntity("http://localhost:" + port + "/products", Product[].class, header);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);

        System.out.println(response.getBody());
        assertThat(response.getBody()).extracting(Product::getName).contains("Prod Test 1");

    }

    //    @Test
    @ParameterizedTest
    @ValueSource(strings = {"a", "x", "y"})
    public void givenUrl_whenGetProductsBadQuery_then404(String text) throws Exception {
        HttpHeaders header = new HttpHeaders();
        header.set("ACCEPT", MediaType.APPLICATION_JSON_VALUE);

//        String text = "a";

        ResponseEntity<StatusMessage> response = restTemplate.getForEntity("http://localhost:" + port + "/products?nombrewith=" + text, StatusMessage.class, header);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);

        System.out.println(response.getBody());
        assertThat(response.getBody()).extracting(StatusMessage::getMessage).isEqualTo("No hay productos");

    }

    @Test
    public void givenAProduct_whenPostWithHeader_thenSuccess() throws URISyntaxException {
        Product newProd = new Product(null, "New Test Prod", "888-000-9999");

        HttpHeaders header = new HttpHeaders();
        header.set("ACCEPT", MediaType.APPLICATION_JSON_VALUE);
        header.set("CONTENT-TYPE", MediaType.APPLICATION_JSON_VALUE);

        HttpEntity<Product> httpEntity = new HttpEntity<>(newProd, header);

        String uri = "http://localhost:" + port + "/products";
        ResponseEntity<Product> response = restTemplate.postForEntity(uri, httpEntity, Product.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);

        System.out.println(response.getBody());
        assertThat(response.getBody()).extracting(Product::getId).isNotNull();
    }

}
