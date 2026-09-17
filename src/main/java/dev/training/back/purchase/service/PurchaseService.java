package dev.training.back.purchase.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import dev.training.back.purchase.mapper.PurchaseMapper;
import dev.training.back.product.mapper.ProductMapper;
import dev.training.back.product.service.ProductService;
import dev.training.back.purchase.model.Purchase;
import org.springframework.transaction.annotation.Transactional;
import dev.training.back.product.model.Product;

@Service
@RequiredArgsConstructor
public class PurchaseService {
    
    private final ProductService productService;
    private final PurchaseMapper purchaseMapper;
    private final ProductMapper productMapper;

    @Transactional
    public Purchase purchase(Long productId, Integer quantity) {
        if ((productId == null || productId <= 0) || (quantity == null || quantity <= 0)) {
            throw new IllegalArgumentException("상품 ID 또는 수량이 유효하지 않습니다. productId=" + productId + ", quantity=" + quantity);
        }

        Product product = productService.getProduct(productId);
        int updated = productMapper.decreaseQuantity(productId, quantity);

        if (updated == 0) {
            throw new IllegalStateException("재고가 부족합니다. id=" + productId + ", stock=" + product.getStock() + ", quantity=" + quantity);
        }

        Purchase purchase = new Purchase();
        purchase.setProductId(productId);
        purchase.setQuantity(quantity);
        purchase.setUnitPrice(product.getPrice());

        purchaseMapper.insert(purchase);
        Purchase saved = purchaseMapper.findById(purchase.getId());

        if (saved == null) {
            throw new IllegalStateException("구매 정보를 찾을 수 없습니다. id=" + purchase.getId());
        }

        return saved;
    }
}
