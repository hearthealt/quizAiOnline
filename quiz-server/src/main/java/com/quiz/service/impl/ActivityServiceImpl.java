package com.quiz.service.impl;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.quiz.common.exception.BizException;
import com.quiz.common.result.PageResult;
import com.quiz.entity.User;
import com.quiz.entity.UserActivityLog;
import com.quiz.mapper.ActivityMapper;
import com.quiz.mapper.UserActivityLogMapper;
import com.quiz.mapper.UserMapper;
import com.quiz.service.ActivityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ActivityServiceImpl implements ActivityService {

    private final ActivityMapper activityMapper;
    private final UserMapper userMapper;
    private final UserActivityLogMapper userActivityLogMapper;

    @Override
    public Map<String, Object> getOverview(String date) {
        DateRange range = parseDateRange(date);
        Map<String, Object> overview = activityMapper.getActivityOverview(range.start(), range.end());
        Map<String, Object> result = overview == null ? new HashMap<>() : new HashMap<>(overview);
        result.put("date", range.date().toString());
        result.putIfAbsent("activeUsers", 0L);
        result.putIfAbsent("loginUsers", 0L);
        result.putIfAbsent("practiceUsers", 0L);
        result.putIfAbsent("practiceAnswers", 0L);
        result.putIfAbsent("examUsers", 0L);
        result.putIfAbsent("examAnswers", 0L);
        result.putIfAbsent("aiUsers", 0L);
        result.putIfAbsent("favoriteUsers", 0L);
        return result;
    }

    @Override
    public PageResult<Map<String, Object>> getActiveUsers(String date, String keyword, Integer pageNum, Integer pageSize) {
        DateRange range = parseDateRange(date);
        int currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int size = pageSize == null || pageSize < 1 ? 10 : pageSize;
        long total = activityMapper.countActiveUsers(range.start(), range.end(), normalizeKeyword(keyword));
        if (total <= 0) {
            return PageResult.empty(currentPage, size);
        }
        List<Map<String, Object>> list = activityMapper.listActiveUsers(
                range.start(),
                range.end(),
                normalizeKeyword(keyword),
                (long) (currentPage - 1) * size,
                size
        );
        return PageResult.of(list, total, currentPage, size);
    }

    @Override
    public Map<String, Object> getUserActivityDetail(Long userId, String date, Integer timelinePageNum, Integer timelinePageSize,
                                                     Integer answerPageNum, Integer answerPageSize) {
        User user = userMapper.selectOneById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        DateRange range = parseDateRange(date);
        int timelinePage = timelinePageNum == null || timelinePageNum < 1 ? 1 : timelinePageNum;
        int timelineSize = timelinePageSize == null || timelinePageSize < 1 ? 10 : Math.min(timelinePageSize, 50);
        int answerPage = answerPageNum == null || answerPageNum < 1 ? 1 : answerPageNum;
        int answerSize = answerPageSize == null || answerPageSize < 1 ? 10 : Math.min(answerPageSize, 50);
        Map<String, Object> payload = new HashMap<>();
        payload.put("date", range.date().toString());
        payload.put("user", buildUserPayload(user));

        Map<String, Object> summary = activityMapper.getActiveUserSummary(range.start(), range.end(), userId);
        if (summary == null) {
            summary = buildEmptySummary(user);
        }
        payload.put("summary", summary);

        long answerTotal = activityMapper.countAnswerDetails(
                range.start(),
                range.end(),
                null,
                null,
                null,
                null,
                userId
        );
        if (answerTotal <= 0) {
            payload.put("answers", PageResult.empty(answerPage, answerSize));
        } else {
            List<Map<String, Object>> answers = activityMapper.listAnswerDetails(
                    range.start(),
                    range.end(),
                    null,
                    null,
                    null,
                    null,
                    userId,
                    (long) (answerPage - 1) * answerSize,
                    answerSize
            );
            payload.put("answers", PageResult.of(answers, answerTotal, answerPage, answerSize));
        }

        Page<UserActivityLog> timeline = userActivityLogMapper.paginate(
                Page.of(timelinePage, timelineSize),
                QueryWrapper.create()
                        .where("user_id = ?", userId)
                        .and("create_time >= ?", range.start())
                        .and("create_time < ?", range.end())
                        .orderBy("create_time", false)
                        .orderBy("id", false)
        );
        payload.put("timeline", PageResult.of(timeline.getRecords(), timeline.getTotalRow(), timelinePage, timelineSize));
        return payload;
    }

    @Override
    public PageResult<Map<String, Object>> getAnswerDetails(String date, String keyword, Long bankId, String source,
                                                            String result, Integer pageNum, Integer pageSize) {
        DateRange range = parseDateRange(date);
        int currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int size = pageSize == null || pageSize < 1 ? 10 : pageSize;
        Integer resultValue = parseResult(result);
        String normalizedSource = normalizeSource(source);
        long total = activityMapper.countAnswerDetails(
                range.start(),
                range.end(),
                normalizeKeyword(keyword),
                bankId,
                normalizedSource,
                resultValue,
                null
        );
        if (total <= 0) {
            return PageResult.empty(currentPage, size);
        }
        List<Map<String, Object>> list = activityMapper.listAnswerDetails(
                range.start(),
                range.end(),
                normalizeKeyword(keyword),
                bankId,
                normalizedSource,
                resultValue,
                null,
                (long) (currentPage - 1) * size,
                size
        );
        return PageResult.of(list, total, currentPage, size);
    }

    private DateRange parseDateRange(String date) {
        LocalDate targetDate;
        try {
            targetDate = StringUtils.hasText(date) ? LocalDate.parse(date.trim()) : LocalDate.now();
        } catch (Exception e) {
            throw new BizException("日期格式不正确");
        }
        return new DateRange(targetDate, targetDate.atStartOfDay(), targetDate.plusDays(1).atStartOfDay());
    }

    private String normalizeKeyword(String keyword) {
        return StringUtils.hasText(keyword) ? keyword.trim() : null;
    }

    private String normalizeSource(String source) {
        if (!StringUtils.hasText(source)) {
            return null;
        }
        String normalized = source.trim().toUpperCase();
        return switch (normalized) {
            case "PRACTICE", "EXAM" -> normalized;
            default -> throw new BizException("来源类型不正确");
        };
    }

    private Integer parseResult(String result) {
        if (!StringUtils.hasText(result) || "ALL".equalsIgnoreCase(result.trim())) {
            return null;
        }
        return switch (result.trim().toUpperCase()) {
            case "CORRECT" -> 1;
            case "WRONG" -> 0;
            case "PENDING" -> -1;
            default -> throw new BizException("结果类型不正确");
        };
    }

    private Map<String, Object> buildEmptySummary(User user) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("userId", user.getId());
        summary.put("userNickname", user.getNickname());
        summary.put("userPhone", user.getPhone());
        summary.put("lastActiveTime", user.getLastLoginTime());
        summary.put("loginCount", 0L);
        summary.put("practiceRecordCount", 0L);
        summary.put("practiceAnswerCount", 0L);
        summary.put("examRecordCount", 0L);
        summary.put("examAnswerCount", 0L);
        summary.put("aiCallCount", 0L);
        summary.put("favoriteCount", 0L);
        summary.put("totalActionCount", 0L);
        return summary;
    }

    private Map<String, Object> buildUserPayload(User user) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", user.getId());
        payload.put("nickname", user.getNickname());
        payload.put("avatar", user.getAvatar());
        payload.put("phone", user.getPhone());
        payload.put("status", user.getStatus());
        payload.put("isVip", user.getIsVip());
        payload.put("vipExpireTime", user.getVipExpireTime());
        payload.put("lastLoginTime", user.getLastLoginTime());
        payload.put("createTime", user.getCreateTime());
        return payload;
    }

    private record DateRange(LocalDate date, LocalDateTime start, LocalDateTime end) {
    }
}
