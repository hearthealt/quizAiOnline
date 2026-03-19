package com.quiz.controller.admin;

import com.quiz.common.result.R;
import com.quiz.service.FavoriteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "收藏管理")
@RestController
@RequestMapping("/api/admin/favorite")
@RequiredArgsConstructor
public class AdminFavoriteController {

    private final FavoriteService favoriteService;

    @Operation(summary = "收藏列表")
    @GetMapping("/list")
    public R<?> list(@RequestParam(required = false) String keyword,
                     @RequestParam(defaultValue = "1") Integer pageNum,
                     @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.ok(favoriteService.adminList(keyword, pageNum, pageSize));
    }
}
