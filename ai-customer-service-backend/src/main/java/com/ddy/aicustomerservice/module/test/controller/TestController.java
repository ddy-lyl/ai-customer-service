package com.ddy.aicustomerservice.module.test.controller;

/**
 * @author 罗亚兰
 * @date 2026/5/15 16:39
 **/

import com.ddy.aicustomerservice.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 系统连通性测试接口（仅 dev 环境注册）
 *
 * 用于验证：
 * 1. 后端服务是否启动
 * 2. MySQL 是否连接成功
 * 3. Redis 是否连接成功
 */
@Profile("dev")
@RestController
@RequiredArgsConstructor
public class TestController {

    private final JdbcTemplate jdbcTemplate;

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 测试后端服务是否启动
     */
    @GetMapping("/api/test/ping")
    public Result<Map<String, Object>> ping() {
        Map<String, Object> data = new HashMap<>();
        data.put("message", "AI智能客服后端启动成功");
        data.put("time", LocalDateTime.now());

        return Result.success(data);
    }

    /**
     * 测试 MySQL 连接
     */
    @GetMapping("/api/test/db")
    public Result<Map<String, Object>> testDb() {
        Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);

        Map<String, Object> data = new HashMap<>();
        data.put("db", "MySQL连接成功");
        data.put("result", result);

        return Result.success(data);
    }

    /**
     * 测试 Redis 连接
     */
    @GetMapping("/api/test/redis")
    public Result<Map<String, Object>> testRedis() {
        String key = "test:ping";
        String value = "redis ok " + LocalDateTime.now();

        stringRedisTemplate.opsForValue().set(key, value, 60, TimeUnit.SECONDS);

        String redisValue = stringRedisTemplate.opsForValue().get(key);

        Map<String, Object> data = new HashMap<>();
        data.put("redis", "Redis连接成功");
        data.put("key", key);
        data.put("value", redisValue);

        return Result.success(data);
    }
}
