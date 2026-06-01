package com.ddy.aicustomerservice.module.ticket.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ddy.aicustomerservice.common.context.LoginUserContext;
import com.ddy.aicustomerservice.common.context.LoginUserInfo;
import com.ddy.aicustomerservice.common.enums.RoleCodeEnum;
import com.ddy.aicustomerservice.common.enums.TicketFlowActionEnum;
import com.ddy.aicustomerservice.common.enums.TicketPriorityEnum;
import com.ddy.aicustomerservice.common.enums.TicketSourceEnum;
import com.ddy.aicustomerservice.common.enums.TicketStatusEnum;
import com.ddy.aicustomerservice.common.enums.TicketTypeEnum;
import com.ddy.aicustomerservice.common.exception.BusinessException;
import com.ddy.aicustomerservice.common.model.PageResult;
import com.ddy.aicustomerservice.common.result.ResultCodeEnum;
import com.ddy.aicustomerservice.module.order.entity.CustomerOrder;
import com.ddy.aicustomerservice.module.order.mapper.CustomerOrderMapper;
import com.ddy.aicustomerservice.module.role.entity.SysRole;
import com.ddy.aicustomerservice.module.role.entity.SysUserRole;
import com.ddy.aicustomerservice.module.role.mapper.SysRoleMapper;
import com.ddy.aicustomerservice.module.role.mapper.SysUserRoleMapper;
import com.ddy.aicustomerservice.module.ticket.dto.TicketAssignRequest;
import com.ddy.aicustomerservice.module.ticket.dto.TicketCancelRequest;
import com.ddy.aicustomerservice.module.ticket.dto.TicketCloseRequest;
import com.ddy.aicustomerservice.module.ticket.dto.TicketCreateRequest;
import com.ddy.aicustomerservice.module.ticket.dto.TicketPageQuery;
import com.ddy.aicustomerservice.module.ticket.dto.TicketProcessRequest;
import com.ddy.aicustomerservice.module.ticket.entity.Ticket;
import com.ddy.aicustomerservice.module.ticket.mapper.TicketMapper;
import com.ddy.aicustomerservice.module.ticket.service.TicketFlowService;
import com.ddy.aicustomerservice.module.ticket.service.TicketService;
import com.ddy.aicustomerservice.module.ticket.support.TicketStatusTransitionHelper;
import com.ddy.aicustomerservice.module.ticket.vo.TicketFlowTimelineVO;
import com.ddy.aicustomerservice.module.ticket.vo.TicketFlowVO;
import com.ddy.aicustomerservice.module.ticket.vo.TicketVO;
import com.ddy.aicustomerservice.module.user.entity.SysUser;
import com.ddy.aicustomerservice.module.user.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 工单业务实现类
 *
 * 设计要点：
 * 1. 普通用户与 AI 创建工单走两个入口，避免 source 字段被前端伪造；
 * 2. orderNo 不为空时必须校验订单存在且当前用户拥有该订单；
 * 3. 列表场景使用批量加载，避免 N+1 查询用户和昵称；
 * 4. 状态流转统一走 {@link TicketStatusTransitionHelper}。
 */
@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

    private final TicketMapper ticketMapper;

    private final TicketFlowService ticketFlowService;

    private final SysUserMapper sysUserMapper;

    private final SysRoleMapper sysRoleMapper;

    private final SysUserRoleMapper sysUserRoleMapper;

    private final CustomerOrderMapper customerOrderMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TicketVO createTicket(TicketCreateRequest request) {
        if (request.getOnBehalfUserId() != null) {
            throw new BusinessException(
                    ResultCodeEnum.PARAM_ERROR.getCode(),
                    "普通用户建单不允许指定代客用户，请使用客服代建接口"
            );
        }

        LoginUserInfo currentUser = LoginUserContext.getRequired();

        Ticket ticket = buildTicket(
                request,
                currentUser.getUserId(),
                currentUser,
                TicketSourceEnum.USER.getCode(),
                null
        );

        ticketMapper.insert(ticket);

        ticketFlowService.recordFlow(
                ticket.getId(),
                currentUser.getUserId(),
                currentUser.getNickname(),
                getCurrentUserMainRole(),
                TicketFlowActionEnum.CREATE.getCode(),
                null,
                TicketStatusEnum.PENDING.getCode(),
                "用户创建工单"
        );

        return convertToTicketVO(ticket, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TicketVO createTicketOnBehalf(TicketCreateRequest request) {
        LoginUserInfo staff = LoginUserContext.getRequired();
        if (!staff.hasRole(RoleCodeEnum.STAFF.getCode())
                && !staff.hasRole(RoleCodeEnum.ADMIN.getCode())) {
            throw new BusinessException(ResultCodeEnum.FORBIDDEN.getCode(), "仅客服或管理员可代客建单");
        }

        Long targetUserId = request.getOnBehalfUserId();
        if (targetUserId == null || targetUserId <= 0) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "代客建单必须指定目标客户用户ID");
        }
        if (!isEndUser(targetUserId)) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "目标客户必须是普通用户账号");
        }

        SysUser targetUser = sysUserMapper.selectById(targetUserId);
        if (targetUser == null) {
            throw new BusinessException(ResultCodeEnum.NOT_FOUND.getCode(), "目标客户用户不存在");
        }

        Ticket ticket = buildTicket(
                request,
                targetUserId,
                staff,
                TicketSourceEnum.STAFF.getCode(),
                null
        );

        ticketMapper.insert(ticket);

        String targetName = getUserNickname(targetUser);
        ticketFlowService.recordFlow(
                ticket.getId(),
                staff.getUserId(),
                staff.getNickname(),
                getCurrentUserMainRole(),
                TicketFlowActionEnum.CREATE.getCode(),
                null,
                TicketStatusEnum.PENDING.getCode(),
                "客服代用户「" + targetName + "」创建工单"
        );

        return convertToTicketVO(ticket, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TicketVO createTicketFromAi(TicketCreateRequest request, Long sessionId) {
        LoginUserInfo currentUser = LoginUserContext.getRequired();

        if (!StringUtils.hasText(request.getOrderNo())) {
            throw new BusinessException(
                    ResultCodeEnum.PARAM_ERROR.getCode(),
                    "AI创建工单必须关联订单，请先让用户选择订单号"
            );
        }

        Ticket ticket = buildTicket(
                request,
                currentUser.getUserId(),
                currentUser,
                TicketSourceEnum.AI.getCode(),
                sessionId
        );

        ticketMapper.insert(ticket);

        ticketFlowService.recordFlow(
                ticket.getId(),
                currentUser.getUserId(),
                "AI智能客服",
                "AI",
                TicketFlowActionEnum.CREATE.getCode(),
                null,
                TicketStatusEnum.PENDING.getCode(),
                "AI根据用户问题自动创建售后工单"
        );

        return convertToTicketVO(ticket, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TicketVO createHandoffTicket(Long sessionId, Long userId, String reason, String description) {
        Ticket ticket = new Ticket();
        ticket.setTicketNo(generateTicketNo());
        ticket.setUserId(userId);
        ticket.setOrderNo(null);
        ticket.setSessionId(sessionId);
        ticket.setTitle("在线客服转人工-" + sessionId);
        ticket.setType(TicketTypeEnum.CONSULTATION.getCode());
        ticket.setDescription(StringUtils.hasText(description) ? description : "用户请求人工客服");
        ticket.setStatus(TicketStatusEnum.PENDING.getCode());
        ticket.setPriority(TicketPriorityEnum.MEDIUM.getCode());
        ticket.setSource(TicketSourceEnum.AI.getCode());

        ticketMapper.insert(ticket);

        String remark = "转人工原因：" + (StringUtils.hasText(reason) ? reason : "未知");
        ticketFlowService.recordFlow(
                ticket.getId(),
                null,
                "AI智能客服",
                "AI",
                TicketFlowActionEnum.CREATE.getCode(),
                null,
                TicketStatusEnum.PENDING.getCode(),
                remark
        );

        return convertToTicketVO(ticket, true);
    }

    @Override
    public PageResult<TicketVO> pageMyTickets(TicketPageQuery query) {
        Long currentUserId = LoginUserContext.getUserId();

        Page<Ticket> page = new Page<>(query.getSafePageNo(), query.getSafePageSize());

        LambdaQueryWrapper<Ticket> wrapper = buildTicketQueryWrapper(query);

        wrapper.eq(Ticket::getUserId, currentUserId);
        wrapper.orderByDesc(Ticket::getCreateTime);

        Page<Ticket> ticketPage = ticketMapper.selectPage(page, wrapper);

        List<TicketVO> records = convertToTicketVOList(ticketPage.getRecords(), false);

        return PageResult.of(
                ticketPage.getCurrent(),
                ticketPage.getSize(),
                ticketPage.getTotal(),
                ticketPage.getPages(),
                records
        );
    }

    @Override
    public TicketVO getTicketDetail(Long id) {
        Ticket ticket = getTicketRequired(id);

        checkTicketViewPermission(ticket);

        return convertToTicketVO(ticket, true);
    }

    @Override
    public List<TicketFlowTimelineVO> listTicketTimeline(Long ticketId) {
        Ticket ticket = getTicketRequired(ticketId);

        checkTicketViewPermission(ticket);

        return ticketFlowService.listTimelineByTicketId(ticketId);
    }

    @Override
    public PageResult<TicketVO> pageStaffTickets(TicketPageQuery query) {
        Long currentUserId = LoginUserContext.getUserId();

        Page<Ticket> page = new Page<>(query.getSafePageNo(), query.getSafePageSize());

        LambdaQueryWrapper<Ticket> wrapper = buildTicketQueryWrapper(query);

        applyStaffScope(wrapper, query.getScope(), currentUserId);
        wrapper.orderByDesc(Ticket::getUpdateTime);

        Page<Ticket> ticketPage = ticketMapper.selectPage(page, wrapper);

        List<TicketVO> records = convertToTicketVOList(ticketPage.getRecords(), false);

        return PageResult.of(
                ticketPage.getCurrent(),
                ticketPage.getSize(),
                ticketPage.getTotal(),
                ticketPage.getPages(),
                records
        );
    }

    @Override
    public PageResult<TicketVO> pageTicketsForAdmin(TicketPageQuery query) {
        Page<Ticket> page = new Page<>(query.getSafePageNo(), query.getSafePageSize());

        LambdaQueryWrapper<Ticket> wrapper = buildTicketQueryWrapper(query);

        wrapper.eq(query.getUserId() != null, Ticket::getUserId, query.getUserId());
        wrapper.eq(query.getStaffId() != null, Ticket::getStaffId, query.getStaffId());
        wrapper.orderByDesc(Ticket::getCreateTime);

        Page<Ticket> ticketPage = ticketMapper.selectPage(page, wrapper);

        List<TicketVO> records = convertToTicketVOList(ticketPage.getRecords(), false);

        return PageResult.of(
                ticketPage.getCurrent(),
                ticketPage.getSize(),
                ticketPage.getTotal(),
                ticketPage.getPages(),
                records
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void claimTicket(Long id) {
        Ticket ticket = getTicketRequired(id);

        if (ticket.getStaffId() != null) {
            throw new BusinessException(
                    ResultCodeEnum.PARAM_ERROR.getCode(),
                    "该工单已被认领或分配"
            );
        }
        if (!TicketStatusEnum.PENDING.getCode().equals(ticket.getStatus())) {
            throw new BusinessException(
                    ResultCodeEnum.PARAM_ERROR.getCode(),
                    "仅待处理状态的工单可以认领"
            );
        }

        LoginUserInfo currentUser = LoginUserContext.getRequired();
        if (!currentUser.hasRole(RoleCodeEnum.STAFF.getCode())) {
            throw new BusinessException(
                    ResultCodeEnum.FORBIDDEN.getCode(),
                    "仅客服账号可认领工单，管理员请在工单管理中进行分配"
            );
        }

        String fromStatus = ticket.getStatus();
        String toStatus = TicketStatusEnum.PROCESSING.getCode();

        TicketStatusTransitionHelper.checkTransition(
                TicketFlowActionEnum.CLAIM.getCode(),
                fromStatus,
                toStatus
        );

        ticket.setStaffId(currentUser.getUserId());
        ticket.setStatus(toStatus);
        ticketMapper.updateById(ticket);

        ticketFlowService.recordFlow(
                ticket.getId(),
                currentUser.getUserId(),
                currentUser.getNickname(),
                getCurrentUserMainRole(),
                TicketFlowActionEnum.CLAIM.getCode(),
                fromStatus,
                toStatus,
                "客服主动认领工单"
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignTicket(Long id, TicketAssignRequest request) {
        Ticket ticket = getTicketRequired(id);

        if (ticket.getStaffId() != null) {
            throw new BusinessException(
                    ResultCodeEnum.PARAM_ERROR.getCode(),
                    "该工单已分配客服，请勿重复分配"
            );
        }
        if (!TicketStatusEnum.PENDING.getCode().equals(ticket.getStatus())) {
            throw new BusinessException(
                    ResultCodeEnum.PARAM_ERROR.getCode(),
                    "仅待处理且未分配的工单可以分配"
            );
        }

        if (!isStaff(request.getStaffId())) {
            throw new BusinessException(
                    ResultCodeEnum.PARAM_ERROR.getCode(),
                    "被分配用户不是客服人员"
            );
        }

        String fromStatus = ticket.getStatus();
        String toStatus = TicketStatusEnum.PROCESSING.getCode();

        TicketStatusTransitionHelper.checkTransition(
                TicketFlowActionEnum.ASSIGN.getCode(),
                fromStatus,
                toStatus
        );

        ticket.setStaffId(request.getStaffId());
        ticket.setStatus(toStatus);

        ticketMapper.updateById(ticket);

        LoginUserInfo currentUser = LoginUserContext.getRequired();
        SysUser staff = sysUserMapper.selectById(request.getStaffId());

        String remark = StringUtils.hasText(request.getRemark())
                ? request.getRemark()
                : "管理员将工单分配给客服：" + getUserNickname(staff);

        ticketFlowService.recordFlow(
                ticket.getId(),
                currentUser.getUserId(),
                currentUser.getNickname(),
                getCurrentUserMainRole(),
                TicketFlowActionEnum.ASSIGN.getCode(),
                fromStatus,
                toStatus,
                remark
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void processTicket(Long id, TicketProcessRequest request) {
        Ticket ticket = getTicketRequired(id);

        checkTicketProcessPermission(ticket);

        if (!TicketStatusEnum.PROCESSING.getCode().equals(ticket.getStatus())) {
            throw new BusinessException(
                    ResultCodeEnum.PARAM_ERROR.getCode(),
                    "仅处理中状态的工单可以提交处理结果，请先认领或等待分配"
            );
        }

        String fromStatus = ticket.getStatus();
        boolean resolved = Boolean.TRUE.equals(request.getResolved());
        String toStatus = resolved
                ? TicketStatusEnum.RESOLVED.getCode()
                : TicketStatusEnum.PROCESSING.getCode();
        String action = resolved
                ? TicketFlowActionEnum.RESOLVE.getCode()
                : TicketFlowActionEnum.PROCESS.getCode();

        TicketStatusTransitionHelper.checkTransition(action, fromStatus, toStatus);

        ticket.setProcessResult(request.getProcessResult());

        if (resolved) {
            ticket.setStatus(TicketStatusEnum.RESOLVED.getCode());
            ticket.setResolvedTime(LocalDateTime.now());
        }

        ticketMapper.updateById(ticket);

        LoginUserInfo currentUser = LoginUserContext.getRequired();

        String remark = StringUtils.hasText(request.getRemark())
                ? request.getRemark()
                : request.getProcessResult();

        ticketFlowService.recordFlow(
                ticket.getId(),
                currentUser.getUserId(),
                currentUser.getNickname(),
                getCurrentUserMainRole(),
                action,
                fromStatus,
                toStatus,
                remark
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelTicket(Long id, TicketCancelRequest request) {
        Ticket ticket = getTicketRequired(id);
        LoginUserInfo currentUser = LoginUserContext.getRequired();

        if (!currentUser.getUserId().equals(ticket.getUserId())) {
            throw new BusinessException(ResultCodeEnum.FORBIDDEN.getCode(), "仅工单所属用户可取消");
        }

        String fromStatus = ticket.getStatus();
        String toStatus = TicketStatusEnum.CANCELLED.getCode();

        TicketStatusTransitionHelper.checkTransition(
                TicketFlowActionEnum.CANCEL.getCode(),
                fromStatus,
                toStatus
        );

        ticket.setStatus(toStatus);
        ticketMapper.updateById(ticket);

        String remark = StringUtils.hasText(request.getCancelReason())
                ? request.getCancelReason()
                : "用户取消工单";

        ticketFlowService.recordFlow(
                ticket.getId(),
                currentUser.getUserId(),
                currentUser.getNickname(),
                RoleCodeEnum.USER.getCode(),
                TicketFlowActionEnum.CANCEL.getCode(),
                fromStatus,
                toStatus,
                remark
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeTicket(Long id, TicketCloseRequest request) {
        Ticket ticket = getTicketRequired(id);

        checkTicketClosePermission(ticket);

        String fromStatus = ticket.getStatus();
        String toStatus = TicketStatusEnum.CLOSED.getCode();

        TicketStatusTransitionHelper.checkTransition(
                TicketFlowActionEnum.CLOSE.getCode(),
                fromStatus,
                toStatus
        );

        ticket.setStatus(toStatus);
        ticket.setCloseReason(request.getCloseReason());
        ticket.setClosedTime(LocalDateTime.now());

        ticketMapper.updateById(ticket);

        LoginUserInfo currentUser = LoginUserContext.getRequired();

        String remark = StringUtils.hasText(request.getCloseReason())
                ? request.getCloseReason()
                : "关闭工单";

        ticketFlowService.recordFlow(
                ticket.getId(),
                currentUser.getUserId(),
                currentUser.getNickname(),
                getCurrentUserMainRole(),
                TicketFlowActionEnum.CLOSE.getCode(),
                fromStatus,
                toStatus,
                remark
        );
    }

    /**
     * 公共的工单构造逻辑：参数校验 + 字段填充
     *
     * 关键校验：
     * 1. 标题、描述、类型不能空（DTO 上的 @NotBlank 已经校验，这里兜底）
     * 2. type 必须是合法枚举
     * 3. 如果用户提供 orderNo，必须确实存在，且必须是 currentUser 自己的订单
     */
    private Ticket buildTicket(TicketCreateRequest request,
                               Long ticketOwnerUserId,
                               LoginUserInfo operator,
                               String source,
                               Long sessionId) {
        if (!StringUtils.hasText(request.getTitle())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "工单标题不能为空");
        }
        if (!StringUtils.hasText(request.getDescription())) {
            throw new BusinessException(ResultCodeEnum.PARAM_ERROR.getCode(), "问题描述不能为空");
        }

        String safeType = resolveType(request.getType());
        String safePriority = resolvePriority(request.getPriority());
        String safeOrderNo = StringUtils.hasText(request.getOrderNo())
                ? request.getOrderNo().trim()
                : null;

        if (safeOrderNo != null) {
            CustomerOrder order = customerOrderMapper.selectOne(
                    new LambdaQueryWrapper<CustomerOrder>()
                            .eq(CustomerOrder::getOrderNo, safeOrderNo)
                            .last("LIMIT 1")
            );

            if (order == null) {
                throw new BusinessException(
                        ResultCodeEnum.NOT_FOUND.getCode(),
                        "关联订单不存在"
                );
            }

            boolean isAdmin = operator.hasRole(RoleCodeEnum.ADMIN.getCode());
            boolean isStaffOnBehalf = TicketSourceEnum.STAFF.getCode().equals(source);
            if (!isAdmin && !isStaffOnBehalf && !ticketOwnerUserId.equals(order.getUserId())) {
                throw new BusinessException(
                        ResultCodeEnum.FORBIDDEN.getCode(),
                        "无权对该订单创建工单"
                );
            }
            if (isStaffOnBehalf && !ticketOwnerUserId.equals(order.getUserId())) {
                throw new BusinessException(
                        ResultCodeEnum.PARAM_ERROR.getCode(),
                        "关联订单不属于目标客户，无法代客建单"
                );
            }
        }

        Ticket ticket = new Ticket();
        ticket.setTicketNo(generateTicketNo());
        ticket.setUserId(ticketOwnerUserId);
        ticket.setOrderNo(safeOrderNo);
        ticket.setSessionId(sessionId);
        ticket.setTitle(request.getTitle());
        ticket.setType(safeType);
        ticket.setDescription(request.getDescription());
        ticket.setStatus(TicketStatusEnum.PENDING.getCode());
        ticket.setPriority(safePriority);
        ticket.setSource(source);
        return ticket;
    }

    private LambdaQueryWrapper<Ticket> buildTicketQueryWrapper(TicketPageQuery query) {
        LambdaQueryWrapper<Ticket> wrapper = new LambdaQueryWrapper<>();

        wrapper.like(StringUtils.hasText(query.getTicketNo()),
                Ticket::getTicketNo, query.getTicketNo());
        wrapper.eq(StringUtils.hasText(query.getOrderNo()),
                Ticket::getOrderNo, query.getOrderNo());
        wrapper.eq(StringUtils.hasText(query.getStatus()),
                Ticket::getStatus, query.getStatus());
        wrapper.eq(StringUtils.hasText(query.getType()),
                Ticket::getType, query.getType());
        wrapper.eq(StringUtils.hasText(query.getPriority()),
                Ticket::getPriority, query.getPriority());
        wrapper.eq(StringUtils.hasText(query.getSource()),
                Ticket::getSource, query.getSource());

        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(w -> w.like(Ticket::getTicketNo, keyword)
                    .or()
                    .like(Ticket::getTitle, keyword));
        }

        return wrapper;
    }

    /**
     * 客服工单列表范围：仅 POOL（待认领）或 MINE（我负责的），不可混查。
     */
    private void applyStaffScope(LambdaQueryWrapper<Ticket> wrapper, String scope, Long staffId) {
        String safeScope = StringUtils.hasText(scope) ? scope.trim().toUpperCase() : "";

        switch (safeScope) {
            case "POOL" -> wrapper.isNull(Ticket::getStaffId)
                    .eq(Ticket::getStatus, TicketStatusEnum.PENDING.getCode());
            case "MINE" -> wrapper.eq(Ticket::getStaffId, staffId);
            default -> throw new BusinessException(
                    ResultCodeEnum.PARAM_ERROR.getCode(),
                    "scope 必须为 POOL（待认领）或 MINE（我负责的）"
            );
        }
    }

    private boolean isPendingPoolTicket(Ticket ticket) {
        return ticket.getStaffId() == null
                && TicketStatusEnum.PENDING.getCode().equals(ticket.getStatus());
    }

    private Ticket getTicketRequired(Long id) {
        Ticket ticket = ticketMapper.selectById(id);

        if (ticket == null) {
            throw new BusinessException(
                    ResultCodeEnum.NOT_FOUND.getCode(),
                    "工单不存在"
            );
        }
        return ticket;
    }

    private void checkTicketViewPermission(Ticket ticket) {
        LoginUserInfo currentUser = LoginUserContext.getRequired();

        if (currentUser.hasRole(RoleCodeEnum.ADMIN.getCode())) {
            return;
        }
        if (currentUser.hasRole(RoleCodeEnum.STAFF.getCode())) {
            if (currentUser.getUserId().equals(ticket.getStaffId())) {
                return;
            }
            if (isPendingPoolTicket(ticket)) {
                return;
            }
        }
        if (currentUser.getUserId().equals(ticket.getUserId())) {
            return;
        }
        throw new BusinessException(
                ResultCodeEnum.FORBIDDEN.getCode(),
                "没有权限查看该工单"
        );
    }

    private void checkTicketProcessPermission(Ticket ticket) {
        LoginUserInfo currentUser = LoginUserContext.getRequired();

        if (currentUser.hasRole(RoleCodeEnum.STAFF.getCode())
                && currentUser.getUserId().equals(ticket.getStaffId())) {
            return;
        }
        throw new BusinessException(
                ResultCodeEnum.FORBIDDEN.getCode(),
                "仅负责该工单的客服可以提交处理结果，管理员请使用分配功能"
        );
    }

    private void checkTicketClosePermission(Ticket ticket) {
        LoginUserInfo currentUser = LoginUserContext.getRequired();

        if (currentUser.hasRole(RoleCodeEnum.ADMIN.getCode())) {
            return;
        }
        if (currentUser.hasRole(RoleCodeEnum.STAFF.getCode())
                && currentUser.getUserId().equals(ticket.getStaffId())) {
            return;
        }
        if (currentUser.getUserId().equals(ticket.getUserId())) {
            return;
        }
        throw new BusinessException(
                ResultCodeEnum.FORBIDDEN.getCode(),
                "没有权限关闭该工单"
        );
    }

    private boolean isStaff(Long userId) {
        return hasRole(userId, RoleCodeEnum.STAFF.getCode());
    }

    private boolean isEndUser(Long userId) {
        return hasRole(userId, RoleCodeEnum.USER.getCode());
    }

    private boolean hasRole(Long userId, String roleCode) {
        SysRole role = sysRoleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>()
                        .eq(SysRole::getRoleCode, roleCode)
                        .last("LIMIT 1")
        );
        if (role == null) {
            return false;
        }
        Long count = sysUserRoleMapper.selectCount(
                new LambdaQueryWrapper<SysUserRole>()
                        .eq(SysUserRole::getUserId, userId)
                        .eq(SysUserRole::getRoleId, role.getId())
        );
        return count != null && count > 0;
    }

    /**
     * 单条工单转 VO（详情场景）
     */
    private TicketVO convertToTicketVO(Ticket ticket, boolean includeFlows) {
        Set<Long> userIds = new HashSet<>();
        if (ticket.getUserId() != null) {
            userIds.add(ticket.getUserId());
        }
        if (ticket.getStaffId() != null) {
            userIds.add(ticket.getStaffId());
        }
        Map<Long, SysUser> userMap = batchLoadUserMap(userIds);

        TicketVO vo = convertToTicketVO(ticket, userMap);
        if (includeFlows) {
            List<TicketFlowVO> flows = ticketFlowService.listByTicketId(ticket.getId());
            vo.setFlows(flows);
        }
        return vo;
    }

    /**
     * 列表场景转 VO，避免 N+1 查询
     */
    private List<TicketVO> convertToTicketVOList(List<Ticket> tickets, boolean includeFlows) {
        if (tickets == null || tickets.isEmpty()) {
            return List.of();
        }

        Set<Long> userIds = new HashSet<>();
        for (Ticket t : tickets) {
            if (t.getUserId() != null) {
                userIds.add(t.getUserId());
            }
            if (t.getStaffId() != null) {
                userIds.add(t.getStaffId());
            }
        }
        Map<Long, SysUser> userMap = batchLoadUserMap(userIds);

        return tickets.stream()
                .map(ticket -> {
                    TicketVO vo = convertToTicketVO(ticket, userMap);
                    if (includeFlows) {
                        vo.setFlows(ticketFlowService.listByTicketId(ticket.getId()));
                    }
                    return vo;
                })
                .toList();
    }

    private Map<Long, SysUser> batchLoadUserMap(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<SysUser> users = sysUserMapper.selectByIds(userIds);
        return users.stream().collect(Collectors.toMap(SysUser::getId, u -> u));
    }

    private TicketVO convertToTicketVO(Ticket ticket, Map<Long, SysUser> userMap) {
        TicketVO vo = new TicketVO();

        vo.setId(ticket.getId());
        vo.setTicketNo(ticket.getTicketNo());
        vo.setUserId(ticket.getUserId());
        vo.setUserNickname(getUserNickname(userMap.get(ticket.getUserId())));
        vo.setOrderNo(ticket.getOrderNo());
        vo.setSessionId(ticket.getSessionId());
        vo.setTitle(ticket.getTitle());
        vo.setType(ticket.getType());
        vo.setTypeName(convertTicketTypeName(ticket.getType()));
        vo.setDescription(ticket.getDescription());
        vo.setStatus(ticket.getStatus());
        vo.setStatusName(convertTicketStatusName(ticket.getStatus()));
        vo.setPriority(ticket.getPriority());
        vo.setPriorityName(convertTicketPriorityName(ticket.getPriority()));
        vo.setStaffId(ticket.getStaffId());
        vo.setStaffNickname(ticket.getStaffId() == null
                ? null
                : getUserNickname(userMap.get(ticket.getStaffId())));
        vo.setSource(ticket.getSource());
        vo.setSourceName(convertTicketSourceName(ticket.getSource()));
        vo.setProcessResult(ticket.getProcessResult());
        vo.setCloseReason(ticket.getCloseReason());
        vo.setResolvedTime(ticket.getResolvedTime());
        vo.setClosedTime(ticket.getClosedTime());
        vo.setCreateTime(ticket.getCreateTime());
        vo.setUpdateTime(ticket.getUpdateTime());

        return vo;
    }

    private String generateTicketNo() {
        String time = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        int random = ThreadLocalRandom.current().nextInt(1000, 9999);
        return "TK" + time + random;
    }

    private String resolveType(String type) {
        if (!StringUtils.hasText(type)) {
            return TicketTypeEnum.OTHER.getCode();
        }
        for (TicketTypeEnum item : TicketTypeEnum.values()) {
            if (item.getCode().equals(type)) {
                return item.getCode();
            }
        }
        throw new BusinessException(
                ResultCodeEnum.PARAM_ERROR.getCode(),
                "工单类型不合法：" + type
        );
    }

    private String resolvePriority(String priority) {
        if (!StringUtils.hasText(priority)) {
            return TicketPriorityEnum.MEDIUM.getCode();
        }
        for (TicketPriorityEnum priorityEnum : TicketPriorityEnum.values()) {
            if (priorityEnum.getCode().equals(priority)) {
                return priority;
            }
        }
        throw new BusinessException(
                ResultCodeEnum.PARAM_ERROR.getCode(),
                "工单优先级不合法"
        );
    }

    private String getCurrentUserMainRole() {
        LoginUserInfo currentUser = LoginUserContext.getRequired();
        if (currentUser.hasRole(RoleCodeEnum.ADMIN.getCode())) {
            return RoleCodeEnum.ADMIN.getCode();
        }
        if (currentUser.hasRole(RoleCodeEnum.STAFF.getCode())) {
            return RoleCodeEnum.STAFF.getCode();
        }
        return RoleCodeEnum.USER.getCode();
    }

    private String getUserNickname(SysUser user) {
        if (user == null) {
            return null;
        }
        if (StringUtils.hasText(user.getNickname())) {
            return user.getNickname();
        }
        return user.getUsername();
    }

    private String convertTicketStatusName(String status) {
        if (status == null) {
            return null;
        }
        for (TicketStatusEnum item : TicketStatusEnum.values()) {
            if (item.getCode().equals(status)) {
                return item.getName();
            }
        }
        return "未知状态";
    }

    private String convertTicketPriorityName(String priority) {
        if (priority == null) {
            return null;
        }
        for (TicketPriorityEnum item : TicketPriorityEnum.values()) {
            if (item.getCode().equals(priority)) {
                return item.getName();
            }
        }
        return "未知优先级";
    }

    private String convertTicketTypeName(String type) {
        if (type == null) {
            return null;
        }
        for (TicketTypeEnum item : TicketTypeEnum.values()) {
            if (item.getCode().equals(type)) {
                return item.getName();
            }
        }
        return "未知类型";
    }

    private String convertTicketSourceName(String source) {
        if (source == null) {
            return null;
        }
        for (TicketSourceEnum item : TicketSourceEnum.values()) {
            if (item.getCode().equals(source)) {
                return item.getName();
            }
        }
        return "未知来源";
    }
}
