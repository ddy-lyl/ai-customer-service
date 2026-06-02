# 云屿售后 AI 智能客服系统

面向电商售后场景的 AI 客服项目。系统以 Spring AI RAG 知识库问答为核心，结合订单查询、售后工单、函数调用、智能转人工、客服实时会话和 AI 链路日志，覆盖用户咨询、客服处理、管理员运营调优的完整演示闭环。

> 当前项目适合学习、课程设计、作品集和企业智能客服原型验证。它已经具备完整业务主线，但默认使用本地 JSON 向量库和演示订单数据，生产落地前仍需补齐权限加固、生产配置、分布式向量库、SLA、监控和外部业务系统对接。

| 后端 | 前端 | AI 能力 |
|------|------|---------|
| Spring Boot 3.5、Java 17、MyBatis-Plus、Spring Security | Vue 3、Vite 5、TypeScript、Pinia | Spring AI、DeepSeek Chat、DashScope Embedding、SimpleVectorStore |

---

## 目录

- [1. 项目定位](#1-项目定位)
- [2. 业务闭环](#2-业务闭环)
- [3. 功能说明](#3-功能说明)
- [4. 技术栈](#4-技术栈)
- [5. 项目结构](#5-项目结构)
- [6. 快速启动](#6-快速启动)
- [7. 演示流程](#7-演示流程)
- [8. 后端说明](#8-后端说明)
- [9. 前端说明](#9-前端说明)
- [10. AI 与 RAG 设计](#10-ai-与-rag-设计)
- [11. 核心接口](#11-核心接口)
- [12. 数据库与存储](#12-数据库与存储)
- [13. 配置说明](#13-配置说明)
- [14. 测试与验证](#14-测试与验证)
- [15. 当前边界与生产化建议](#15-当前边界与生产化建议)

---

## 1. 项目定位

本项目以「云屿售后」为业务背景，模拟电商平台的售后客服系统。用户可以向 AI 咨询退款、退货、换货、物流、质检等问题；AI 可以基于企业知识库回答政策类问题，也可以通过工具查询订单、查询工单、创建售后工单；当 AI 无法解决或用户主动要求人工时，系统会进入人工接待流程。

系统包含三类角色：

| 角色 | 代码 | 主要职责 |
|------|------|----------|
| 用户 | `USER` | 咨询售后政策、查询本人订单和工单、创建工单、申请转人工 |
| 客服 | `STAFF` | 接入人工会话、认领和处理工单、使用客服侧 AI 助手 |
| 管理员 | `ADMIN` | 管理知识库、用户、订单、工单，查看 AI 检索和工具调用日志 |

---

## 2. 业务闭环

整体流程：

```text
用户提问
  -> AI 问答
    -> RAG 检索知识库
    -> 同步问答可调用业务工具
      -> 查订单 / 查工单 / 创建工单
    -> 判断是否需要转人工
      -> WAITING_AGENT 等待客服
      -> STAFF 接入后进入 HUMAN 模式
        -> WebSocket 实时聊天
  -> 管理员查看日志与 Trace
  -> 管理员维护知识库并重新向量化
```

关键数据关系：

| 数据表 | 作用 |
|--------|------|
| `chat_session` | 保存 AI 会话、服务模式、转人工状态、接待客服、关联工单 |
| `chat_message` | 保存用户、AI、系统、客服消息 |
| `customer_order` | 演示订单数据，供页面和 AI 工具查询 |
| `ticket` | 售后工单，支持用户创建、AI 创建、转人工创建、客服代建 |
| `ticket_flow` | 工单流转记录 |
| `knowledge_base`、`knowledge_document`、`document_chunk` | 知识库、文档和分块 |
| `ai_retrieval_log` | RAG 检索日志 |
| `ai_tool_call_log` | AI 工具调用日志 |

---

## 3. 功能说明

### 3.1 用户端

| 功能 | 说明 |
|------|------|
| AI 售后问答 | 支持同步问答和 SSE 流式问答 |
| RAG 知识库回答 | 管理员上传知识文档并向量化后，用户提问会检索知识片段 |
| 订单查询 | 页面查询本人订单；AI 同步问答可调用 `QUERY_ORDER` |
| 工单查询 | 页面查看本人工单；AI 可查询指定工单状态 |
| 创建工单 | 页面手动创建，或 AI 通过 `CREATE_TICKET` 创建 |
| 转人工 | 用户主动转人工，或系统根据 RAG 未命中、低分、拒答等策略自动转人工 |
| 人工会话 | 转人工后，用户继续在聊天窗口发送消息，客服通过 WebSocket 接收 |

### 3.2 客服端

| 功能 | 说明 |
|------|------|
| 在线客服 | 查看等待接入会话和本人接待会话 |
| 接入会话 | 通过 `expectedVersion` 配合 `chat_session.version` 做乐观锁控制 |
| 实时聊天 | REST 入库，WebSocket 推送消息和会话状态 |
| 工单池 | 查看待认领工单 |
| 我的工单 | 查看已分配给自己的工单 |
| 工单处理 | 提交处理结果，可标记为已解决 |
| 代客建单 | `/api/staff/tickets` 支持客服为用户创建工单 |
| 客服 AI 助手 | 可列出工单池或我的工单，辅助生成处理建议 |

### 3.3 管理端

| 功能 | 说明 |
|------|------|
| 用户管理 | 分页查看用户、创建客服账号、启停账号、重置密码 |
| 订单管理 | 查看全平台订单 |
| 工单管理 | 查看全平台工单，给客服指派工单 |
| 知识库管理 | 创建知识库、上传文档、解析、分块、向量化 |
| 检索调试 | 管理端调用 RAG 检索接口查看召回结果 |
| AI 链路追踪 | 查看会话、消息、检索日志、工具调用日志和 Trace |
| 管理员 AI 助手 | 可查询平台概览、热点问题、订单和工单上下文 |

---

## 4. 技术栈

### 4.1 后端

| 分类 | 技术 |
|------|------|
| 语言 | Java 17 |
| 框架 | Spring Boot 3.5.14 |
| AI 框架 | Spring AI 1.1.6 |
| 对话模型 | DeepSeek `deepseek-chat` |
| Embedding | DashScope `text-embedding-v3`，通过 OpenAI Compatible API 接入 |
| 向量库 | Spring AI `SimpleVectorStore`，本地 JSON 持久化 |
| ORM | MyBatis-Plus 3.5.16 |
| 数据库 | MySQL 8.x |
| 缓存 | Redis |
| 安全 | Spring Security、JWT、Redis Token 黑名单与版本 |
| WebSocket | Spring WebSocket |
| 文档解析 | Apache Tika 3.3.0 |
| API 文档 | SpringDoc OpenAPI |
| 中文转换 | opencc4j |

### 4.2 前端

| 分类 | 技术 |
|------|------|
| 框架 | Vue 3 |
| 构建 | Vite 5 |
| 语言 | TypeScript 5 |
| 路由 | Vue Router |
| 状态管理 | Pinia |
| HTTP | Axios、Fetch SSE |
| Markdown | marked、DOMPurify |
| 实时通信 | 原生 WebSocket |

---

## 5. 项目结构

```text
ai-customer-service/
├── ai-customer-service-backend/
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/ddy/aicustomerservice/
│       │   │   ├── AiCustomerServiceBackendApplication.java
│       │   │   ├── common/
│       │   │   ├── config/
│       │   │   └── module/
│       │   │       ├── admin/ailog/
│       │   │       ├── ai/tool/
│       │   │       ├── auth/
│       │   │       ├── chat/
│       │   │       ├── document/
│       │   │       ├── knowledge/
│       │   │       ├── order/
│       │   │       ├── retrieval/
│       │   │       ├── role/
│       │   │       ├── ticket/
│       │   │       └── user/
│       │   └── resources/
│       │       ├── application.yml
│       │       ├── application-dev.yml
│       │       ├── application-local.yml.example
│       │       └── db/
│       │           ├── init-ai-customer-service.sql
│       │           ├── 01_schema.sql
│       │           ├── 02_seed_data.sql
│       │           ├── 03_live_chat_migration.sql
│       │           └── 04_chat_session_version.sql
│       └── test/
│           ├── java/
│           └── resources/rag-eval/
├── ai-customer-service-frontend/
│   ├── package.json
│   ├── vite.config.ts
│   └── src/
│       ├── api/
│       ├── components/
│       ├── config/
│       ├── layouts/
│       ├── router/
│       ├── stores/
│       ├── utils/
│       └── views/
├── AI客服系统文档/
├── docs/
└── scripts/
```

运行时目录：

| 路径 | 说明 |
|------|------|
| `ai-customer-service-backend/uploads/knowledge/` | 上传的原始知识文档 |
| `ai-customer-service-backend/uploads/vector-store/knowledge-vector-store.json` | 本地向量库 JSON 文件 |
| `ai-customer-service-backend/target/` | Maven 编译产物 |
| `ai-customer-service-frontend/node_modules/` | 前端依赖 |
| `ai-customer-service-frontend/dist/` | 前端构建产物 |

---

## 6. 快速启动

### 6.1 环境要求

| 依赖 | 说明 |
|------|------|
| JDK | 17 |
| Maven | 3.8+，当前仓库没有 Maven Wrapper |
| Node.js | 18+ |
| MySQL | 8.x；开发配置默认连接 `localhost:8090` |
| Redis | 默认 `localhost:6379` |
| API Key | DeepSeek 对话模型、DashScope Embedding |

### 6.2 初始化数据库

开发演示推荐直接执行一键初始化脚本：

```bash
mysql -u root -p < ai-customer-service-backend/src/main/resources/db/init-ai-customer-service.sql
```

该脚本会：

- 删除并重建数据库 `customer-service-ai`；
- 创建系统用户、角色、订单、工单、对话、知识库、日志等表；
- 写入演示账号和示例订单。

> 注意：`init-ai-customer-service.sql` 包含 `DROP DATABASE IF EXISTS`，只适合本地开发和演示，不要在生产库执行。

如果你想按迁移方式拆分执行，可参考 `db/01_schema.sql`、`02_seed_data.sql`、`03_live_chat_migration.sql`、`04_chat_session_version.sql`。

### 6.3 配置本地密钥

复制模板：

```bash
copy ai-customer-service-backend\src\main\resources\application-local.yml.example ai-customer-service-backend\src\main\resources\application-local.yml
```

填写本机配置：

```yaml
spring:
  datasource:
    password: your-mysql-password
  ai:
    deepseek:
      api-key: your-deepseek-api-key
    openai:
      api-key: your-dashscope-api-key

jwt:
  secret: your-local-jwt-secret-at-least-32-characters-long
```

`application-local.yml` 已在 `.gitignore` 中忽略，不要提交真实密钥。如果真实 API Key 曾经被提交或截图暴露，建议立即更换。

### 6.4 启动后端

```bash
cd ai-customer-service-backend
mvn spring-boot:run
```

默认地址：

| 地址 | 说明 |
|------|------|
| `http://localhost:8080` | 后端 API |
| `http://localhost:8080/swagger-ui.html` | Swagger UI，仅开发环境 |
| `http://localhost:8080/v3/api-docs` | OpenAPI JSON |

### 6.5 启动前端

```bash
cd ai-customer-service-frontend
npm install
npm run dev
```

访问：

```text
http://localhost:5173
```

开发环境下，Vite 会把 `/api` 和 `/ws` 代理到 `http://localhost:8080`。

### 6.6 演示账号

初始化 SQL 后可用，密码均为：

```text
123456
```

| 用户名 | 角色 | 建议入口 |
|--------|------|----------|
| `user01` | USER | `/chat`、`/orders`、`/tickets` |
| `user02` | USER | `/chat`、`/orders`、`/tickets` |
| `staff01` | STAFF | `/staff/live-chat`、`/staff/tickets/pool`、`/staff/tickets/mine` |
| `admin` | ADMIN | `/admin/knowledge`、`/admin/users`、`/admin/orders`、`/admin/tickets`、`/admin/ai-logs` |

---

## 7. 演示流程

### 7.1 准备知识库

1. 使用 `admin` 登录。
2. 进入 `/admin/knowledge`。
3. 创建一个知识库。
4. 上传 `AI客服系统文档/` 目录下的售后政策文本。
5. 对文档执行解析、分块、向量化。
6. 在检索调试或用户聊天中验证召回。

如果没有执行向量化，RAG 检索结果为空，AI 只能依赖模型自身能力或触发转人工。

### 7.2 用户 AI 问答

使用 `user01` 登录，进入 `/chat`，可以尝试：

```text
退款多久到账？
换货质检要多久？
查一下订单 ORD202606010003
我要申请退货
转人工
```

说明：

- 普通政策问题优先走 SSE 流式问答；
- 检测到订单号、工单号、建单、查单、转人工等关键词时，前端会切到同步 `/ask`，以便后端挂载 Tools；
- `/ask/stream` 当前不挂载业务工具，主要用于知识问答和打字机效果；
- 演示查单建议输入连续关键词 **「查订单」** 或订单号（如 `ORD202606010003`）。口语如「查我的订单」若未命中 `intent.ts` 中的子串规则，仍会走流式，模型可能口头描述工具但不会真正调用。

### 7.3 AI 建单

用户明确售后诉求并提供有效本人订单号时，AI 可调用 `CREATE_TICKET` 创建工单。未提供订单号时，AI 应先调用 `LIST_MY_ORDERS` 让用户选择订单。

AI 创建工单会写入：

- `ticket.source = AI`
- `ticket.session_id = 当前会话 ID`
- `ticket_flow` 创建记录
- `ai_tool_call_log` 工具调用记录

### 7.4 转人工和客服接待

用户主动点击转人工，或 AI 回合后满足转人工策略时（默认不含 RAG 未命中自动转人工，见 [10.5](#105-转人工策略)）：

1. `chat_session.service_mode` 从 `AI` 变为 `WAITING_AGENT`。
2. 系统创建咨询类转人工工单。
3. `staff01` 在 `/staff/live-chat` 查看等待队列。
4. 客服接入后，`service_mode` 变为 `HUMAN`。
5. 用户和客服通过 WebSocket 接收消息推送，消息仍通过 REST 接口入库。

### 7.5 管理员查看 AI 链路

使用 `admin` 登录，进入 `/admin/ai-logs`，可以查看：

- 会话列表；
- 单会话消息；
- RAG 检索记录；
- 工具调用记录；
- 会话 Trace。

---

## 8. 后端说明

### 8.1 启动入口

| 项 | 值 |
|----|----|
| 主类 | `com.ddy.aicustomerservice.AiCustomerServiceBackendApplication` |
| 默认端口 | `8080` |
| 默认 Profile | `dev` |
| 异步支持 | `@EnableAsync` |
| 配置属性扫描 | `@ConfigurationPropertiesScan` |

### 8.2 模块职责

| 模块 | 路径 | 说明 |
|------|------|------|
| 认证 | `module/auth` | 注册、登录、登出、当前用户 |
| 用户 | `module/user` | 管理端用户分页、客服账号、状态和密码 |
| 角色 | `module/role` | 用户角色绑定 |
| 订单 | `module/order` | 用户、客服、管理员订单查询 |
| 工单 | `module/ticket` | 工单创建、代建、认领、指派、处理、关闭、取消、流转 |
| 对话 | `module/chat` | AI 问答、会话、消息、转人工、在线客服 |
| WebSocket | `module/chat/websocket` | 握手、订阅、推送 |
| 知识库 | `module/knowledge` | 知识库元数据 |
| 文档 | `module/document` | 上传、解析、分块、向量化 |
| 检索 | `module/retrieval` | 向量检索、RRF 重排、检索日志 |
| AI 工具 | `module/ai/tool` | Spring AI `@Tool`、工具上下文、调用日志 |
| AI 日志 | `module/admin/ailog` | 管理端 Trace、检索日志、工具日志 |

### 8.3 安全模型

`SecurityConfig` 使用 JWT 无状态鉴权：

| 路径 | 权限 |
|------|------|
| `POST /api/auth/register` | 公开 |
| `POST /api/auth/login` | 公开 |
| `/api/test/**` | 仅 `dev` Profile 放开 |
| Swagger | 仅 `dev` Profile 放开 |
| `/ws/**` | 放行给 WebSocket 握手，握手拦截器校验 JWT |
| `/api/admin/**` | `ROLE_ADMIN` |
| `/api/staff/**` | `ROLE_STAFF` 或 `ROLE_ADMIN` |
| 其他接口 | 已登录 |

JWT 相关机制：

- `JwtAuthenticationFilter` 解析 `Authorization: Bearer <token>`；
- Redis 黑名单用于登出后 token 失效；
- `UserTokenVersionService` 支持 token 版本，便于重置密码等场景使旧 token 失效；
- `JwtSecretProdValidator` 在 `prod` Profile 下校验 JWT Secret 不能为空、不能使用默认值、长度至少 32。

SSE 流式与异步鉴权（`/ask/stream`）：

- 无 Session 场景下，`SecurityContext` 使用 `RequestAttributeSecurityContextRepository` 写入 request attribute，避免 `SseEmitter` 触发 Tomcat `ASYNC` 派发时登录态丢失；
- `DispatcherType.ASYNC`、`ERROR` 的二次派发在首次 `REQUEST` 已校验 JWT 的前提下放行，防止流式结束时报 `AuthorizationDeniedException` 或 `response already committed`；
- `application.yml` 中配置 `spring.security.filter.dispatcher-types: request,async,error`，与上述策略配合。

### 8.4 工单状态

| 状态 | 说明 |
|------|------|
| `PENDING` | 待分配 |
| `PROCESSING` | 处理中 |
| `RESOLVED` | 已解决 |
| `CLOSED` | 已关闭 |
| `CANCELLED` | 已取消 |

工单类型：

```text
REFUND、RETURN_GOODS、EXCHANGE、LOGISTICS、INVOICE、PRODUCT_QUALITY、ACCOUNT、CONSULTATION、OTHER
```

### 8.5 会话状态

| 状态 | 说明 |
|------|------|
| `AI` | AI 自动接待 |
| `WAITING_AGENT` | 已转人工，等待客服接入 |
| `HUMAN` | 客服已接入，人工接待中 |

`chat_session.version` 使用 MyBatis-Plus 乐观锁。客服接入会话时，前端传入 `expectedVersion`，后端校验当前版本，减少多个客服同时抢接同一会话造成的并发问题。

---

## 9. 前端说明

### 9.1 页面路由

| 路径 | 角色 | 页面 |
|------|------|------|
| `/login` | 公开 | 登录 |
| `/register` | 公开 | 注册 |
| `/chat` | USER、STAFF、ADMIN | AI 助手 |
| `/orders` | USER | 我的订单 |
| `/tickets` | USER | 我的工单 |
| `/tickets/:id` | USER、STAFF、ADMIN | 工单详情 |
| `/staff/live-chat` | STAFF | 在线客服 |
| `/staff/tickets/pool` | STAFF | 待认领工单 |
| `/staff/tickets/mine` | STAFF | 我的处理工单 |
| `/admin/orders` | ADMIN | 订单管理 |
| `/admin/users` | ADMIN | 用户管理 |
| `/admin/tickets` | ADMIN | 工单管理 |
| `/admin/knowledge` | ADMIN | 知识库管理 |
| `/admin/ai-logs` | ADMIN | AI 链路追踪 |

### 9.2 前端目录

```text
src/
├── api/
│   ├── admin.ts
│   ├── auth.ts
│   ├── chat.ts
│   ├── http.ts
│   ├── liveChat.ts
│   ├── order.ts
│   └── ticket.ts
├── components/
├── config/
├── layouts/
├── router/
├── stores/
├── utils/
│   ├── intent.ts
│   ├── liveChatWs.ts
│   ├── markdown.ts
│   └── stream-reveal.ts
└── views/
```

### 9.3 前端环境变量

| 文件 | 当前值 | 说明 |
|------|--------|------|
| `.env.development` | `VITE_API_BASE=` | 开发环境留空，走 Vite 代理 |
| `.env.production` | `VITE_API_BASE=http://localhost:8080` | 当前仍是本地地址，真实部署前需要修改 |

WebSocket 地址由 `src/utils/liveChatWs.ts` 根据当前页面协议和域名拼接：

```text
ws://当前前端域名/ws/live-chat?token=<JWT>
```

开发环境由 Vite 代理到后端 `/ws`。

---

## 10. AI 与 RAG 设计

### 10.1 知识库流水线

```text
管理员上传文档
  -> Apache Tika 解析文本
  -> 文本分块
  -> DashScope text-embedding-v3 向量化
  -> 写入 SimpleVectorStore
  -> 保存到本地 JSON
  -> 用户提问时向量检索
  -> 可选 RRF 重排
  -> 拼入 Prompt
  -> DeepSeek 生成回答
```

相关配置：

| 配置 | 说明 |
|------|------|
| `file.upload.knowledge-dir` | 原始文档上传目录 |
| `vector.store.simple-store-path` | 本地向量库 JSON 路径 |
| `ai-customer-service.knowledge.top-k` | 最终返回片段数 |
| `ai-customer-service.knowledge.similarity-threshold` | 相似度阈值 |
| `ai-customer-service.retrieval.rerank-enabled` | 是否启用 RRF 重排 |

### 10.2 同步问答与流式问答

| 接口 | 特点 |
|------|------|
| `POST /api/ai/knowledge-chat/ask` | 同步返回，挂载业务 Tools，适合查单、建单、查工单、转人工 |
| `POST /api/ai/knowledge-chat/ask/stream` | SSE 流式返回，当前不挂载业务 Tools，适合政策问答和打字机效果 |

前端通过 `src/utils/intent.ts` 的 `shouldUseSyncMode()` 路由：

- 命中 `ORD…` / `TK…` 单号正则，或问题中包含配置短语（如 `查订单`、`申请售后`、`转人工` 等）→ 调用同步 `/ask`；
- 匹配为 **连续子串**（例如必须包含「查订单」，「查我的订单」不会命中）→ 未命中则走 `/ask/stream`；
- 聊天页「智能动作」开关关闭时，一律走流式，不触发工具。

### 10.3 角色 Prompt

| 角色 | Prompt 定位 |
|------|-------------|
| USER | 消费者售后客服，回答政策、查本人订单、查本人工单、引导建单或转人工 |
| STAFF | 客服工作台助手，提供订单上下文、工单摘要和回复建议 |
| ADMIN | 管理后台运营助手，分析平台概览、热点问题、知识库优化方向 |

Prompt 配置在 `application.yml` 的 `ai-customer-service.chat` 下。

### 10.4 AI 工具

| 工具 | 可用角色 | 说明 |
|------|----------|------|
| `QUERY_ORDER` | USER、STAFF、ADMIN | USER 只能查本人订单；STAFF/ADMIN 可查全平台订单 |
| `LIST_MY_ORDERS` | USER | 列出当前用户订单，供建工单前选择 |
| `CREATE_TICKET` | USER | AI 创建用户售后工单，必须绑定有效本人订单号 |
| `QUERY_TICKET_STATUS` | USER、STAFF、ADMIN | 按权限查询工单状态 |
| `LIST_STAFF_TICKETS` | STAFF、ADMIN | 查询工单池或我的工单 |
| `ADMIN_PLATFORM_OVERVIEW` | ADMIN | 查询平台订单、工单、工具失败概览 |
| `ADMIN_HOT_QUESTIONS` | ADMIN | 分析近期用户热点问题 |

### 10.5 转人工策略

配置位置：

```yaml
ai-customer-service:
  handoff:
    enabled: true
    rag-miss-auto: false   # 默认关闭：不因 RAG 未命中/低分自动转人工
    low-score-threshold: 0.55
    repeat-miss-count: 2
    user-keywords: 转人工,人工客服,找客服,真人,人工服务,要投诉
    refusal-phrases: 无法回答,没有相关,建议联系人工,不清楚,无法确定,联系人工客服
```

触发来源（`HandoffTriggerEvaluator`）：

- 用户点击「转人工」或消息命中 `user-keywords`；
- AI 返回拒答短语（`refusal-phrases`）或服务异常提示；
- 当 `rag-miss-auto: true` 时 additionally：RAG 未命中、召回分数低于阈值、连续未命中。

当前默认 `rag-miss-auto: false`，政策类问答即使未命中知识库也不会自动排队人工；若需演示「检索失败即转人工」，可在 `application.yml` 或 `application-local.yml` 中改回 `true`。

---

## 11. 核心接口

所有受保护接口均使用：

```text
Authorization: Bearer <JWT>
```

### 11.1 认证

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/auth/register` | 用户注册 |
| `POST` | `/api/auth/login` | 登录 |
| `GET` | `/api/auth/me` | 当前用户 |
| `POST` | `/api/auth/logout` | 登出 |

### 11.2 AI 问答与会话

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/api/ai/knowledge-chat/ask` | 同步问答，支持 Tools |
| `POST` | `/api/ai/knowledge-chat/ask/stream` | SSE 流式问答 |
| `GET` | `/api/ai/knowledge-chat/sessions/my` | 我的会话分页 |
| `GET` | `/api/ai/knowledge-chat/sessions/{sessionId}` | 会话详情 |
| `GET` | `/api/ai/knowledge-chat/sessions/{sessionId}/messages` | 消息列表 |
| `POST` | `/api/ai/knowledge-chat/sessions/{sessionId}/handoff` | 用户主动转人工 |
| `POST` | `/api/ai/knowledge-chat/sessions/{sessionId}/messages` | 用户在人工模式发送消息 |

### 11.3 客服在线会话

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/staff/live-chat/sessions/waiting` | 等待接入会话 |
| `GET` | `/api/staff/live-chat/sessions/mine` | 我的接待中会话 |
| `GET` | `/api/staff/live-chat/sessions/{sessionId}` | 会话详情 |
| `POST` | `/api/staff/live-chat/sessions/{sessionId}/accept` | 接入会话 |
| `POST` | `/api/staff/live-chat/sessions/{sessionId}/end` | 结束会话 |
| `GET` | `/api/staff/live-chat/sessions/{sessionId}/messages` | 消息列表 |
| `POST` | `/api/staff/live-chat/sessions/{sessionId}/messages` | 客服发送消息 |

### 11.4 订单

| 前缀 | 方法与路径 | 角色 |
|------|------------|------|
| `/api/orders` | `GET /my`、`GET /{orderNo}` | USER |
| `/api/staff/orders` | `GET /{orderNo}` | STAFF、ADMIN |
| `/api/admin/orders` | `GET /page`、`GET /{orderNo}` | ADMIN |

### 11.5 工单

| 前缀 | 主要接口 |
|------|----------|
| `/api/tickets` | 创建、我的工单、详情、关闭、取消 |
| `/api/staff/tickets` | 代客建单、分页、认领、处理 |
| `/api/admin/tickets` | 管理员分页、指派 |
| `/api/tickets/{ticketId}/flows` | 工单流转时间线 |
| `/api/admin/ticket-flows/page` | 管理端流转分页 |

### 11.6 知识库和文档

| 前缀 | 主要接口 |
|------|----------|
| `/api/admin/knowledge-bases` | 知识库 CRUD、启停、分页 |
| `/api/admin/knowledge-documents` | 上传、分页、详情、解析、批量解析、删除 |
| `/api/admin/document-chunks` | 分块、批量分块、分页、删除 |
| `/api/admin/document-vectors` | 向量化、批量向量化、删除向量、状态 |
| `/api/admin/knowledge-retrieval` | 检索调试 |

### 11.7 管理端日志

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/api/admin/ai-logs/sessions/page` | AI 会话分页 |
| `GET` | `/api/admin/ai-logs/sessions/{sessionId}/messages` | 会话消息 |
| `GET` | `/api/admin/ai-logs/sessions/{sessionId}/trace` | 会话 Trace |
| `GET` | `/api/admin/ai-logs/retrieval/page` | 检索日志分页 |
| `GET` | `/api/admin/ai-logs/retrieval/{id}` | 检索日志详情 |
| `GET` | `/api/admin/ai-logs/tools/page` | 工具日志分页 |
| `GET` | `/api/admin/ai-logs/tools/{id}` | 工具日志详情 |

---

## 12. 数据库与存储

### 12.1 MySQL

默认数据库名：

```text
ai-customer-service
```

主要表：

| 表 | 说明 |
|----|------|
| `sys_user` | 用户 |
| `sys_role` | 角色 |
| `sys_user_role` | 用户角色关系 |
| `customer_order` | 演示订单 |
| `ticket` | 售后工单 |
| `ticket_flow` | 工单流转记录 |
| `chat_session` | AI/人工会话 |
| `chat_message` | 消息 |
| `knowledge_base` | 知识库 |
| `knowledge_document` | 知识文档 |
| `document_chunk` | 文档分块 |
| `ai_retrieval_log` | 检索日志 |
| `ai_tool_call_log` | 工具调用日志 |

### 12.2 Redis

当前用途：

- JWT 登出黑名单；
- 用户 token version；
- 后续可扩展热点缓存、会话缓存、限流等。

### 12.3 本地文件

| 路径 | 说明 |
|------|------|
| `./uploads/knowledge` | 上传知识文档 |
| `./uploads/vector-store/knowledge-vector-store.json` | SimpleVectorStore 本地向量文件 |

---

## 13. 配置说明

### 13.1 后端配置分层

| 文件 | 是否应提交 | 说明 |
|------|------------|------|
| `application.yml` | 是 | 公共配置、AI 模型、Prompt、RAG、转人工策略 |
| `application-dev.yml` | 是 | 开发数据库、Redis、Swagger、日志 |
| `application-local.yml.example` | 是 | 本地密钥模板 |
| `application-local.yml` | 否 | 本地真实密钥、数据库密码 |
| `application-prod.yml` | 视实际内容 | 当前项目应检查并补齐生产配置 |
| `application-docker.yml` | 视实际内容 | Docker 部署配置，使用前需确认内容完整 |

### 13.2 当前开发默认值

| 配置 | 默认值 |
|------|--------|
| `spring.profiles.active` | `dev` |
| `server.port` | `8080` |
| MySQL URL | `jdbc:mysql://localhost:8090/customer-service-ai` |
| Redis | `localhost:6379` |
| DeepSeek Base URL | `https://api.deepseek.com` |
| DashScope Base URL | `https://dashscope.aliyuncs.com/compatible-mode/v1` |
| Chat Model | `deepseek-chat` |
| Embedding Model | `text-embedding-v3` |
| 向量文件 | `./uploads/vector-store/knowledge-vector-store.json` |

### 13.3 生产配置提醒

生产部署前至少需要处理：

- 设置 `SPRING_PROFILES_ACTIVE=prod`；
- 设置强随机 `JWT_SECRET`；
- 使用生产数据库和 Redis；
- 设置 DeepSeek 和 DashScope API Key；
- 修改前端 `.env.production` 的 `VITE_API_BASE`；
- 配置 HTTPS 和 WSS；
- 确认 WebSocket 代理规则；
- 替换或共享向量存储；
- 禁止执行带 `DROP DATABASE` 的初始化脚本。

---

## 14. 测试与验证

### 14.1 后端测试

```bash
cd ai-customer-service-backend
mvn test
```

当前 Maven Surefire 配置默认排除 `integration` 分组。

RAG 评测相关文件：

| 路径 | 说明 |
|------|------|
| `src/test/java/com/ddy/aicustomerservice/rag/RagEvaluationTest.java` | RAG 关键词评测与集成测试占位 |
| `src/test/resources/rag-eval/questions.json` | 评测问题集 |

### 14.2 前端构建

```bash
cd ai-customer-service-frontend
npm install
npm run build
```

### 14.3 推荐手工验收清单

1. `admin` 登录成功。
2. 上传 `AI客服系统文档/` 并完成解析、分块、向量化。
3. `user01` 提问「退款多久到账」能召回知识库片段。
4. `user01` 提问「查订单 ORD202606010003」能触发工具调用。
5. `user01` 申请售后能创建工单。
6. `staff01` 能在工单池认领并处理工单。
7. 用户主动转人工后，`staff01` 能接入在线会话。
8. 用户和客服消息能通过 WebSocket 实时出现在双方页面。
9. `admin` 能在 AI 链路追踪看到检索日志和工具日志。

---

## 15. 当前边界与生产化建议

### 15.1 当前边界

| 类别 | 当前情况 |
|------|----------|
| 向量库 | 使用 `SimpleVectorStore` 本地 JSON，适合单机演示，不适合多实例生产 |
| 订单系统 | 订单为本地表演示数据，当前主要是查询，没有对接真实 OMS/ERP |
| 工单与订单 | 工单处理不会自动更新订单退款、换货、物流等状态 |
| 流式问答 | `/ask/stream` 不挂业务工具；需口语化查单时扩展 `intent.ts` 或统一走同步 |
| 转人工策略 | 默认 `rag-miss-auto: false`，仅关键词/拒答/异常等触发；可按演示需要开启 RAG 自动转人工 |
| 同步路由 | `intent.ts` 为子串匹配，「查我的订单」与「查订单」行为不同 |
| WebSocket | 握手校验 JWT；订阅会话级权限建议继续加固 |
| 生产配置 | 需要补齐 prod/docker 配置、前端生产 API 地址和 HTTPS/WSS |
| 初始化脚本 | 一键初始化会删除数据库，只适合本地 |

### 15.2 生产化建议

- 将本地 JSON 向量库替换为 pgvector、Milvus、Redis Stack 或云向量服务；
- WebSocket 订阅时校验当前用户是否为会话用户、接待客服或管理员；
- 增加客服在线状态、技能组、排队策略、SLA 超时升级；
- 工单处理联动订单、退款、换货、物流等外部系统；
- 对手机号、地址、聊天内容、工具入参和日志做脱敏；
- 增加限流、审计、监控、告警和日志归档；
- 使用 Flyway 或 Liquibase 管理数据库迁移；
- 为 RAG 引入文档版本、生效期、审核流程；
- 将 AI 工具调用失败、模型超时、向量库异常纳入可观测指标；
- 补充端到端测试和关键服务单元测试。

---

## 16. 常见问题

### 16.1 为什么问政策没有知识库来源？

需要先用管理员上传知识文档，并完成解析、分块、向量化。仅初始化数据库不会自动导入 `AI客服系统文档/`。

### 16.2 为什么查订单没有触发工具？

工具只挂在同步 `/ask` 接口。前端 `shouldUseSyncMode()` 使用 **连续子串** 匹配，例如：

| 用户输入 | 是否走同步 | 说明 |
|----------|------------|------|
| `查订单` | 是 | 命中短语「查订单」 |
| `ORD202606010003 到哪了` | 是 | 命中订单号正则 |
| `查我的订单` | 否 | 中间有「我的」，不包含连续子串「查订单」 |
| `查最近订单` | 否 | 中间有「最近」 |

未走同步时会调用 `/ask/stream`，模型可能在文案里写「正在调用 LIST_MY_ORDERS」，但 **不会真正执行工具**，意图也常显示为 `KNOWLEDGE_QA`。

演示建议直接输入：

```text
查订单
查订单 ORD202606010003
```

并确认聊天页 **「智能动作」** 已开启。长期可在 `src/utils/intent.ts` 增加「我的订单」等短语或正则 `查.{0,4}订单`。

### 16.3 流式问答报错 Access Denied 或连接中断？

多出现在 `/ask/stream`：`SseEmitter` 触发 Servlet `ASYNC` 派发时，JWT 无状态场景下 `SecurityContext` 未绑定到 request，二次鉴权失败。项目已在 `SecurityConfig` 使用 `RequestAttributeSecurityContextRepository`，并对 `ASYNC`/`ERROR` 派发做放行；同时配置 `spring.security.filter.dispatcher-types`。修改后需 **重启后端**。

### 16.4 为什么开发数据库端口是 8090？

当前 `application-dev.yml` 配置为 `localhost:8090`。如果你的 MySQL 是默认 `3306`，请在 `application-local.yml` 或 `application-dev.yml` 中调整 URL。

### 16.5 为什么还会自动转人工？

默认 `rag-miss-auto: false`，**不会因知识库未命中或低分自动转人工**。仍可能触发的情况：

- 用户说「转人工」等 `user-keywords`；
- AI 回答包含 `refusal-phrases` 中的短语（如「建议联系人工」）；
- 用户点击聊天页「转人工」按钮。

若需恢复「RAG 失败即转人工」演示，将 `ai-customer-service.handoff.rag-miss-auto` 设为 `true`。

### 16.6 为什么生产前端还指向 localhost？

`.env.production` 当前写的是 `VITE_API_BASE=http://localhost:8080`，这是本地演示值。部署到服务器前需要改成真实 API 域名。

### 16.7 在线客服消息重复或用户端 AI 仍插话？

- 坐席发消息后若同一条显示两次：HTTP 返回与 WebSocket 推送各追加一次，前端已按消息 `id` 去重（`StaffLiveChatView`）。
- 人工已接入（`HUMAN`）后用户发消息：不应再插入 AI 占位回复，仅 `WAITING_AGENT` 排队时提示等待（`ChatView`）。

---

## 17. 参考入口

| 类型 | 路径 |
|------|------|
| 后端主类 | `ai-customer-service-backend/src/main/java/com/ddy/aicustomerservice/AiCustomerServiceBackendApplication.java` |
| 后端配置 | `ai-customer-service-backend/src/main/resources/application.yml` |
| 本地密钥模板 | `ai-customer-service-backend/src/main/resources/application-local.yml.example` |
| 数据库初始化 | `ai-customer-service-backend/src/main/resources/db/init-ai-customer-service.sql` |
| 前端入口 | `ai-customer-service-frontend/src/main.ts` |
| 前端路由 | `ai-customer-service-frontend/src/router/index.ts` |
| AI 聊天页面 | `ai-customer-service-frontend/src/views/chat/ChatView.vue` |
| 在线客服页面 | `ai-customer-service-frontend/src/views/staff/StaffLiveChatView.vue` |
| 知识库页面 | `ai-customer-service-frontend/src/views/admin/KnowledgeView.vue` |
