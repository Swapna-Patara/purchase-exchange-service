package com.example.purchase.repository;

import com.example.purchase.model.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for Purchase entity.
 * Provides CRUD operations and custom query methods for Purchase data access.
 */
@Repository
public interface PurchaseRepository extends JpaRepository<Purchase, String> {
}
