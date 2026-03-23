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
            TreasuryResponse response = fetchTreasuryData(country, currency, purchaseDate);

            if (isEmptyResponse(response)) {
                return null;
            }

            List<TreasuryData> rates = response.getData();

            // API is configured to return only one record (page[size]=1),
            // explicitly check that single record's date is within the valid range.
            if (rates.size() == 1) {
                TreasuryData single = rates.get(0);
                LocalDate rateDate = LocalDate.parse(single.getRecordDate());
                if (isWithinValidDateRange(rateDate, purchaseDate, cutoffDate)) {
                    return mapToRate(single).exchangeRate;
                }
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
    private TreasuryResponse fetchTreasuryData(String country, String currency, LocalDate purchaseDate) {
        URI uri = buildTreasuryApiUri(country, currency, purchaseDate);
        logger.info("Fetching data from: {}", uri);

        ResponseEntity<TreasuryResponse> response = restTemplate.getForEntity(uri, TreasuryResponse.class);
        return response.getBody();
    }

    /**
     * Builds the URI for Treasury API with query parameters.
     */
    private URI buildTreasuryApiUri(String country, String currency, LocalDate purchaseDate) {
        return UriComponentsBuilder.fromUriString(props.getBaseUrl())
                .queryParam("filter", String.format("country:eq:%s,currency:eq:%s,record_date:lte:%s", country, currency, purchaseDate))
                .queryParam("sort", "-record_date")
                .queryParam("page[size]", props.getPageSize())
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
     * Checks if a date is within the valid range (not after purchase date and not before cutoff).
     */
    private boolean isWithinValidDateRange(LocalDate rateDate, LocalDate purchaseDate, LocalDate cutoffDate) {
        return !rateDate.isAfter(purchaseDate) && !rateDate.isBefore(cutoffDate);
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
