package com.quiz.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.quiz.common.constant.RedisKey;
import com.quiz.common.exception.BizException;
import com.quiz.common.result.PageResult;
import com.quiz.dto.app.SaveExamAnswerDTO;
import com.quiz.dto.app.StartExamDTO;
import com.quiz.dto.app.SubmitExamDTO;
import com.quiz.entity.*;
import com.quiz.mapper.*;
import com.quiz.service.ExamService;
import com.quiz.service.QuestionBankService;
import com.quiz.service.UserActivityService;
import com.quiz.service.WrongQuestionService;
import com.quiz.util.AppViewMapper;
import com.quiz.vo.app.ExamOngoingVO;
import com.quiz.vo.app.ExamResultVO;
import com.quiz.vo.app.ExamSessionVO;
import com.quiz.vo.app.RecordVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.*;
import java.util.function.Function;
import java.util.Locale;
import java.util.stream.Collectors;

import static com.quiz.entity.table.ExamAnswerTableDef.EXAM_ANSWER;
import static com.quiz.entity.table.ExamRecordTableDef.EXAM_RECORD;
import static com.quiz.entity.table.QuestionTableDef.QUESTION;
import static com.quiz.entity.table.QuestionOptionTableDef.QUESTION_OPTION;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExamServiceImpl implements ExamService {

    private static final int DEFAULT_EXAM_TIME_PER_QUESTION = 120;
    private final ExamRecordMapper examRecordMapper;
    private final ExamAnswerMapper examAnswerMapper;
    private final QuestionMapper questionMapper;
    private final QuestionOptionMapper questionOptionMapper;
    private final QuestionBankMapper questionBankMapper;
    private final WrongQuestionService wrongQuestionService;
    private final QuestionBankService questionBankService;
    private final UserActivityService userActivityService;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public ExamSessionVO startExam(Long userId, StartExamDTO dto) {
        QuestionBank bank = questionBankMapper.selectOneById(dto.getBankId());
        if (bank == null) throw new BizException("题库不存在");

        ExamRecord ongoing = examRecordMapper.selectOneByQuery(
                QueryWrapper.create()
                        .where(EXAM_RECORD.USER_ID.eq(userId))
                        .and(EXAM_RECORD.BANK_ID.eq(dto.getBankId()))
                        .and(EXAM_RECORD.STATUS.eq(0))
                        .orderBy(EXAM_RECORD.CREATE_TIME, false)
        );
        if (ongoing != null) {
            ExamSessionVO session = loadExamSession(ongoing);
            if (session != null && session.getExamTime() != null) {
                int leftSeconds = calcLeftSeconds(ongoing.getStartTime(), session.getExamTime());
                if (leftSeconds > 0) {
                    if (Boolean.TRUE.equals(dto.getRestart())) {
                        closeExamRecord(ongoing);
                    } else {
                        session.setAnswers(loadExamAnswers(ongoing.getId()));
                        session.setLeftSeconds(leftSeconds);
                        return session;
                    }
                } else {
                    closeExamRecord(ongoing);
                }
            } else {
                closeExamRecord(ongoing);
            }
        }

        List<Question> allQuestions = questionMapper.selectListByQuery(
                QueryWrapper.create().where(QUESTION.BANK_ID.eq(dto.getBankId()))
                        .and(QUESTION.STATUS.eq(1))
                        .orderBy(QUESTION.SORT.asc()));

        int count = allQuestions.size();
        if (count <= 0) {
            throw new BizException("该题库暂无题目");
        }
        List<Question> examQuestions = allQuestions;
        String questionIdsJson = serializeQuestionIds(examQuestions);

        ExamRecord record = new ExamRecord();
        record.setUserId(userId);
        record.setBankId(dto.getBankId());
        record.setTotalCount(count);
        record.setCorrectCount(0);
        record.setScore(0);
        record.setIsPass(0);
        record.setDuration(0);
        record.setPassScore(bank.getPassScore());
        record.setStatus(0);
        record.setStartTime(LocalDateTime.now());
        record.setCreateTime(LocalDateTime.now());
        record.setQuestionIds(questionIdsJson);
        examRecordMapper.insert(record);
        incrementPracticeCount(dto.getBankId());
        userActivityService.recordExamStart(userId, dto.getBankId(), record.getId(), count);

        int examTime = resolveExamTime(bank, count);
        ExamSessionVO session = buildExamSession(record.getId(), count, examTime, examQuestions);
        cacheExamSession(session, examTime);
        session.setLeftSeconds(examTime * 60);
        return session;
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
    @Transactional
    public ExamResultVO submitExam(Long examId, SubmitExamDTO dto, Long userId) {
        ExamRecord record = examRecordMapper.selectOneById(examId);
        if (record == null || !record.getUserId().equals(userId)) {
            throw new BizException("考试记录不存在");
        }
        if (record.getStatus() == 1) {
            throw new BizException("已交卷，不能重复提交");
        }

        List<SubmitExamDTO.Answer> submittedAnswers = dto != null && dto.getAnswers() != null
                ? dto.getAnswers().stream().filter(Objects::nonNull).toList()
                : Collections.emptyList();
        int correctCount = 0;
        int totalCount = record.getTotalCount();
        Map<Long, Question> questionMap = batchQueryQuestions(
                submittedAnswers.stream()
                        .map(SubmitExamDTO.Answer::getQuestionId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet())
        );
        int answerTime = calcAnswerTime(record.getStartTime());

        for (SubmitExamDTO.Answer ans : submittedAnswers) {
            Question question = questionMap.get(ans.getQuestionId());
            if (question == null) continue;

            boolean isCorrect = isAnswerCorrect(question, ans.getAnswer());
            boolean answerChanged = upsertSubmittedExamAnswer(record, ans, isCorrect ? 1 : 0, answerTime);
            if (answerChanged) {
                userActivityService.recordExamAnswer(
                        userId,
                        record.getBankId(),
                        examId,
                        ans.getQuestionId(),
                        ans.getAnswer(),
                        answerTime
                );
            }

            if (isCorrect) {
                correctCount++;
            } else {
                wrongQuestionService.addWrongQuestion(userId, ans.getQuestionId(),
                        record.getBankId(), ans.getAnswer());
            }
        }

        int score = totalCount > 0 ? (int) Math.round(correctCount * 100.0 / totalCount) : 0;
        long duration = ChronoUnit.SECONDS.between(record.getStartTime(), LocalDateTime.now());

        record.setCorrectCount(correctCount);
        record.setScore(score);
        record.setDuration((int) duration);
        record.setIsPass(score >= record.getPassScore() ? 1 : 0);
        record.setStatus(1);
        record.setEndTime(LocalDateTime.now());
        examRecordMapper.update(record);
        userActivityService.recordExamSubmit(userId, record.getBankId(), examId, totalCount, correctCount, score);
        clearExamSession(record.getId());
        clearExamAnswers(record.getId());

        return buildExamResult(record);
    }

    @Override
    public ExamResultVO getResult(Long examId, Long userId) {
        ExamRecord record = examRecordMapper.selectOneById(examId);
        if (record == null) throw new BizException("记录不存在");
        if (!record.getUserId().equals(userId)) {
            throw new BizException(403, "无权访问该记录");
        }
        return buildExamResult(record);
    }

    @Override
    public PageResult<RecordVO> getRecords(Long userId, Integer pageNum, Integer pageSize) {
        QueryWrapper qw = QueryWrapper.create()
                .where(EXAM_RECORD.USER_ID.eq(userId))
                .orderBy(EXAM_RECORD.CREATE_TIME, false);
        Page<ExamRecord> page = examRecordMapper.paginate(Page.of(pageNum, pageSize), qw);

        Set<Long> bankIds = page.getRecords().stream()
                .map(ExamRecord::getBankId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, QuestionBank> bankMap = bankIds.isEmpty() ? Collections.emptyMap()
                : questionBankMapper.selectListByIds(bankIds).stream()
                .collect(Collectors.toMap(QuestionBank::getId, bank -> bank, (a, b) -> a));

        List<RecordVO> list = page.getRecords().stream().map(r -> {
            RecordVO vo = new RecordVO();
            vo.setId(r.getId());
            QuestionBank bank = bankMap.get(r.getBankId());
            vo.setBankName(bank != null ? bank.getName() : "");
            vo.setMode("EXAM");
            vo.setTotalCount(r.getTotalCount());
            vo.setCorrectCount(r.getCorrectCount());
            vo.setCorrectRate(r.getTotalCount() > 0 ?
                    Math.round(r.getCorrectCount() * 10000.0 / r.getTotalCount()) / 100.0 : 0.0);
            vo.setCreateTime(r.getCreateTime());
            vo.setType("exam");
            return vo;
        }).collect(Collectors.toList());

        return new PageResult<>(list, page.getTotalRow(), pageNum, pageSize);
    }

    @Override
    public ExamOngoingVO getOngoingExam(Long userId, Long bankId) {
        ExamOngoingVO vo = new ExamOngoingVO();
        if (bankId == null) {
            vo.setExists(false);
            return vo;
        }
        ExamRecord record = examRecordMapper.selectOneByQuery(
                QueryWrapper.create()
                        .where(EXAM_RECORD.USER_ID.eq(userId))
                        .and(EXAM_RECORD.BANK_ID.eq(bankId))
                        .and(EXAM_RECORD.STATUS.eq(0))
                        .orderBy(EXAM_RECORD.CREATE_TIME, false)
        );
        if (record == null) {
            vo.setExists(false);
            return vo;
        }
        ExamSessionVO session = loadExamSession(record);
        if (session == null || session.getExamTime() == null) {
            closeExamRecord(record);
            vo.setExists(true);
            vo.setExpired(true);
            vo.setExamId(record.getId());
            return vo;
        }
        int leftSeconds = calcLeftSeconds(record.getStartTime(), session.getExamTime());
        if (leftSeconds <= 0) {
            closeExamRecord(record);
            vo.setExists(true);
            vo.setExpired(true);
            vo.setExamId(record.getId());
            return vo;
        }
        vo.setExists(true);
        vo.setExpired(false);
        vo.setExamId(record.getId());
        vo.setLeftSeconds(leftSeconds);
        return vo;
    }

    @Override
    public ExamSessionVO getExamSession(Long userId, Long examId) {
        ExamRecord record = examRecordMapper.selectOneById(examId);
        if (record == null || !record.getUserId().equals(userId)) {
            throw new BizException("考试记录不存在");
        }
        if (record.getStatus() != null && record.getStatus() == 1) {
            throw new BizException("考试已结束");
        }
        ExamSessionVO session = loadExamSession(record);
        if (session == null || session.getExamTime() == null) {
            closeExamRecord(record);
            throw new BizException("考试已过期");
        }
        int leftSeconds = calcLeftSeconds(record.getStartTime(), session.getExamTime());
        if (leftSeconds <= 0) {
            closeExamRecord(record);
            throw new BizException("考试已过期");
        }
        session.setAnswers(loadExamAnswers(record.getId()));
        session.setLeftSeconds(leftSeconds);
        return session;
    }

    @Override
    public void saveExamAnswer(Long userId, Long examId, SaveExamAnswerDTO dto) {
        if (dto == null || dto.getQuestionId() == null) {
            throw new BizException("题目不能为空");
        }
        ExamRecord record = examRecordMapper.selectOneById(examId);
        if (record == null || !record.getUserId().equals(userId)) {
            throw new BizException("考试记录不存在");
        }
        if (record.getStatus() != null && record.getStatus() == 1) {
            throw new BizException("考试已结束");
        }
        ExamSessionVO session = loadExamSession(record);
        if (session == null || session.getExamTime() == null) {
            closeExamRecord(record);
            throw new BizException("考试已过期");
        }
        int leftSeconds = calcLeftSeconds(record.getStartTime(), session.getExamTime());
        if (leftSeconds <= 0) {
            closeExamRecord(record);
            throw new BizException("考试已过期");
        }
        String answer = dto.getAnswer() == null ? "" : dto.getAnswer();
        if (answer.trim().isEmpty()) {
            deleteExamAnswer(record.getId(), dto.getQuestionId());
            stringRedisTemplate.opsForHash().delete(examAnswerKey(record.getId()), String.valueOf(dto.getQuestionId()));
            stringRedisTemplate.expire(examAnswerKey(record.getId()), Duration.ofSeconds(leftSeconds));
            return;
        }
        boolean changed = upsertDraftExamAnswer(record, dto.getQuestionId(), answer);
        if (changed) {
            userActivityService.recordExamAnswer(
                    userId,
                    record.getBankId(),
                    examId,
                    dto.getQuestionId(),
                    answer,
                    calcAnswerTime(record.getStartTime())
            );
        }
        stringRedisTemplate.opsForHash().put(examAnswerKey(record.getId()), String.valueOf(dto.getQuestionId()), answer);
        stringRedisTemplate.expire(examAnswerKey(record.getId()), Duration.ofSeconds(leftSeconds));
    }

    private int resolveExamTime(QuestionBank bank, int count) {
        return (int) Math.ceil(count * DEFAULT_EXAM_TIME_PER_QUESTION / 60.0);
    }

    private ExamSessionVO buildExamSession(Long examId, int count, int examTime, List<Question> questions) {
        ExamSessionVO session = new ExamSessionVO();
        session.setExamId(examId);
        session.setTotalCount(count);
        session.setExamTime(examTime);

        Map<Long, List<QuestionOption>> optionsMap = batchQueryQuestionOptions(
                questions.stream()
                        .map(Question::getId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet())
        );

        List<ExamSessionVO.ExamQuestionVO> list = new ArrayList<>();
        for (Question q : questions) {
            List<QuestionOption> options = optionsMap.getOrDefault(q.getId(), Collections.emptyList());
            ExamSessionVO.ExamQuestionVO qvo = AppViewMapper.toExamQuestionVO(q, options);
            list.add(qvo);
        }
        session.setQuestions(list);
        return session;
    }

    private void cacheExamSession(ExamSessionVO session, int examTime) {
        if (session == null || session.getExamId() == null) {
            return;
        }
        try {
            ExamSessionVO cache = new ExamSessionVO();
            cache.setExamId(session.getExamId());
            cache.setTotalCount(session.getTotalCount());
            cache.setExamTime(session.getExamTime());
            cache.setQuestions(session.getQuestions());
            String json = objectMapper.writeValueAsString(cache);
            stringRedisTemplate.opsForValue().set(examSessionKey(session.getExamId()), json, Duration.ofMinutes(examTime));
        } catch (JsonProcessingException e) {
            log.warn("缓存考试会话失败", e);
        }
    }

    private List<ExamSessionVO.ExamAnswerVO> loadExamAnswers(Long examId) {
        if (examId == null) {
            return Collections.emptyList();
        }
        List<ExamAnswer> savedAnswers = examAnswerMapper.selectListByQuery(
                QueryWrapper.create()
                        .where(EXAM_ANSWER.EXAM_ID.eq(examId))
                        .orderBy(EXAM_ANSWER.ID.asc())
        );
        if (!savedAnswers.isEmpty()) {
            return savedAnswers.stream().map(answer -> {
                ExamSessionVO.ExamAnswerVO vo = new ExamSessionVO.ExamAnswerVO();
                vo.setQuestionId(answer.getQuestionId());
                vo.setAnswer(answer.getUserAnswer());
                return vo;
            }).toList();
        }
        Map<Object, Object> map = stringRedisTemplate.opsForHash().entries(examAnswerKey(examId));
        if (map == null || map.isEmpty()) {
            return Collections.emptyList();
        }
        List<ExamSessionVO.ExamAnswerVO> list = new ArrayList<>();
        for (Map.Entry<Object, Object> entry : map.entrySet()) {
            if (entry.getKey() == null) continue;
            ExamSessionVO.ExamAnswerVO vo = new ExamSessionVO.ExamAnswerVO();
            try {
                vo.setQuestionId(Long.parseLong(entry.getKey().toString()));
            } catch (NumberFormatException e) {
                continue;
            }
            vo.setAnswer(entry.getValue() != null ? entry.getValue().toString() : "");
            list.add(vo);
        }
        return list;
    }

    private ExamSessionVO loadExamSession(ExamRecord record) {
        if (record == null || record.getId() == null) {
            return null;
        }
        String json = stringRedisTemplate.opsForValue().get(examSessionKey(record.getId()));
        if (json == null || json.isEmpty()) {
            ExamSessionVO rebuilt = rebuildExamSession(record);
            if (rebuilt != null && rebuilt.getExamTime() != null) {
                cacheExamSession(rebuilt, rebuilt.getExamTime());
            }
            return rebuilt;
        }
        try {
            return objectMapper.readValue(json, ExamSessionVO.class);
        } catch (JsonProcessingException e) {
            log.warn("解析考试会话失败", e);
            return rebuildExamSession(record);
        }
    }

    private void clearExamSession(Long examId) {
        if (examId == null) {
            return;
        }
        stringRedisTemplate.delete(examSessionKey(examId));
    }

    private String examSessionKey(Long examId) {
        return RedisKey.EXAM_SESSION + examId;
    }

    private void clearExamAnswers(Long examId) {
        if (examId == null) {
            return;
        }
        stringRedisTemplate.delete(examAnswerKey(examId));
    }

    private String examAnswerKey(Long examId) {
        return RedisKey.EXAM_ANSWER + examId;
    }

    private int calcAnswerTime(LocalDateTime startTime) {
        if (startTime == null) {
            return 0;
        }
        return Math.max((int) ChronoUnit.SECONDS.between(startTime, LocalDateTime.now()), 0);
    }

    private int calcLeftSeconds(LocalDateTime startTime, Integer examTime) {
        if (examTime == null || examTime <= 0) {
            return 0;
        }
        if (startTime == null) {
            return examTime * 60;
        }
        long used = ChronoUnit.SECONDS.between(startTime, LocalDateTime.now());
        long left = examTime * 60L - used;
        return (int) Math.max(0, left);
    }

    private void closeExamRecord(ExamRecord record) {
        if (record == null) {
            return;
        }
        record.setStatus(1);
        record.setEndTime(LocalDateTime.now());
        if (record.getStartTime() != null) {
            long duration = ChronoUnit.SECONDS.between(record.getStartTime(), LocalDateTime.now());
            record.setDuration((int) duration);
        }
        if (record.getCorrectCount() == null) {
            record.setCorrectCount(0);
        }
        if (record.getScore() == null) {
            record.setScore(0);
        }
        if (record.getIsPass() == null) {
            record.setIsPass(0);
        }
        examRecordMapper.update(record);
        clearExamSession(record.getId());
        clearExamAnswers(record.getId());
    }

    private ExamResultVO buildExamResult(ExamRecord record) {
        ExamResultVO vo = new ExamResultVO();
        vo.setExamId(record.getId());
        vo.setScore(record.getScore());
        vo.setTotalCount(record.getTotalCount());
        vo.setCorrectCount(record.getCorrectCount());
        vo.setCorrectRate(record.getTotalCount() > 0 ?
                Math.round(record.getCorrectCount() * 10000.0 / record.getTotalCount()) / 100.0 : 0.0);
        vo.setDuration(record.getDuration());
        vo.setPassScore(record.getPassScore());
        vo.setIsPass(record.getIsPass() == 1);

        List<ExamAnswer> answers = examAnswerMapper.selectListByQuery(
                QueryWrapper.create().where(EXAM_ANSWER.EXAM_ID.eq(record.getId())));
        Map<Long, Question> questionMap = batchQueryQuestions(
                answers.stream()
                        .map(ExamAnswer::getQuestionId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet())
        );
        List<ExamResultVO.ExamAnswerDetail> details = answers.stream().map(a -> {
            Question q = questionMap.get(a.getQuestionId());
            return AppViewMapper.toExamAnswerDetail(a, q);
        }).collect(Collectors.toList());
        vo.setDetails(details);
        return vo;
    }

    private Map<Long, Question> batchQueryQuestions(Set<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return questionMapper.selectListByIds(questionIds).stream()
                .collect(Collectors.toMap(Question::getId, Function.identity(), (a, b) -> a));
    }

    private Map<Long, List<QuestionOption>> batchQueryQuestionOptions(Set<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<QuestionOption> options = questionOptionMapper.selectListByQuery(
                QueryWrapper.create()
                        .where(QUESTION_OPTION.QUESTION_ID.in(questionIds))
                        .orderBy(QUESTION_OPTION.QUESTION_ID.asc(), QUESTION_OPTION.SORT.asc())
        );
        return options.stream().collect(Collectors.groupingBy(
                QuestionOption::getQuestionId,
                LinkedHashMap::new,
                Collectors.toList()
        ));
    }

    private String serializeQuestionIds(List<Question> questions) {
        if (questions == null || questions.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(
                    questions.stream()
                            .map(Question::getId)
                            .filter(Objects::nonNull)
                            .toList()
            );
        } catch (JsonProcessingException e) {
            throw new BizException("序列化考试题目列表失败");
        }
    }

    private List<Long> parseQuestionIds(String questionIdsJson) {
        if (questionIdsJson == null || questionIdsJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            List<Long> ids = objectMapper.readValue(questionIdsJson, new TypeReference<List<Long>>() {});
            return ids == null ? Collections.emptyList() : ids.stream().filter(Objects::nonNull).toList();
        } catch (JsonProcessingException e) {
            log.warn("解析考试题目列表失败", e);
            return Collections.emptyList();
        }
    }

    private ExamSessionVO rebuildExamSession(ExamRecord record) {
        if (record == null || record.getId() == null) {
            return null;
        }
        List<Long> questionIds = parseQuestionIds(record.getQuestionIds());
        if (questionIds.isEmpty()) {
            return null;
        }
        QuestionBank bank = questionBankMapper.selectOneById(record.getBankId());
        if (bank == null) {
            return null;
        }
        List<Question> questions = loadQuestionsByIds(questionIds);
        if (questions.isEmpty()) {
            return null;
        }
        int examTime = resolveExamTime(bank, Math.max(record.getTotalCount() == null ? 0 : record.getTotalCount(), questions.size()));
        return buildExamSession(record.getId(), record.getTotalCount() != null ? record.getTotalCount() : questions.size(), examTime, questions);
    }

    private List<Question> loadQuestionsByIds(List<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, Question> questionMap = questionMapper.selectListByIds(questionIds).stream()
                .collect(Collectors.toMap(Question::getId, Function.identity(), (a, b) -> a));
        List<Question> ordered = new ArrayList<>();
        for (Long questionId : questionIds) {
            Question question = questionMap.get(questionId);
            if (question != null) {
                ordered.add(question);
            }
        }
        return ordered;
    }

    private boolean upsertDraftExamAnswer(ExamRecord record, Long questionId, String answer) {
        ExamAnswer existing = getExamAnswer(record.getId(), questionId);
        int answerTime = calcAnswerTime(record.getStartTime());
        if (existing == null) {
            ExamAnswer draft = new ExamAnswer();
            draft.setExamId(record.getId());
            draft.setQuestionId(questionId);
            draft.setUserAnswer(answer);
            draft.setAnswerTime(answerTime);
            draft.setCreateTime(LocalDateTime.now());
            examAnswerMapper.insert(draft);
            return true;
        }
        if (Objects.equals(existing.getUserAnswer(), answer)) {
            return false;
        }
        existing.setUserAnswer(answer);
        existing.setAnswerTime(answerTime);
        existing.setCreateTime(LocalDateTime.now());
        examAnswerMapper.update(existing, false);
        return true;
    }

    private boolean upsertSubmittedExamAnswer(ExamRecord record, SubmitExamDTO.Answer answer, Integer isCorrect, int answerTime) {
        ExamAnswer existing = getExamAnswer(record.getId(), answer.getQuestionId());
        boolean changed = existing == null || !Objects.equals(existing.getUserAnswer(), answer.getAnswer());
        if (existing == null) {
            ExamAnswer created = new ExamAnswer();
            created.setExamId(record.getId());
            created.setQuestionId(answer.getQuestionId());
            created.setUserAnswer(answer.getAnswer());
            created.setIsCorrect(isCorrect);
            created.setAnswerTime(answerTime);
            created.setCreateTime(LocalDateTime.now());
            examAnswerMapper.insert(created);
            return true;
        }
        existing.setUserAnswer(answer.getAnswer());
        existing.setIsCorrect(isCorrect);
        if (changed) {
            existing.setAnswerTime(answerTime);
            existing.setCreateTime(LocalDateTime.now());
        } else if (existing.getAnswerTime() == null) {
            existing.setAnswerTime(answerTime);
        }
        examAnswerMapper.update(existing, false);
        return changed;
    }

    private ExamAnswer getExamAnswer(Long examId, Long questionId) {
        return examAnswerMapper.selectOneByQuery(
                QueryWrapper.create()
                        .where(EXAM_ANSWER.EXAM_ID.eq(examId))
                        .and(EXAM_ANSWER.QUESTION_ID.eq(questionId))
        );
    }

    private void deleteExamAnswer(Long examId, Long questionId) {
        if (examId == null || questionId == null) {
            return;
        }
        examAnswerMapper.deleteByQuery(
                QueryWrapper.create()
                        .where(EXAM_ANSWER.EXAM_ID.eq(examId))
                        .and(EXAM_ANSWER.QUESTION_ID.eq(questionId))
        );
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
