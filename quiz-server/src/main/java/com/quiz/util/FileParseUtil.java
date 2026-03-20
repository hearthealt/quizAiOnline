package com.quiz.util;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.quiz.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 文件解析工具类 - 支持 xlsx/csv/pdf/txt
 */
@Slf4j
public class FileParseUtil {

    private FileParseUtil() {
    }

    /**
     * 解析文件内容为文本
     */
    public static String parseToText(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new BizException("文件名不能为空");
        }

        String ext = filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();

        try {
            return switch (ext) {
                case "xlsx", "xls" -> parseExcel(file.getInputStream());
                case "csv" -> parseCsv(file.getInputStream());
                case "pdf" -> parsePdf(file.getBytes());
                case "txt" -> parseTxt(file.getInputStream());
                case "docx" -> parseDocx(file.getInputStream());
                default -> throw new BizException("不支持的文件格式: " + ext);
            };
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("文件解析失败", e);
            throw new BizException("文件解析失败: " + e.getMessage());
        }
    }

    /**
     * 解析 Excel 文件
     */
    private static String parseExcel(InputStream inputStream) {
        StringBuilder sb = new StringBuilder();
        List<Map<Integer, String>> rows = new ArrayList<>();

        EasyExcel.read(inputStream, new ReadListener<Map<Integer, String>>() {
            @Override
            public void invoke(Map<Integer, String> rowData, AnalysisContext context) {
                rows.add(rowData);
            }

            @Override
            public void doAfterAllAnalysed(AnalysisContext context) {
            }
        }).sheet().headRowNumber(0).doRead();

        for (Map<Integer, String> row : rows) {
            List<String> cells = new ArrayList<>();
            for (int i = 0; i < row.size(); i++) {
                String cell = row.get(i);
                if (cell != null && !cell.trim().isEmpty()) {
                    cells.add(cell.trim());
                }
            }
            if (!cells.isEmpty()) {
                sb.append(String.join("\t", cells)).append("\n");
            }
        }

        return sb.toString();
    }

    /**
     * 解析 CSV 文件
     */
    private static String parseCsv(InputStream inputStream) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.trim().isEmpty()) {
                    sb.append(line).append("\n");
                }
            }
        }
        return sb.toString();
    }

    /**
     * 解析 PDF 文件
     */
    private static String parsePdf(byte[] bytes) throws Exception {
        try (PDDocument document = Loader.loadPDF(bytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    /**
     * 解析 TXT 文件
     */
    private static String parseTxt(InputStream inputStream) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * 解析 Word (.docx) 文件
     */
    private static String parseDocx(InputStream inputStream) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            // 读取段落
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String text = paragraph.getText();
                if (text != null && !text.trim().isEmpty()) {
                    sb.append(text.trim()).append("\n");
                }
            }
            // 读取表格（有些题目文件用表格排版）
            for (XWPFTable table : document.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    List<String> cells = new ArrayList<>();
                    for (XWPFTableCell cell : row.getTableCells()) {
                        String text = cell.getText();
                        if (text != null && !text.trim().isEmpty()) {
                            cells.add(text.trim());
                        }
                    }
                    if (!cells.isEmpty()) {
                        sb.append(String.join("\t", cells)).append("\n");
                    }
                }
            }
        }
        return sb.toString();
    }
}
