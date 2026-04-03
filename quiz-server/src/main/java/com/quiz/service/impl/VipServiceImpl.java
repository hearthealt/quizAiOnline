package com.quiz.service.impl;

import cn.hutool.core.util.IdUtil;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.quiz.common.exception.BizException;
import com.quiz.common.result.PageResult;
import com.quiz.dto.admin.VipPlanDTO;
import com.quiz.dto.app.CreateVipOrderDTO;
import com.quiz.dto.app.PayVipOrderDTO;
import com.quiz.entity.User;
import com.quiz.entity.VipOrder;
import com.quiz.entity.VipPlan;
import com.quiz.mapper.UserMapper;
import com.quiz.mapper.VipOrderMapper;
import com.quiz.mapper.VipPlanMapper;
import com.quiz.service.VipService;
import com.quiz.vo.admin.VipStatsVO;
import com.quiz.vo.app.VipInfoVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.quiz.entity.table.VipOrderTableDef.VIP_ORDER;
import static com.quiz.entity.table.VipPlanTableDef.VIP_PLAN;
import static com.quiz.entity.table.UserTableDef.USER;

@Service
@RequiredArgsConstructor
public class VipServiceImpl implements VipService {

    private final VipPlanMapper vipPlanMapper;
    private final VipOrderMapper vipOrderMapper;
    private final UserMapper userMapper;

    @Override
    public List<VipPlan> getPlans() {
        return vipPlanMapper.selectListByQuery(
                QueryWrapper.create().where(VIP_PLAN.STATUS.eq(1))
                        .orderBy(VIP_PLAN.SORT, true));
    }

    @Override
    public VipInfoVO getVipStatus(Long userId) {
        User user = userMapper.selectOneById(userId);
        if (user == null) throw new BizException("用户不存在");

        VipInfoVO vo = new VipInfoVO();
        boolean isVip = user.getIsVip() == 1 && user.getVipExpireTime() != null
                && user.getVipExpireTime().isAfter(LocalDateTime.now());
        if (!isVip && user.getIsVip() == 1) {
            user.setIsVip(0);
            userMapper.update(user);
        }
        vo.setIsVip(isVip);
        vo.setExpireTime(user.getVipExpireTime());
        return vo;
    }

    @Override
    @Transactional
    public VipOrder createOrder(Long userId, CreateVipOrderDTO dto) {
        VipPlan plan = vipPlanMapper.selectOneById(dto.getPlanId());
        if (plan == null || plan.getStatus() != 1) throw new BizException("套餐不存在或已下架");

        VipOrder order = new VipOrder();
        order.setOrderNo(IdUtil.getSnowflakeNextIdStr());
        order.setUserId(userId);
        order.setPlanId(plan.getId());
        order.setPlanName(plan.getName());
        order.setAmount(plan.getPrice());
        order.setDuration(plan.getDuration());
        order.setStatus(0);
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        vipOrderMapper.insert(order);
        return order;
    }

    @Override
    @Transactional
    public VipOrder payOrder(Long orderId, PayVipOrderDTO dto, Long userId) {
        VipOrder order = vipOrderMapper.selectOneById(orderId);
        if (order == null || !order.getUserId().equals(userId)) throw new BizException("订单不存在");
        if (order.getStatus() != 0) throw new BizException("订单状态异常");

        // 模拟支付成功
        order.setStatus(1);
        order.setPaymentMethod(dto.getPaymentMethod());
        order.setPaidTime(LocalDateTime.now());

        User user = userMapper.selectOneById(userId);
        LocalDateTime startTime;
        if (user.getIsVip() == 1 && user.getVipExpireTime() != null
                && user.getVipExpireTime().isAfter(LocalDateTime.now())) {
            startTime = user.getVipExpireTime();
        } else {
            startTime = LocalDateTime.now();
        }
        LocalDateTime endTime = startTime.plusDays(order.getDuration());

        order.setVipStartTime(startTime);
        order.setVipEndTime(endTime);
        order.setUpdateTime(LocalDateTime.now());
        vipOrderMapper.update(order);

        user.setIsVip(1);
        user.setVipExpireTime(endTime);
        userMapper.update(user);

        return order;
    }

    @Override
    public PageResult<VipOrder> getUserOrders(Long userId, Integer pageNum, Integer pageSize) {
        QueryWrapper qw = QueryWrapper.create().where(VIP_ORDER.USER_ID.eq(userId))
                .orderBy(VIP_ORDER.CREATE_TIME, false);
        Page<VipOrder> page = vipOrderMapper.paginate(Page.of(pageNum, pageSize), qw);
        return new PageResult<>(page.getRecords(), page.getTotalRow(), pageNum, pageSize);
    }

    @Override
    public PageResult<VipPlan> adminPlanList() {
        List<VipPlan> list = vipPlanMapper.selectListByQuery(
                QueryWrapper.create().orderBy(VIP_PLAN.SORT, true));
        return new PageResult<>(list, (long) list.size(), 1, list.size());
    }

    @Override
    public void createPlan(VipPlanDTO dto) {
        VipPlan plan = new VipPlan();
        copyPlanFields(plan, dto);
        vipPlanMapper.insert(plan);
    }

    @Override
    public void updatePlan(Long id, VipPlanDTO dto) {
        VipPlan plan = vipPlanMapper.selectOneById(id);
        if (plan == null) throw new BizException("套餐不存在");
        copyPlanFields(plan, dto);
        vipPlanMapper.update(plan);
    }

    @Override
    public void deletePlan(Long id) {
        vipPlanMapper.deleteById(id);
    }

    @Override
    public void togglePlanStatus(Long id, Integer status) {
        VipPlan plan = vipPlanMapper.selectOneById(id);
        if (plan == null) throw new BizException("套餐不存在");
        plan.setStatus(status);
        vipPlanMapper.update(plan);
    }

    @Override
    public PageResult<Map<String, Object>> adminOrderList(String keyword, Integer status, Integer pageNum, Integer pageSize) {
        // 关键词搜索用户
        List<Long> matchedUserIds = null;
        if (StringUtils.hasText(keyword)) {
            QueryWrapper userQuery = QueryWrapper.create()
                    .select(USER.ID)
                    .where(USER.NICKNAME.like(keyword).or(USER.PHONE.like(keyword)));
            List<User> matchedUsers = userMapper.selectListByQuery(userQuery);
            matchedUserIds = matchedUsers.stream().map(User::getId).toList();
            if (matchedUserIds.isEmpty()) {
                return PageResult.empty(pageNum, pageSize);
            }
        }

        QueryWrapper qw = QueryWrapper.create();
        if (matchedUserIds != null) qw.and(VIP_ORDER.USER_ID.in(matchedUserIds));
        if (status != null) qw.and(VIP_ORDER.STATUS.eq(status));
        qw.orderBy(VIP_ORDER.CREATE_TIME, false);
        Page<VipOrder> page = vipOrderMapper.paginate(Page.of(pageNum, pageSize), qw);

        if (page.getRecords().isEmpty()) {
            return PageResult.empty(pageNum, pageSize);
        }

        // 批量查询用户
        Set<Long> userIds = page.getRecords().stream().map(VipOrder::getUserId).collect(Collectors.toSet());
        Map<Long, User> userMap = userIds.isEmpty() ? Collections.emptyMap() :
                userMapper.selectListByIds(userIds).stream().collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));

        List<Map<String, Object>> resultList = page.getRecords().stream().map(order -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", order.getId());
            map.put("orderNo", order.getOrderNo());
            map.put("userId", order.getUserId());
            map.put("userNickname", userMap.getOrDefault(order.getUserId(), new User()).getNickname());
            map.put("userPhone", userMap.getOrDefault(order.getUserId(), new User()).getPhone());
            map.put("planId", order.getPlanId());
            map.put("planName", order.getPlanName());
            map.put("amount", order.getAmount());
            map.put("duration", order.getDuration());
            map.put("status", order.getStatus());
            map.put("paymentMethod", order.getPaymentMethod());
            map.put("paidTime", order.getPaidTime());
            map.put("vipStartTime", order.getVipStartTime());
            map.put("vipEndTime", order.getVipEndTime());
            map.put("createTime", order.getCreateTime());
            return map;
        }).toList();

        return new PageResult<>(resultList, page.getTotalRow(), pageNum, pageSize);
    }

    @Override
    @Transactional
    public void approveOrder(Long orderId, Long adminId) {
        VipOrder order = vipOrderMapper.selectOneById(orderId);
        if (order == null) throw new BizException("订单不存在");
        if (order.getStatus() != 0) throw new BizException("订单状态异常");

        // 人工审核通过
        order.setStatus(1);
        order.setPaymentMethod("manual");
        order.setPaidTime(LocalDateTime.now());

        User user = userMapper.selectOneById(order.getUserId());
        if (user == null) throw new BizException("用户不存在");
        LocalDateTime startTime;
        if (user.getIsVip() == 1 && user.getVipExpireTime() != null
                && user.getVipExpireTime().isAfter(LocalDateTime.now())) {
            startTime = user.getVipExpireTime();
        } else {
            startTime = LocalDateTime.now();
        }
        LocalDateTime endTime = startTime.plusDays(order.getDuration());

        order.setVipStartTime(startTime);
        order.setVipEndTime(endTime);
        order.setUpdateTime(LocalDateTime.now());
        vipOrderMapper.update(order);

        user.setIsVip(1);
        user.setVipExpireTime(endTime);
        userMapper.update(user);
    }

    @Override
    @Transactional
    public void rejectOrder(Long orderId, Long adminId) {
        VipOrder order = vipOrderMapper.selectOneById(orderId);
        if (order == null) throw new BizException("订单不存在");
        if (order.getStatus() != 0) throw new BizException("订单状态异常");

        order.setStatus(2);
        order.setPaymentMethod("rejected");
        order.setUpdateTime(LocalDateTime.now());
        vipOrderMapper.update(order);
    }

    @Override
    public VipStatsVO getVipStats() {
        VipStatsVO vo = new VipStatsVO();
        vo.setTotalVipUsers(userMapper.selectCountByQuery(
                QueryWrapper.create().where("is_vip = 1")));
        vo.setTotalRevenue(Optional.ofNullable(vipOrderMapper.sumPaidAmount()).orElse(BigDecimal.ZERO));
        vo.setMonthRevenue(Optional.ofNullable(
                vipOrderMapper.sumPaidAmountFrom(
                        LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0)
                )
        ).orElse(BigDecimal.ZERO));
        vo.setActiveVipCount(vo.getTotalVipUsers());
        return vo;
    }

    private void copyPlanFields(VipPlan plan, VipPlanDTO dto) {
        plan.setName(dto.getName());
        plan.setDuration(dto.getDuration());
        plan.setPrice(dto.getPrice());
        plan.setOriginalPrice(dto.getOriginalPrice());
        plan.setDescription(dto.getDescription());
        plan.setSort(dto.getSort());
        if (dto.getStatus() != null) plan.setStatus(dto.getStatus());
    }
}
