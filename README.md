# Quiz AI Online - 在线答题系统

一个基于 Spring Boot + Vue 3 + uni-app 的智能在线答题系统，支持 Web 管理后台和微信小程序端。

## 项目结构

```
quizAiOnline/
├── quiz-server/          # 后端服务 (Spring Boot)
├── quiz-admin/           # Web 管理后台 (Vue 3 + Naive UI)
├── quiz-miniapp/         # 微信小程序 (uni-app + Vue 3)
├── sql/                  # 数据库脚本
├── uploads/              # 上传文件目录
└── docs/                 # 文档
```

## 技术栈

### 后端 (quiz-server)
- **框架**: Spring Boot 3.2.5
- **ORM**: MyBatis-Flex 1.8.9
- **权限**: Sa-Token 1.38.0
- **数据库**: MySQL 8.0+
- **缓存**: Redis
- **文档**: Knife4j (OpenAPI 3)
- **工具**: Hutool、Lombok、EasyExcel

### 管理后台 (quiz-admin)
- **框架**: Vue 3.4 + TypeScript
- **构建**: Vite 5
- **UI**: Naive UI 2.38
- **状态**: Pinia
- **图表**: ECharts 5
- **HTTP**: Axios

### 小程序 (quiz-miniapp)
- **框架**: uni-app + Vue 3 + TypeScript
- **UI**: uview-plus
- **状态**: Pinia

## 功能模块

### 小程序端
- 微信一键登录 / 手机号登录
- 题库分类浏览
- 练习模式 (顺序/随机/错题)
- 模拟考试
- 收藏夹 / 错题本
- AI 智能辅导
- VIP 会员

### 管理后台
- 仪表盘统计
- 分类管理
- 题库管理
- 试题管理 (支持 Excel/PDF 批量导入)
- 用户管理
- 答题记录
- VIP 套餐管理
- AI 配置 (多模型支持)
- 系统配置

## 环境要求

- JDK 17+
- Node.js 18+
- MySQL 8.0+
- Redis 6.0+
- 微信开发者工具 (小程序开发)

## 快速开始

### 1. 数据库初始化

```bash
# 创建数据库并导入脚本
mysql -u root -p < sql/quiz_ai_online.sql
```

### 2. 后端启动

```bash
cd quiz-server

# 复制生产配置模板并填写自己的数据库 / Redis / 上传目录
# Windows:
copy src\main\resources\application-prod.example.yml src\main\resources\application-prod.yml
# macOS / Linux:
cp src/main/resources/application-prod.example.yml src/main/resources/application-prod.yml

# 启动服务
./mvnw spring-boot:run
```

访问 API 文档: http://localhost:8080/doc.html

### 3. 管理后台启动

```bash
cd quiz-admin

# 复制环境变量模板
# Windows:
copy .env.development.example .env.development
# macOS / Linux:
cp .env.development.example .env.development

# 安装依赖
npm install

# 开发模式
npm run dev
```

访问: http://localhost:5173

### 4. 小程序启动

```bash
cd quiz-miniapp

# 复制环境变量模板
# Windows:
copy .env.development.example .env.development
copy .env.production.example .env.production
# macOS / Linux:
cp .env.development.example .env.development
cp .env.production.example .env.production

# 安装依赖
npm install

# 编译到微信小程序
npm run dev:mp-weixin
```

使用微信开发者工具打开 `dist/dev/mp-weixin` 目录

## 配置说明

### 后端配置 (application.yml)

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/quiz_ai_online
    username: root
    password: your_password
  data:
    redis:
      host: localhost
      port: 6379

# 微信小程序配置
wx:
  miniapp:
    appid: your_appid
    secret: your_secret

# 文件上传目录
upload:
  path: ./uploads
```

### 前端配置

管理后台:
- 先复制 `quiz-admin/.env.development.example` 或 `quiz-admin/.env.production.example`
- 开发环境可将 `VITE_API_BASE_URL` 留空，走 `quiz-admin/vite.config.ts` 里的 Vite 代理
- 生产环境建议让 Nginx 反向代理 `/api` 和 `/uploads`，`VITE_API_BASE_URL` 也可留空
- 如果不是反向代理模式，再填写完整根地址，例如 `https://example.com`

微信小程序:
- 先复制 `quiz-miniapp/.env.development.example` 和 `quiz-miniapp/.env.production.example`
- 开发环境地址写在 `quiz-miniapp/.env.development`
- 生产环境地址写在 `quiz-miniapp/.env.production`
- `VITE_API_BASE_URL` 必须填写服务器域名根地址，例如 `https://example.com`
- 不要写成 `https://example.com/api`，因为代码里会自动请求 `/api/...`
- 小程序真机和正式版不能使用 `127.0.0.1` 或 `localhost`，那只会指向用户手机/开发机自己
- 微信公众平台里还要把该域名加入 `request`、`uploadFile`、`downloadFile` 合法域名

后端生产配置:
- 仓库内提供 `quiz-server/src/main/resources/application-prod.example.yml`
- 本地或服务器请复制为 `application-prod.yml` 后再填写真实配置
- `application-prod.yml` 已加入忽略规则，不建议提交真实密码

## API 接口

### 小程序端 (/api/app/*)
| 模块 | 接口前缀 | 说明 |
|------|----------|------|
| 认证 | /auth | 登录/注册 |
| 首页 | /home | 首页数据 |
| 分类 | /category | 分类列表 |
| 题库 | /bank | 题库列表/详情 |
| 练习 | /practice | 练习答题 |
| 考试 | /exam | 模拟考试 |
| 收藏 | /favorite | 收藏管理 |
| 错题 | /wrong | 错题本 |
| AI | /ai | AI 辅导 |
| VIP | /vip | 会员服务 |

### 管理后台 (/api/admin/*)
| 模块 | 接口前缀 | 说明 |
|------|----------|------|
| 认证 | /auth | 管理员登录 |
| 仪表盘 | /dash | 统计数据 |
| 分类 | /category | 分类 CRUD |
| 题库 | /bank | 题库 CRUD |
| 试题 | /question | 试题 CRUD/导入 |
| 用户 | /user | 用户管理 |
| VIP | /vip | 套餐管理 |
| AI | /ai | AI 配置/日志 |
| 系统 | /system | 系统配置 |

## 默认账号

管理后台:
- 超级管理员: `admin` / `123456`

## 目录说明

```
quiz-server/
├── src/main/java/com/quiz/
│   ├── common/         # 通用类 (结果封装、异常等)
│   ├── config/         # 配置类
│   ├── controller/     # 控制器
│   │   ├── admin/      # 后台接口
│   │   └── app/        # 小程序接口
│   ├── dto/            # 数据传输对象
│   ├── entity/         # 实体类
│   ├── mapper/         # MyBatis Mapper
│   ├── service/        # 业务逻辑
│   ├── task/           # 定时任务
│   ├── util/           # 工具类
│   └── vo/             # 视图对象
└── src/main/resources/
    └── application.yml # 配置文件
```

## License

MIT
