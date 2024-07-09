//package com.study.gateway.service;
//
//
//import com.study.gateway.dao.busy.TestDao;
//import com.study.gateway.dao.common.VersionDao;
//import com.study.gateway.entity.busy.Test;
//import com.study.gateway.entity.common.Version;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//@Service
//public class TestService {
//
//    @Autowired
//    private TestDao aDao;
//
//    @Autowired
//    private VersionDao versionDao;
//
//    public void test() {
//        Version version = versionDao.getLatestVersion();
//        System.out.println("Current version is: " + version.getVersion());
//
//        Test test = aDao.getById(1L);
//        System.out.println("A's name is: " + test.getName());
//    }
//}