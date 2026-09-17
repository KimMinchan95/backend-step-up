package dev.training.back.purchase.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import dev.training.back.purchase.service.PurchaseService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.ResponseEntity;
import dev.training.back.purchase.dto.PurchaseRequest;
import dev.training.back.purchase.dto.PurchaseResponse;
import dev.training.back.purchase.model.Purchase;
import org.springframework.http.HttpStatus;


@RestController
@RequestMapping("/purchases")
@RequiredArgsConstructor
public class PurchaseController {
    
    private final PurchaseService purchaseService;

    @PostMapping
    public ResponseEntity<PurchaseResponse> purchase(@Valid @RequestBody PurchaseRequest request) {
        Purchase saved = purchaseService.purchase(request.getProductId(), request.getQuantity());
        return ResponseEntity.status(HttpStatus.CREATED).body(PurchaseResponse.from(saved));
    }
}
