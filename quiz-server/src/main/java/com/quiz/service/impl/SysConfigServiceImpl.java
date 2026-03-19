package com.quiz.service.impl;

import com.mybatisflex.core.query.QueryWrapper;
import com.quiz.entity.SysConfig;
import com.quiz.mapper.SysConfigMapper;
import com.quiz.service.SysConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.quiz.entity.table.SysConfigTableDef.SYS_CONFIG;

@Service
@RequiredArgsConstructor
public class SysConfigServiceImpl implements SysConfigService {

    private final SysConfigMapper sysConfigMapper;

    @Override
    public Map<String, String> getAll() {
        List<SysConfig> list = sysConfigMapper.selectAll();
        Map<String, String> map = new LinkedHashMap<>();
        for (SysConfig config : list) {
            map.put(config.getConfigKey(), config.getConfigValue());
        }
        return map;
    }

    @Override
    public String getValue(String key) {
        return getValue(key, null);
    }

    @Override
    public String getValue(String key, String defaultValue) {
        SysConfig config = sysConfigMapper.selectOneByQuery(
                QueryWrapper.create().where(SYS_CONFIG.CONFIG_KEY.eq(key)));
        if (config != null && config.getConfigValue() != null) {
            return config.getConfigValue();
        }
        return defaultValue;
    }

    @Override
    @Transactional
    public void batchUpdate(Map<String, String> configs) {
        for (Map.Entry<String, String> entry : configs.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            SysConfig existing = sysConfigMapper.selectOneByQuery(
                    QueryWrapper.create().where(SYS_CONFIG.CONFIG_KEY.eq(key)));

            if (existing != null) {
                existing.setConfigValue(value);
                sysConfigMapper.update(existing);
            } else {
                SysConfig config = new SysConfig();
                config.setConfigKey(key);
                config.setConfigValue(value);
                sysConfigMapper.insert(config);
            }
        }
    }
}
