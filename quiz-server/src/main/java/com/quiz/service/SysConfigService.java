package com.quiz.service;

import java.util.Map;

public interface SysConfigService {

    /**
     * 获取所有配置
     */
    Map<String, String> getAll();

    /**
     * 获取单个配置值
     */
    String getValue(String key);

    /**
     * 获取单个配置值，带默认值
     */
    String getValue(String key, String defaultValue);

    /**
     * 批量更新配置
     */
    void batchUpdate(Map<String, String> configs);
}
