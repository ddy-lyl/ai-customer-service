package com.ddy.aicustomerservice.config;

/**
 * @author 罗亚兰
 * @date 2026/5/15 16:30
 **/

import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MyBatis-Plus 自动填充处理器
 *
 * 作用：
 * 1. 新增数据时自动填充 createTime、updateTime、deleted
 * 2. 修改数据时自动更新 updateTime
 */
@Component
public class MyBatisMetaObjectHandler implements MetaObjectHandler {

    /**
     * 新增时填充
     */
    @Override
    public void insertFill(MetaObject metaObject) {
        LocalDateTime now = LocalDateTime.now();

        this.strictInsertFill(metaObject, "createTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "updateTime", LocalDateTime.class, now);
        this.strictInsertFill(metaObject, "deleted", Integer.class, 0);
    }

    /**
     * 修改时填充
     */
    @Override
    public void updateFill(MetaObject metaObject) {
        this.strictUpdateFill(metaObject, "updateTime", LocalDateTime.class, LocalDateTime.now());
    }
}