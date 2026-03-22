package com.example.purchase.service.impl;

import com.example.purchase.dto.PurchaseRequest;
import com.example.purchase.dto.PurchaseResponse;
import com.example.purchase.exception.ResourceNotFoundException;
import com.example.purchase.model.Purchase;
import com.example.purchase.repository.PurchaseRepository;
import com.example.purchase.service.IExchangeRateService;
import com.example.purchase.service.IPurchaseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;


@Service
@Transactional
public class PurchaseServiceImpl implements IPurchaseService {

    private static final Logger logger = LoggerFactory.getLogger(PurchaseServiceImpl.class);

    private final PurchaseRepository repo;
    private final IExchangeRateService exchangeService;


    public PurchaseServiceImpl(PurchaseRepository repo, 
                               IExchangeRateService exchangeService) {
        this.repo = repo;
        this.exchangeService = exchangeService;
     }

    @Override
    public Purchase save(PurchaseRequest req) {
        logger.info("Creating purchase");
        Purchase purchase = buildPurchase(req);
        return repo.save(purchase);
    }

    @Override
    public PurchaseResponse convert(String id, String currency, String country) {
        Purchase purchase = findPurchaseById(id);

        logger.info("Converting purchase {} from amount {} to currency {}",
                id, purchase.getAmount(), currency);

        BigDecimal exchangeRate = exchangeService.getRate(country, currency, purchase.getTransactionDate());
        BigDecimal convertedAmount = calculateConvertedAmount(purchase.getAmount(), exchangeRate);

        logger.info("Successfully converted purchase {} with exchange rate {}: {} USD → {} {}",
                id, exchangeRate, purchase.getAmount(), convertedAmount, currency);

        return buildPurchaseResponse(purchase, exchangeRate, convertedAmount);
    }

    /**
     * Finds a purchase by ID.
     *
     * @param id the purchase ID
     * @return the purchase entity
     * @throws ResourceNotFoundException if purchase not found
     */
    private Purchase findPurchaseById(String id) {
        return repo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase not found with id: " + id));
    }

    /**
     * Builds a Purchase entity from a request.
     *
     * @param request the purchase request
     * @return the purchase entity
     */
    private Purchase buildPurchase(PurchaseRequest request) {
        Purchase purchase = new Purchase();
        purchase.setDescription(request.getDescription());
        purchase.setTransactionDate(request.getTransactionDate());
        purchase.setAmount(request.getAmount().setScale(2, RoundingMode.HALF_UP));
        return purchase;
    }

    /**
     * Calculates the converted amount.
     *
     * @param originalAmount the original amount
     * @param exchangeRate   the exchange rate
     * @return the converted amount rounded to 2 decimal places
     */
    private BigDecimal calculateConvertedAmount(BigDecimal originalAmount, BigDecimal exchangeRate) {
        return originalAmount
                .multiply(exchangeRate)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Builds a PurchaseResponse.
     *
     * @param purchase        the purchase entity
     * @param exchangeRate    the exchange rate
     * @param convertedAmount the converted amount
     * @return the purchase response DTO
     */
    private PurchaseResponse buildPurchaseResponse(Purchase purchase, BigDecimal exchangeRate, BigDecimal convertedAmount) {
        return new PurchaseResponse(
                purchase.getId(),
                purchase.getDescription(),
                purchase.getTransactionDate(),
                purchase.getAmount(),
                exchangeRate,
                convertedAmount
        );
    }
}
