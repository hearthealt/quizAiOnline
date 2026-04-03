package com.quiz.service.impl;

import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.quiz.common.exception.BizException;
import com.quiz.common.result.PageResult;
import com.quiz.dto.app.UpdateProfileDTO;
import com.quiz.dto.app.WxLoginDTO;
import com.quiz.entity.User;
import com.quiz.mapper.PracticeDetailMapper;
import com.quiz.mapper.UserMapper;
import com.quiz.service.SysConfigService;
import com.quiz.service.UserService;
import com.quiz.util.SecurityUtils;
import com.quiz.util.WxUtil;
import com.quiz.vo.app.StudyStatsVO;
import com.quiz.vo.app.UserInfoVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static com.quiz.entity.table.UserTableDef.USER;

@Slf4j
@Service
public class UserServiceImpl implements UserService {

    private static final String DEFAULT_AVATAR = "/static/default-avatar.png";
    private static final String DEFAULT_NICKNAME = "微信用户";
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private PracticeDetailMapper practiceDetailMapper;

    @Autowired
    private SysConfigService sysConfigService;

    @Value("${wx.appid:}")
    private String wxAppId;

    @Value("${wx.secret:}")
    private String wxAppSecret;

    @Override
    @Transactional
    public User wxLogin(WxLoginDTO dto) {
        // 优先从系统配置读取，回退到application.yml配置
        String appId = sysConfigService.getValue("wxAppId", wxAppId);
        String appSecret = sysConfigService.getValue("wxAppSecret", wxAppSecret);
        String openid = WxUtil.getOpenid(dto.getCode(), appId, appSecret);

        QueryWrapper query = QueryWrapper.create()
                .where(USER.OPENID.eq(openid));
        User user = userMapper.selectOneByQuery(query);

        if (user == null) {
            // 检查是否开放注册
            String registerEnabled = sysConfigService.getValue("registerEnabled", "1");
            if (!"1".equals(registerEnabled)) {
                throw new BizException("系统暂未开放注册");
            }

            // 新用户注册，使用微信传来的昵称和头像
            String defaultAvatar = DEFAULT_AVATAR;
            String defaultNickname = DEFAULT_NICKNAME;

            user = new User();
            user.setOpenid(openid);
            String wxNickname = normalize(dto.getNickname());
            String wxAvatar = normalize(dto.getAvatar());
            user.setNickname(!wxNickname.isEmpty() ? wxNickname : defaultNickname);
            user.setAvatar(!wxAvatar.isEmpty() ? wxAvatar : defaultAvatar);
            user.setStatus(1);
            user.setIsVip(0);
            user.setLastLoginTime(LocalDateTime.now());

            // 如果传了 phoneCode，通过微信接口获取手机号
            if (dto.getPhoneCode() != null && !dto.getPhoneCode().isEmpty()) {
                String phone = WxUtil.getPhoneNumber(dto.getPhoneCode(), appId, appSecret);
                if (phone != null) {
                    user.setPhone(phone);
                }
            }

            userMapper.insert(user);
        } else {
            String wxNickname = normalize(dto.getNickname());
            String wxAvatar = normalize(dto.getAvatar());
            String defaultAvatar = DEFAULT_AVATAR;
            String defaultNickname = DEFAULT_NICKNAME;
            // 老用户登录：仅当用户未自定义（默认值或空）时才用微信返回的昵称/头像更新
            String currentNickname = user.getNickname();
            String currentAvatar = user.getAvatar();
            boolean nicknameIsDefault = currentNickname == null || currentNickname.trim().isEmpty() 
                    || defaultNickname.equals(currentNickname.trim());
            boolean avatarIsDefault = currentAvatar == null || currentAvatar.trim().isEmpty() 
                    || defaultAvatar.equals(currentAvatar.trim());
            if (nicknameIsDefault && !wxNickname.isEmpty()) {
                user.setNickname(wxNickname);
            } else if (nicknameIsDefault) {
                user.setNickname(defaultNickname);
            }
            if (avatarIsDefault && !wxAvatar.isEmpty()) {
                user.setAvatar(wxAvatar);
            } else if (avatarIsDefault) {
                user.setAvatar(defaultAvatar);
            }
            // 绑定手机号（之前没有且这次传了）
            if (user.getPhone() == null && dto.getPhoneCode() != null && !dto.getPhoneCode().isEmpty()) {
                String phone = WxUtil.getPhoneNumber(dto.getPhoneCode(), appId, appSecret);
                if (phone != null) {
                    user.setPhone(phone);
                }
            }
            user.setLastLoginTime(LocalDateTime.now());
            userMapper.update(user);
        }

        return user;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    @Override
    public User phoneLogin(String phone, String password) {
        QueryWrapper query = QueryWrapper.create()
                .where(USER.PHONE.eq(phone));
        User user = userMapper.selectOneByQuery(query);

        if (user == null) {
            throw new BizException("手机号或密码错误");
        }
        if (!SecurityUtils.checkPassword(password, user.getPassword())) {
            throw new BizException("手机号或密码错误");
        }
        if (user.getStatus() != 1) {
            throw new BizException("账号已被禁用");
        }

        user.setLastLoginTime(LocalDateTime.now());
        userMapper.update(user);

        return user;
    }

    @Override
    public UserInfoVO getUserInfo(Long userId) {
        User user = userMapper.selectOneById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        boolean isVipActive = user.getIsVip() != null
                && user.getIsVip() == 1
                && user.getVipExpireTime() != null
                && user.getVipExpireTime().isAfter(LocalDateTime.now());
        if (!isVipActive && user.getIsVip() != null && user.getIsVip() == 1) {
            user.setIsVip(0);
            userMapper.update(user);
        }

        UserInfoVO vo = new UserInfoVO();
        vo.setId(user.getId());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setPhone(user.getPhone());
        vo.setIsVip(isVipActive ? 1 : 0);
        vo.setVipExpireTime(user.getVipExpireTime());
        return vo;
    }

    @Override
    @Transactional
    public void updateProfile(Long userId, UpdateProfileDTO dto) {
        User user = userMapper.selectOneById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }

        if (dto.getNickname() != null) {
            user.setNickname(dto.getNickname());
        }
        if (dto.getAvatar() != null) {
            user.setAvatar(dto.getAvatar());
        }
        if (dto.getPhone() != null) {
            String phone = dto.getPhone().trim();
            if (!phone.isEmpty()) {
                QueryWrapper phoneQuery = QueryWrapper.create()
                        .where(USER.PHONE.eq(phone))
                        .and(USER.ID.ne(userId));
                User exists = userMapper.selectOneByQuery(phoneQuery);
                if (exists != null) {
                    throw new BizException("手机号已被占用");
                }
                user.setPhone(phone);
            } else {
                user.setPhone(null);
            }
        }
        userMapper.update(user);
    }

    @Override
    public StudyStatsVO getStudyStats(Long userId) {
        StudyStatsVO vo = new StudyStatsVO();
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        Map<String, Object> stats = practiceDetailMapper.getStudyStats(userId, todayStart);

        long totalAnswered = toLong(stats != null ? stats.get("totalAnswered") : null);
        long correctCount = toLong(stats != null ? stats.get("correctCount") : null);
        vo.setTotalAnswered((int) totalAnswered);
        vo.setTotalDays((int) toLong(stats != null ? stats.get("totalDays") : null));
        vo.setTodayAnswered((int) toLong(stats != null ? stats.get("todayAnswered") : null));
        vo.setCorrectRate(totalAnswered > 0 ? Math.round(correctCount * 10000.0 / totalAnswered) / 100.0 : 0.0);

        return vo;
    }

    private long toLong(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    @Override
    @Transactional
    public void updateSettings(Long userId, String settings) {
        User user = userMapper.selectOneById(userId);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        user.setSettings(settings);
        userMapper.update(user);
    }

    @Override
    public PageResult<User> list(String keyword, Integer status, Integer pageNum, Integer pageSize) {
        QueryWrapper qw = QueryWrapper.create();
        if (keyword != null && !keyword.isEmpty()) {
            qw.and(USER.NICKNAME.like(keyword).or(USER.PHONE.like(keyword)));
        }
        if (status != null) {
            qw.and(USER.STATUS.eq(status));
        }
        qw.orderBy(USER.CREATE_TIME, false);
        Page<User> page = userMapper.paginate(Page.of(pageNum, pageSize), qw);
        return new PageResult<>(page.getRecords(), page.getTotalRow(), pageNum, pageSize);
    }

    @Override
    public User getById(Long id) {
        User user = userMapper.selectOneById(id);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        return user;
    }

    @Override
    @Transactional
    public void updateStatus(Long id, Integer status) {
        User user = userMapper.selectOneById(id);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        user.setStatus(status);
        userMapper.update(user);
    }

    @Override
    @Transactional
    public void setVip(Long id, Integer isVip, LocalDateTime expireTime) {
        User user = userMapper.selectOneById(id);
        if (user == null) {
            throw new BizException("用户不存在");
        }
        log.info("设置 VIP - userId: {}, isVip: {}, expireTime: {}", id, isVip, expireTime);
        user.setIsVip(isVip);
        if (isVip == 1) {
            // 开通 VIP：设置到期时间
            user.setVipExpireTime(expireTime != null ? expireTime : LocalDate.now().plusMonths(1).atStartOfDay());
            log.info("开通 VIP - 设置到期时间：{}", user.getVipExpireTime());
        } else {
            // 取消 VIP：清空到期时间
            user.setVipExpireTime(null);
            log.info("取消 VIP - 清空到期时间");
        }
        // ignoreNulls=false 确保 null 字段也会更新到数据库
        userMapper.update(user, false);
        log.info("VIP 设置完成 - userId: {}, final isVip: {}, final vipExpireTime: {}", id, user.getIsVip(), user.getVipExpireTime());
    }
}
