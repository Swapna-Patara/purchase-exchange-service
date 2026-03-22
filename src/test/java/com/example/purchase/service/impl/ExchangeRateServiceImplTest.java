package com.example.purchase.service.impl;

import com.example.purchase.config.ExchangeProperties;
import com.example.purchase.dto.TreasuryData;
import com.example.purchase.dto.TreasuryResponse;
import com.example.purchase.exception.ExchangeRateNotFoundException;
import com.example.purchase.service.IExchangeRateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExchangeRateServiceImplTest {

    private RestTemplate restTemplate;
    private ExchangeProperties props;
    private IExchangeRateService service;

    @BeforeEach
    void setUp() {
        restTemplate = mock(RestTemplate.class);
        props = mock(ExchangeProperties.class);

        // Provide configuration
        when(props.getMaxMonths()).thenReturn(6);
        when(props.getPageSize()).thenReturn(50);
        when(props.getMaxIterations()).thenReturn(5);
        when(props.getBaseUrl()).thenReturn("http://example.com/api");

        service = new ExchangeRateServiceImpl(restTemplate, props);
    }

    @Test
    void testGetRate_success() {
        String country = "USA";
        String currency = "EUR";
        LocalDate purchaseDate = LocalDate.of(2026, 3, 22);

        TreasuryData data = new TreasuryData();
        data.setRecordDate("2026-03-20");
        data.setExchangeRate("1.2345");

        TreasuryResponse treasuryResponse = new TreasuryResponse();
        treasuryResponse.setData(List.of(data));
        treasuryResponse.setLinks(null); // no next page

        when(restTemplate.getForEntity(ArgumentMatchers.any(), ArgumentMatchers.eq(TreasuryResponse.class)))
                .thenReturn(ResponseEntity.ok(treasuryResponse));

        BigDecimal rate = service.getRate(country, currency, purchaseDate);

        assertEquals(new BigDecimal("1.2345"), rate);
        verify(restTemplate, atLeastOnce()).getForEntity(any(), eq(TreasuryResponse.class));
    }

    @Test
    void testGetRate_noRates_throwsException() {
        String country = "USA";
        String currency = "EUR";
        LocalDate purchaseDate = LocalDate.of(2026, 3, 22);

        TreasuryResponse emptyResponse = new TreasuryResponse();
        emptyResponse.setData(Collections.emptyList());
        emptyResponse.setLinks(null);

        when(restTemplate.getForEntity(any(), eq(TreasuryResponse.class)))
                .thenReturn(ResponseEntity.ok(emptyResponse));

        ExchangeRateNotFoundException ex = assertThrows(ExchangeRateNotFoundException.class,
                () -> service.getRate(country, currency, purchaseDate));

        assertTrue(ex.getMessage().contains("No exchange rate found"));
    }

    @Test
    void testGetRate_withPagination_stopsAtCutoff() {
        String country = "USA";
        String currency = "EUR";
        LocalDate purchaseDate = LocalDate.of(2026, 3, 22);

        TreasuryData oldData = new TreasuryData();
        oldData.setRecordDate("2025-01-01"); // older than cutoff
        oldData.setExchangeRate("1.0");

        TreasuryResponse responsePage1 = new TreasuryResponse();
        responsePage1.setData(List.of(oldData));
        responsePage1.setLinks(null); // no next page

        when(restTemplate.getForEntity(any(), eq(TreasuryResponse.class)))
                .thenReturn(ResponseEntity.ok(responsePage1));

        ExchangeRateNotFoundException ex = assertThrows(ExchangeRateNotFoundException.class,
                () -> service.getRate(country, currency, purchaseDate));

        assertTrue(ex.getMessage().contains("No exchange rate found"));
        verify(restTemplate, atLeastOnce()).getForEntity(any(), eq(TreasuryResponse.class));
    }
}