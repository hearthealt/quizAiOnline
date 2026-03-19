package com.quiz.service;

import com.quiz.dto.app.PhoneLoginDTO;
import com.quiz.dto.app.WxLoginDTO;
import com.quiz.vo.app.LoginVO;
import com.quiz.vo.app.UserInfoVO;

public interface WxAuthService {

    LoginVO wxLogin(WxLoginDTO dto);

    LoginVO phoneLogin(PhoneLoginDTO dto);

    void logout();

    UserInfoVO getInfo();
}
