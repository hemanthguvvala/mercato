package com.interview.catalogservice.service;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

import com.interview.catalogservice.entities.ProductEntity;
import com.interview.catalogservice.repository.ProductRepository;
import com.interview.catalogservice.web.Product;

@Service
public class ProductService {

	private final ProductRepository repository;

	public ProductService(ProductRepository productRepository) {
		this.repository = productRepository;
	}

	public Optional<Product> findById(Long id) {
		return repository.findById(id).map(this::toDto);
	}

	public List<Product> search(String name) {
		return repository.findByNameIgnoreCase(name).stream().map(this::toDto).toList();
	}

	public Product create(Product productRequest) {
		ProductEntity saved = repository.save(new ProductEntity(productRequest.name(), productRequest.price()));
		return toDto(saved);
	}

	public Product update(Long id, Product request) {
		ProductEntity entity = new ProductEntity(id, request.name(), request.price(), request.version());
		return toDto(repository.save(entity));
	}

	public void delete(Long id) {
		repository.deleteById(id);
	}

	public Page<Product> page(Pageable pageable) {
		return repository.findAll(pageable).map(this::toDto);
	}

	public Slice<Product> findBy(Pageable pageable) {
		return repository.findAllSliced(pageable).map(this::toDto);
	}

	private Product toDto(ProductEntity productEntity) {
		return new Product(productEntity.getId(), productEntity.getName(), productEntity.getPrice(),
				productEntity.getVersion());
	}

}
