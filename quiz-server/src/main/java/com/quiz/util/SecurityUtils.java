package com.quiz.util;

import cn.hutool.crypto.digest.BCrypt;
import com.quiz.common.exception.BizException;
import com.quiz.config.StpKit;

/**
 * 安全工具类 - 用户身份 & 密码处理
 */
public class SecurityUtils {

    private SecurityUtils() {
    }

    /**
     * 获取当前 APP 端登录用户 ID
     *
     * @return 用户 ID
     */
    public static Long getAppUserId() {
        try {
            return StpKit.APP.getLoginIdAsLong();
        } catch (Exception e) {
            throw new BizException(401, "未登录或登录已过期");
        }
    }

    /**
     * 获取当前后台管理员 ID
     *
     * @return 管理员 ID
     */
    public static Long getAdminId() {
        try {
            return StpKit.ADMIN.getLoginIdAsLong();
        } catch (Exception e) {
            throw new BizException(401, "未登录或登录已过期");
        }
    }

    /**
     * 对明文密码进行 BCrypt 哈希
     *
     * @param password 明文密码
     * @return 哈希后的密码
     */
    public static String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    /**
     * 校验明文密码与哈希密码是否匹配
     *
     * @param rawPassword    明文密码
     * @param hashedPassword 哈希密码
     * @return 是否匹配
     */
    public static boolean checkPassword(String rawPassword, String hashedPassword) {
        return BCrypt.checkpw(rawPassword, hashedPassword);
    }
}
