package com.example.purchase.service;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Service interface for fetching exchange rates from external APIs.
 */
public interface IExchangeRateService {
    
    /**
     * Retrieves the exchange rate for a given country, currency, and purchase date.
     * Searches within the configured time window before the purchase date.
     *
     * @param country the country code
     * @param currency the currency code
     * @param purchaseDate the date of purchase
     * @return the exchange rate
     * @throws com.example.purchase.exception.ExchangeRateNotFoundException if no rate is found
     */
    BigDecimal getRate(String country, String currency, LocalDate purchaseDate);
}
