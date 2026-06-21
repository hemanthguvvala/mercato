package com.interview.catalogservice.repository;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.interview.catalogservice.entities.ProductEntity;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, Long> {

	List<ProductEntity> findByNameIgnoreCase(String name);

	@Query("select p from ProductEntity p")
	Slice<ProductEntity> findAllSliced(Pageable pageable);
}
