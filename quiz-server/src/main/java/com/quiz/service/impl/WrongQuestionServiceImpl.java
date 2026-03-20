package com.quiz.service.impl;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.quiz.common.exception.BizException;
import com.quiz.common.result.PageResult;
import com.quiz.entity.Question;
import com.quiz.entity.QuestionBank;
import com.quiz.entity.QuestionOption;
import com.quiz.entity.User;
import com.quiz.entity.WrongQuestion;
import com.quiz.mapper.QuestionBankMapper;
import com.quiz.mapper.QuestionMapper;
import com.quiz.mapper.QuestionOptionMapper;
import com.quiz.mapper.UserMapper;
import com.quiz.mapper.WrongQuestionMapper;
import com.quiz.service.WrongQuestionService;
import com.quiz.vo.app.WrongQuestionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.quiz.entity.table.WrongQuestionTableDef.WRONG_QUESTION;
import static com.quiz.entity.table.UserTableDef.USER;
import static com.quiz.entity.table.QuestionOptionTableDef.QUESTION_OPTION;

@Slf4j
@Service
@RequiredArgsConstructor
public class WrongQuestionServiceImpl implements WrongQuestionService {

    private static final boolean SHOW_ANALYSIS = true;

    private final WrongQuestionMapper wrongQuestionMapper;
    private final QuestionMapper questionMapper;
    private final QuestionBankMapper questionBankMapper;
    private final QuestionOptionMapper questionOptionMapper;
    private final UserMapper userMapper;

    @Override
    public PageResult<WrongQuestionVO> list(Long userId, Long bankId, Integer pageNum, Integer pageSize) {
        QueryWrapper query = QueryWrapper.create()
                .where(WRONG_QUESTION.USER_ID.eq(userId))
                .and(WRONG_QUESTION.BANK_ID.eq(bankId).when(bankId != null))
                .orderBy(WRONG_QUESTION.UPDATE_TIME.desc());
        Page<WrongQuestion> page = wrongQuestionMapper.paginate(pageNum, pageSize, query);

        if (page.getRecords().isEmpty()) {
            return PageResult.of(Collections.emptyList(), 0L, pageNum, pageSize);
        }

        // 批量查询题目
        Set<Long> questionIds = page.getRecords().stream()
                .map(WrongQuestion::getQuestionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Question> questionMap = questionIds.isEmpty() ? Collections.emptyMap() :
                questionMapper.selectListByIds(questionIds).stream()
                        .collect(Collectors.toMap(Question::getId, Function.identity(), (a, b) -> a));

        // 批量查询题库
        Set<Long> bankIds = page.getRecords().stream()
                .map(WrongQuestion::getBankId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, QuestionBank> bankMap = bankIds.isEmpty() ? Collections.emptyMap() :
                questionBankMapper.selectListByIds(bankIds).stream()
                        .collect(Collectors.toMap(QuestionBank::getId, Function.identity(), (a, b) -> a));

        // 批量查询选项
        Map<Long, List<QuestionOption>> optionsMap = questionIds.isEmpty() ? Collections.emptyMap() :
                questionOptionMapper.selectListByQuery(
                        QueryWrapper.create()
                                .where(QUESTION_OPTION.QUESTION_ID.in(questionIds))
                                .orderBy(QUESTION_OPTION.SORT.asc())
                ).stream().collect(Collectors.groupingBy(QuestionOption::getQuestionId));

        List<WrongQuestionVO> voList = page.getRecords().stream().map(wq -> {
            WrongQuestionVO vo = new WrongQuestionVO();
            vo.setId(wq.getId());
            vo.setQuestionId(wq.getQuestionId());
            vo.setBankId(wq.getBankId());
            vo.setWrongCount(wq.getWrongCount());
            vo.setLastWrongAnswer(wq.getLastWrongAnswer());
            vo.setCreateTime(wq.getCreateTime());

            Question question = questionMap.get(wq.getQuestionId());
            if (question != null) {
                vo.setContent(question.getContent());
                vo.setType(question.getType());
                vo.setAnswer(question.getAnswer());
                vo.setAnalysis(SHOW_ANALYSIS ? question.getAnalysis() : null);

                List<QuestionOption> options = optionsMap.getOrDefault(question.getId(), Collections.emptyList());
                List<WrongQuestionVO.OptionVO> optionVOs = options.stream().map(opt -> {
                    WrongQuestionVO.OptionVO optVO = new WrongQuestionVO.OptionVO();
                    optVO.setLabel(opt.getLabel());
                    optVO.setContent(opt.getContent());
                    return optVO;
                }).collect(Collectors.toList());
                vo.setOptions(optionVOs);
            }

            QuestionBank bank = bankMap.get(wq.getBankId());
            if (bank != null) {
                vo.setBankName(bank.getName());
            }

            return vo;
        }).collect(Collectors.toList());

        return PageResult.of(voList, page.getTotalRow(), pageNum, pageSize);
    }

    @Override
    public Map<String, Object> stats(Long userId) {
        QueryWrapper query = QueryWrapper.create()
                .where(WRONG_QUESTION.USER_ID.eq(userId));
        List<WrongQuestion> wrongQuestions = wrongQuestionMapper.selectListByQuery(query);

        // 按题库分组统计
        Map<Long, Integer> bankCountMap = new HashMap<>();
        for (WrongQuestion wq : wrongQuestions) {
            bankCountMap.merge(wq.getBankId(), 1, Integer::sum);
        }

        List<Map<String, Object>> bankStats = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : bankCountMap.entrySet()) {
            Map<String, Object> item = new HashMap<>();
            item.put("bankId", entry.getKey());
            item.put("count", entry.getValue());

            QuestionBank bank = questionBankMapper.selectOneById(entry.getKey());
            item.put("bankName", bank != null ? bank.getName() : "未知题库");
            bankStats.add(item);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("totalCount", wrongQuestions.size());
        result.put("bankStats", bankStats);
        return result;
    }

    @Override
    @Transactional
    public void remove(Long userId, Long id) {
        QueryWrapper query = QueryWrapper.create()
                .where(WRONG_QUESTION.ID.eq(id))
                .and(WRONG_QUESTION.USER_ID.eq(userId));
        long count = wrongQuestionMapper.selectCountByQuery(query);
        if (count == 0) {
            throw new BizException("错题记录不存在");
        }
        wrongQuestionMapper.deleteById(id);
    }

    @Override
    @Transactional
    public void addWrongQuestion(Long userId, Long questionId, Long bankId, String wrongAnswer) {
        QueryWrapper query = QueryWrapper.create()
                .where(WRONG_QUESTION.USER_ID.eq(userId))
                .and(WRONG_QUESTION.QUESTION_ID.eq(questionId));
        WrongQuestion existing = wrongQuestionMapper.selectOneByQuery(query);

        if (existing != null) {
            existing.setWrongCount(existing.getWrongCount() + 1);
            existing.setLastWrongAnswer(wrongAnswer);
            wrongQuestionMapper.update(existing);
        } else {
            WrongQuestion wq = new WrongQuestion();
            wq.setUserId(userId);
            wq.setQuestionId(questionId);
            wq.setBankId(bankId);
            wq.setWrongCount(1);
            wq.setLastWrongAnswer(wrongAnswer);
            wrongQuestionMapper.insert(wq);
        }
    }

    @Override
    public PageResult<Map<String, Object>> adminList(String keyword, Long bankId, Integer pageNum, Integer pageSize) {
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
        if (matchedUserIds != null) qw.and(WRONG_QUESTION.USER_ID.in(matchedUserIds));
        if (bankId != null) qw.and(WRONG_QUESTION.BANK_ID.eq(bankId));
        qw.orderBy(WRONG_QUESTION.CREATE_TIME.desc());
        Page<WrongQuestion> page = wrongQuestionMapper.paginate(pageNum, pageSize, qw);

        if (page.getRecords().isEmpty()) {
            return PageResult.empty(pageNum, pageSize);
        }

        // 批量查询用户、题目、题库
        Set<Long> userIds = page.getRecords().stream().map(WrongQuestion::getUserId).collect(Collectors.toSet());
        Set<Long> questionIds = page.getRecords().stream().map(WrongQuestion::getQuestionId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> bankIds = page.getRecords().stream().map(WrongQuestion::getBankId).filter(Objects::nonNull).collect(Collectors.toSet());

        Map<Long, User> userMap = userIds.isEmpty() ? Collections.emptyMap() :
                userMapper.selectListByIds(userIds).stream().collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));
        Map<Long, Question> questionMap = questionIds.isEmpty() ? Collections.emptyMap() :
                questionMapper.selectListByIds(questionIds).stream().collect(Collectors.toMap(Question::getId, Function.identity(), (a, b) -> a));
        Map<Long, QuestionBank> bankMap = bankIds.isEmpty() ? Collections.emptyMap() :
                questionBankMapper.selectListByIds(bankIds).stream().collect(Collectors.toMap(QuestionBank::getId, Function.identity(), (a, b) -> a));

        List<Map<String, Object>> resultList = page.getRecords().stream().map(wq -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", wq.getId());
            map.put("userId", wq.getUserId());
            map.put("userNickname", userMap.getOrDefault(wq.getUserId(), new User()).getNickname());
            map.put("userPhone", userMap.getOrDefault(wq.getUserId(), new User()).getPhone());
            map.put("questionId", wq.getQuestionId());
            map.put("questionContent", questionMap.getOrDefault(wq.getQuestionId(), new Question()).getContent());
            map.put("bankId", wq.getBankId());
            map.put("bankName", bankMap.getOrDefault(wq.getBankId(), new QuestionBank()).getName());
            map.put("wrongCount", wq.getWrongCount());
            map.put("lastWrongAnswer", wq.getLastWrongAnswer());
            map.put("createTime", wq.getCreateTime());
            map.put("updateTime", wq.getUpdateTime());
            return map;
        }).toList();

        return new PageResult<>(resultList, page.getTotalRow(), pageNum, pageSize);
    }
}
