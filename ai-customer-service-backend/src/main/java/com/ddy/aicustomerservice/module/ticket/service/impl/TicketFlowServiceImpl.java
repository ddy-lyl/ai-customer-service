package com.ddy.aicustomerservice.module.ticket.service.impl;

/**
 * @author 罗亚兰
 * @date 2026/5/17 14:28
 **/
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ddy.aicustomerservice.common.enums.RoleCodeEnum;
import com.ddy.aicustomerservice.common.enums.TicketFlowActionEnum;
import com.ddy.aicustomerservice.common.enums.TicketStatusEnum;
import com.ddy.aicustomerservice.common.model.PageResult;
import com.ddy.aicustomerservice.module.ticket.dto.TicketFlowPageQuery;
import com.ddy.aicustomerservice.module.ticket.entity.TicketFlow;
import com.ddy.aicustomerservice.module.ticket.mapper.TicketFlowMapper;
import com.ddy.aicustomerservice.module.ticket.service.TicketFlowService;
import com.ddy.aicustomerservice.module.ticket.support.TicketStatusTransitionHelper;
import com.ddy.aicustomerservice.module.ticket.vo.TicketFlowTimelineVO;
import com.ddy.aicustomerservice.module.ticket.vo.TicketFlowVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 工单流转记录业务实现类
 */
@Service
@RequiredArgsConstructor
public class TicketFlowServiceImpl implements TicketFlowService {

    private final TicketFlowMapper ticketFlowMapper;

    /**
     * 记录工单流转
     *
     * 注意：
     * 这里会统一校验状态流转是否合法。
     */
    @Override
    public void recordFlow(Long ticketId,
                           Long operatorId,
                           String operatorName,
                           String operatorRole,
                           String action,
                           String fromStatus,
                           String toStatus,
                           String remark) {
        TicketStatusTransitionHelper.checkTransition(action, fromStatus, toStatus);

        TicketFlow flow = new TicketFlow();

        flow.setTicketId(ticketId);
        flow.setOperatorId(operatorId);
        flow.setOperatorName(operatorName);
        flow.setOperatorRole(operatorRole);
        flow.setAction(action);
        flow.setFromStatus(fromStatus);
        flow.setToStatus(toStatus);
        flow.setRemark(remark);

        ticketFlowMapper.insert(flow);
    }

    /**
     * 查询工单流转记录
     */
    @Override
    public List<TicketFlowVO> listByTicketId(Long ticketId) {
        List<TicketFlow> flows = ticketFlowMapper.selectList(
                new LambdaQueryWrapper<TicketFlow>()
                        .eq(TicketFlow::getTicketId, ticketId)
                        .orderByAsc(TicketFlow::getCreateTime)
        );

        return flows.stream()
                .map(this::convertToVO)
                .toList();
    }

    /**
     * 查询工单时间线
     */
    @Override
    public List<TicketFlowTimelineVO> listTimelineByTicketId(Long ticketId) {
        List<TicketFlow> flows = ticketFlowMapper.selectList(
                new LambdaQueryWrapper<TicketFlow>()
                        .eq(TicketFlow::getTicketId, ticketId)
                        .orderByAsc(TicketFlow::getCreateTime)
        );

        return flows.stream()
                .map(this::convertToTimelineVO)
                .toList();
    }

    /**
     * 管理员分页查询所有工单流转记录
     */
    @Override
    public PageResult<TicketFlowVO> pageFlowsForAdmin(TicketFlowPageQuery query) {
        Page<TicketFlow> page = new Page<>(
                query.getSafePageNo(),
                query.getSafePageSize()
        );

        LambdaQueryWrapper<TicketFlow> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(query.getTicketId() != null,
                TicketFlow::getTicketId,
                query.getTicketId());

        wrapper.eq(query.getOperatorId() != null,
                TicketFlow::getOperatorId,
                query.getOperatorId());

        wrapper.eq(StringUtils.hasText(query.getOperatorRole()),
                TicketFlow::getOperatorRole,
                query.getOperatorRole());

        wrapper.eq(StringUtils.hasText(query.getAction()),
                TicketFlow::getAction,
                query.getAction());

        wrapper.eq(StringUtils.hasText(query.getFromStatus()),
                TicketFlow::getFromStatus,
                query.getFromStatus());

        wrapper.eq(StringUtils.hasText(query.getToStatus()),
                TicketFlow::getToStatus,
                query.getToStatus());

        wrapper.orderByDesc(TicketFlow::getCreateTime);

        Page<TicketFlow> flowPage = ticketFlowMapper.selectPage(page, wrapper);

        List<TicketFlowVO> records = flowPage.getRecords()
                .stream()
                .map(this::convertToVO)
                .toList();

        return PageResult.of(
                flowPage.getCurrent(),
                flowPage.getSize(),
                flowPage.getTotal(),
                flowPage.getPages(),
                records
        );
    }

    /**
     * Entity 转普通 VO
     */
    private TicketFlowVO convertToVO(TicketFlow flow) {
        TicketFlowVO vo = new TicketFlowVO();

        vo.setId(flow.getId());
        vo.setTicketId(flow.getTicketId());
        vo.setOperatorId(flow.getOperatorId());
        vo.setOperatorName(flow.getOperatorName());
        vo.setOperatorRole(flow.getOperatorRole());
        vo.setAction(flow.getAction());
        vo.setActionName(convertActionName(flow.getAction()));
        vo.setFromStatus(flow.getFromStatus());
        vo.setFromStatusName(convertStatusName(flow.getFromStatus()));
        vo.setToStatus(flow.getToStatus());
        vo.setToStatusName(convertStatusName(flow.getToStatus()));
        vo.setRemark(flow.getRemark());
        vo.setCreateTime(flow.getCreateTime());

        return vo;
    }

    /**
     * Entity 转时间线 VO
     */
    private TicketFlowTimelineVO convertToTimelineVO(TicketFlow flow) {
        TicketFlowTimelineVO vo = new TicketFlowTimelineVO();

        String actionName = convertActionName(flow.getAction());
        String fromStatusName = convertStatusName(flow.getFromStatus());
        String toStatusName = convertStatusName(flow.getToStatus());

        vo.setId(flow.getId());
        vo.setTicketId(flow.getTicketId());
        vo.setTitle(actionName);
        vo.setDescription(buildTimelineDescription(flow, actionName, fromStatusName, toStatusName));
        vo.setOperatorId(flow.getOperatorId());
        vo.setOperatorName(flow.getOperatorName());
        vo.setOperatorRole(flow.getOperatorRole());
        vo.setOperatorRoleName(convertRoleName(flow.getOperatorRole()));
        vo.setAction(flow.getAction());
        vo.setActionName(actionName);
        vo.setFromStatus(flow.getFromStatus());
        vo.setFromStatusName(fromStatusName);
        vo.setToStatus(flow.getToStatus());
        vo.setToStatusName(toStatusName);
        vo.setStatusChangeText(buildStatusChangeText(fromStatusName, toStatusName));
        vo.setRemark(flow.getRemark());
        vo.setCreateTime(flow.getCreateTime());

        return vo;
    }

    /**
     * 构建时间线描述
     */
    private String buildTimelineDescription(TicketFlow flow,
                                            String actionName,
                                            String fromStatusName,
                                            String toStatusName) {
        String operator = StringUtils.hasText(flow.getOperatorName())
                ? flow.getOperatorName()
                : "系统";

        String statusChange = buildStatusChangeText(fromStatusName, toStatusName);

        if (StringUtils.hasText(flow.getRemark())) {
            return operator + "执行了【" + actionName + "】，" + flow.getRemark();
        }

        if (StringUtils.hasText(statusChange)) {
            return operator + "执行了【" + actionName + "】，状态变化：" + statusChange;
        }

        return operator + "执行了【" + actionName + "】";
    }

    /**
     * 构建状态变化描述
     */
    private String buildStatusChangeText(String fromStatusName, String toStatusName) {
        if (!StringUtils.hasText(fromStatusName) && !StringUtils.hasText(toStatusName)) {
            return null;
        }

        if (!StringUtils.hasText(fromStatusName)) {
            return toStatusName;
        }

        if (!StringUtils.hasText(toStatusName)) {
            return fromStatusName;
        }

        return fromStatusName + " → " + toStatusName;
    }

    /**
     * 操作类型转中文
     */
    private String convertActionName(String action) {
        if (action == null) {
            return null;
        }

        for (TicketFlowActionEnum actionEnum : TicketFlowActionEnum.values()) {
            if (actionEnum.getCode().equals(action)) {
                return actionEnum.getName();
            }
        }

        return "未知操作";
    }

    /**
     * 工单状态转中文
     */
    private String convertStatusName(String status) {
        if (status == null) {
            return null;
        }

        for (TicketStatusEnum statusEnum : TicketStatusEnum.values()) {
            if (statusEnum.getCode().equals(status)) {
                return statusEnum.getName();
            }
        }

        return "未知状态";
    }

    /**
     * 角色编码转中文
     */
    private String convertRoleName(String roleCode) {
        if (roleCode == null) {
            return null;
        }

        if (RoleCodeEnum.USER.getCode().equals(roleCode)) {
            return RoleCodeEnum.USER.getName();
        }

        if (RoleCodeEnum.STAFF.getCode().equals(roleCode)) {
            return RoleCodeEnum.STAFF.getName();
        }

        if (RoleCodeEnum.ADMIN.getCode().equals(roleCode)) {
            return RoleCodeEnum.ADMIN.getName();
        }

        if ("AI".equals(roleCode)) {
            return "AI助手";
        }

        return "未知角色";
    }
}