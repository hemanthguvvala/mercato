package com.interview.orderservice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.interview.orderservice.entity.OrderEntity;

@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, Long> {

	@Query("""
			select distinct o from OrderEntity o
			left join fetch o.items 
			""")
	List<OrderEntity> findAllWithItems();
	
	@Override
	@EntityGraph(attributePaths = {"items"})
	List<OrderEntity> findAll();
}
