package com.microcompany.productsservice.service;

import com.microcompany.productsservice.exception.NewProductException;
import com.microcompany.productsservice.model.Product;
import com.microcompany.productsservice.persistence.ProductsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

@Service
public class ProductsService {
    @Autowired
    private ProductsRepository productsRepository;

    @PersistenceContext
    EntityManager em;

    public List<Product> getProductsByText(String text) {
        return productsRepository.findByNameContaining(text);
    }

    public Product duplicate(Long id) {
        Product currProd = productsRepository.findById(id).orElseThrow(() -> new RuntimeException());
        em.detach(currProd);
        currProd.setId(null);
        return productsRepository.save(currProd);
//        Product newProduct = new Product(null, currProd.getName(), currProd.getSerial());
//        return productsRepository.save(newProduct);
    }

     public Product create(Product product) {
        try {
            product.isValid();
            return productsRepository.save(product);
        } catch (Exception e) {
            throw new NewProductException(e.getMessage());
        }
    }
}
