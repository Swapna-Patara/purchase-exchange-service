package com.example.purchase.integration;

import com.example.purchase.dto.PurchaseRequest;
import com.example.purchase.dto.PurchaseResponse;
import com.example.purchase.model.Purchase;
import com.example.purchase.repository.PurchaseRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for Purchase Exchange Service.
 * Tests the complete flow of creating and converting purchases with real database.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Purchase Integration Tests")
class PurchaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PurchaseRepository purchaseRepository;

    private ObjectMapper objectMapper;
    private PurchaseRequest validRequest;

    @BeforeEach
    void setUp() {
        // Clear repository before each test
        purchaseRepository.deleteAll();

        // Create and configure ObjectMapper
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // Create valid purchase request
        validRequest = new PurchaseRequest();
        validRequest.setDescription("Laptop");
        validRequest.setTransactionDate(LocalDate.of(2024, 1, 15));
        validRequest.setAmount(new BigDecimal("1500.50"));
    }

    // ============== CREATE PURCHASE TESTS ==============

    @Test
    @DisplayName("Should create purchase successfully with valid data")
    void testCreatePurchase_Success() throws Exception {
        // When
        MvcResult result = mockMvc.perform(post("/purchases")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.description").value("Laptop"))
                .andExpect(jsonPath("$.transactionDate").value("2024-01-15"))
                .andExpect(jsonPath("$.amount").value(1500.50))
                .andReturn();

        // Then - Verify purchase is saved in database
        assertEquals(1, purchaseRepository.count());
        
        // Extract ID from response and verify in DB
        String responseBody = result.getResponse().getContentAsString();
        String purchaseId = objectMapper.readTree(responseBody).get("id").asText();
        assertTrue(purchaseRepository.existsById(purchaseId));
    }

    @Test
    @DisplayName("Should fail to create purchase with description exceeding 50 characters")
    void testCreatePurchase_DescriptionTooLong() throws Exception {
        // Given - Description longer than 50 characters
        validRequest.setDescription("This is a very long description that definitely exceeds fifty characters limit");

        // When & Then
        mockMvc.perform(post("/purchases")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest());

        // Verify purchase was not saved
        assertEquals(0, purchaseRepository.count());
    }

    @Test
    @DisplayName("Should fail to create purchase with null description")
    void testCreatePurchase_NullDescription() throws Exception {
        // Given
        validRequest.setDescription(null);

        // When & Then
        mockMvc.perform(post("/purchases")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest());

        assertEquals(0, purchaseRepository.count());
    }

    @Test
    @DisplayName("Should fail to create purchase with invalid transaction date format")
    void testCreatePurchase_InvalidDateFormat() throws Exception {
        // Given - Manually create JSON with invalid date
        String jsonRequest = """
                {
                    "description": "Laptop",
                    "transactionDate": "invalid-date",
                    "amount": 1500.50
                }
                """;

        // When & Then
        mockMvc.perform(post("/purchases")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonRequest))
                .andExpect(status().isInternalServerError());

        assertEquals(0, purchaseRepository.count());
    }

    @Test
    @DisplayName("Should fail to create purchase with negative amount")
    void testCreatePurchase_NegativeAmount() throws Exception {
        // Given
        validRequest.setAmount(new BigDecimal("-100.00"));

        // When & Then
        mockMvc.perform(post("/purchases")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest());

        assertEquals(0, purchaseRepository.count());
    }

    @Test
    @DisplayName("Should fail to create purchase with zero amount")
    void testCreatePurchase_ZeroAmount() throws Exception {
        // Given
        validRequest.setAmount(new BigDecimal("0.00"));

        // When & Then
        mockMvc.perform(post("/purchases")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest());

        assertEquals(0, purchaseRepository.count());
    }

    @Test
    @DisplayName("Should create purchase with proper decimal rounding")
    void testCreatePurchase_ProperDecimalHandling() throws Exception {
        // Given
        validRequest.setAmount(new BigDecimal("1500.505")); // 3 decimal places

        // When
        MvcResult result = mockMvc.perform(post("/purchases")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        // Then - Amount should be rounded to 2 decimal places
        String responseBody = result.getResponse().getContentAsString();
        String purchaseId = objectMapper.readTree(responseBody).get("id").asText();
        Purchase purchase = purchaseRepository.findById(purchaseId).orElseThrow();
        assertEquals(new BigDecimal("1500.51"), purchase.getAmount());
    }

    // ============== CONVERT CURRENCY TESTS ==============

    @Test
    @DisplayName("Should convert purchase to Euro successfully")
    void testConvertPurchase_ToEuro_Success() throws Exception {
        // Given - Create a purchase first
        Purchase purchase = createTestPurchase("Book", LocalDate.of(2024, 1, 15), "100.00");

        // When
        MvcResult result = mockMvc.perform(get("/purchases/{id}/convert", purchase.getId())
                .param("currency", "Euro")
                .param("country", "Euro Zone"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(purchase.getId()))
                .andExpect(jsonPath("$.description").value("Book"))
                .andExpect(jsonPath("$.transactionDate").value("2024-01-15"))
                .andExpect(jsonPath("$.originalAmount").value(100.00))
                .andExpect(jsonPath("$.exchangeRate").isNotEmpty())
                .andExpect(jsonPath("$.convertedAmount").isNotEmpty())
                .andReturn();

        // Then - Verify response structure
        String responseBody = result.getResponse().getContentAsString();
        PurchaseResponse response = objectMapper.readValue(responseBody, PurchaseResponse.class);
        assertNotNull(response.getExchangeRate());
        assertNotNull(response.getConvertedAmount());
        assertTrue(response.getConvertedAmount().signum() > 0);
    }

    @Test
    @DisplayName("Should convert purchase to GBP successfully")
    void testConvertPurchase_ToGBP_Success() throws Exception {
        // Given
        Purchase purchase = createTestPurchase("Smartphone", LocalDate.of(2024, 1, 15), "500.00");

        // When
        mockMvc.perform(get("/purchases/{id}/convert", purchase.getId())
                .param("currency", "Pound")
                .param("country", "United Kingdom"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(purchase.getId()))
                .andExpect(jsonPath("$.convertedAmount").isNotEmpty())
                .andExpect(jsonPath("$.exchangeRate").isNotEmpty());
    }

    @Test
    @DisplayName("Should fail to convert with missing currency parameter")
    void testConvertPurchase_MissingCurrency() throws Exception {
        // Given
        Purchase purchase = createTestPurchase("Book", LocalDate.of(2024, 1, 15), "100.00");

        // When & Then
        mockMvc.perform(get("/purchases/{id}/convert", purchase.getId())
                .param("country", "Euro Zone"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("Should fail to convert with missing country parameter")
    void testConvertPurchase_MissingCountry() throws Exception {
        // Given
        Purchase purchase = createTestPurchase("Book", LocalDate.of(2024, 1, 15), "100.00");

        // When & Then
        mockMvc.perform(get("/purchases/{id}/convert", purchase.getId())
                .param("currency", "Euro"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("Should fail to convert non-existent purchase")
    void testConvertPurchase_NotFound() throws Exception {
        // When & Then
        mockMvc.perform(get("/purchases/{id}/convert", "non-existent-id")
                .param("currency", "Euro")
                .param("country", "Euro Zone"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should fail to convert with blank currency parameter")
    void testConvertPurchase_BlankCurrency() throws Exception {
        // Given
        Purchase purchase = createTestPurchase("Book", LocalDate.of(2024, 1, 15), "100.00");

        // When & Then
        mockMvc.perform(get("/purchases/{id}/convert", purchase.getId())
                .param("currency", "   ")
                .param("country", "Euro Zone"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("Should fail to convert with blank country parameter")
    void testConvertPurchase_BlankCountry() throws Exception {
        // Given
        Purchase purchase = createTestPurchase("Book", LocalDate.of(2024, 1, 15), "100.00");

        // When & Then
        mockMvc.perform(get("/purchases/{id}/convert", purchase.getId())
                .param("currency", "Euro")
                .param("country", "   "))
                .andExpect(status().isInternalServerError());
    }

    // ============== END-TO-END WORKFLOW TESTS ==============

    @Test
    @DisplayName("Should create purchase and then convert to multiple currencies")
    void testEndToEnd_CreateAndConvertMultipleCurrencies() throws Exception {
        // Given & When - Create purchase
        MvcResult createResult = mockMvc.perform(post("/purchases")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        String responseBody = createResult.getResponse().getContentAsString();
        String purchaseId = objectMapper.readTree(responseBody).get("id").asText();

        // Then - Convert to Euro
        mockMvc.perform(get("/purchases/{id}/convert", purchaseId)
                .param("currency", "Euro")
                .param("country", "Euro Zone"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(purchaseId));

        // Then - Convert to GBP
        mockMvc.perform(get("/purchases/{id}/convert", purchaseId)
                .param("currency", "Pound")
                .param("country", "United Kingdom"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(purchaseId));

        // Then - Convert to Japanese Yen
        mockMvc.perform(get("/purchases/{id}/convert", purchaseId)
                .param("currency", "Yen")
                .param("country", "Japan"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(purchaseId));

        // Verify purchase still exists in DB
        assertTrue(purchaseRepository.existsById(purchaseId));
    }

    @Test
    @DisplayName("Should create multiple purchases and verify database persistence")
    void testCreateMultiplePurchases() throws Exception {
        // Given
        PurchaseRequest request1 = createPurchaseRequest("Laptop", "2024-01-15", "1500.00");
        PurchaseRequest request2 = createPurchaseRequest("Mouse", "2024-02-20", "50.00");
        PurchaseRequest request3 = createPurchaseRequest("Keyboard", "2024-03-10", "150.00");

        // When
        mockMvc.perform(post("/purchases")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/purchases")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/purchases")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request3)))
                .andExpect(status().isCreated());

        // Then
        assertEquals(3, purchaseRepository.count());
    }

    // ============== HELPER METHODS ==============

    /**
     * Creates a test purchase in the database.
     */
    private Purchase createTestPurchase(String description, LocalDate date, String amount) {
        Purchase purchase = new Purchase();
        purchase.setDescription(description);
        purchase.setTransactionDate(date);
        purchase.setAmount(new BigDecimal(amount));
        return purchaseRepository.save(purchase);
    }

    /**
     * Creates a purchase request with given parameters.
     */
    private PurchaseRequest createPurchaseRequest(String description, String date, String amount) {
        PurchaseRequest request = new PurchaseRequest();
        request.setDescription(description);
        request.setTransactionDate(LocalDate.parse(date));
        request.setAmount(new BigDecimal(amount));
        return request;
    }
}



