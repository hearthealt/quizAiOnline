package com.quiz.controller.app;

import com.quiz.common.result.PageResult;
import com.quiz.common.result.R;
import com.quiz.config.StpKit;
import com.quiz.service.FavoriteService;
import com.quiz.vo.app.FavoriteVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "小程序-收藏")
@RestController
@RequestMapping("/api/app/favorite")
@RequiredArgsConstructor
public class AppFavoriteController {

    private final FavoriteService favoriteService;

    @Operation(summary = "收藏/取消收藏")
    @PostMapping("/toggle")
    public R<Boolean> toggle(@RequestParam Long questionId) {
        Long userId = StpKit.APP.getLoginIdAsLong();
        return R.ok(favoriteService.toggle(userId, questionId));
    }

    @Operation(summary = "收藏列表")
    @GetMapping("/list")
    public R<PageResult<FavoriteVO>> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                          @RequestParam(defaultValue = "10") Integer pageSize) {
        Long userId = StpKit.APP.getLoginIdAsLong();
        return R.ok(favoriteService.list(userId, pageNum, pageSize));
    }

    @Operation(summary = "检查是否收藏")
    @GetMapping("/check/{questionId}")
    public R<Boolean> check(@PathVariable Long questionId) {
        Long userId = StpKit.APP.getLoginIdAsLong();
        return R.ok(favoriteService.isFavorite(userId, questionId));
    }

    @Operation(summary = "批量取消收藏")
    @DeleteMapping("/batch")
    public R<Void> batchRemove(@RequestBody List<Long> questionIds) {
        Long userId = StpKit.APP.getLoginIdAsLong();
        favoriteService.batchRemove(userId, questionIds);
        return R.ok();
    }
}
