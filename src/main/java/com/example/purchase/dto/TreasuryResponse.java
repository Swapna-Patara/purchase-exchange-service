package com.example.purchase.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Data Transfer Object for Treasury API response containing exchange rate data.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TreasuryResponse {
    private List<TreasuryData> data;
    private TreasuryLinks links;
}
