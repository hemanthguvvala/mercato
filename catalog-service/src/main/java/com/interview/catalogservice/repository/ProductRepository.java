package com.interview.catalogservice.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.interview.catalogservice.entities.ProductEntity;

@Repository
public interface ProductRepository extends JpaRepository<ProductEntity, Long> {

	List<ProductEntity> findByNameIgnoreCase(String name);

	@Query("select p from ProductEntity p")
	Slice<ProductEntity> findAllSliced(Pageable pageable);

	@Query("select distinct p from ProductEntity p left join fetch p.categories pc left join fetch pc.category "
			+ "where p.id = :id")
	Optional<ProductEntity> findWithCategoriesById(@Param("id") Long id);
}
