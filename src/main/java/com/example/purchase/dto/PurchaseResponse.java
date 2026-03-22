package com.example.purchase.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Data Transfer Object for purchase response with currency conversion details.
 * Contains the original purchase information and conversion details.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseResponse {
    
    /**
     * Unique purchase identifier
     */
    @JsonProperty("id")
    private String id;
    
    /**
     * Purchase description
     */
    @JsonProperty("description")
    private String description;
    
    /**
     * Transaction date
     */
    @JsonProperty("transactionDate")
    private LocalDate transactionDate;
    
    /**
     * Original purchase amount in USD
     */
    @JsonProperty("originalAmount")
    private BigDecimal originalAmount;
    
    /**
     * Exchange rate used for conversion
     */
    @JsonProperty("exchangeRate")
    private BigDecimal exchangeRate;
    
    /**
     * Amount after currency conversion, rounded to 2 decimal places
     */
    @JsonProperty("convertedAmount")
    private BigDecimal convertedAmount;
}
