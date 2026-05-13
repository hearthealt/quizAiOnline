# Quiz AI Online - 在线答题系统

一个基于 Spring Boot + Vue 3 + uni-app 的智能在线答题系统，包含后端 API、Web 管理后台和微信小程序端。系统覆盖题库管理、练习考试、错题收藏、AI 辅导、AI 批量生成、EZTest 直连导出、VIP 审核和运营统计等场景。

## 项目结构

```text
quizAiOnline/
├── quiz-server/          # 后端服务 (Spring Boot)
├── quiz-admin/           # Web 管理后台 (Vue 3 + Naive UI)
├── quiz-miniapp/         # 微信小程序 (uni-app + Vue 3)
├── sql/                  # 数据库初始化脚本
└── uploads/              # 上传和导出文件目录，运行时生成
```

## 技术栈

### 后端 (quiz-server)

- **框架**: Spring Boot 3.2.5
- **ORM**: MyBatis-Flex 1.8.9
- **权限**: Sa-Token 1.38.0，支持管理端和小程序端分端登录
- **数据库**: MySQL 8.0+
- **缓存**: Redis
- **接口文档**: Knife4j (OpenAPI 3)
- **文件处理**: EasyExcel、Apache POI、PDFBox
- **HTTP/AI**: OkHttp、OpenAI 兼容 Chat Completions 接口
- **工具**: Hutool、Lombok

### 管理后台 (quiz-admin)

- **框架**: Vue 3.4 + TypeScript
- **构建**: Vite 5，开发端口默认 `3000`
- **UI**: Naive UI 2.38
- **状态管理**: Pinia
- **图表**: ECharts 5
- **HTTP**: Axios
- **工程化**: unplugin-auto-import、unplugin-vue-components

### 小程序 (quiz-miniapp)

- **框架**: uni-app + Vue 3 + TypeScript
- **UI**: uview-plus、uni-ui
- **状态管理**: Pinia
- **构建目标**: 微信小程序，兼容 uni-app 其他平台脚本

## 功能模块

### 小程序端

- 微信 `code` 登录、手机号登录、退出登录和用户信息获取
- 首页数据、每日一题、热门题库和搜索
- 分类浏览、题库列表、题库详情和题目列表
- 练习模式，支持顺序、随机、错题等练习流程
- 模拟考试，支持开考、保存答案、交卷、结果和考试记录
- 收藏夹、错题本、做题记录和记录详情
- 用户资料编辑、学习统计、设置页、服务协议和隐私政策
- VIP 套餐、提交记录和提交成功页
- AI 学习助手，含历史记录和清空历史，当前后端限制为 VIP 用户可用
- 图片上传和小程序合法域名配置支持

### 管理后台

- 仪表盘概览、趋势统计和排行榜
- 分类管理、题库管理、题目管理和上下架控制
- 题目 Excel 导入、模板下载、题目转换和转换结果导入题库
- 题目转换支持 `xlsx`、`xls`、`csv`、`pdf`、`docx`、`txt`
- EZTest 直连导出，支持按准考证号或手机号获取题库、批量导出 XLSX、含答案 PDF、不含答案 PDF，并可导出后导入题库
- 用户列表、用户详情、状态管理、VIP 调整、用户记录和 AI 对话查看
- 活跃用户分析、今日答题、练习记录、考试记录
- 收藏管理、错题管理和数据统计
- VIP 套餐配置、订单列表、订单审核和驳回
- AI 模型配置、模型列表获取、连通测试、单题生成、批量生成、任务暂停/继续/重试/取消、调用日志和统计
- 系统设置，包含站点名称、Logo、版权、备案号、练习负责人联系方式、微信 AppId/Secret、注册开关、AI 助手人设和欢迎语
- 管理员管理、个人信息、头像上传和密码修改
- 角色权限：超级管理员拥有全部菜单；普通管理员默认仅可访问仪表盘、分类、题库、题目和 EZTest 直连

### AI 能力

- 支持 `OPENAI`、`DEEPSEEK`、`CUSTOM` 三类提供商配置
- Base URL 使用 OpenAI 兼容 `/chat/completions` 和 `/models`
- 支持生成解析、生成答案、同时生成答案和解析
- 支持按题库或题目列表批量生成，默认并发 `5`，最大并发 `10`
- 批量任务会记录成功、失败、跳过、重试和错误信息，服务重启后会尝试恢复未完成任务
- AI 对话会保存用户端消息历史，并写入调用日志

## 环境要求

- JDK 17+
- Maven 3.8+
- Node.js 18+
- npm 9+
- MySQL 8.0+
- Redis 6.0+
- 中文 TrueType 字体，用于后端 PDF 导出
- 微信开发者工具，小程序开发和上传时需要

## 快速开始

### 1. 初始化数据库

```bash
mysql -u root -p < sql/quiz_ai_online.sql
```

默认后台账号：

- 用户名：`admin`
- 密码：`123456`

### 2. 启动后端

后端默认端口是 `9091`，`application.yml` 默认激活 `prod` profile。首次运行请复制生产配置模板并填写数据库、Redis 和上传目录。

```bash
cd quiz-server

# Windows
copy src\main\resources\application-prod.example.yml src\main\resources\application-prod.yml

# macOS / Linux
cp src/main/resources/application-prod.example.yml src/main/resources/application-prod.yml

mvn spring-boot:run
```

访问接口文档：

- Knife4j: http://localhost:9091/doc.html
- OpenAPI: http://localhost:9091/v3/api-docs

如需使用本地开发配置，可以改 `quiz-server/src/main/resources/application.yml` 中的 `spring.profiles.active` 为 `dev`，或启动时指定 profile。

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 3. 启动管理后台

```bash
cd quiz-admin

# Windows
copy .env.development.example .env.development

# macOS / Linux
cp .env.development.example .env.development

npm install
npm run dev
```

访问地址：

- 管理后台: http://localhost:3000
- 开发代理：`/api` 和 `/uploads` 默认转发到 `http://127.0.0.1:9091`

### 4. 启动微信小程序

```bash
cd quiz-miniapp

# Windows
copy .env.development.example .env.development
copy .env.production.example .env.production

# macOS / Linux
cp .env.development.example .env.development
cp .env.production.example .env.production

npm install
npm run dev:mp-weixin
```

使用微信开发者工具打开：

```text
quiz-miniapp/dist/dev/mp-weixin
```

## 常用命令

### 后端

```bash
cd quiz-server
mvn spring-boot:run
mvn test
mvn clean package -DskipTests
```

### 管理后台

```bash
cd quiz-admin
npm run dev
npm run build
npm run preview
```

### 小程序

```bash
cd quiz-miniapp
npm run dev:mp-weixin
npm run build:mp-weixin
npm run type-check
```

## 配置说明

### 后端配置

主要配置文件：

- `quiz-server/src/main/resources/application.yml`
- `quiz-server/src/main/resources/application-dev.yml`
- `quiz-server/src/main/resources/application-prod.example.yml`
- 本地生产配置请复制为 `application-prod.yml`，该文件已加入 `.gitignore`

核心配置示例：

```yaml
server:
  port: 9091

spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/quiz_ai_online?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: quiz_ai_online
    password: change_me
  data:
    redis:
      host: 127.0.0.1
      port: 6379
      database: 1

file:
  upload-path: /www/wwwroot/quizAiOnline/uploads/

wx:
  appid: your_appid
  secret: your_secret

quiz:
  ai:
    batch-concurrency: 5
    batch-max-concurrency: 10
```

微信配置也可以在后台“系统设置”里维护 `wxAppId` 和 `wxAppSecret`，系统配置优先于配置文件默认值。

### 前端环境变量

管理后台：

- 复制 `quiz-admin/.env.development.example` 或 `quiz-admin/.env.production.example`
- 开发环境可将 `VITE_API_BASE_URL` 留空，走 Vite 代理
- 生产环境建议让 Nginx 反向代理 `/api` 和 `/uploads`，`VITE_API_BASE_URL` 也可留空
- 如果不是反向代理模式，填写完整根地址，例如 `https://example.com`

微信小程序：

- 复制 `quiz-miniapp/.env.development.example` 和 `quiz-miniapp/.env.production.example`
- `VITE_API_BASE_URL` 必须填写服务器域名根地址，例如 `https://example.com`
- 不要写成 `https://example.com/api`，代码会自动拼接 `/api/...`
- 真机和正式版不能使用 `127.0.0.1` 或 `localhost`
- 微信公众平台需要把域名加入 `request`、`uploadFile`、`downloadFile` 合法域名

### AI 配置

后台路径：`AI配置 -> 模型配置`

- Provider: `OPENAI`、`DEEPSEEK` 或 `CUSTOM`
- OpenAI 默认 Base URL: `https://api.openai.com/v1`
- DeepSeek 默认 Base URL: `https://api.deepseek.com/v1`
- 自定义服务需要兼容 OpenAI `/chat/completions` 和 `/models`
- gpt-5、o1、o3、o4 系列会自动使用 `max_completion_tokens`，并避免发送不兼容的 `temperature`

### PDF 字体

EZTest/PDF 导出需要服务器安装可用的中文 TrueType 字体，否则可能提示“未找到可用中文字体”或 `OTF fonts do not have a glyf table`。

Ubuntu / Debian 推荐安装文泉驿正黑：

```bash
sudo apt update
sudo apt install -y fonts-wqy-zenhei
sudo fc-cache -fv
```

如果后端运行在 Docker 容器里，需要把字体安装到镜像或容器内，而不是只安装到宿主机。当前后端会优先查找 `/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc`，同时兼容 Windows 的微软雅黑、宋体、黑体和 macOS 的苹方。

## API 接口

### 小程序端 (/api/app/*)

| 模块 | 接口前缀 | 说明 |
|------|----------|------|
| 认证 | `/auth` | 微信登录、手机号登录、退出、当前用户 |
| 首页 | `/home` | 首页数据、每日一题 |
| 配置 | `/config` | 小程序运行配置 |
| 搜索 | `/search` | 关键词搜索、热门搜索 |
| 分类 | `/category` | 分类列表、分类下题库 |
| 题库 | `/bank` | 题库列表、详情、题目、热门题库 |
| 练习 | `/practice` | 开始练习、提交答案、完成练习、进度 |
| 考试 | `/exam` | 开始考试、保存答案、交卷、结果、记录 |
| 记录 | `/record` | 做题记录、记录详情 |
| 收藏 | `/favorite` | 收藏切换、列表、检查、批量删除 |
| 错题 | `/wrong` | 错题列表、统计、删除 |
| 上传 | `/upload` | 图片上传 |
| AI | `/ai` | AI 对话、历史、清空历史 |
| VIP | `/vip` | 套餐、状态、订单、提交支付记录 |
| 用户 | `/user` | 资料、学习统计、设置 |

### 管理后台 (/api/admin/*)

| 模块 | 接口前缀 | 说明 |
|------|----------|------|
| 认证 | `/auth` | 登录、退出、当前管理员、个人资料、修改密码 |
| 仪表盘 | `/dashboard` | 概览、趋势、排行 |
| 分类 | `/category` | 分类分页、全部分类、CRUD、状态、批量操作 |
| 题库 | `/bank` | 题库分页、详情、CRUD、状态、批量操作 |
| 题目 | `/question` | 题目 CRUD、状态、导入、模板、智能转换 |
| EZTest | `/eztest` | 配置、题库列表、导出任务、文件下载、导入题库 |
| 用户 | `/user` | 用户列表、详情、状态、记录、AI 历史、VIP 调整 |
| 活跃分析 | `/activity` | 活跃概览、用户活跃、今日答题 |
| 记录 | `/record` | 练习记录、考试记录和详情 |
| 收藏 | `/favorite` | 收藏列表 |
| 错题 | `/wrong` | 错题列表 |
| 数据统计 | `/stat` | 用户增长、答题趋势、热门题库、错题排行、正确率分布、收藏排行 |
| VIP | `/vip` | 套餐配置、订单列表、审批、驳回、统计 |
| AI | `/ai` | 模型配置、连通测试、生成、批量任务、日志、统计 |
| 上传 | `/upload` | 图片上传 |
| 系统 | `/system` | 管理员管理、系统设置 |

## 数据库

初始化脚本位于 `sql/quiz_ai_online.sql`，包含主要业务表：

- 管理员、用户、系统配置
- 分类、题库、题目、题目选项
- 练习记录、考试记录、考试答案
- 收藏、错题、用户活动日志
- AI 配置、调用日志、对话历史、批量任务
- EZTest 配置、导出任务、任务文件
- VIP 套餐和订单

## 部署提示

- 后端生产建议使用 `application-prod.yml` 管理真实数据库、Redis、上传目录和微信配置
- 上传目录需要后端进程可写，并通过 Nginx 或后端静态资源映射对外提供 `/uploads/**`
- 管理后台生产构建产物在 `quiz-admin/dist/`
- 小程序生产构建产物在 `quiz-miniapp/dist/build/mp-weixin/`
- 生产环境建议使用 HTTPS 域名，并在微信公众平台配置合法域名
- 不要提交真实 API Key、数据库密码、微信密钥或 `application-prod.yml`

## License

MIT
