package com.quiz.service.impl;

import com.quiz.config.StpKit;
import com.quiz.dto.app.PhoneLoginDTO;
import com.quiz.dto.app.WxLoginDTO;
import com.quiz.entity.User;
import com.quiz.service.UserService;
import com.quiz.service.WxAuthService;
import com.quiz.vo.app.LoginVO;
import com.quiz.vo.app.UserInfoVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WxAuthServiceImpl implements WxAuthService {

    private final UserService userService;

    @Override
    public LoginVO wxLogin(WxLoginDTO dto) {
        User user = userService.wxLogin(dto);
        StpKit.APP.login(user.getId());
        LoginVO vo = new LoginVO();
        vo.setToken(StpKit.APP.getTokenValue());
        vo.setUserInfo(userService.getUserInfo(user.getId()));
        return vo;
    }

    @Override
    public LoginVO phoneLogin(PhoneLoginDTO dto) {
        User user = userService.phoneLogin(dto.getPhone(), dto.getPassword());
        StpKit.APP.login(user.getId());
        LoginVO vo = new LoginVO();
        vo.setToken(StpKit.APP.getTokenValue());
        vo.setUserInfo(userService.getUserInfo(user.getId()));
        return vo;
    }

    @Override
    public void logout() {
        StpKit.APP.logout();
    }

    @Override
    public UserInfoVO getInfo() {
        Long userId = StpKit.APP.getLoginIdAsLong();
        return userService.getUserInfo(userId);
    }
}
