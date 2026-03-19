package com.quiz.service;

import com.quiz.common.result.PageResult;
import com.quiz.dto.admin.CategoryDTO;
import com.quiz.entity.Category;

import java.util.List;

public interface CategoryService {

    PageResult<Category> list(String keyword, Integer pageNum, Integer pageSize);

    List<Category> listAll();

    void create(CategoryDTO dto);

    void update(Long id, CategoryDTO dto);

    void delete(Long id);

    void toggleStatus(Long id, Integer status);
}
