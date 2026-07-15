package com.interview.inventoryservice.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.interview.inventoryservice.entity.StockReservation;

@Repository
public interface StockReservationRepository extends JpaRepository<StockReservation, Long> {

	Optional<StockReservation> findByOrderIdAndProductId(Long orderId, Long productId);
}
