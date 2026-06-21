package com.interview.orderservice.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.interview.orderservice.aspect.Audited;
import com.interview.orderservice.entity.ProductEntity;
import com.interview.orderservice.repository.ProductRepository;
import com.interview.orderservice.web.Product;

@Service
public class ProductService {

	private final ProductRepository repository;
	private final AuditLogService auditLogService;

	public ProductService(ProductRepository productRepository, AuditLogService auditLogService) {
		this.repository = productRepository;
		this.auditLogService = auditLogService;
	}

	public Optional<Product> findById(Long id) {
		return repository.findById(id).map(this::toDto);
	}

	public List<Product> search(String name) {
		return repository.findByNameIgnoreCase(name).stream().map(this::toDto).toList();
	}

	@Audited(action = "CREATE_PRODUCT")
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

	@Transactional(rollbackFor = Exception.class)
	public void createThenFailChecked(Product request) throws BusinessException {
		auditLogService.log("ATTEMPT_CREATE " + request.name());
		repository.save(new ProductEntity(request.name(), request.price()));
		throw new BusinessException("Simulated Failure After insert");
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
