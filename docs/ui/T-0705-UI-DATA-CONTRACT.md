# T-0705 — Architecture Workspace UI Data Contract Audit

> 页面级 design handoff。本文连接 T-0705 TODO、公共 `AnalysisReport`、Stitch 设计与后续 Vue 实现，不是长期架构决策；最终实现状态以 `STATE.md` 为准。

## 1. 审计结论

当前 schema `1.2` 已足以实现 T-0705 的最小 Architecture Workspace，不需要为了页面视觉补充后端公共模型。

核心契约是：

- 节点只能来自 `components[]`。
- 图中可确认的边只能来自 `dependencies[]` 中 `kind = COMPONENT_INJECTION`、`status = CONFIRMED` 且 `targetId` 存在的关系。
- `MAVEN_DECLARATION` 属于模块图，不得与组件图混画。
- `AMBIGUOUS`、`UNRESOLVED` 关系不画成确定边，只能在 Inspector、诊断或覆盖提示中表达。
- 模块名称通过 `component.moduleId` 与 `modules[].id` 的确定性连接获得；不能从包名、路径或类名推断架构分层。
- T-0705 只实现可读的基础图、选择、Inspector、Source Evidence 和基本视图控制。组合筛选、节点搜索、一阶邻域探索和可搜索列表属于 T-0706。

主要代码依据：

- `core.model.AnalysisReport`
- `core.model.Component`
- `core.model.ComponentKind`
- `core.model.Dependency`
- `core.model.DependencyKind`
- `core.model.ResolutionStatus`
- `core.model.Evidence`
- `core.model.SourceLocation`
- `serialization.ReportJsonDocument`
- `frontend/src/App.vue`

## 2. 字段审计

| UI Field | Semantic Meaning | Level | AnalysisReport / Public Model 来源 | Fact Type | 当前支持 | Evidence / SourceLocation | 处理意见 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Component node ID | 图节点稳定身份 | Component | `components[].id` | Direct Fact | 是 | Component 自身有 location / evidence | 仅用于身份与选择，不作为用户主标签 |
| Component display label | 组件的人类可读标签 | Component | 优先 `components[].qualifiedName` 的最后一个类型片段；不能可靠分割时展示完整 qualifiedName | Deterministic Derived Fact | 是 | 继承 Component 的证据 | 类名缩写只是显示变换，不推断职责 |
| Qualified name | Java 全限定类型名 | Component | `components[].qualifiedName` | Direct Fact | 是 | Component location / evidence | 放 Inspector；节点空间足够时可作次级文本 |
| Declared component name | 框架报告的可选组件名 | Component | `components[].name` | Direct Fact | 是，可为空 | Component evidence，不一定是 name 字段级证据 | 只在存在时放 Inspector；为空显示 Unknown，不从类名生成 bean name |
| Component kind | 已确认的组件角色 | Component | `components[].kind` | Direct Fact | 是 | Component evidence | 直接支持 Controller、REST Controller、Service、Repository、Component、Configuration |
| Mapper | Mapper 组件角色 | Component | 公共 `ComponentKind` 无 `MAPPER`；T-0404 仍可选延后 | Unavailable | 否 | 无 | T-0705 核心不依赖；从 Stitch 删除，不补模型、不显示 0 Mapper |
| Framework | 组件所属框架 | Component | `components[].framework` | Direct Fact | 是 | Component evidence | Inspector 次级字段；V0.1 当前通常为 Spring Boot |
| Owning module | 组件所属模块 | Component / Module | `component.moduleId` 与 `modules[].id` 等值连接 | Deterministic Derived Fact | 是 | Component location；Module 有 POM evidence | 可用于节点次级标签和 Inspector，不以包名推断模块 |
| Component source location | 组件声明文件、行列和 symbol | Component | `components[].location` | Direct Fact | 是 | 即 SourceLocation | Inspector 的 Source Evidence 主入口；未知位置不显示 0 |
| Component evidence | 组件为何被识别 | Component | `components[].evidence[]` | Direct Fact | 是 | `type`、`ruleId`、location、relatedLocations | 适合 Inspector；不要把 ruleId 伪装成自然语言结论 |
| Directed component edge | 已确认组件 A 注入组件 B | Relation | `dependencies[]` 过滤 `COMPONENT_INJECTION + CONFIRMED + targetId`；sourceId / targetId 均匹配 Component ID | Deterministic Derived Fact over direct relation | 是 | Dependency location / evidence | 画有方向的确定边；方向是 source → target |
| Edge kind | 关系种类 | Relation | `dependency.kind` | Direct Fact | 是 | Dependency evidence | T-0705 图内只允许 `COMPONENT_INJECTION` |
| Edge resolution status | 关系解析状态 | Relation | `dependency.status` | Direct Fact | 是 | Dependency location / evidence | 已绘制边为 CONFIRMED；完整状态放 Inspector / coverage |
| Declared target | 源码中声明的目标类型文本 | Relation | `dependency.declaredTarget` | Direct Fact | 是 | Dependency location / evidence | 放 Edge Inspector；不能拿它在 UI 中二次猜目标 |
| Incoming count | 指向选中组件的已确认组件边数量 | Component | 对允许绘制的边按 `targetId` 计数 | Deterministic Derived Fact | 是 | 每条计数边有 evidence | Inspector 摘要，可明确标注 Confirmed incoming |
| Outgoing count | 从选中组件发出的已确认组件边数量 | Component | 对允许绘制的边按 `sourceId` 计数 | Deterministic Derived Fact | 是 | 每条计数边有 evidence | Inspector 摘要，可明确标注 Confirmed outgoing |
| First-degree neighbors | 与选中组件直接相连的组件集合 | Component | 对允许绘制边的 sourceId / targetId 邻接计算 | Deterministic Derived Fact | 是 | 每条邻接边有 evidence | 数据支持，但交互式邻域筛选属于 T-0706；T-0705 不将其做成筛选模式 |
| Component totals | 报告中的组件总数及角色分布 | Repository / Scope | `summary.componentCount`、角色计数或对 `components[]` 确定性计数 | Direct summary / Deterministic Derived Fact | 是 | 各组件有 evidence；summary 本身无独立 evidence | 可用于页面概览和图范围提示；PARTIAL 时标注 reported |
| Confirmed edge count | 当前范围内可绘制的确认边数量 | Repository / Scope | 对允许绘制的 dependencies 计数 | Deterministic Derived Fact | 是 | 每条边有 evidence | 可在工具栏/范围提示中展示 |
| Ambiguous / unresolved count | 当前报告内未能确定目标的组件注入关系数量 | Repository / Scope | 按 `COMPONENT_INJECTION` 与 status 计数 | Deterministic Derived Fact | 是 | Dependency location / evidence | 只做覆盖提示或 Inspector，不画成确定边 |
| Module scope | 当前图限定到的模块 | View state | 模块列表 + component.moduleId | Deterministic UI state | 是 | 继承模块和组件事实 | 为控制图规模所必需；T-0705 只允许单一模块范围，不实现 T-0706 的组合筛选 |
| Cross-module component edge | 两端组件 moduleId 不同的确认注入关系 | Relation | 已确认边两端 Component 的 moduleId 比较 | Deterministic Derived Fact | 是 | 关系与两端组件均有 evidence | 在 repository 范围可显示；单模块范围下按范围规则处理，不转成模块边 |
| Maven module edge | POM 中模块依赖 | Module relation | `dependencies[].kind = MAVEN_DECLARATION` | Direct Fact | 有数据但不属于本图 | POM location / evidence | 属于 Module Explorer；T-0705 不显示 |
| Analysis status | 整次分析 SUCCESS / PARTIAL / FAILED | Repository | `AnalysisReport.status`、`summary.coverageLimited` | Direct Fact | 是 | 由 diagnostics 支撑 | 放 Shell / Graph scope notice；不能生成组件级“健康度” |
| Diagnostics | 警告、错误及覆盖限制 | Repository，部分关联 Module / File | `diagnostics[]` | Direct Fact | 是 | location 可选 | 显示相关摘要；无 moduleId 的诊断保持 repository-level |
| Graph coordinates / layout | 节点在画布中的位置 | View state | 前端图布局算法 | Deterministic presentation state | 后端无需支持 | 无 | 不进入公共模型，不作为分析事实持久化 |
| Node budget / shown / hidden | 当前视图为可读性限制展示多少节点 | View state | `components[]` 与已选择 scope、前端预算规则 | Deterministic Derived UI state | 是 | 无独立 source evidence | T-0705 必须防止无界绘制；具体阈值是可测试实现参数，不写入报告 |
| Routes / endpoints | HTTP mapping 与 handler | Endpoint | `endpoints[]` | Direct Fact | 是 | Endpoint evidence / location | 属于 T-0707，不进入 T-0705 图 |
| Entry points | 应用启动入口 | EntryPoint | `entryPoints[]` | Direct Fact | 是 | EntryPoint evidence / location | 不是 Component 图节点；T-0705 不显示，除非未来另有明确模型关系 |
| Method calls | 方法级调用关系 | Method relation | 公共报告无此模型 | Unavailable | 否 | 无 | V0.1 非目标；删除 |
| Architectural layer | UI / domain / infrastructure 等分层 | Inferred grouping | 公共报告无此模型 | Speculative | 否 | 无 | 不按包名、路径或组件名称推断；删除 |
| Importance / centrality score | 节点重要性或排序分 | Derived ranking | 公共报告无正式规则 | Unavailable | 否 | 无 | 不属于 T-0705；删除 |
| External system / database nodes | 数据库、消息队列、外部 API 等 | External entity | 公共报告无此模型 | Unavailable | 否 | 无 | 删除，不以依赖名或类名推断 |

## 3. 状态语义

- **0**：相关数组真实存在，按确定规则计数后结果为零，例如当前 scope 的 confirmed edge count 为 0。
- **Unknown**：公共字段存在，但某一实体没有值，例如 Component 的可选 `name` 为空。
- **Unavailable**：当前 schema 没有该概念，例如 Mapper、方法调用、架构分层。
- **Unresolved**：仅当 `dependency.status = UNRESOLVED` 或报告有对应显式诊断时使用。
- **Ambiguous**：仅当 `dependency.status = AMBIGUOUS` 时使用；不能自行挑选一个目标。
- **PARTIAL**：报告保留了可确认事实，但覆盖受限。计数应表述为 reported，不得宣称完整。
- 图中“无边”不等于“无依赖”：应区分真实 0 个组件关系、只有 unresolved / ambiguous 关系、以及 PARTIAL 覆盖。

## 4. 字段分类

### A — 应直接展示

- Component 节点：显示标签、kind、所属 module
- 仅确认的 `COMPONENT_INJECTION` 有向边
- 当前 scope 的 reported component count 与 confirmed edge count
- Repository analysis status / coverage notice
- 基础图视图：zoom、pan、fit、node / edge selection

### B — 有数据，但适合 Inspector / 次级区域

- qualifiedName、可选 component name、framework
- 完整 module 标识
- Component / Dependency SourceLocation 与 Evidence
- declaredTarget、edge status
- confirmed incoming / outgoing count
- ambiguous / unresolved 关系数量与相关 diagnostics
- first-degree neighbors 的只读摘要（不作为 T-0705 筛选器）

### C — T-0705 需要，但当前数据契约缺失

- **无。**

T-0705 的核心节点、确认边、模块归属、状态与证据均已存在。图布局、节点预算和选择状态是前端表现规则，不应补进公共报告。

### D — 不应该出现在 T-0705

- Mapper 节点或 Mapper 计数（T-0404 未实现）
- Maven module dependency 与 Component dependency 混图
- Routes、Endpoint、EntryPoint 节点
- method call graph
- 根据包名、路径、坐标、类名推断的 layer、依赖或职责
- importance / health / quality score
- external system、database、queue 等推测节点
- 将 AMBIGUOUS / UNRESOLVED 关系画成确定边
- T-0706 的组合筛选、节点搜索、邻域筛选、Focus Selection、可搜索列表

## 5. T-0705 / T-0706 边界

| 能力 | T-0705 | T-0706 |
| --- | --- | --- |
| 基础 Component graph | 实现 | 增强 |
| 单一 module scope | 作为防止无界图的最小范围控制 | 与类型、关系、搜索组合 |
| Zoom / Pan / Fit View | 实现 | 保留 |
| Node / Edge selection | 实现 | 增强为聚焦与邻域探索 |
| Inspector / Source Evidence | 实现 | 与筛选、邻域联动 |
| Component kind filter | 不实现 | 实现 |
| Relationship type filter | 不实现；当前图只有一种边 | 实现扩展规则 |
| Node Search | 不实现 | 实现 |
| Selected-node Neighborhood mode | 不实现 | 实现 |
| Searchable list fallback | 不实现复杂版本；可保留非搜索的安全回退状态 | 实现完整可搜索列表 |

## 6. 设计交接结论

Stitch 原型必须围绕“一个有范围约束、只展示已确认组件注入关系的工作台”设计。原型可以使用示例内容表达布局，但所有示例值都必须视为 design sample，而不是仓库事实；Vue 实现时只能绑定上述 A / B 类数据。
