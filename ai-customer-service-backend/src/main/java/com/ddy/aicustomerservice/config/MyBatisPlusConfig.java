package com.ddy.aicustomerservice.config;

/**
 * @author 罗亚兰
 * @date 2026/5/15 16:29
 **/


import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置类
 */
@Configuration
@MapperScan("com.ddy.aicustomerservice.module.**.mapper")
public class MyBatisPlusConfig {

    /**
     * MyBatis-Plus 插件配置
     *
     * 这里先配置分页插件。
     * 后面查询工单列表、用户列表、知识库列表都会用到分页。
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());

        PaginationInnerInterceptor paginationInnerInterceptor =
                new PaginationInnerInterceptor(DbType.MYSQL);

        interceptor.addInnerInterceptor(paginationInnerInterceptor);
        return interceptor;
    }
}
