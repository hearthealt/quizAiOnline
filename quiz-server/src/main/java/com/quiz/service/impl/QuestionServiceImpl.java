package com.quiz.service.impl;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import static com.mybatisflex.core.query.QueryMethods.max;
import com.quiz.common.exception.BizException;
import com.quiz.common.result.PageResult;
import com.quiz.dto.admin.QuestionDTO;
import com.quiz.entity.Question;
import com.quiz.entity.QuestionBank;
import com.quiz.entity.QuestionOption;
import com.quiz.mapper.FavoriteMapper;
import com.quiz.mapper.QuestionBankMapper;
import com.quiz.mapper.QuestionMapper;
import com.quiz.mapper.QuestionOptionMapper;
import com.quiz.service.QuestionService;
import com.quiz.util.AppViewMapper;
import com.quiz.util.ExcelUtil;
import com.quiz.vo.admin.QuestionDetailVO;
import com.quiz.vo.app.QuestionListVO;
import com.quiz.vo.app.QuestionVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.quiz.entity.table.FavoriteTableDef.FAVORITE;
import static com.quiz.entity.table.QuestionOptionTableDef.QUESTION_OPTION;
import static com.quiz.entity.table.QuestionTableDef.QUESTION;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionServiceImpl implements QuestionService {

    private static final boolean SHOW_ANALYSIS = true;

    private final QuestionMapper questionMapper;
    private final QuestionOptionMapper questionOptionMapper;
    private final QuestionBankMapper questionBankMapper;
    private final FavoriteMapper favoriteMapper;

    @Override
    public PageResult<Question> pageList(Long bankId, Integer type, String keyword, Integer pageNum, Integer pageSize) {
        QueryWrapper query = QueryWrapper.create()
                .where(QUESTION.BANK_ID.eq(bankId).when(bankId != null))
                .and(QUESTION.TYPE.eq(type).when(type != null))
                .and(QUESTION.CONTENT.like(keyword).when(keyword != null && !keyword.isEmpty()))
                .orderBy(QUESTION.SORT.asc(), QUESTION.CREATE_TIME.desc());
        Page<Question> page = questionMapper.paginate(pageNum, pageSize, query);
        return PageResult.of(page.getRecords(), page.getTotalRow(), pageNum, pageSize);
    }

    @Override
    public PageResult<QuestionListVO> pageAppList(Long bankId, String keyword, Integer pageNum, Integer pageSize) {
        PageResult<Question> page = pageList(bankId, null, keyword, pageNum, pageSize);
        List<QuestionListVO> list = page.getList().stream()
                .map(AppViewMapper::toQuestionListVO)
                .collect(Collectors.toList());
        return PageResult.of(list, page.getTotal(), pageNum, pageSize);
    }

    @Override
    public QuestionDetailVO getDetail(Long id) {
        Question question = questionMapper.selectOneById(id);
        if (question == null) {
            throw new BizException("题目不存在");
        }

        // 查询选项
        QueryWrapper optionQuery = QueryWrapper.create()
                .where(QUESTION_OPTION.QUESTION_ID.eq(id))
                .orderBy(QUESTION_OPTION.SORT.asc());
        List<QuestionOption> options = questionOptionMapper.selectListByQuery(optionQuery);

        // 查询题库名称
        String bankName = null;
        if (question.getBankId() != null) {
            QuestionBank bank = questionBankMapper.selectOneById(question.getBankId());
            if (bank != null) {
                bankName = bank.getName();
            }
        }

        return AppViewMapper.toQuestionDetailVO(question, bankName, options);
    }

    @Override
    public QuestionVO getQuestionVO(Long id, Long userId) {
        Question question = questionMapper.selectOneById(id);
        if (question == null) {
            throw new BizException("题目不存在");
        }

        // 查询选项
        QueryWrapper optionQuery = QueryWrapper.create()
                .where(QUESTION_OPTION.QUESTION_ID.eq(id))
                .orderBy(QUESTION_OPTION.SORT.asc());
        List<QuestionOption> options = questionOptionMapper.selectListByQuery(optionQuery);

        QuestionVO vo = AppViewMapper.toQuestionVO(question, options, true, SHOW_ANALYSIS);

        // 检查收藏状态
        if (userId != null) {
            QueryWrapper favQuery = QueryWrapper.create()
                    .where(FAVORITE.USER_ID.eq(userId))
                    .and(FAVORITE.QUESTION_ID.eq(id));
            long favCount = favoriteMapper.selectCountByQuery(favQuery);
            vo.setIsFavorite(favCount > 0);
        } else {
            vo.setIsFavorite(false);
        }

        return vo;
    }

    @Override
    public QuestionListVO getQuestionListVO(Long id) {
        Question question = questionMapper.selectOneById(id);
        if (question == null) {
            throw new BizException("题目不存在");
        }
        return AppViewMapper.toQuestionListVO(question);
    }

    @Override
    @Transactional
    public void create(QuestionDTO dto) {
        Question question = new Question();
        question.setBankId(dto.getBankId());
        question.setType(dto.getType());
        question.setContent(dto.getContent());
        question.setAnswer(dto.getAnswer());
        question.setAnalysis(dto.getAnalysis());
        question.setDifficulty(dto.getDifficulty());
        if (dto.getSort() != null && dto.getSort() > 0) {
            question.setSort(dto.getSort());
        } else {
            Integer maxSort = questionMapper.selectObjectByQueryAs(
                    QueryWrapper.create().select(max(QUESTION.SORT))
                            .where(QUESTION.BANK_ID.eq(dto.getBankId())), Integer.class);
            question.setSort(maxSort != null ? maxSort + 1 : 1);
        }
        question.setStatus(1);
        questionMapper.insert(question);

        // 插入选项
        if (dto.getOptions() != null && !dto.getOptions().isEmpty()) {
            int sortIndex = 0;
            for (QuestionDTO.OptionDTO optDTO : dto.getOptions()) {
                QuestionOption option = new QuestionOption();
                option.setQuestionId(question.getId());
                option.setLabel(optDTO.getLabel());
                option.setContent(optDTO.getContent());
                option.setSort(sortIndex++);
                questionOptionMapper.insert(option);
            }
        }

        // 更新题库题目数量
        updateBankQuestionCount(dto.getBankId());
    }

    @Override
    @Transactional
    public void update(Long id, QuestionDTO dto) {
        Question question = questionMapper.selectOneById(id);
        if (question == null) {
            throw new BizException("题目不存在");
        }

        Long oldBankId = question.getBankId();

        if (dto.getBankId() != null) {
            question.setBankId(dto.getBankId());
        }
        if (dto.getType() != null) {
            question.setType(dto.getType());
        }
        if (dto.getContent() != null) {
            question.setContent(dto.getContent());
        }
        if (dto.getAnswer() != null) {
            question.setAnswer(dto.getAnswer());
        }
        if (dto.getAnalysis() != null) {
            question.setAnalysis(dto.getAnalysis());
        }
        if (dto.getDifficulty() != null) {
            question.setDifficulty(dto.getDifficulty());
        }
        if (dto.getSort() != null) {
            question.setSort(dto.getSort());
        }
        questionMapper.update(question);

        // 删除旧选项，插入新选项
        if (dto.getOptions() != null) {
            QueryWrapper deleteQuery = QueryWrapper.create()
                    .where(QUESTION_OPTION.QUESTION_ID.eq(id));
            questionOptionMapper.deleteByQuery(deleteQuery);

            int sortIndex = 0;
            for (QuestionDTO.OptionDTO optDTO : dto.getOptions()) {
                QuestionOption option = new QuestionOption();
                option.setQuestionId(id);
                option.setLabel(optDTO.getLabel());
                option.setContent(optDTO.getContent());
                option.setSort(sortIndex++);
                questionOptionMapper.insert(option);
            }
        }

        // 更新题库题目数量
        updateBankQuestionCount(question.getBankId());
        if (oldBankId != null && !oldBankId.equals(question.getBankId())) {
            updateBankQuestionCount(oldBankId);
        }
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Question question = questionMapper.selectOneById(id);
        if (question == null) {
            throw new BizException("题目不存在");
        }
        questionMapper.deleteById(id);
        updateBankQuestionCount(question.getBankId());
    }

    @Override
    @Transactional
    public void batchDelete(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        // 收集涉及的bankId
        QueryWrapper query = QueryWrapper.create()
                .select(QUESTION.BANK_ID)
                .where(QUESTION.ID.in(ids));
        List<Question> questions = questionMapper.selectListByQuery(query);
        List<Long> bankIds = questions.stream()
                .map(Question::getBankId)
                .distinct()
                .toList();

        // 批量删除
        questionMapper.deleteBatchByIds(ids);

        // 更新相关题库题目数量
        for (Long bankId : bankIds) {
            updateBankQuestionCount(bankId);
        }
    }

    @Override
    @Transactional
    public QuestionImportResult importFromExcel(Long bankId, InputStream inputStream) {
        QuestionBank bank = questionBankMapper.selectOneById(bankId);
        if (bank == null) {
            throw new BizException("题库不存在");
        }

        List<ExcelUtil.QuestionExcelData> dataList = ExcelUtil.readQuestions(inputStream);
        int successCount = 0;
        int failCount = 0;
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < dataList.size(); i++) {
            ExcelUtil.QuestionExcelData data = dataList.get(i);
            int rowNum = i + 2; // Excel行号（第1行是标题）
            try {
                // 验证必填字段
                if (data.getContent() == null || data.getContent().trim().isEmpty()) {
                    errors.add("第" + rowNum + "行：题目内容不能为空");
                    failCount++;
                    continue;
                }
                if (data.getAnswer() == null || data.getAnswer().trim().isEmpty()) {
                    errors.add("第" + rowNum + "行：正确答案不能为空");
                    failCount++;
                    continue;
                }

                // 解析题型
                int type = parseQuestionType(data.getType());

                // 解析难度
                int difficulty = parseDifficulty(data.getDifficulty());

                Question question = new Question();
                question.setBankId(bankId);
                question.setType(type);
                question.setContent(data.getContent().trim());
                question.setAnswer(data.getAnswer().trim());
                question.setAnalysis(data.getAnalysis());
                question.setDifficulty(difficulty);
                question.setSort(0);
                question.setStatus(1);
                questionMapper.insert(question);

                // 插入选项（支持任意数量选项）
                int sortIndex = 0;
                for (String[] pair : data.getOptions()) {
                    QuestionOption option = new QuestionOption();
                    option.setQuestionId(question.getId());
                    option.setLabel(pair[0]);
                    option.setContent(pair[1]);
                    option.setSort(sortIndex++);
                    questionOptionMapper.insert(option);
                }

                successCount++;
            } catch (Exception e) {
                errors.add("第" + rowNum + "行：" + e.getMessage());
                failCount++;
            }
        }

        // 更新题库题目数量
        updateBankQuestionCount(bankId);

        return new QuestionImportResult(successCount, failCount, errors);
    }

    @Override
    @Transactional
    @SuppressWarnings("unchecked")
    public QuestionImportResult importFromConverted(Long bankId, List<Map<String, Object>> questions) {
        QuestionBank bank = questionBankMapper.selectOneById(bankId);
        if (bank == null) {
            throw new BizException("题库不存在");
        }

        int successCount = 0;
        int failCount = 0;
        List<String> errors = new ArrayList<>();

        for (int i = 0; i < questions.size(); i++) {
            Map<String, Object> data = questions.get(i);
            int rowNum = i + 1;
            try {
                String content = (String) data.get("content");
                String answer = (String) data.get("answer");
                if (content == null || content.trim().isEmpty()) {
                    errors.add("第" + rowNum + "题：题目内容不能为空");
                    failCount++;
                    continue;
                }
                if (answer == null || answer.trim().isEmpty()) {
                    errors.add("第" + rowNum + "题：正确答案不能为空");
                    failCount++;
                    continue;
                }

                String typeStr = (String) data.get("type");
                int type = parseQuestionType(typeStr);
                String diffStr = (String) data.get("difficulty");
                int difficulty = parseDifficulty(diffStr);

                Question question = new Question();
                question.setBankId(bankId);
                question.setType(type);
                question.setContent(content.trim());
                question.setAnswer(answer.trim());
                question.setAnalysis((String) data.get("analysis"));
                question.setDifficulty(difficulty);
                question.setSort(0);
                question.setStatus(1);
                questionMapper.insert(question);

                // 插入选项
                List<String> options = (List<String>) data.get("options");
                if (options != null) {
                    char label = 'A';
                    int sortIndex = 0;
                    for (String optContent : options) {
                        if (optContent != null && !optContent.trim().isEmpty()) {
                            QuestionOption option = new QuestionOption();
                            option.setQuestionId(question.getId());
                            option.setLabel(String.valueOf(label));
                            option.setContent(optContent.trim());
                            option.setSort(sortIndex++);
                            questionOptionMapper.insert(option);
                        }
                        label++;
                    }
                }

                successCount++;
            } catch (Exception e) {
                errors.add("第" + rowNum + "题：" + e.getMessage());
                failCount++;
            }
        }

        updateBankQuestionCount(bankId);
        return new QuestionImportResult(successCount, failCount, errors);
    }

    @Override
    public List<Question> listByBankId(Long bankId) {
        QueryWrapper query = QueryWrapper.create()
                .where(QUESTION.BANK_ID.eq(bankId))
                .and(QUESTION.STATUS.eq(1))
                .orderBy(QUESTION.SORT.asc(), QUESTION.CREATE_TIME.asc());
        return questionMapper.selectListByQuery(query);
    }

    private void updateBankQuestionCount(Long bankId) {
        if (bankId == null) {
            return;
        }
        QueryWrapper countQuery = QueryWrapper.create()
                .where(QUESTION.BANK_ID.eq(bankId))
                .and(QUESTION.STATUS.eq(1));
        long count = questionMapper.selectCountByQuery(countQuery);

        QuestionBank bank = questionBankMapper.selectOneById(bankId);
        if (bank != null) {
            bank.setQuestionCount((int) count);
            questionBankMapper.update(bank);
        }
    }

    private int parseQuestionType(String typeStr) {
        if (typeStr == null || typeStr.trim().isEmpty()) {
            return 1; // 默认单选
        }
        return switch (typeStr.trim()) {
            case "单选", "单选题" -> 1;
            case "多选", "多选题" -> 2;
            case "判断", "判断题" -> 3;
            case "填空", "填空题" -> 4;
            default -> 1;
        };
    }

    private int parseDifficulty(String difficultyStr) {
        if (difficultyStr == null || difficultyStr.trim().isEmpty()) {
            return 1; // 默认简单
        }
        return switch (difficultyStr.trim()) {
            case "简单" -> 1;
            case "中等" -> 2;
            case "困难" -> 3;
            default -> 1;
        };
    }
}
