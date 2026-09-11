# T-0706 — Architecture Exploration & Filtering Stitch Design Brief

> 供 Stitch 在已导入的 T-0705 Architecture Workspace 基础上生成增量高保真设计。数据边界以 `T-0706-UI-DATA-CONTRACT.md` 为准；本轮只设计 T-0706，不包含 API Map、Source Navigation 或新的分析事实。

## 1. 页面目标

让用户在中大型 Spring Boot 模块中快速回答：

1. 如何按模块和精确组件类型缩小架构范围？
2. 如何搜索并定位某个真实 Component？
3. 选中组件的一阶 confirmed dependency neighborhood 是什么？
4. 当前图显示、过滤或因预算暂缓了多少节点？
5. 图过大、不可用或不适合当前窗口时，如何通过列表完成同一探索任务？

这是 T-0705 的增量探索层。页面仍是只读、确定性、evidence-first 的代码库理解工具，不是图编辑器、调用跟踪器或架构评分仪表盘。

## 2. 必须继承 T-0705 的现状

直接以已导入的 T-0705 高保真 Architecture Workspace 为设计基线：

- 保留现有 Sidebar、Repository Context、Architecture Header、中央图画布和右侧 Inspector。
- 保留当前浅色、高密度、企业技术工作台风格和现有尺寸节奏。
- 保留节点类型视觉、确认有向边、selection highlight、Evidence 层级和底部 status bar。
- 保留 `Architecture` active navigation；API Map 与 Start Here 仍不可用。
- 不重新切换主题，不引入与当前实现竞争的 design system。
- 不增加第二个左侧树状导航或永久第四栏。

新增能力集中在：可组合 toolbar、graph scope notice、selection neighborhood，以及中央工作区内的可搜索 Component list。

## 3. 事实边界

设计中只能出现：

- Component nodes：Controller、REST Controller、Service、Repository、Component、Configuration。
- Confirmed directed relationship：`Component injection`。
- Module、Component kind、qualified name、optional declared component name。
- Confirmed incoming / outgoing relation counts。
- SourceLocation、Evidence、analysis status、ambiguous / unresolved coverage counts。

不得出现：

- Mapper 或 `0 Mappers`
- Maven module edges
- Endpoint / EntryPoint nodes
- method calls、runtime calls、event flows
- inferred layers、domains、responsibilities
- database、queue、external service guesses
- health、importance、risk 或 centrality scores
- ambiguous / unresolved dashed edges

## 4. 关键前提：Relationship Type 只有一个真实选项

当前组件图可用的关系类型只有：

```text
Component injection · Confirmed
```

请设计一个可扩展但诚实的 Relationship filter：

- 当前只显示一个带计数的 `Component injection` 选项。
- 不要添加 Maven dependency、Method call、Runtime call 或 Unresolved 等假选项。
- 关闭唯一选项时，组件仍可显示，关系数变为已知 0，并显示 `No relationship types selected`。
- `Confirmed` 是事实安全门，不是允许切换到 Ambiguous / Unresolved 的状态筛选。

## 5. 1440 × 900 主画面

主画面建议表现：

- Module：`spring-petclinic-api-gateway`。
- Component kinds：Controller、Service、Repository 已选；Configuration / Component 未选。
- Search：空或显示轻量 placeholder，不遮挡主要筛选状态。
- Selected node：`CustomersServiceClient`（Service）。
- Neighborhood：开启 `1 hop`。
- Scope notice：例如 `4 of 8 reported components shown · 3 confirmed relations · 4 filtered`。
- 图中选中节点及一阶邻域保持高对比，其余被过滤的节点不画入当前 neighborhood 图。
- Inspector 保留真实字段风格，新增当前 confirmed incoming / outgoing neighbors 与 `Focus selection` 上下文。

示意结构：

```text
┌──────────────┬──────────────────────────────────────────────┬──────────────────┐
│ Product Nav  │ Repository Context                           │ Inspector        │
│              ├──────────────────────────────────────────────┤                  │
│ Overview     │ Architecture Header + Legend                 │ Selected node    │
│ Modules      ├──────────────────────────────────────────────┤ Neighbors        │
│ Architecture│ Module | Kinds | Relationship | Search        │ Source evidence  │
│ APIs         │ Reset | Neighborhood | Focus | Fit            │                  │
│ Start Here   ├──────────────────────────────────────────────┤                  │
│              │ Scope: shown / filtered / withheld           │                  │
│              ├──────────────────────────────────────────────┤                  │
│              │ Architecture Canvas                          │                  │
│              ├──────────────────────────────────────────────┤                  │
│              │ Searchable Components tray                   │                  │
└──────────────┴──────────────────────────────────────────────┴──────────────────┘
```

可搜索列表使用中央工作区的底部 tray / results region，不增加永久第四栏。主画面中列表应展开到足以证明它是可用入口，但不要压缩画布到无法阅读。

## 6. Exploration Toolbar

工具栏按任务顺序组织，避免把所有控件挤成一行：

### 第一行 — Scope and filters

- `Module` 单选 select，显示模块名和 reported component count。
- `Component kinds` compact multi-select / filter popover，trigger 显示已选数量，例如 `3 of 5 kinds`。
- `Relationship` compact filter，当前显示 `Component injection (18)` 单一真实选项。
- `Search components`，匹配 label、qualified name、optional declared name、source path 和 symbol。

### 第二行 — View actions

- `Reset filters`
- `1-hop neighborhood` toggle；没有选中 Component 时 disabled 并给出短说明。
- `Focus selection`；没有可见 selection 或图不可用时 disabled。
- `Fit view`

动作语义必须清楚：

- Focus 只移动 viewport，不改变筛选。
- Neighborhood 才缩小到一阶关系。
- Reset 保留当前 Module，但清空其他 filters、query、selection 和 neighborhood。

不要增加 Apply 按钮；本地报告上的筛选即时生效。动态结果数量使用克制的 `aria-live` 状态，不频繁弹 Toast。

## 7. Component Kind Filter

显示报告中实际出现的精确类型和计数，例如：

```text
REST Controller  2
Controller       1
Service          5
Repository       4
Component        3
Configuration    2
```

- 允许多选，多个 kind 是 OR。
- REST Controller 与 Controller 可以共享色彩家族，但必须保留两个精确选项。
- 提供 `All kinds` / `Clear`，但不显示 Mapper。
- kind 全部清空时使用筛选空状态，不假装模块没有组件。

## 8. Searchable Component List

列表是图的同等事实入口，不是错误页专用 fallback。

宽屏建议使用中央画布下方的可展开 results tray：

- Header：`Components`、当前结果数、展开 / 收起动作。
- 展开时保留 search input，或明确复用 toolbar query。
- 每行显示：短名称、精确 kind、module、confirmed incoming / outgoing count。
- 当前 selection 使用与图相同的 selected state。
- 点击 / Enter 选择后更新图和 Inspector；提供 `Focus` 或 `Show 1-hop` 的明确动作。
- 长 qualified name 可作为次级行或可访问 title，不挤压主标签。

列表必须在以下状态仍可用：

- 图超预算
- Cytoscape 渲染失败
- 没有 confirmed edges
- 窄屏 / 约 500px 桌面窗口
- 用户无法通过指针精确操作图

## 9. Graph Scope Notice

始终展示当前范围，至少包含：

- 当前 module
- shown components
- filtered components
- confirmed relations
- neighborhood 状态（若开启）
- analysis coverage（PARTIAL / FAILED 时）

正常示例：

```text
4 of 8 reported components shown · 4 filtered · 3 confirmed relations · 1-hop neighborhood
```

超预算示例：

```text
Graph paused for readability
84 components and 163 confirmed relations match the current filters. 84 components are withheld from the graph. Narrow the module, component kinds, or select a component from the list and show its 1-hop neighborhood.
```

由于不能画随机截断子图，超预算状态不要显示“shown 60 / hidden 24”的残缺画布。图应整体暂停，列表保留全部 84 个筛选结果。

## 10. Selection and Neighborhood

### Node selected, neighborhood off

- 保留完整的当前可绘制图。
- 选中节点和 direct confirmed edges 高亮。
- 其他节点降低对比但保持可读上下文。
- Inspector 显示 confirmed incoming / outgoing neighbors。

### Node selected, 1-hop neighborhood on

- 只显示中心节点和当前过滤条件下的一跳 confirmed neighbors。
- 使用 direction markers 区分 incoming / outgoing。
- 显示 `Center + N neighbors`。
- 不自动加入被 kind filter 排除的节点。
- 不递归展示二跳关系。

### Edge selected

- 保留 T-0705 Edge Inspector。
- Neighborhood toggle disabled 或保持上次 component-centered 状态；不要把 edge 猜成中心节点。

## 11. 必须设计的状态

在同一 Stitch 项目中至少提供主画面和以下状态规范：

### A. Ready — Combined filters + selected 1-hop neighborhood

本轮主高保真画面。证明 Module、kind 和 neighborhood 可组合，图 / 列表 / Inspector selection 一致。

### B. Over budget after filters

- 不画截断图。
- 显示 matching node / edge count 和 withheld-from-graph count。
- Module / kind filters 保持可用。
- 可搜索列表完整可用，并引导选择节点后显示 1-hop。

### C. Filtered empty

- 文案：`No components match the current filters.`
- 提供 `Reset filters`。
- 区分于 `No components reported for this module.`

### D. Search no match

- 文案：`No reported components match “…” in this scope.`
- 若可能被 kind filter 排除，提示清除 kind filter。
- 不将 unresolved declaredTarget 变成节点。

### E. No relationship types selected

- 组件和列表仍显示。
- 图不画边，confirmed relation count 为 0 under current filter。
- 明确说明这是筛选结果，不是分析结论。

### F. Components without confirmed edges

- 组件可选择和检查。
- 文案：`No confirmed component relationships in this scope.`
- 不当作错误。

### G. PARTIAL / ambiguous / unresolved

- 图仍只画 confirmed edges。
- coverage notice 显示 reported / limited 语义和明确计数。
- 不画虚线未知边。

### H. Graph runtime fallback

- 筛选与可搜索列表完整保留。
- Inspector 可用。
- 提供重试图或继续列表阅读的明确路径。

### I. Loading / report error / failed analysis

- 复用已有 Shell 和 StatePanel。
- 不用模拟 skeleton nodes 伪造结构。

## 12. 响应式要求

### ≥ 1280px

- 保留三栏 Shell。
- toolbar 可使用两行紧凑布局。
- 图是主区域，Component list 作为下方可展开 tray。
- Inspector 固定在右侧。

### 960–1279px

- filters 可折叠进一个 `Filters` popover / drawer，但 active filter chips 和结果数必须可见。
- Inspector 延续现有下移或 drawer 行为。
- list tray 可以占主区域下半部，不把图高度压到不可用。

### < 960px 与约 500px 桌面窗口

- 默认 list-first；图作为可选次级视图。
- Module 与 search 保持直接可见，其余 filters 进入清晰的 filter sheet。
- selection detail 在列表下方或 Inspector drawer 展示。
- 不依赖 hover、右键或精确拖拽。
- 路径、qualifiedName 和 symbol 安全换行。

## 13. 可访问性

- 所有 select、multi-select、toggle、search、Reset、Focus、Fit 和 list row 可用键盘操作。
- Filter popover 使用真实 label、checkbox 和可恢复焦点，不用不可访问的自绘下拉。
- List row 通过 Enter / Space 选择；Escape 清除 selection 或关闭 popover。
- 图不是唯一信息载体；列表和 Inspector 提供等价身份、关系摘要和 Evidence 入口。
- 颜色不是 kind、selection、direction、warning 的唯一表达。
- 动态数量使用 `aria-live="polite"`，避免每次按键重复播报整页。
- 遵守 `prefers-reduced-motion`；Focus / Fit 不需要动画。

## 14. Stitch 示例数据约束

可沿用 T-0705 的通用示例名称：

```text
BoundaryApiController
CustomersServiceClient
CustomerRepository
SecurityConfiguration
PaymentClientComponent
```

只允许根据示例中明确列出的 confirmed injection edges 连线。所有数字、路径、模块名和 Evidence 都是 design sample，Vue 实现时不能硬编码。

不要使用真实产品之外的顶部搜索、Re-analyze、Export、Settings、Open in IDE 或 Copy 等动作来暗示 T-0706 已包含这些功能。现有实现没有这些宿主或文件操作能力。

## 15. 可直接粘贴给 Stitch 的主提示词

```text
Extend the existing imported RepoOnboard T-0705 Architecture Workspace into the T-0706 Architecture Exploration & Filtering screen. Treat the imported T-0705 screen as the visual source of truth: preserve its light, compact, enterprise technical workbench, left product navigation, repository context, central architecture canvas, right Evidence Inspector, graph node styling, directed confirmed edges, and status bar. This is an incremental design, not a redesign.

RepoOnboard is a local-first, deterministic tool for understanding unfamiliar Java, Maven, and Spring Boot repositories. Every visible entity must come from the current AnalysisReport or a deterministic filter over it. The graph may contain only real Component nodes and CONFIRMED COMPONENT_INJECTION directed edges whose source and target Components both exist. Do not show Maven edges, Mapper, Endpoint or EntryPoint nodes, method calls, runtime flows, databases, external systems, inferred layers, health scores, ranking, or ambiguous/unresolved edges.

Design a 1440x900 primary screen with a selected Service node and its 1-hop confirmed neighborhood active. Add a compact two-row exploration toolbar: Module selector; Component kinds multi-select with exact reported kinds and counts; Relationship filter; Search components; Reset filters; 1-hop neighborhood; Focus selection; and Fit view. Focus changes only the viewport, while Neighborhood changes the visible graph scope.

Important contract limitation: the current component graph has exactly one real relationship type, Component injection (Confirmed). Design the Relationship filter as an extensible but honest control with only this one counted option. Do not invent Maven dependency, method call, runtime call, unresolved, or ambiguous relationship options. If the only option is unchecked, keep components visible and show a clear filtered zero-edge state.

Add an always-available searchable Component list as a bottom results tray inside the central workspace, not as a permanent fourth column or a second left navigation tree. Each row shows component name, exact kind, module, and confirmed incoming/outgoing counts. The graph and list share the same filters and selection. Selecting a list row updates the graph and Inspector and can be focused or used as the center of a 1-hop neighborhood.

Show a persistent scope notice such as “4 of 8 reported components shown · 4 filtered · 3 confirmed relations · 1-hop neighborhood.” When the filtered result still exceeds 60 nodes or 120 confirmed edges, do not draw the first N items or imply a complete partial graph. Pause the graph, report the full matching node/edge count and the number withheld from the graph, keep all filters active, and keep the searchable list usable so the user can select a component and enter its 1-hop neighborhood.

Also design states for filtered empty, search no match, no relationship types selected, components with no confirmed edges, PARTIAL analysis with ambiguous/unresolved counts, graph runtime fallback, loading, report error, and failed analysis. Distinguish “no reported components” from “no components match the current filters.” Unresolved declared targets must never become graph nodes.

Responsive behavior: retain the three-pane workspace on wide desktop; allow filters to collapse while active chips and counts remain visible on compact desktop; use a list-first layout below 960px and in approximately 500px desktop windows, with graph as an optional secondary view. All controls and list rows must be keyboard operable, focus-visible, and not rely on color alone. Do not add native desktop chrome or future API Map / Source Navigation features.
```

## 16. 交付前自检

- [ ] 以已导入 T-0705 页面为视觉基线，没有重做 Shell 或主题。
- [ ] Module、Component kind 与 selected-node 1-hop neighborhood 在主画面中可组合。
- [ ] Relationship filter 只有真实的 Component injection 选项。
- [ ] Search、图、列表和 Inspector 共享 selection 与过滤状态。
- [ ] 可搜索列表是常驻可访问入口，不只是错误页临时 fallback。
- [ ] 超预算时不截断图，并显示完整 matching / withheld 数量与缩小范围动作。
- [ ] 区分仓库已知 0、筛选后为空、Unavailable、PARTIAL、FAILED 和 graph runtime failure。
- [ ] 没有 Mapper、Maven 边、Endpoint、EntryPoint、方法调用、推断 layer 或未知虚线边。
- [ ] Focus 与 Neighborhood 是两个不同动作。
- [ ] 窄屏和约 500px 桌面窗口采用 list-first，仍可完成核心探索。
- [ ] 未加入 T-0707 API Map、T-0708 Source Navigation 或宿主专属动作。
