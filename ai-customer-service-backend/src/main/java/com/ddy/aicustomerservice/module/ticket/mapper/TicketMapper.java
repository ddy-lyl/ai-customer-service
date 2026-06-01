package com.ddy.aicustomerservice.module.ticket.mapper;

/**
 * @author 罗亚兰
 * @date 2026/5/17 14:23
 **/
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ddy.aicustomerservice.module.ticket.entity.Ticket;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工单 Mapper
 */
@Mapper
public interface TicketMapper extends BaseMapper<Ticket> {
}