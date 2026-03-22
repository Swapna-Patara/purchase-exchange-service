package com.example.purchase.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Exchange rate data from Treasury API.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TreasuryData {
    @JsonProperty("country_currency_desc")
    private String countryCurrencyDesc;
    
    @JsonProperty("exchange_rate")
    private String exchangeRate;
    
    @JsonProperty("record_date")
    private String recordDate;
}
