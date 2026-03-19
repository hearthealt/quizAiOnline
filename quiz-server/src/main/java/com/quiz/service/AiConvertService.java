package com.quiz.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 题目转换服务（规则解析）
 */
@Slf4j
@Service
public class AiConvertService {

    private static final String[] DIFFICULTIES = {"简单", "中等", "困难"};

    /**
     * 规则解析
     */
    public List<Map<String, Object>> parseByRule(String content) {
        List<Map<String, Object>> questions = new ArrayList<>();
        
        // 按题号分割：1. 2. 3. 或 1、 2、 或 一、 二、
        String[] blocks = content.split("(?=(?:^|\\n)\\s*(?:\\d+[.、]|[一二三四五六七八九十]+[.、]))");
        
        int diffIndex = 0;
        
        for (String block : blocks) {
            block = block.trim();
            if (block.isEmpty()) continue;
            
            Map<String, Object> q = new HashMap<>();
            
            // 提取选项
            List<String> options = new ArrayList<>();
            Pattern optPattern = Pattern.compile("(?:^|\\n)\\s*([A-Z])[.、:：\\s]\\s*(.+?)(?=(?:\\n\\s*[A-Z][.、:：\\s])|(?:\\n\\s*(?:答案|正确答案|解析))|$)", Pattern.DOTALL);
            Matcher optMatcher = optPattern.matcher(block);
            while (optMatcher.find()) {
                String optContent = optMatcher.group(2).trim();
                optContent = optContent.replaceAll("[\\r\\n]+", " ").trim();
                if (!optContent.isEmpty()) {
                    options.add(optContent);
                }
            }
            
            // 提取答案
            String answer = "";
            Pattern ansPattern = Pattern.compile("(?:答案|正确答案)[:：]\\s*([A-Za-z,，、]+)", Pattern.CASE_INSENSITIVE);
            Matcher ansMatcher = ansPattern.matcher(block);
            if (ansMatcher.find()) {
                answer = ansMatcher.group(1).trim().toUpperCase();
            }
            
            // 提取解析
            String analysis = "";
            Pattern anaPattern = Pattern.compile("解析[:：]\\s*(.+?)(?=(?:\\n\\s*\\d+[.、])|$)", Pattern.DOTALL);
            Matcher anaMatcher = anaPattern.matcher(block);
            if (anaMatcher.find()) {
                analysis = anaMatcher.group(1).trim().replaceAll("[\\r\\n]+", " ");
            }
            
            // 提取题目内容
            String questionContent = block;
            questionContent = questionContent.replaceFirst("^\\s*(?:\\d+[.、]|[一二三四五六七八九十]+[.、])\\s*", "");
            questionContent = questionContent.replaceAll("(?:^|\\n)\\s*[A-Z][.、:：\\s].+", "");
            questionContent = questionContent.replaceAll("(?:答案|正确答案|解析)[:：].+", "");
            questionContent = questionContent.trim().replaceAll("[\\r\\n]+", " ");
            
            if (questionContent.isEmpty()) continue;
            
            // 判断题型
            String type = "单选";
            if (block.contains("多选") || (answer.length() > 1 && !answer.contains(","))) {
                type = "多选";
            } else if (block.contains("判断") || options.isEmpty()) {
                type = "判断";
            } else if (block.contains("填空")) {
                type = "填空";
            }
            
            q.put("content", questionContent);
            q.put("type", type);
            q.put("answer", answer);
            q.put("analysis", analysis);
            q.put("difficulty", DIFFICULTIES[diffIndex++ % 3]);
            q.put("options", options);
            
            questions.add(q);
        }
        
        return questions;
    }
}
