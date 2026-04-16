package com.quiz.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.quiz.common.exception.BizException;
import com.quiz.common.result.PageResult;
import com.quiz.entity.*;
import com.quiz.mapper.*;
import com.quiz.service.RecordService;
import com.quiz.vo.app.RecordVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.quiz.entity.table.PracticeRecordTableDef.PRACTICE_RECORD;
import static com.quiz.entity.table.PracticeDetailTableDef.PRACTICE_DETAIL;
import static com.quiz.entity.table.ExamRecordTableDef.EXAM_RECORD;
import static com.quiz.entity.table.ExamAnswerTableDef.EXAM_ANSWER;
import static com.quiz.entity.table.UserTableDef.USER;

@Service
@RequiredArgsConstructor
public class RecordServiceImpl implements RecordService {

    private final PracticeRecordMapper practiceRecordMapper;
    private final PracticeDetailMapper practiceDetailMapper;
    private final ExamRecordMapper examRecordMapper;
    private final ExamAnswerMapper examAnswerMapper;
    private final QuestionBankMapper questionBankMapper;
    private final QuestionMapper questionMapper;
    private final QuestionOptionMapper questionOptionMapper;
    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    @Override
    public PageResult<RecordVO> appRecordList(Long userId, String type, Integer pageNum, Integer pageSize) {
        if ("practice".equals(type)) {
            return pagePracticeRecordsForApp(userId, pageNum, pageSize);
        }
        if ("exam".equals(type)) {
            return pageExamRecordsForApp(userId, pageNum, pageSize);
        }

        int fetchSize = Math.max(pageNum * pageSize, pageSize);
        PageResult<RecordVO> practicePage = pagePracticeRecordsForApp(userId, 1, fetchSize);
        PageResult<RecordVO> examPage = pageExamRecordsForApp(userId, 1, fetchSize);
        List<RecordVO> allRecords = new ArrayList<>();
        allRecords.addAll(practicePage.getList());
        allRecords.addAll(examPage.getList());
        allRecords.sort((a, b) -> b.getCreateTime().compareTo(a.getCreateTime()));
        int start = (pageNum - 1) * pageSize;
        int end = Math.min(start + pageSize, allRecords.size());
        List<RecordVO> pageList = start < allRecords.size() ? allRecords.subList(start, end) : List.of();
        long total = practicePage.getTotal() + examPage.getTotal();
        return new PageResult<>(pageList, total, pageNum, pageSize);
    }

    @Override
    public Map<String, Object> appRecordDetail(Long id, String type, Long userId) {
        Map<String, Object> result = new HashMap<>();
        if ("practice".equals(type)) {
            PracticeRecord record = practiceRecordMapper.selectOneById(id);
            if (record == null) throw new BizException("记录不存在");
            if (userId != null && !Objects.equals(record.getUserId(), userId)) {
                throw new BizException(403, "无权访问该记录");
            }
            result.put("record", record);
            List<PracticeDetail> details = practiceDetailMapper.selectListByQuery(
                    QueryWrapper.create().where(PRACTICE_DETAIL.RECORD_ID.eq(id)));
            Map<Long, Question> questionMap = batchQueryQuestions(details.stream().map(PracticeDetail::getQuestionId).toList());
            Map<Long, List<Map<String, String>>> optionsMap = batchQueryQuestionOptions(questionMap.keySet());
            List<Map<String, Object>> detailList = details.stream().map(d -> {
                Map<String, Object> m = new HashMap<>();
                m.put("questionId", d.getQuestionId());
                m.put("userAnswer", d.getUserAnswer());
                m.put("isCorrect", d.getIsCorrect());
                Question q = questionMap.get(d.getQuestionId());
                if (q != null) {
                    m.put("content", q.getContent());
                    m.put("correctAnswer", q.getAnswer());
                    m.put("analysis", q.getAnalysis());
                    m.put("options", optionsMap.getOrDefault(d.getQuestionId(), Collections.emptyList()));
                }
                return m;
            }).collect(Collectors.toList());
            result.put("details", detailList);
        } else {
            ExamRecord record = examRecordMapper.selectOneById(id);
            if (record == null) throw new BizException("记录不存在");
            if (userId != null && !Objects.equals(record.getUserId(), userId)) {
                throw new BizException(403, "无权访问该记录");
            }
            result.put("record", record);
            List<ExamAnswer> answers = examAnswerMapper.selectListByQuery(
                    QueryWrapper.create().where(EXAM_ANSWER.EXAM_ID.eq(id)));
            Map<Long, Question> questionMap = batchQueryQuestions(answers.stream().map(ExamAnswer::getQuestionId).toList());
            Map<Long, List<Map<String, String>>> optionsMap = batchQueryQuestionOptions(questionMap.keySet());
            List<Map<String, Object>> detailList = answers.stream().map(a -> {
                Map<String, Object> m = new HashMap<>();
                m.put("questionId", a.getQuestionId());
                m.put("userAnswer", a.getUserAnswer());
                m.put("isCorrect", a.getIsCorrect());
                Question q = questionMap.get(a.getQuestionId());
                if (q != null) {
                    m.put("content", q.getContent());
                    m.put("correctAnswer", q.getAnswer());
                    m.put("analysis", q.getAnalysis());
                    m.put("options", optionsMap.getOrDefault(a.getQuestionId(), Collections.emptyList()));
                }
                return m;
            }).collect(Collectors.toList());
            result.put("details", detailList);
        }
        return result;
    }

    @Override
    public Map<String, Object> adminPracticeRecordDetail(Long id, String keyword, String result, Integer pageNum, Integer pageSize) {
        PracticeRecord record = practiceRecordMapper.selectOneById(id);
        if (record == null) {
            throw new BizException("记录不存在");
        }

        List<PracticeDetail> answeredDetails = practiceDetailMapper.selectListByQuery(
                QueryWrapper.create()
                        .where(PRACTICE_DETAIL.RECORD_ID.eq(id))
                        .orderBy(PRACTICE_DETAIL.ID.asc())
        );
        Map<Long, PracticeDetail> detailMap = answeredDetails.stream()
                .filter(detail -> detail.getQuestionId() != null)
                .collect(Collectors.toMap(
                        PracticeDetail::getQuestionId,
                        Function.identity(),
                        (first, second) -> first,
                        LinkedHashMap::new
                ));

        List<Long> questionIds = parseQuestionIds(record.getQuestionIds());
        if (questionIds.isEmpty()) {
            questionIds = answeredDetails.stream()
                    .map(PracticeDetail::getQuestionId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
        }

        Map<Long, Question> questionMap = batchQueryQuestions(questionIds);
        Map<Long, List<Map<String, String>>> optionsMap = batchQueryQuestionOptions(new LinkedHashSet<>(questionIds));

        List<Map<String, Object>> allDetails = new ArrayList<>();
        for (int i = 0; i < questionIds.size(); i++) {
            Long questionId = questionIds.get(i);
            PracticeDetail detail = detailMap.get(questionId);
            Question question = questionMap.get(questionId);

            Map<String, Object> item = new HashMap<>();
            item.put("seq", i + 1);
            item.put("questionId", questionId);
            item.put("userAnswer", detail != null ? detail.getUserAnswer() : null);
            item.put("isCorrect", detail != null ? detail.getIsCorrect() : null);
            if (question != null) {
                item.put("content", question.getContent());
                item.put("correctAnswer", question.getAnswer());
                item.put("analysis", question.getAnalysis());
                item.put("options", optionsMap.getOrDefault(questionId, Collections.emptyList()));
            }
            allDetails.add(item);
        }

        int totalCount = firstNonNegative(record.getTotalCount(), questionIds.size());
        int answerCount = firstNonNegative(record.getAnswerCount(), answeredDetails.size());
        int correctCount = firstNonNegative(record.getCorrectCount(), 0);

        Map<String, Object> payload = new HashMap<>();
        payload.put("record", record);
        payload.put("summary", buildAnswerSummary(totalCount, answerCount, correctCount));
        payload.put("details", paginateList(filterDetailItems(allDetails, keyword, result), pageNum, pageSize));
        return payload;
    }

    @Override
    public Map<String, Object> adminExamRecordDetail(Long id, String keyword, String result, Integer pageNum, Integer pageSize) {
        ExamRecord record = examRecordMapper.selectOneById(id);
        if (record == null) {
            throw new BizException("记录不存在");
        }

        List<ExamAnswer> answers = examAnswerMapper.selectListByQuery(
                QueryWrapper.create()
                        .where(EXAM_ANSWER.EXAM_ID.eq(id))
                        .orderBy(EXAM_ANSWER.ID.asc())
        );

        Map<Long, Question> questionMap = batchQueryQuestions(answers.stream()
                .map(ExamAnswer::getQuestionId)
                .filter(Objects::nonNull)
                .distinct()
                .toList());
        Map<Long, List<Map<String, String>>> optionsMap = batchQueryQuestionOptions(questionMap.keySet());

        List<Map<String, Object>> detailList = new ArrayList<>();
        for (int i = 0; i < answers.size(); i++) {
            ExamAnswer answer = answers.get(i);
            Question question = questionMap.get(answer.getQuestionId());

            Map<String, Object> item = new HashMap<>();
            item.put("seq", i + 1);
            item.put("questionId", answer.getQuestionId());
            item.put("userAnswer", answer.getUserAnswer());
            item.put("isCorrect", answer.getIsCorrect());
            if (question != null) {
                item.put("content", question.getContent());
                item.put("correctAnswer", question.getAnswer());
                item.put("analysis", question.getAnalysis());
                item.put("options", optionsMap.getOrDefault(answer.getQuestionId(), Collections.emptyList()));
            }
            detailList.add(item);
        }

        int totalCount = firstNonNegative(record.getTotalCount(), 0);
        int answerCount = answers.size();
        int correctCount = firstNonNegative(record.getCorrectCount(), 0);

        Map<String, Object> payload = new HashMap<>();
        payload.put("record", record);
        payload.put("summary", buildAnswerSummary(totalCount, answerCount, correctCount));
        payload.put("details", paginateList(filterDetailItems(detailList, keyword, result), pageNum, pageSize));
        return payload;
    }

    private PageResult<RecordVO> pagePracticeRecordsForApp(Long userId, Integer pageNum, Integer pageSize) {
        Page<PracticeRecord> page = practiceRecordMapper.paginate(
                Page.of(pageNum, pageSize),
                QueryWrapper.create()
                        .where(PRACTICE_RECORD.USER_ID.eq(userId))
                        .orderBy(PRACTICE_RECORD.CREATE_TIME, false)
        );
        Map<Long, QuestionBank> bankMap = batchQueryBanks(page.getRecords().stream()
                .map(PracticeRecord::getBankId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        List<RecordVO> list = page.getRecords().stream().map(p -> {
            RecordVO vo = new RecordVO();
            vo.setId(p.getId());
            QuestionBank bank = bankMap.get(p.getBankId());
            vo.setBankName(bank != null ? bank.getName() : "");
            vo.setMode(p.getMode());
            vo.setTotalCount(p.getTotalCount());
            vo.setCorrectCount(p.getCorrectCount());
            vo.setCorrectRate(p.getTotalCount() > 0 ?
                    Math.round(p.getCorrectCount() * 10000.0 / p.getTotalCount()) / 100.0 : 0.0);
            vo.setCreateTime(p.getCreateTime());
            vo.setType("practice");
            return vo;
        }).toList();
        return new PageResult<>(list, page.getTotalRow(), pageNum, pageSize);
    }

    private PageResult<RecordVO> pageExamRecordsForApp(Long userId, Integer pageNum, Integer pageSize) {
        Page<ExamRecord> page = examRecordMapper.paginate(
                Page.of(pageNum, pageSize),
                QueryWrapper.create()
                        .where(EXAM_RECORD.USER_ID.eq(userId))
                        .orderBy(EXAM_RECORD.CREATE_TIME, false)
        );
        Map<Long, QuestionBank> bankMap = batchQueryBanks(page.getRecords().stream()
                .map(ExamRecord::getBankId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        List<RecordVO> list = page.getRecords().stream().map(e -> {
            RecordVO vo = new RecordVO();
            vo.setId(e.getId());
            QuestionBank bank = bankMap.get(e.getBankId());
            vo.setBankName(bank != null ? bank.getName() : "");
            vo.setMode("EXAM");
            vo.setTotalCount(e.getTotalCount());
            vo.setCorrectCount(e.getCorrectCount());
            vo.setCorrectRate(e.getTotalCount() > 0 ?
                    Math.round(e.getCorrectCount() * 10000.0 / e.getTotalCount()) / 100.0 : 0.0);
            vo.setScore(e.getScore());
            vo.setCreateTime(e.getCreateTime());
            vo.setType("exam");
            return vo;
        }).toList();
        return new PageResult<>(list, page.getTotalRow(), pageNum, pageSize);
    }

    private Map<Long, QuestionBank> batchQueryBanks(Set<Long> bankIds) {
        if (bankIds == null || bankIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return questionBankMapper.selectListByIds(bankIds).stream()
                .collect(Collectors.toMap(QuestionBank::getId, Function.identity(), (a, b) -> a));
    }

    private Map<Long, Question> batchQueryQuestions(List<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return questionMapper.selectListByIds(questionIds.stream().filter(Objects::nonNull).distinct().toList()).stream()
                .collect(Collectors.toMap(Question::getId, Function.identity(), (a, b) -> a));
    }

    private Map<Long, List<Map<String, String>>> batchQueryQuestionOptions(Set<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            return Collections.emptyMap();
        }
        QueryWrapper qw = QueryWrapper.create()
                .where(com.quiz.entity.table.QuestionOptionTableDef.QUESTION_OPTION.QUESTION_ID.in(questionIds))
                .orderBy(com.quiz.entity.table.QuestionOptionTableDef.QUESTION_OPTION.SORT.asc());
        List<QuestionOption> options = questionOptionMapper.selectListByQuery(qw);
        return options.stream().collect(Collectors.groupingBy(
                QuestionOption::getQuestionId,
                Collectors.mapping(opt -> {
                    Map<String, String> m = new HashMap<>();
                    m.put("label", opt.getLabel());
                    m.put("content", opt.getContent());
                    return m;
                }, Collectors.toList())
        ));
    }

    @Override
    public PageResult<Map<String, Object>> adminPracticeRecords(String keyword, Long bankId, Integer pageNum, Integer pageSize) {
        // 关键词搜索用户
        List<Long> matchedUserIds = null;
        if (StringUtils.hasText(keyword)) {
            QueryWrapper userQuery = QueryWrapper.create()
                    .select(USER.ID)
                    .where(USER.NICKNAME.like(keyword).or(USER.PHONE.like(keyword)));
            List<User> matchedUsers = userMapper.selectListByQuery(userQuery);
            matchedUserIds = matchedUsers.stream().map(User::getId).toList();
            if (matchedUserIds.isEmpty()) {
                return PageResult.empty(pageNum, pageSize);
            }
        }

        QueryWrapper qw = QueryWrapper.create();
        if (matchedUserIds != null) qw.and(PRACTICE_RECORD.USER_ID.in(matchedUserIds));
        if (bankId != null) qw.and(PRACTICE_RECORD.BANK_ID.eq(bankId));
        qw.orderBy(PRACTICE_RECORD.CREATE_TIME, false);
        Page<PracticeRecord> page = practiceRecordMapper.paginate(Page.of(pageNum, pageSize), qw);

        if (page.getRecords().isEmpty()) {
            return PageResult.empty(pageNum, pageSize);
        }

        // 批量查询用户和题库
        Set<Long> userIds = page.getRecords().stream().map(PracticeRecord::getUserId).collect(Collectors.toSet());
        Set<Long> bankIds = page.getRecords().stream().map(PracticeRecord::getBankId).filter(Objects::nonNull).collect(Collectors.toSet());

        Map<Long, User> userMap = userIds.isEmpty() ? Collections.emptyMap() :
                userMapper.selectListByIds(userIds).stream().collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));
        Map<Long, QuestionBank> bankMap = bankIds.isEmpty() ? Collections.emptyMap() :
                questionBankMapper.selectListByIds(bankIds).stream().collect(Collectors.toMap(QuestionBank::getId, Function.identity(), (a, b) -> a));

        List<Map<String, Object>> resultList = page.getRecords().stream().map(record -> {
            int totalCount = firstNonNegative(record.getTotalCount(), 0);
            int answerCount = firstNonNegative(record.getAnswerCount(), 0);
            int correctCount = firstNonNegative(record.getCorrectCount(), 0);
            Map<String, Object> summary = buildAnswerSummary(totalCount, answerCount, correctCount);
            Map<String, Object> map = new HashMap<>();
            map.put("id", record.getId());
            map.put("userId", record.getUserId());
            map.put("userNickname", userMap.getOrDefault(record.getUserId(), new User()).getNickname());
            map.put("userPhone", userMap.getOrDefault(record.getUserId(), new User()).getPhone());
            map.put("bankId", record.getBankId());
            map.put("bankName", bankMap.getOrDefault(record.getBankId(), new QuestionBank()).getName());
            map.put("mode", record.getMode());
            map.putAll(summary);
            map.put("status", record.getStatus());
            map.put("createTime", record.getCreateTime());
            return map;
        }).toList();

        return new PageResult<>(resultList, page.getTotalRow(), pageNum, pageSize);
    }

    @Override
    public PageResult<Map<String, Object>> adminExamRecords(String keyword, Long bankId, Integer pageNum, Integer pageSize) {
        // 关键词搜索用户
        List<Long> matchedUserIds = null;
        if (StringUtils.hasText(keyword)) {
            QueryWrapper userQuery = QueryWrapper.create()
                    .select(USER.ID)
                    .where(USER.NICKNAME.like(keyword).or(USER.PHONE.like(keyword)));
            List<User> matchedUsers = userMapper.selectListByQuery(userQuery);
            matchedUserIds = matchedUsers.stream().map(User::getId).toList();
            if (matchedUserIds.isEmpty()) {
                return PageResult.empty(pageNum, pageSize);
            }
        }

        QueryWrapper qw = QueryWrapper.create();
        if (matchedUserIds != null) qw.and(EXAM_RECORD.USER_ID.in(matchedUserIds));
        if (bankId != null) qw.and(EXAM_RECORD.BANK_ID.eq(bankId));
        qw.orderBy(EXAM_RECORD.CREATE_TIME, false);
        Page<ExamRecord> page = examRecordMapper.paginate(Page.of(pageNum, pageSize), qw);

        if (page.getRecords().isEmpty()) {
            return PageResult.empty(pageNum, pageSize);
        }

        // 批量查询用户和题库
        Set<Long> userIds = page.getRecords().stream().map(ExamRecord::getUserId).collect(Collectors.toSet());
        Set<Long> bankIds = page.getRecords().stream().map(ExamRecord::getBankId).filter(Objects::nonNull).collect(Collectors.toSet());

        Map<Long, User> userMap = userIds.isEmpty() ? Collections.emptyMap() :
                userMapper.selectListByIds(userIds).stream().collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));
        Map<Long, QuestionBank> bankMap = bankIds.isEmpty() ? Collections.emptyMap() :
                questionBankMapper.selectListByIds(bankIds).stream().collect(Collectors.toMap(QuestionBank::getId, Function.identity(), (a, b) -> a));
        Map<Long, Long> answerCountMap = loadExamAnswerCountMap(page.getRecords().stream().map(ExamRecord::getId).toList());

        List<Map<String, Object>> resultList = page.getRecords().stream().map(record -> {
            int totalCount = firstNonNegative(record.getTotalCount(), 0);
            int answerCount = answerCountMap.getOrDefault(record.getId(), 0L).intValue();
            int correctCount = firstNonNegative(record.getCorrectCount(), 0);
            Map<String, Object> summary = buildAnswerSummary(totalCount, answerCount, correctCount);
            Map<String, Object> map = new HashMap<>();
            map.put("id", record.getId());
            map.put("userId", record.getUserId());
            map.put("userNickname", userMap.getOrDefault(record.getUserId(), new User()).getNickname());
            map.put("userPhone", userMap.getOrDefault(record.getUserId(), new User()).getPhone());
            map.put("bankId", record.getBankId());
            map.put("bankName", bankMap.getOrDefault(record.getBankId(), new QuestionBank()).getName());
            map.putAll(summary);
            map.put("score", record.getScore());
            map.put("duration", record.getDuration());
            map.put("passScore", record.getPassScore());
            map.put("isPass", record.getIsPass());
            map.put("status", record.getStatus());
            map.put("startTime", record.getStartTime());
            map.put("endTime", record.getEndTime());
            map.put("createTime", record.getCreateTime());
            return map;
        }).toList();

        return new PageResult<>(resultList, page.getTotalRow(), pageNum, pageSize);
    }

    private Map<String, Object> buildAnswerSummary(int totalCount, int answerCount, int correctCount) {
        int total = Math.max(totalCount, 0);
        int answered = Math.max(Math.min(answerCount, total), 0);
        int correct = Math.max(Math.min(correctCount, answered), 0);
        int wrong = Math.max(answered - correct, 0);
        int unanswered = Math.max(total - answered, 0);

        Map<String, Object> summary = new HashMap<>();
        summary.put("totalCount", total);
        summary.put("answerCount", answered);
        summary.put("correctCount", correct);
        summary.put("wrongCount", wrong);
        summary.put("unansweredCount", unanswered);
        return summary;
    }

    private List<Map<String, Object>> filterDetailItems(List<Map<String, Object>> source, String keyword, String result) {
        if ((source == null || source.isEmpty()) && !StringUtils.hasText(keyword) && !StringUtils.hasText(result)) {
            return Collections.emptyList();
        }
        if (!StringUtils.hasText(keyword) && !StringUtils.hasText(result)) {
            return source == null ? Collections.emptyList() : source;
        }
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim().toLowerCase(Locale.ROOT) : null;
        String normalizedResult = StringUtils.hasText(result) ? result.trim().toUpperCase(Locale.ROOT) : null;

        return (source == null ? Collections.<Map<String, Object>>emptyList() : source).stream()
                .filter(item -> matchKeyword(item, normalizedKeyword))
                .filter(item -> matchResult(item, normalizedResult))
                .toList();
    }

    private boolean matchKeyword(Map<String, Object> item, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String content = Objects.toString(item.get("content"), "").toLowerCase(Locale.ROOT);
        String questionId = Objects.toString(item.get("questionId"), "").toLowerCase(Locale.ROOT);
        return content.contains(keyword) || questionId.contains(keyword);
    }

    private boolean matchResult(Map<String, Object> item, String result) {
        if (!StringUtils.hasText(result) || "ALL".equals(result)) {
            return true;
        }
        Integer isCorrect = asInteger(item.get("isCorrect"));
        return switch (result) {
            case "CORRECT" -> Integer.valueOf(1).equals(isCorrect);
            case "WRONG" -> Integer.valueOf(0).equals(isCorrect);
            case "UNANSWERED" -> isCorrect == null;
            default -> true;
        };
    }

    private Integer asInteger(Object value) {
        if (value instanceof Integer integer) {
            return integer;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String str && StringUtils.hasText(str)) {
            try {
                return Integer.parseInt(str.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private int firstNonNegative(Integer primary, int fallback) {
        if (primary != null && primary >= 0) {
            return primary;
        }
        return Math.max(fallback, 0);
    }

    private List<Long> parseQuestionIds(String questionIdsJson) {
        if (!StringUtils.hasText(questionIdsJson)) {
            return Collections.emptyList();
        }
        try {
            List<Long> ids = objectMapper.readValue(questionIdsJson, new TypeReference<List<Long>>() {});
            return ids == null ? Collections.emptyList() : ids.stream().filter(Objects::nonNull).toList();
        } catch (JsonProcessingException e) {
            throw new BizException("解析练习题目列表失败");
        }
    }

    private Map<Long, Long> loadExamAnswerCountMap(List<Long> examIds) {
        if (examIds == null || examIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<ExamAnswer> answers = examAnswerMapper.selectListByQuery(
                QueryWrapper.create()
                        .select(EXAM_ANSWER.EXAM_ID)
                        .where(EXAM_ANSWER.EXAM_ID.in(examIds))
        );
        return answers.stream()
                .filter(answer -> answer.getExamId() != null)
                .collect(Collectors.groupingBy(ExamAnswer::getExamId, Collectors.counting()));
    }

    private <T> PageResult<T> paginateList(List<T> source, Integer pageNum, Integer pageSize) {
        int currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int size = pageSize == null || pageSize < 1 ? 10 : pageSize;
        if (source == null || source.isEmpty()) {
            return PageResult.empty(currentPage, size);
        }
        int fromIndex = Math.min((currentPage - 1) * size, source.size());
        int toIndex = Math.min(fromIndex + size, source.size());
        return PageResult.of(source.subList(fromIndex, toIndex), source.size(), currentPage, size);
    }
}
