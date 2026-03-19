package com.quiz.controller.app;

import com.quiz.common.result.PageResult;
import com.quiz.common.result.R;
import com.quiz.config.StpKit;
import com.quiz.dto.app.CreateVipOrderDTO;
import com.quiz.dto.app.PayVipOrderDTO;
import com.quiz.entity.VipOrder;
import com.quiz.entity.VipPlan;
import com.quiz.service.VipService;
import com.quiz.vo.app.VipInfoVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "小程序-会员")
@RestController
@RequestMapping("/api/app/vip")
@RequiredArgsConstructor
public class AppVipController {

    private final VipService vipService;

    @Operation(summary = "会员套餐列表")
    @GetMapping("/plans")
    public R<List<VipPlan>> plans() {
        return R.ok(vipService.getPlans());
    }

    @Operation(summary = "会员状态")
    @GetMapping("/status")
    public R<VipInfoVO> status() {
        Long userId = StpKit.APP.getLoginIdAsLong();
        return R.ok(vipService.getVipStatus(userId));
    }

    @Operation(summary = "创建订单")
    @PostMapping("/order")
    public R<VipOrder> createOrder(@RequestBody CreateVipOrderDTO dto) {
        Long userId = StpKit.APP.getLoginIdAsLong();
        return R.ok(vipService.createOrder(userId, dto));
    }

    @Operation(summary = "支付订单")
    @PostMapping("/order/{id}/pay")
    public R<VipOrder> payOrder(@PathVariable Long id, @RequestBody PayVipOrderDTO dto) {
        Long userId = StpKit.APP.getLoginIdAsLong();
        return R.ok(vipService.payOrder(id, dto, userId));
    }

    @Operation(summary = "订单列表")
    @GetMapping("/orders")
    public R<PageResult<VipOrder>> orders(@RequestParam(defaultValue = "1") Integer pageNum,
                                           @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = StpKit.APP.getLoginIdAsLong();
        return R.ok(vipService.getUserOrders(userId, pageNum, pageSize));
    }
}
