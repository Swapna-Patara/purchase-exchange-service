package com.example.purchase.controller;

import com.example.purchase.dto.PurchaseRequest;
import com.example.purchase.dto.PurchaseResponse;
import com.example.purchase.model.Purchase;
import com.example.purchase.service.IPurchaseService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PurchaseController.class)
class PurchaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IPurchaseService purchaseService;

    // Configure ObjectMapper for LocalDate serialization
    private ObjectMapper objectMapper;

    private PurchaseRequest request;
    private Purchase purchase;
    private PurchaseResponse response;

    @BeforeEach
    void setUp() {
        // Configure ObjectMapper to handle Java 8 dates
        objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Sample request
        request = new PurchaseRequest();
        request.setDescription("Office Supplies");
        request.setTransactionDate(LocalDate.of(2026, 3, 22));
        request.setAmount(new BigDecimal("123.45"));

        // Sample saved Purchase
        purchase = new Purchase();
        purchase.setId("1");
        purchase.setDescription(request.getDescription());
        purchase.setTransactionDate(request.getTransactionDate());
        purchase.setAmount(request.getAmount());

        // Sample converted PurchaseResponse
        response = new PurchaseResponse(
                purchase.getId(),
                purchase.getDescription(),
                purchase.getTransactionDate(),
                purchase.getAmount(),
                new BigDecimal("1.2"),
                new BigDecimal("148.14")
        );
    }

    @Test
    void testCreatePurchase_success() throws Exception {
        Mockito.when(purchaseService.save(any(PurchaseRequest.class))).thenReturn(purchase);

        mockMvc.perform(post("/purchases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is("1")))
                .andExpect(jsonPath("$.description", is("Office Supplies")))
                .andExpect(jsonPath("$.amount", is(123.45)));
    }

    @Test
    void testCreatePurchase_validationError_missingAmount() throws Exception {
        request.setAmount(null); // missing required field

        mockMvc.perform(post("/purchases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors", notNullValue()));
    }

    @Test
    void testConvertPurchase_success() throws Exception {
        Mockito.when(purchaseService.convert(eq("1"), eq("EUR"), eq("Germany"))).thenReturn(response);

        mockMvc.perform(get("/purchases/1/convert")
                        .param("currency", "EUR")
                        .param("country", "Germany"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is("1")))
                .andExpect(jsonPath("$.convertedAmount", is(148.14)))
                .andExpect(jsonPath("$.exchangeRate", is(1.2)));
    }

    @Test
    void testConvertPurchase_missingCurrencyParam() throws Exception {
        mockMvc.perform(get("/purchases/1/convert")
                        .param("country", "Germany"))
                .andExpect(status().isInternalServerError());

    }

    @Test
    void testConvertPurchase_missingCountryParam() throws Exception {
        mockMvc.perform(get("/purchases/1/convert")
                        .param("currency", "EUR"))
                .andExpect(status().isInternalServerError());

    }
}