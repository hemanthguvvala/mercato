package com.interview.catalogservice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.interview.catalogservice.entities.ProductCategory;
import com.interview.catalogservice.entities.ProductCategoryId;

@Repository
public interface ProductCategoryRepository extends JpaRepository<ProductCategory, ProductCategoryId> {

	List<ProductCategory> findByCategory_Id(Long categoryId);

	List<ProductCategory> findByProduct_Id(Long productId);
}
