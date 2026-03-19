package com.quiz.controller.admin;

import com.quiz.common.result.R;
import com.quiz.dto.admin.VipPlanDTO;
import com.quiz.service.VipService;
import com.quiz.config.StpKit;
import com.quiz.vo.admin.VipStatsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "VIP管理")
@RestController
@RequestMapping("/api/admin/vip")
@RequiredArgsConstructor
public class AdminVipController {

    private final VipService vipService;

    @Operation(summary = "套餐列表")
    @GetMapping("/plan/list")
    public R<?> planList() {
        return R.ok(vipService.adminPlanList());
    }

    @Operation(summary = "创建套餐")
    @PostMapping("/plan")
    public R<Void> createPlan(@RequestBody VipPlanDTO dto) {
        vipService.createPlan(dto);
        return R.ok();
    }

    @Operation(summary = "更新套餐")
    @PutMapping("/plan/{id}")
    public R<Void> updatePlan(@PathVariable Long id, @RequestBody VipPlanDTO dto) {
        vipService.updatePlan(id, dto);
        return R.ok();
    }

    @Operation(summary = "删除套餐")
    @DeleteMapping("/plan/{id}")
    public R<Void> deletePlan(@PathVariable Long id) {
        vipService.deletePlan(id);
        return R.ok();
    }

    @Operation(summary = "切换套餐状态")
    @PutMapping("/plan/{id}/status")
    public R<Void> togglePlanStatus(@PathVariable Long id, @RequestParam Integer status) {
        vipService.togglePlanStatus(id, status);
        return R.ok();
    }

    @Operation(summary = "订单列表")
    @GetMapping("/order/list")
    public R<?> orderList(@RequestParam(required = false) String keyword,
                          @RequestParam(required = false) Integer status,
                          @RequestParam(defaultValue = "1") Integer pageNum,
                          @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.ok(vipService.adminOrderList(keyword, status, pageNum, pageSize));
    }

    @Operation(summary = "审核订单")
    @PutMapping("/order/{id}/approve")
    public R<Void> approveOrder(@PathVariable Long id) {
        Long adminId = StpKit.ADMIN.getLoginIdAsLong();
        vipService.approveOrder(id, adminId);
        return R.ok();
    }

    @Operation(summary = "驳回订单")
    @PutMapping("/order/{id}/reject")
    public R<Void> rejectOrder(@PathVariable Long id) {
        Long adminId = StpKit.ADMIN.getLoginIdAsLong();
        vipService.rejectOrder(id, adminId);
        return R.ok();
    }

    @Operation(summary = "VIP统计")
    @GetMapping("/stats")
    public R<VipStatsVO> stats() {
        return R.ok(vipService.getVipStats());
    }
}
