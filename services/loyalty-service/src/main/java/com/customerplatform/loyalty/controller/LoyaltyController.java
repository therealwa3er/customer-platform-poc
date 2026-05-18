package com.customerplatform.loyalty.controller;

import com.customerplatform.loyalty.dto.AddPointsRequest;
import com.customerplatform.loyalty.dto.LoyaltyAccountResponse;
import com.customerplatform.loyalty.service.LoyaltyService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/loyalty")
public class LoyaltyController {

    private final LoyaltyService loyaltyService;

    public LoyaltyController(LoyaltyService loyaltyService) {
        this.loyaltyService = loyaltyService;
    }

    @GetMapping("/me")
    public LoyaltyAccountResponse me(@AuthenticationPrincipal Jwt jwt) {
        return loyaltyService.getAccount(jwt.getSubject());
    }

    @PostMapping("/me/points")
    public LoyaltyAccountResponse addPoints(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AddPointsRequest request
    ) {
        return loyaltyService.addPoints(jwt.getSubject(), request);
    }
}
