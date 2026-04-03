package com.quiz.service.impl;

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
            Map<String, Object> map = new HashMap<>();
            map.put("id", record.getId());
            map.put("userId", record.getUserId());
            map.put("userNickname", userMap.getOrDefault(record.getUserId(), new User()).getNickname());
            map.put("userPhone", userMap.getOrDefault(record.getUserId(), new User()).getPhone());
            map.put("bankId", record.getBankId());
            map.put("bankName", bankMap.getOrDefault(record.getBankId(), new QuestionBank()).getName());
            map.put("mode", record.getMode());
            map.put("totalCount", record.getTotalCount());
            map.put("correctCount", record.getCorrectCount());
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

        List<Map<String, Object>> resultList = page.getRecords().stream().map(record -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", record.getId());
            map.put("userId", record.getUserId());
            map.put("userNickname", userMap.getOrDefault(record.getUserId(), new User()).getNickname());
            map.put("userPhone", userMap.getOrDefault(record.getUserId(), new User()).getPhone());
            map.put("bankId", record.getBankId());
            map.put("bankName", bankMap.getOrDefault(record.getBankId(), new QuestionBank()).getName());
            map.put("totalCount", record.getTotalCount());
            map.put("correctCount", record.getCorrectCount());
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
}
