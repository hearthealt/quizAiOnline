package com.quiz.dto.app;

import lombok.Data;

@Data
public class WxLoginDTO {
    /** wx.login 获取的 code */
    private String code;
    /** 微信昵称（前端通过 nickname input 获取） */
    private String nickname;
    /** 微信头像（前端通过 chooseAvatar 获取） */
    private String avatar;
    /** getPhoneNumber 返回的 code，用于获取手机号 */
    private String phoneCode;
}
