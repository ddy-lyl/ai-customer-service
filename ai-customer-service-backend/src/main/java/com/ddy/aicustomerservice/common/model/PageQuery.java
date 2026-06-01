package com.ddy.aicustomerservice.common.model;

/**
 * @author 罗亚兰
 * @date 2026/5/15 20:31
 **/

import lombok.Data;

/**
 * 通用分页请求参数
 *
 * 前端分页查询时传：
 * pageNo：第几页
 * pageSize：每页多少条
 */
@Data
public class PageQuery {

    /**
     * 当前页码
     */
    private Long pageNo = 1L;

    /**
     * 每页条数
     */
    private Long pageSize = 10L;

    /**
     * 获取安全页码
     */
    public Long getSafePageNo() {
        if (pageNo == null || pageNo < 1) {
            return 1L;
        }
        return pageNo;
    }

    /**
     * 获取安全每页条数
     */
    public Long getSafePageSize() {
        if (pageSize == null || pageSize < 1) {
            return 10L;
        }

        // 防止前端一次查太多数据
        if (pageSize > 100) {
            return 100L;
        }

        return pageSize;
    }
}
