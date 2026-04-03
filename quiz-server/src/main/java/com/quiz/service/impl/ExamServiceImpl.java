package com.quiz.service.impl;

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
import com.quiz.util.AppViewMapper;
import com.quiz.service.WrongQuestionService;
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
import java.util.*;
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
            ExamSessionVO session = loadExamSession(ongoing.getId());
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
        List<Question> examQuestions = allQuestions;

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
        examRecordMapper.insert(record);
        incrementPracticeCount(dto.getBankId());

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

        int correctCount = 0;
        int totalCount = record.getTotalCount();

        for (SubmitExamDTO.Answer ans : dto.getAnswers()) {
            Question question = questionMapper.selectOneById(ans.getQuestionId());
            if (question == null) continue;

            boolean isCorrect = question.getAnswer().equalsIgnoreCase(
                    ans.getAnswer() != null ? ans.getAnswer().trim() : "");

            ExamAnswer examAnswer = new ExamAnswer();
            examAnswer.setExamId(examId);
            examAnswer.setQuestionId(ans.getQuestionId());
            examAnswer.setUserAnswer(ans.getAnswer());
            examAnswer.setIsCorrect(isCorrect ? 1 : 0);
            examAnswerMapper.insert(examAnswer);

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
        ExamSessionVO session = loadExamSession(record.getId());
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
        ExamSessionVO session = loadExamSession(record.getId());
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
        ExamSessionVO session = loadExamSession(record.getId());
        if (session == null || session.getExamTime() == null) {
            closeExamRecord(record);
            throw new BizException("考试已过期");
        }
        int leftSeconds = calcLeftSeconds(record.getStartTime(), session.getExamTime());
        if (leftSeconds <= 0) {
            closeExamRecord(record);
            throw new BizException("考试已过期");
        }
        String key = examAnswerKey(record.getId());
        String answer = dto.getAnswer() == null ? "" : dto.getAnswer();
        if (answer.trim().isEmpty()) {
            stringRedisTemplate.opsForHash().delete(key, String.valueOf(dto.getQuestionId()));
            stringRedisTemplate.expire(key, Duration.ofSeconds(leftSeconds));
            return;
        }
        stringRedisTemplate.opsForHash().put(key, String.valueOf(dto.getQuestionId()), answer);
        stringRedisTemplate.expire(key, Duration.ofSeconds(leftSeconds));
    }

    private int resolveExamTime(QuestionBank bank, int count) {
        return (int) Math.ceil(count * DEFAULT_EXAM_TIME_PER_QUESTION / 60.0);
    }

    private ExamSessionVO buildExamSession(Long examId, int count, int examTime, List<Question> questions) {
        ExamSessionVO session = new ExamSessionVO();
        session.setExamId(examId);
        session.setTotalCount(count);
        session.setExamTime(examTime);

        List<ExamSessionVO.ExamQuestionVO> list = new ArrayList<>();
        for (Question q : questions) {
            List<QuestionOption> options = questionOptionMapper.selectListByQuery(
                    QueryWrapper.create().where(QUESTION_OPTION.QUESTION_ID.eq(q.getId()))
                            .orderBy(QUESTION_OPTION.SORT, true));
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

    private ExamSessionVO loadExamSession(Long examId) {
        if (examId == null) {
            return null;
        }
        String json = stringRedisTemplate.opsForValue().get(examSessionKey(examId));
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, ExamSessionVO.class);
        } catch (JsonProcessingException e) {
            log.warn("解析考试会话失败", e);
            return null;
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
        List<ExamResultVO.ExamAnswerDetail> details = answers.stream().map(a -> {
            Question q = questionMapper.selectOneById(a.getQuestionId());
            return AppViewMapper.toExamAnswerDetail(a, q);
        }).collect(Collectors.toList());
        vo.setDetails(details);
        return vo;
    }
}
