package com.ddy.aicustomerservice.config;

/**
 * @author 罗亚兰
 * @date 2026/5/15 16:37
 **/

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * 跨域配置
 *
 * 解决前端 Vue 调用后端接口时的跨域问题。
 */
@Configuration
public class CorsConfig {

    @Bean
    public UrlBasedCorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 开发阶段允许本地前端访问
        configuration.addAllowedOriginPattern("http://localhost:*");
        configuration.addAllowedOriginPattern("http://127.0.0.1:*");

        // 允许所有请求方法：GET、POST、PUT、DELETE 等
        configuration.addAllowedMethod("*");

        // 允许所有请求头
        configuration.addAllowedHeader("*");

        // 允许携带认证信息
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source =
                new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
