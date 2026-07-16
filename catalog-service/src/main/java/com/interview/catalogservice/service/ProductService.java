package com.interview.catalogservice.service;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.interview.catalogservice.entities.Category;
import com.interview.catalogservice.entities.Dimensions;
import com.interview.catalogservice.entities.ProductDetail;
import com.interview.catalogservice.entities.ProductEntity;
import com.interview.catalogservice.repository.CategoryRepository;
import com.interview.catalogservice.repository.ProductDetailRepository;
import com.interview.catalogservice.repository.ProductRepository;
import com.interview.catalogservice.repository.ProductSpecifications;
import com.interview.catalogservice.web.Product;
import com.interview.catalogservice.web.ProductSearchCriteria;
import com.interview.catalogservice.web.ProductWithCategories;

@Service
public class ProductService {

	private final ProductRepository repository;
	private final CategoryRepository categoryRepository;
	private final ProductDetailRepository productDetailRepository;

	public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository,
			ProductDetailRepository productDetailRepository) {
		this.repository = productRepository;
		this.categoryRepository = categoryRepository;
		this.productDetailRepository = productDetailRepository;
	}

	@Cacheable(value = "products", key = "#id")
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

	@CacheEvict(value = "products", key = "#id")
	public Product update(Long id, Product request) {
		ProductEntity entity = new ProductEntity(id, request.name(), request.price(), request.version());
		return toDto(repository.save(entity));
	}

	@CacheEvict(value = "products", key = "#id")
	public void delete(Long id) {
		repository.deleteById(id);
	}

	@Transactional
	public void assignCategory(Long productId, Long categoryId, int position) {
		ProductEntity product = repository.findById(productId)
				.orElseThrow(() -> new IllegalArgumentException("No product " + productId));
		Category category = categoryRepository.findById(categoryId)
				.orElseThrow(() -> new IllegalArgumentException("No category " + categoryId));
		product.addCategory(category, position);
	}

	@Transactional
	public void upsertDetail(Long productId, String description) {
		ProductEntity product = repository.findById(productId)
				.orElseThrow(() -> new IllegalArgumentException("No product " + productId));
		ProductDetail productDetail = productDetailRepository.findById(productId)
				.orElseGet(() -> new ProductDetail(product, description));
		productDetail.setDescription(description);
		productDetailRepository.save(productDetail);
	}

	@Transactional
	public void addTags(Long productId, Set<String> tags) {
		ProductEntity product = repository.findById(productId)
				.orElseThrow(() -> new IllegalArgumentException("No product " + productId));
		product.getTags().addAll(tags);
	}

	@Transactional
	public void setDimensions(Long productId, Dimensions dimensions) {
		ProductDetail detail = productDetailRepository.findById(productId)
				.orElseThrow(() -> new IllegalArgumentException("No product " + productId));
		detail.setDimensions(dimensions);
	}

	@Transactional(readOnly = true)
	public Optional<ProductWithCategories> getWithCategories(Long id) {
		return repository.findWithCategoriesById(id).map(this::toWithCategories);
	}

	private ProductWithCategories toWithCategories(ProductEntity p) {
		List<ProductWithCategories.CategoryView> cats = p.getCategories().stream()
				.map(pc -> new ProductWithCategories.CategoryView(pc.getCategory().getId(), pc.getCategory().getName(),
						pc.getPosition()))
				.toList();
		return new ProductWithCategories(p.getId(), p.getName(), p.getPrice(), cats);
	}
	
	@Transactional(readOnly = true)
	public List<Product> search(ProductSearchCriteria criteria) {
		Specification<ProductEntity> spec = Specification.where(null);
		if (criteria.name() != null && !criteria.name().isBlank()) {
			spec = spec.and(ProductSpecifications.nameContains(criteria.name()));
		}
		if (criteria.minPrice() != null) {
			spec = spec.and(ProductSpecifications.priceAtLeast(criteria.minPrice()));
		}
		if (criteria.maxPrice() != null) {
			spec = spec.and(ProductSpecifications.priceAtMost(criteria.maxPrice()));
		}
		if (criteria.categoryId() != null) {
			spec = spec.and(ProductSpecifications.inCategory(criteria.categoryId()));
		}

		return repository.findAll(spec).stream().map(this::toDto).toList();
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
