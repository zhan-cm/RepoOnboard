# RepoOnboard 前端产品需求文档

> 面向本地 Web UI 与未来桌面应用的产品体验基线。

## 1. 文档信息

- 产品：RepoOnboard
- 文档类型：前端产品需求文档（PRD）
- 当前产品阶段：M7 — Local Web UI
- 当前实现：产品 Shell、视觉系统、Repository Overview、Module Explorer
- V0.1 交付形态：本地 Web UI + 可执行 JAR + 启动脚本
- 未来候选形态：复用同一套 Web 前端的桌面应用窗口

本文件描述 RepoOnboard 前端“应该是什么样”，用于统一产品、交互、视觉和后续页面开发。它不是实现日志，也不改变 `PROJECT.md`、`DECISIONS.md` 或 `TODO.md` 已确定的范围和任务顺序。

---

## 2. 产品定位

RepoOnboard 是一个本地优先、确定性、可解释的代码库理解与开发者上手工具。它把 Maven、Java、Spring Boot 静态分析结果转换成从整体到细节的项目理解界面，帮助用户快速回答：

- 这是一个什么项目？
- 项目由哪些模块组成？
- 入口、组件和 HTTP API 在哪里？
- 组件之间存在什么已确认的依赖关系？
- 第一次阅读源码应该从哪里开始？
- 某个结论来自哪个文件、符号和位置？

前端不是源码分析器，也不负责猜测业务语义。所有事实必须来自 `AnalysisReport`，或来自对报告实体执行的明确、稳定、可重现的派生规则。

### 2.1 核心价值

> 让第一次接触仓库的开发者，在几分钟内形成可信、可继续深入的代码库心智模型。

### 2.2 体验原则

1. **整体先于细节**：Overview → Module → Component / API → Source Evidence。
2. **证据先于推断**：重要结论能够回到来源位置；不确定性必须显式表达。
3. **清晰先于炫技**：图、动效和装饰不能损害阅读效率。
4. **渐进披露**：默认只展示最重要的信息，通过筛选、选择和 Inspector 逐步深入。
5. **部分成功可用**：PARTIAL 结果仍可浏览，但覆盖限制必须始终可见。
6. **本地与私密**：不要求账号、云服务、API Key 或外部数据库，不上传用户源码。

---

## 3. 产品目标与非目标

### 3.1 前端目标

- 用户在打开 Overview 后 30 秒内理解仓库的基本情况和分析可信度。
- 用户能从模块、架构和 API 三个角度探索同一份结构化报告。
- 用户选择任意重要实体后，能在上下文中看到模块、关系和来源证据。
- 用户面对大项目时能通过筛选、邻域和列表视图避免“图毛线球”。
- Web 页面可以在未来桌面容器中复用，不依赖浏览器地址栏或直接文件系统访问。
- 界面在正常结果、空结果、部分结果、失败结果下都能给出明确下一步。

### 3.2 V0.1 非目标

- 不在前端重新解析源码、POM 或注解。
- 不根据文件名、类名或路径猜测技术版本、业务领域或组件职责。
- 不提供源码编辑、自动修改、重构、Code Review 或 Bug 修复。
- 不提供 LLM 对话、RAG、AI Agent 或不透明的智能评分。
- 不要求登录、团队空间、云同步或遥测。
- 不在 V0.1 引入桌面容器、原生安装器或自带 Java Runtime。
- 不追求完整 IDE 体验，也不展示无界的全仓库调用图。

---

## 4. 目标用户

| 用户类型 | 核心诉求 | 典型任务 | 前端重点 |
| --- | --- | --- | --- |
| 新加入项目的开发者 | 快速建立项目全貌 | 找入口、模块、核心组件和 API | Overview、Start Here、证据跳转 |
| 实习生 / 初级开发者 | 理解框架分层和代码关系 | 看 Controller → Service → Repository | 清晰角色标识、邻域关系、解释文案 |
| 开源贡献者 | 找到适合开始阅读和贡献的位置 | 判断项目结构、定位功能入口 | Module、API Map、Start Here |
| 接手遗留系统的维护者 | 在资料不足时降低理解成本 | 识别模块边界、配置和依赖风险 | PARTIAL 提示、Diagnostics、来源证据 |
| 学生和教学使用者 | 观察真实 Spring Boot 项目结构 | 对照组件、入口、API 与源码 | 层级清楚、术语一致、可追溯 |
| 未来工具集成方 | 消费稳定的结构化分析结果 | 将报告接入其他开发工具 | 稳定信息架构和宿主无关页面 |

### 4.1 用户共同特征

- 熟悉基本开发术语，但不熟悉当前仓库。
- 更关注“先看哪里”和“为什么”，而不是所有细节一次性展开。
- 对错误关系非常敏感；宁可看到“未解析”，也不希望看到伪造关系。
- 通常在桌面显示器上使用，但可能在窄窗口、分屏或远程桌面中查看。

---

## 5. 核心使用场景

### 5.1 首次打开陌生仓库

1. 用户在仓库目录运行 `repoonboard .`。
2. CLI 完成分析并打开本地页面。
3. 用户先查看仓库身份、技术事实、统计和覆盖状态。
4. 用户根据规模与入口判断下一步进入 Modules、Architecture、API Map 或 Start Here。

成功标准：用户不需要先阅读全部 README 和目录，就能描述项目的基本技术轮廓。

### 5.2 从模块理解系统边界

1. 用户进入 Modules。
2. 浏览模块层级、坐标、源码根、框架和模块统计。
3. 选择模块后查看该模块的组件、入口和已确认内部依赖。
4. 从模块继续进入组件或来源证据。

成功标准：用户能区分聚合结构、代码归属和模块之间的确定关系。

### 5.3 从架构关系追踪功能入口

1. 用户进入 Architecture。
2. 默认看到经过模块、角色或节点预算控制的可读视图。
3. 通过搜索或筛选定位 Controller / Service / Repository。
4. 选择节点后在 Inspector 查看一阶邻域、关系类型和来源证据。

成功标准：用户能理解主要组件关系，而不是面对一张不可读的全量图。

### 5.4 查找 HTTP API

1. 用户进入 API Map。
2. 按 method、path 或 module 搜索与过滤。
3. 查看 handler、mapping conditions、路径是否已解析。
4. 在 Inspector 查看类级与方法级 mapping Evidence。

成功标准：用户能快速从 API 跳到对应 Controller、方法和源码位置。

### 5.5 决定阅读顺序

1. 用户进入 Start Here。
2. 查看推荐文件顺序和每项推荐原因。
3. 了解推荐依据是入口、公开 API、配置还是已确认依赖。
4. 逐项查看来源位置或复制路径。

成功标准：推荐具有可解释性，且 PARTIAL 报告会提醒依据可能不完整。

### 5.6 面对不完整或失败分析

1. 页面保持已成功生成的事实可见。
2. 顶部持续显示 PARTIAL / FAILED 状态。
3. 用户可查看限制代码、受影响阶段、文件和诊断信息。
4. 未知内容显示“不可用 / 未解析”，不显示虚假的 0 或推断值。

成功标准：用户清楚“当前知道什么、缺少什么、为什么缺少”。

---

## 6. 整体信息架构

### 6.1 主导航

```text
Overview
Modules
Architecture
API Map
Start Here
```

Diagnostics、Evidence 和 Source Detail 属于跨页面辅助能力，优先通过状态入口、Inspector 或详情面板进入，不必与主要理解路径争夺一级导航。

### 6.2 产品 Shell

```text
┌──────────────┬──────────────────────────────┬──────────────────┐
│ Sidebar      │ Context Header               │ Inspector        │
│              ├──────────────────────────────┤                  │
│ Primary Nav  │ Active Page                  │ Selection        │
│ Local State  │ Summary → Detail             │ Evidence         │
│              │                              │ Related Facts    │
└──────────────┴──────────────────────────────┴──────────────────┘
```

Shell 的固定职责：

- Sidebar：品牌、一级导航、本地运行状态。
- Context Header：当前仓库、当前页面、分析状态、报告 schema 和关键上下文。
- Main Content：页面主要内容、搜索、筛选、列表或图。
- Inspector：当前选择的详情、关系、Evidence 和来源位置。
- Global State：报告加载、连接失败、PARTIAL / FAILED、未知 schema 等全局状态。

---

## 7. 页面列表与优先级

| 页面 / 表面 | 主要问题 | V0.1 | 当前状态 |
| --- | --- | --- | --- |
| Repository Overview | 这是一个什么项目？分析是否可信？ | 必需 | 已实现基础版本 |
| Module Explorer | 项目由哪些模块组成？ | 必需 | 已实现，T-0704 |
| Architecture Workspace | 主要组件如何组织和依赖？ | 必需 | 计划中，T-0705 / T-0706 |
| API Map | 项目暴露哪些 HTTP API？ | 必需 | 计划中，T-0707 |
| Source / Evidence Detail | 这个结论来自哪里？ | 必需 | 计划中，T-0708 |
| Diagnostics / Coverage | 哪些结果不完整或失败？ | 必需的辅助表面 | 分阶段完善，T-0708 / T-0709 |
| Start Here | 第一次阅读应该从哪里开始？ | 必需 | 计划中，M8 |
| Desktop Home / Open Repository | 如何在桌面应用中打开项目？ | V0.2 候选 | 不属于 V0.1 |
| Desktop Settings / About | 如何管理本地偏好和版本？ | V0.2 候选 | 不属于 V0.1 |

---

## 8. 各页面模块结构

### 8.1 Repository Overview

#### 页面目标

让用户在 30 秒内了解仓库规模、技术轮廓、入口和分析覆盖情况。

#### 模块结构

1. **Context Header**
   - 仓库名称。
   - SUCCESS / PARTIAL / FAILED 状态。
   - 报告 schema 与本地会话标识。
   - 仓库名缺失时显示“Repository name unavailable”。

2. **Analysis Coverage Banner**
   - 分析完成、覆盖受限或分析失败。
   - 显著诊断数量。
   - 覆盖限制代码。
   - 不用绿色成功态掩盖 WARNING 或 ERROR。

3. **Project at a Glance**
   - Modules。
   - Source Files。
   - Components。
   - Endpoints。
   - Entry Points。
   - Dependencies。

4. **Technology Facts**
   - Build System。
   - 明确报告的 Languages。
   - 明确报告的 Frameworks。
   - Source Roots 数量和路径摘要。
   - 当前公共报告没有版本事实时，不显示 Java / Spring Boot 版本。

5. **Component Profile**
   - Controllers。
   - Services。
   - Repositories。
   - Configurations。
   - Other Components。

6. **Application Entry Points**
   - 全限定名、类型、模块、来源位置。
   - 没有入口时显示明确空状态。

7. **Warnings and Errors**
   - 默认展示少量关键诊断及总数。
   - 显示 severity、code、stage 和 message。
   - 完整诊断可在后续详情表面查看。

#### 关键交互

- 统计卡未来可进入对应已实现页面；目标页面未完成前不提供无效跳转。
- 入口点可选择并在 Inspector 显示 Evidence；源码导航能力未完成前仅展示文本位置。
- PARTIAL / FAILED 状态应在滚动后仍能通过 Header 或状态入口识别。

---

### 8.2 Module Explorer

#### 页面目标

帮助用户理解 Maven 多模块结构、源码归属和模块内部概况。

#### 模块结构

1. **Module Summary Bar**
   - 模块总数、根模块、含源码模块、Spring Boot 模块。
   - 只展示报告能明确表达的统计。

2. **Module Hierarchy / Module List**
   - 树形结构和可搜索列表。
   - 根模块、子模块、聚合层级清晰区分。
   - 如果公共报告没有显式父子身份，不允许 UI 依据路径自行猜测。

3. **Module Card / Row**
   - artifactId、groupId、version、packaging。
   - Base Directory、POM、Source Roots、Frameworks。
   - 模块内 Source File、Component、Endpoint、Entry Point 统计。
   - 缺失字段单独显示“Not available”，不让整张卡消失。

4. **Module Detail Inspector**
   - 模块完整坐标和来源。
   - 模块内组件角色分布。
   - 明确的内部模块依赖。
   - 相关诊断与 Evidence。

#### 关键交互

- 搜索模块名、坐标或路径。
- 展开 / 收起模块层级。
- 单击选择模块，Inspector 更新但保留列表滚动位置。
- 支持“只看含源码”“只看 Spring Boot 模块”等低复杂度筛选。
- 多模块过多时使用虚拟列表或分段呈现，不一次性展开所有详情。

---

### 8.3 Architecture Workspace

#### 页面目标

用可读、可筛选的视图展示主要组件及已确认依赖关系。

#### 模块结构

1. **Workspace Toolbar**
   - Module 筛选。
   - Component Kind 筛选。
   - Relationship Type 筛选。
   - Node Search。
   - Reset、Focus Selection、Fit View。

2. **Architecture Canvas**
   - Controller、Service、Repository、Component、Configuration 等节点。
   - 有类型、有方向的确定依赖边。
   - 图中不混用 Maven 模块依赖与组件依赖。
   - 默认视图受节点预算约束。

3. **List Fallback**
   - 始终提供可搜索、可键盘操作的组件列表。
   - 图不可用、过大或用户不便操作图时，列表仍能完成核心任务。

4. **Selection Inspector**
   - 组件名称、角色、模块和来源。
   - 入边、出边、一阶邻域。
   - 关系类型、解析状态和 Evidence。

5. **Graph Scope Notice**
   - 当前筛选条件。
   - 已显示 / 已隐藏节点数。
   - 超过预算时明确要求缩小范围。

#### 关键交互

- Zoom、Pan、Fit View、Node / Edge Selection。
- 选择节点后突出一阶邻域，其余内容降噪而不是完全丢失上下文。
- 搜索结果可聚焦节点；找不到时说明是不存在、被筛除还是未解析。
- 图和列表共享同一筛选与选择状态。
- 不画未确认的目标为确定边；未解析关系通过 Inspector 或诊断表达。

---

### 8.4 API Map

#### 页面目标

让用户快速查找项目公开的 Spring MVC HTTP Endpoint，并追溯到 handler 和 mapping Evidence。

#### 模块结构

1. **API Summary**
   - Endpoint 总数。
   - HTTP method 分布。
   - 涉及模块和 Controller 数量。
   - unresolved path 和 conditional mapping 数量。

2. **Search and Filters**
   - 按 path、handler、Controller 搜索。
   - 按 HTTP method、module 过滤。
   - 可切换只看 unresolved / conditional。

3. **Endpoint List / Table**
   - Method、Path、Controller、Handler、Module。
   - `ANY` 不转换成 GET。
   - 未解析 path 显示明确标记，不使用虚构路径。
   - 条件映射以标签或摘要显示。

4. **Endpoint Inspector**
   - 完整 handler 信息。
   - 类级和方法级 mapping Evidence。
   - params、headers、consumes、produces。
   - Source File 和 1-based 位置。

#### 关键交互

- 搜索和筛选即时生效，并显示结果数量。
- 表头在长列表中保持可识别。
- 选择 Endpoint 后可复制 path、handler 和来源位置。
- 空筛选结果与仓库本身没有 Endpoint 使用不同文案。

---

### 8.5 Source / Evidence Detail

#### 页面目标

解释 RepoOnboard 为什么产生某个结论，并给出安全、可操作的来源信息。

#### 模块结构

1. **Entity Identity**
   - 实体类型、名称、模块、框架 / 语言。

2. **Source Location**
   - 扫描根相对路径。
   - 1-based 行列；未知位置不显示 0。
   - Symbol。

3. **Evidence List**
   - Evidence Type。
   - Rule ID。
   - Primary Location 与 Related Locations。

4. **Related Facts**
   - 相关组件、Endpoint、Dependency、Entry Point。
   - 只展示报告中可确认的关联。

5. **Actions**
   - Copy Path。
   - Copy Symbol。
   - Copy Location。
   - 未来桌面宿主可增加“在文件管理器 / IDE 中打开”，但必须通过真实宿主能力提供。

#### 关键交互

- 内容全部按文本安全渲染，不执行项目名、路径或诊断中的 HTML。
- 复制成功提供轻量反馈；剪贴板不可用时提供可选中文本。
- 未知行列、符号或 Related Location 分别显示缺失状态。

---

### 8.6 Diagnostics / Coverage

#### 页面目标

帮助用户判断报告可靠范围，而不是把部分结果伪装成完整分析。

#### 模块结构

1. **Overall Status**
   - SUCCESS、PARTIAL、FAILED。
   - 对应的用户可理解说明。

2. **Diagnostic Summary**
   - ERROR、WARNING、INFO 数量。
   - 按 stage、module、file 聚合。

3. **Diagnostic List**
   - Severity、Code、Stage、Message。
   - Module、File、Source Location（如有）。

4. **Coverage Limitations**
   - 影响哪些计数、页面或来源范围。
   - 如果报告当前只能提供 limitation code，则明确显示 code，不扩写不存在的原因。

#### 关键交互

- Severity、stage、module 筛选。
- 从诊断跳到对应模块或来源详情。
- PARTIAL 状态入口在所有主页面中保持可发现。

---

### 8.7 Start Here

#### 页面目标

给第一次接触仓库的用户一条确定、稳定、可解释的阅读路径。

#### 模块结构

1. **Reading Path Summary**
   - 推荐项总数。
   - 推荐依据说明。
   - PARTIAL 时显示“推荐依据可能不完整”。

2. **Ordered Reading Path**
   - 序号、文件、模块、角色、来源位置。
   - 默认最多显示 10 项，可展开更多。

3. **Why Read This**
   - 应用入口。
   - 配置作用。
   - 公开 Endpoint 数量。
   - 已确认的依赖中心性或邻域。
   - 原因必须能回溯到报告事实。

4. **Reading Item Inspector**
   - 相关 Component / Endpoint / Dependency。
   - Evidence 与来源位置。

#### 关键交互

- 按推荐顺序逐项浏览。
- 选择推荐项后在 Inspector 展示原因和证据。
- 支持复制路径；未来桌面宿主可提供打开文件动作。
- 不允许 AI 式模糊理由或无法解释的分数。

---

## 9. 通用交互需求

### 9.1 导航与上下文保持

- 当前一级导航具有明确 active 状态和 `aria-current`。
- 未实现页面不提供可点击的假入口；可以禁用并标记规划状态。
- 页面切换后保留仓库身份和分析状态。
- 从列表进入详情后返回时，尽量保留搜索条件、筛选条件、滚动位置和选择。
- 页面不能依赖浏览器地址栏完成主要工作流。

### 9.2 选择与 Inspector

- 桌面宽屏中，选择实体后 Inspector 原位更新。
- 取消选择后显示页面相关的引导空状态，而不是空白区域。
- Inspector 内容按 Identity → Summary → Relations → Evidence → Actions 排列。
- Inspector 不应成为唯一的信息入口；键盘和窄屏用户必须能访问同样内容。

### 9.3 搜索与筛选

- 搜索默认匹配名称、全限定名、path 或 handler 等明确字段。
- 筛选条件可组合，结果数量持续可见。
- 提供 Reset / Clear All。
- “没有数据”和“筛选后无结果”必须使用不同状态。
- 搜索不生成新事实，只缩小现有报告实体。

### 9.4 图交互

- 初始视图优先可读，不默认载入无界全量节点。
- 选择节点后突出邻域，边方向和类型清晰。
- 图例、当前范围和隐藏数量始终可找到。
- 所有图功能有列表回退，核心信息不只通过颜色编码。

### 9.5 反馈与动作

- Copy Path / Symbol / Location 使用短暂、非阻塞反馈。
- 失败动作说明原因和回退方法。
- 不使用频繁 Toast 替代页面内状态。
- 危险操作在 V0.1 原则上不存在；RepoOnboard 保持只读。

### 9.6 键盘与无障碍

- 提供 Skip Link，主内容区可被聚焦。
- 所有按钮、导航、筛选和列表选择支持键盘操作。
- `:focus-visible` 清晰且不被截断。
- 文本与背景对比度至少满足 WCAG AA；状态不能只靠颜色区分。
- 动效尊重 `prefers-reduced-motion`。
- 图视图必须有语义化列表替代。
- 动态状态使用合适的 `status`、`alert` 和 `aria-live`，避免重复播报。

---

## 10. 状态设计

### 10.1 加载状态

### 全局报告加载

- Header 显示“Preparing workspace”。
- Main 显示统一 Loading Panel，说明正在连接本地分析服务。
- Inspector 显示“Overview not ready”或对应页面引导。
- 不使用虚假的统计骨架值，避免用户误认为数据已确定。

### 页面内加载

- 当前 V0.1 报告一次性本地加载，通常不需要每个模块独立加载。
- 如果未来出现分片数据，应优先保持已加载内容，只在目标区域显示局部加载。
- 加载动效低调、短促，不能制造云端处理的错觉。

### 10.2 空状态

空状态至少区分以下类型：

| 类型 | 示例 | 推荐文案方向 |
| --- | --- | --- |
| 合法的零结果 | 项目没有 HTTP Endpoint | “No HTTP endpoints were reported.” |
| 字段不可用 | 报告没有 Java Version | “Not available in this report.” |
| 未解析 | Endpoint path 无法确定 | “Path unresolved; review mapping evidence.” |
| 筛选后无结果 | 当前筛选没有匹配组件 | “No components match these filters.” |
| 尚未选择 | Inspector 没有选中实体 | “Select an item to inspect its evidence.” |
| 功能尚未实现 | 后续页面占位 | 禁用导航，不渲染伪页面 |

空状态必须包含：发生了什么、是否影响可信度、用户下一步可以做什么。没有合理下一步时不要添加无效按钮。

### 10.3 错误状态

### 报告连接失败

- 页面显示本地报告不可用和 HTTP / 连接上下文。
- 不清空仍可用的 Shell。
- 如果未来支持重试，重试必须请求同一本地服务，不改变分析输入。

### FAILED 报告

- 明确这是分析失败，不是页面加载失败。
- 仍显示报告中存在的项目身份、诊断和可用部分结果。
- 计数可能不完整时必须说明，不以 0 替代未知。

### 不兼容 schema

- 显示当前 UI 与报告版本不兼容。
- 提示升级或使用匹配版本，不尝试静默猜测字段。

### 局部渲染异常

- 图或单个详情失败时，其他页面和列表回退保持可用。
- 错误信息不泄露本地绝对路径、秘密配置或源码正文。

### 10.4 PARTIAL 状态

- Header、Overview 和相关页面持续展示 PARTIAL。
- 显示 coverage limitation codes 和显著诊断数量。
- 对可能受影响的统计或列表增加轻量提示。
- 不因 PARTIAL 阻止用户浏览已确认的事实。

---

## 11. 视觉风格

### 11.1 风格关键词

```text
简约
企业级
专业
克制
可信
安静
结构清晰
开发者工具感
数据密集但不拥挤
证据驱动
本地与私密
```

### 11.2 反向关键词

```text
营销落地页
赛博朋克
游戏化
高饱和霓虹
大面积玻璃拟态
过度渐变
夸张圆角
无意义动效
装饰性 3D
密集毛线球图
AI 神秘感
```

### 11.3 视觉方向

- **Dark-first**：当前采用深蓝黑画布、深色表面和冷色边框，适合长时间代码工具使用。
- **单一主强调色**：使用低饱和青蓝作为选择、焦点和关键导航色。
- **语义状态色**：绿色只用于确认成功，黄色用于覆盖受限，红色用于失败和终止性错误。
- **低层级阴影**：主要依赖边框、表面明度和间距建立层级，阴影只用于重要浮层或高优先级卡片。
- **适度圆角**：控件和卡片采用中小圆角，避免消费级、玩具化外观。
- **内容主导**：装饰必须让位于名称、类型、数量、关系和证据。
- **未来浅色主题**：可作为后续增强，但不是 V0.1 门槛；不能因此同时维护两套竞争视觉系统。

### 11.4 排版

- UI 主字体使用系统无衬线字体，优先清晰和跨平台稳定。
- 路径、坐标、symbol、schema、method 等代码事实使用等宽字体。
- 页面标题清楚但不过大，避免营销页式巨型标题。
- Eyebrow 用于页面或模块类别，不用于每一个小字段。
- 文案层级建议：Page Title → Section Title → Entity Name → Metadata → Supporting Copy。

### 11.5 密度与间距

- 默认采用中等密度，适合开发工具持续阅读。
- 列表、表格和图工具栏可以更紧凑；Overview 保留更充分的扫读空间。
- 同组字段靠近，不同语义区块使用明显间距或细边框分隔。
- 不通过堆叠大量卡片制造“仪表盘感”；卡片只用于真正独立的信息单元。

### 11.6 图标与图形

- 使用简洁、统一笔画的线性或几何图标。
- 图标必须辅助识别，不替代文字标签。
- Component Kind 使用稳定形状 / 缩写与文字组合，不只靠颜色。
- Architecture 图的颜色数量受控，优先通过边型、标签和分组表达语义。

---

## 12. 响应式与移动端适配

RepoOnboard 的主要使用环境是桌面，但必须支持窄窗口、分屏和移动浏览。移动端目标是“可查看、可搜索、可追溯”，不要求提供与宽屏图工作区完全相同的并排密度。

### 12.1 参考布局区间

| 区间 | 当前参考 | 布局策略 |
| --- | --- | --- |
| Wide Desktop | 大于约 1120px | Sidebar + Main + Inspector 三栏 |
| Compact Desktop / Split View | 约 760–1120px | Sidebar + Main，Inspector 移至内容下方或按需展开 |
| Narrow / Tablet | 小于约 760px | 顶部品牌与横向导航，Main 单列，Inspector 作为内联详情 |
| Small Mobile | 小于约 496px | 单列卡片、纵向状态、紧凑间距，保持最小 320px 可用 |

断点应由内容可用性决定，而不是绑定特定设备型号。

### 12.2 页面适配规则

- Sidebar 在窄屏变为顶部横向可滚动导航，当前项始终可识别。
- Inspector 在窄屏变为页面内详情、折叠区或底部 Sheet；不能永久占据半屏。
- Overview 指标卡从三列降为两列和一列。
- Module Tree 在窄屏优先切换为缩进列表，避免横向树图。
- Architecture 在移动端默认使用组件列表和选中节点邻域；完整画布为次要入口。
- API Table 在窄屏切换为分组列表 / 卡片，Method 和 Path 保持第一视觉优先级。
- 长路径和全限定名允许安全换行或横向滚动，不截断关键尾部且不撑破页面。
- 触控目标至少约 44×44px；Hover 不能是获取信息的唯一方式。
- 软键盘出现时搜索框、清除按钮和当前结果保持可操作。

### 12.3 移动端不优先实现的能力

- 无界的大型架构图编辑式操作。
- 多栏同时比较大量实体。
- 依赖精细 Hover 的关系阅读。

这些能力在移动端应以列表、邻域详情和分步浏览替代。

---

## 13. 当前 Web 端需求

### 13.1 启动方式

- 用户通过 CLI 在目标仓库中启动 RepoOnboard。
- Java 进程完成分析并在系统分配的 `127.0.0.1` 端口提供 UI。
- 默认打开浏览器；`--no-open` 时输出可手动访问的地址。
- 页面关闭不等同于停止分析服务；Ctrl+C 停止服务并释放端口。

### 13.2 Web 端约束

- 静态资源随 JAR 提供，不依赖运行时 CDN。
- 前端只读取受控报告接口，不直接访问本地文件系统。
- 不依赖浏览器扩展、账号或远端服务。
- 未知路径和非只读请求不能成为文件系统入口。
- 项目名、路径、symbol 和诊断一律作为文本渲染。
- 页面刷新后仍能读取当前进程中的同一份报告。

### 13.3 Web 端当前状态

- 已有响应式 App Shell、Sidebar、Context Header、Page Layout、Inspector 和统一状态组件。
- 已有 Repository Overview。
- 已有 Module Explorer，包括显式层级、模块事实、内部模块依赖、搜索/筛选和 Inspector。
- Architecture、API Map、Source Evidence 和 Start Here 仍按 TODO 顺序开发。

### 13.4 后续页面设计交接流程

从 T-0705 起，新的前端页面在编码前必须先完成用户可审阅的 Stitch 设计交接：

```text
TODO / AnalysisReport / 当前实现
        ↓
Codex Data Contract Audit + Stitch 设计方案
        ↓
用户完成 Stitch 高保真设计并导入项目
        ↓
Codex Vue Implementation
        ↓
Browser Validation
```

Codex 提供的 Stitch 方案必须列明页面目标、模块结构、可展示字段、状态、交互、响应式行为以及不可推断的数据；不得为迁就原型制造后端事实。用户完成设计前，不提前实现该页面 UI。

---

## 14. 未来桌面应用窗口需求

桌面应用属于 V0.2 候选，本节只定义产品体验，不决定 Tauri、Electron 或其他容器，也不改变 V0.1 发布门槛。

### 14.1 桌面应用目标

- 降低用户对终端命令和 Java 安装路径的感知成本。
- 提供受控的文件夹选择、最近项目和分析生命周期管理。
- 复用 Web 端主要页面、视觉系统、状态和交互，不形成第二套产品。
- 通过最小宿主适配能力提供“选择目录、复制 / 打开来源、窗口管理”等原生动作。

### 14.2 桌面窗口结构

```text
┌─────────────────────────────────────────────────────────────┐
│ Native Title Bar / Window Controls / Repository Context     │
├──────────────┬──────────────────────────────┬───────────────┤
│ Sidebar      │ Main Workspace               │ Inspector     │
│ Overview     │ Same host-independent pages  │ Evidence      │
│ Modules      │ as Web                       │ Source        │
│ Architecture │                              │ Actions       │
│ APIs         │                              │               │
│ Start Here   │                              │               │
└──────────────┴──────────────────────────────┴───────────────┘
```

### 14.3 桌面专属页面 / 表面

#### Home / Open Repository

- 打开文件夹。
- 最近分析过的仓库。
- 当前支持范围：Java + Maven + Spring Boot。
- 本地处理和隐私说明。
- 无最近项目时提供单一明确主动作，不制造空仪表盘。

#### Analysis Progress

- 当前仓库。
- Detect Maven → Analyze Java → Analyze Spring → Build Report 的阶段进度。
- 当前警告数和可继续状态。
- 允许取消时必须安全释放资源；不展示伪造百分比。

#### Settings / About

- 应用版本和报告 schema 支持范围。
- Java Runtime 状态（如果未来自带 Runtime，则显示实际来源）。
- 主题与基础可访问性偏好。
- 更新通道、日志导出等只有在实现后才显示。
- 不包含账号、云同步或遥测开关，除非未来产品方向正式改变。

### 14.4 桌面窗口行为

- 建议默认窗口约 1280×800，最小可用窗口不高于 1024×640 的需求目标。
- 记住窗口大小和位置时，应避免在显示器变化后恢复到屏幕外。
- 窗口缩窄时遵循 Web 响应式布局，不维护桌面专属页面副本。
- 关闭正在分析的窗口时，明确说明进程是否会停止；不留下孤立后台服务。
- 同一时间是否支持多仓库 / 多窗口，应在真实需求出现后决定，V0.2 初期可保持单窗口单仓库。
- 原生标题栏、拖拽区和系统按钮必须在 Windows、macOS、Linux 保持可用。

### 14.5 最小宿主能力边界

只有真实调用方出现时才建立以下能力：

- `selectRepositoryDirectory`
- `openSourceLocation`
- `revealInFileManager`
- `copyToClipboard`
- `getApplicationVersion`
- `requestWindowClose`

Web 环境必须有清晰回退。例如不能打开 IDE 时，仍可复制相对路径和 symbol。前端业务页面不得直接耦合具体桌面容器 API。

### 14.6 Web 与桌面一致性

| 能力 | Web V0.1 | 未来桌面应用 |
| --- | --- | --- |
| 打开仓库 | CLI 参数 | 原生文件夹选择 + 最近项目 |
| 分析执行 | 当前 Java CLI 进程 | 宿主管理 Java 分析生命周期 |
| Overview / Modules / Architecture / APIs / Start Here | 同一套 Web 页面 | 直接复用 |
| Source action | 复制路径 / symbol | 可增加打开文件或在文件管理器中显示 |
| 服务地址 | loopback URL 可见 | 用户无需感知地址 |
| 关闭行为 | Ctrl+C 停止 | 窗口关闭与分析进程协调 |
| 更新与安装 | JAR / 启动脚本 | V0.2 技术试验后决定 |

---

## 15. 内容与文案规则

- 使用直接、事实型语言，避免“智能理解”“魔法分析”等营销表达。
- 技术名词保留业界通用写法，例如 Maven、Spring Boot、HTTP、Controller。
- SUCCESS 表示没有已报告的覆盖限制，不承诺静态分析等同运行时真相。
- PARTIAL 使用“已保留可确认结果”的正向说明，同时明确限制。
- FAILED 区分“报告分析失败”和“页面连接失败”。
- 0、Unknown、Unavailable、Unresolved 含义不同，不得互换：
  - `0`：报告明确确认数量为零。
  - `Unknown / Not available`：报告没有该事实。
  - `Unresolved`：发现声明但无法可靠解析结果。
- 不把内部异常堆栈直接展示给普通用户；保留可操作的 code、stage、file 和 message。

---

## 16. 数据真实性与安全要求

- 所有用户可见事实必须映射到 `AnalysisReport` 字段或有测试的纯派生函数。
- 派生统计必须稳定、确定，不依赖遍历顺序、随机数或浏览器 locale。
- 报告没有 Java / Spring Boot Version 时，页面不得通过依赖名、路径或字符串猜测。
- 未解析依赖不能画成确定关系。
- 绝对本地路径默认不进入公共报告和页面；优先使用扫描根相对路径。
- 不渲染项目提供的 HTML；文本、路径、诊断和 symbol 必须转义。
- 不向远端发送报告、源码、路径或使用数据。
- 未经产品方向变更，不加入隐藏遥测。

---

## 17. 性能与大数据量体验

- 报告加载后应尽快显示 Shell 和状态，避免整页空白。
- 大列表使用分页、窗口化或渐进展示；优先保留搜索和筛选可用性。
- Architecture 设置节点预算，默认只展示模块、类型筛选或选中节点邻域。
- Diagnostics 默认摘要和前若干项，完整列表按需查看。
- 搜索输入避免每次按键触发高成本全图重排。
- 解析和聚合相同报告的数据应集中处理，避免每个组件重复扫描全部实体。
- 性能优化以真实中大型仓库测量为依据，不提前引入复杂缓存系统。

---

## 18. 验收维度

每个前端页面完成时至少验证：

1. **事实正确**：所有字段有报告来源或明确派生规则。
2. **状态完整**：SUCCESS、PARTIAL、FAILED、Loading、Empty、Unavailable 均有对应行为。
3. **可追溯**：重要实体能展示 Evidence 或明确说明当前尚无 Evidence。
4. **可访问**：键盘、焦点、语义、对比度和 reduced motion 通过验证。
5. **响应式**：宽屏、1024 级桌面窗口、760 以下窄屏和约 500px 小窗口可用。
6. **大数据量可控**：列表、图和诊断不会一次性无界渲染。
7. **安全**：恶意名称、路径、symbol、诊断不会作为可执行内容。
8. **宿主无关**：主要页面不依赖浏览器地址栏、直接文件系统或桌面容器 API。
9. **离线可用**：生产资源随 JAR 提供，不使用运行时 CDN。
10. **回退明确**：图、剪贴板或宿主动作不可用时仍有可完成任务的替代路径。

---

## 19. 版本范围建议

### V0.1 Web-first

- Repository Overview。
- Module Explorer。
- Architecture Workspace + Filtering + List Fallback。
- API Map。
- Source / Evidence Detail。
- Diagnostics / Coverage 可发现性。
- Explainable Start Here。
- Loopback 安全、离线 JAR 和响应式验证。

### V0.2 候选

- Desktop Home 与原生文件夹选择。
- 最近项目。
- 桌面分析生命周期和窗口关闭协调。
- 打开源码位置的宿主动作。
- 安装器、自带 Runtime 和更新机制的技术试验。

### 更长期候选

- 第二语言 / 框架生态。
- 基于已验证结构化结果的可解释 AI 辅助。
- 更深入但仍可追溯的业务流理解。

---

## 20. 产品风格总结

RepoOnboard 的前端应该像一件可靠的工程工具：

> 打开后先给出可信全貌，需要时再逐层深入；界面安静、专业、克制，每一个数字和关系都有来源，每一个缺失和失败都有清楚解释。

它不应该像营销网站，也不应该用复杂动画或巨大图表掩盖分析的不确定性。Web 端和未来桌面端应共享同一套信息架构、视觉系统与页面体验；桌面应用只增加必要的本地宿主能力，不重做产品核心。
