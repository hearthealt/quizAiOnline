package com.quiz.util;

import com.quiz.entity.Category;
import com.quiz.entity.Favorite;
import com.quiz.entity.ExamAnswer;
import com.quiz.entity.Question;
import com.quiz.entity.QuestionBank;
import com.quiz.entity.QuestionOption;
import com.quiz.entity.WrongQuestion;
import com.quiz.vo.admin.QuestionDetailVO;
import com.quiz.vo.app.ExamResultVO;
import com.quiz.vo.app.ExamSessionVO;
import com.quiz.vo.app.FavoriteVO;
import com.quiz.vo.app.HomeVO;
import com.quiz.vo.app.QuestionListVO;
import com.quiz.vo.app.QuestionVO;
import com.quiz.vo.app.WrongQuestionVO;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class AppViewMapper {

    private AppViewMapper() {
    }

    public static HomeVO.CategoryVO toHomeCategoryVO(Category category) {
        HomeVO.CategoryVO vo = new HomeVO.CategoryVO();
        vo.setId(category.getId());
        vo.setName(category.getName());
        vo.setIcon(category.getIcon());
        return vo;
    }

    public static HomeVO.BankSimpleVO toHomeBankSimpleVO(QuestionBank bank) {
        HomeVO.BankSimpleVO vo = new HomeVO.BankSimpleVO();
        vo.setId(bank.getId());
        vo.setName(bank.getName());
        vo.setCover(bank.getCover());
        vo.setQuestionCount(bank.getQuestionCount());
        vo.setPracticeCount(bank.getPracticeCount());
        return vo;
    }

    public static QuestionListVO toQuestionListVO(Question question) {
        QuestionListVO vo = new QuestionListVO();
        vo.setId(question.getId());
        vo.setBankId(question.getBankId());
        vo.setType(question.getType());
        vo.setContent(question.getContent());
        vo.setDifficulty(question.getDifficulty());
        return vo;
    }

    public static QuestionVO toQuestionVO(Question question,
                                          List<QuestionOption> options,
                                          boolean includeAnswer,
                                          boolean includeAnalysis) {
        QuestionVO vo = new QuestionVO();
        vo.setId(question.getId());
        vo.setBankId(question.getBankId());
        vo.setType(question.getType());
        vo.setContent(question.getContent());
        vo.setAnswer(includeAnswer ? question.getAnswer() : null);
        vo.setAnalysis(includeAnalysis ? question.getAnalysis() : null);
        vo.setDifficulty(question.getDifficulty());
        vo.setIsFavorite(false);
        vo.setOptions(toQuestionOptionVOs(options));
        return vo;
    }

    public static List<QuestionVO.OptionVO> toQuestionOptionVOs(List<QuestionOption> options) {
        if (options == null || options.isEmpty()) {
            return Collections.emptyList();
        }
        return options.stream().map(option -> {
            QuestionVO.OptionVO vo = new QuestionVO.OptionVO();
            vo.setLabel(option.getLabel());
            vo.setContent(option.getContent());
            return vo;
        }).collect(Collectors.toList());
    }

    public static FavoriteVO toFavoriteVO(Favorite favorite,
                                          Question question,
                                          QuestionBank bank,
                                          List<QuestionOption> options,
                                          boolean includeAnalysis) {
        FavoriteVO vo = new FavoriteVO();
        vo.setId(favorite.getId());
        vo.setQuestionId(favorite.getQuestionId());
        vo.setCreateTime(favorite.getCreateTime());

        if (question != null) {
            vo.setContent(question.getContent());
            vo.setType(question.getType());
            vo.setAnswer(question.getAnswer());
            vo.setAnalysis(includeAnalysis ? question.getAnalysis() : null);
            vo.setOptions(toFavoriteOptionVOs(options));
        } else {
            vo.setOptions(Collections.emptyList());
        }

        if (bank != null) {
            vo.setBankName(bank.getName());
        }
        return vo;
    }

    public static WrongQuestionVO toWrongQuestionVO(WrongQuestion wrongQuestion,
                                                    Question question,
                                                    QuestionBank bank,
                                                    List<QuestionOption> options,
                                                    boolean includeAnalysis) {
        WrongQuestionVO vo = new WrongQuestionVO();
        vo.setId(wrongQuestion.getId());
        vo.setQuestionId(wrongQuestion.getQuestionId());
        vo.setBankId(wrongQuestion.getBankId());
        vo.setWrongCount(wrongQuestion.getWrongCount());
        vo.setLastWrongAnswer(wrongQuestion.getLastWrongAnswer());
        vo.setCreateTime(wrongQuestion.getCreateTime());

        if (question != null) {
            vo.setContent(question.getContent());
            vo.setType(question.getType());
            vo.setAnswer(question.getAnswer());
            vo.setAnalysis(includeAnalysis ? question.getAnalysis() : null);
            vo.setOptions(toWrongQuestionOptionVOs(options));
        } else {
            vo.setOptions(Collections.emptyList());
        }

        if (bank != null) {
            vo.setBankName(bank.getName());
        }
        return vo;
    }

    public static QuestionDetailVO toQuestionDetailVO(Question question,
                                                      String bankName,
                                                      List<QuestionOption> options) {
        QuestionDetailVO vo = new QuestionDetailVO();
        vo.setId(question.getId());
        vo.setBankId(question.getBankId());
        vo.setBankName(bankName);
        vo.setType(question.getType());
        vo.setContent(question.getContent());
        vo.setAnswer(question.getAnswer());
        vo.setAnalysis(question.getAnalysis());
        vo.setDifficulty(question.getDifficulty());
        vo.setSort(question.getSort());
        vo.setOptions(toQuestionDetailOptionVOs(options));
        return vo;
    }

    public static ExamSessionVO.ExamQuestionVO toExamQuestionVO(Question question,
                                                                List<QuestionOption> options) {
        ExamSessionVO.ExamQuestionVO vo = new ExamSessionVO.ExamQuestionVO();
        vo.setId(question.getId());
        vo.setType(question.getType());
        vo.setContent(question.getContent());
        vo.setOptions(toExamQuestionOptionVOs(options));
        return vo;
    }

    public static ExamResultVO.ExamAnswerDetail toExamAnswerDetail(ExamAnswer answer, Question question) {
        ExamResultVO.ExamAnswerDetail detail = new ExamResultVO.ExamAnswerDetail();
        detail.setQuestionId(answer.getQuestionId());
        detail.setUserAnswer(answer.getUserAnswer());
        detail.setIsCorrect(answer.getIsCorrect() == 1);
        if (question != null) {
            detail.setContent(question.getContent());
            detail.setCorrectAnswer(question.getAnswer());
            detail.setAnalysis(question.getAnalysis());
        }
        return detail;
    }

    private static List<FavoriteVO.OptionVO> toFavoriteOptionVOs(List<QuestionOption> options) {
        if (options == null || options.isEmpty()) {
            return Collections.emptyList();
        }
        return options.stream().map(option -> {
            FavoriteVO.OptionVO vo = new FavoriteVO.OptionVO();
            vo.setLabel(option.getLabel());
            vo.setContent(option.getContent());
            return vo;
        }).collect(Collectors.toList());
    }

    private static List<WrongQuestionVO.OptionVO> toWrongQuestionOptionVOs(List<QuestionOption> options) {
        if (options == null || options.isEmpty()) {
            return Collections.emptyList();
        }
        return options.stream().map(option -> {
            WrongQuestionVO.OptionVO vo = new WrongQuestionVO.OptionVO();
            vo.setLabel(option.getLabel());
            vo.setContent(option.getContent());
            return vo;
        }).collect(Collectors.toList());
    }

    private static List<QuestionDetailVO.OptionVO> toQuestionDetailOptionVOs(List<QuestionOption> options) {
        if (options == null || options.isEmpty()) {
            return Collections.emptyList();
        }
        return options.stream().map(option -> {
            QuestionDetailVO.OptionVO vo = new QuestionDetailVO.OptionVO();
            vo.setLabel(option.getLabel());
            vo.setContent(option.getContent());
            return vo;
        }).collect(Collectors.toList());
    }

    private static List<ExamSessionVO.ExamQuestionOptionVO> toExamQuestionOptionVOs(List<QuestionOption> options) {
        if (options == null || options.isEmpty()) {
            return Collections.emptyList();
        }
        return options.stream().map(option -> {
            ExamSessionVO.ExamQuestionOptionVO vo = new ExamSessionVO.ExamQuestionOptionVO();
            vo.setLabel(option.getLabel());
            vo.setContent(option.getContent());
            return vo;
        }).collect(Collectors.toList());
    }
}
