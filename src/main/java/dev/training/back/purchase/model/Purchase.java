package dev.training.back.purchase.model;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Purchase {
    
    private Long id;
    private Long productId;
    private Integer quantity;
    private Integer unitPrice;
    private LocalDateTime purchasedAt;
}
