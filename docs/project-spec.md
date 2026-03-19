# 在线答题系统 - 完整产品方案

---

## 一、系统整体架构设计

```
┌──────────────────────────────────────────────────────────────┐
│                        用户层                                 │
│  ┌─────────────────────┐    ┌──────────────────────────┐     │
│  │  UniApp 小程序端     │    │  Vue3 管理后台            │     │
│  │  (Vue3 + uView Plus)│    │  (Naive UI + ECharts)    │     │
│  │  VIP解析展示/遮挡    │    │  AI生成解析(题目管理)    │     │
│  └────────┬────────────┘    └───────────┬──────────────┘     │
└───────────┼─────────────────────────────┼────────────────────┘
            │ HTTPS                       │ HTTPS
┌───────────▼─────────────────────────────▼────────────────────┐
│                       Nginx 反向代理                          │
│           /api/app/** → 小程序接口                            │
│           /api/admin/** → 管理端接口                          │
│           /admin/** → 管理后台静态资源                         │
└───────────────────────┬──────────────────────────────────────┘
                        │
┌───────────────────────▼──────────────────────────────────────┐
│                   Spring Boot 3 后端服务                      │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ ┌────────────────┐  │
│  │ Sa-Token │ │ Knife4j  │ │  Hutool  │ │ MyBatis-Flex   │  │
│  │ 认证鉴权  │ │ API文档  │ │ 工具集   │ │ 数据持久层      │  │
│  │ +VIP校验  │ │          │ │          │ │                │  │
│  └──────────┘ └──────────┘ └──────────┘ └────────────────┘  │
│  ┌───────────────────────────────────────────────────────┐   │
│  │           AiAnalysisService (管理端调用)               │   │
│  │  iFlow API (OpenAI兼容) → 生成解析/答案 → 写入DB       │   │
│  │  管理后台可配置: base_url / api_key / model            │   │
│  └───────────────────────────────────────────────────────┘   │
└──────┬───────────────────────────────┬───────────┬───────────┘
       │                               │           │
┌──────▼──────┐                 ┌──────▼──────┐    │  HTTPS
│   Redis     │                 │   MySQL     │    │
│  缓存/会话   │                 │  业务数据    │    ▼
│  +VIP状态    │                 │  +解析字段   │  ┌─────────────┐
└─────────────┘                 └─────────────┘  │ iFlow AI API│
                                                 │ (可配置)     │
                                                 └─────────────┘
```

**技术选型明细：**

| 层级     | 技术                     | 版本    | 用途                     |
| -------- | ------------------------ | ------- | ------------------------ |
| 小程序端 | UniApp + Vue3            | 3.x     | 跨端小程序开发           |
| 小程序UI | uView Plus               | 3.x     | 小程序组件库             |
| 管理前端 | Vue3 + Vite + TypeScript | 3.4+    | 管理后台 SPA             |
| 管理UI   | Naive UI                 | 2.x     | 后台组件库               |
| 图表     | ECharts                  | 5.x     | 数据可视化               |
| HTTP     | Axios                    | 1.x     | HTTP 请求               |
| 后端框架 | Spring Boot              | 3.2+    | 后端主框架               |
| ORM      | MyBatis-Flex             | 1.8+    | 数据持久层               |
| 认证     | Sa-Token                 | 1.38+   | 认证鉴权                 |
| 缓存     | Redis (Lettuce)          | 7.x     | 缓存 + 会话存储          |
| 数据库   | MySQL                    | 8.0+    | 关系型数据存储           |
| 文档     | Knife4j                  | 4.x     | API 文档                |
| 工具     | Hutool                   | 5.8+    | Java 工具集              |
| AI 服务  | iFlow API (OpenAI兼容)   | -       | 管理端题目导入时AI生成解析/答案 |
| 流式输出 | SSE (SseEmitter)         | -       | AI 解析实时流式推送        |

---

## 二、功能模块拆解

### 2.1 小程序端功能

| 模块       | 功能点                                           | 优先级 |
| ---------- | ------------------------------------------------ | ------ |
| 认证模块   | 微信登录、手机号登录、退出登录                     | P0     |
| 首页       | 题库分类展示、热门题库推荐、每日一题、学习数据概览 | P0     |
| 题库       | 题库列表、分类筛选、题库详情、题目数量展示         | P0     |
| 练习模式   | 顺序练习、随机练习、按章节练习、答题进度保存       | P0     |
| 模拟考试   | 限时考试、自动评分、考试结果、答题卡               | P0     |
| 收藏       | 收藏题目、取消收藏、收藏列表、收藏夹练习           | P1     |
| 错题本     | 自动收录错题、错题练习、错题统计、移除错题         | P1     |
| 做题记录   | 历史记录列表、答题详情回顾、成绩趋势               | P1     |
| 个人中心   | 个人信息、修改头像昵称、学习统计、设置             | P1     |
| 搜索       | 题目关键词搜索、题库搜索                           | P2     |
| VIP会员    | VIP 开通/续费、会员权益展示、会员中心               | P1     |
| AI智能解析 | VIP专属：查看AI生成的题目解析，非VIP遮挡+开通提示   | P1     |
| 设置       | 答题设置（自动翻页等）、清除缓存、关于             | P2     |

### 2.2 管理后台功能

| 模块         | 功能点                                             | 优先级 |
| ------------ | -------------------------------------------------- | ------ |
| 管理员认证   | 管理员登录、退出、修改密码                           | P0     |
| 仪表盘       | 用户总数、今日活跃、题目总数、答题次数、趋势图表     | P0     |
| 题库分类管理 | 分类 CRUD、排序、启用/禁用                           | P0     |
| 题库管理     | 题库 CRUD、关联分类、题目数量统计                    | P0     |
| 题目管理     | 题目 CRUD、批量导入(Excel)、按题库筛选、题型筛选    | P0     |
| 用户管理     | 用户列表、状态管理（启用/禁用）、用户详情             | P1     |
| 答题记录管理 | 记录列表、按用户/题库筛选、详情查看                  | P1     |
| 收藏管理     | 收藏数据统计、热门收藏题目                           | P2     |
| 错题管理     | 错题统计、高频错题排行                               | P2     |
| 考试记录管理 | 考试记录列表、成绩分布、通过率统计                   | P1     |
| 数据统计     | 用户增长、答题趋势、题库热度、正确率分布             | P1     |
| VIP管理      | VIP套餐配置、VIP订单列表、会员用户列表、收入统计     | P1     |
| AI配置       | AI模型/Key/Prompt模板配置、批量生成解析、调用日志     | P1     |

---

## 三、小程序页面结构

```
pages/
├── login/                     # 登录页
│   └── index.vue
├── index/                     # 首页（Tab页）
│   └── index.vue
├── category/                  # 题库分类页（Tab页）
│   └── index.vue
├── category/
│   └── detail.vue             # 分类下题库列表
├── bank/
│   ├── detail.vue             # 题库详情页
│   └── list.vue               # 题库列表（全部）
├── practice/
│   ├── index.vue              # 练习答题页
│   └── result.vue             # 练习结果页
├── exam/
│   ├── index.vue              # 模拟考试答题页
│   ├── result.vue             # 考试结果页
│   └── list.vue               # 考试记录列表
├── search/
│   └── index.vue              # 搜索页
├── favorite/
│   └── index.vue              # 收藏列表页
├── wrong/
│   └── index.vue              # 错题本页
├── record/
│   ├── index.vue              # 做题记录列表页
│   └── detail.vue             # 记录详情页
├── vip/
│   ├── index.vue              # VIP会员中心（套餐+权益展示+开通）
│   └── success.vue            # 开通成功页
├── mine/                      # 个人中心（Tab页）
│   ├── index.vue
│   ├── profile.vue            # 编辑个人信息
│   └── settings.vue           # 设置页
```

**Tab 页结构（底部导航）：**

| Tab    | 图标     | 页面路径        |
| ------ | -------- | --------------- |
| 首页   | home     | pages/index     |
| 题库   | category | pages/category  |
| 错题   | wrong    | pages/wrong     |
| 我的   | user     | pages/mine      |

**页面功能明细：**

| 页面             | 核心组件/功能                                                 |
| ---------------- | ------------------------------------------------------------- |
| 登录页           | 微信一键登录按钮、手机号登录表单                                |
| 首页             | 搜索栏、轮播推荐、分类九宫格、热门题库列表、每日一题卡片、学习数据 |
| 题库分类页       | 分类列表（左侧分类+右侧题库）                                  |
| 题库详情页       | 题库信息、题目数/已答数/正确率、开始练习/模拟考试按钮           |
| 练习答题页       | 题目展示、选项选择、上下题切换、答题卡、收藏按钮、**解析区域(VIP可见/非VIP遮挡+开通入口)** |
| 模拟考试页       | 倒计时、题目展示、答题卡、交卷按钮                              |
| 考试结果页       | 得分、正确率、用时、错题回顾入口                                |
| 搜索页           | 搜索输入框、搜索历史、搜索结果列表                              |
| 收藏列表页       | 收藏题目列表、取消收藏、开始练习                                |
| 错题本页         | 错题列表、按题库筛选、错题练习、移除错题                        |
| 做题记录列表页   | 记录列表（按时间倒序）、筛选                                    |
| 记录详情页       | 答题详情回顾、每题答案对比                                      |
| VIP会员中心页    | VIP套餐卡片列表、权益说明、当前VIP状态、开通/续费按钮            |
| 个人中心页       | 头像昵称、VIP标识、学习天数、总答题数、正确率、功能入口列表      |
| 编辑个人信息页   | 修改头像、昵称                                                  |
| 设置页           | 答题设置、清除缓存、关于系统、退出登录                          |

---

## 四、管理后台页面结构

```
src/
├── views/
│   ├── login/
│   │   └── index.vue              # 登录页
│   ├── dashboard/
│   │   └── index.vue              # 仪表盘
│   ├── category/
│   │   └── index.vue              # 题库分类管理
│   ├── bank/
│   │   ├── index.vue              # 题库管理列表
│   │   └── detail.vue             # 题库详情/编辑
│   ├── question/
│   │   ├── index.vue              # 题目管理列表
│   │   ├── form.vue               # 题目新增/编辑
│   │   └── import.vue             # 批量导入
│   ├── user/
│   │   ├── index.vue              # 用户管理列表
│   │   └── detail.vue             # 用户详情
│   ├── record/
│   │   ├── practice.vue           # 练习记录
│   │   └── exam.vue               # 考试记录
│   ├── favorite/
│   │   └── index.vue              # 收藏管理
│   ├── wrong/
│   │   └── index.vue              # 错题管理
│   ├── statistics/
│   │   └── index.vue              # 数据统计
│   ├── vip/
│   │   ├── plan.vue               # VIP套餐配置
│   │   └── order.vue              # VIP订单列表
│   ├── ai/
│   │   ├── config.vue             # AI配置（base_url/api_key/model/prompt）
│   │   └── log.vue                # AI调用日志
│   └── system/
│       ├── admin.vue              # 管理员管理
│       └── setting.vue            # 系统设置
├── layouts/
│   ├── BasicLayout.vue            # 带侧边栏布局
│   └── BlankLayout.vue            # 空白布局（登录页）
├── router/
│   └── index.ts
├── api/                           # 按模块拆分 API
│   ├── auth.ts
│   ├── dashboard.ts
│   ├── category.ts
│   ├── bank.ts
│   ├── question.ts
│   ├── user.ts
│   ├── record.ts
│   ├── statistics.ts
│   ├── vip.ts
│   ├── ai.ts
│   └── index.ts
├── stores/                        # Pinia 状态管理
│   ├── auth.ts
│   └── app.ts
├── utils/
│   ├── request.ts                 # Axios 封装
│   └── index.ts
├── composables/                   # 组合式函数
│   ├── useTable.ts                # 表格通用逻辑
│   └── useForm.ts                 # 表单通用逻辑
└── types/
    └── index.ts                   # TypeScript 类型定义
```

**管理后台侧边栏菜单结构：**

| 一级菜单 | 二级菜单     | 路由路径                | 图标         |
| -------- | ------------ | ----------------------- | ------------ |
| 仪表盘   | -            | /dashboard              | DashboardOutlined |
| 题库管理 | 分类管理     | /category               | FolderOutlined    |
|          | 题库列表     | /bank                   | BookOutlined      |
|          | 题目管理     | /question               | FileTextOutlined  |
| 用户管理 | 用户列表     | /user                   | UserOutlined      |
| 记录管理 | 练习记录     | /record/practice        | EditOutlined      |
|          | 考试记录     | /record/exam            | FormOutlined      |
| 数据中心 | 收藏管理     | /favorite               | HeartOutlined     |
|          | 错题管理     | /wrong                  | CloseCircleOutlined |
|          | 数据统计     | /statistics             | BarChartOutlined  |
| VIP管理  | 套餐配置     | /vip/plan               | CrownOutlined     |
|          | 订单列表     | /vip/order              | WalletOutlined    |
| AI配置   | 模型配置     | /ai/config              | RobotOutlined     |
|          | 调用日志     | /ai/log                 | FileSearchOutlined|
| 系统管理 | 管理员管理   | /system/admin           | SettingOutlined   |
|          | 系统设置     | /system/setting         | ToolOutlined      |

---

## 五、后端项目结构

```
quiz-server/
├── pom.xml
├── src/main/java/com/quiz/
│   ├── QuizApplication.java                 # 启动类
│   │
│   ├── common/                              # 公共模块
│   │   ├── result/
│   │   │   ├── R.java                       # 统一响应体
│   │   │   └── PageResult.java              # 分页响应体
│   │   ├── exception/
│   │   │   ├── BizException.java            # 业务异常
│   │   │   └── GlobalExceptionHandler.java  # 全局异常处理
│   │   ├── constant/
│   │   │   ├── RedisKey.java                # Redis Key 常量
│   │   │   └── CommonConstant.java          # 通用常量
│   │   └── enums/
│   │       ├── QuestionType.java            # 题目类型枚举
│   │       ├── ExamStatus.java              # 考试状态枚举
│   │       └── UserStatus.java              # 用户状态枚举
│   │
│   ├── config/                              # 配置类
│   │   ├── SaTokenConfig.java               # Sa-Token 配置
│   │   ├── RedisConfig.java                 # Redis 配置
│   │   ├── MybatisFlexConfig.java           # MyBatis-Flex 配置
│   │   ├── WebMvcConfig.java                # Web MVC 配置
│   │   ├── Knife4jConfig.java               # API文档配置
│   │   └── CorsConfig.java                  # 跨域配置
│   │
│   ├── entity/                              # 实体类
│   │   ├── base/
│   │   │   └── BaseEntity.java              # 基础实体（id, createTime, updateTime）
│   │   ├── User.java
│   │   ├── Admin.java
│   │   ├── Category.java
│   │   ├── QuestionBank.java
│   │   ├── Question.java
│   │   ├── QuestionOption.java
│   │   ├── PracticeRecord.java
│   │   ├── PracticeDetail.java
│   │   ├── ExamRecord.java
│   │   ├── ExamAnswer.java
│   │   ├── Favorite.java
│   │   ├── WrongQuestion.java
│   │   ├── VipPlan.java
│   │   ├── VipOrder.java
│   │   ├── AiConfig.java
│   │   └── AiCallLog.java
│   │
│   ├── mapper/                              # Mapper 接口
│   │   ├── UserMapper.java
│   │   ├── AdminMapper.java
│   │   ├── CategoryMapper.java
│   │   ├── QuestionBankMapper.java
│   │   ├── QuestionMapper.java
│   │   ├── QuestionOptionMapper.java
│   │   ├── PracticeRecordMapper.java
│   │   ├── PracticeDetailMapper.java
│   │   ├── ExamRecordMapper.java
│   │   ├── ExamAnswerMapper.java
│   │   ├── FavoriteMapper.java
│   │   ├── WrongQuestionMapper.java
│   │   ├── VipPlanMapper.java
│   │   ├── VipOrderMapper.java
│   │   ├── AiConfigMapper.java
│   │   └── AiCallLogMapper.java
│   │
│   ├── service/                             # Service 接口层
│   │   ├── UserService.java
│   │   ├── AdminService.java
│   │   ├── CategoryService.java
│   │   ├── QuestionBankService.java
│   │   ├── QuestionService.java
│   │   ├── PracticeService.java
│   │   ├── ExamService.java
│   │   ├── FavoriteService.java
│   │   ├── WrongQuestionService.java
│   │   ├── StatisticsService.java
│   │   ├── WxAuthService.java
│   │   ├── VipService.java
│   │   ├── AiAnalysisService.java
│   │   └── impl/                            # Service 实现层
│   │       ├── UserServiceImpl.java
│   │       ├── AdminServiceImpl.java
│   │       ├── CategoryServiceImpl.java
│   │       ├── QuestionBankServiceImpl.java
│   │       ├── QuestionServiceImpl.java
│   │       ├── PracticeServiceImpl.java
│   │       ├── ExamServiceImpl.java
│   │       ├── FavoriteServiceImpl.java
│   │       ├── WrongQuestionServiceImpl.java
│   │       ├── StatisticsServiceImpl.java
│   │       ├── WxAuthServiceImpl.java
│   │       ├── VipServiceImpl.java
│   │       └── AiAnalysisServiceImpl.java
│   │
│   ├── controller/
│   │   ├── app/                             # 小程序端接口
│   │   │   ├── AppAuthController.java
│   │   │   ├── AppHomeController.java
│   │   │   ├── AppCategoryController.java
│   │   │   ├── AppBankController.java
│   │   │   ├── AppQuestionController.java
│   │   │   ├── AppPracticeController.java
│   │   │   ├── AppExamController.java
│   │   │   ├── AppFavoriteController.java
│   │   │   ├── AppWrongController.java
│   │   │   ├── AppRecordController.java
│   │   │   ├── AppSearchController.java
│   │   │   ├── AppUserController.java
│   │   │   └── AppVipController.java
│   │   └── admin/                           # 管理端接口
│   │       ├── AdminAuthController.java
│   │       ├── AdminDashController.java
│   │       ├── AdminCategoryController.java
│   │       ├── AdminBankController.java
│   │       ├── AdminQuestionController.java
│   │       ├── AdminUserController.java
│   │       ├── AdminRecordController.java
│   │       ├── AdminFavoriteController.java
│   │       ├── AdminWrongController.java
│   │       ├── AdminStatController.java
│   │       ├── AdminSystemController.java
│   │       ├── AdminVipController.java
│   │       └── AdminAiController.java
│   │
│   ├── dto/                                 # 数据传输对象
│   │   ├── app/
│   │   │   ├── WxLoginDTO.java
│   │   │   ├── PhoneLoginDTO.java
│   │   │   ├── SubmitAnswerDTO.java
│   │   │   ├── SubmitExamDTO.java
│   │   │   └── UpdateProfileDTO.java
│   │   └── admin/
│   │       ├── AdminLoginDTO.java
│   │       ├── CategoryDTO.java
│   │       ├── QuestionBankDTO.java
│   │       ├── QuestionDTO.java
│   │       ├── QuestionImportDTO.java
│   │       ├── AdminQueryDTO.java
│   │       ├── VipPlanDTO.java
│   │       └── AiConfigDTO.java
│   │
│   ├── vo/                                  # 视图对象
│   │   ├── app/
│   │   │   ├── HomeVO.java
│   │   │   ├── BankDetailVO.java
│   │   │   ├── QuestionVO.java
│   │   │   ├── PracticeResultVO.java
│   │   │   ├── ExamResultVO.java
│   │   │   ├── UserInfoVO.java
│   │   │   ├── StudyStatsVO.java
│   │   │   └── VipInfoVO.java
│   │   └── admin/
│   │       ├── DashboardVO.java
│   │       ├── QuestionDetailVO.java
│   │       └── StatisticsVO.java
│   │
│   └── util/                                # 工具类
│       ├── WxUtil.java                      # 微信工具
│       └── ExcelUtil.java                   # Excel导入工具
│
├── src/main/resources/
│   ├── application.yml                      # 主配置
│   ├── application-dev.yml                  # 开发环境
│   ├── application-prod.yml                 # 生产环境
│   └── mapper/                              # XML映射文件（复杂查询用）
│       ├── QuestionMapper.xml
│       └── StatisticsMapper.xml
```

---

## 六、小程序端接口设计

### 6.1 认证模块

| 方法 | 路径                         | 说明           | 请求参数                           | 返回         |
| ---- | ---------------------------- | -------------- | ---------------------------------- | ------------ |
| POST | `/api/app/auth/wx-login`     | 微信登录       | `{ code: string }`                 | token + 用户信息 |
| POST | `/api/app/auth/phone-login`  | 手机号登录     | `{ phone, password }`              | token + 用户信息 |
| POST | `/api/app/auth/logout`       | 退出登录       | -                                  | -            |
| GET  | `/api/app/auth/info`         | 获取当前用户信息 | -                                | 用户信息     |

### 6.2 首页模块

| 方法 | 路径                              | 说明           | 请求参数 | 返回                           |
| ---- | --------------------------------- | -------------- | -------- | ------------------------------ |
| GET  | `/api/app/home/index`             | 首页聚合数据   | -        | 分类列表+热门题库+每日一题+学习数据 |
| GET  | `/api/app/home/daily-question`    | 每日一题       | -        | 题目详情                       |

### 6.3 题库分类模块

| 方法 | 路径                                    | 说明             | 请求参数       | 返回           |
| ---- | --------------------------------------- | ---------------- | -------------- | -------------- |
| GET  | `/api/app/category/list`               | 分类列表          | -              | 分类列表       |
| GET  | `/api/app/category/{id}/banks`         | 分类下的题库列表  | `pageNum, pageSize` | 题库分页列表 |

### 6.4 题库模块

| 方法 | 路径                                | 说明           | 请求参数              | 返回                   |
| ---- | ----------------------------------- | -------------- | --------------------- | ---------------------- |
| GET  | `/api/app/bank/list`               | 题库列表(全部)  | `categoryId?, pageNum, pageSize` | 题库分页列表   |
| GET  | `/api/app/bank/{id}`               | 题库详情        | -                     | 题库信息+题目数+用户进度 |
| GET  | `/api/app/bank/{id}/questions`     | 题库下题目列表  | `pageNum, pageSize`    | 题目分页列表           |
| GET  | `/api/app/bank/hot`                | 热门题库        | `limit`                | 题库列表               |

### 6.5 练习模块

| 方法 | 路径                                   | 说明             | 请求参数                           | 返回             |
| ---- | -------------------------------------- | ---------------- | ---------------------------------- | ---------------- |
| POST | `/api/app/practice/start`             | 开始练习          | `{ bankId, mode: "ORDER/RANDOM/CHAPTER", chapterId? }` | 练习记录ID+首题 |
| GET  | `/api/app/practice/{id}/question`     | 获取当前题目      | `index`                            | 题目详情         |
| POST | `/api/app/practice/{id}/submit`       | 提交单题答案      | `{ questionId, answer }`           | 是否正确+解析    |
| POST | `/api/app/practice/{id}/finish`       | 结束练习          | -                                  | 练习结果统计     |
| GET  | `/api/app/practice/{id}/progress`     | 获取练习进度      | -                                  | 已答/总数/正确数 |

### 6.6 考试模块

| 方法 | 路径                                | 说明           | 请求参数                           | 返回             |
| ---- | ----------------------------------- | -------------- | ---------------------------------- | ---------------- |
| POST | `/api/app/exam/start`              | 开始考试        | `{ bankId }`                       | 考试记录ID+题目列表 |
| POST | `/api/app/exam/{id}/submit`        | 交卷            | `{ answers: [{questionId, answer}] }` | 考试结果       |
| GET  | `/api/app/exam/{id}/result`        | 考试结果详情    | -                                  | 分数+用时+答题详情 |
| GET  | `/api/app/exam/records`            | 考试记录列表    | `pageNum, pageSize`                | 记录分页列表     |

### 6.7 收藏模块

| 方法   | 路径                                | 说明         | 请求参数              | 返回           |
| ------ | ----------------------------------- | ------------ | --------------------- | -------------- |
| POST   | `/api/app/favorite/toggle`         | 收藏/取消收藏 | `{ questionId }`      | 收藏状态       |
| GET    | `/api/app/favorite/list`           | 收藏列表      | `pageNum, pageSize`   | 题目分页列表   |
| GET    | `/api/app/favorite/check/{questionId}` | 检查是否已收藏 | -                  | boolean        |
| DELETE | `/api/app/favorite/batch`          | 批量取消收藏  | `{ questionIds[] }`   | -              |

### 6.8 错题本模块

| 方法   | 路径                                | 说明           | 请求参数                 | 返回           |
| ------ | ----------------------------------- | -------------- | ------------------------ | -------------- |
| GET    | `/api/app/wrong/list`              | 错题列表        | `bankId?, pageNum, pageSize` | 错题分页列表 |
| GET    | `/api/app/wrong/stats`             | 错题统计        | -                        | 各题库错题数   |
| POST   | `/api/app/wrong/practice`          | 开始错题练习    | `{ bankId? }`            | 练习ID+首题    |
| DELETE | `/api/app/wrong/{id}`              | 移除错题        | -                        | -              |

### 6.9 做题记录模块

| 方法 | 路径                                | 说明           | 请求参数              | 返回           |
| ---- | ----------------------------------- | -------------- | --------------------- | -------------- |
| GET  | `/api/app/record/list`             | 记录列表        | `type?, pageNum, pageSize` | 记录分页列表 |
| GET  | `/api/app/record/{id}/detail`      | 记录详情        | -                     | 答题明细       |

### 6.10 搜索模块

| 方法 | 路径                           | 说明         | 请求参数              | 返回           |
| ---- | ------------------------------ | ------------ | --------------------- | -------------- |
| GET  | `/api/app/search`             | 搜索题目      | `keyword, pageNum, pageSize` | 题目分页列表 |
| GET  | `/api/app/search/hot`         | 热门搜索词    | -                     | 搜索词列表     |

### 6.11 用户模块

| 方法 | 路径                              | 说明           | 请求参数                     | 返回         |
| ---- | --------------------------------- | -------------- | ---------------------------- | ------------ |
| GET  | `/api/app/user/profile`          | 个人信息        | -                            | 用户详情     |
| PUT  | `/api/app/user/profile`          | 修改个人信息    | `{ nickname?, avatar? }`     | -            |
| GET  | `/api/app/user/study-stats`      | 学习统计        | -                            | 统计数据     |
| PUT  | `/api/app/user/settings`         | 更新设置        | `{ autoNext?, showAnalysis? }` | -          |

### 6.12 VIP会员模块

| 方法 | 路径                              | 说明             | 请求参数              | 返回                     |
| ---- | --------------------------------- | ---------------- | --------------------- | ------------------------ |
| GET  | `/api/app/vip/plans`             | VIP套餐列表       | -                     | 套餐列表(价格/时长/权益)  |
| GET  | `/api/app/vip/status`            | 当前VIP状态       | -                     | 是否VIP+到期时间          |
| POST | `/api/app/vip/order`             | 创建VIP订单       | `{ planId }`          | 订单ID+支付参数           |
| POST | `/api/app/vip/order/{id}/pay`    | 确认支付(模拟)    | `{ paymentMethod }`   | 支付结果                  |
| GET  | `/api/app/vip/orders`            | 我的订单列表      | `pageNum, pageSize`   | 订单分页列表              |

> **解析展示逻辑说明**：小程序获取题目详情时，`analysis` 字段始终返回。前端根据 `user.isVip` 判断：VIP 用户直接展示解析内容，非 VIP 用户用模糊遮罩覆盖解析区域并显示"开通VIP查看完整解析"引导按钮。

---

## 七、管理端接口设计

### 7.1 认证模块

| 方法 | 路径                           | 说明         | 请求参数                   |
| ---- | ------------------------------ | ------------ | -------------------------- |
| POST | `/api/admin/auth/login`       | 管理员登录    | `{ username, password }`   |
| POST | `/api/admin/auth/logout`      | 退出登录      | -                          |
| GET  | `/api/admin/auth/info`        | 当前管理员信息 | -                         |
| PUT  | `/api/admin/auth/password`    | 修改密码      | `{ oldPassword, newPassword }` |

### 7.2 仪表盘

| 方法 | 路径                                 | 说明           |
| ---- | ------------------------------------ | -------------- |
| GET  | `/api/admin/dashboard/overview`     | 总览数据（用户数/题目数/今日答题等） |
| GET  | `/api/admin/dashboard/trend`        | 近7/30天趋势数据 |
| GET  | `/api/admin/dashboard/rank`         | 题库热度排行    |

### 7.3 题库分类管理

| 方法   | 路径                              | 说明         | 请求参数                           |
| ------ | --------------------------------- | ------------ | ---------------------------------- |
| GET    | `/api/admin/category/list`       | 分类列表      | `keyword?, pageNum, pageSize`      |
| GET    | `/api/admin/category/all`        | 全部分类(下拉) | -                                  |
| POST   | `/api/admin/category`            | 新增分类      | `{ name, icon?, sort, status }`    |
| PUT    | `/api/admin/category/{id}`       | 编辑分类      | `{ name, icon?, sort, status }`    |
| DELETE | `/api/admin/category/{id}`       | 删除分类      | -                                  |
| PUT    | `/api/admin/category/{id}/status`| 切换状态      | `{ status }`                       |

### 7.4 题库管理

| 方法   | 路径                            | 说明         | 请求参数                                              |
| ------ | ------------------------------- | ------------ | ----------------------------------------------------- |
| GET    | `/api/admin/bank/list`         | 题库列表      | `categoryId?, keyword?, pageNum, pageSize`             |
| GET    | `/api/admin/bank/{id}`         | 题库详情      | -                                                     |
| POST   | `/api/admin/bank`              | 新增题库      | `{ categoryId, name, description, cover?, examTime?, passScore? }` |
| PUT    | `/api/admin/bank/{id}`         | 编辑题库      | 同上                                                  |
| DELETE | `/api/admin/bank/{id}`         | 删除题库      | -                                                     |

### 7.5 题目管理

| 方法   | 路径                               | 说明         | 请求参数                                              |
| ------ | ---------------------------------- | ------------ | ----------------------------------------------------- |
| GET    | `/api/admin/question/list`        | 题目列表      | `bankId?, type?, keyword?, pageNum, pageSize`          |
| GET    | `/api/admin/question/{id}`        | 题目详情      | -                                                     |
| POST   | `/api/admin/question`             | 新增题目      | `{ bankId, type, content, options[], answer, analysis }` |
| PUT    | `/api/admin/question/{id}`        | 编辑题目      | 同上                                                  |
| DELETE | `/api/admin/question/{id}`        | 删除题目      | -                                                     |
| DELETE | `/api/admin/question/batch`       | 批量删除      | `{ ids[] }`                                           |
| POST   | `/api/admin/question/import`      | Excel导入     | `multipart/form-data: file + bankId`                  |
| GET    | `/api/admin/question/template`    | 下载导入模板  | -                                                     |

### 7.6 用户管理

| 方法 | 路径                              | 说明         | 请求参数                          |
| ---- | --------------------------------- | ------------ | --------------------------------- |
| GET  | `/api/admin/user/list`           | 用户列表      | `keyword?, status?, pageNum, pageSize` |
| GET  | `/api/admin/user/{id}`           | 用户详情      | -                                 |
| PUT  | `/api/admin/user/{id}/status`    | 启用/禁用     | `{ status }`                      |
| GET  | `/api/admin/user/{id}/records`   | 用户答题记录  | `pageNum, pageSize`                |

### 7.7 记录管理

| 方法 | 路径                                   | 说明           | 请求参数                                    |
| ---- | -------------------------------------- | -------------- | ------------------------------------------- |
| GET  | `/api/admin/record/practice/list`     | 练习记录列表    | `userId?, bankId?, pageNum, pageSize`        |
| GET  | `/api/admin/record/practice/{id}`     | 练习记录详情    | -                                           |
| GET  | `/api/admin/record/exam/list`         | 考试记录列表    | `userId?, bankId?, pageNum, pageSize`        |
| GET  | `/api/admin/record/exam/{id}`         | 考试记录详情    | -                                           |

### 7.8 数据统计

| 方法 | 路径                                | 说明             | 请求参数                |
| ---- | ----------------------------------- | ---------------- | ----------------------- |
| GET  | `/api/admin/stat/user-growth`      | 用户增长趋势      | `startDate, endDate`    |
| GET  | `/api/admin/stat/answer-trend`     | 答题趋势          | `startDate, endDate`    |
| GET  | `/api/admin/stat/bank-hot`         | 题库热度排行      | `limit`                 |
| GET  | `/api/admin/stat/wrong-rank`       | 高频错题排行      | `bankId?, limit`        |
| GET  | `/api/admin/stat/accuracy-dist`    | 正确率分布        | `bankId?`               |
| GET  | `/api/admin/stat/favorite-rank`    | 热门收藏排行      | `limit`                 |

### 7.9 系统管理

| 方法   | 路径                              | 说明           | 请求参数                        |
| ------ | --------------------------------- | -------------- | ------------------------------- |
| GET    | `/api/admin/system/admin/list`   | 管理员列表      | `pageNum, pageSize`             |
| POST   | `/api/admin/system/admin`        | 新增管理员      | `{ username, password, role }`  |
| PUT    | `/api/admin/system/admin/{id}`   | 编辑管理员      | `{ username, role }`            |
| DELETE | `/api/admin/system/admin/{id}`   | 删除管理员      | -                               |

### 7.10 VIP管理

| 方法   | 路径                              | 说明           | 请求参数                                      |
| ------ | --------------------------------- | -------------- | --------------------------------------------- |
| GET    | `/api/admin/vip/plan/list`       | 套餐列表        | -                                             |
| POST   | `/api/admin/vip/plan`            | 新增套餐        | `{ name, duration, price, description, sort }` |
| PUT    | `/api/admin/vip/plan/{id}`       | 编辑套餐        | 同上                                          |
| DELETE | `/api/admin/vip/plan/{id}`       | 删除套餐        | -                                             |
| PUT    | `/api/admin/vip/plan/{id}/status`| 启用/禁用       | `{ status }`                                  |
| GET    | `/api/admin/vip/order/list`      | 订单列表        | `userId?, status?, pageNum, pageSize`          |
| GET    | `/api/admin/vip/stats`           | VIP统计概览     | -                                              |

### 7.11 AI解析配置

| 方法   | 路径                                  | 说明               | 请求参数                                         |
| ------ | ------------------------------------- | ------------------ | ------------------------------------------------ |
| GET    | `/api/admin/ai/config`               | 获取当前AI配置      | -                                                |
| PUT    | `/api/admin/ai/config`               | 更新AI配置          | `{ baseUrl, apiKey, model, promptTemplate }`     |
| POST   | `/api/admin/ai/test`                 | 测试AI连通性        | -                                                |
| POST   | `/api/admin/ai/generate`             | 单题生成解析        | `{ questionId, mode }` (见下方mode说明)           |
| POST   | `/api/admin/ai/batch-generate`       | 批量生成解析        | `{ bankId?, questionIds[]?, mode }`               |
| GET    | `/api/admin/ai/log/list`             | AI调用日志列表      | `pageNum, pageSize`                               |
| GET    | `/api/admin/ai/stats`                | AI调用量统计        | -                                                 |

**AI生成 mode 说明：**

| mode                     | 输入                 | AI输出         | 场景                       |
| ------------------------ | -------------------- | -------------- | -------------------------- |
| `GENERATE_ANALYSIS`      | 题目+选项+答案       | 生成解析        | 有答案缺解析               |
| `GENERATE_ANSWER`        | 题目+选项+解析       | 推导答案        | 有解析缺答案               |
| `GENERATE_BOTH`          | 题目+选项            | 答案+解析       | 答案和解析都缺             |

---

## 八、MySQL 数据库表设计

### 8.1 用户表 `user`

```sql
CREATE TABLE `user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `openid` VARCHAR(64) DEFAULT NULL COMMENT '微信openid',
  `union_id` VARCHAR(64) DEFAULT NULL COMMENT '微信unionId',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `password` VARCHAR(255) DEFAULT NULL COMMENT '密码(手机号登录用)',
  `nickname` VARCHAR(50) DEFAULT '' COMMENT '昵称',
  `avatar` VARCHAR(500) DEFAULT '' COMMENT '头像URL',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-正常',
  `settings` JSON DEFAULT NULL COMMENT '用户设置(JSON)',
  `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除: 0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_openid` (`openid`),
  UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
```

### 8.2 管理员表 `admin`

```sql
CREATE TABLE `admin` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '管理员ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `password` VARCHAR(255) NOT NULL COMMENT '密码',
  `nickname` VARCHAR(50) DEFAULT '' COMMENT '昵称',
  `avatar` VARCHAR(500) DEFAULT '' COMMENT '头像',
  `role` VARCHAR(20) NOT NULL DEFAULT 'admin' COMMENT '角色: super_admin/admin',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-正常',
  `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员表';
```

### 8.3 题库分类表 `category`

```sql
CREATE TABLE `category` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
  `icon` VARCHAR(500) DEFAULT '' COMMENT '图标URL',
  `sort` INT NOT NULL DEFAULT 0 COMMENT '排序(越小越前)',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-正常',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题库分类表';
```

### 8.4 题库表 `question_bank`

```sql
CREATE TABLE `question_bank` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '题库ID',
  `category_id` BIGINT NOT NULL COMMENT '分类ID',
  `name` VARCHAR(100) NOT NULL COMMENT '题库名称',
  `description` VARCHAR(500) DEFAULT '' COMMENT '题库描述',
  `cover` VARCHAR(500) DEFAULT '' COMMENT '封面图URL',
  `question_count` INT NOT NULL DEFAULT 0 COMMENT '题目总数(冗余)',
  `exam_time` INT DEFAULT 60 COMMENT '考试时长(分钟)',
  `exam_question_count` INT DEFAULT 50 COMMENT '考试抽题数量',
  `pass_score` INT DEFAULT 60 COMMENT '及格分数',
  `sort` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-正常',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_category_id` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题库表';
```

### 8.5 题目表 `question`

```sql
CREATE TABLE `question` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '题目ID',
  `bank_id` BIGINT NOT NULL COMMENT '所属题库ID',
  `type` TINYINT NOT NULL COMMENT '题型: 1-单选 2-多选 3-判断 4-填空',
  `content` TEXT NOT NULL COMMENT '题目内容',
  `answer` VARCHAR(500) NOT NULL COMMENT '正确答案(多选用逗号分隔)',
  `analysis` TEXT DEFAULT NULL COMMENT '答案解析',
  `difficulty` TINYINT NOT NULL DEFAULT 1 COMMENT '难度: 1-简单 2-中等 3-困难',
  `sort` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-正常',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_bank_id` (`bank_id`),
  KEY `idx_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目表';
```

### 8.6 题目选项表 `question_option`

```sql
CREATE TABLE `question_option` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '选项ID',
  `question_id` BIGINT NOT NULL COMMENT '题目ID',
  `label` VARCHAR(10) NOT NULL COMMENT '选项标签: A/B/C/D',
  `content` VARCHAR(1000) NOT NULL COMMENT '选项内容',
  `sort` INT NOT NULL DEFAULT 0 COMMENT '排序',
  PRIMARY KEY (`id`),
  KEY `idx_question_id` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题目选项表';
```

### 8.7 练习记录表 `practice_record`

```sql
CREATE TABLE `practice_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `bank_id` BIGINT NOT NULL COMMENT '题库ID',
  `mode` VARCHAR(20) NOT NULL COMMENT '练习模式: ORDER/RANDOM/WRONG',
  `total_count` INT NOT NULL DEFAULT 0 COMMENT '总题数',
  `answer_count` INT NOT NULL DEFAULT 0 COMMENT '已答题数',
  `correct_count` INT NOT NULL DEFAULT 0 COMMENT '正确数',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0-进行中 1-已完成',
  `last_index` INT NOT NULL DEFAULT 0 COMMENT '上次答到的题号索引',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_bank` (`user_id`, `bank_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='练习记录表';
```

### 8.8 练习答题明细表 `practice_detail`

```sql
CREATE TABLE `practice_detail` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `record_id` BIGINT NOT NULL COMMENT '练习记录ID',
  `question_id` BIGINT NOT NULL COMMENT '题目ID',
  `user_answer` VARCHAR(500) DEFAULT NULL COMMENT '用户答案',
  `is_correct` TINYINT DEFAULT NULL COMMENT '是否正确: 0-错 1-对',
  `answer_time` INT DEFAULT NULL COMMENT '答题用时(秒)',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_record_id` (`record_id`),
  UNIQUE KEY `uk_record_question` (`record_id`, `question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='练习答题明细表';
```

### 8.9 考试记录表 `exam_record`

```sql
CREATE TABLE `exam_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `bank_id` BIGINT NOT NULL COMMENT '题库ID',
  `total_count` INT NOT NULL COMMENT '总题数',
  `correct_count` INT NOT NULL DEFAULT 0 COMMENT '正确数',
  `score` INT NOT NULL DEFAULT 0 COMMENT '得分',
  `duration` INT DEFAULT NULL COMMENT '用时(秒)',
  `pass_score` INT NOT NULL COMMENT '及格分',
  `is_pass` TINYINT NOT NULL DEFAULT 0 COMMENT '是否及格: 0-否 1-是',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0-考试中 1-已交卷',
  `start_time` DATETIME NOT NULL COMMENT '开始时间',
  `end_time` DATETIME DEFAULT NULL COMMENT '交卷时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_bank_id` (`bank_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试记录表';
```

### 8.10 考试答题表 `exam_answer`

```sql
CREATE TABLE `exam_answer` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `exam_id` BIGINT NOT NULL COMMENT '考试记录ID',
  `question_id` BIGINT NOT NULL COMMENT '题目ID',
  `user_answer` VARCHAR(500) DEFAULT NULL COMMENT '用户答案',
  `is_correct` TINYINT DEFAULT NULL COMMENT '是否正确',
  PRIMARY KEY (`id`),
  KEY `idx_exam_id` (`exam_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试答题表';
```

### 8.11 收藏表 `favorite`

```sql
CREATE TABLE `favorite` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `question_id` BIGINT NOT NULL COMMENT '题目ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_question` (`user_id`, `question_id`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='收藏表';
```

### 8.12 错题表 `wrong_question`

```sql
CREATE TABLE `wrong_question` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `question_id` BIGINT NOT NULL COMMENT '题目ID',
  `bank_id` BIGINT NOT NULL COMMENT '题库ID',
  `wrong_count` INT NOT NULL DEFAULT 1 COMMENT '错误次数',
  `last_wrong_answer` VARCHAR(500) DEFAULT NULL COMMENT '最近一次错误答案',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_question` (`user_id`, `question_id`),
  KEY `idx_user_bank` (`user_id`, `bank_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='错题表';
```

### 8.13 VIP套餐表 `vip_plan`

```sql
CREATE TABLE `vip_plan` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '套餐ID',
  `name` VARCHAR(50) NOT NULL COMMENT '套餐名称(如: 月度VIP/季度VIP/年度VIP)',
  `duration` INT NOT NULL COMMENT '时长(天)',
  `price` DECIMAL(10,2) NOT NULL COMMENT '价格(元)',
  `original_price` DECIMAL(10,2) DEFAULT NULL COMMENT '原价(用于划线价展示)',
  `description` VARCHAR(500) DEFAULT '' COMMENT '套餐描述/权益说明',
  `sort` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-下架 1-上架',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='VIP套餐表';
```

### 8.14 VIP订单表 `vip_order`

```sql
CREATE TABLE `vip_order` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '订单ID',
  `order_no` VARCHAR(64) NOT NULL COMMENT '订单编号',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `plan_id` BIGINT NOT NULL COMMENT '套餐ID',
  `plan_name` VARCHAR(50) NOT NULL COMMENT '套餐名称(冗余)',
  `amount` DECIMAL(10,2) NOT NULL COMMENT '实付金额',
  `duration` INT NOT NULL COMMENT '购买时长(天)',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0-待支付 1-已支付 2-已取消',
  `payment_method` VARCHAR(20) DEFAULT NULL COMMENT '支付方式: wxpay/mock',
  `paid_time` DATETIME DEFAULT NULL COMMENT '支付时间',
  `vip_start_time` DATETIME DEFAULT NULL COMMENT 'VIP生效时间',
  `vip_end_time` DATETIME DEFAULT NULL COMMENT 'VIP到期时间',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='VIP订单表';
```

### 8.15 AI配置表 `ai_config`

```sql
CREATE TABLE `ai_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `base_url` VARCHAR(500) NOT NULL DEFAULT 'https://apis.iflow.cn/v1' COMMENT 'API地址',
  `api_key` VARCHAR(500) NOT NULL COMMENT 'API Key',
  `model` VARCHAR(100) NOT NULL DEFAULT 'TBStars2-200B-A13B' COMMENT '模型名称',
  `prompt_analysis` TEXT NOT NULL COMMENT '生成解析的Prompt模板',
  `prompt_answer` TEXT NOT NULL COMMENT '推导答案的Prompt模板',
  `prompt_both` TEXT NOT NULL COMMENT '生成答案+解析的Prompt模板',
  `max_tokens` INT NOT NULL DEFAULT 2000 COMMENT '最大token数',
  `temperature` DECIMAL(2,1) NOT NULL DEFAULT 0.7 COMMENT '温度参数',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI配置表';

-- 初始化默认配置
INSERT INTO `ai_config` (`base_url`, `api_key`, `model`, `prompt_analysis`, `prompt_answer`, `prompt_both`, `max_tokens`, `temperature`) VALUES
('https://apis.iflow.cn/v1', '', 'TBStars2-200B-A13B',
 '你是一个专业的题目解析专家。请根据以下题目信息生成详细的解析说明。\n\n题目：{content}\n选项：{options}\n正确答案：{answer}\n\n请输出清晰易懂的解析，说明为什么正确答案是对的，其他选项为什么不对。',
 '你是一个专业的答题专家。请根据以下题目和解析推导出正确答案。\n\n题目：{content}\n选项：{options}\n解析：{analysis}\n\n请只输出正确答案的选项字母，如有多个用逗号分隔。',
 '你是一个专业的题目解析专家。请根据以下题目信息给出正确答案和详细解析。\n\n题目：{content}\n选项：{options}\n\n请按以下格式输出：\n答案：[选项字母]\n解析：[详细解析内容]',
 2000, 0.7);
```

### 8.16 AI调用日志表 `ai_call_log`

```sql
CREATE TABLE `ai_call_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `question_id` BIGINT DEFAULT NULL COMMENT '关联题目ID',
  `mode` VARCHAR(30) NOT NULL COMMENT '调用模式: GENERATE_ANALYSIS/GENERATE_ANSWER/GENERATE_BOTH',
  `prompt` TEXT NOT NULL COMMENT '实际发送的Prompt',
  `result` TEXT DEFAULT NULL COMMENT 'AI返回结果',
  `tokens_used` INT DEFAULT NULL COMMENT '消耗token数',
  `cost_ms` INT DEFAULT NULL COMMENT '耗时(毫秒)',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-失败 1-成功',
  `error_msg` VARCHAR(1000) DEFAULT NULL COMMENT '错误信息',
  `operator_id` BIGINT DEFAULT NULL COMMENT '操作管理员ID',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_question_id` (`question_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI调用日志表';
```

### 8.17 用户表新增VIP字段

```sql
-- 在 user 表新增VIP相关字段
ALTER TABLE `user`
  ADD COLUMN `is_vip` TINYINT NOT NULL DEFAULT 0 COMMENT 'VIP状态: 0-否 1-是' AFTER `status`,
  ADD COLUMN `vip_expire_time` DATETIME DEFAULT NULL COMMENT 'VIP到期时间' AFTER `is_vip`;
```

### 8.18 ER 关系概览

```
user (1) ──── (N) practice_record (1) ──── (N) practice_detail
  │                   │
  │                   └── bank_id → question_bank
  │
  ├──── (N) exam_record (1) ──── (N) exam_answer
  │             │
  │             └── bank_id → question_bank
  │
  ├──── (N) favorite ──── question_id → question
  │
  ├──── (N) wrong_question ──── question_id → question
  │
  └──── (N) vip_order ──── plan_id → vip_plan
                │
                └── 支付成功 → 更新 user.is_vip + user.vip_expire_time

category (1) ──── (N) question_bank (1) ──── (N) question (1) ──── (N) question_option
                                                    │
                                               ai_call_log (AI生成解析时记录)
```

---

## 九、Redis 缓存设计建议

### 9.1 Key 命名规范

统一前缀: `quiz:`

| Key                                      | 类型   | TTL       | 说明                     |
| ---------------------------------------- | ------ | --------- | ------------------------ |
| `quiz:token:app:{token}`                | String | 7天       | 小程序用户会话(Sa-Token)  |
| `quiz:token:admin:{token}`              | String | 2小时     | 管理端会话(Sa-Token)      |
| `quiz:category:list`                    | String | 1小时     | 分类列表缓存              |
| `quiz:bank:hot`                         | String | 30分钟    | 热门题库缓存              |
| `quiz:bank:detail:{bankId}`             | String | 30分钟    | 题库详情缓存              |
| `quiz:question:bank:{bankId}`           | String | 15分钟    | 题库下题目ID列表           |
| `quiz:daily:question:{date}`            | String | 24小时    | 每日一题缓存              |
| `quiz:user:stats:{userId}`              | Hash   | 10分钟    | 用户学习统计数据           |
| `quiz:practice:progress:{recordId}`     | Hash   | 2小时     | 练习进度(当前题号等)       |
| `quiz:exam:answers:{examId}`            | Hash   | 3小时     | 考试中临时答案(防丢失)     |
| `quiz:search:hot`                       | ZSet   | 1小时     | 热搜词排行                |
| `quiz:dashboard:overview`               | String | 5分钟     | 仪表盘概览数据             |
| `quiz:stat:answer:count:{date}`         | String | 25小时    | 每日答题计数               |
| `quiz:user:vip:{userId}`               | Hash   | 10分钟    | VIP状态缓存(isVip+expireTime) |
| `quiz:vip:plans`                       | String | 1小时     | VIP套餐列表缓存             |
| `quiz:ai:config`                       | String | -         | AI配置缓存(修改时主动清除)   |

### 9.2 缓存策略

| 场景               | 策略                          | 说明                                       |
| ------------------ | ----------------------------- | ------------------------------------------ |
| 分类/题库列表      | Cache-Aside + 管理端操作时删缓存 | 读多写少，管理端新增/编辑/删除时主动清缓存    |
| 题库详情           | Cache-Aside                   | 题库信息+题目数，管理端变更时清除            |
| 每日一题           | 定时任务每天0点写入            | 随机选题后写缓存，当天内所有用户读同一题     |
| 练习进度           | Write-Through                 | 每次答题都更新Redis，定期同步DB             |
| 考试答案暂存       | Write-Through                 | 每答一题存Redis，交卷时一次性写DB，防掉线丢失 |
| 热搜词             | 用 ZINCRBY 累加搜索词分数      | 每次搜索+1，取 TOP 10                      |
| 仪表盘数据         | 短TTL自动过期                  | 5分钟自动刷新，避免频繁聚合查询              |
| VIP状态            | Cache-Aside                   | 登录时/支付成功时写缓存，过期自动失效         |
| AI配置             | 手动管理                      | 管理端修改时主动删缓存，下次读DB再缓存         |

### 9.3 缓存清除触发点

| 管理端操作         | 需清除的缓存 Key                          |
| ------------------ | ----------------------------------------- |
| 新增/编辑/删除分类 | `quiz:category:list`                     |
| 新增/编辑/删除题库 | `quiz:bank:hot`, `quiz:bank:detail:{id}`, `quiz:category:list` |
| 新增/编辑/删除题目 | `quiz:bank:detail:{bankId}`, `quiz:question:bank:{bankId}` |
| 禁用用户           | `quiz:token:app:{该用户token}`            |
| VIP套餐增删改      | `quiz:vip:plans`                         |
| VIP支付成功        | `quiz:user:vip:{userId}`                 |
| AI配置修改         | `quiz:ai:config`                         |

---

## 十、Sa-Token 权限设计

### 10.1 双端登录体系

Sa-Token 通过 **多账号认证** 实现小程序端和管理端分离：

```java
// 两套账号体系，互不干扰
public class StpKit {
    // 小程序端用户认证
    public static final StpLogic APP = new StpLogic("app");
    // 管理后台认证
    public static final StpLogic ADMIN = new StpLogic("admin");
}
```

### 10.2 接口拦截配置

```java
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 小程序端接口：登录校验
        registry.addInterceptor(new SaInterceptor(handle -> {
            SaRouter.match("/api/app/**")
                .notMatch(
                    "/api/app/auth/wx-login",
                    "/api/app/auth/phone-login"
                )
                .check(r -> StpKit.APP.checkLogin());
        })).addPathPatterns("/api/app/**");

        // 管理端接口：登录 + 角色校验
        registry.addInterceptor(new SaInterceptor(handle -> {
            SaRouter.match("/api/admin/**")
                .notMatch("/api/admin/auth/login")
                .check(r -> StpKit.ADMIN.checkLogin());
            // 超级管理员专属接口
            SaRouter.match("/api/admin/system/**")
                .check(r -> StpKit.ADMIN.checkRole("super_admin"));
        })).addPathPatterns("/api/admin/**");
    }
}
```

### 10.3 角色与权限

| 端     | 角色          | 权限范围                                     |
| ------ | ------------- | -------------------------------------------- |
| 小程序 | user          | 所有 `/api/app/**` 接口（登录后）             |
| 小程序 | vip           | 非独立角色，通过 user.is_vip 字段判断，前端控制解析展示 |
| 管理端 | super_admin   | 所有 `/api/admin/**` 接口，含系统管理+AI配置  |
| 管理端 | admin         | 所有 `/api/admin/**` 接口，排除系统管理       |

> **VIP 权限说明**：VIP 不走 Sa-Token 角色体系，因为它是"内容展示权限"而非"接口访问权限"。后端返回题目时始终包含 `analysis` 字段，前端根据 `/api/app/vip/status` 返回的 VIP 状态决定是否展示或遮挡解析区域。这样实现最简单且不影响接口设计。

### 10.4 Token 配置

```yaml
# application.yml
sa-token:
  token-name: Authorization
  timeout: 604800        # 小程序端 token 7天有效
  is-concurrent: true    # 允许同账号多端登录
  is-share: false
  is-read-body: false
  is-read-header: true
  is-log: false

# 管理端单独配置超时（在代码中设置）
# StpKit.ADMIN.login(adminId, SaLoginConfig.create().setTimeout(7200))
```

---

## 十一、小程序前端代码生成建议

### 11.1 项目初始化

```bash
# 使用 HBuilderX 创建 UniApp Vue3 项目，或使用 CLI
npx degit dcloudio/uni-preset-vue#vite-ts quiz-miniapp
cd quiz-miniapp
npm install uview-plus
npm install pinia
npm install @dcloudio/uni-ui
```

### 11.2 项目结构规范

```
quiz-miniapp/
├── src/
│   ├── api/                    # 接口定义（按模块拆分）
│   │   ├── auth.ts
│   │   ├── home.ts
│   │   ├── bank.ts
│   │   ├── practice.ts
│   │   ├── exam.ts
│   │   ├── favorite.ts
│   │   ├── wrong.ts
│   │   ├── user.ts
│   │   └── vip.ts
│   ├── utils/
│   │   ├── request.ts          # uni.request 封装（拦截器、token注入、错误处理）
│   │   └── storage.ts          # 本地存储工具
│   ├── stores/
│   │   ├── user.ts             # 用户信息 + token
│   │   └── app.ts              # 全局状态
│   ├── components/             # 公共组件
│   │   ├── QuestionCard.vue    # 题目卡片（核心组件）
│   │   ├── OptionItem.vue      # 选项组件
│   │   ├── AnswerSheet.vue     # 答题卡组件
│   │   ├── AnalysisBlock.vue   # 解析展示组件（VIP可见/非VIP遮挡）
│   │   ├── VipGuide.vue        # VIP引导开通弹窗
│   │   ├── ExamTimer.vue       # 考试倒计时
│   │   ├── EmptyState.vue      # 空状态
│   │   └── LoadMore.vue        # 加载更多
│   ├── pages/                  # 页面（同第三节结构）
│   ├── static/                 # 静态资源
│   ├── pages.json              # 页面配置
│   ├── manifest.json           # 应用配置
│   ├── App.vue
│   └── main.ts
```

### 11.3 核心代码片段

**请求封装 `request.ts`：**
```typescript
const BASE_URL = import.meta.env.VITE_API_BASE_URL

export const request = <T = any>(options: UniApp.RequestOptions): Promise<T> => {
  return new Promise((resolve, reject) => {
    const userStore = useUserStore()
    uni.request({
      ...options,
      url: BASE_URL + options.url,
      header: {
        ...options.header,
        Authorization: userStore.token || ''
      },
      success: (res) => {
        if (res.statusCode === 200) {
          const data = res.data as { code: number; data: T; msg: string }
          if (data.code === 200) {
            resolve(data.data)
          } else if (data.code === 401) {
            userStore.logout()
            uni.reLaunch({ url: '/pages/login/index' })
            reject(new Error('登录已过期'))
          } else {
            uni.showToast({ title: data.msg, icon: 'none' })
            reject(new Error(data.msg))
          }
        }
      },
      fail: (err) => {
        uni.showToast({ title: '网络异常', icon: 'none' })
        reject(err)
      }
    })
  })
}
```

**题目卡片组件要点：**
- 支持单选/多选/判断/填空四种题型渲染
- 选项点击后立即判断正误（练习模式）或仅记录（考试模式）
- 支持左右滑动切换题目
- 收藏按钮集成
- 解析区域集成 AnalysisBlock 组件（根据 VIP 状态展示/遮挡）

### 11.4 关键页面开发要点

| 页面       | 开发要点                                                     |
| ---------- | ------------------------------------------------------------ |
| 练习答题页 | 最复杂的页面。需处理：题目渲染、答案提交、进度条、上下题切换、答题卡弹窗、收藏交互、解析展示、断点续答（从Redis恢复进度） |
| 模拟考试页 | 与练习页类似但增加：倒计时组件、答案暂存(每答一题调接口存Redis)、交卷确认弹窗、时间到自动交卷 |
| 首页       | 聚合接口一次返回所有数据，减少请求数。注意下拉刷新               |
| 错题本     | 按题库分组展示，支持筛选。点击"错题练习"复用练习答题页(mode=WRONG) |
| VIP会员中心 | 套餐卡片横向滑动，当前VIP状态置顶，开通按钮调用支付接口(MVP阶段可模拟支付) |
| 解析遮挡   | AnalysisBlock 组件：VIP用户正常渲染markdown解析内容；非VIP用户用CSS `filter:blur(5px)` 模糊文字 + 绝对定位蒙层 + "开通VIP查看解析"按钮跳转VIP页 |

---

## 十二、后端代码生成建议

### 12.1 项目初始化

**pom.xml 核心依赖：**
```xml
<parent>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-parent</artifactId>
    <version>3.2.5</version>
</parent>

<dependencies>
    <!-- Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- MyBatis-Flex -->
    <dependency>
        <groupId>com.mybatis-flex</groupId>
        <artifactId>mybatis-flex-spring-boot3-starter</artifactId>
        <version>1.8.9</version>
    </dependency>
    <dependency>
        <groupId>com.mybatis-flex</groupId>
        <artifactId>mybatis-flex-processor</artifactId>
        <version>1.8.9</version>
        <scope>provided</scope>
    </dependency>

    <!-- Sa-Token -->
    <dependency>
        <groupId>cn.dev33</groupId>
        <artifactId>sa-token-spring-boot3-starter</artifactId>
        <version>1.38.0</version>
    </dependency>
    <dependency>
        <groupId>cn.dev33</groupId>
        <artifactId>sa-token-redis-jackson</artifactId>
        <version>1.38.0</version>
    </dependency>

    <!-- Redis -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-redis</artifactId>
    </dependency>
    <dependency>
        <groupId>org.apache.commons</groupId>
        <artifactId>commons-pool2</artifactId>
    </dependency>

    <!-- MySQL -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
    </dependency>

    <!-- Knife4j -->
    <dependency>
        <groupId>com.github.xiaoymin</groupId>
        <artifactId>knife4j-openapi3-jakarta-spring-boot-starter</artifactId>
        <version>4.5.0</version>
    </dependency>

    <!-- Hutool -->
    <dependency>
        <groupId>cn.hutool</groupId>
        <artifactId>hutool-all</artifactId>
        <version>5.8.27</version>
    </dependency>

    <!-- Lombok -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- EasyExcel (题目导入) -->
    <dependency>
        <groupId>com.alibaba</groupId>
        <artifactId>easyexcel</artifactId>
        <version>3.3.4</version>
    </dependency>

    <!-- OkHttp (调用 iFlow AI API) -->
    <dependency>
        <groupId>com.squareup.okhttp3</groupId>
        <artifactId>okhttp</artifactId>
    </dependency>
</dependencies>
```

### 12.2 核心编码规范

**统一响应体：**
```java
@Data
public class R<T> {
    private int code;
    private String msg;
    private T data;

    public static <T> R<T> ok(T data) {
        R<T> r = new R<>();
        r.setCode(200);
        r.setMsg("success");
        r.setData(data);
        return r;
    }

    public static <T> R<T> fail(String msg) {
        R<T> r = new R<>();
        r.setCode(500);
        r.setMsg(msg);
        return r;
    }

    public static <T> R<T> fail(int code, String msg) {
        R<T> r = new R<>();
        r.setCode(code);
        r.setMsg(msg);
        return r;
    }
}
```

**MyBatis-Flex 实体示例：**
```java
@Data
@Table("question")
public class Question {
    @Id(keyType = KeyType.Auto)
    private Long id;
    private Long bankId;
    private Integer type;       // 1-单选 2-多选 3-判断 4-填空
    private String content;
    private String answer;
    private String analysis;
    private Integer difficulty;
    private Integer sort;
    private Integer status;
    @Column(onInsertValue = "now()")
    private LocalDateTime createTime;
    @Column(onInsertValue = "now()", onUpdateValue = "now()")
    private LocalDateTime updateTime;
    @Column(isLogicDelete = true)
    private Integer deleted;
}
```

**Controller 示例：**
```java
@RestController
@RequestMapping("/api/app/bank")
@Tag(name = "小程序-题库接口")
public class AppBankController {

    @Autowired
    private QuestionBankService bankService;

    @GetMapping("/list")
    @Operation(summary = "题库列表")
    public R<PageResult<QuestionBank>> list(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        return R.ok(bankService.pageList(categoryId, pageNum, pageSize));
    }

    @GetMapping("/{id}")
    @Operation(summary = "题库详情")
    public R<BankDetailVO> detail(@PathVariable Long id) {
        Long userId = StpKit.APP.getLoginIdAsLong();
        return R.ok(bankService.getDetail(id, userId));
    }
}
```

### 12.3 开发要点

| 模块     | 要点                                                         |
| -------- | ------------------------------------------------------------ |
| 微信登录 | 小程序传 `code`→后端调微信 `jscode2session` 获取 `openid`→查库→创建/返回用户→Sa-Token登录 |
| 练习模块 | 开始练习时根据 mode 生成题目顺序(顺序/随机/错题)，存入 Redis；每次答题更新 Redis 进度；结束时汇总写库 |
| 考试模块 | 开始时从题库随机抽 N 题，返回题目(不含答案)；每答一题存 Redis；交卷时对比答案计分，一次性写入 exam_record + exam_answer |
| 错题收录 | 练习/考试中答错自动写入 wrong_question 表，已存在则 wrong_count+1 |
| 题目导入 | 用 EasyExcel 读取 Excel，逐行校验后批量插入 question + question_option |
| 题目数冗余 | question_bank.question_count 字段，在题目增删时同步更新（或用定时任务校正） |
| AI解析服务 | AiAnalysisService 核心逻辑：从 DB/Redis 读 ai_config → 拼装 Prompt（替换 {content}/{options}/{answer}/{analysis} 占位符）→ OkHttp 调 iFlow API（OpenAI兼容格式）→ 解析返回结果 → 写入 question.analysis/answer 字段 → 记录 ai_call_log |
| VIP判断   | 查 user.is_vip=1 且 vip_expire_time > NOW()；支付成功时更新这两个字段（续费场景取 MAX(当前到期时间, NOW()) + 购买天数） |

---

## 十三、管理后台代码生成建议

### 13.1 项目初始化

```bash
npm create vite@latest quiz-admin -- --template vue-ts
cd quiz-admin
npm install naive-ui @vicons/ionicons5
npm install axios
npm install vue-router@4 pinia
npm install echarts
npm install @vueuse/core
npm install dayjs
```

### 13.2 Axios 封装

```typescript
// src/utils/request.ts
import axios from 'axios'
import { useAuthStore } from '@/stores/auth'
import { useMessage } from 'naive-ui'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 15000
})

request.interceptors.request.use((config) => {
  const authStore = useAuthStore()
  if (authStore.token) {
    config.headers.Authorization = authStore.token
  }
  return config
})

request.interceptors.response.use(
  (response) => {
    const { code, data, msg } = response.data
    if (code === 200) return data
    if (code === 401) {
      const authStore = useAuthStore()
      authStore.logout()
      window.location.href = '/login'
    }
    return Promise.reject(new Error(msg))
  },
  (error) => Promise.reject(error)
)

export default request
```

### 13.3 通用表格 Composable

```typescript
// src/composables/useTable.ts
import { ref, reactive } from 'vue'

export function useTable<T>(fetchFn: (params: any) => Promise<{ list: T[]; total: number }>) {
  const loading = ref(false)
  const data = ref<T[]>([])
  const pagination = reactive({
    page: 1,
    pageSize: 10,
    itemCount: 0,
    showSizePicker: true,
    pageSizes: [10, 20, 50],
    onChange: (page: number) => { pagination.page = page; fetchData() },
    onUpdatePageSize: (size: number) => { pagination.pageSize = size; pagination.page = 1; fetchData() }
  })

  const fetchData = async (extra?: Record<string, any>) => {
    loading.value = true
    try {
      const res = await fetchFn({ pageNum: pagination.page, pageSize: pagination.pageSize, ...extra })
      data.value = res.list
      pagination.itemCount = res.total
    } finally {
      loading.value = false
    }
  }

  return { loading, data, pagination, fetchData }
}
```

### 13.4 页面开发要点

| 页面         | 核心组件/要点                                                |
| ------------ | ------------------------------------------------------------ |
| 登录页       | NForm 表单验证 + 登录API + token 持久化                      |
| 仪表盘       | NGrid 布局统计卡片 + ECharts 折线图(趋势) + 柱状图(排行)     |
| 分类管理     | NDataTable + NModal(表单弹窗) + 拖拽排序                     |
| 题库管理     | NDataTable + 搜索筛选表单 + 封面图上传                        |
| 题目管理     | 最复杂页面。NDataTable列表 + 筛选(题库/题型) + NDrawer抽屉编辑(动态选项表单) + Excel导入弹窗 |
| 用户管理     | NDataTable + 状态切换(NSwitch) + 查看详情抽屉                 |
| 数据统计     | NDatePicker日期范围 + 多个 ECharts 图表                       |
| 题目编辑表单 | 动态表单：根据题型切换选项输入区域；多选题支持多个正确答案勾选；富文本/Markdown编辑题目内容 |
| 题目管理-AI  | 列表每行增加"AI生成解析"按钮；题库级别增加"批量生成解析"按钮；点击后选择mode弹窗确认→调接口→成功后刷新列表 |
| AI配置页     | NForm 表单：base_url + api_key(密码输入框) + model(下拉/输入) + prompt模板(NInput textarea) + 参数调节；"测试连通"按钮调 /ai/test 接口 |
| VIP套餐配置  | NDataTable + NModal 表单：套餐名称/时长/价格/原价/描述/排序/状态 |
| VIP订单列表  | NDataTable + 筛选(用户/状态)；展示订单号/用户/套餐/金额/状态/支付时间 |

---

## 十四、前后端联调说明

### 14.1 联调环境配置

**后端跨域配置（开发阶段）：**
```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOriginPatterns("*")
            .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
            .allowedHeaders("*")
            .allowCredentials(true)
            .maxAge(3600);
    }
}
```

**小程序端开发环境：**
```javascript
// manifest.json 中配置开发服务器代理，或直接配置后端地址
// .env.development
VITE_API_BASE_URL = http://localhost:8080
```

**管理后台 Vite 代理：**
```typescript
// vite.config.ts
export default defineConfig({
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
```

### 14.2 联调流程

```
阶段1: 接口文档对齐
  后端启动 → 访问 Knife4j 文档 (http://localhost:8080/doc.html)
  前端开发者按文档开发，或直接在文档页面测试接口

阶段2: Mock 阶段（可选）
  前端使用 Mock 数据先行开发页面
  后端完成后替换为真实接口

阶段3: 真实联调
  后端本地运行 → 前端连接本地后端 → 逐模块联调
  联调顺序（见 14.3）

阶段4: 集成测试
  部署到测试环境 → Nginx 统一代理 → 端到端测试
```

### 14.3 联调顺序（按模块）

| 序号 | 模块         | 联调方            | 前置条件           | 预计耗时 |
| ---- | ------------ | ----------------- | ------------------ | -------- |
| 1    | 管理端-认证  | 管理后台 ↔ 后端    | 后端认证接口完成    | 0.5天    |
| 2    | 分类管理     | 管理后台 ↔ 后端    | 管理端认证联调完成  | 0.5天    |
| 3    | 题库管理     | 管理后台 ↔ 后端    | 分类数据就绪        | 0.5天    |
| 4    | 题目管理     | 管理后台 ↔ 后端    | 题库数据就绪        | 1天      |
| 5    | 小程序-认证  | 小程序 ↔ 后端      | 后端认证接口完成    | 0.5天    |
| 6    | 首页+题库    | 小程序 ↔ 后端      | 分类/题库/题目数据就绪 | 0.5天  |
| 7    | 练习模块     | 小程序 ↔ 后端      | 题目数据就绪        | 1天      |
| 8    | 考试模块     | 小程序 ↔ 后端      | 题目数据就绪        | 1天      |
| 9    | 收藏+错题    | 小程序 ↔ 后端      | 练习/考试联调完成    | 0.5天   |
| 10   | 用户管理     | 管理后台 ↔ 后端    | 有用户数据          | 0.5天    |
| 11   | 记录+统计    | 双端 ↔ 后端        | 有答题数据          | 1天      |
| 12   | AI解析配置   | 管理后台 ↔ 后端    | AI配置接口完成       | 0.5天   |
| 13   | AI生成解析   | 管理后台 ↔ 后端    | 题目数据+AI配置就绪   | 0.5天   |
| 14   | VIP套餐管理  | 管理后台 ↔ 后端    | VIP接口完成          | 0.5天   |
| 15   | VIP开通+展示 | 小程序 ↔ 后端      | VIP套餐+解析数据就绪  | 1天     |

### 14.4 统一接口规范

**请求头：**
```
Content-Type: application/json
Authorization: <Sa-Token返回的token值>
```

**统一响应格式：**
```json
{
  "code": 200,
  "msg": "success",
  "data": {}
}
```

**分页响应格式：**
```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "list": [],
    "total": 100,
    "pageNum": 1,
    "pageSize": 10
  }
}
```

**错误码约定：**

| code | 含义         | 处理方式                 |
| ---- | ------------ | ------------------------ |
| 200  | 成功         | 正常处理                 |
| 400  | 参数错误     | 前端提示 msg             |
| 401  | 未登录/过期  | 跳转登录页               |
| 403  | 无权限       | 提示无权限               |
| 500  | 服务器错误   | 提示"系统繁忙"           |

---

## 十五、MVP 版本开发优先级

### MVP v1.0 核心功能（建议 3-4 周完成）

| 优先级 | 模块                   | 功能范围                                       |
| ------ | ---------------------- | ---------------------------------------------- |
| P0     | 数据库+基础框架搭建    | 建表、Spring Boot 骨架、Sa-Token 集成、统一响应 |
| P0     | 管理端-认证            | 登录/退出                                       |
| P0     | 管理端-分类管理        | CRUD                                            |
| P0     | 管理端-题库管理        | CRUD                                            |
| P0     | 管理端-题目管理        | CRUD + Excel导入                                |
| P0     | 小程序-认证            | 微信登录                                        |
| P0     | 小程序-首页            | 分类+热门题库+学习数据                           |
| P0     | 小程序-题库浏览        | 分类列表+题库列表+题库详情                       |
| P0     | 小程序-练习模式        | 顺序练习+答题+结果                               |
| P0     | 小程序-模拟考试        | 限时考试+评分+结果                               |

### v1.1 增强功能（第5-6周）

| 优先级 | 模块             | 功能范围                     |
| ------ | ---------------- | ---------------------------- |
| P1     | 小程序-收藏      | 收藏/取消/收藏列表            |
| P1     | 小程序-错题本    | 错题自动收录/列表/错题练习    |
| P1     | 小程序-做题记录  | 记录列表/详情回顾             |
| P1     | 管理端-用户管理  | 列表/启禁用                   |
| P1     | 管理端-记录管理  | 练习记录/考试记录列表查看     |
| P1     | 管理端-仪表盘    | 统计卡片+趋势图表             |
| P1     | 管理端-考试记录  | 考试记录列表+成绩分布         |
| P1     | **管理端-AI配置**   | **AI模型/Key/Prompt配置+测试连通** |
| P1     | **管理端-AI生成解析** | **单题/批量生成解析+调用日志** |
| P1     | **管理端-VIP管理**   | **VIP套餐CRUD+订单列表**     |
| P1     | **小程序-VIP模块**   | **VIP状态展示+开通+解析遮挡/展示** |

### v1.2 完善功能（第7-8周）

| 优先级 | 模块             | 功能范围                     |
| ------ | ---------------- | ---------------------------- |
| P2     | 小程序-搜索      | 关键词搜索+热搜               |
| P2     | 小程序-设置      | 答题设置/个人信息修改          |
| P2     | 管理端-数据统计  | 全部图表页面                   |
| P2     | 管理端-收藏管理  | 热门收藏统计                   |
| P2     | 管理端-错题管理  | 高频错题排行                   |
| P2     | 管理端-系统管理  | 管理员管理                     |
| P2     | 缓存优化         | Redis 全面接入                 |

---

## 十六、完整项目开发顺序建议

```
Week 1: 基础设施
├── Day 1-2: 后端项目搭建
│   ├── Spring Boot 初始化 + 依赖配置
│   ├── MySQL 建表（全部16张表，含VIP和AI表）
│   ├── MyBatis-Flex 代码生成（Entity + Mapper）
│   ├── 统一响应体 + 全局异常处理
│   ├── Sa-Token 配置（双端认证体系）
│   ├── Redis 配置
│   ├── Knife4j 配置
│   └── 跨域配置
│
├── Day 3: 管理后台项目搭建
│   ├── Vite + Vue3 + TS 初始化
│   ├── Naive UI 引入 + 全局样式
│   ├── Router 配置 + 路由守卫
│   ├── Axios 封装 + 请求拦截
│   ├── Pinia 状态管理
│   └── 基础布局组件（侧边栏 + 顶栏 + 内容区）
│
├── Day 4: 小程序项目搭建
│   ├── UniApp Vue3 初始化
│   ├── uView Plus 引入
│   ├── Request 封装
│   ├── Pinia 状态管理
│   ├── pages.json 页面路由配置
│   └── TabBar 配置
│
└── Day 5: 认证模块（三端联调）
    ├── 后端：管理员登录接口 + 微信登录接口
    ├── 管理后台：登录页面 + Token 持久化 + 路由守卫
    └── 小程序：登录页面 + Token 存储

Week 2: 管理端核心（录入数据）
├── Day 1: 后端-分类+题库 CRUD 接口
├── Day 2: 管理后台-分类管理页面 + 联调
├── Day 3: 管理后台-题库管理页面 + 联调
├── Day 4: 后端-题目 CRUD + Excel 导入接口
└── Day 5: 管理后台-题目管理页面（含导入）+ 联调

Week 3: 小程序核心（答题功能）
├── Day 1: 后端-首页聚合接口 + 题库列表/详情接口
├── Day 2: 小程序-首页 + 题库分类页 + 题库详情页 + 联调
├── Day 3: 后端-练习模块全部接口（开始/答题/结束）
├── Day 4: 小程序-练习答题页面（题目渲染/选项交互/翻页/答题卡）+ 联调
└── Day 5: 小程序-练习结果页 + 断点续答 + 联调

Week 4: 考试 + 收藏 + 错题
├── Day 1: 后端-考试模块全部接口
├── Day 2: 小程序-考试答题页（倒计时/暂存/交卷）+ 考试结果页 + 联调
├── Day 3: 后端-收藏+错题接口 | 小程序-收藏页面
├── Day 4: 小程序-错题本页面 + 错题练习 + 联调
└── Day 5: 后端-做题记录接口 | 小程序-记录列表页+详情页 + 联调

Week 5: 管理端完善 + 统计 + AI + VIP
├── Day 1: 管理后台-仪表盘页面（统计卡片+图表）
├── Day 2: 后端-仪表盘+统计接口 | 管理后台联调
├── Day 3: 管理后台-用户管理 + 记录管理页面 + 联调
├── Day 4: 后端-AI配置/生成解析接口 + AiAnalysisService（iFlow调用）
│   管理后台-AI配置页 + 批量生成解析 + 联调
└── Day 5: 后端-VIP套餐/订单接口 | 管理后台-VIP管理页面 + 联调

Week 6: 小程序VIP + 搜索 + 个人中心
├── Day 1: 后端-小程序VIP状态/套餐/订单接口
├── Day 2: 小程序-VIP会员中心页 + 开通流程(模拟支付) + 联调
├── Day 3: 小程序-AnalysisBlock解析遮挡组件 + VipGuide引导组件
│   集成到练习答题页/考试结果页/错题详情页
├── Day 4: 小程序-搜索页 + 个人中心(含VIP标识) + 设置页
└── Day 5: 管理后台-数据统计页面（多图表）+ 联调

Week 7: 优化 + 测试 + 部署
├── Day 1: Redis 缓存全面接入 + VIP状态缓存 + 性能优化
├── Day 2: 全量功能测试 + Bug 修复
├── Day 3: Bug 修复 + 边界情况处理
├── Day 4: 部署配置（Nginx + 后端打包 + 小程序提审）
└── Day 5: 上线验证 + 监控
```

**关键里程碑：**

| 里程碑   | 时间节点     | 交付物                                   |
| -------- | ------------ | ---------------------------------------- |
| M1       | Week 1 结束  | 三端骨架跑通 + 登录流程走通               |
| M2       | Week 2 结束  | 管理端可录入分类/题库/题目数据             |
| M3       | Week 3 结束  | 小程序可浏览题库 + 完成练习答题            |
| M4       | Week 4 结束  | MVP 功能完整（练习+考试+收藏+错题+记录）  |
| M5       | Week 5 结束  | 管理端完整 + AI生成解析可用 + VIP后台配置  |
| M6       | Week 6 结束  | 小程序VIP开通+解析遮挡展示 + 搜索+个人中心 |
| M7       | Week 7 结束  | 优化完成 + 上线                           |
