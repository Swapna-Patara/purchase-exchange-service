package com.example.purchase.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Data Transfer Object for purchase creation requests.
 * All fields are validated according to business requirements.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseRequest {
    
    /**
     * Purchase description - required, max 50 characters
     */
    @NotBlank(message = "Description is required")
    @Size(max = 50, message = "Description must not exceed 50 characters")
    @JsonProperty("description")
    private String description;
    
    /**
     * Transaction date - required, must not be in the future
     */
    @NotNull(message = "Transaction date is required")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
    @JsonProperty("transactionDate")
    private LocalDate transactionDate;
    
    /**
     * Purchase amount in USD - required, must be positive
     */
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be a valid positive amount")
    @JsonProperty("amount")
    private BigDecimal amount;
}
