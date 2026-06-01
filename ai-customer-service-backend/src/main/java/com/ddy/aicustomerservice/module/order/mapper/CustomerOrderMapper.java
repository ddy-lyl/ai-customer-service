package com.ddy.aicustomerservice.module.order.mapper;

/**
 * @author 罗亚兰
 * @date 2026/5/17 13:50
 **/

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ddy.aicustomerservice.module.order.entity.CustomerOrder;
import org.apache.ibatis.annotations.Mapper;

/**
 * 客户订单 Mapper
 *
 * 负责 customer_order 表的数据库操作。
 */
@Mapper
public interface CustomerOrderMapper extends BaseMapper<CustomerOrder> {
}
