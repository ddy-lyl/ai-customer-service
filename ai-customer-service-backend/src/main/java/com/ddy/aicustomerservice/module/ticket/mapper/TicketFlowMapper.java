package com.ddy.aicustomerservice.module.ticket.mapper;

/**
 * @author 罗亚兰
 * @date 2026/5/17 14:24
 **/
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ddy.aicustomerservice.module.ticket.entity.TicketFlow;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工单流转记录 Mapper
 */
@Mapper
public interface TicketFlowMapper extends BaseMapper<TicketFlow> {
}
