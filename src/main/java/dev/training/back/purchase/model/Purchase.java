package dev.training.back.purchase.model;

import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter 
@Setter
public class Purchase {
    
    private Long id;
    private Long productId;
    private Integer quantity;
    private Integer unitPrice;
    private LocalDateTime purchasedAt;
}
