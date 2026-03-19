package com.quiz.service;

import com.quiz.common.result.PageResult;
import com.quiz.vo.app.FavoriteVO;

import java.util.List;
import java.util.Map;

public interface FavoriteService {

    boolean toggle(Long userId, Long questionId);

    PageResult<FavoriteVO> list(Long userId, Integer pageNum, Integer pageSize);

    boolean isFavorite(Long userId, Long questionId);

    void batchRemove(Long userId, List<Long> questionIds);

    PageResult<Map<String, Object>> adminList(String keyword, Integer pageNum, Integer pageSize);
}
