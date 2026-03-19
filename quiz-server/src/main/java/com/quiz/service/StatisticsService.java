package com.quiz.service;

import com.quiz.vo.admin.DashboardVO;
import com.quiz.vo.admin.StatisticsVO;

import java.util.List;

public interface StatisticsService {

    DashboardVO getDashboardOverview();

    List<DashboardVO.TrendItem> getDashboardTrend(Integer days);

    List<StatisticsVO.RankItem> getBankHotRank(Integer limit);

    List<DashboardVO.TrendItem> getUserGrowth(String startDate, String endDate);

    List<DashboardVO.TrendItem> getAnswerTrend(String startDate, String endDate);

    List<StatisticsVO.RankItem> getWrongRank(Long bankId, Integer limit);

    List<StatisticsVO.RankItem> getAccuracyDist(Long bankId);

    List<StatisticsVO.RankItem> getFavoriteRank(Integer limit);
}
