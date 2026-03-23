package com.example.purchase.service.impl;

import com.example.purchase.config.ExchangeProperties;
import com.example.purchase.dto.TreasuryData;
import com.example.purchase.dto.TreasuryResponse;
import com.example.purchase.exception.ExchangeRateNotFoundException;
import com.example.purchase.service.IExchangeRateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;


@Service
public class ExchangeRateServiceImpl implements IExchangeRateService {

    private static final Logger logger =
            LoggerFactory.getLogger(ExchangeRateServiceImpl.class);

    private final RestTemplate restTemplate;
    private final ExchangeProperties props;

    public ExchangeRateServiceImpl(RestTemplate restTemplate, ExchangeProperties props) {
        this.restTemplate = restTemplate;
        this.props = props;
    }


    @Override
    public BigDecimal getRate(String country, String currency, LocalDate purchaseDate) {
        String normalizedCountry = country.trim();
        String normalizedCurrency = currency.trim();

        logger.info("Fetching exchange rate for {} in {} on {}", normalizedCurrency, normalizedCountry, purchaseDate);

        LocalDate cutoffDate = calculateCutoffDate(purchaseDate);

        BigDecimal exchangeRate = searchExchangeRate(normalizedCountry, normalizedCurrency, purchaseDate, cutoffDate);

        if (exchangeRate != null) {
            return exchangeRate;
        }

        throw new ExchangeRateNotFoundException(
                String.format("No exchange rate found within %d months before %s for Country & Currency - %s & %s",
                        props.getMaxMonths(), purchaseDate, normalizedCountry, normalizedCurrency)
        );
    }

    /**
     * Searches for exchange rate by paginating through Treasury API responses.
     */
    private BigDecimal searchExchangeRate(String country, String currency, LocalDate purchaseDate, LocalDate cutoffDate) {
        int currentPage = 1;
        final int MAX_ITERATIONS = props.getMaxIterations(); // Safety limit to prevent infinite loop

        while (currentPage <= MAX_ITERATIONS) { // Safety limit to prevent infinite loop
            TreasuryResponse response = fetchTreasuryData(country, currency, currentPage, purchaseDate);

            if (isEmptyResponse(response)) {
                return null;
            }

            List<TreasuryData> rates = response.getData();

            // Try to find the best matching rate within the valid date range
            BigDecimal exchangeRate = findBestMatchingRate(rates, purchaseDate, cutoffDate);
            if (exchangeRate != null) {
                return exchangeRate;
            }

            // Stop pagination if oldest record is beyond cutoff date
            if (shouldStopPagination(rates, cutoffDate)) {
                return null;
            }

            // Check if there's a next page
            if (!hasNextPage(response)) {
                logger.info("No more pages");
                return null;
            }

            currentPage++;
        }

        return null;
    }


    /**
     * Calculates the cutoff date (N months before purchase date).
     */
    private LocalDate calculateCutoffDate(LocalDate purchaseDate) {
        return purchaseDate.minusMonths(props.getMaxMonths());
    }

    /**
     * Fetches exchange rate data from Treasury API for a specific page.
     */
    private TreasuryResponse fetchTreasuryData(String country, String currency, int page, LocalDate purchaseDate) {
        URI uri = buildTreasuryApiUri(country, currency, page, purchaseDate);
        logger.info("Fetching data from: {}", uri);

        ResponseEntity<TreasuryResponse> response = restTemplate.getForEntity(uri, TreasuryResponse.class);
        return response.getBody();
    }

    /**
     * Builds the URI for Treasury API with query parameters.
     */
    private URI buildTreasuryApiUri(String country, String currency, int page, LocalDate purchaseDate) {
        return UriComponentsBuilder.fromUriString(props.getBaseUrl())
                .queryParam("filter", String.format("country:eq:%s,currency:eq:%s,record_date:lt:%s", country, currency, purchaseDate))
                .queryParam("sort", "-record_date")
                .queryParam("page[size]", props.getPageSize())
                .queryParam("page[number]", page)
                .build()
                .toUri();
    }

    /**
     * Checks if the response is empty or null.
     */
    private boolean isEmptyResponse(TreasuryResponse response) {
        return response == null || response.getData() == null || response.getData().isEmpty();
    }

    /**
     * Finds the best matching exchange rate within the valid date range.
     * Returns the rate with the most recent date that is not after the purchase date
     * and not before the cutoff date.
     */
    private BigDecimal findBestMatchingRate(List<TreasuryData> rates, LocalDate purchaseDate, LocalDate cutoffDate) {
        return rates.stream()
                .map(this::mapToRate)
                .filter(rate -> isWithinValidDateRange(rate.date, purchaseDate, cutoffDate))
                .max(Comparator.comparing(rate -> rate.date))
                .map(rate -> rate.exchangeRate)
                .orElse(null);
    }

    /**
     * Checks if a date is within the valid range (not after purchase date and not before cutoff).
     */
    private boolean isWithinValidDateRange(LocalDate rateDate, LocalDate purchaseDate, LocalDate cutoffDate) {
        return !rateDate.isAfter(purchaseDate) && !rateDate.isBefore(cutoffDate);
    }

    /**
     * Determines if pagination should stop based on the oldest record in current page.
     * Stops if the oldest record is older than the cutoff date.
     */
    private boolean shouldStopPagination(List<TreasuryData> rates, LocalDate cutoffDate) {
        LocalDate oldestDateInPage = rates.stream()
                .map(rate -> LocalDate.parse(rate.getRecordDate()))
                .min(LocalDate::compareTo)
                .orElse(LocalDate.now());

        return oldestDateInPage.isBefore(cutoffDate);
    }

    /**
     * Checks if there's a next page available in the response.
     */
    private boolean hasNextPage(TreasuryResponse response) {
        return response != null
                && response.getLinks() != null
                && response.getLinks().getNext() != null;
    }


    private Rate mapToRate(TreasuryData d) {
        return new Rate(
                LocalDate.parse(d.getRecordDate()),
                new BigDecimal(d.getExchangeRate())
        );
    }

    /**
     * Internal class to hold exchange rate data with its associated date.
     */
    private static class Rate {
        final LocalDate date;
        final BigDecimal exchangeRate;

        Rate(LocalDate date, BigDecimal exchangeRate) {
            this.date = date;
            this.exchangeRate = exchangeRate;
        }
    }

}
