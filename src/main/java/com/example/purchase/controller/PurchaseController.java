package com.example.purchase.controller;

import com.example.purchase.dto.PurchaseRequest;
import com.example.purchase.dto.PurchaseResponse;
import com.example.purchase.model.Purchase;
import com.example.purchase.service.IPurchaseService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for Purchase operations.
 * Handles creation of purchase transactions and currency conversion.
 */
@RestController
@RequestMapping("/purchases")
public class PurchaseController {

    private final IPurchaseService purchaseService;

    public PurchaseController(IPurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    /**
     * Creates a new purchase transaction.
     *
     * @param req the purchase request with description, date, and amount
     * @return the created purchase with generated ID
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<Purchase> create(@Valid @RequestBody PurchaseRequest req) {
        Purchase saved = purchaseService.save(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    /**
     * Converts a purchase amount to a specified currency.
     *
     * @param id the purchase ID
     * @param currency the target currency code
     * @param country the country code for exchange rate lookup
     * @return the purchase response with converted amount
     */
    @GetMapping("/{id}/convert")
    public PurchaseResponse convert(
            @PathVariable String id,
            @RequestParam
            @NotBlank(message = "Currency parameter is required")
            String currency,
            @RequestParam
            @NotBlank(message = "Country parameter is required")
            String country) {
        return purchaseService.convert(id, currency, country);
    }

}
