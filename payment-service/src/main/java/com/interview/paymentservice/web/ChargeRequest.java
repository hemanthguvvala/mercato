package com.interview.paymentservice.web;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ChargeRequest (@NotNull Long orderId, @Positive double amount){

}
