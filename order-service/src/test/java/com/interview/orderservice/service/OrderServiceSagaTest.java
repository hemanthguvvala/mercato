package com.interview.orderservice.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
 * The reserves now fan out on an Executor; the test injects a SYNCHRONOUS executor (Runnable::run)
 * so the parallel path runs deterministically on the calling thread.
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

	OrderService orderService;

	@BeforeEach
	void setUp() {
		// synchronous executor: CompletableFuture.runAsync runs each reserve on the calling thread
		orderService = new OrderService(orderRepository, catalogGateway, paymentWebClient,
				inventoryClient, orderPersistence, Runnable::run);

		// savePending returns the same order with a generated id (100) stamped on
		when(orderPersistence.savePending(any(OrderEntity.class))).thenAnswer(inv -> {
			OrderEntity o = inv.getArgument(0);
			ReflectionTestUtils.setField(o, "id", 100L);
			return o;
		});
	}

	@Test
	void happyPath_confirmsOrder_andNeverCompensates() {
		when(catalogGateway.getProduct(1L)).thenReturn(new CatalogProduct(1L, "Phone", 499.0, 0L));
		CreateOrderRequest request = new CreateOrderRequest("Hemanth", List.of(new CreateOrderRequest.Line(1L, 2)));

		orderService.create(request);

		verify(inventoryClient).reserve(new StockRequest(100L, 1L, 2));
		verify(paymentWebClient).charge(new ChargeRequest(100L, 998.0));
		verify(orderPersistence).confirm(100L, 998.0);

		verify(orderPersistence, never()).fail(anyLong());
		verify(inventoryClient, never()).release(any());
		verify(paymentWebClient, never()).refund(any());
	}

	@Test
	void multipleItems_allReservedConcurrently_thenConfirmed() {
		when(catalogGateway.getProduct(1L)).thenReturn(new CatalogProduct(1L, "Phone", 499.0, 0L));
		when(catalogGateway.getProduct(2L)).thenReturn(new CatalogProduct(2L, "Case", 20.0, 0L));
		CreateOrderRequest request = new CreateOrderRequest("Hemanth",
				List.of(new CreateOrderRequest.Line(1L, 2), new CreateOrderRequest.Line(2L, 1)));

		orderService.create(request);

		// BOTH line items reserved (the fan-out), then the order confirmed
		verify(inventoryClient).reserve(new StockRequest(100L, 1L, 2));
		verify(inventoryClient).reserve(new StockRequest(100L, 2L, 1));
		verify(orderPersistence).confirm(eq(100L), anyDouble());
		verify(orderPersistence, never()).fail(anyLong());
	}

	@Test
	void chargeFails_releasesStock_failsOrder_refunds_andThrows() {
		when(catalogGateway.getProduct(1L)).thenReturn(new CatalogProduct(1L, "Phone", 499.0, 0L));
		CreateOrderRequest request = new CreateOrderRequest("Hemanth", List.of(new CreateOrderRequest.Line(1L, 2)));
		doThrow(WebClientResponseException.create(500, "Server Error", HttpHeaders.EMPTY, new byte[0], null))
				.when(paymentWebClient).charge(any(ChargeRequest.class));

		assertThatThrownBy(() -> orderService.create(request))
				.isInstanceOf(OrderFailedException.class);

		verify(inventoryClient).release(new StockRequest(100L, 1L, 2));
		verify(orderPersistence).fail(100L);
		verify(paymentWebClient).refund(new ChargeRequest(100L, 998.0));
		verify(orderPersistence, never()).confirm(anyLong(), anyDouble());
	}

	@Test
	void oneReserveFails_releasesAllItems_failsOrder_andThrows() {
		when(catalogGateway.getProduct(1L)).thenReturn(new CatalogProduct(1L, "Phone", 499.0, 0L));
		when(catalogGateway.getProduct(2L)).thenReturn(new CatalogProduct(2L, "Case", 20.0, 0L));
		CreateOrderRequest request = new CreateOrderRequest("Hemanth",
				List.of(new CreateOrderRequest.Line(1L, 2), new CreateOrderRequest.Line(2L, 1)));
		// product 2's reserve blows up; the fan-out surfaces it wrapped in a CompletionException
		doThrow(new RuntimeException("inventory unavailable"))
				.when(inventoryClient).reserve(new StockRequest(100L, 2L, 1));

		assertThatThrownBy(() -> orderService.create(request))
				.isInstanceOf(OrderFailedException.class);

		// release-ALL compensation: both items released (R39 idempotency = the non-reserved one is a no-op)
		verify(inventoryClient).release(new StockRequest(100L, 1L, 2));
		verify(inventoryClient).release(new StockRequest(100L, 2L, 1));
		verify(orderPersistence).fail(100L);
		verify(paymentWebClient, never()).charge(any());
		verify(orderPersistence, never()).confirm(anyLong(), anyDouble());
	}
}
