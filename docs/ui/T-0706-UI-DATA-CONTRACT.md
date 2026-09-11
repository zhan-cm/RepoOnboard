# T-0706 — Architecture Exploration & Filtering UI Data Contract Audit

> 页面级 design handoff。本文连接 T-0706 TODO、公共 `AnalysisReport`、现有 T-0705 Architecture Workspace、Stitch 设计与后续 Vue 实现，不是长期架构决策；最终实现状态以 `STATE.md` 为准。

## 1. 审计结论

当前 schema `1.2` 已足以实现 T-0706 的模块、组件类型、关系类型、节点搜索、一阶邻域、Reset / Focus / Fit Selection、隐藏数量和可搜索列表，不需要扩展后端公共模型。

T-0706 是现有 Architecture Workspace 的探索能力增强，不是新的架构事实来源。所有结果都必须来自 T-0705 已接受的事实边界：

- 节点只来自 `components[]`。
- 可进入图和邻域计算的边只来自 `dependencies[]` 中 `kind = COMPONENT_INJECTION`、`status = CONFIRMED` 且 source / target 均能连接真实 Component 的关系。
- `MAVEN_DECLARATION`、Endpoint、EntryPoint、Mapper、方法调用、推断 layer 和外部系统都不进入组件图。
- `AMBIGUOUS` 与 `UNRESOLVED` 关系不画成边，也不参与一阶邻域，只保留为 coverage facts、列表提示或 Inspector 内容。
- 搜索和筛选只能缩小已有报告实体，不能按名称、包、路径或组件类型补造关系。
- 图和可搜索列表必须消费同一个 exploration state，选择结果和计数不能互相矛盾。

主要代码依据：

- `frontend/src/lib/reportArchitecture.js`
- `frontend/src/components/ArchitectureWorkspaceView.vue`
- `frontend/src/components/ArchitectureGraph.vue`
- `frontend/src/components/ArchitectureInspector.vue`
- `frontend/src/App.vue`
- `core.model.AnalysisReport`
- `core.model.Component`
- `core.model.ComponentKind`
- `core.model.Dependency`
- `core.model.DependencyKind`
- `core.model.ResolutionStatus`
- `serialization.ReportJsonDocument`

## 2. 前提核对与约束

### 2.1 Relationship Type 当前只有一个真实可选值

TODO 要求 `By Relationship Type`，但 schema `1.2` 中组件图当前只有一种允许展示的关系类型：

```text
COMPONENT_INJECTION + CONFIRMED
```

因此 T-0706 可以建立基于报告 `dependency.kind` 的通用筛选状态，但当前 UI 只能诚实地呈现 `Component injection` 这一项。不得为了让筛选器看起来丰富而加入：

- Maven dependency
- method call
- runtime call
- ambiguous injection
- unresolved injection
- inferred reference

当仅有一个可用关系类型时，设计应使用单选项 / checkbox 或带计数的 filter menu，并明确它是当前报告唯一可绘制的关系类型；不展示无效的多选样例。关闭该项可以得到“只看组件、不画关系”的已知零边状态。

### 2.2 Search 与结构筛选职责不同

搜索默认匹配报告中可确认的 Component 文本字段：

- `label`：由 `qualifiedName` 确定性截取的简短类型名
- `qualifiedName`
- 可选 `name`
- 可选 `location.sourceFileId` 与 `location.symbol`

搜索不匹配推断职责、注解展示文案、Evidence rule ID 或不存在的业务标签。搜索结果用于列表缩小和节点定位；选择结果后可执行 `Focus selection` 或开启一阶邻域。搜索本身不应默默改变关系语义。

### 2.3 大图预算不能通过随机截断解决

现有预算为：

```text
60 component nodes
120 confirmed component-injection edges
```

预算检查发生在模块、组件类型、关系类型和邻域筛选之后。若最终结果仍超预算：

- 图区域不绘制任意前 N 个节点或边。
- 显示最终候选节点 / 边总数与被图暂缓的数量。
- 明确要求继续缩小 module、kind 或 neighborhood scope。
- 可搜索列表仍显示当前筛选结果，并可用于选择一个组件后进入一阶邻域。

这保留“完整事实列表可浏览”和“图不能伪装完整”两个目标。

## 3. 字段审计

| UI Field / State | Semantic Meaning | 来源 | Fact Type | 当前支持 | T-0706 处理规则 |
| --- | --- | --- | --- | --- | --- |
| Module options | 可选择的模块范围 | `modules[].id`、`artifactId` 与组件归属 | Direct / Deterministic Derived | 是 | 单选；切换模块时清理不再存在的 selection 与 neighborhood |
| Component kind options | 当前报告中真实出现的组件角色 | `components[].kind` | Direct Fact | 是 | 多选、同组 OR；只显示实际支持/出现的 kind 与计数，不显示 Mapper |
| Relationship kind options | 可绘制的确认关系类型 | `dependencies[].kind` 经过 T-0705 安全过滤 | Direct Fact | 有限 | 当前只有 `COMPONENT_INJECTION`；筛选结构可扩展，但不补造选项 |
| Node search text | 用户输入的节点查询 | View state | UI State | 是 | 大小写不敏感，trim 后匹配 label / qualifiedName / optional name / sourceFileId / symbol |
| Search result count | 与 query 和当前 module / kind 条件匹配的组件数 | 对 Component 确定性过滤 | Derived UI Fact | 是 | 与列表完全一致；0 表示筛选无结果，不表示仓库无组件 |
| Selected component | 当前选中的 Component | `components[].id` | Direct identity + UI State | 是 | 图和列表共享；模块切换或实体被移出有效范围时清除 |
| Selected relationship | 当前选中的确认 Dependency | `dependencies[].id` | Direct identity + UI State | 是 | 关系类型被关闭或端点被过滤后清除 |
| First-degree neighborhood | 选中节点及其一跳入邻居 / 出邻居 | 允许边的 sourceId / targetId 邻接 | Deterministic Derived Fact | 是 | 只使用 confirmed component-injection edges；保留方向和原始 Evidence |
| Neighborhood mode | 是否把图缩小到选中节点的一阶邻域 | View state | UI State | 是 | 无 component selection 时禁用；edge selection 不自动成为中心 |
| Incoming / outgoing neighbor counts | 当前有效筛选下的一跳关系数量 | 有效确认边按方向计数 | Deterministic Derived Fact | 是 | 明确标为 confirmed incoming / outgoing；不包含 limited relations |
| Focus selection | 将已选节点适配并居中 | Cytoscape presentation API | UI Action | 是 | 只改变 viewport，不改变筛选事实；不可见时说明原因 |
| Fit view | 适配当前已绘制图 | Cytoscape presentation API | UI Action | 是 | 不改变 selection 或 filters |
| Reset | 恢复当前模块的默认 exploration state | View state | UI Action | 是 | 清空 query、kind / relationship narrowing、neighborhood 和 selection；保留当前 module |
| Candidate component count | 当前 module 中进入筛选流水线的组件数 | `components[]` + moduleId | Derived UI Fact | 是 | PARTIAL 时使用 reported 语义 |
| Visible component count | 最终可绘制 / 列表结果中的组件数 | 筛选流水线结果 | Derived UI Fact | 是 | 图未超预算时与图节点一致；列表继续表达完整过滤结果 |
| Filtered component count | 被 kind / neighborhood 排除的组件数 | candidate - filtered | Derived UI Fact | 是 | 不称为 unresolved 或 missing |
| Withheld-from-graph count | 因最终图仍超预算而未绘制的节点数 | over-budget result | Derived UI Fact | 是 | 因为禁止截断，超预算时等于最终候选节点数；文案说明列表仍可浏览 |
| Visible / confirmed edge count | 当前最终节点集合内的允许边数量 | filtered confirmed relations | Derived UI Fact | 是 | 仅当前关系类型开启时大于 0 |
| Ambiguous / unresolved count | 当前 module 源组件上的未确认注入关系数 | `limitedRelations` by sourceId | Derived over Direct Facts | 是 | coverage notice / Inspector；不受 relationship-kind checkbox 伪装为可绘制边 |
| Component list row | 图的语义化替代入口 | 同一 filtered Component 集合 | Direct + Derived presentation | 是 | 始终可搜索、键盘操作；显示 label、kind、module、confirmed in/out |
| Match reason | 搜索为何命中 | 对实际匹配字段的确定性判断 | Derived UI Fact | 是 | 可选显示 `Name` / `Qualified name` / `Declared name` / `Source path` / `Symbol`，不生成自然语言职责 |
| Graph dimming | 选择节点后对非邻域上下文降噪 | Selection + confirmed adjacency | Presentation State | 是 | 默认选择只降噪；Neighborhood mode 才真正缩小集合 |
| Persisted filters | 跨页面/重启保存筛选 | 无公共契约 | Unavailable | 否 | T-0706 不增加持久化、URL 路由或 localStorage 需求 |

## 4. 过滤流水线

为保证图、列表、Inspector 和计数一致，后续实现应集中使用一条纯函数流水线：

```text
AnalysisReport
    ↓ T-0705 safety gate
Real Components + Confirmed Component-Injection Relations
    ↓ Module scope
Module nodes + induced confirmed edges
    ↓ Component kind filter (OR across selected kinds)
Kind-filtered nodes + induced edges
    ↓ Relationship kind filter
Allowed confirmed edges
    ↓ Optional selected-node neighborhood
Selected node + one-hop neighbors + induced allowed edges
    ↓ Readability budget
Renderable graph OR explicit over-budget state
```

搜索与该流水线共享 module / kind 范围，但主要驱动可搜索列表和定位：

```text
Module + kind-filtered nodes
    ↓ Search query
Searchable list results
    ↓ Select a result
Shared selection → Inspector / Focus / optional Neighborhood
```

### 4.1 组件类型组合规则

- 默认选择当前报告中全部可用 component kinds。
- 多个 kind 使用 OR，而不是要求节点同时具有多个 kind。
- 清空全部 kind 是合法筛选状态，结果为 `No components match the current filters`。
- `REST_CONTROLLER` 保留精确类型；视觉上可归入 Controller 家族，但筛选值不能悄悄改写成 `CONTROLLER`。
- Mapper 不存在于当前公共模型，不能显示禁用 Mapper 或 `0 Mapper`。

### 4.2 邻域组合规则

- 只有选中 Component 后才能启用。
- 中心节点始终保留；邻居仅来自当前 module、当前 kind 条件和当前允许关系类型下的一跳 confirmed edges。
- 一跳邻域不递归扩展，不包含二跳节点。
- 当前 kind 过滤会排除中心节点时，清除 selection 和 neighborhood，避免幽灵选择。
- `Focus selection` 仅居中；`Neighborhood` 才缩小结构，两者不能混为同一动作。

### 4.3 搜索反馈规则

未命中时区分：

1. 当前 module 根本不存在匹配 Component。
2. 有文本匹配，但被 component-kind filter 排除。
3. 输入文本更像 unresolved `declaredTarget`，但没有真实 Component 可选。

第三种情况只能提示“没有已解析为组件的匹配节点”；不能把 `declaredTarget` 变成图节点。

## 5. 状态语义

- **Known zero**：集合存在且筛选后为 0，例如某模块确实没有 Service。
- **Filtered empty**：模块有 Component，但当前 filters / query 没有结果；必须提供 Reset / Clear filters。
- **Unavailable**：报告没有 `components[]` 或 `dependencies[]`；筛选器不得制造 0。
- **Over budget**：完整过滤结果存在，但不安全绘图；显示候选与 withheld 数量，列表仍可用。
- **Unresolved / Ambiguous**：报告明确给出的关系解析状态；不参与图、邻域或 confirmed count。
- **PARTIAL**：显示的是 reported facts，不宣称完整覆盖；筛选仍可用。
- **FAILED**：若报告仍含部分实体，可浏览已报告事实并保留失败提示；没有实体时使用失败状态。
- **Graph rendering fallback**：Cytoscape 运行时失败；可搜索列表和 Inspector 保持可用。

## 6. 图与可搜索列表的共同契约

- 使用同一 module、kind、relationship、neighborhood 与 selection state。
- 列表至少展示当前 module / kind 范围内的 Component；query 只缩小列表结果。
- 图未超预算时，列表中的选择应能在图中聚焦同一 ID。
- 图超预算或运行时失败时，列表仍允许选择 Component、查看 Inspector、启用一阶邻域并再次尝试绘图。
- 列表不显示图中不存在的推断 relation；in/out 数量只统计当前允许的 confirmed edges。
- 图和列表都使用稳定 Component ID 作为选择身份，显示文本不是身份键。

## 7. 前端实现影响审计

### 7.1 预计需要修改

- `frontend/src/lib/reportArchitecture.js`
  - 增加 exploration state 纯函数、kind / relationship options、搜索匹配、邻域和预算结果。
- `frontend/src/components/ArchitectureWorkspaceView.vue`
  - 增加组合 toolbar、scope notice、可搜索列表与空筛选状态。
- `frontend/src/components/ArchitectureGraph.vue`
  - 增加 focus selection、选择邻域降噪和当前过滤结果更新。
- `frontend/src/components/ArchitectureInspector.vue`
  - 显示当前筛选下的一阶邻域和 selection 可用动作。
- `frontend/src/App.vue`
  - 管理 Architecture exploration state，或将其封装在 Workspace 内；页面切换时保持当前会话状态。
- `frontend/src/styles/architecture.css`
  - 扩展工具栏、scope notice、list fallback 和响应式规则。
- 对应 Vitest 测试。

### 7.2 不需要修改

- Java analyzers
- Core Model
- JSON schema / serialization DTO
- loopback server
- Maven / Java / Spring 分析边界
- T-0707 API Map

若设计要求上述后端修改，应先回到本审计核对；不能为了视觉样例扩大事实模型。

## 8. T-0706 验收映射

| TODO Acceptance Criterion | 设计 / 实现映射 |
| --- | --- |
| 模块、组件类型和一阶邻域筛选可组合 | 使用固定流水线；模块单选、kind OR、一阶 confirmed 邻域 |
| 超过节点预算时显示隐藏数量并要求缩小范围 | 过滤后再检查预算；不截断图；显示候选 / withheld 并提供可用筛选动作 |
| 始终提供可搜索列表作为图的替代入口 | 主工作区保留可搜索 Component list；图失败、过大和窄屏时仍可完成选择与 Inspector 阅读 |

## 9. 设计交接结论

Stitch 应在现有 T-0705 高保真页面上做增量设计，不重新设计 Shell、主题或架构图事实。主画面应展示一个已选 Service、组合筛选、一阶邻域模式、可见 / 隐藏计数，以及不与右侧 Inspector 争夺空间的可搜索列表入口。

本审计完成后，下一步是用户生成、审阅并导入 T-0706 Stitch 原型。原型导入前不开始 Vue 实现。
