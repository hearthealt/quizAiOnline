package com.quiz.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybatisflex.core.query.QueryWrapper;
import com.quiz.common.exception.BizException;
import com.quiz.dto.app.StartPracticeDTO;
import com.quiz.dto.app.SubmitAnswerDTO;
import com.quiz.entity.PracticeDetail;
import com.quiz.entity.PracticeRecord;
import com.quiz.entity.Question;
import com.quiz.entity.QuestionOption;
import com.quiz.entity.QuestionBank;
import com.quiz.mapper.PracticeDetailMapper;
import com.quiz.mapper.PracticeRecordMapper;
import com.quiz.mapper.QuestionMapper;
import com.quiz.mapper.QuestionBankMapper;
import com.quiz.mapper.QuestionOptionMapper;
import com.quiz.service.QuestionBankService;
import com.quiz.service.PracticeService;
import com.quiz.util.AppViewMapper;
import com.quiz.service.WrongQuestionService;
import com.quiz.vo.app.PracticeResultVO;
import com.quiz.vo.app.QuestionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.*;
import java.util.Locale;
import java.util.stream.Collectors;

import static com.quiz.entity.table.PracticeDetailTableDef.PRACTICE_DETAIL;
import static com.quiz.entity.table.PracticeRecordTableDef.PRACTICE_RECORD;
import static com.quiz.entity.table.QuestionTableDef.QUESTION;
import static com.quiz.entity.table.QuestionOptionTableDef.QUESTION_OPTION;
import static com.quiz.entity.table.WrongQuestionTableDef.WRONG_QUESTION;

@Slf4j
@Service
@RequiredArgsConstructor
public class PracticeServiceImpl implements PracticeService {

    private static final String PRACTICE_KEY_PREFIX = "practice:questions:";
    private static final boolean ALLOW_WRONG_RETRY = true;
    private static final boolean SHOW_ANALYSIS = true;

    private final PracticeRecordMapper practiceRecordMapper;
    private final PracticeDetailMapper practiceDetailMapper;
    private final QuestionMapper questionMapper;
    private final QuestionBankMapper questionBankMapper;
    private final QuestionOptionMapper questionOptionMapper;
    private final WrongQuestionService wrongQuestionService;
    private final QuestionBankService questionBankService;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public PracticeRecord startPractice(Long userId, StartPracticeDTO dto) {
        Long bankId = dto.getBankId();
        String mode = dto.getMode();
        boolean restart = dto.getRestart() != null && dto.getRestart();

        if (restart) {
            PracticeRecord ongoing = practiceRecordMapper.selectOneByQuery(
                    QueryWrapper.create()
                            .where(PRACTICE_RECORD.USER_ID.eq(userId))
                            .and(PRACTICE_RECORD.BANK_ID.eq(bankId))
                            .and(PRACTICE_RECORD.MODE.eq(mode))
                            .and(PRACTICE_RECORD.STATUS.eq(0))
                            .orderBy(PRACTICE_RECORD.CREATE_TIME.desc())
            );
            if (ongoing != null) {
                ongoing.setStatus(1);
                practiceRecordMapper.update(ongoing);
            }
        }

        if (!restart) {
            PracticeRecord existing = practiceRecordMapper.selectOneByQuery(
                    QueryWrapper.create()
                            .where(PRACTICE_RECORD.USER_ID.eq(userId))
                            .and(PRACTICE_RECORD.BANK_ID.eq(bankId))
                            .and(PRACTICE_RECORD.MODE.eq(mode))
                            .and(PRACTICE_RECORD.STATUS.eq(0))
                            .orderBy(PRACTICE_RECORD.CREATE_TIME.desc())
            );
            if (existing != null) {
                ensureQuestionIds(existing, userId);
                cacheQuestionIds(existing.getId(), existing.getQuestionIds());
                return existing;
            }
        }

        // 错题重练开关检查
        if ("WRONG".equals(mode)) {
            if (!ALLOW_WRONG_RETRY) {
                throw new BizException("错题重练功能已关闭");
            }
        }

        List<Long> questionIds = queryQuestionIds(userId, bankId, mode, dto);

        if (questionIds.isEmpty()) {
            throw new BizException("该题库暂无题目");
        }

        // Shuffle if RANDOM mode
        if ("RANDOM".equals(mode)) {
            Collections.shuffle(questionIds);
        }

        // Create practice record
        PracticeRecord record = new PracticeRecord();
        record.setUserId(userId);
        record.setBankId(bankId);
        record.setMode(mode);
        record.setTotalCount(questionIds.size());
        record.setAnswerCount(0);
        record.setCorrectCount(0);
        record.setStatus(0); // 0=in progress
        record.setLastIndex(0);
        practiceRecordMapper.insert(record);
        incrementPracticeCount(bankId);

        // Store question IDs in record and Redis
        try {
            String json = objectMapper.writeValueAsString(questionIds);
            record.setQuestionIds(json);
            practiceRecordMapper.update(record);
            cacheQuestionIds(record.getId(), json);
        } catch (JsonProcessingException e) {
            throw new BizException("序列化题目ID列表失败");
        }

        return record;
    }

    private void incrementPracticeCount(Long bankId) {
        if (bankId == null) {
            return;
        }
        QuestionBank bank = questionBankMapper.selectOneById(bankId);
        if (bank == null) {
            return;
        }
        Integer count = bank.getPracticeCount();
        bank.setPracticeCount(count == null ? 1 : count + 1);
        questionBankMapper.update(bank);
        questionBankService.evictCache(bankId);
    }

    @Override
    public QuestionVO getQuestion(Long recordId, Integer index, Long userId) {
        getOwnedRecord(recordId, userId);
        // Get question IDs from Redis
        List<Long> questionIds = getQuestionIds(recordId);
        if (index < 0 || index >= questionIds.size()) {
            throw new BizException("题目索引超出范围");
        }

        Long questionId = questionIds.get(index);
        return buildQuestionVO(questionId, userId, recordId);
    }

    @Override
    @Transactional
    public boolean submitAnswer(Long recordId, SubmitAnswerDTO dto, Long userId) {
        PracticeRecord record = getOwnedRecord(recordId, userId);
        Long questionId = dto.getQuestionId();

        // Get the question's correct answer
        Question question = questionMapper.selectOneById(questionId);
        if (question == null) {
            throw new BizException("题目不存在");
        }

        PracticeDetail existed = practiceDetailMapper.selectOneByQuery(
                QueryWrapper.create()
                        .where(PRACTICE_DETAIL.RECORD_ID.eq(recordId))
                        .and(PRACTICE_DETAIL.QUESTION_ID.eq(questionId))
        );
        if (existed != null) {
            return existed.getIsCorrect() != null && existed.getIsCorrect() == 1;
        }

        boolean isCorrect = isAnswerCorrect(question, dto.getAnswer());

        // Save practice detail
        PracticeDetail detail = new PracticeDetail();
        detail.setRecordId(recordId);
        detail.setQuestionId(questionId);
        detail.setUserAnswer(dto.getAnswer());
        detail.setIsCorrect(isCorrect ? 1 : 0);
        detail.setAnswerTime(dto.getAnswerTime());
        practiceDetailMapper.insert(detail);

        // Update practice record
        record.setAnswerCount(record.getAnswerCount() + 1);
        if (isCorrect) {
            record.setCorrectCount(record.getCorrectCount() + 1);
        }
        // Update lastIndex to current question's position
        List<Long> questionIds = getQuestionIds(recordId);
        int idx = questionIds.indexOf(questionId);
        if (idx >= 0) {
            record.setLastIndex(idx);
        }
        if (record.getTotalCount() != null
                && record.getAnswerCount() != null
                && record.getAnswerCount() >= record.getTotalCount()) {
            record.setStatus(1);
        }
        practiceRecordMapper.update(record);
        if (record.getStatus() != null && record.getStatus() == 1) {
            stringRedisTemplate.delete(PRACTICE_KEY_PREFIX + recordId);
        }

        // If wrong, add to wrong questions
        if (!isCorrect) {
            wrongQuestionService.addWrongQuestion(userId, questionId, record.getBankId(), dto.getAnswer());
        }

        return isCorrect;
    }

    @Override
    @Transactional
    public PracticeResultVO finishPractice(Long recordId, Long userId) {
        PracticeRecord record = getOwnedRecord(recordId, userId);

        // Mark as completed
        record.setStatus(1);
        practiceRecordMapper.update(record);

        // Clean up Redis
        stringRedisTemplate.delete(PRACTICE_KEY_PREFIX + recordId);

        // Build result
        PracticeResultVO vo = new PracticeResultVO();
        vo.setRecordId(record.getId());
        vo.setTotalCount(record.getTotalCount());
        vo.setAnswerCount(record.getAnswerCount());
        vo.setCorrectCount(record.getCorrectCount());
        vo.setCorrectRate(record.getAnswerCount() > 0
                ? Math.round(record.getCorrectCount() * 10000.0 / record.getAnswerCount()) / 100.0
                : 0.0);
        // Calculate duration in seconds
        if (record.getCreateTime() != null) {
            long seconds = Duration.between(record.getCreateTime(), LocalDateTime.now()).getSeconds();
            vo.setDuration((int) seconds);
        }

        return vo;
    }

    @Override
    public Map<String, Object> getProgress(Long recordId, Long userId) {
        PracticeRecord record = getOwnedRecord(recordId, userId);

        Map<String, Object> progress = new HashMap<>();
        progress.put("answerCount", record.getAnswerCount());
        progress.put("totalCount", record.getTotalCount());
        progress.put("correctCount", record.getCorrectCount());
        progress.put("lastIndex", record.getLastIndex());
        return progress;
    }

    private PracticeRecord getOwnedRecord(Long recordId, Long userId) {
        PracticeRecord record = practiceRecordMapper.selectOneById(recordId);
        if (record == null) {
            throw new BizException("练习记录不存在");
        }
        if (userId != null && !Objects.equals(record.getUserId(), userId)) {
            throw new BizException(403, "无权访问该练习记录");
        }
        return record;
    }

    private List<Long> getQuestionIds(Long recordId) {
        String key = PRACTICE_KEY_PREFIX + recordId;
        String json = stringRedisTemplate.opsForValue().get(key);
        if (json == null) {
            PracticeRecord record = practiceRecordMapper.selectOneById(recordId);
            if (record == null) {
                throw new BizException("练习记录不存在");
            }
            ensureQuestionIds(record, record.getUserId());
            json = record.getQuestionIds();
        }
        try {
            cacheQuestionIds(recordId, json);
            return objectMapper.readValue(json, new TypeReference<List<Long>>() {});
        } catch (JsonProcessingException e) {
            throw new BizException("解析题目ID列表失败");
        }
    }

    private QuestionVO buildQuestionVO(Long questionId, Long userId, Long recordId) {
        Question question = questionMapper.selectOneById(questionId);
        if (question == null) {
            throw new BizException("题目不存在");
        }

        // Get options
        QueryWrapper optQw = QueryWrapper.create()
                .from(QUESTION_OPTION)
                .where(QUESTION_OPTION.QUESTION_ID.eq(questionId))
                .orderBy(QUESTION_OPTION.SORT.asc());
        List<QuestionOption> options = questionOptionMapper.selectListByQuery(optQw);

        PracticeDetail detail = null;
        if (recordId != null) {
            detail = practiceDetailMapper.selectOneByQuery(
                QueryWrapper.create()
                    .where(PRACTICE_DETAIL.RECORD_ID.eq(recordId))
                    .and(PRACTICE_DETAIL.QUESTION_ID.eq(questionId))
            );
        }
        boolean answered = detail != null;
        QuestionVO vo = AppViewMapper.toQuestionVO(question, options, answered, answered && SHOW_ANALYSIS);
        if (detail != null) {
            vo.setUserAnswer(detail.getUserAnswer());
            vo.setIsCorrect(detail.getIsCorrect());
        }

        return vo;
    }

    private void cacheQuestionIds(Long recordId, String json) {
        if (recordId == null || json == null) {
            return;
        }
        String key = PRACTICE_KEY_PREFIX + recordId;
        stringRedisTemplate.opsForValue().set(key, json);
    }

    private void ensureQuestionIds(PracticeRecord record, Long userId) {
        if (record == null || record.getId() == null) {
            return;
        }
        String existing = record.getQuestionIds();
        if (existing != null && !existing.isEmpty()) {
            return;
        }
        String mode = record.getMode();
        List<Long> questionIds = queryQuestionIds(userId, record.getBankId(), mode, null);
        int limit = record.getTotalCount() != null ? record.getTotalCount() : 0;
        if (limit > 0 && limit < questionIds.size()) {
            questionIds = questionIds.subList(0, limit);
        }
        try {
            String json = objectMapper.writeValueAsString(questionIds);
            record.setQuestionIds(json);
            practiceRecordMapper.update(record);
        } catch (JsonProcessingException e) {
            throw new BizException("序列化题目ID列表失败");
        }
    }

    private List<Long> queryQuestionIds(Long userId, Long bankId, String mode, StartPracticeDTO dto) {
        List<Long> questionIds;
        if ("WRONG".equals(mode)) {
            QueryWrapper wrongQw = QueryWrapper.create()
                    .select(QUESTION.ID)
                    .from(QUESTION)
                    .where(QUESTION.BANK_ID.eq(bankId))
                    .and(QUESTION.STATUS.eq(1))
                    .and(QUESTION.ID.in(
                            QueryWrapper.create()
                                    .select(WRONG_QUESTION.QUESTION_ID)
                                    .from(WRONG_QUESTION)
                                    .where(WRONG_QUESTION.USER_ID.eq(userId))
                                    .and(WRONG_QUESTION.BANK_ID.eq(bankId))
                    ))
                    .orderBy(QUESTION.SORT.asc());
            List<Question> questions = questionMapper.selectListByQuery(wrongQw);
            questionIds = questions.stream().map(Question::getId).collect(Collectors.toList());
        } else {
            QueryWrapper qw = QueryWrapper.create()
                    .select(QUESTION.ID)
                    .from(QUESTION)
                    .where(QUESTION.BANK_ID.eq(bankId))
                    .and(QUESTION.STATUS.eq(1))
                    .orderBy(QUESTION.SORT.asc());
            List<Question> questions = questionMapper.selectListByQuery(qw);
            questionIds = questions.stream().map(Question::getId).collect(Collectors.toList());
        }

        if ("RANDOM".equals(mode)) {
            Collections.shuffle(questionIds);
        }

        if (questionIds.isEmpty()) {
            throw new BizException("该题库暂无题目");
        }
        return questionIds;
    }

    private boolean isAnswerCorrect(Question question, String submittedAnswer) {
        if (question == null || question.getAnswer() == null) {
            return false;
        }
        Integer type = question.getType();
        String correctAnswer = normalizeAnswer(question.getAnswer(), type);
        String actualAnswer = normalizeAnswer(submittedAnswer, type);
        if (type != null && (type == 1 || type == 3)) {
            return correctAnswer.equalsIgnoreCase(actualAnswer);
        }
        return Objects.equals(correctAnswer, actualAnswer);
    }

    private String normalizeAnswer(String answer, Integer type) {
        if (answer == null) {
            return "";
        }
        if (type != null && type == 2) {
            return Arrays.stream(answer.split(","))
                    .map(String::trim)
                    .filter(item -> !item.isEmpty())
                    .map(item -> item.toUpperCase(Locale.ROOT))
                    .distinct()
                    .sorted()
                    .collect(Collectors.joining(","));
        }
        if (type != null && (type == 1 || type == 3)) {
            return answer.trim().toUpperCase(Locale.ROOT);
        }
        return answer.trim();
    }
}
