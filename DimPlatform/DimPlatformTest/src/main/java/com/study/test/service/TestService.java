package com.study.test.service;

import org.springframework.stereotype.Service;

@Service
public class TestService {

    public String performTest() {
        return "Test successful!";
    }
}
