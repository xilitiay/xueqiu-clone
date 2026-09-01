# 投资圈 · 雪球风格社区（演示版）

一个**功能对标雪球财经**的投资社区演示项目。**非雪球官方产品**，不复制其源码、商标或任何版权内容；
所有数据均为本地 Mock 的「示意性」市场评论，不构成任何投资建议。

## 技术栈（贴近雪球真实后端栈）

| 层 | 技术 | 说明 |
|----|------|------|
| 前端 | React 18 + Vite + React Router | 单页应用，雪球 Web 同为 React 技术体系 |
| 后端 | Java 17 + Spring Boot 3 + Spring Data JPA | 雪球后端以 Java/Spring 微服务为主 |
| 存储 | H2（演示）/ MySQL（生产） | 演示用内存库零依赖启动；生产可换 MySQL + Redis |
| 接口 | RESTful JSON（/api/**） | 信息流、个股、点赞、评论 |
| 实时推送 | WebSocket（SockJS + STOMP） | 行情/指数实时推送，替代前端轮询（贴近雪球实时推送架构） |

> 雪球生产环境还包含微服务拆分、Thrift/Dubbo RPC、Kafka、Elasticsearch、Redis 等。
> 本演示做了合理简化，但保留了「前端 React + 后端 Java/Spring」这一核心架构对应关系。

## 目录结构

```
.
├── backend/          # Spring Boot 后端（Java）
│   ├── pom.xml
│   └── src/main/java/com/xueqiu/clone/
│       ├── model/       实体：User / Stock / Post / Comment / PostLike / IndexDef
│       ├── repository/  JPA 仓储（含 PostLikeRepository / IndexDefRepository）
│       ├── service/     FeedService / StockService / UserService / QuoteService / QuotePushScheduler
│       ├── controller/  FeedController / StockController / UserController / QuoteController / AuthController / SearchController / IndexDefController
│       ├── dto/         响应体（避免直接暴露实体）
│       ├── filter/      JWT 认证过滤器
│       └── config/      CORS、安全、JWT、WebSocket、Mock 数据初始化
└── frontend/         # React + Vite 前端
    ├── src/components/  Header / FeedPage / PostCard / StockDetailPage / Sparkline ...
    ├── src/api/client.js  API 层（后端不可用时自动回退到 public/mock）
    └── public/mock/      离线预览用的示例 JSON
```

## 运行后端（需要 JDK 17+ 与 Maven）

```bash
cd backend
mvn spring-boot:run
# 接口示例：
#   GET  http://localhost:8080/api/feed
#   GET  http://localhost:8080/api/stocks/hot
#   GET  http://localhost:8080/api/stocks/SH600519
#   GET  http://localhost:8080/api/users/1
#   GET  http://localhost:8080/api/quote/SH600519        # 实时行情
#   GET  http://localhost:8080/api/quotes?symbols=SH600519,SZ300750
#   GET  http://localhost:8080/api/indices               # 市场指数实时行情
#   GET  http://localhost:8080/api/index-defs            # 指数配置列表（可配置落库）
#   POST http://localhost:8080/api/index-defs            # 新增指数配置（body: {code,name,market,sortOrder,enabled}）
#   PUT  http://localhost:8080/api/index-defs/{id}       # 修改（重排序/启停/改名）
#   DELETE http://localhost:8080/api/index-defs/{id}     # 删除
#   WebSocket: ws://localhost:8080/ws  (SockJS+STOMP，订阅 /topic/quotes、/topic/indices 收实时行情)
#   GET  http://localhost:8080/api/search?q=茅台&type=all  # 搜索
#   POST http://localhost:8080/api/auth/register         # 注册（body: {username,name,password}）
#   POST http://localhost:8080/api/auth/login            # 登录（body: {username,password}）
#   POST http://localhost:8080/api/posts                 # 发帖（需 Authorization: Bearer <token>）
#   POST http://localhost:8080/api/posts/2/comments      # 评论（需 token）
#   POST http://localhost:8080/api/feed/2/like           # 点赞（需 token）
# H2 控制台：http://localhost:8080/h2-console
#
# 演示账号：用户名 demo / 密码 123456（前端会自动以此账号登录）
# 写接口需在请求头携带：Authorization: Bearer <登录返回的 token>
```

## 运行前端

```bash
cd frontend
npm install
npm run dev        # 开发服务器 http://localhost:5173
# 或
npm run build && npm run preview
```

前端默认把 `/api` 代理到 `http://localhost:8080`。**若后端未启动，前端会自动回退到
`public/mock` 下的静态示例数据**，纯前端环境下也能直接预览界面。

## 已实现功能

- 首页信息流：卡片式动态流，无限滚动加载（进入视口自动加载下一页）
- **发帖**：首页顶部发帖框，需登录，关联股票代码（可选）
- 点赞 / 取消点赞（乐观更新，需登录）
- **评论**：展开、分页加载（"查看更多评论"）、发表评论（需登录）
- **搜索** `/search`：综合检索帖子 / 股票 / 用户
- 热门股票榜（侧栏）
- 个股详情页：行情、涨跌幅、迷你 K 线走势图、相关讨论、实时报价网格（今开/高/低/昨收/换手/PE/PB/市值）
- **用户主页** `/user/:id`：资料、关注数、该用户发布的全部帖子
- **行情中心** `/market`：指数条 + 自选股实时报价表（自动刷新 + 手动刷新）+ 热门股票
- **真实鉴权**：Spring Security + JWT；读接口公开，写接口（发帖/点赞/评论）需 Bearer Token
- **点赞按用户持久化**：点赞记录落库（PostLike 表），多人/多端状态一致，可重复切换点赞/取消
- **指数列表可配置落库**：侧栏与指数条展示哪些指数、顺序、启停由 `t_index_def` 表驱动，
  提供 `IndexDefController` 进行增删改查，数据源不可达时回退内置 Mock
- **WebSocket 实时推送行情**：后端 `/ws`（SockJS+STOMP）每 5 秒向 `/topic/quotes`、`/topic/indices`
  推送实时行情，行情中心、侧栏指数、个股详情页订阅后即时跳动，无需轮询
- 路由：首页 `/`、行情 `/market`、个股 `/stock/:symbol`、帖子 `/post/:id`、用户 `/user/:id`、搜索 `/search`

### 实时行情接入（可配置行情 Key）

个股详情页、行情中心通过后端 `QuoteService` 代理**公开行情接口（腾讯 gtimg，无需授权 key）**
获取 A股/港股/美股的实时报价，仅在交易时段实时；后端代理可规避浏览器跨域。
**市场指数**（`/api/indices`）同样接入实时源，接口不可达时回退内置 Mock，侧栏/指数条始终有数据。

想接入**你自己的行情 Key**（如付费 JSON 行情源），修改 `backend/src/main/resources/application.yml`：

```yaml
app:
  quote:
    provider: json          # tencent=免key公开源；json=接入你自己的 JSON 行情源
    base-url: "https://你的行情域名/quote?="   # 你的行情接口地址
    api-key: "你的授权Key"                       # 后端会作为 Bearer Token 注入请求头
    indices: "SH000001,SZ399001,SZ399006,SH000300,HKHSI,USSPX"
```

- `provider=tencent`：沿用腾讯 gtimg 公开行情（免 key），无需配置 api-key。
- `provider=json`：后端以 `Authorization: Bearer <api-key>` 调用 `base-url`，并把返回的
  `{ symbol: { name, price, changePercent, ... } }` 映射为统一的 `QuoteDTO` / `IndexDTO`。
  换源只需调整这三处配置与（必要时）`QuoteService.fetchJsonQuotes()` 的字段映射。

### 实时推送（WebSocket / SockJS + STOMP）

为还原雪球「行情实时跳动」的体验，后端在 `/ws` 暴露 SockJS 端点，并用 STOMP 协议向客户端
推送实时行情，替代前端轮询：

- **端点**：`ws://localhost:8080/ws`（SockJS 在原生 WebSocket 不可用时自动降级为 HTTP 流/轮询）
- **主题**：
  - `/topic/quotes` —— `Map<symbol, QuoteDTO>`，批量个股实时行情
  - `/topic/indices` —— `List<IndexDTO>`，市场指数实时行情
- **推送节奏**：`QuotePushScheduler` 每 5 秒（上一轮完成后延迟 5s）推送一次；**仅在有客户端
  连接时**才请求外部行情源，避免无人订阅时空跑出网、触发行情接口频率限制。
- **推送清单**：个股清单由 `app.quote.ws-symbols` 配置（默认 5 只热点股）；指数清单由
  `t_index_def` 表（可配置落库）驱动。

前端（`src/api/wsClient.js`）维护单条长连接（自动重连），组件通过 `subscribeQuote(topic, cb)`
订阅主题即可拿到实时数据；**后端不可达时静默无数据，组件保留 REST 初始加载兜底**，页面不报错。
行情中心、侧栏指数、个股详情页均已接入。

> 说明：本机环境无 JDK/Maven、且禁止出网，后端未在此编译/运行；逻辑已审阅，请在本机
> `mvn spring-boot:run`（JDK17+）后验证真实行情代理、鉴权与 WebSocket 推送流程。

## 演示账号与安全边界

- 演示账号：`demo / 123456`（另含 user1~user6，密码同为 123456）。前端启动时自动以 `demo` 登录获取 JWT。
- JWT 密钥、token 有效期在 `application.yml` 的 `app.jwt` 下配置；**生产环境务必更换 `secret` 为足够长
  （≥32 字节）的随机串，并接入真实用户体系与 HTTPS**。
- 读接口对未登录用户开放以便浏览；写接口强制鉴权，缺失/无效令牌返回 401。

## 说明与边界

- 本仓库为**学习 / 演示用途**的技术还原，未使用「雪球」名称、Logo 或任何受版权保护的内容。
- 演示数据均为「示意性」市场评论，非真实行情、不代表任何真实作者观点，不构成投资建议。
- 生产化还可补充：推送横向扩缩（WebSocket 集群需借助 Redis 广播）、分页与缓存、微服务拆分、限流与防刷等。
