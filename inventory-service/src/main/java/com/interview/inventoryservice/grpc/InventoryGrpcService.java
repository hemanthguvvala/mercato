package com.interview.inventoryservice.grpc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.interview.inventoryservice.exception.InSufficientStockException;
import com.interview.inventoryservice.service.InventoryService;

import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

/**
 * gRPC server implementation for stock reservation.
 *
 * Lives in the same package as the generated stubs (com.interview.inventoryservice.grpc), so it
 * needs no imports for ReserveRequest / ReserveResponse / InventoryReservationServiceGrpc.
 *
 * @GrpcService (net.devh) registers this with the auto-started gRPC server (port from
 * grpc.server.port). It just delegates to the existing InventoryService — same business logic as
 * the REST path — and maps the outcome onto the gRPC response.
 */
@GrpcService
public class InventoryGrpcService extends InventoryReservationServiceGrpc.InventoryReservationServiceImplBase {

	private static final Logger log = LoggerFactory.getLogger(InventoryGrpcService.class);

	private final InventoryService inventoryService;

	public InventoryGrpcService(InventoryService inventoryService) {
		this.inventoryService = inventoryService;
	}

	@Override
	public void reserve(ReserveRequest request, StreamObserver<ReserveResponse> responseObserver) {
		ReserveResponse response;
		try {
			inventoryService.reserve(request.getProductId(), request.getQuantity());
			response = ReserveResponse.newBuilder()
					.setSuccess(true)
					.setMessage("Reserved " + request.getQuantity() + " of product " + request.getProductId())
					.build();
		} catch (InSufficientStockException e) {
			log.info("gRPC reserve declined: {}", e.getMessage());
			response = ReserveResponse.newBuilder()
					.setSuccess(false)
					.setMessage(e.getMessage())
					.build();
		}
		responseObserver.onNext(response);   // send the single response
		responseObserver.onCompleted();      // close the call
	}
}
