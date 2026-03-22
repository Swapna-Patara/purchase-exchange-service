package com.example.purchase.exception;

/**
 * Exception thrown when an exchange rate cannot be found for the specified criteria.
 * This is typically thrown when the Treasury API does not have data for the requested
 * country, currency, and date combination.
 */
public class ExchangeRateNotFoundException extends RuntimeException {

     /**
     * Constructs a new ExchangeRateNotFoundException with the specified detail message.
     *
     * @param message the detail message explaining why the exchange rate was not found
     */
    public ExchangeRateNotFoundException(String message) {
        super(message);
    }

    /**
     * Constructs a new ExchangeRateNotFoundException with the specified detail message and cause.
     *
     * @param message the detail message explaining why the exchange rate was not found
     * @param cause the underlying cause of this exception
     */
    public ExchangeRateNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
