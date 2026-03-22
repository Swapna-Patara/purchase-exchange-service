package com.example.purchase.service;

import com.example.purchase.dto.PurchaseRequest;
import com.example.purchase.dto.PurchaseResponse;
import com.example.purchase.model.Purchase;

/**
 * Service interface for managing purchase transactions.
 */
public interface IPurchaseService {
    
    /**
     * Saves a new purchase transaction.
     *
     * @param req the purchase request containing transaction details
     * @return the saved Purchase entity
     */
    Purchase save(PurchaseRequest req);
    
    /**
     * Converts a purchase amount to a different currency.
     *
     * @param id the purchase ID
     * @param currency the target currency code
     * @param country the country code for the exchange rate
     * @return the purchase response with converted amount
     * @throws com.example.purchase.exception.ResourceNotFoundException if purchase is not found
     * @throws IllegalArgumentException if country or currency is invalid
     */
    PurchaseResponse convert(String id, String currency, String country);
}
