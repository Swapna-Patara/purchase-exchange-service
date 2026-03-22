package com.example.purchase.service.impl;

import com.example.purchase.dto.PurchaseRequest;
import com.example.purchase.dto.PurchaseResponse;
import com.example.purchase.exception.ResourceNotFoundException;
import com.example.purchase.model.Purchase;
import com.example.purchase.repository.PurchaseRepository;
import com.example.purchase.service.IExchangeRateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PurchaseServiceImplTest {

    private PurchaseRepository repo;
    private IExchangeRateService exchangeService;
    private PurchaseServiceImpl service;

    @BeforeEach
    void setUp() {
        repo = mock(PurchaseRepository.class);
        exchangeService = mock(IExchangeRateService.class);
        service = new PurchaseServiceImpl(repo, exchangeService);
    }

    @Test
    void testSavePurchase_success() {
        PurchaseRequest request = new PurchaseRequest();
        request.setDescription("Test Purchase");
        request.setTransactionDate(LocalDate.of(2026, 3, 22));
        request.setAmount(new BigDecimal("123.456"));

        Purchase savedPurchase = new Purchase();
        savedPurchase.setId("1");
        savedPurchase.setDescription(request.getDescription());
        savedPurchase.setTransactionDate(request.getTransactionDate());
        savedPurchase.setAmount(new BigDecimal("123.46")); // Rounded

        when(repo.save(any(Purchase.class))).thenReturn(savedPurchase);

        Purchase result = service.save(request);

        // Verify repository save called with properly rounded amount
        ArgumentCaptor<Purchase> captor = ArgumentCaptor.forClass(Purchase.class);
        verify(repo, times(1)).save(captor.capture());
        assertEquals(new BigDecimal("123.46"), captor.getValue().getAmount());

        // Verify returned entity
        assertNotNull(result);
        assertEquals("1", result.getId());
        assertEquals("Test Purchase", result.getDescription());
        assertEquals(new BigDecimal("123.46"), result.getAmount());
    }

    @Test
    void testConvert_success() {
        String purchaseId = "1";
        String currency = "EUR";
        String country = "Germany";

        Purchase purchase = new Purchase();
        purchase.setId(purchaseId);
        purchase.setDescription("Test Purchase");
        purchase.setTransactionDate(LocalDate.of(2026, 3, 22));
        purchase.setAmount(new BigDecimal("100.00"));

        when(repo.findById(purchaseId)).thenReturn(Optional.of(purchase));
        when(exchangeService.getRate(country, currency, purchase.getTransactionDate()))
                .thenReturn(new BigDecimal("1.2345"));

        PurchaseResponse response = service.convert(purchaseId, currency, country);

        // Verify converted amount
        BigDecimal expectedConverted = new BigDecimal("123.45"); // 100 * 1.2345 rounded
        assertEquals(expectedConverted, response.getConvertedAmount());

        // Verify exchange rate
        assertEquals(new BigDecimal("1.2345"), response.getExchangeRate());

        // Verify other fields
        assertEquals(purchaseId, response.getId());
        assertEquals(purchase.getDescription(), response.getDescription());
        assertEquals(purchase.getTransactionDate(), response.getTransactionDate());
        assertEquals(purchase.getAmount(), response.getOriginalAmount());
    }

    @Test
    void testConvert_purchaseNotFound_throwsException() {
        String purchaseId = "unknown";
        when(repo.findById(purchaseId)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(ResourceNotFoundException.class,
                () -> service.convert(purchaseId, "USD", "USA"));

        assertTrue(exception.getMessage().contains("Purchase not found with id"));
    }
}