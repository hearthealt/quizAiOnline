package com.quiz.common.constant;

/**
 * Redis Key 常量
 */
public final class RedisKey {

    private RedisKey() {
        throw new UnsupportedOperationException("常量类不允许实例化");
    }

    /**
     * Key 前缀
     */
    public static final String PREFIX = "quiz:";

    // ==================== 分类相关 ====================

    /**
     * 分类列表缓存
     */
    public static final String CATEGORY_LIST = PREFIX + "category:list";

    // ==================== 题库相关 ====================

    /**
     * 热门题库 quiz:bank:hot:{limit}
     */
    public static final String BANK_HOT = PREFIX + "bank:hot";

    /**
     * 热门题库缓存索引 quiz:bank:hot:keys
     */
    public static final String BANK_HOT_KEYS = PREFIX + "bank:hot:keys";

    /**
     * 题库详情 quiz:bank:detail:{bankId}
     */
    public static final String BANK_DETAIL = PREFIX + "bank:detail:v2:";

    /**
     * 题库题目列表 quiz:bank:questions:{bankId}
     */
    public static final String BANK_QUESTIONS = PREFIX + "bank:questions:";

    // ==================== 每日一题 ====================

    /**
     * 每日一题 quiz:daily:question:{date}
     */
    public static final String DAILY_QUESTION = PREFIX + "daily:question:";

    // ==================== 用户相关 ====================

    /**
     * 用户统计数据 quiz:user:stats:{userId}
     */
    public static final String USER_STATS = PREFIX + "user:stats:";

    /**
     * 用户答题记录 quiz:user:answer:{userId}
     */
    public static final String USER_ANSWER = PREFIX + "user:answer:";

    /**
     * 用户收藏 quiz:user:favorite:{userId}
     */
    public static final String USER_FAVORITE = PREFIX + "user:favorite:";

    /**
     * 用户错题本 quiz:user:wrong:{userId}
     */
    public static final String USER_WRONG = PREFIX + "user:wrong:";

    // ==================== VIP 相关 ====================

    /**
     * VIP 套餐列表
     */
    public static final String VIP_PLANS = PREFIX + "vip:plans";

    // ==================== AI 相关 ====================

    /**
     * AI 配置
     */
    public static final String AI_CONFIG = PREFIX + "ai:config";

    /**
     * AI 对话记录 quiz:ai:chat:{userId}
     */
    public static final String AI_CHAT = PREFIX + "ai:chat:";

    // ==================== 后台管理 ====================

    /**
     * 后台仪表盘概览
     */
    public static final String DASHBOARD_OVERVIEW = PREFIX + "dashboard:overview";

    /**
     * 后台仪表盘统计 quiz:dashboard:stats:{type}
     */
    public static final String DASHBOARD_STATS = PREFIX + "dashboard:stats:";

    // ==================== 验证码 ====================

    /**
     * 图形验证码 quiz:captcha:{key}
     */
    public static final String CAPTCHA = PREFIX + "captcha:";

    /**
     * 短信/邮件验证码 quiz:verify:code:{phone/email}
     */
    public static final String VERIFY_CODE = PREFIX + "verify:code:";

    // ==================== 考试相关 ====================

    /**
     * 考试会话 quiz:exam:session:{sessionId}
     */
    public static final String EXAM_SESSION = PREFIX + "exam:session:";

    /**
     * 考试答题临时存储 quiz:exam:answer:{sessionId}
     */
    public static final String EXAM_ANSWER = PREFIX + "exam:answer:";

    // ==================== 排行榜 ====================

    /**
     * 答题排行榜
     */
    public static final String RANK_ANSWER = PREFIX + "rank:answer";

    /**
     * 积分排行榜
     */
    public static final String RANK_SCORE = PREFIX + "rank:score";
}
