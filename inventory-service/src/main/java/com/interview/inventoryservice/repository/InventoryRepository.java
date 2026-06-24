package com.interview.inventoryservice.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.interview.inventoryservice.entity.InventoryItem;

import jakarta.persistence.LockModeType;

@Repository
public interface InventoryRepository extends JpaRepository<InventoryItem, Long> {

	Optional<InventoryItem> findByProductId(Long id);
	
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select i from InventoryItem i where i.productId = :productId ")
	Optional<InventoryItem> findByProductIdForUpdate(@Param("productId") Long id);
}
