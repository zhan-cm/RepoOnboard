# T-0707 — API Map Stitch Design Brief

> 供 Stitch 在现有 RepoOnboard 产品 Shell 基础上生成高保真设计。数据边界以 `T-0707-UI-DATA-CONTRACT.md` 为准；本轮只设计 T-0707，不包含 Source Navigation、Start Here、OpenAPI、调用链或新的分析事实。

## 1. 页面目标

让第一次进入陌生 Spring Boot 仓库的开发者快速回答：

1. 当前报告发现了哪些 HTTP Endpoint？
2. 如何按 module、HTTP method 和 path 缩小范围？
3. 每个 Endpoint 由哪个 Controller handler 提供？
4. `ANY`、unresolved path / method / conditions 分别意味着什么？
5. 选中 Endpoint 的 handler 和 class-level / method-level mapping Evidence 位于哪里？

这是静态分析结果的 API 索引，不是 OpenAPI 文档站、API 测试客户端或运行时监控台。

## 2. 必须继承现有产品外壳

直接沿用已实现的 Overview、Module Explorer 和 Architecture Workspace：

- 保留左侧产品导航、顶部 Repository Context、中央工作区、右侧 Inspector。
- `APIs` 为当前 active navigation；Overview、Modules、Architecture 可进入；Start Here 仍为未开放。
- 延续现有浅色、低噪声、高密度、企业技术工作台风格。
- 使用已有 design tokens、字体、边框、圆角、状态色、间距和 focus 样式。
- 不引入第二套 design system，不增加永久第四栏。
- 不画原生桌面标题栏、最近项目、文件选择器或 IDE 专属控件。

视觉关键词：

```text
Evidence-first
Route index
Technical workbench
Calm
Dense but readable
Deterministic
Local-first
```

## 3. 主画板结构

建议 1440 × 900，继续使用现有三栏 Shell：

```text
┌──────────────┬────────────────────────────────────────────────┬──────────────────┐
│ Product Nav  │ Repository Context / reported endpoint count   │ API Inspector    │
│              ├────────────────────────────────────────────────┤                  │
│ Overview     │ API Map header + coverage status               │ Selected route   │
│ Modules      ├────────────────────────────────────────────────┤ Handler / source │
│ Architecture│ Module  Method  Search path/controller/handler │ Conditions       │
│ APIs         ├────────────────────────────────────────────────┤ Mapping evidence │
│ Start Here   │ Method │ Path │ Controller.handler │ Module   │                  │
│              │ route rows / explicit states                  │                  │
└──────────────┴────────────────────────────────────────────────┴──────────────────┘
```

中央区域的 route index 是视觉主角。Inspector 信息密度高但不挤压主列表；不要用架构图表达 API。

## 4. 页面模块

### 4.1 Repository Context Bar

沿用现有 workbench top bar：

- 左侧：`Repository` + repository name。
- 右侧：module count、reported endpoint count、analysis status。
- PARTIAL / FAILED 使用现有 status badge。
- 计数来自 report summary 或 `endpoints[]` 确定性计数；PARTIAL 时文案使用 reported。

### 4.2 API Header

- Eyebrow：`API map`
- Title：`HTTP endpoints`
- Description：`Explore reported Spring MVC routes and trace each handler back to its mapping evidence.`
- 轻量摘要：`{shown} of {reported} endpoints`、`{module count} modules`、coverage state。
- 不显示 API health、coverage percentage、REST score 或运行时请求量。

### 4.3 Filter Bar

最小控件：

- `Module`：All modules + 报告中的 exact modules。
- `HTTP method`：All methods + 当前报告实际出现的精确 method 值。
- 搜索框：`Search path, controller, handler, or source`。
- `Reset filters`。
- 只读 active filter summary 与 shown / filtered count。

交互要求：

- Module 与 HTTP method 使用精确值，不按文本推断。
- `ANY` 是一个可选 method 值，不代表 All methods。
- `UNRESOLVED` 独立出现并带 warning 语义。
- Path 搜索为不区分大小写的文本包含；不做正则、glob 或 path 参数解释。
- 筛选可组合；无结果时保留筛选栏并提供 Reset。
- 切换 scope 后若当前 selected Endpoint 不再可见，应清除选择。

### 4.4 Endpoint Index

宽屏优先使用紧凑、可扫描的 table-like list。建议列：

1. `Method`
2. `Path`
3. `Handler`
4. `Controller`
5. `Module`
6. `Source`
7. `Conditions / status`

每一行整体可选择并打开右侧 Inspector。使用 Endpoint ID 作为稳定身份，不以 path 或 handler 作为 key。

Method badge：

- GET、HEAD、POST、PUT、PATCH、DELETE、OPTIONS、TRACE 使用有限且稳定的色彩。
- ANY 使用中性 badge，文本必须完整可读。
- UNRESOLVED 使用 warning outline / icon，不能与 ANY 同色同文案。
- 颜色不是唯一区分手段，badge 必须显示 method 文本。

Path：

- resolved path 使用 mono 字体，保留 `/` 与 `{id}` 等原值。
- unresolved path 使用明确的 `Unresolved path` warning row，不显示空白、`/` 或伪路径。
- 长 path 可以换行或安全横向滚动；不得只留下无法识别的中间省略号。

Handler / Controller：

- 主文本可用 `ControllerName.handlerMethod`。
- 次级文本显示完整 Controller qualified name。
- 若 componentId 无法连接，显示 `Controller unavailable`，不从文件名猜测。
- 公共报告只有 handler method name；不要显示伪参数、返回类型或完整 signature。

Conditions：

- 有 params / headers / consumes / produces 时显示紧凑标记，例如 `4 conditions`，详细值进入 Inspector。
- `conditions.unresolved = true` 时额外显示 `Conditions unresolved`。
- 没有条件时可显示 `No additional conditions` 或保持安静，但不能在 unresolved 时显示 none。

Source：

- 显示 scan-root-relative file path 与存在时的 line。
- 此处只展示，不加入 `Open file`、`Copy path` 或 IDE 图标；这些属于 T-0708。

### 4.5 Inspector — Endpoint Selected

右侧信息层级：

1. `Selected endpoint`
2. Method badge + resolved path / `Unresolved path`
3. Handler method
4. Controller qualified name 与 exact component kind（连接成功时）
5. Module、Framework
6. `Mapping conditions`
   - Params
   - Headers
   - Consumes
   - Produces
   - `Some conditions unresolved`（适用时）
7. `Handler source`
   - relative source path
   - 1-based line / column（存在才显示）
   - symbol（存在才显示）
8. `Mapping evidence`
   - Controller mapping：`SPRING_MVC_TYPE_MAPPING`
   - Handler mapping：`SPRING_MVC_METHOD_MAPPING`
   - 每条显示 evidence type、location、ruleId、relatedLocations

证据区域必须明确是“位置和规则证据”，不能把 final path 拆成虚构的 class path 与 method path。不要从 ruleId 生成未经报告验证的注解源码。

### 4.6 Inspector — Nothing Selected

显示：

- `Select an endpoint to inspect its handler and mapping evidence.`
- 当前 module / method / search scope。
- reported、shown、filtered endpoint count。
- ANY、unresolved path、unresolved method、unresolved conditions 数量（只显示报告可确定的计数）。
- PARTIAL / FAILED 时显示显式 coverage diagnostics 摘要。

不要显示：API ownership、quality、security、request / response model 或 inferred domain。

## 5. 必须设计的状态

请在同一 Stitch 项目中提供一个主高保真画面，并在旁边提供关键状态规范或附加画板。

### Ready — Resolved Endpoint Selected

主画面选中一个 resolved GET Endpoint：

- 列表行高亮。
- Inspector 展示 handler source、conditions 和 class / method mapping Evidence。
- 示例可以有 class-level + method-level 两条 evidence，但必须标注为 design sample。

### ANY Method

- 使用 `ANY` badge。
- 说明 `No HTTP method restriction was reported.`
- 不把它扩展成多个 method，也不显示 unknown。

### Unresolved Path

- Endpoint 行仍存在，可选择。
- Path 显示 `Unresolved path`。
- Inspector 仍展示 method、handler、source、Evidence 与相关诊断。
- 不用 `/unknown`、变量名或注解文本补路径。

### Unresolved Method / Conditions

- `UNRESOLVED` method 独立 warning badge。
- 已确认 condition values 继续显示。
- `Some conditions unresolved` 与已知列表并存。

### Known Empty

`endpoints[]` 存在且为空：

- `No HTTP endpoints were reported.`
- PARTIAL 时补 coverage 提示。
- 不显示虚构 sample row。

### Filtered Empty

- `No endpoints match the current filters.`
- 显示当前 filters 和 Reset。
- 不误报为分析失败。

### Endpoint Data Unavailable

- `Endpoint data is unavailable in this report.`
- 与 known 0 保持不同文案与视觉语义。

### Partial / Failed

- 保留能确认的 Endpoint 列表。
- 使用 `reported endpoints`。
- 显示 mapping / endpoint 相关 diagnostics，不生成 completeness 百分比。

### Controller Link Unavailable

- Endpoint route、handler 和 source 继续显示。
- Controller 字段明确 unavailable。
- 不按 source path 或 symbol 猜 Controller。

### Loading / Report Error

- 复用现有 StatePanel。
- Shell 与仓库上下文尽量保持稳定。
- 不用伪 route skeleton 暗示真实 API 数量。

## 6. 响应式规则

### ≥ 1280px

- 完整三栏：导航 / Endpoint index / Inspector。
- 列表保持足够宽度，path 与 handler 是优先列。

### 960–1279px

- Inspector 移到主列表下方或使用既有紧凑 Inspector 策略。
- 次要列可折入 route row 的第二行，但 method、path、handler、状态不能消失。

### < 960px 与约 500px 桌面小窗口

- 复用顶部横向导航。
- Endpoint table 转为纵向 route cards：method + path 为首行，handler / module / source 为后续行。
- Filters 纵向堆叠，触控目标足够大。
- Inspector 成为列表下方详情，不依赖 hover。
- 长 path、qualifiedName、source path 和 ruleId 必须可换行或安全横向滚动。
- 不要求用户横向滚动整张数据表才能看到核心信息。

## 7. 可访问性要求

- Endpoint index 使用真实 table semantics，或使用带清晰列表 / 行标签的可访问结构。
- 每行可通过键盘选择；焦点与 selected state 明确区分。
- Filter input、Module、HTTP method、Reset 都有可访问名称。
- Method 与 unresolved 状态不能只依赖颜色。
- Conditions 使用有标题的列表，不把多个值拼成难读的 tooltip。
- Evidence location 与 ruleId 可由辅助技术完整读取。
- reduced-motion 下不使用列表重排或 Inspector 切换动画。
- Tooltip 不能承载唯一信息。

## 8. Stitch 示例数据约束

可使用以下纯设计样例展示不同状态：

```text
GET     /api/users          UserController.list
POST    /api/search         UserController.search
ANY     /api/any            UserController.anyMethod
DELETE  Unresolved path     UserController.unresolvedPath
PATCH   /api/users/{id}     UserController.unresolvedCondition
```

示例 conditions：

```text
headers: X-Tenant
params: active=true
consumes: application/json
produces: application/json
```

约束：

- 所有示例都是 design sample，不是 RepoOnboard 当前扫描结果。
- 不加入 request / response JSON、status code、DTO schema、authentication、owner、traffic 或 latency。
- 不展示虚构的 `@GetMapping("...")` 源码；Evidence 只显示 type、ruleId 与位置。
- 不把 `ANY` 标成 unknown，也不为 unresolved path 填入看似真实的路径。

## 9. 不要设计

- Swagger / OpenAPI 文档门户
- Try it / Send request / API client
- request / response schema 或示例 payload
- HTTP status code、exception mapping
- authentication / authorization
- API version、owner、domain、quality score
- runtime traffic、latency、availability
- Handler 调用链、Service / Repository 依赖图
- 根据 path 自动分组的业务领域
- Source Navigation 的 Copy / Open / IDE actions
- 新图表、饼图、趋势图或装饰性指标

## 10. Stitch 输出要求

请输出：

1. 1440 × 900 主高保真页面：resolved Endpoint selected。
2. 同一设计系统下的 ANY、unresolved path / conditions、filtered empty 与 known empty 状态规范。
3. 约 500px 宽的小窗口 / 窄屏布局。
4. 关键 spacing、row height、badge、filter、selected / focus、Inspector 和 responsive 说明。
5. 可导入仓库的静态 HTML / CSS 与截图；不依赖外部 CDN、远程字体或运行时网络资源。

Stitch 输出是视觉与交互交接资料，不是生产实现。后续 Codex 必须再次审阅其示例值、状态与事实边界，再映射到 Vue 和当前公共报告。
