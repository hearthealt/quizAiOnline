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
import com.quiz.mapper.QuestionMapper;
import com.quiz.mapper.QuestionBankMapper;
import com.quiz.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import static com.quiz.entity.table.CategoryTableDef.CATEGORY;
import static com.quiz.entity.table.QuestionTableDef.QUESTION;
import static com.quiz.entity.table.QuestionBankTableDef.QUESTION_BANK;

@Slf4j
@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryMapper categoryMapper;

    @Autowired
    private QuestionBankMapper questionBankMapper;

    @Autowired
    private QuestionMapper questionMapper;

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
        Integer status = dto.getStatus() != null ? dto.getStatus() : 1;
        ensureValidStatus(status);
        category.setStatus(status);
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
            ensureValidStatus(dto.getStatus());
            ensureCategoryCanBeDisabled(id, dto.getStatus());
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
        long bankCount = questionBankMapper.selectCountByQuery(
                QueryWrapper.create().where(QUESTION_BANK.CATEGORY_ID.eq(id))
        );
        if (bankCount > 0) {
            throw new BizException("分类下存在题库，不能删除");
        }
        categoryMapper.deleteById(id);
        clearCache();
    }

    @Override
    @Transactional
    public void batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            throw new BizException("请选择要删除的分类");
        }
        for (Long id : ids) {
            delete(id);
        }
    }

    @Override
    @Transactional
    public void toggleStatus(Long id, Integer status) {
        ensureValidStatus(status);
        Category category = categoryMapper.selectOneById(id);
        if (category == null) {
            throw new BizException("分类不存在");
        }
        ensureCategoryCanBeDisabled(id, status);
        category.setStatus(status);
        categoryMapper.update(category);
        clearCache();
    }

    @Override
    @Transactional
    public void batchToggleStatus(List<Long> ids, Integer status) {
        ensureValidStatus(status);
        if (ids == null || ids.isEmpty()) {
            throw new BizException("请选择要操作的分类");
        }
        for (Long id : ids) {
            Category category = categoryMapper.selectOneById(id);
            if (category == null) {
                throw new BizException("分类不存在");
            }
            ensureCategoryCanBeDisabled(id, status);
            category.setStatus(status);
            categoryMapper.update(category);
        }
        clearCache();
    }

    private void ensureValidStatus(Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BizException("状态参数无效");
        }
    }

    private void ensureCategoryCanBeDisabled(Long id, Integer status) {
        if (!Integer.valueOf(0).equals(status)) {
            return;
        }
        long activeQuestionCount = questionMapper.selectCountByQuery(
                QueryWrapper.create()
                        .where(QUESTION.STATUS.eq(1))
                        .and(QUESTION.BANK_ID.in(
                                QueryWrapper.create()
                                        .select(QUESTION_BANK.ID)
                                        .from(QUESTION_BANK)
                                        .where(QUESTION_BANK.CATEGORY_ID.eq(id))
                                        .and(QUESTION_BANK.STATUS.eq(1))
                        ))
        );
        if (activeQuestionCount > 0) {
            throw new BizException("分类下存在启用题目，不能禁用");
        }
    }

    private void clearCache() {
        redisTemplate.delete(RedisKey.CATEGORY_LIST);
        clearHotBanksCache();
    }

    private void clearHotBanksCache() {
        Set<Object> cacheKeys = redisTemplate.opsForSet().members(RedisKey.BANK_HOT_KEYS);
        if (cacheKeys != null && !cacheKeys.isEmpty()) {
            redisTemplate.delete(cacheKeys.stream().map(String::valueOf).toList());
        }
        redisTemplate.delete(RedisKey.BANK_HOT_KEYS);
    }

    @SuppressWarnings("unchecked")
    private List<Category> getCachedCategoryList() {
        try {
            Object cached = redisTemplate.opsForValue().get(RedisKey.CATEGORY_LIST);
            if (cached instanceof List<?> list) {
                return (List<Category>) list;
            }
        } catch (SerializationException e) {
            log.warn("分类列表缓存反序列化失败，已清除脏缓存", e);
            redisTemplate.delete(RedisKey.CATEGORY_LIST);
        }
        return null;
    }
}
