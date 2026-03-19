package com.quiz.service.impl;

import com.quiz.mapper.StatisticsMapper;
import com.quiz.service.StatisticsService;
import com.quiz.vo.admin.DashboardVO;
import com.quiz.vo.admin.StatisticsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {

    private final StatisticsMapper statisticsMapper;

    @Override
    public DashboardVO getDashboardOverview() {
        DashboardVO vo = new DashboardVO();
        vo.setTotalUsers((long) statisticsMapper.countTotalUsers());
        vo.setTotalQuestions((long) statisticsMapper.countTotalQuestions());
        vo.setTodayActive((long) statisticsMapper.countTodayActiveUsers());
        vo.setTodayAnswers((long) statisticsMapper.countTodayAnswers());
        return vo;
    }

    @Override
    public List<DashboardVO.TrendItem> getDashboardTrend(Integer days) {
        LocalDate startDate = LocalDate.now().minusDays(days);
        LocalDate endDate = LocalDate.now();
        List<Map<String, Object>> rawList = statisticsMapper.getAnswerTrend(startDate, endDate);
        return convertToTrendItems(rawList);
    }

    @Override
    public List<StatisticsVO.RankItem> getBankHotRank(Integer limit) {
        List<Map<String, Object>> rawList = statisticsMapper.getBankHotRank(limit);
        return convertToRankItems(rawList, "bankName", "totalCount");
    }

    @Override
    public List<DashboardVO.TrendItem> getUserGrowth(String startDate, String endDate) {
        LocalDate end = (endDate != null && !endDate.isEmpty()) ? LocalDate.parse(endDate) : LocalDate.now();
        LocalDate start = (startDate != null && !startDate.isEmpty()) ? LocalDate.parse(startDate) : end.minusDays(30);
        List<Map<String, Object>> rawList = statisticsMapper.getUserGrowthTrend(start, end);
        return convertToTrendItems(rawList);
    }

    @Override
    public List<DashboardVO.TrendItem> getAnswerTrend(String startDate, String endDate) {
        LocalDate end = (endDate != null && !endDate.isEmpty()) ? LocalDate.parse(endDate) : LocalDate.now();
        LocalDate start = (startDate != null && !startDate.isEmpty()) ? LocalDate.parse(startDate) : end.minusDays(30);
        List<Map<String, Object>> rawList = statisticsMapper.getAnswerTrend(start, end);
        return convertToTrendItems(rawList);
    }

    @Override
    public List<StatisticsVO.RankItem> getWrongRank(Long bankId, Integer limit) {
        List<Map<String, Object>> rawList = statisticsMapper.getWrongQuestionRank(bankId, limit);
        return convertToRankItems(rawList, "questionContent", "totalWrongCount");
    }

    @Override
    public List<StatisticsVO.RankItem> getAccuracyDist(Long bankId) {
        List<Map<String, Object>> rawList = statisticsMapper.getBankHotRank(10);
        return convertToRankItems(rawList, "bankName", "totalCount");
    }

    @Override
    public List<StatisticsVO.RankItem> getFavoriteRank(Integer limit) {
        List<Map<String, Object>> rawList = statisticsMapper.getFavoriteRank(limit);
        return convertToRankItems(rawList, "questionContent", "favoriteCount");
    }

    /**
     * Convert raw Map list from mapper to DashboardVO.TrendItem list
     */
    private List<DashboardVO.TrendItem> convertToTrendItems(List<Map<String, Object>> rawList) {
        List<DashboardVO.TrendItem> items = new ArrayList<>();
        if (rawList == null) return items;
        for (Map<String, Object> map : rawList) {
            DashboardVO.TrendItem item = new DashboardVO.TrendItem();
            item.setDate(String.valueOf(map.get("date")));
            Object count = map.get("count");
            item.setCount(count instanceof Number ? ((Number) count).longValue() : 0L);
            items.add(item);
        }
        return items;
    }

    /**
     * Convert raw Map list from mapper to StatisticsVO.RankItem list
     */
    private List<StatisticsVO.RankItem> convertToRankItems(List<Map<String, Object>> rawList,
                                                            String nameKey, String countKey) {
        List<StatisticsVO.RankItem> items = new ArrayList<>();
        if (rawList == null) return items;
        for (Map<String, Object> map : rawList) {
            StatisticsVO.RankItem item = new StatisticsVO.RankItem();
            item.setName(String.valueOf(map.getOrDefault(nameKey, "")));
            Object count = map.get(countKey);
            item.setCount(count instanceof Number ? ((Number) count).longValue() : 0L);
            items.add(item);
        }
        return items;
    }
}
