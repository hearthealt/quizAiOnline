package com.quiz.util;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.quiz.common.exception.BizException;
import lombok.extern.slf4j.Slf4j;

/**
 * 微信小程序工具类
 */
@Slf4j
public class WxUtil {

    private static final String JSCODE2SESSION_URL =
            "https://api.weixin.qq.com/sns/jscode2session?appid={}&secret={}&js_code={}&grant_type=authorization_code";

    private static final String GET_ACCESS_TOKEN_URL =
            "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid={}&secret={}";

    private static final String GET_PHONE_URL =
            "https://api.weixin.qq.com/wxa/business/getuserphonenumber?access_token={}";

    private WxUtil() {
    }

    /**
     * 通过 code 换取用户 openid
     *
     * @param code      小程序登录时获取的 code
     * @param appId     小程序 appId
     * @param appSecret 小程序 appSecret
     * @return openid
     */
    public static String getOpenid(String code, String appId, String appSecret) {
        String url = JSCODE2SESSION_URL
                .replaceFirst("\\{}", appId)
                .replaceFirst("\\{}", appSecret)
                .replaceFirst("\\{}", code);
        try {
            String response = HttpUtil.get(url, 5000);
            log.debug("微信 jscode2session 响应: {}", response);

            JSONObject json = JSONUtil.parseObj(response);

            if (json.containsKey("errcode") && json.getInt("errcode") != 0) {
                log.error("微信登录失败, errcode={}, errmsg={}", json.getInt("errcode"), json.getStr("errmsg"));
                throw new BizException("微信登录失败: " + json.getStr("errmsg"));
            }

            String openid = json.getStr("openid");
            if (openid == null || openid.isEmpty()) {
                throw new BizException("微信登录失败: 未获取到openid");
            }
            return openid;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.error("请求微信接口异常", e);
            throw new BizException("微信登录失败: 请求微信接口异常");
        }
    }

    /**
     * 通过 getPhoneNumber 的 code 获取用户手机号
     *
     * @param phoneCode getPhoneNumber 返回的 code
     * @param appId     小程序 appId
     * @param appSecret 小程序 appSecret
     * @return 手机号，失败返回 null
     */
    public static String getPhoneNumber(String phoneCode, String appId, String appSecret) {
        try {
            // 1. 获取 access_token
            String tokenUrl = GET_ACCESS_TOKEN_URL
                    .replaceFirst("\\{}", appId)
                    .replaceFirst("\\{}", appSecret);
            String tokenResp = HttpUtil.get(tokenUrl, 5000);
            JSONObject tokenJson = JSONUtil.parseObj(tokenResp);
            String accessToken = tokenJson.getStr("access_token");
            if (accessToken == null || accessToken.isEmpty()) {
                log.warn("获取access_token失败: {}", tokenResp);
                return null;
            }

            // 2. 用 code 换手机号
            String phoneUrl = GET_PHONE_URL.replaceFirst("\\{}", accessToken);
            JSONObject body = new JSONObject();
            body.set("code", phoneCode);
            String phoneResp = HttpUtil.post(phoneUrl, body.toString());
            JSONObject phoneJson = JSONUtil.parseObj(phoneResp);

            if (phoneJson.getInt("errcode", -1) != 0) {
                log.warn("获取手机号失败: {}", phoneResp);
                return null;
            }

            JSONObject phoneInfo = phoneJson.getJSONObject("phone_info");
            if (phoneInfo != null) {
                return phoneInfo.getStr("phoneNumber");
            }
            return null;
        } catch (Exception e) {
            log.error("获取微信手机号异常", e);
            return null;
        }
    }
}
