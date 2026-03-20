package com.quiz.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 题目转换服务 - 智能解析（Excel结构化 / 文本规则解析）
 */
@Slf4j
@Service
public class QuestionConvertService {

    private static final String[] DIFFICULTIES = {"简单", "中等", "困难"};


    // ==================== 答案模式 ====================
    private static final Pattern ANSWER_PATTERN = Pattern.compile(
            "(?:答案|正确答案|参考答案|标准答案|[Aa]nswer|[Kk]ey)\\s*[:：是为]?\\s*" +
                    "([A-Za-z,，、\\s]+|对|错|正确|错误|√|×|[TFtf])",
            Pattern.CASE_INSENSITIVE
    );


    // ==================== 判断题答案值 ====================
    private static final Set<String> TRUE_VALUES = Set.of("对", "正确", "√", "T", "t", "TRUE", "true", "是");
    private static final Set<String> FALSE_VALUES = Set.of("错", "错误", "×", "F", "f", "FALSE", "false", "否");

    // ==================== 智能解析统一入口 ====================

    /**
     * 智能解析文件 - 自动识别格式
     * Excel/CSV: 尝试结构化解析
     * 其他格式: 文本解析 + 规则解析
     */
    public List<Map<String, Object>> parseSmart(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            return new ArrayList<>();
        }

        String ext = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();

        // Excel/CSV 文件：先尝试智能结构化解析
        if (ext.equals("xlsx") || ext.equals("xls") || ext.equals("csv")) {
            try {
                List<Map<String, Object>> result = parseExcelSmart(file);
                if (result != null && !result.isEmpty()) {
                    log.info("智能结构化解析成功，共解析 {} 道题", result.size());
                    return result;
                }
            } catch (Exception e) {
                log.warn("Excel结构化解析失败，回退到文本解析: {}", e.getMessage());
            }
        }

        // 回退到文本解析 + 规则解析
        try {
            String text = com.quiz.util.FileParseUtil.parseToText(file);
            List<Map<String, Object>> questions = parseByRule(text);
            log.info("文本规则解析完成，共解析 {} 道题", questions.size());
            return questions;
        } catch (Exception e) {
            log.error("文件解析失败", e);
            return new ArrayList<>();
        }
    }

    // ==================== Excel/CSV 结构化解析 ====================

    /**
     * Excel/CSV 结构化解析 - 通过表头关键词自动映射列
     * @return 解析结果列表，如果表头匹配失败返回null
     */
    private List<Map<String, Object>> parseExcelSmart(MultipartFile file) throws Exception {
        String filename = file.getOriginalFilename();
        if (filename == null) return null;

        String ext = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();

        if (ext.equals("csv")) {
            return parseCsvSmart(file.getInputStream());
        } else {
            return parseExcelSheetSmart(file.getInputStream());
        }
    }

    private List<Map<String, Object>> parseExcelSheetSmart(InputStream inputStream) {
        List<Map<Integer, String>> allRows = new ArrayList<>();

        EasyExcel.read(inputStream, new ReadListener<Map<Integer, String>>() {
            @Override
            public void invoke(Map<Integer, String> rowData, AnalysisContext context) {
                allRows.add(rowData);
            }
            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {}
        }).sheet().headRowNumber(0).doRead();

        if (allRows.isEmpty()) return null;

        // 第一行作为表头
        Map<Integer, String> headerRow = allRows.get(0);
        Map<String, Integer> columnMapping = detectColumnMapping(headerRow);

        if (columnMapping.get("content") == null) {
            return null; // 无法识别题目内容列，回退
        }

        List<Map<String, Object>> questions = new ArrayList<>();
        int diffIndex = 0;

        for (int i = 1; i < allRows.size(); i++) {
            Map<Integer, String> row = allRows.get(i);
            Map<String, Object> q = buildQuestionFromRow(row, columnMapping, diffIndex);
            if (q != null) {
                questions.add(q);
                diffIndex++;
            }
        }

        log.info("智能Excel解析完成，共解析 {} 道题", questions.size());
        return questions.isEmpty() ? null : questions;
    }

    private List<Map<String, Object>> parseCsvSmart(InputStream inputStream) throws Exception {
        List<String[]> allRows = new ArrayList<>();
        try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(inputStream, java.nio.charset.StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    allRows.add(line.split("[,\t]", -1));
                }
            }
        }

        if (allRows.isEmpty()) return null;

        // 转为 Map<Integer, String> 格式
        Map<Integer, String> headerRow = new HashMap<>();
        String[] headers = allRows.get(0);
        for (int i = 0; i < headers.length; i++) {
            headerRow.put(i, headers[i].trim());
        }

        Map<String, Integer> columnMapping = detectColumnMapping(headerRow);
        if (columnMapping.get("content") == null) return null;

        List<Map<String, Object>> questions = new ArrayList<>();
        int diffIndex = 0;

        for (int i = 1; i < allRows.size(); i++) {
            String[] cells = allRows.get(i);
            Map<Integer, String> row = new HashMap<>();
            for (int j = 0; j < cells.length; j++) {
                row.put(j, cells[j].trim());
            }
            Map<String, Object> q = buildQuestionFromRow(row, columnMapping, diffIndex);
            if (q != null) {
                questions.add(q);
                diffIndex++;
            }
        }

        return questions.isEmpty() ? null : questions;
    }

    /**
     * 检测表头列映射关系
     */
    private Map<String, Integer> detectColumnMapping(Map<Integer, String> headerRow) {
        Map<String, Integer> mapping = new HashMap<>();
        TreeMap<Integer, String> optionColumns = new TreeMap<>(); // 保持顺序

        for (Map.Entry<Integer, String> entry : headerRow.entrySet()) {
            int col = entry.getKey();
            String header = entry.getValue();
            if (header == null || header.trim().isEmpty()) continue;
            header = header.trim();

            // 题目内容
            if (matchesAny(header, "题目内容", "题目", "题干", "问题", "内容", "题面", "试题")) {
                mapping.put("content", col);
            }
            // 题型
            else if (matchesAny(header, "题型", "类型", "题目类型", "试题类型")) {
                mapping.put("type", col);
            }
            // 答案
            else if (matchesAny(header, "答案", "正确答案", "参考答案", "标准答案")) {
                mapping.put("answer", col);
            }
            // 解析
            else if (matchesAny(header, "解析", "详解", "说明", "解答", "答案解析")) {
                mapping.put("analysis", col);
            }
            // 难度
            else if (matchesAny(header, "难度", "难易度", "难度等级")) {
                mapping.put("difficulty", col);
            }
            // 选项 - 多种格式
            else if (header.matches("(?i)选项\\s*[A-Z]|[A-Z]\\s*选项|选项[A-Z]|[A-Z][.、]?|option\\s*[A-Z]")) {
                String letter = header.replaceAll("(?i)[^A-Z]", "");
                if (!letter.isEmpty()) {
                    optionColumns.put(col, letter.substring(0, 1).toUpperCase());
                }
            }
        }

        // 存储选项列映射
        if (!optionColumns.isEmpty()) {
            mapping.put("optionStart", optionColumns.firstEntry().getKey());
            mapping.put("optionCount", optionColumns.size());
        }
        // 如果没有明确的选项列头，尝试将content之后、answer之前的无标题列当作选项
        else if (mapping.get("content") != null) {
            int contentCol = mapping.get("content");
            int maxCol = headerRow.size();
            // 查找content之后是否有连续的列可作为选项
            List<Integer> possibleOptionCols = new ArrayList<>();
            for (int c = 0; c < maxCol; c++) {
                if (!mapping.containsValue(c) && c != contentCol) {
                    String h = headerRow.get(c);
                    // 单字母列头如 A B C D
                    if (h != null && h.trim().matches("(?i)[A-F]")) {
                        possibleOptionCols.add(c);
                    }
                }
            }
            if (!possibleOptionCols.isEmpty()) {
                mapping.put("optionStart", possibleOptionCols.get(0));
                mapping.put("optionCount", possibleOptionCols.size());
            }
        }

        log.debug("列映射结果: {}", mapping);
        return mapping;
    }

    private boolean matchesAny(String value, String... keywords) {
        String lower = value.toLowerCase().trim();
        for (String keyword : keywords) {
            if (lower.equals(keyword.toLowerCase()) || lower.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 从一行数据构建题目
     */
    private Map<String, Object> buildQuestionFromRow(Map<Integer, String> row, Map<String, Integer> mapping, int diffIndex) {
        String content = getCell(row, mapping.get("content"));
        if (content == null || content.isEmpty()) return null;

        Map<String, Object> q = new HashMap<>();
        q.put("content", content);

        // 答案
        String answer = getCell(row, mapping.get("answer"));
        answer = normalizeAnswer(answer != null ? answer : "");
        q.put("answer", answer);

        // 解析
        String analysis = getCell(row, mapping.get("analysis"));
        q.put("analysis", analysis != null ? analysis : "");

        // 选项
        List<String> options = new ArrayList<>();
        Integer optStart = mapping.get("optionStart");
        Integer optCount = mapping.get("optionCount");
        if (optStart != null && optCount != null) {
            for (int i = optStart; i < optStart + optCount; i++) {
                String opt = getCell(row, i);
                if (opt != null && !opt.isEmpty()) {
                    options.add(opt);
                }
            }
        }
        q.put("options", options);

        // 题型
        String type = getCell(row, mapping.get("type"));
        q.put("type", detectType(type, content, answer, options));

        // 难度
        String difficulty = getCell(row, mapping.get("difficulty"));
        q.put("difficulty", normalizeDifficulty(difficulty, diffIndex));

        return q;
    }

    private String getCell(Map<Integer, String> row, Integer index) {
        if (index == null) return null;
        String val = row.get(index);
        return val != null ? val.trim() : null;
    }

    // ==================== 规则解析（增强版） ====================

    /**
     * 规则解析 - 增强版
     */
    public List<Map<String, Object>> parseByRule(String content) {
        // 预处理
        content = preprocess(content);

        List<Map<String, Object>> questions = new ArrayList<>();

        // 按题号分割
        String splitPattern = "(?=(?:^|\\n)\\s*(?:" +
                "\\d+\\s*[.、.）)]" +
                "|[（(]\\s*\\d+\\s*[）)]" +
                "|[\\[【]\\s*\\d+\\s*[\\]】]" +
                "|第\\s*(?:\\d+|[一二三四五六七八九十百]+)\\s*题" +
                "|[Qq]\\s*\\d+" +
                "|题目\\s*(?:\\d+|[一二三四五六七八九十百]+)" +
                "|[一二三四五六七八九十百]+\\s*[.、.)]" +
                "))";
        String[] blocks = content.split(splitPattern);

        int diffIndex = 0;

        for (String block : blocks) {
            block = block.trim();
            if (block.isEmpty()) continue;

            Map<String, Object> q = parseBlock(block, diffIndex);
            if (q != null) {
                questions.add(q);
                diffIndex++;
            }
        }

        return questions;
    }

    /**
     * 解析单个题目块
     */
    private Map<String, Object> parseBlock(String block, int diffIndex) {
        Map<String, Object> q = new HashMap<>();

        // 提取选项
        List<String> options = extractOptions(block);

        // 提取答案
        String answer = extractAnswer(block);

        // 提取解析
        String analysis = extractAnalysis(block);

        // 提取题目内容（移除题号、选项、答案、解析）
        String questionContent = extractContent(block);
        if (questionContent.isEmpty()) return null;

        // 判断题型
        String type = detectType(null, questionContent + " " + block, answer, options);

        q.put("content", questionContent);
        q.put("type", type);
        q.put("answer", answer);
        q.put("analysis", analysis);
        q.put("difficulty", DIFFICULTIES[diffIndex % 3]);
        q.put("options", options);

        return q;
    }

    /**
     * 文本预处理
     */
    private String preprocess(String content) {
        if (content == null || content.isEmpty()) return "";

        // 移除BOM
        content = content.replaceAll("\\uFEFF", "");

        // 统一换行符
        content = content.replaceAll("\\r\\n", "\n").replaceAll("\\r", "\n");

        // 移除页眉页脚噪音
        content = content.replaceAll("(?m)^\\s*第\\s*\\d+\\s*页.*$", "");
        content = content.replaceAll("(?m)^\\s*共\\s*\\d+\\s*页.*$", "");
        content = content.replaceAll("(?m)^\\s*-\\s*\\d+\\s*-\\s*", "");  // - 1 -
        content = content.replaceAll("(?m)^\\s*\\d+\\s*/\\s*\\d+\\s*$", ""); // 1/10

        // 移除纯数字行（可能是页码）但保留可能是答案的行
        content = content.replaceAll("(?m)^\\s*(\\d{1,3})\\s*$", "");

        // 合并断行（PDF常见问题）：如果一行不是题号/选项/答案开头，且较短，合并到上一行
        String[] lines = content.split("\\n");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (i > 0 && !line.trim().isEmpty()
                    && !isQuestionStart(line) && !isOptionStart(line)
                    && !isAnswerStart(line) && !isAnalysisStart(line)) {
                // 如果上一行不为空且当前行是短文本续行
                if (sb.length() > 0 && sb.charAt(sb.length() - 1) != '\n') {
                    sb.append(" ").append(line.trim());
                    continue;
                }
            }
            if (sb.length() > 0) sb.append("\n");
            sb.append(line);
        }
        content = sb.toString();

        // 压缩连续空行
        content = content.replaceAll("\\n{3,}", "\n\n");

        return content.trim();
    }

    private boolean isQuestionStart(String line) {
        return line.trim().matches("\\s*(?:\\d+\\s*[.、.）)]|[（(]\\s*\\d+\\s*[）)]|[\\[【]\\s*\\d+\\s*[\\]】]|第\\s*(?:\\d+|[一二三四五六七八九十百]+)\\s*题|[Qq]\\s*\\d+|题目\\s*(?:\\d+|[一二三四五六七八九十百]+)|[一二三四五六七八九十百]+\\s*[.、.)]).*");
    }

    private boolean isOptionStart(String line) {
        return line.trim().matches("\\s*(?:[（(]\\s*[A-Za-z]\\s*[）)]|[\\[【]\\s*[A-Za-z]\\s*[\\]】]|[A-Za-z]\\s*[.、:：)）]).*");
    }

    private boolean isAnswerStart(String line) {
        return line.trim().matches("(?i)\\s*(?:答案|正确答案|参考答案|标准答案|answer|key)\\s*[:：是为]?.*");
    }

    private boolean isAnalysisStart(String line) {
        return line.trim().matches("(?i)\\s*(?:解析|详解|说明|解答|analysis)\\s*[:：].*");
    }

    /**
     * 提取选项（增强版）
     */
    private List<String> extractOptions(String block) {
        List<String> options = new ArrayList<>();

        // 构建选项匹配正则 - 支持多种格式
        Pattern optPattern = Pattern.compile(
                "(?:^|\\n)\\s*(?:" +
                        "[（(]\\s*([A-Za-z])\\s*[）)]" +    // (A) （A）
                        "|[\\[【]\\s*([A-Za-z])\\s*[\\]】]" + // [A] 【A】
                        "|([A-Za-z])\\s*[.、:：)）]" +        // A. A、 A: A： A) A）
                        ")\\s*(.+?)(?=" +
                        "(?:\\n\\s*(?:[（(]\\s*[A-Za-z]\\s*[）)]|[\\[【]\\s*[A-Za-z]\\s*[\\]】]|[A-Za-z]\\s*[.、:：)）]))" +
                        "|(?:\\n\\s*(?:答案|正确答案|参考答案|标准答案|解析|详解|说明|解答))" +
                        "|$)",
                Pattern.DOTALL | Pattern.CASE_INSENSITIVE
        );

        Matcher optMatcher = optPattern.matcher(block);
        while (optMatcher.find()) {
            String optContent = optMatcher.group(4);
            if (optContent != null) {
                optContent = optContent.trim().replaceAll("[\\r\\n]+", " ").trim();
                if (!optContent.isEmpty()) {
                    options.add(optContent);
                }
            }
        }

        return options;
    }

    /**
     * 提取答案（增强版）
     */
    private String extractAnswer(String block) {
        Matcher ansMatcher = ANSWER_PATTERN.matcher(block);
        if (ansMatcher.find()) {
            String raw = ansMatcher.group(1).trim();
            return normalizeAnswer(raw);
        }
        return "";
    }

    /**
     * 标准化答案格式
     */
    private String normalizeAnswer(String raw) {
        if (raw == null || raw.isEmpty()) return "";

        raw = raw.trim();

        // 判断题答案
        if (TRUE_VALUES.contains(raw)) return "对";
        if (FALSE_VALUES.contains(raw)) return "错";

        // 选择题答案：提取字母
        String letters = raw.replaceAll("[^A-Za-z]", "").toUpperCase();
        if (!letters.isEmpty()) {
            // 去重并排序
            TreeSet<Character> sorted = new TreeSet<>();
            for (char c : letters.toCharArray()) {
                sorted.add(c);
            }
            StringBuilder sb = new StringBuilder();
            for (char c : sorted) {
                if (sb.length() > 0) sb.append(",");
                sb.append(c);
            }
            return sb.toString();
        }

        return raw.toUpperCase();
    }

    /**
     * 提取解析（增强版）
     */
    private String extractAnalysis(String block) {
        Pattern anaPattern = Pattern.compile(
                "(?:解析|详解|说明|解答|[Aa]nalysis)\\s*[:：]\\s*(.+?)(?=\\s*$)",
                Pattern.DOTALL | Pattern.CASE_INSENSITIVE
        );
        Matcher anaMatcher = anaPattern.matcher(block);
        if (anaMatcher.find()) {
            return anaMatcher.group(1).trim().replaceAll("[\\r\\n]+", " ");
        }
        return "";
    }

    /**
     * 提取题目内容
     */
    private String extractContent(String block) {
        String content = block;

        // 移除题号前缀
        content = content.replaceFirst(
                "^\\s*(?:" +
                        "\\d+\\s*[.、.）)]" +
                        "|[（(]\\s*\\d+\\s*[）)]" +
                        "|[\\[【]\\s*\\d+\\s*[\\]】]" +
                        "|第\\s*(?:\\d+|[一二三四五六七八九十百]+)\\s*题[.、:：]?" +
                        "|[Qq]\\s*\\d+[.、:：]?" +
                        "|题目\\s*(?:\\d+|[一二三四五六七八九十百]+)[.、:：]?" +
                        "|[一二三四五六七八九十百]+\\s*[.、.)]" +
                        ")\\s*", "");

        // 移除选项部分
        content = content.replaceAll(
                "(?:^|\\n)\\s*(?:[（(]\\s*[A-Za-z]\\s*[）)]|[\\[【]\\s*[A-Za-z]\\s*[\\]】]|[A-Za-z]\\s*[.、:：)）])\\s*.+", "");

        // 移除答案和解析部分
        content = content.replaceAll(
                "(?i)(?:答案|正确答案|参考答案|标准答案|解析|详解|说明|解答|answer|key|analysis)\\s*[:：是为]?.*", "");

        // 移除题型标记（如果在内容开头）
        content = content.replaceFirst("^\\s*[（(【\\[]\\s*(?:单选|多选|判断|填空|单选题|多选题|判断题|填空题)\\s*[）)】\\]]\\s*", "");

        return content.trim().replaceAll("[\\r\\n]+", " ");
    }

    /**
     * 检测题型
     */
    private String detectType(String explicitType, String contextText, String answer, List<String> options) {
        // 如果有明确的题型标注
        if (explicitType != null && !explicitType.trim().isEmpty()) {
            String t = explicitType.trim();
            if (t.contains("多选") || t.contains("不定项")) return "多选";
            if (t.contains("判断")) return "判断";
            if (t.contains("填空")) return "填空";
            if (t.contains("单选")) return "单选";
        }

        // 根据上下文推断
        if (contextText != null) {
            if (contextText.contains("多选") || contextText.contains("不定项")) return "多选";
            if (contextText.contains("判断")) return "判断";
            if (contextText.contains("填空") || contextText.contains("___") || contextText.matches(".*[（(]\\s{2,}[）)].*")) return "填空";
        }

        // 根据答案推断
        if (answer != null && !answer.isEmpty()) {
            if (TRUE_VALUES.contains(answer) || FALSE_VALUES.contains(answer)) return "判断";
            // 多个字母 = 多选
            String letters = answer.replaceAll("[^A-Za-z]", "");
            if (letters.length() > 1) return "多选";
        }

        // 无选项 = 判断题
        if (options == null || options.isEmpty()) return "判断";

        return "单选";
    }

    /**
     * 标准化难度
     */
    private String normalizeDifficulty(String raw, int diffIndex) {
        if (raw != null && !raw.trim().isEmpty()) {
            String d = raw.trim();
            if (d.contains("简单") || d.contains("容易") || d.contains("easy") || d.equals("1")) return "简单";
            if (d.contains("中等") || d.contains("一般") || d.contains("medium") || d.equals("2")) return "中等";
            if (d.contains("困难") || d.contains("难") || d.contains("hard") || d.equals("3")) return "困难";
        }
        return DIFFICULTIES[diffIndex % 3];
    }
}