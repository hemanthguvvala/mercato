package com.interview.inventoryservice.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
	
	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(InSufficientStockException.class)
	public ProblemDetail handleStockException(InSufficientStockException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
	}
	
	@ExceptionHandler(Exception.class)
	public ProblemDetail handleGeneric(Exception ex) {
		log.info("Unhandle exception - {} ", ex);
		return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,  "Something went wrong please contact admin.");
	}
	
	@ExceptionHandler(IllegalStateException.class)
	public ProblemDetail handleStateException(IllegalStateException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
	}
}
