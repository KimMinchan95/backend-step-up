package dev.training.back.purchase.dto;

import lombok.RequiredArgsConstructor;
import lombok.Getter;
import dev.training.back.purchase.model.Purchase;
import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class PurchaseResponse {

    private final Long id;
    private final Long productId;
    private final Integer unitPrice;
    private final Integer quantity;
    private final LocalDateTime purchasedAt;
    private final String status;
    private final LocalDateTime canceledAt;

    public static PurchaseResponse from(Purchase purchase) {
        return new PurchaseResponse(purchase.getId(), purchase.getProductId(), purchase.getUnitPrice(), purchase.getQuantity(), purchase.getPurchasedAt(), purchase.getStatus(), purchase.getCanceledAt());
    }
 }
