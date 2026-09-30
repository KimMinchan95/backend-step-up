package dev.training.back.purchase.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import dev.training.back.purchase.dto.PurchaseResponse;
import dev.training.back.purchase.mapper.PurchaseMapper;
import dev.training.back.product.mapper.ProductMapper;
import dev.training.back.product.service.ProductService;
import dev.training.back.purchase.model.Purchase;
import org.springframework.transaction.annotation.Transactional;
import dev.training.back.product.model.Product;
import java.time.LocalDateTime;
import dev.training.back.purchase.model.PurchaseStatus;

@Service
@RequiredArgsConstructor
public class PurchaseService {
    private final ProductService productService;
    private final PurchaseMapper purchaseMapper;
    private final ProductMapper productMapper;

    @Transactional
    public PurchaseResponse createPurchase(Long productId, Integer quantity) {
        if ((productId == null || productId <= 0) || (quantity == null || quantity <= 0)) {
            throw new IllegalArgumentException("상품 ID 또는 수량이 유효하지 않습니다. productId=" + productId + ", quantity=" + quantity);
        }

        Product product = productService.getProduct(productId);

        if (product.getStock() < quantity) {
            throw new IllegalStateException("재고가 부족합니다. productId=" + productId + ", stock=" + product.getStock() + ", quantity=" + quantity);
        }

        int updated = productMapper.decreaseQuantity(productId, quantity);

        if (updated == 0) {
            throw new IllegalStateException("업데이트를 실패했습니다. productId=" + productId + ", stock=" + product.getStock() + ", quantity=" + quantity);
        }

        Purchase purchase = Purchase.builder()
            .productId(productId)
            .quantity(quantity)
            .unitPrice(product.getPrice())
            .status(PurchaseStatus.COMPLETED)
            .purchasedAt(LocalDateTime.now())
            .canceledAt(null)
            .build();

        int inserted = purchaseMapper.insert(purchase);

        if (inserted == 0 || purchase.getId() == null) {
            throw new IllegalStateException("구매 정보를 찾을 수 없습니다. id=" + purchase.getId());
        }

        return PurchaseResponse.from(purchase);
    }

    public Purchase findById(Long purchaseId) {
        if (purchaseId == null || purchaseId <= 0) {
            throw new IllegalArgumentException("구매 ID가 유효하지 않습니다. purchaseId=" + purchaseId);
        }

        Purchase purchase = purchaseMapper.findById(purchaseId);
        if (purchase == null) {
            throw new IllegalArgumentException("구매 정보를 찾을 수 없습니다. purchaseId=" + purchaseId);
        }
        return purchase;
    }

    @Transactional
    public PurchaseResponse cancelPurchase(Long purchaseId) {
        if (purchaseId == null || purchaseId <= 0) {
            throw new IllegalArgumentException("구매 ID가 유효하지 않습니다. purchaseId=" + purchaseId);
        }

        Purchase purchase = findById(purchaseId);

        if (purchase.getStatus() != PurchaseStatus.COMPLETED) {
            throw new IllegalStateException("구매 상태가 완료되지 않았습니다. purchaseId=" + purchaseId);
        }

        int canceled = purchaseMapper.cancelById(purchaseId);

        if (canceled == 0) {
            throw new IllegalStateException("구매 취소를 실패했습니다. purchaseId=" + purchaseId);
        }
        
        int increased = productMapper.increaseQuantity(purchase.getProductId(), purchase.getQuantity());

        if (increased != 1) {
            throw new IllegalStateException("재고 증가를 실패했습니다. productId=" + purchase.getProductId() + ", quantity=" + purchase.getQuantity());
        }

        return PurchaseResponse.from(findById(purchaseId));
    }
}
