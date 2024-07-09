//package com.study.gateway.controller;
//
//import com.study.gateway.dao.busy.TestDao;
//import com.study.gateway.entity.busy.Test;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//
//@RestController
//public class TestController {
//
//    @Autowired
//    private TestDao testDao;
//
//    @GetMapping("/test")
//    public Test getTest(@RequestParam String version, @RequestParam Long id) {
//        return testDao.getById(id);
//    }
//}