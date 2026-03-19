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
import com.quiz.vo.app.FavoriteVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.quiz.entity.table.FavoriteTableDef.FAVORITE;
import static com.quiz.entity.table.UserTableDef.USER;
import static com.quiz.entity.table.QuestionOptionTableDef.QUESTION_OPTION;

@Slf4j
@Service
public class FavoriteServiceImpl implements FavoriteService {

    private static final boolean SHOW_ANALYSIS = true;
    @Autowired
    private FavoriteMapper favoriteMapper;

    @Autowired
    private QuestionMapper questionMapper;

    @Autowired
    private QuestionBankMapper questionBankMapper;

    @Autowired
    private QuestionOptionMapper questionOptionMapper;

    @Autowired
    private UserMapper userMapper;

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
            return true;
        }
    }

    @Override
    public PageResult<FavoriteVO> list(Long userId, Integer pageNum, Integer pageSize) {
        QueryWrapper query = QueryWrapper.create()
                .where(FAVORITE.USER_ID.eq(userId))
                .orderBy(FAVORITE.CREATE_TIME.desc());
        Page<Favorite> page = favoriteMapper.paginate(pageNum, pageSize, query);

        List<FavoriteVO> voList = new ArrayList<>();
        for (Favorite fav : page.getRecords()) {
            FavoriteVO vo = new FavoriteVO();
            vo.setId(fav.getId());
            vo.setQuestionId(fav.getQuestionId());
            vo.setCreateTime(fav.getCreateTime());

            // 查询题目信息
            Question question = questionMapper.selectOneById(fav.getQuestionId());
            if (question != null) {
                vo.setContent(question.getContent());
                vo.setType(question.getType());
                vo.setAnswer(question.getAnswer());
                vo.setAnalysis(SHOW_ANALYSIS ? question.getAnalysis() : null);

                List<QuestionOption> options = questionOptionMapper.selectListByQuery(
                        QueryWrapper.create()
                                .from(QUESTION_OPTION)
                                .where(QUESTION_OPTION.QUESTION_ID.eq(question.getId()))
                                .orderBy(QUESTION_OPTION.SORT.asc()));
                List<FavoriteVO.OptionVO> optionVOs = options.stream().map(opt -> {
                    FavoriteVO.OptionVO optVO = new FavoriteVO.OptionVO();
                    optVO.setLabel(opt.getLabel());
                    optVO.setContent(opt.getContent());
                    return optVO;
                }).collect(Collectors.toList());
                vo.setOptions(optionVOs);

                // 查询题库名称
                if (question.getBankId() != null) {
                    QuestionBank bank = questionBankMapper.selectOneById(question.getBankId());
                    if (bank != null) {
                        vo.setBankName(bank.getName());
                    }
                }
            }
            voList.add(vo);
        }

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
    public PageResult<Map<String, Object>> adminList(String keyword, Integer pageNum, Integer pageSize) {
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
        qw.orderBy(FAVORITE.CREATE_TIME.desc());
        Page<Favorite> page = favoriteMapper.paginate(pageNum, pageSize, qw);

        if (page.getRecords().isEmpty()) {
            return PageResult.empty(pageNum, pageSize);
        }

        // 批量查询用户和题目
        Set<Long> userIds = page.getRecords().stream().map(Favorite::getUserId).collect(Collectors.toSet());
        Set<Long> questionIds2 = page.getRecords().stream().map(Favorite::getQuestionId).filter(Objects::nonNull).collect(Collectors.toSet());

        Map<Long, User> userMap = userIds.isEmpty() ? Collections.emptyMap() :
                userMapper.selectListByIds(userIds).stream().collect(Collectors.toMap(User::getId, Function.identity(), (a, b) -> a));
        Map<Long, Question> questionMap = questionIds2.isEmpty() ? Collections.emptyMap() :
                questionMapper.selectListByIds(questionIds2).stream().collect(Collectors.toMap(Question::getId, Function.identity(), (a, b) -> a));

        List<Map<String, Object>> resultList = page.getRecords().stream().map(fav -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", fav.getId());
            map.put("userId", fav.getUserId());
            map.put("userNickname", userMap.getOrDefault(fav.getUserId(), new User()).getNickname());
            map.put("userPhone", userMap.getOrDefault(fav.getUserId(), new User()).getPhone());
            map.put("questionId", fav.getQuestionId());
            map.put("questionContent", questionMap.getOrDefault(fav.getQuestionId(), new Question()).getContent());
            map.put("createTime", fav.getCreateTime());
            return map;
        }).toList();

        return new PageResult<>(resultList, page.getTotalRow(), pageNum, pageSize);
    }
}
