# T-0705 — Architecture Workspace Stitch Design Brief

> 供 Stitch 生成高保真页面使用。数据边界以 `T-0705-UI-DATA-CONTRACT.md` 为准；本轮只设计 T-0705，不包含 T-0706 的高级探索与筛选。

## 1. 页面目标

让第一次进入陌生 Java / Spring Boot 仓库的开发者，在不阅读全部源码的情况下回答：

1. 当前模块有哪些已确认的主要 Spring 组件？
2. 哪些组件通过已确认的注入关系直接依赖另一个组件？
3. 选中的组件或关系来自哪个文件、哪一行、哪条分析规则？
4. 当前图是否受到分析覆盖或规模预算限制？

页面是只读架构理解工具，不是图编辑器，也不是架构质量评分仪表盘。

## 2. 必须延续的产品外壳

严格沿用现有 Overview / Module Explorer 的视觉体系：

- 浅色、低噪声、企业级技术工具风格。
- 左侧产品导航、顶部仓库上下文、中央工作区、右侧 Inspector。
- 延续已有 sidebar 宽度、顶部栏高度、边框、圆角、间距、字体层级和状态色。
- `Architecture` 导航项为当前激活项；Overview、Modules 可进入；APIs、Start Here 继续保持未开放。
- 不引入深色主题、渐变大卡片、玻璃拟态、霓虹色、营销式插画或装饰性图表。
- 图节点颜色少而稳定，组件种类主要通过小型 type badge、图标轮廓和文字区分，不做彩虹图。

视觉关键词：

```text
Minimal
Enterprise
Technical Workbench
Evidence-first
Calm
Readable
Precise
Local-first
```

## 3. 宽屏页面结构

建议 1440 × 900 主画板，使用现有三栏 workbench：

```text
┌──────────────┬───────────────────────────────────────────┬──────────────────┐
│ Product Nav  │ Repository Context / Scope                │ Inspector        │
│              ├───────────────────────────────────────────┤                  │
│ Overview     │ Architecture header + compact summary     │ Selected node    │
│ Modules      ├───────────────────────────────────────────┤ or edge          │
│ Architecture│ Module scope     Fit view     Graph status │                  │
│ APIs         ├───────────────────────────────────────────┤ Source evidence  │
│ Start Here   │                                           │                  │
│              │        Architecture Canvas                │                  │
│              │                                           │                  │
└──────────────┴───────────────────────────────────────────┴──────────────────┘
```

中央画布必须是视觉主角；Inspector 信息密度高但不抢占主画布。不要再增加左侧组件树，避免形成四栏。

## 4. 页面模块

### 4.1 Repository Context Bar

沿用 Module Explorer 顶部栏：

- 左侧：`Repository` + repository name。
- 右侧：reported component count、confirmed relation count、analysis status。
- PARTIAL / FAILED 使用现有状态语义，不出现组件级 Healthy。

### 4.2 Architecture Header

- Eyebrow：`Architecture workspace`
- Title：`Component relationships`
- Description：`Confirmed Spring component dependencies from the current analysis report.`
- 一行轻量 legend：Controller、Service、Repository、Component、Configuration。
- REST Controller 在 UI 上归入 Controller 家族，但 Inspector 保留精确 `REST_CONTROLLER` 类型。
- 不展示 Mapper；当前公共模型没有 Mapper 数据。

### 4.3 Minimal Scope Bar

T-0705 只保留保障可读性所需的最小控制：

- 单选 `Module scope`。
- `Fit view` 按钮。
- 只读范围摘要：`{shown} reported components · {confirmed} confirmed relations`。
- 图被预算限制时，显示明确 notice：`The graph is limited for readability. Choose a module to continue.`

不要在这版原型加入以下可操作控件：

- Component Kind filter
- Relationship Type filter
- Node Search
- Selected-neighborhood mode
- Reset / Focus Selection
- 可搜索组件列表

这些属于 T-0706。可为后续保留合理空间，但不能用禁用按钮制造已实现错觉。

### 4.4 Architecture Canvas

节点规范：

- 白色或极浅底色、1px 中性边框、轻微圆角与克制阴影。
- 第一行：type glyph + 简短类名。
- 第二行：component kind badge。
- 可选第三行：所属 module 的短标签；长名称省略时必须有可访问完整值。
- 选中节点使用品牌蓝描边和轻量外发光；焦点态必须与 hover 区分。
- 未选中节点保持足够对比度，不因“降噪”变成不可读。

边规范：

- 只画 `COMPONENT_INJECTION` 且 `CONFIRMED` 的有向边。
- 使用清晰箭头表达 source → target。
- 默认中性灰；选中边或连接选中节点时使用品牌蓝。
- 不以动画流线表达调用，也不使用粗重曲线。
- 不绘制 Maven module dependency、ambiguous 或 unresolved 关系。

布局规范：

- 优先形成左到右或上到下的稳定层次，但不能因节点 kind 强行制造不存在的“层”。
- 节点之间保留足够呼吸空间，标签不重叠。
- 空白画布区域可用于 pan，不放装饰图案。
- 右下角提供紧凑 zoom in、zoom out、fit controls；触控区域约 44px。
- 初始布局不可无界渲染全仓库图。单模块项目可自动使用唯一模块；多模块项目默认要求选择模块，或仅在预算内允许 repository scope。

### 4.5 Inspector — Node Selected

右侧 Inspector 层级：

1. `Selected component`
2. 简短类名 + 精确 kind badge
3. Qualified name
4. Module
5. Framework
6. Declared component name（仅有值时）
7. `Confirmed relations`：incoming count / outgoing count
8. `Source evidence`
   - relative source path
   - line / column（存在才显示）
   - symbol（存在才显示）
   - evidence type / rule ID

不显示：health score、importance score、推断职责、推断 layer。

### 4.6 Inspector — Edge Selected

层级：

1. `Selected relationship`
2. Source component → Target component
3. `Component injection`
4. `Confirmed`
5. Declared target
6. Source module / Target module
7. Dependency SourceLocation
8. Evidence type / rule ID / related locations

关系方向必须与公共模型一致，不能根据 Controller / Service 类型反转。

### 4.7 Inspector — Nothing Selected

显示使用提示和契约说明：

- `Select a component or confirmed relationship to inspect its source evidence.`
- 当前 module scope。
- reported components / confirmed relations。
- ambiguous / unresolved relation count（仅大于 0 时出现）。
- 若报告 PARTIAL，显示 coverage limitation 摘要。

## 5. 必须设计的页面状态

请在同一 Stitch 项目中至少提供以下关键状态，或在主屏旁给出状态组件规范：

### Ready — Node Selected

主高保真画面。选中一个 Service 节点，相关确定边高亮，Inspector 展示源码证据。

### Ready — Edge Selected

选中一条 Controller → Service 的 confirmed injection edge，Inspector 展示 declared target 与 Evidence。

### Module Scope Required

多模块仓库且 repository graph 超过预算：

- 不渲染截断后看似完整的随机子图。
- 画布中央说明需要选择 module。
- Module scope 控件成为主要操作。

### Components Without Confirmed Edges

- 正常显示组件节点或安全的非图摘要。
- 文案：`No confirmed component relationships in this scope.`
- 不把它当成分析错误。

### Empty

`components[]` 已知为空：

- `No framework components were reported.`
- 如状态 PARTIAL，补充 coverage 说明。
- 不显示 `0 Mappers`。

### Partial / Unresolved

- 只画 confirmed edges。
- 顶部或 Inspector 显示 `Some relationships could not be resolved.`
- 明确 ambiguous / unresolved 数量，不用虚线假装边已知。

### Loading

Shell 立即出现；中央用已有 StatePanel 语言，不制作模拟节点骨架以免形成错误结构。

### Error / Failed

区分网络/报告加载失败与 `AnalysisReport.status = FAILED`。保留仓库上下文和诊断入口，不展示虚构图。

### Graph Rendering Fallback

若图形运行时不可用，提供清楚的只读组件摘要/基础列表和 Inspector 入口。T-0706 再完善为可搜索列表。

## 6. 响应式与未来桌面窗口

### ≥ 1280px

- 完整三栏：导航 / 画布 / Inspector。
- 画布保持最大可用空间。

### 960–1279px

- 左侧导航保持紧凑。
- Inspector 移到中央内容下方或作为右侧可关闭 drawer。
- 画布最小高度约 560px，不允许被工具栏挤压。

### < 960px

- 复用现有顶部横向导航。
- 默认优先只读组件列表 / 选中项详情；完整画布作为次级入口。
- Inspector 变为页面内详情或 bottom sheet。
- 不依赖 hover；所有关键信息可通过点击和键盘获得。

### 约 500px 的桌面小窗口

- 行为与窄屏一致，不能假设桌面应用永远有大窗口。
- 路径、qualifiedName 和 symbol 安全换行或横向滚动，不裁掉关键尾部。
- 当前 V0.1 仍是 Web-first；不要画原生标题栏、文件选择器、最近项目或桌面专属按钮。

## 7. 可访问性要求

- 图之外必须存在可被辅助技术理解的节点 / 关系摘要。
- 节点可获得键盘焦点；Enter / Space 选择，Escape 清除选择。
- 焦点环清晰；颜色不是区分 component kind 或选中状态的唯一手段。
- Zoom / Fit 控件有文本标签或明确 aria-label。
- reduced-motion 下不使用缩放过渡或布局动画。
- Tooltip 不能承载唯一信息。

## 8. Stitch 示例数据约束

示例可使用以下通用名称以表达布局：

```text
OrderController
OrderService
OrderRepository
PaymentClient
ApplicationConfig
```

允许的示例边：

```text
OrderController → OrderService
OrderService → OrderRepository
OrderService → PaymentClient
```

这些只是视觉样本。不要在设计里写死真实仓库数量、文件路径、版本、Mapper、数据库、消息队列、外部服务或方法调用。不要根据名称额外画边。

## 9. 可直接粘贴给 Stitch 的主提示词

```text
Design a high-fidelity Architecture Workspace screen for RepoOnboard, a local-first enterprise developer tool for understanding unfamiliar Java, Maven, and Spring Boot repositories.

Strictly reuse the existing RepoOnboard Module Explorer visual system: light theme, compact left navigation, slim repository context bar, spacious central workbench, and right-side Inspector. The style is minimal, calm, precise, technical, and evidence-first. Avoid gradients, glassmorphism, marketing visuals, dark theme, colorful graph decoration, and dashboard-style vanity charts.

The Architecture page is active in navigation. The main canvas shows only confirmed Spring component injection relationships. Nodes may represent Controller, REST Controller, Service, Repository, Component, and Configuration. Do not show Mapper. Use white or very light node cards, neutral borders, restrained type badges, clear labels, and directed arrows. Do not mix Maven module dependency edges into this graph. Do not show inferred layers, method calls, databases, external systems, health scores, or importance scores.

Use a three-pane 1440x900 workbench. At the top of the main area, show the repository context and a compact Architecture header. Provide only a single Module scope selector, Fit View, a shown component count, a confirmed relation count, and a graph scope notice. Do not add component kind filters, relationship filters, node search, selected-neighborhood mode, Reset, Focus Selection, or a searchable list; those belong to the next milestone.

Show a selected Service node in the primary screen. Highlight that node and its directly connected confirmed edges with restrained brand blue while preserving readable context. Add compact zoom in, zoom out, and fit controls at the bottom-right of the canvas.

The right Inspector must show selected component name, exact component kind, qualified name, module, framework, confirmed incoming/outgoing counts, relative source path, optional line and symbol, evidence type, and rule ID. It must never show inferred responsibility, layer, health, or ranking.

Also define visual states for: selected edge, module scope required because the graph exceeds its readability budget, components with no confirmed edges, no components reported, partial analysis with ambiguous/unresolved relationship counts, loading, report error, and graph rendering fallback.

Responsive behavior: full three-pane layout on wide desktop, Inspector below or in a drawer on compact desktop, and list-first details on mobile or approximately 500px desktop windows. Do not design native desktop chrome; V0.1 remains Web-first.
```

## 10. 交付前自检

- [ ] Architecture 导航处于激活状态，并延续现有 Shell。
- [ ] 画布只包含 Component 节点与 confirmed component injection 边。
- [ ] 没有 Mapper、Maven 边、Endpoint、EntryPoint、方法调用或推测节点。
- [ ] T-0706 的筛选、搜索和邻域模式未提前出现。
- [ ] Node 与 Edge Inspector 都能回到 Source Evidence。
- [ ] 已设计 scope required、empty、no-edge、partial、loading、error 和 fallback。
- [ ] 图规模限制不会伪装成完整结果。
- [ ] 窄屏和约 500px 桌面窗口仍可完成核心阅读任务。
