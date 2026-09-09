package dev.training.back.product.service;

import dev.training.back.product.mapper.ProductMapper;
import dev.training.back.product.model.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductMapper productMapper;

    public List<Product> getProducts() {
        return productMapper.findAll();
    }

    public Product getProduct(Long productId) {
        Product product = productMapper.findById(productId);
        if (product == null) {
            throw new IllegalArgumentException("상품을 찾을 수 없습니다. id=" + productId);
        }
        return product;
    }
}
