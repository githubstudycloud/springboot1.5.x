//package com.study.gateway.service;
//
//import com.study.gateway.config.BusyDbConfig;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.cloud.context.config.annotation.RefreshScope;
//import org.springframework.stereotype.Service;
//
//import javax.annotation.PostConstruct;
//
//@Service
//@RefreshScope
//public class ConfigService {
//
//    @Autowired
//    private BusyDbConfig busyDbConfig;
//
//    @PostConstruct
//    public void init() {
//        refreshConfig();
//    }
//
//    public void refreshConfig() {
//        // 从配置中心获取最新的配置并更新BusyDbConfig
//        // ...
//    }
//}