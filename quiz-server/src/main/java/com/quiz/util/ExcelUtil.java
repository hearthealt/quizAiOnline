package com.quiz.util;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.quiz.common.exception.BizException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Excel 工具类 - 题目导入/模板下载
 */
@Slf4j
public class ExcelUtil {

    private ExcelUtil() {
    }

    /**
     * 题目 Excel 数据模型（选项动态支持任意数量）
     */
    @Data
    public static class QuestionExcelData {
        private String content;      // 题目内容
        private String type;         // 题型
        private String answer;       // 正确答案
        private String analysis;     // 解析
        private String difficulty;   // 难度
        private List<String[]> options = new ArrayList<>(); // 选项列表 [["A","内容"],["B","内容"]...]
    }

    /**
     * 从输入流中读取题目数据（动态读取，支持任意数量选项）
     */
    public static List<QuestionExcelData> readQuestions(InputStream inputStream) {
        List<QuestionExcelData> dataList = new ArrayList<>();
        try {
            EasyExcel.read(inputStream, new ReadListener<Map<Integer, String>>() {
                @Override
                public void invoke(Map<Integer, String> rowData, AnalysisContext context) {
                    QuestionExcelData data = new QuestionExcelData();
                    // 固定列：题目内容(0), 题型(1), 正确答案(2), 解析(3), 难度(4)
                    data.setContent(getCell(rowData, 0));
                    data.setType(getCell(rowData, 1));
                    data.setAnswer(getCell(rowData, 2));
                    data.setAnalysis(getCell(rowData, 3));
                    data.setDifficulty(getCell(rowData, 4));
                    
                    // 动态选项列：从第6列开始，每列是一个选项
                    char label = 'A';
                    for (int i = 5; i < rowData.size(); i++) {
                        String optContent = getCell(rowData, i);
                        if (optContent != null && !optContent.isEmpty()) {
                            data.getOptions().add(new String[]{String.valueOf(label), optContent});
                        }
                        label++;
                    }
                    dataList.add(data);
                }

                @Override
                public void doAfterAllAnalysed(AnalysisContext context) {
                    log.info("Excel 读取完成, 共 {} 条题目数据", dataList.size());
                }
            }).sheet().headRowNumber(1).doRead();
        } catch (Exception e) {
            log.error("读取 Excel 文件失败", e);
            throw new BizException("读取 Excel 文件失败: " + e.getMessage());
        }
        return dataList;
    }

    private static String getCell(Map<Integer, String> rowData, int index) {
        String val = rowData.get(index);
        return val != null ? val.trim() : null;
    }

    /**
     * 模板导出用的数据模型
     */
    @Data
    public static class TemplateData {
        @ExcelProperty("题目内容")
        private String content;
        @ExcelProperty("题型")
        private String type;
        @ExcelProperty("正确答案")
        private String answer;
        @ExcelProperty("解析")
        private String analysis;
        @ExcelProperty("难度")
        private String difficulty;
        @ExcelProperty("选项A")
        private String optionA;
        @ExcelProperty("选项B")
        private String optionB;
        @ExcelProperty("选项C")
        private String optionC;
        @ExcelProperty("选项D")
        private String optionD;
    }

    /**
     * 生成并下载题目导入模板
     */
    public static void getTemplate(HttpServletResponse response) {
        try {
            String fileName = URLEncoder.encode("题目导入模板", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setCharacterEncoding("utf-8");
            response.setHeader("Content-Disposition", "attachment;filename=" + fileName + ".xlsx");

            TemplateData sample = new TemplateData();
            sample.setContent("Java中哪个关键字用于定义类？");
            sample.setType("单选");
            sample.setAnswer("A");
            sample.setAnalysis("class 是 Java 中用于定义类的关键字");
            sample.setDifficulty("简单");
            sample.setOptionA("class");
            sample.setOptionB("interface");
            sample.setOptionC("enum");
            sample.setOptionD("struct");

            EasyExcel.write(response.getOutputStream(), TemplateData.class)
                    .sheet("题目模板")
                    .doWrite(List.of(sample));
        } catch (Exception e) {
            log.error("生成题目导入模板失败", e);
            throw new BizException("生成题目导入模板失败: " + e.getMessage());
        }
    }
}
