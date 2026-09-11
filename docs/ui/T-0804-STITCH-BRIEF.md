# T-0804 — Start Here Stitch Design Brief

> 供 Stitch 在现有 RepoOnboard 产品 Shell 基础上生成高保真设计。数据边界以 `T-0804-UI-DATA-CONTRACT.md` 为准；本轮只设计 T-0804，不包含 AI 总结、学习进度、源码正文、IDE 集成、Settings / 语言切换或新的分析规则。

## 1. 页面目标

让第一次进入陌生 Spring Boot 仓库的开发者快速回答：

1. 我应该按什么顺序开始阅读？
2. 每个文件为什么被推荐？
3. 文件属于哪个 module，来源 Evidence 在哪里？
4. 哪些原因来自入口、配置、Endpoint 或确认依赖？
5. 如何从推荐项继续进入相关 Component 或 Endpoint？
6. 当分析是 PARTIAL 时，哪些结论仍可使用、哪些可能不完整？

这是一个 source-backed onboarding guide，不是 AI 教程、文件排行榜或学习进度工具。

## 2. 必须继承现有产品外壳

直接沿用已实现的 RepoOnboard Shell：

- 左侧 Product Nav、顶部 Repository Context、中央主内容和右侧 Inspector。
- `Start Here` 为 active navigation，移除 `Soon`；Overview、Modules、Architecture、APIs 仍可进入。
- 延续现有深色、低噪声、高密度、技术工作台风格。
- 复用 design tokens、字体、边框、圆角、状态色、focus 和 reduced-motion 规则。
- 不加入原生桌面标题栏、账户、云同步、Settings 或语言切换控件。
- 不建立第二套插画式 onboarding design system。

视觉关键词：

```text
Guided
Evidence-first
Calm
Sequential
Traceable
Honest coverage
Technical but approachable
```

## 3. 主画板结构

建议 1440 × 900，使用现有三栏 Shell：

```text
┌──────────────┬──────────────────────────────────────────────────┬────────────────────┐
│ Product Nav  │ Repository Context / recommendation count       │ Reading Inspector  │
│              ├──────────────────────────────────────────────────┤                    │
│ Overview     │ START HERE                                      │ Selected file      │
│ Modules      │ Source-backed reading path + status             │ Module / source    │
│ Architecture ├──────────────────────────────────────────────────┤                    │
│ APIs         │ Coverage notice (when required)                 │ Why this file?     │
│ Start Here ● ├──────────────────────────────────────────────────┤ Evidence locations │
│              │ 01 pom.xml                                      │                    │
│              │    Root Maven build file                        │ Related component  │
│              │ 02 DemoApplication.java                         │ Related endpoints  │
│              │    Application entry point                      │ [Explore ...]      │
│              │ 03 OrderController.java  ← selected             │                    │
│              │    Exposes 2 HTTP endpoints                     │                    │
│              │ ...                                             │                    │
│              │ [Show 4 more recommended files]                 │                    │
└──────────────┴──────────────────────────────────────────────────┴────────────────────┘
```

中央编号阅读路径是视觉主角。Inspector 负责完整解释、Evidence 和导航，不要让摘要指标或装饰插画抢占首屏。

## 4. 页面模块

### 4.1 Repository Context Bar

- 左侧：`Repository` + 当前 report 中的 repository name。
- 右侧：module count、recommended file count、analysis status。
- SUCCESS / PARTIAL / FAILED 使用现有 status badge。
- PARTIAL / FAILED 时使用 `reported recommendations` 或 `based on available facts`，不使用 complete / comprehensive。
- 不显示绝对仓库路径、branch、Git dirty state 或扫描耗时。

### 4.2 Start Here Header

- Eyebrow：`Onboarding guide`
- Title：`Start Here`
- Description：`Follow a deterministic, source-backed path through the repository's build, entry points, APIs, and confirmed dependencies.`
- Metadata：`{total} recommended files`、`Default view: {defaultLimit}`、coverage state。
- 可有一个简短说明：`The order is generated from confirmed report facts, not an AI score.`

不要显示：

- 推荐可信度百分比
- onboarding score
- 项目难度
- 预计总阅读时间
- 个性化推荐或“为你生成”

### 4.3 Coverage Notice

仅在 backend projection 的 `coverageLimited` 为 true 时显示明显但不遮挡的提示：

- 主文案原样使用 `coverageNotice`。
- 可显示 `{n} coverage limitation codes reported`，具体 codes 放 Inspector 或展开详情。
- 已确认推荐继续保留，不能因为 PARTIAL 隐藏列表。
- FAILED 且仍有 facts 时强调结果只反映 remaining available facts。
- 不生成完整度百分比或预计遗漏数量。

### 4.4 Reading Path List

每个文件卡片 / row 必须包含：

1. 固定宽度的 1-based 序号，例如 `01`、`02`。
2. 文件名：从 `sourceFileId` 最后一个 path segment 确定性展示。
3. 完整 scan-root-relative path，mono，可换行或安全滚动。
4. Module badge；moduleId 缺失或 broken join 时明确 unavailable。
5. 第一条 reason message 作为主要说明。
6. 额外 reason 数量，例如 `+2 source-backed reasons`。
7. 可选、低调的 reason-kind 标签，例如 `ENTRY POINT`、`HTTP ENDPOINTS`、`DEPENDENCY PATH`。

整行使用真实 button 或可访问 list selection 打开右侧 Inspector。Selected 与 focus 必须不同：

- selected：稳定背景 / 边框强调。
- focus-visible：清晰外框。
- hover 不能承载唯一信息。

不要加入 checkbox、完成勾选、drag reorder、星标、评分、阅读时长或代码行数。

### 4.5 Default Limit / Expansion

- 首次显示 projection 提供的默认列表，当前规则最多 10 项。
- 有隐藏项时按钮写真实数量：`Show 4 more recommended files`。
- 展开后保持原序号和 selected item，按钮变为 `Show first 10` 或等价明确文案。
- 不使用无限滚动、分页或重新排名。
- 列表少于 10 项时不显示空的 expand control。

### 4.6 Reading Inspector — Selected File

信息层级：

1. `Reading step 3 of 14`
2. 文件名和完整 source path
3. Module label / unavailable
4. `Why this file?`
5. 全部 reasons
6. `Source evidence`
7. `Related exploration`

每条 reason 展示：

- 后端 message 原文。
- 稳定 kind label。
- `factCount`，使用 `2 supporting facts` 等技术文案。
- dependencyDistance 存在时显示 `Confirmed dependency distance: 2`。
- Evidence 数量。
- 可展开 Evidence details。

不要把 reason 改写为业务语义，例如 `Core order workflow`、`Handles critical logic` 或 `Most important service`。

### 4.7 Source Evidence

每条 Evidence 展示：

- Evidence type
- Rule ID
- Primary location：path + 存在时的 1-based line / column
- Related locations（存在时）

同一文件可能有多条位置；设计应允许逐条浏览，不生成一个虚假的唯一 primary line。

Location 文本保持可选择。不加入：

- 源码片段
- Open in IDE / Reveal / Run
- 绝对路径或 `file://`
- 从 ruleId 推断的注解源码

### 4.8 Related Exploration

只显示 exact supporting IDs 可连接的目标：

- Component：name / qualified name、kind、module，动作 `Explore component`。
- Endpoint：method、resolved path / `Unresolved path`、handler，动作 `Explore endpoint`。

动作效果：

- Component 进入 Architecture，并使目标 Component 可见且选中。
- Endpoint 进入 APIs，并使目标 Endpoint 可见且选中。

若同一推荐项有多个 Endpoint，逐项列出，不随机选择第一条。若没有相关 Component / Endpoint，显示轻量文案：`No component or endpoint navigation target is attached to this recommendation.`，不要显示禁用的假动作。

Root POM 和纯 Entry Point 项没有独立详情页是合法状态。

### 4.9 Nothing Selected Inspector

如果实现不默认选择首项，Inspector 显示：

- `Select a recommended file to inspect all reasons and source evidence.`
- total / visible item count。
- 规则说明：confirmed facts、stable order、no AI score。
- PARTIAL 时保留 coverage notice。

主画板建议默认选择第一项，以便首次进入时立即看到 explanation pattern；这只是 UI selection，不改变 ranking。

## 5. 必须设计的状态

请在同一 Stitch 项目中提供主高保真画面，并在旁边提供关键状态规范或附加画板。

### Ready — API Controller Selected

- Reading path 中选中一个 Controller 文件。
- 主原因是 `Exposes 2 HTTP endpoints`。
- Inspector 显示 Component + 两个 Endpoint exact targets。
- Evidence 展示 Controller / mapping 的真实形状，但明确为 design sample。

### Ready — Confirmed Dependency Neighbor

- 选中 Service 或 Repository 文件。
- 显示 dependency distance、incoming / outgoing confirmed count reason。
- Related Component 可以进入 Architecture。
- 不把组件注入边称为 handler call chain 或业务 flow。

### Root POM / No SourceFile Join

- 文件为 `pom.xml`，模块仍可 exact join。
- 显示 `Root Maven build file` 和 Maven Evidence location。
- 不要求 Java SourceFile metadata，不显示 Open source / IDE。

### Multiple Reasons in One File

- 同一文件包含 Entry Point + Configuration 或 Controller + Endpoint reasons。
- 卡片显示 primary reason + additional count。
- Inspector 展开全部 reasons 和各自 Evidence。
- 不合并成未经后端提供的新业务摘要。

### Default 10 / Expanded

- 默认显示 10 项，按钮显示准确隐藏数量。
- 展开后显示完整序号，selected state 保留。
- 提供收起动作，不重新排序。

### Known Empty

- Start Here projection 可用且 `items[]` 为空。
- 文案：`No recommended files could be produced from the confirmed facts.`
- 若报告 SUCCESS，可说明当前分析未发现 ADR-0013 支持的入口。
- 不插入 design sample row，不建议 AI 猜测。

### PARTIAL with Recommendations

- 顶部显示后端 coverage notice。
- 使用 `reported recommended files`。
- 已确认顺序、原因、Evidence 和导航继续工作。
- limitation codes 可在技术详情中查看。

### FAILED / Empty

- 使用 danger 状态和 remaining facts / failed analysis 文案。
- 不用成功 empty illustration。
- 返回其他页面导航仍可用。

### Guide Unavailable

- `/api/report` 可用但 `/api/start-here` 加载失败。
- 只在 Start Here 主区显示 `Reading guide unavailable` 和具体本地响应错误。
- Overview、Modules、Architecture、APIs 不受影响。
- 不用前端临时重算推荐作 fallback。

### Broken Module / Supporting Entity Join

- 文件、原因和 Evidence 继续显示。
- Module 或 related navigation 单独显示 unavailable。
- 不按 path、qualified name 或 ID 前缀猜替代实体。

## 6. 响应式规则

### ≥ 1280px

- 保持三栏 Shell。
- 中央阅读路径占主要宽度，右侧 Inspector 完整展示全部 reasons、Evidence 和 navigation。
- 长 path、qualified name、ruleId 不使用破坏 identity 的中间省略号。

### 960–1279px

- Inspector 可移到主列表下方，或使用现有紧凑 Inspector 策略。
- 序号、文件名、primary reason 和 module 不能消失。
- Evidence details 可折叠，但 coverage notice 必须保持可见。

### < 960px 与约 500px 小窗口

- 复用顶部横向导航，Start Here active。
- Header、coverage、reading cards、selected detail 按顺序纵向堆叠。
- 每个卡片首行是序号 + 文件名，path 和 reason 后续展示。
- `Show more` 和 Explore actions 使用清晰文本按钮，触控目标足够大。
- path、qualifiedName、ruleId 可换行或字段内安全横向滚动。
- 不依赖 hover、右键菜单或整页横向滚动。

## 7. 可访问性要求

- Reading path 使用有序列表语义；视觉序号与辅助技术顺序一致。
- 每个可选择项使用真实 button 或明确可键盘操作的 list option。
- selected state 用 `aria-current="step"` 或合适的 selection semantics 表达。
- Explore component / endpoint 使用完整可访问名称，包含目标 label。
- Show more 文案包含隐藏数量；展开状态使用 `aria-expanded`。
- Coverage notice 使用 `role="status"`，FAILED error 使用合适的 alert 语义。
- Evidence 使用有标题的列表 / definition list；location、ruleId 可完整读取。
- 状态、reason kind 和导航目标不能只依赖颜色或图标。
- reduced-motion 下不使用路径重排、Inspector 滑入或进度动画。

## 8. Stitch 示例数据约束

可以使用以下纯设计样例：

```text
01 pom.xml
   Root Maven build file

02 src/main/java/fixture/start/StartHereApplication.java
   Application entry point

03 src/main/java/fixture/start/OrderConfiguration.java
   Configuration component

04 src/main/java/fixture/start/OrderController.java
   Exposes 2 HTTP endpoints

05 src/main/java/fixture/start/OrderService.java
   Reached from a higher-priority component at confirmed dependency distance 1
   Used by 1 confirmed component
   Depends on 1 confirmed component

06 src/main/java/fixture/start/OrderRepository.java
   Reached from a higher-priority component at confirmed dependency distance 2
   Used by 1 confirmed component
```

Endpoint sample：

```text
GET  /api/orders       OrderController.list
POST /api/orders       OrderController.create
```

Evidence sample 只包含：

```text
type
ruleId
primary SourceLocation
related SourceLocations
```

约束：

- 所有值必须标注为 design sample，不代表当前用户仓库结果。
- 原因文案尽量使用 T-0803 当前真实 message，不自行增加业务结论。
- 不添加 payment、authentication、critical、core domain 等示例语义。
- 不展示 Java 源码正文、完整方法签名、Git 信息或绝对路径。
- 相关 Endpoint / Component 必须能由 supporting ID exact join 解释。

## 9. 不要设计

- AI-generated overview、chat、问答或智能摘要
- importance / confidence / quality score
- 业务职责、领域标签、owner 或关键程度猜测
- 推荐重排、drag and drop 或个性化 ranking
- checkbox、完成进度、收藏、笔记、分享或账号功能
- reading time、复杂度、代码行数或任务估时
- 文件树、代码编辑器、源码预览或 syntax highlighting
- Open in IDE、Reveal、Edit、Run、Terminal 或 file URI
- Git branch、commit、author、blame 或 dirty status
- Endpoint call chain、runtime flow、sequence diagram
- 新的 Settings / language selector；语言切换另行排期
- 外部 CDN、远程字体、图片或运行时网络资源

## 10. Stitch 输出要求

请输出：

1. 1440 × 900 主高保真页面：API Controller 推荐项 selected。
2. 同一设计系统下的 dependency neighbor、root POM、multiple reasons、default / expanded、known empty、PARTIAL、guide unavailable 与 broken join 状态规范。
3. 约 500px 宽的小窗口 / 窄屏布局。
4. Reading row、序号、module / kind badges、selected / focus、Show more、Coverage notice、Evidence、Related Exploration 和 responsive 说明。
5. 可导入仓库的静态 HTML / CSS 与截图；不依赖外部 CDN、远程字体或运行时网络资源。

Stitch 输出是视觉与交互交接资料，不是生产实现。后续 Codex 必须再次审阅其中的示例值、原因文案、状态、Evidence、导航动作和数据能力，再映射到 Vue、`/api/report` 与新的只读 `/api/start-here` projection。
