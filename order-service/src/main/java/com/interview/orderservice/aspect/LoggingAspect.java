package com.interview.orderservice.aspect;

import java.util.Arrays;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public final class LoggingAspect {

	private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

	@Around("execution(* com.interview.orderservice.service..*(..))")
	public Object logExecution(ProceedingJoinPoint joinPoint) throws Throwable {

		String method = joinPoint.getSignature().toShortString();

		log.info(" -> {} arg = {}", method, Arrays.toString(joinPoint.getArgs()));

		long start = System.nanoTime();

		try {
			Object result = joinPoint.proceed();
			long ms = (System.nanoTime() - start) / 1_000_000;
			log.info(" <- {} returned {} ({} ms) ", method, result, ms);
			return result;
		} catch (Throwable ex) {
			log.error(" X {} threw {}", method, ex.toString());
			throw ex;
		}

	}
}
