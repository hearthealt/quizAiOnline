USE `quiz_ai_online`;

INSERT INTO `sys_config` (`config_key`, `config_value`)
SELECT 'aiPromptAnalysis', `prompt_analysis`
FROM `ai_config`
WHERE `prompt_analysis` IS NOT NULL
  AND `prompt_analysis` <> ''
  AND NOT EXISTS (
    SELECT 1 FROM `sys_config` WHERE `config_key` = 'aiPromptAnalysis'
  );

INSERT INTO `sys_config` (`config_key`, `config_value`)
SELECT 'aiPromptAnswer', `prompt_answer`
FROM `ai_config`
WHERE `prompt_answer` IS NOT NULL
  AND `prompt_answer` <> ''
  AND NOT EXISTS (
    SELECT 1 FROM `sys_config` WHERE `config_key` = 'aiPromptAnswer'
  );

INSERT INTO `sys_config` (`config_key`, `config_value`)
SELECT 'aiPromptBoth', `prompt_both`
FROM `ai_config`
WHERE `prompt_both` IS NOT NULL
  AND `prompt_both` <> ''
  AND NOT EXISTS (
    SELECT 1 FROM `sys_config` WHERE `config_key` = 'aiPromptBoth'
  );

ALTER TABLE `ai_config`
  DROP COLUMN `prompt_analysis`,
  DROP COLUMN `prompt_answer`,
  DROP COLUMN `prompt_both`;
