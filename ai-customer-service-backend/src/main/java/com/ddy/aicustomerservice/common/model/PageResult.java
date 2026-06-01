package com.ddy.aicustomerservice.common.model;

/**
 * @author 罗亚兰
 * @date 2026/5/15 20:31
 **/

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.util.List;

/**
 * 通用分页响应对象
 *
 * @param <T> 列表数据类型
 */
@Data
public class PageResult<T> {

    /**
     * 当前页码
     */
    private Long pageNo;

    /**
     * 每页条数
     */
    private Long pageSize;

    /**
     * 总条数
     */
    private Long total;

    /**
     * 总页数
     */
    private Long pages;

    /**
     * 当前页数据
     */
    private List<T> records;

    /**
     * 根据 MyBatis-Plus 的 IPage 创建分页结果
     */
    public static <T> PageResult<T> of(IPage<T> page) {
        PageResult<T> result = new PageResult<>();
        result.setPageNo(page.getCurrent());
        result.setPageSize(page.getSize());
        result.setTotal(page.getTotal());
        result.setPages(page.getPages());
        result.setRecords(page.getRecords());
        return result;
    }

    /**
     * 手动创建分页结果
     */
    public static <T> PageResult<T> of(Long pageNo,
                                       Long pageSize,
                                       Long total,
                                       Long pages,
                                       List<T> records) {
        PageResult<T> result = new PageResult<>();
        result.setPageNo(pageNo);
        result.setPageSize(pageSize);
        result.setTotal(total);
        result.setPages(pages);
        result.setRecords(records);
        return result;
    }
}
