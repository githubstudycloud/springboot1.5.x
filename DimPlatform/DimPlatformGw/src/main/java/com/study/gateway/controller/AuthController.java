package com.study.gateway.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    @GetMapping("/auth")
    public String authenticate() {
        return "Authentication success!";
    }
}
