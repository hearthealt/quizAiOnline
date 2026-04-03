USE `quiz_ai_online`;

ALTER TABLE `ai_config`
  ADD COLUMN `provider` VARCHAR(20) NOT NULL DEFAULT 'CUSTOM' COMMENT '提供商: OPENAI/DEEPSEEK/CUSTOM' AFTER `id`;

UPDATE `ai_config`
SET `provider` = CASE
  WHEN LOWER(`base_url`) LIKE '%api.openai.com%' THEN 'OPENAI'
  WHEN LOWER(`base_url`) LIKE '%api.deepseek.com%' THEN 'DEEPSEEK'
  ELSE 'CUSTOM'
END;

UPDATE `ai_config`
SET `base_url` = CASE
  WHEN `provider` = 'OPENAI' AND TRIM(TRAILING '/' FROM `base_url`) = 'https://api.openai.com' THEN 'https://api.openai.com/v1'
  WHEN `provider` = 'DEEPSEEK' AND TRIM(TRAILING '/' FROM `base_url`) = 'https://api.deepseek.com' THEN 'https://api.deepseek.com/v1'
  ELSE TRIM(TRAILING '/' FROM `base_url`)
END;

ALTER TABLE `ai_config`
  DROP COLUMN `bx_auth`,
  DROP COLUMN `iflow_name`,
  DROP COLUMN `expire_time`,
  DROP COLUMN `auto_renew`,
  DROP COLUMN `last_renew_time`,
  DROP COLUMN `last_renew_result`;
