package com.quiz.service.impl;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import static com.mybatisflex.core.query.QueryMethods.max;
import com.quiz.common.constant.RedisKey;
import com.quiz.common.exception.BizException;
import com.quiz.common.result.PageResult;
import com.quiz.dto.admin.QuestionBankDTO;
import com.quiz.entity.PracticeDetail;
import com.quiz.entity.PracticeRecord;
import com.quiz.entity.QuestionBank;
import com.quiz.mapper.PracticeDetailMapper;
import com.quiz.mapper.PracticeRecordMapper;
import com.quiz.mapper.QuestionBankMapper;
import com.quiz.service.QuestionBankService;
import com.quiz.vo.app.BankDetailVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import static com.quiz.entity.table.PracticeDetailTableDef.PRACTICE_DETAIL;
import static com.quiz.entity.table.PracticeRecordTableDef.PRACTICE_RECORD;
import static com.quiz.entity.table.QuestionBankTableDef.QUESTION_BANK;

@Slf4j
@Service
public class QuestionBankServiceImpl implements QuestionBankService {

    @Autowired
    private QuestionBankMapper questionBankMapper;

    @Autowired
    private PracticeRecordMapper practiceRecordMapper;

    @Autowired
    private PracticeDetailMapper practiceDetailMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public PageResult<QuestionBank> pageList(Long categoryId, Integer pageNum, Integer pageSize) {
        QueryWrapper query = QueryWrapper.create()
                .where(QUESTION_BANK.CATEGORY_ID.eq(categoryId).when(categoryId != null))
                .and(QUESTION_BANK.STATUS.eq(1))
                .orderBy(QUESTION_BANK.SORT.asc(), QUESTION_BANK.CREATE_TIME.desc());
        Page<QuestionBank> page = questionBankMapper.paginate(pageNum, pageSize, query);
        return PageResult.of(page.getRecords(), page.getTotalRow(), pageNum, pageSize);
    }

    @Override
    public BankDetailVO getDetail(Long id, Long userId) {
        BankDetailVO vo = getCachedBankDetail(id);
        if (vo == null) {
            QuestionBank bank = questionBankMapper.selectOneById(id);
            if (bank == null) {
                throw new BizException("题库不存在");
            }
            vo = buildBaseDetail(bank);
            redisTemplate.opsForValue().set(RedisKey.BANK_DETAIL + id, vo, Duration.ofMinutes(30));
        }

        // 查询用户对该题库的当前练习进度（进行中的练习记录）
        if (userId != null) {
            PracticeRecord record = practiceRecordMapper.selectOneByQuery(
                    QueryWrapper.create()
                            .where(PRACTICE_RECORD.USER_ID.eq(userId))
                            .and(PRACTICE_RECORD.BANK_ID.eq(id))
                            .and(PRACTICE_RECORD.STATUS.eq(0))
                            .orderBy(PRACTICE_RECORD.CREATE_TIME.desc())
            );
            if (record != null) {
                vo.setPracticeTotalCount(record.getTotalCount());
                QueryWrapper detailQuery = QueryWrapper.create()
                        .select(PRACTICE_DETAIL.QUESTION_ID, PRACTICE_DETAIL.IS_CORRECT)
                        .where(PRACTICE_DETAIL.RECORD_ID.eq(record.getId()));
                List<PracticeDetail> details = practiceDetailMapper.selectListByQuery(detailQuery);
                long answeredCount = details.stream()
                        .map(PracticeDetail::getQuestionId)
                        .distinct()
                        .count();
                vo.setUserProgress((int) answeredCount);

                long totalCount = details.size();
                long correctCount = details.stream()
                        .filter(d -> d.getIsCorrect() != null && d.getIsCorrect() == 1)
                        .count();
                vo.setUserCorrectRate(totalCount > 0 ? Math.round(correctCount * 10000.0 / totalCount) / 100.0 : 0.0);
            } else {
                vo.setPracticeTotalCount(0);
                vo.setUserProgress(0);
                vo.setUserCorrectRate(0.0);
            }
        } else {
            vo.setPracticeTotalCount(0);
            vo.setUserProgress(0);
            vo.setUserCorrectRate(0.0);
        }

        return vo;
    }

    @SuppressWarnings("unchecked")
    private BankDetailVO getCachedBankDetail(Long bankId) {
        String cacheKey = RedisKey.BANK_DETAIL + bankId;
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof BankDetailVO bankDetailVO) {
                return bankDetailVO;
            }
        } catch (SerializationException e) {
            log.warn("题库详情缓存反序列化失败，已清除脏缓存，bankId={}", bankId, e);
            redisTemplate.delete(cacheKey);
        }
        return null;
    }

    private BankDetailVO buildBaseDetail(QuestionBank bank) {
        if (bank == null) {
            throw new BizException("题库不存在");
        }

        BankDetailVO vo = new BankDetailVO();
        vo.setId(bank.getId());
        vo.setCategoryId(bank.getCategoryId());
        vo.setName(bank.getName());
        vo.setDescription(bank.getDescription());
        vo.setCover(bank.getCover());
        vo.setQuestionCount(bank.getQuestionCount());
        vo.setPracticeCount(bank.getPracticeCount());
        vo.setPassScore(bank.getPassScore());
        vo.setPracticeTotalCount(0);
        vo.setUserProgress(0);
        vo.setUserCorrectRate(0.0);
        return vo;
    }

    @Override
    @Transactional
    public void create(QuestionBankDTO dto) {
        QuestionBank bank = new QuestionBank();
        bank.setCategoryId(dto.getCategoryId());
        bank.setName(dto.getName());
        bank.setDescription(dto.getDescription());
        bank.setCover(dto.getCover());
        bank.setPassScore(dto.getPassScore());
        if (dto.getSort() != null && dto.getSort() > 0) {
            bank.setSort(dto.getSort());
        } else {
            Integer maxSort = questionBankMapper.selectObjectByQueryAs(
                    QueryWrapper.create().select(max(QUESTION_BANK.SORT)), Integer.class);
            bank.setSort(maxSort != null ? maxSort + 1 : 1);
        }
        bank.setStatus(dto.getStatus() != null ? dto.getStatus() : 1);
        bank.setQuestionCount(0);
        bank.setPracticeCount(0);
        questionBankMapper.insert(bank);
        clearCache(bank.getId());
    }

    @Override
    @Transactional
    public void update(Long id, QuestionBankDTO dto) {
        QuestionBank bank = questionBankMapper.selectOneById(id);
        if (bank == null) {
            throw new BizException("题库不存在");
        }
        if (dto.getCategoryId() != null) {
            bank.setCategoryId(dto.getCategoryId());
        }
        if (dto.getName() != null) {
            bank.setName(dto.getName());
        }
        if (dto.getDescription() != null) {
            bank.setDescription(dto.getDescription());
        }
        if (dto.getCover() != null) {
            bank.setCover(dto.getCover());
        }
        if (dto.getPassScore() != null) {
            bank.setPassScore(dto.getPassScore());
        }
        if (dto.getSort() != null) {
            bank.setSort(dto.getSort());
        }
        if (dto.getStatus() != null) {
            bank.setStatus(dto.getStatus());
        }
        questionBankMapper.update(bank);
        clearCache(bank.getId());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        QuestionBank bank = questionBankMapper.selectOneById(id);
        if (bank == null) {
            throw new BizException("题库不存在");
        }
        questionBankMapper.deleteById(id);
        clearCache(id);
    }

    @Override
    public List<QuestionBank> hotBanks(Integer limit) {
        int finalLimit = limit != null ? limit : 10;
        List<QuestionBank> cached = getCachedHotBanks(finalLimit);
        if (cached != null) {
            return cached;
        }

        QueryWrapper query = QueryWrapper.create()
                .where(QUESTION_BANK.STATUS.eq(1))
                .orderBy(QUESTION_BANK.QUESTION_COUNT.desc())
                .limit(finalLimit);
        List<QuestionBank> banks = questionBankMapper.selectListByQuery(query);
        String cacheKey = buildHotBanksKey(finalLimit);
        redisTemplate.opsForValue().set(cacheKey, banks, Duration.ofMinutes(15));
        redisTemplate.opsForSet().add(RedisKey.BANK_HOT_KEYS, cacheKey);
        redisTemplate.expire(RedisKey.BANK_HOT_KEYS, Duration.ofHours(1));
        return banks;
    }

    private void clearCache(Long bankId) {
        clearHotBanksCache();
        if (bankId != null) {
            redisTemplate.delete(RedisKey.BANK_DETAIL + bankId);
            redisTemplate.delete(RedisKey.BANK_QUESTIONS + bankId);
        }
    }

    @SuppressWarnings("unchecked")
    private List<QuestionBank> getCachedHotBanks(int limit) {
        String cacheKey = buildHotBanksKey(limit);
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof List<?> list) {
                return (List<QuestionBank>) list;
            }
        } catch (SerializationException e) {
            log.warn("热门题库缓存反序列化失败，已清除脏缓存，limit={}", limit, e);
            redisTemplate.delete(cacheKey);
        }
        return null;
    }

    private String buildHotBanksKey(int limit) {
        return RedisKey.BANK_HOT + ":" + limit;
    }

    private void clearHotBanksCache() {
        Set<Object> cacheKeys = redisTemplate.opsForSet().members(RedisKey.BANK_HOT_KEYS);
        if (cacheKeys != null && !cacheKeys.isEmpty()) {
            redisTemplate.delete(cacheKeys.stream().map(String::valueOf).toList());
        }
        redisTemplate.delete(RedisKey.BANK_HOT_KEYS);
    }
}
