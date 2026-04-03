package com.quiz.controller.app;

import com.quiz.common.result.R;
import com.quiz.config.StpKit;
import com.quiz.entity.Category;
import com.quiz.entity.Question;
import com.quiz.entity.QuestionBank;
import com.quiz.service.CategoryService;
import com.quiz.service.QuestionBankService;
import com.quiz.service.QuestionService;
import com.quiz.service.UserService;
import com.quiz.util.AppViewMapper;
import com.quiz.vo.app.HomeVO;
import com.quiz.vo.app.QuestionListVO;
import com.quiz.vo.app.StudyStatsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "小程序-首页")
@RestController
@RequestMapping("/api/app/home")
@RequiredArgsConstructor
public class AppHomeController {

    private final CategoryService categoryService;
    private final QuestionBankService bankService;
    private final QuestionService questionService;
    private final UserService userService;

    @Operation(summary = "首页数据")
    @GetMapping("/index")
    public R<HomeVO> index() {
        Long userId = getUserIdOrNull();
        HomeVO vo = new HomeVO();

        // 分类列表
        List<Category> categories = categoryService.listAll();
        List<HomeVO.CategoryVO> categoryVOList = categories.stream()
                .map(AppViewMapper::toHomeCategoryVO)
                .collect(Collectors.toList());
        vo.setCategories(categoryVOList);

        // 热门题库
        List<QuestionBank> hotBanks = bankService.hotBanks(6);
        List<HomeVO.BankSimpleVO> bankVOList = hotBanks.stream()
                .map(AppViewMapper::toHomeBankSimpleVO)
                .collect(Collectors.toList());
        vo.setHotBanks(bankVOList);

        // 每日一题：取第一个题库的第一道题
        if (!hotBanks.isEmpty()) {
            List<Question> questions = questionService.listByBankId(hotBanks.get(0).getId());
            if (!questions.isEmpty()) {
                vo.setDailyQuestion(questionService.getQuestionListVO(questions.get(0).getId()));
            }
        }

        // 学习统计
        if (userId != null) {
            StudyStatsVO studyStats = userService.getStudyStats(userId);
            vo.setStudyStats(studyStats);
        } else {
            StudyStatsVO studyStats = new StudyStatsVO();
            studyStats.setTotalDays(0);
            studyStats.setTotalAnswered(0);
            studyStats.setCorrectRate(0.0);
            studyStats.setTodayAnswered(0);
            vo.setStudyStats(studyStats);
        }

        return R.ok(vo);
    }

    @Operation(summary = "每日一题")
    @GetMapping("/daily-question")
    public R<QuestionListVO> dailyQuestion() {
        // 随机获取一道题
        List<QuestionBank> banks = bankService.hotBanks(1);
        if (!banks.isEmpty()) {
            List<Question> questions = questionService.listByBankId(banks.get(0).getId());
            if (!questions.isEmpty()) {
                int randomIndex = (int) (Math.random() * questions.size());
                return R.ok(questionService.getQuestionListVO(questions.get(randomIndex).getId()));
            }
        }
        return R.ok(null);
    }

    private Long getUserIdOrNull() {
        return StpKit.APP.isLogin() ? StpKit.APP.getLoginIdAsLong() : null;
    }
}
