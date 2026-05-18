package com.customerplatform.customer.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminController {

    @GetMapping("/api/admin/dashboard")
    public String dashboard() {
        return "Admin dashboard";
    }
}
