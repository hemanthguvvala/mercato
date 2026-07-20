package com.interview.orderservice.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.interview.orderservice.entity.OrderView;

@Repository
public interface OrderViewRepository extends JpaRepository<OrderView, Long> {

	Page<OrderView> findByCustomerName(String customerName, Pageable pageable);

	Optional<OrderView> findByOrderIdAndCustomerName(Long orderId, String customerName);
}
