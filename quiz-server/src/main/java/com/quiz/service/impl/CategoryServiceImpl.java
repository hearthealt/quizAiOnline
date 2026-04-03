package com.quiz.service.impl;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import static com.mybatisflex.core.query.QueryMethods.max;
import com.quiz.common.constant.RedisKey;
import com.quiz.common.exception.BizException;
import com.quiz.common.result.PageResult;
import com.quiz.dto.admin.CategoryDTO;
import com.quiz.entity.Category;
import com.quiz.mapper.CategoryMapper;
import com.quiz.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

import static com.quiz.entity.table.CategoryTableDef.CATEGORY;

@Slf4j
@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public PageResult<Category> list(String keyword, Integer pageNum, Integer pageSize) {
        QueryWrapper query = QueryWrapper.create()
                .where(CATEGORY.NAME.like(keyword).when(keyword != null && !keyword.isEmpty()))
                .orderBy(CATEGORY.SORT.asc(), CATEGORY.CREATE_TIME.desc());
        Page<Category> page = categoryMapper.paginate(pageNum, pageSize, query);
        return PageResult.of(page.getRecords(), page.getTotalRow(), pageNum, pageSize);
    }

    @Override
    public List<Category> listAll() {
        List<Category> cached = getCachedCategoryList();
        if (cached != null) {
            return cached;
        }

        QueryWrapper query = QueryWrapper.create()
                .where(CATEGORY.STATUS.eq(1))
                .orderBy(CATEGORY.SORT.asc());
        List<Category> categories = categoryMapper.selectListByQuery(query);
        redisTemplate.opsForValue().set(RedisKey.CATEGORY_LIST, categories, Duration.ofMinutes(30));
        return categories;
    }

    @Override
    @Transactional
    public void create(CategoryDTO dto) {
        Category category = new Category();
        category.setName(dto.getName());
        category.setIcon(dto.getIcon());
        if (dto.getSort() != null && dto.getSort() > 0) {
            category.setSort(dto.getSort());
        } else {
            Integer maxSort = categoryMapper.selectObjectByQueryAs(
                    QueryWrapper.create().select(max(CATEGORY.SORT)), Integer.class);
            category.setSort(maxSort != null ? maxSort + 1 : 1);
        }
        category.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        categoryMapper.insert(category);
        clearCache();
    }

    @Override
    @Transactional
    public void update(Long id, CategoryDTO dto) {
        Category category = categoryMapper.selectOneById(id);
        if (category == null) {
            throw new BizException("分类不存在");
        }
        if (dto.getName() != null) {
            category.setName(dto.getName());
        }
        if (dto.getIcon() != null) {
            category.setIcon(dto.getIcon());
        }
        if (dto.getSort() != null) {
            category.setSort(dto.getSort());
        }
        if (dto.getStatus() != null) {
            category.setStatus(dto.getStatus());
        }
        categoryMapper.update(category);
        clearCache();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Category category = categoryMapper.selectOneById(id);
        if (category == null) {
            throw new BizException("分类不存在");
        }
        categoryMapper.deleteById(id);
        clearCache();
    }

    @Override
    @Transactional
    public void toggleStatus(Long id, Integer status) {
        Category category = categoryMapper.selectOneById(id);
        if (category == null) {
            throw new BizException("分类不存在");
        }
        category.setStatus(status);
        categoryMapper.update(category);
        clearCache();
    }

    private void clearCache() {
        redisTemplate.delete(RedisKey.CATEGORY_LIST);
    }

    @SuppressWarnings("unchecked")
    private List<Category> getCachedCategoryList() {
        Object cached = redisTemplate.opsForValue().get(RedisKey.CATEGORY_LIST);
        if (cached instanceof List<?> list) {
            return (List<Category>) list;
        }
        return null;
    }
}
