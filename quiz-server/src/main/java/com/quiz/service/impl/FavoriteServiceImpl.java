package com.quiz.service.impl;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.quiz.common.result.PageResult;
import com.quiz.entity.Favorite;
import com.quiz.entity.Question;
import com.quiz.entity.QuestionBank;
import com.quiz.entity.QuestionOption;
import com.quiz.entity.User;
import com.quiz.mapper.FavoriteMapper;
import com.quiz.mapper.QuestionBankMapper;
import com.quiz.mapper.QuestionMapper;
import com.quiz.mapper.QuestionOptionMapper;
import com.quiz.mapper.UserMapper;
import com.quiz.service.FavoriteService;
import com.quiz.service.UserActivityService;
import com.quiz.util.AppViewMapper;
import com.quiz.vo.app.FavoriteVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.quiz.entity.table.FavoriteTableDef.FAVORITE;
import static com.quiz.entity.table.QuestionTableDef.QUESTION;
import static com.quiz.entity.table.UserTableDef.USER;
import static com.quiz.entity.table.QuestionOptionTableDef.QUESTION_OPTION;

@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteServiceImpl implements FavoriteService {

    private static final boolean SHOW_ANALYSIS = true;

    private final FavoriteMapper favoriteMapper;
    private final QuestionMapper questionMapper;
    private final QuestionBankMapper questionBankMapper;
    private final QuestionOptionMapper questionOptionMapper;
    private final UserMapper userMapper;
    private final UserActivityService userActivityService;

    @Override
    @Transactional
    public boolean toggle(Long userId, Long questionId) {
        QueryWrapper query = QueryWrapper.create()
                .where(FAVORITE.USER_ID.eq(userId))
                .and(FAVORITE.QUESTION_ID.eq(questionId));
        Favorite existing = favoriteMapper.selectOneByQuery(query);

        if (existing != null) {
            favoriteMapper.deleteById(existing.getId());
            return false;
        } else {
            Favorite favorite = new Favorite();
            favorite.setUserId(userId);
            favorite.setQuestionId(questionId);
            favoriteMapper.insert(favorite);
            Question question = questionMapper.selectOneById(questionId);
            userActivityService.recordFavoriteAdd(userId, questionId, question != null ? question.getBankId() : null);
            return true;
        }
    }

    @Override
    public PageResult<FavoriteVO> list(Long userId, Integer pageNum, Integer pageSize) {
        QueryWrapper query = QueryWrapper.create()
                .where(FAVORITE.USER_ID.eq(userId))
                .orderBy(FAVORITE.CREATE_TIME.desc());
        Page<Favorite> page = favoriteMapper.paginate(pageNum, pageSize, query);

        if (page.getRecords().isEmpty()) {
            return PageResult.of(Collections.emptyList(), 0L, pageNum, pageSize);
        }

        // 批量查询题目
        Set<Long> questionIds = page.getRecords().stream()
                .map(Favorite::getQuestionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, Question> questionMap = questionIds.isEmpty() ? Collections.emptyMap() :
                questionMapper.selectListByIds(questionIds).stream()
                        .collect(Collectors.toMap(Question::getId, Function.identity(), (a, b) -> a));

        // 批量查询题库
        Set<Long> bankIds = questionMap.values().stream()
                .map(Question::getBankId)
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

        List<FavoriteVO> voList = page.getRecords().stream().map(fav -> {
            Question question = questionMap.get(fav.getQuestionId());
            QuestionBank bank = question != null ? bankMap.get(question.getBankId()) : null;
            List<QuestionOption> options = question != null
                    ? optionsMap.getOrDefault(question.getId(), Collections.emptyList())
                    : Collections.emptyList();
            return AppViewMapper.toFavoriteVO(fav, question, bank, options, SHOW_ANALYSIS);
        }).collect(Collectors.toList());

        return PageResult.of(voList, page.getTotalRow(), pageNum, pageSize);
    }

    @Override
    public boolean isFavorite(Long userId, Long questionId) {
        QueryWrapper query = QueryWrapper.create()
                .where(FAVORITE.USER_ID.eq(userId))
                .and(FAVORITE.QUESTION_ID.eq(questionId));
        return favoriteMapper.selectCountByQuery(query) > 0;
    }

    @Override
    @Transactional
    public void batchRemove(Long userId, List<Long> questionIds) {
        if (questionIds == null || questionIds.isEmpty()) {
            return;
        }
        QueryWrapper query = QueryWrapper.create()
                .where(FAVORITE.USER_ID.eq(userId))
                .and(FAVORITE.QUESTION_ID.in(questionIds));
        favoriteMapper.deleteByQuery(query);
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
        if (matchedUserIds != null) qw.and(FAVORITE.USER_ID.in(matchedUserIds));
        if (bankId != null) {
            List<Long> matchedQuestionIds = questionMapper.selectListByQuery(
                    QueryWrapper.create()
                            .select(QUESTION.ID)
                            .where(QUESTION.BANK_ID.eq(bankId))
            ).stream().map(Question::getId).filter(Objects::nonNull).toList();
            if (matchedQuestionIds.isEmpty()) {
                return PageResult.empty(pageNum, pageSize);
            }
            qw.and(FAVORITE.QUESTION_ID.in(matchedQuestionIds));
        }
        qw.orderBy(FAVORITE.CREATE_TIME.desc());
        Page<Favorite> page = favoriteMapper.paginate(pageNum, pageSize, qw);

        if (page.getRecords().isEmpty()) {
            return PageResult.empty(pageNum, pageSize);
        }

        // 批量查询用户、题目、题库
        Set<Long> userIds = page.getRecords().stream().map(Favorite::getUserId).collect(Collectors.toSet());
        Set<Long> questionIds2 = page.getRecords().stream().map(Favorite::getQuestionId).filter(Objects::nonNull).collect(Collectors.toSet());

        Map<Long, User> userMap = userIds.isEmpty() ? Collections.emptyMap() :
                userMapper.selectListByIds(userIds).stream().collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));
        Map<Long, Question> questionMap = questionIds2.isEmpty() ? Collections.emptyMap() :
                questionMapper.selectListByIds(questionIds2).stream().collect(Collectors.toMap(Question::getId, Function.identity(), (a, b) -> a));
        Set<Long> bankIds = questionMap.values().stream()
                .map(Question::getBankId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, QuestionBank> bankMap = bankIds.isEmpty() ? Collections.emptyMap() :
                questionBankMapper.selectListByIds(bankIds).stream().collect(Collectors.toMap(QuestionBank::getId, Function.identity(), (a, b) -> a));

        List<Map<String, Object>> resultList = page.getRecords().stream().map(fav -> {
            Question question = questionMap.getOrDefault(fav.getQuestionId(), new Question());
            Map<String, Object> map = new HashMap<>();
            map.put("id", fav.getId());
            map.put("userId", fav.getUserId());
            map.put("userNickname", userMap.getOrDefault(fav.getUserId(), new User()).getNickname());
            map.put("userPhone", userMap.getOrDefault(fav.getUserId(), new User()).getPhone());
            map.put("questionId", fav.getQuestionId());
            map.put("questionContent", question.getContent());
            map.put("bankId", question.getBankId());
            map.put("bankName", bankMap.getOrDefault(question.getBankId(), new QuestionBank()).getName());
            map.put("createTime", fav.getCreateTime());
            return map;
        }).toList();

        return new PageResult<>(resultList, page.getTotalRow(), pageNum, pageSize);
    }
}
