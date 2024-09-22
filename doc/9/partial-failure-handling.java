package com.example.demo.service.impl;

import com.example.demo.exception.BusinessException;
import com.example.demo.model.UserProfile;
import com.example.demo.service.UserProfileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
public class UserProfileServiceImpl implements UserProfileService {

    private static final Logger logger = LoggerFactory.getLogger(UserProfileServiceImpl.class);

    @Override
    public UserProfile getUserProfile(Long userId) throws BusinessException {
        UserProfile profile = new UserProfile(userId);
        Map<String, CompletableFuture<Void>> futures = new HashMap<>();

        // 异步获取基本信息
        futures.put("basicInfo", CompletableFuture.runAsync(() -> {
            try {
                Map<String, String> basicInfo = getBasicInfo(userId);
                profile.setBasicInfo(basicInfo);
            } catch (Exception e) {
                logger.error("Failed to get basic info for user {}: {}", userId, e.getMessage());
            }
        }));

        // 异步获取订单历史
        futures.put("orderHistory", CompletableFuture.runAsync(() -> {
            try {
                List<Order> orderHistory = getOrderHistory(userId);
                profile.setOrderHistory(orderHistory);
            } catch (Exception e) {
                logger.error("Failed to get order history for user {}: {}", userId, e.getMessage());
            }
        }));

        // 异步获取推荐列表
        futures.put("recommendations", CompletableFuture.runAsync(() -> {
            try {
                List<String> recommendations = getRecommendations(userId);
                profile.setRecommendations(recommendations);
            } catch (Exception e) {
                logger.error("Failed to get recommendations for user {}: {}", userId, e.getMessage());
            }
        }));

        // 等待所有异步操作完成
        CompletableFuture.allOf(futures.values().toArray(new CompletableFuture[0])).join();

        // 检查是否所有操作都失败了
        if (profile.getBasicInfo() == null && profile.getOrderHistory() == null && profile.getRecommendations() == null) {
            throw new BusinessException("Failed to retrieve any user profile data");
        }

        return profile;
    }

    private Map<String, String> getBasicInfo(Long userId) throws Exception {
        // 模拟获取基本信息的操作
        if (userId < 0) {
            throw new Exception("Invalid user ID");
        }
        Map<String, String> basicInfo = new HashMap<>();
        basicInfo.put("name", "User " + userId);
        basicInfo.put("email", "user" + userId + "@example.com");
        return basicInfo;
    }

    private List<Order> getOrderHistory(Long userId) throws Exception {
        // 模拟获取订单历史的操作
        if (userId % 2 == 0) {
            throw new Exception("Order history service unavailable");
        }
        return List.of(new Order(1L, "Product A"), new Order(2L, "Product B"));
    }

    private List<String> getRecommendations(Long userId) throws Exception {
        // 模拟获取推荐列表的操作
        if (userId % 3 == 0) {
            throw new Exception("Recommendation service unavailable");
        }
        return List.of("Recommended Product 1", "Recommended Product 2");
    }
}
