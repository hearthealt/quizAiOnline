# QuizAiOnline Code Review And Optimization

## 项目概览

当前仓库是一个三端项目：

- `quiz-server`：Spring Boot 后端，负责小程序端和管理端接口
- `quiz-admin`：Vue 3 管理后台
- `quiz-miniapp`：uni-app 小程序端

主业务链路已经比较完整，覆盖：

- 题库分类与题库浏览
- 顺序/随机/错题练习
- 模拟考试
- 收藏、错题、答题记录
- AI 辅导与题目解析生成
- VIP 与后台运营配置

## 代码审查结论

### P0 级问题

1. 小程序练习记录存在越权访问风险
   - 练习接口在通过 `recordId` 取题、提交、结束、查询进度时，原先没有统一校验记录归属。
   - 风险：已登录用户理论上可以通过猜测 `recordId` 访问别人的练习记录。

2. 小程序记录详情存在越权访问风险
   - 记录详情接口原先只按 `id` 和 `type` 查记录，没有校验记录是否属于当前用户。
   - 风险：用户可能查看他人的练习/考试详情。

3. 分类下题库接口有实际功能 bug
   - `/api/app/category/{id}/banks` 传入的分类 ID 没有真正参与查询，返回结果和“全部题库”一致。
   - 风险：分类页展示数据错误，影响用户使用。

### P1 级问题

1. 记录列表存在性能隐患
   - `appRecordList` 先把用户全部练习和考试记录拉到内存，再排序分页。
   - 数据量大后会变慢，也会增加不必要的数据库与内存压力。

2. 记录详情存在典型 N+1 查询
   - 详情页逐题查询题目与选项。
   - 数据量大后接口耗时会明显上升。

3. 搜索与题库题目列表接口直接返回实体
   - 一些面向小程序的接口返回 `Question` 实体，天然带 `answer`、`analysis` 字段。
   - 如果前端不做严格控制，存在答案泄露风险。

4. 缓存清理方式较粗
   - 题库缓存清理用了 Redis `keys` 前缀删除。
   - 在 key 数量大时会阻塞 Redis。

### P2 级问题

1. `QuestionBankService.pageList` 接口设计含糊
   - 方法签名里存在无语义的第二个参数，导致调用方容易传错。

2. 部分服务仍有较多业务逻辑直接堆在实现类里
   - 例如练习、记录、AI 分析服务都偏长，后续可拆为更小的领域服务或组装层。

3. 测试覆盖不足
   - 当前仓库没有看到系统化的后端单元测试或集成测试目录。

## 本轮已完成优化

### 1. 修复分类题库过滤 bug

已调整 `QuestionBankService.pageList` 的签名与调用方式，分类页现在会按 `categoryId` 正确查询题库。

涉及文件：

- `quiz-server/src/main/java/com/quiz/service/QuestionBankService.java`
- `quiz-server/src/main/java/com/quiz/service/impl/QuestionBankServiceImpl.java`
- `quiz-server/src/main/java/com/quiz/controller/app/AppCategoryController.java`
- `quiz-server/src/main/java/com/quiz/controller/app/AppBankController.java`
- `quiz-server/src/main/java/com/quiz/controller/admin/AdminBankController.java`

### 2. 修复小程序记录详情越权问题

已让小程序记录详情接口传入当前登录用户 ID，并在服务层校验记录归属。

处理后行为：

- 当前用户查看自己的记录：正常返回
- 当前用户查看他人的记录：返回 403
- 管理后台查看记录详情：不受影响

涉及文件：

- `quiz-server/src/main/java/com/quiz/controller/app/AppRecordController.java`
- `quiz-server/src/main/java/com/quiz/controller/admin/AdminRecordController.java`
- `quiz-server/src/main/java/com/quiz/service/RecordService.java`
- `quiz-server/src/main/java/com/quiz/service/impl/RecordServiceImpl.java`

### 3. 修复练习接口越权问题

已为以下练习接口补齐记录归属校验：

- 获取练习题目
- 提交答案
- 结束练习
- 获取练习进度

处理后行为：

- 当前用户只能访问自己的练习记录
- 非本人访问会返回 403

涉及文件：

- `quiz-server/src/main/java/com/quiz/controller/app/AppPracticeController.java`
- `quiz-server/src/main/java/com/quiz/service/PracticeService.java`
- `quiz-server/src/main/java/com/quiz/service/impl/PracticeServiceImpl.java`

### 4. 修复小程序公开题目接口答案泄露问题

已将以下小程序端接口从直接返回 `Question` 实体改为返回安全版 `QuestionVO`：

- 搜索题目
- 题库题目列表
- 首页每日一题

处理后行为：

- 前端仍能拿到题目基础信息
- 不再直接返回 `answer` 和 `analysis`
- 练习/考试等需要答案校验的闭环仍走专用接口，不受影响

涉及文件：

- `quiz-server/src/main/java/com/quiz/service/QuestionService.java`
- `quiz-server/src/main/java/com/quiz/service/impl/QuestionServiceImpl.java`
- `quiz-server/src/main/java/com/quiz/controller/app/AppSearchController.java`
- `quiz-server/src/main/java/com/quiz/controller/app/AppBankController.java`
- `quiz-server/src/main/java/com/quiz/controller/app/AppHomeController.java`

### 5. 优化小程序记录列表分页

已将小程序端按类型查询记录的主路径改为数据库分页，不再先把全部记录加载到内存后再分页。

处理后行为：

- `practice` 类型记录：数据库分页
- `exam` 类型记录：数据库分页
- `type` 为空的兜底场景：只拉取当前页所需窗口数据后做合并排序，不再全量加载

涉及文件：

- `quiz-server/src/main/java/com/quiz/service/impl/RecordServiceImpl.java`

### 6. 优化记录详情查询，减少 N+1

已将练习/考试记录详情中的题目与选项查询改为批量查询，不再按每道题逐条查询。

处理后收益：

- 详情页数据库查询次数显著下降
- 管理后台和小程序详情接口都同步受益

涉及文件：

- `quiz-server/src/main/java/com/quiz/service/impl/RecordServiceImpl.java`

### 7. 移除高风险 Redis keys 扫描

已将题库缓存清理从 `redisTemplate.keys(prefix*)` 改为精确 key 删除，不再对 Redis 做前缀扫描。

处理后收益：

- 避免在 key 数量较多时阻塞 Redis
- 缓存失效范围更可控

涉及文件：

- `quiz-server/src/main/java/com/quiz/service/impl/QuestionBankServiceImpl.java`

### 8. 为分类列表和热门题库补实际缓存读写

已将之前“只有缓存 key 常量、但基本没有实际命中逻辑”的部分补成真正的缓存读写闭环。

当前已落地：

- 分类列表缓存
- 热门题库缓存（按 limit 维度区分 key）
- 题库详情公共部分缓存（用户进度动态叠加）

处理后收益：

- 首页与分类页的公共数据命中率更高
- 避免每次都重复查库
- 缓存失效仍保持精确删除，不再依赖 `keys`

涉及文件：

- `quiz-server/src/main/java/com/quiz/service/impl/CategoryServiceImpl.java`
- `quiz-server/src/main/java/com/quiz/service/impl/QuestionBankServiceImpl.java`
- `quiz-server/src/main/java/com/quiz/common/constant/RedisKey.java`

### 9. 拆分题目列表型返回结构

已为“搜索结果 / 题库题目列表 / 每日一题”引入更轻量的 `QuestionListVO`，不再继续复用练习详情使用的 `QuestionVO`。

处理后收益：

- 列表型接口语义更清晰
- 前后端类型边界更明确
- 避免后续误把详情字段继续带进列表接口

涉及文件：

- `quiz-server/src/main/java/com/quiz/vo/app/QuestionListVO.java`
- `quiz-server/src/main/java/com/quiz/service/QuestionService.java`
- `quiz-server/src/main/java/com/quiz/service/impl/QuestionServiceImpl.java`
- `quiz-server/src/main/java/com/quiz/controller/app/AppSearchController.java`
- `quiz-server/src/main/java/com/quiz/controller/app/AppBankController.java`
- `quiz-server/src/main/java/com/quiz/controller/app/AppHomeController.java`
- `quiz-miniapp/src/api/home.ts`
- `quiz-miniapp/src/api/search.ts`
- `quiz-miniapp/src/api/bank.ts`

### 10. 抽取公共视图映射逻辑

已将首页分类、热门题库、题目详情基础字段、题目选项这类重复的组装逻辑抽到统一 mapper 中，减少控制器和服务类里的重复代码。

处理后收益：

- `QuestionServiceImpl` 和 `PracticeServiceImpl` 重复映射减少
- 首页控制器更聚焦于业务编排而不是字段搬运
- 后续若需要调整题目展示字段，只需改一处 mapper

涉及文件：

- `quiz-server/src/main/java/com/quiz/util/AppViewMapper.java`
- `quiz-server/src/main/java/com/quiz/service/impl/QuestionServiceImpl.java`
- `quiz-server/src/main/java/com/quiz/service/impl/PracticeServiceImpl.java`
- `quiz-server/src/main/java/com/quiz/controller/app/AppHomeController.java`

### 11. 统一 Favorite/Wrong 的题目视图映射

已将收藏和错题列表中的题目基础字段、题库名、选项映射也并入统一 mapper，减少两份重复实现。

处理后收益：

- 收藏和错题页的视图组装逻辑更一致
- 后续如果调整题目展示字段，不需要在多个 service 中重复修改

涉及文件：

- `quiz-server/src/main/java/com/quiz/util/AppViewMapper.java`
- `quiz-server/src/main/java/com/quiz/service/impl/FavoriteServiceImpl.java`
- `quiz-server/src/main/java/com/quiz/service/impl/WrongQuestionServiceImpl.java`

### 12. 扩展题目视图 mapper 到 Admin/Exam 场景

已继续将以下场景的重复题目视图组装逻辑并入统一 mapper：

- 管理端题目详情
- 考试会话题目
- 考试结果详情
- 考试记录列表中的题库名批量查询

处理后收益：

- `QuestionServiceImpl` 和 `ExamServiceImpl` 冗余字段组装减少
- 考试记录列表去掉题库逐条查询
- 题目内容、选项、结果详情映射更统一

涉及文件：

- `quiz-server/src/main/java/com/quiz/util/AppViewMapper.java`
- `quiz-server/src/main/java/com/quiz/service/impl/QuestionServiceImpl.java`
- `quiz-server/src/main/java/com/quiz/service/impl/ExamServiceImpl.java`

### 13. 优化 UserServiceImpl 学习统计聚合

已将用户学习统计从“先查全部练习记录 ID，再多次扫描明细表”改为数据库聚合查询。

处理后收益：

- `getStudyStats` 查询次数下降
- 避免用户练习记录很多时在 service 层拼装大 ID 集合

涉及文件：

- `quiz-server/src/main/java/com/quiz/mapper/PracticeDetailMapper.java`
- `quiz-server/src/main/java/com/quiz/service/impl/UserServiceImpl.java`

### 14. 优化 VipServiceImpl 统计聚合

已将 VIP 总收入和当月收入统计下推到数据库，不再把全部已支付订单拉到内存求和。

处理后收益：

- VIP 统计接口在订单量增长后仍保持稳定
- 为后台后续展示月收入提供了真实数据来源

涉及文件：

- `quiz-server/src/main/java/com/quiz/mapper/VipOrderMapper.java`
- `quiz-server/src/main/java/com/quiz/service/impl/VipServiceImpl.java`

### 15. 补后端单元测试骨架

已补一组不依赖数据库启动的 service 层 mock 单测，优先覆盖：

- 练习记录权限校验
- 记录详情权限校验
- 记录分页结果
- 用户学习统计聚合
- VIP 统计聚合

涉及文件：

- `quiz-server/src/test/java/com/quiz/service/impl/PracticeServiceImplTest.java`
- `quiz-server/src/test/java/com/quiz/service/impl/RecordServiceImplTest.java`
- `quiz-server/src/test/java/com/quiz/service/impl/UserServiceImplTest.java`
- `quiz-server/src/test/java/com/quiz/service/impl/VipServiceImplTest.java`
- `quiz-server/pom.xml`

## AI 模块近期调整记录

AI 模块近期已经同步做过以下整理：

- 移除 iFlow 专属逻辑
- AI 配置改为通用提供商模式：`provider / baseUrl / apiKey / model`
- Prompt 模板从 `ai_config` 迁移到 `sys_config`
- 管理台支持通过当前 `baseUrl + apiKey` 自动拉取 `/v1/models`

如果部署到已有数据库，需要执行 SQL 迁移脚本：

- `sql/migrations/20260403_migrate_ai_config_remove_iflow.sql`
- `sql/migrations/20260404_remove_ai_prompt_columns.sql`

## 后续建议的优化顺序

### 第一阶段

1. 为题库和搜索结果补更精简的列表 VO，继续压缩无用字段
2. 清理首页/题库/搜索重复的题目映射逻辑
3. 继续评估是否要补题库详情等更细粒度缓存

### 第二阶段

1. 为练习、考试、记录三个核心模块补集成测试
2. 将系统配置、AI 配置、题库缓存策略再细化
3. 清理长服务类，拆分部分查询构建逻辑和聚合逻辑

### 第三阶段

1. 建立统一的接口安全检查清单
2. 为关键接口补访问审计和性能监控
3. 逐步补文档，包括数据库迁移和发布说明

## 当前结论

项目整体并不是“不可维护”，而是已经进入“功能能跑，但边界和工程质量需要补”的阶段。

本轮优先修掉了最容易出线上问题的三类问题：

- 功能错误
- 权限越权
- 接口设计含糊

接下来最值得做的是数据返回边界和记录模块性能优化。
