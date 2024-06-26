package com.study.gateway.controller;

import com.study.gateway.service.ConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConfigController {

    @Autowired
    private ConfigService configService;

    @PostMapping("/refreshConfig")
    public void refreshConfig() {
        configService.refreshConfig();
    }
}