package com.microcompany.productsservice.controller;

import com.microcompany.productsservice.exception.ProductNotfoundException;
import com.microcompany.productsservice.model.Product;
import com.microcompany.productsservice.persistence.ProductsRepository;
import com.microcompany.productsservice.service.ProductsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

// TODO: uncomment and implement methods
@ExtendWith(MockitoExtension.class) // más puro
@MockitoSettings(strictness = Strictness.LENIENT) // más puro
class ProductServiceControllerTestPure {

    @BeforeEach
    public void setUp() {

        List<Product> productsFake = List.of(
                new Product(1l, "Fake prod 1", "111-222-3333"),
                new Product(2l, "Fake prod 2", "111-222-4444"),
                new Product(3l, "Fake prod 3", "111-222-555")
        );

        Mockito.when(prodServiceMock.getProductsByText("Fake")).thenReturn(productsFake);
        Mockito.when(prodServiceMock.getProductsByText("a")).thenThrow(new ProductNotfoundException("No hay productos mock"));

        Mockito.when(prodServiceMock.create(Mockito.any(Product.class)))
                .thenAnswer(elem -> {
                    Product ap = (Product) elem.getArguments()[0];
                    ap.setId(100L);
                    return ap;
                });
    }

    @InjectMocks // más puro
    private ProductServiceController controller;

    @Mock
    private ProductsService prodServiceMock;

    @Mock
    ProductsRepository productsRepositoryMock;


    @Test
    void givenProducts_whengetAllProductsGoodSearch_thenOK() {
        ResponseEntity<List<Product>> response = controller.getAllProducts("");
        assertThat(response.getStatusCode().value()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.getBody()).isNotNull();

        List<Product> prods = response.getBody();
        System.out.println("Prods: " + prods);
    }

    @Test
    void givenProducts_whengetAllProductsBadSearch_thenNOTFOUND() {
        assertThatExceptionOfType(ProductNotfoundException.class).isThrownBy(() -> {
            ResponseEntity<List<Product>> response = controller.getAllProducts("a");
        });

    }

    @Test
    void givenProducts_whenVaildCreateProduct_thenIsCreatedAndHaveId() {


    }
}