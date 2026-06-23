package com.interview.paymentservice.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.interview.paymentservice.web.PaymentDeclinedException;

@RestControllerAdvice
public class GlobalExceptionHandler {
	
	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(PaymentDeclinedException.class)
	public ProblemDetail handlePaymentFailure(PaymentDeclinedException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.PAYMENT_REQUIRED, ex.getMessage());
	}
	
	@ExceptionHandler(Exception.class)
	public ProblemDetail handleGeneric(Exception  ex) {
		log.info("Unhandled exception - {} ", ex);
		return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());

	}
}
