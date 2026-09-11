# T-0708 — Source Navigation Stitch Design Brief

> 供 Stitch 在现有 RepoOnboard 产品 Shell 基础上生成高保真设计。数据边界以 `T-0708-UI-DATA-CONTRACT.md` 为准；本轮只设计 T-0708，不包含源码读取、文件树、IDE 集成、Git 信息、调用链或新的分析事实。

## 1. 页面目标

让用户从已选择的 Component 或 Endpoint 继续回答：

1. 这个实体来自哪个扫描根相对文件？
2. 报告确认的起止行列和 symbol 是什么？
3. 文件属于哪个 module？
4. 哪些 Evidence 和相关事实能解释这条来源？
5. 如何安全复制 path、symbol 或 location，并在剪贴板不可用时继续手动操作？

这是 Source / Evidence Detail，不是代码编辑器或文件管理器。设计应强调 traceability、selection context 和可操作文本。

## 2. 信息架构边界

Source Detail 是跨页面辅助表面，不是一级导航：

- Sidebar 仍只有 Overview、Modules、Architecture、APIs、Start Here。
- 从 Architecture 的 Component Inspector 或 API Map 的 Endpoint Inspector 使用 `View source details` 进入。
- 进入后保留来源页面的 module、filters、search、selection 和滚动上下文。
- 提供清晰的 `Back to component` / `Back to endpoint`，返回原选择状态。
- 不新增永久 `Source` 导航项，不把用户带到浏览器地址栏或外部应用。

## 3. 必须继承现有产品外壳

直接沿用已实现的 Shell：

- 左侧 Product Nav、顶部 Repository Context、中央主内容和右侧 Inspector 视觉体系。
- 浅色、低噪声、高密度、技术工作台风格。
- 复用现有 design tokens、字体、圆角、边框、状态色、focus 和 reduced-motion 规则。
- 不设计原生桌面标题栏、文件选择器、IDE 品牌按钮或第二套 design system。

视觉关键词：

```text
Traceable
Evidence-first
Selectable text
Context preserving
Calm technical detail
Honest capability
```

## 4. 主画板结构

建议 1440 × 900，主画面从 API Map 的 selected Endpoint 进入：

```text
┌──────────────┬──────────────────────────────────────────────────┬──────────────────┐
│ Product Nav  │ Repository Context                               │ Related Facts    │
│              ├──────────────────────────────────────────────────┤                  │
│ Overview     │ ← Back to endpoint   SOURCE / EVIDENCE DETAIL   │ Controller       │
│ Modules      │ Entity identity + analysis status                │ Components       │
│ Architecture ├──────────────────────────────────────────────────┤ Endpoints        │
│ APIs active  │ Source path              [Copy path]             │ Dependencies     │
│ Start Here   │ Start / end position     [Copy location]         │ Diagnostics      │
│              │ Symbol                   [Copy symbol]           │                  │
│              ├──────────────────────────────────────────────────┤                  │
│              │ Module / Source File metadata                    │                  │
│              ├──────────────────────────────────────────────────┤                  │
│              │ Evidence list + related locations                │                  │
└──────────────┴──────────────────────────────────────────────────┴──────────────────┘
```

中央区是来源与 Evidence 主体，右侧只放可确认的 Related Facts。不要用代码编辑器占据中央画布。

## 5. 页面模块

### 5.1 Repository Context

- 保留 repository name、module / relevant entity count 和 SUCCESS / PARTIAL / FAILED。
- 保留来源页面 `APIs` 或 `Architecture` active 状态，表示 Source Detail 是该探索流的下钻层。
- 不显示本机绝对仓库路径。

### 5.2 Return Context

- Back 文案明确，例如 `Back to GET /api/users` 或 `Back to UserController`。
- 显示入口类型：`Endpoint source` / `Component source`。
- 可显示 entity stable ID 作为次级技术信息，但不让 ID 成为标题。
- 返回动作必须明显，不依赖浏览器 Back。

### 5.3 Entity Identity

Endpoint 示例：

- method badge + resolved path / `Unresolved path`
- handler method
- exact Controller qualified name（连接成功时）
- framework、module

Component 示例：

- component name / qualified name
- exact kind、framework、module

不要显示方法签名、参数、返回类型、owner、业务域或运行时状态。

### 5.4 Primary Source Location

按层级展示：

1. `Source path`：完整 scan-root-relative path，mono，可选择，可换行。
2. `Start`：`Line 14, column 5`，只显示存在的字段。
3. `End`：`Line 17, column 5`，只显示存在的字段。
4. `Symbol`：报告原值；缺失时显示 `Symbol unavailable`。
5. `Language`：仅在 Source File exact join 成功时展示。

动作：

- `Copy path`
- `Copy location`
- `Copy symbol`（symbol 可用时）

不要设计 Open File、Open IDE、Reveal、file URI 或 command snippet。

### 5.5 Copy Feedback

请设计三种轻量反馈：

- Success：`Path copied.` / `Symbol copied.` / `Location copied.`
- Clipboard unavailable：`Copy unavailable — select the text manually.`
- Copy failed / denied：保留可选择文本，并提供同一手动复制说明。

反馈应靠近动作、可被辅助技术读取且不遮挡内容。不要用只有图标或颜色的 toast；不要让失败清空 selection。

### 5.6 Module / Source File Metadata

- Module label、base directory、POM（exact module join 成功时）。
- Source File path、language（exact source file join 成功时）。
- 连接失败分别显示 `Module details unavailable` / `Source file metadata unavailable`。
- 不能按 path 前缀、package 或 repository name 猜 module。

### 5.7 Evidence List

每条 Evidence 展示：

- Evidence type
- Rule ID
- Primary location
- Related locations（可为 0 条）

Location 文本可选择，并可复用 Copy Location，但不要设计为可执行链接。Endpoint 的 class-level 与 method-level mapping Evidence 继续清晰分组；Component 保留其原 Evidence type。

不要从 ruleId 生成注解源码、解释文字或代码片段。

### 5.8 Related Facts

右侧按存在的数据分组：

- `Owning controller`：Endpoint exact componentId join。
- `Components in this file`：exact source path match。
- `Controller endpoints`：exact componentId match。
- `Confirmed dependency neighbors`：方向、kind、目标 Component 与来源位置。
- `Entry points in this file`：exact source path match。
- `File diagnostics`：severity、code、stage、message 和位置。

对 Endpoint 必须把依赖分组命名为 `Controller dependency neighbors`，并加短说明：`These are confirmed component relationships, not a handler call chain.`

不要按文件名、package、相似路径或文本匹配生成 Related Facts。

## 6. 必须设计的状态

请在同一 Stitch 项目中提供主高保真画面，并在旁边提供关键状态规范或附加画板。

### Ready — Endpoint Source

- 从一个 resolved Endpoint 进入。
- 显示 handler location、Controller、module、mapping Evidence。
- 右侧展示 same-controller endpoints 和 Controller confirmed dependency neighbors。
- 示例关系明确不是 call chain。

### Ready — Component Source

- 从 Architecture Component 进入。
- 显示 declaration location、kind、framework、module、Evidence。
- Related Facts 展示 same-file components 与确认 dependency neighbors。

### File-only Location

- 只有 source path，没有行列。
- 显示 `Exact line and column were not reported.`。
- Copy Path 与 Copy Location 仍可用，Location payload 等于 path。
- 不出现 line 0、column 0 或空 range。

### Symbol Unavailable

- 位置仍完整显示。
- `Copy symbol` 不可用或不显示，旁边明确 `Symbol unavailable`。
- 不从 handler / class label 补造 symbol。

### Clipboard Unavailable / Denied

- 文本保持可选择。
- 显示手动复制指导和失败状态。
- 不显示虚假成功反馈。

### Source File / Module Link Unavailable

- entity location 与 Evidence 继续显示。
- 连接失败的 metadata 单独显示 unavailable。
- 不隐藏整个详情，也不从 path 推断。

### Known Empty Related Facts

- 相关 collections 可用但没有 exact match。
- 显示 `No related facts were reported for this source.`。

### Related Data Unavailable

- 相关 collection 在报告中不可用。
- 使用与 known empty 不同的 unavailable 文案。

### PARTIAL / FAILED + File Diagnostics

- 顶部保持 analysis status。
- 已确认 location / Evidence 继续展示。
- 文件相关 diagnostics 可见，使用 `reported` / `available` 文案，不承诺完整性。

### Stale Selection

- 当前报告找不到原 entity ID 时显示明确 unavailable。
- 提供返回来源页面动作，不按 path 选择另一个实体。

## 7. 响应式规则

### ≥ 1280px

- 保持三栏 Shell。
- Source / Evidence 在中央，Related Facts 在右侧 Inspector。
- 长 path、qualifiedName、ruleId 不截断成不可辨识的中段省略号。

### 960–1279px

- Related Facts 可移到主内容下方或使用现有紧凑 Inspector 策略。
- Source path 与复制动作保持同一语义分组；按钮可换行但不覆盖文本。

### < 960px 与约 500px 小窗口

- 复用顶部横向导航。
- Back / identity / source / evidence / related facts 按顺序纵向堆叠。
- Copy actions 为清晰文本按钮，触控目标足够大。
- path、symbol、qualifiedName、ruleId 可换行或在字段内部安全横向滚动。
- 不依赖 hover、右键菜单或整页横向滚动。

## 8. 可访问性与安全要求

- Back、Copy Path、Copy Location、Copy Symbol 使用真实 button 与可访问名称。
- Copy feedback 使用 `aria-live="polite"`；错误不只依赖颜色。
- path / symbol / location 保持键盘可选文本。
- Evidence 和 Related Facts 使用有标题的列表 / definition list。
- 未知行列不输出 0；Unavailable 与 Known empty 文案不同。
- 所有项目文本按纯文本渲染，不使用 HTML preview。
- 不把 path 放入可点击 URL、shell command 或 IDE URI。
- reduced-motion 下取消 toast / panel 的非必要动画。

## 9. Stitch 示例数据约束

可以沿用 T-0707 fixture 风格作为 design sample：

```text
Endpoint: GET /api/users
Handler: UserController.list
Controller: fixture.api.UserController
Module: spring-api-project
Source: src/main/java/fixture/api/UserController.java
Start: line 14, column 5
End: line 17, column 5
Symbol: fixture.api.UserController#list
```

Evidence 示例仅包含：

```text
SPRING_MVC_METHOD_MAPPING
SPRING_MVC_TYPE_MAPPING
ruleId
primary location
related locations
```

约束：

- 所有值必须标注为 design sample，不代表当前扫描结果。
- 不展示真实 Windows 用户目录或绝对路径。
- 不添加 Java 源码片段、完整方法签名、Git author 或 IDE 状态。
- Related Components 必须带 confirmed relationship 来源；不生成调用链。
- 不显示 Open in IntelliJ / VS Code、Reveal、Edit 或 Run。

## 10. 不要设计

- 新的 Source 一级导航项
- 文件树、代码编辑器、语法高亮源码预览
- arbitrary file read / download
- 绝对路径、`file://`、IDE deep links
- Open / Reveal / Edit / Run / Terminal actions
- Git blame、history、author、branch 或 dirty status
- method signature、参数、返回值、注释或 imports
- Endpoint 调用链、sequence diagram 或 data flow
- 根据路径 / package 推断的 ownership、domain 或 importance
- 自动复制、隐藏 payload 或命令行生成器

## 11. Stitch 输出要求

请输出：

1. 1440 × 900 主高保真页面：从 resolved Endpoint 进入 Source Detail。
2. 同一设计系统下的 Component 入口、file-only location、symbol unavailable、clipboard unavailable、broken joins 与 PARTIAL 状态规范。
3. 约 500px 宽的小窗口 / 窄屏布局。
4. Back context、source field、copy action、feedback、Evidence、Related Facts、focus 与 responsive 说明。
5. 可导入仓库的静态 HTML / CSS 与截图；不依赖外部 CDN、远程字体或运行时网络资源。

Stitch 输出是视觉与交互交接资料，不是生产实现。后续 Codex 必须再次审阅其中的示例值、复制能力、related facts 和宿主动作，再映射到 Vue 与当前公共报告。
