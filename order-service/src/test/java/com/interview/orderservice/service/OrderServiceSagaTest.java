package com.interview.orderservice.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.interview.orderservice.client.catalog.CatalogGateway;
import com.interview.orderservice.client.catalog.CatalogProduct;
import com.interview.orderservice.client.inventory.InventoryClient;
import com.interview.orderservice.client.inventory.StockRequest;
import com.interview.orderservice.client.payment.ChargeRequest;
import com.interview.orderservice.client.payment.PaymentWebClient;
import com.interview.orderservice.entity.OrderEntity;
import com.interview.orderservice.repository.OrderRepository;
import com.interview.orderservice.web.OrderDtos.CreateOrderRequest;
import com.interview.orderservice.web.OrderFailedException;

/**
 * Unit tests for the order saga in {@link OrderService} — collaborators mocked, no Spring/DB/Kafka.
 * Proves the compensation path: a downstream failure must release reserved stock, mark the order
 * FAILED, and never confirm.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceSagaTest {

	@Mock
	OrderRepository orderRepository;
	@Mock
	CatalogGateway catalogGateway;
	@Mock
	PaymentWebClient paymentWebClient;
	@Mock
	InventoryClient inventoryClient;
	@Mock
	OrderPersistence orderPersistence;

	@InjectMocks
	OrderService orderService;

	private CreateOrderRequest request;

	@BeforeEach
	void setUp() {
		// one line: product 1, qty 2  ->  total = 499.0 * 2 = 998.0
		request = new CreateOrderRequest("Hemanth", List.of(new CreateOrderRequest.Line(1L, 2)));

		when(catalogGateway.getProduct(1L)).thenReturn(new CatalogProduct(1L, "Phone", 499.0, 0L));

		// savePending returns the same order (items already added) with a generated id stamped on
		when(orderPersistence.savePending(any(OrderEntity.class))).thenAnswer(inv -> {
			OrderEntity o = inv.getArgument(0);
			ReflectionTestUtils.setField(o, "id", 100L);
			return o;
		});
	}

	@Test
	void happyPath_confirmsOrder_andNeverCompensates() {
		orderService.create(request);

		verify(inventoryClient).reserve(new StockRequest(1L, 2));
		verify(paymentWebClient).charge(new ChargeRequest(100L, 998.0));
		verify(orderPersistence).confirm(100L, 998.0);

		verify(orderPersistence, never()).fail(anyLong());
		verify(inventoryClient, never()).release(any());
		verify(paymentWebClient, never()).refund(any());
	}

	@Test
	void chargeFails_releasesStock_failsOrder_refunds_andThrows() {
		doThrow(WebClientResponseException.create(500, "Server Error", HttpHeaders.EMPTY, new byte[0], null))
				.when(paymentWebClient).charge(any(ChargeRequest.class));

		assertThatThrownBy(() -> orderService.create(request))
				.isInstanceOf(OrderFailedException.class);

		// compensation ran:
		verify(inventoryClient).release(new StockRequest(1L, 2));        // reserved stock released
		verify(orderPersistence).fail(100L);                            // order marked FAILED
		verify(paymentWebClient).refund(new ChargeRequest(100L, 998.0)); // defensive refund (charge was attempted)

		// and it did NOT confirm the order
		verify(orderPersistence, never()).confirm(anyLong(), anyDouble());
	}
}
