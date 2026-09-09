package dev.training.back.product.service;

import dev.training.back.product.model.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "spring.docker.compose.skip.in-tests=false")
@Transactional
class ProductServiceIntegrationTests {

    @Autowired
    private ProductService productService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void readsProductFieldsFromMySql() {
        Long productId = insertProduct("조회 테스트 상품", 1200, 7);

        Product product = productService.getProduct(productId);

        assertThat(product.getId()).isEqualTo(productId);
        assertThat(product.getName()).isEqualTo("조회 테스트 상품");
        assertThat(product.getPrice()).isEqualTo(1200);
        assertThat(product.getStock()).isEqualTo(7);
    }

    @Test
    void listsProductsInIdOrderIncludingSoldOutProducts() {
        Long inStockId = insertProduct("재고 있는 상품", 1000, 3);
        Long soldOutId = insertProduct("품절 상품", 800, 0);

        assertThat(productService.getProducts())
                .extracting(Product::getId)
                .isSorted()
                .contains(inStockId, soldOutId);
    }

    @Test
    void rejectsMissingProduct() {
        Long productId = insertProduct("삭제할 상품", 1000, 1);
        jdbcTemplate.update("DELETE FROM products WHERE id = ?", productId);

        assertThatThrownBy(() -> productService.getProduct(productId))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("상품을 찾을 수 없습니다.");
    }

    private Long insertProduct(String name, int price, int stock) {
        jdbcTemplate.update(
                "INSERT INTO products (name, price, stock) VALUES (?, ?, ?)",
                name, price, stock);
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }
}
