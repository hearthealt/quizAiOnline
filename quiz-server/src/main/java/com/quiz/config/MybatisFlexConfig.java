package com.quiz.config;

import com.mybatisflex.core.FlexGlobalConfig;
import com.mybatisflex.core.audit.AuditManager;
import com.mybatisflex.spring.boot.MyBatisFlexCustomizer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Flex 配置
 */
@Slf4j
@Configuration
public class MybatisFlexConfig implements MyBatisFlexCustomizer {

    @Override
    public void customize(FlexGlobalConfig globalConfig) {
        // 开启审计功能
        AuditManager.setAuditEnable(true);
        AuditManager.setMessageCollector(auditMessage ->
                log.debug("SQL审计: {} | 耗时: {}ms", auditMessage.getFullSql(), auditMessage.getElapsedTime())
        );

        // 设置逻辑删除默认值
        globalConfig.setNormalValueOfLogicDelete(0);
        globalConfig.setDeletedValueOfLogicDelete(1);
    }
}
