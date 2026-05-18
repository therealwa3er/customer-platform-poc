package com.customerplatform.customer.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LoyaltyController {

    @GetMapping("/api/loyalty/me")
    public String myLoyalty() {
        return "My loyalty points";
    }
}
