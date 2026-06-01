package com.ddy.aicustomerservice.common.utils;

/**
 * @author 罗亚兰
 * @date 2026/5/15 20:43
 **/

import org.springframework.beans.BeanUtils;

import java.util.Collections;
import java.util.List;

/**
 * Bean 拷贝工具类
 *
 * 适用于字段名相同的简单对象转换。
 */
public class BeanCopyUtils {

    private BeanCopyUtils() {
    }

    /**
     * 单个对象转换
     *
     * @param source 源对象
     * @param targetClass 目标类型
     * @return 目标对象
     */
    public static <T> T copy(Object source, Class<T> targetClass) {
        if (source == null) {
            return null;
        }

        try {
            T target = targetClass.getDeclaredConstructor().newInstance();
            BeanUtils.copyProperties(source, target);
            return target;
        } catch (Exception e) {
            throw new IllegalStateException("对象转换失败：" + targetClass.getName(), e);
        }
    }

    /**
     * 列表对象转换
     *
     * @param sourceList 源对象列表
     * @param targetClass 目标类型
     * @return 目标对象列表
     */
    public static <S, T> List<T> copyList(List<S> sourceList, Class<T> targetClass) {
        if (sourceList == null || sourceList.isEmpty()) {
            return Collections.emptyList();
        }

        return sourceList.stream()
                .map(item -> copy(item, targetClass))
                .toList();
    }
}
