package com.quiz.service;

import com.quiz.common.result.PageResult;
import com.quiz.dto.admin.VipPlanDTO;
import com.quiz.dto.app.CreateVipOrderDTO;
import com.quiz.dto.app.PayVipOrderDTO;
import com.quiz.entity.VipOrder;
import com.quiz.entity.VipPlan;
import com.quiz.vo.admin.VipStatsVO;
import com.quiz.vo.app.VipInfoVO;

import java.util.List;
import java.util.Map;

public interface VipService {

    List<VipPlan> getPlans();

    VipInfoVO getVipStatus(Long userId);

    VipOrder createOrder(Long userId, CreateVipOrderDTO dto);

    VipOrder payOrder(Long orderId, PayVipOrderDTO dto, Long userId);

    PageResult<VipOrder> getUserOrders(Long userId, Integer pageNum, Integer pageSize);

    PageResult<VipPlan> adminPlanList();

    void createPlan(VipPlanDTO dto);

    void updatePlan(Long id, VipPlanDTO dto);

    void deletePlan(Long id);

    void togglePlanStatus(Long id, Integer status);

    PageResult<Map<String, Object>> adminOrderList(String keyword, Integer status, Integer pageNum, Integer pageSize);

    void approveOrder(Long orderId, Long adminId);

    void rejectOrder(Long orderId, Long adminId);

    VipStatsVO getVipStats();
}
