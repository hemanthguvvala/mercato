package com.interview.orderservice.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.interview.orderservice.client.CatalogClient;
import com.interview.orderservice.client.CatalogProduct;
import com.interview.orderservice.entity.OrderEntity;
import com.interview.orderservice.entity.OrderItem;
import com.interview.orderservice.repository.OrderRepository;
import com.interview.orderservice.web.OrderDtos.CreateOrderRequest;
import com.interview.orderservice.web.OrderDtos.OrderResponse;
import com.interview.orderservice.web.ResourceNotFoundException;

import feign.FeignException;

@Service
public class OrderService {

	private final OrderRepository orderRepository;
	private final CatalogClient catalogClient;

	public OrderService(OrderRepository orderRepository, CatalogClient catalogClient) {
		this.catalogClient = catalogClient;
		this.orderRepository = orderRepository;
	}

	@Transactional
	public OrderResponse create(CreateOrderRequest request) {
		OrderEntity order = new OrderEntity(request.customerName());
		for (CreateOrderRequest.Line line : request.lines()) {
			CatalogProduct product;
			try {
				product = catalogClient.getProduct(line.productId());
			} catch (FeignException.NotFound e) {
				throw new ResourceNotFoundException("Product Not Found : - " + line.productId());
			}
			order.addItem(new OrderItem(product.id(), product.name(), product.price(), line.quantity()));
		}
		return toResponse(orderRepository.save(order));
	}

	@Transactional(readOnly = true)
	public List<OrderResponse> findAll() {
		return orderRepository.findAllWithItems().stream().map(this::toResponse).toList();
	}

	@Transactional(readOnly = true)
	public int itemCountBad(Long orderId) {
		OrderEntity item = orderRepository.findById(orderId).orElseThrow();
		return item.getItems().size();
	}

	private OrderResponse toResponse(OrderEntity o) {
		List<OrderResponse.Item> items = o.getItems().stream().map(
				i -> new OrderResponse.Item(i.getProductId(), i.getProductName(), i.getUnitPrice(), i.getQuantity()))
				.toList();
		return new OrderResponse(o.getId(), o.getCustomerName(), items);
	}
}
