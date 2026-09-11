# 当前项目状态

## 1. 当前阶段

RepoOnboard 已完成 **M7 — Local Web UI**，当前处于 **M8 — Start Here**。

- M0 至 M7 已完成。
- **T-0701 至 T-0709** 已完成，本地 UI 运行闭环、视觉系统、Repository Overview、Module Explorer、Architecture Workspace、探索筛选能力、API Map、跨页面 Source Navigation，以及本地 Web / 打包安全边界已建立。
- **T-0801 至 T-0803** 已完成，确定性重要度信号、文件级推荐阅读路径和来源可回溯的解释已经建立，M8 正在进行。
- 当前任务是 **T-0804 — Start Here UI**。页面级数据契约审计与 Stitch Design Brief 已完成；下一步等待用户生成、审阅并导入 Stitch 原型，在此之前不实现页面。
- Java 21 / Maven 3.9.16 `clean verify` 当前运行 74 项前端测试和 156 项 Java 测试并全部通过，并在 verify 阶段直接检查生产 JAR 内的离线 UI 资源与 schema 契约。真实 loopback 服务的安全响应头、外部 Origin 拒绝、1280×800 / 1024×640 四个主要页面布局和干净浏览器控制台均已验证。

## 2. 已完成任务

### M0 — Technical Architecture

- **T-0001 至 T-0004**：完成技术架构、产品边界、ADR 集合和 V0.1 里程碑划分。

### M1 — Project Foundation

- **T-0101 至 T-0105**：建立 Java 21 单模块工程、Maven Wrapper、JUnit 测试基础、Picocli CLI，以及 `SourceLocation`、`Evidence`、`Diagnostic` 和分析状态契约。

### M2 — Maven Project Analysis

- **T-0201 至 T-0206**：实现 Maven 项目检测、坐标与依赖提取、受限离线模型解析、多模块分析、profile/parent/BOM 支持和 Spring Boot 构建信号识别。

### M3 — Java Source Analysis

- **T-0301 至 T-0305**：实现模块感知的源码根和 Java 文件发现、JavaParser AST facts、项目内类型引用解析及声明索引。

### M4 — Spring Boot Analysis

- **T-0401 至 T-0403**：实现 Spring 组件、配置类、应用入口、注入候选和项目内组合注解识别。
- **T-0404 — Mapper Detection** 保持可选延后，不阻塞 M4。

### M5 — API & Dependency Analysis

- **T-0501 至 T-0504**：实现 Spring MVC mapping、完整 Endpoint 组合、组件依赖提取、歧义保留、Evidence 合并和确认邻接查询。

### M6 — Report Assembly & Serialization

- **T-0601**：用 `ProjectAnalysisInput` 和 `ProjectModelAssembler` 将 M2 至 M5 facts 组装为 analyzer-neutral `AnalysisReport`。
- **T-0602**：用 `StableIdentifiers`、确定排序和冲突诊断提供稳定实体身份。
- **T-0603**：用 `AnalysisReportJson`、显式 DTO 和 Jackson 3 提供 UTF-8、固定 LF、版本化 JSON codec。
- **T-0604**：从报告实体派生 `AnalysisSummary`，并校验 Summary 与实体一致性。

### M7 — Local Web UI

- **T-0701**：CLI 现在组装统一 `AnalysisReport`，启动只绑定 `127.0.0.1` 且使用系统分配端口的 JDK `HttpServer`，通过固定只读路由提供 `/api/report` 和随包发布的 Vue 3 最小 App Shell。CLI 支持默认打开浏览器、`--no-open`、浏览器失败时输出手动地址，以及 Ctrl+C 关闭与资源释放。
- **T-0702**：建立集中 design tokens、全局基础样式和响应式产品 Shell；新增可复用 Sidebar、Context Header、Page Layout、Inspector 与 loading/empty/error/ready 状态组件，并加入 skip link、可见键盘焦点、文本对比度门禁和 reduced-motion 支持。宽屏采用三栏布局，紧凑桌面尺寸折叠 Inspector，窄屏导航可横向滚动。
- **T-0703**：实现 Repository Overview，展示仓库身份、构建系统、已报告语言/框架、模块/源码根摘要、组件角色与主要实体计数、应用入口和显式覆盖诊断。SUCCESS、PARTIAL、FAILED 分别呈现完成、受限和失败状态；缺失事实显示不可用或明确空状态。
- **T-0704**：实现本地 Stitch 原型对应的 Module Explorer。公共 `Module` 增加显式聚合父 ID、带 Evidence 的 Java / Framework 版本 facts；Maven 依赖仅在完整 GAV 唯一匹配时连接真实模块。页面提供模块层级、搜索/筛选、统计、Maven 元数据、源码根、技术版本、内部模块关系、组件/入口内容和 Inspector，并明确区分已知 0 与不可用数据。schema 当前为 `1.2`，继续兼容读取 `1.0` / `1.1`。
- **T-0705**：按照 UI Data Contract Audit、用户导入的 Stitch 原型和产品需求实现 Architecture Workspace。Cytoscape.js 仅绘制当前模块中的 `COMPONENT_INJECTION + CONFIRMED` 边，提供 zoom、pan、fit、节点/关系选择、Inspector、源码位置与原始 Evidence；PARTIAL、FAILED、Unavailable、Unresolved、Ambiguous 和已知 0 保持不同语义。移动端以组件列表回退，超过 60 个节点或 120 条确认边时拒绝绘制误导性的残缺图。
- **T-0706**：在 Architecture Workspace 中实现模块、组件类型、关系类型的可组合筛选、节点搜索、所选组件一阶邻域、Reset / Focus / Fit，以及始终可用的可搜索组件列表。预算在筛选后计算；超预算时完整显示匹配数量和隐藏数量，停止图渲染但不截断列表。
- **T-0707**：按照数据契约审计和用户导入的 Stitch 原型实现 API Map。页面提供 module、HTTP method 与 path/handler/controller/source 的可组合过滤，区分数据不可用、已知空和筛选为空；`ANY`、未解析 method/path、mapping conditions 与未解析条件均保持显式。选中 Endpoint 后可查看 handler、controller、源码位置及类级/方法级 mapping Evidence。
- **T-0708**：按照数据契约审计和用户导入的 Stitch 原型实现 Source & Evidence Detail。Architecture 组件和 API Endpoint 共用该跨页面辅助表面，可查看并复制 scan-root-relative path、1-based 行列和可选 symbol；module、SourceFile、Evidence、同文件实体、确认依赖邻居和诊断均通过精确报告身份连接。未知位置不补 `0`，复制不可用或失败时保留可选文本并给出明确反馈。
- **T-0709**：完成 ADR-0011 / ADR-0012 边界加固与验收。服务严格绑定 `127.0.0.1`，校验 Host / Origin，拒绝路径穿越、编码或带 query 的非规范路由、任意文件和写请求，并为全部响应发送 CSP、no-store、nosniff、same-origin resource policy、no-referrer 和 frame denial。前端校验报告 schema major 与打包 marker，恶意仓库名、路径和诊断只按文本渲染；Maven verify 直接读取 JAR 检查本地资源闭包。报告组装现已纳入源码发现、注入和 MVC mapping 阶段诊断，CLI 与 UI 的 PARTIAL 状态和覆盖提示保持一致。

### M8 — Start Here

- **T-0801**：新增框架中立的 `ReadingImportanceHeuristics` 和显式重要度信号。根 POM、应用入口、配置、拥有 Endpoint 的 Controller，以及沿确认组件注入边可达的 Service / Repository 按固定规则、明确计数与依赖距离排序；依赖邻域保留支持它的 dependency ID。规则不按文件去重、不限制数量、不生成最终解释，以保持 T-0802 / T-0803 边界。
- **T-0802**：新增 `RecommendedReadingPathGenerator`、文件级 `RecommendedReadingItem` 和带默认边界的 `RecommendedReadingPath`。同一文件的多条重要度信号按原优先级合并且全部保留，模块间使用稳定身份定序；完整路径不可变，默认最多返回 10 项并可切换到完整列表。unresolved dependency 不会通过 T-0801 边界进入路径。
- **T-0803**：新增 `RecommendationExplanationGenerator`、结构化原因和 `ExplainedReadingPath`。每个推荐文件的根构建、应用入口、配置、Endpoint 暴露和确认依赖原因均保留报告实体 ID 与原始 Evidence；Endpoint、确认入边/出边计数直接由当前 `AnalysisReport` 事实计算，依赖路径保留距离。PARTIAL / FAILED 和异常覆盖状态携带显式限制代码与说明，解释顺序严格对应 T-0802 阅读路径。

## 3. 当前实现能力

项目已经能够在本地、离线优先地分析 Java 21 / Maven / Spring Boot 仓库，并通过响应式 Web 产品 Shell 交付结果：

- 从 CLI 接收仓库路径、显式 profile、本地 Maven 仓库位置和 `--no-open`。
- 检测并受限解析 Maven 项目、模块、坐标、源码目录、依赖和 Spring Boot 构建信号；局部失败尽量保留可用事实。
- 发现并解析模块所属 Java 文件、声明、注解和项目内可确认类型引用。
- 识别 Spring 组件、配置类、应用入口、注入候选、组合注解、MVC Endpoint 和组件依赖。
- 对确认、歧义和未解析关系分别表达，并为重要结果保留来源证据和诊断。
- 将全部分析结果组装为稳定、确定排序的 `AnalysisReport`，生成派生 Summary，并以 schema `1.2` JSON 序列化；旧 `1.0` / `1.1` 报告仍可读取。
- 从统一 `AnalysisReport` 派生稳定的 Start Here 重要度信号和文件级阅读路径；只接受可验证的入口、配置、Endpoint 和确认组件注入关系，不从类名猜测业务重要性。路径按文件去重，保留完整结果并提供默认 10 项视图。
- 分析成功后在系统分配的 loopback 端口启动本地服务；`/api/report` 返回本次报告，固定静态路由返回打包的前端资源，未知路径和非只读请求不会访问用户文件。
- Repository Overview 能在 30 秒摘要层展示仓库上下文、报告状态、技术事实、统计、应用入口和覆盖限制，全部来自报告字段或可重现的实体派生规则。
- Module Explorer 能按显式 Maven aggregation 关系浏览模块，按报告实体确定性计算模块统计，查看元数据、源码根、通用版本 facts、确认的内部模块依赖、组件/入口、诊断与 Evidence；不从路径、名称、坐标或类名补造事实。
- Architecture Workspace 能按模块查看确认的 Spring 组件注入图，区分 Controller、REST Controller、Service、Repository、Component 和 Configuration；组件类型、关系类型和一阶邻域可组合，搜索列表可按名称、限定名、路径或符号定位组件。图可缩放、平移、适配与聚焦选择，并在 Inspector 中查看报告原值、当前范围内的确认关系、源码位置和原始 Evidence。Maven 边、Mapper、Endpoint、EntryPoint、歧义/未解析边和任何推断关系都不会混入组件图。
- API Map 能按模块和 HTTP method 精确筛选，并按 path、handler、controller、module 或 source 搜索。列表明确呈现 `ANY`、未解析 method/path、已知 mapping conditions 和未解析条件；Inspector 展示 Endpoint 报告原值、handler 源码位置以及方法级和类级 mapping Evidence，不补造原始分层 path、方法签名、请求响应模型、鉴权或调用链。
- Source Navigation 从选中的 Architecture Component 或 API Endpoint 进入统一详情，展示扫描根相对 SourceLocation、真实 1-based 行列、可选 symbol、所属模块、SourceFile 元数据和原始 Evidence；related facts 只使用 exact ID/path 与两端确认的组件依赖，Endpoint 明确说明组件关系不是 handler 调用链。返回后保留原页面选择，不读取源码正文，也不提供虚假的 Open / IDE 动作。
- loading、empty、warning、error、ready 具有统一组件、文案层级和 live-region 语义；键盘用户可使用 skip link 和清晰的 `:focus-visible` 状态。
- 集中 tokens 管理色彩、字体、间距、边框、圆角、阴影和布局尺寸；响应式布局覆盖宽屏、紧凑桌面与窄屏模式。
- 浏览器无法自动打开时服务保持运行并给出地址；Ctrl+C 后服务线程和端口释放。

## 4. 当前架构与核心模块

项目仍是单模块、单进程 Java 应用，前端在构建时打包进 JAR：

```text
RepoOnboard
├── src/main/java/io/github/zhancm/repoonboard
│   ├── cli
│   ├── analyzer
│   │   ├── maven
│   │   ├── java
│   │   └── spring
│   ├── analysis
│   ├── core.model
│   ├── serialization
│   └── web
└── frontend
    └── src
```

- **`cli/RepoOnboardCommand`**：处理参数、分析流水线、文本诊断与退出码；成功获得 Maven 根模型后组装报告并交给 UI 启动器。
- **`analyzer.maven`**：负责受限、离线的 Maven 模型、模块、依赖和 Boot 构建信号分析。
- **`analyzer.java`**：负责源码发现、JavaParser facts、声明索引和保守的项目内引用解析。
- **`analyzer.spring`**：负责组件、配置、入口、注入、MVC Endpoint、组件依赖及组合注解 facts。
- **`analysis`**：`ProjectAnalysisInput` 汇集分析器输出，`ProjectModelAssembler` 转换为公共报告。
- **`core.model`**：保存框架中立的项目实体、状态、证据、诊断、稳定身份和派生 Summary；`Module` 持有 aggregation parent ID，`LanguageVersion` / `FrameworkVersion` 表达带证据的版本事实。
- **`serialization`**：`AnalysisReportJson` 与 `ReportJsonDocument` 提供版本化 JSON 边界。
- **`web/LocalUiLauncher`**：CLI 与 UI 生命周期之间的最小接口。
- **`web/LocalUiApplication`**：协调本地服务、浏览器启动、`--no-open`、关闭钩子和等待生命周期。
- **`web/LocalUiServer`**：使用 JDK `HttpServer` 绑定 `127.0.0.1`，只提供固定前端资源与当前报告；校验 Host / Origin 与规范路径，拒绝通用文件系统访问和写请求，并集中发送本地 UI 安全响应头。
- **`frontend/src/components`**：`AppShell`、`SidebarNav`、`ContextHeader`、`PageLayout`、`InspectorPanel`、`StatePanel` 构成可复用展示骨架；`OverviewView` / `MetricCard` 负责 Overview，`ModuleExplorerView` / `ModuleInspector` 负责模块工作台，`ArchitectureWorkspaceView` / `ArchitectureGraph` / `ArchitectureInspector` 负责组件关系工作台，`ApiMapView` / `ApiInspector` 负责 HTTP Endpoint 工作台，`SourceDetailView` / `SourceRelatedInspector` 负责 Component / Endpoint 共用的 Source & Evidence Detail，`App.vue` 负责报告加载、页面切换、范围和选择状态。
- **`frontend/src/lib/reportOverview.js`**：把 Summary 和报告实体转换为纯展示模型，集中管理计数回退、技术事实去重、状态/覆盖说明、入口位置与缺失值语义。
- **`frontend/src/lib/reportModules.js`**：以显式 parent ID 构造模块树，按 moduleId 分组实体与统计，只读取 schema `1.2` 的确认内部 Maven 边，并集中处理 Unavailable / 0 语义。
- **`frontend/src/lib/reportArchitecture.js`**：从公共报告构造组件图展示模型，仅接受有真实源/目标组件的确认注入边；确定性组合模块、组件类型、关系类型和一阶邻域，派生可搜索列表、隐藏数量、未确认关系覆盖和筛选后图规模预算，并保持 Unavailable 与 0 的区别。
- **`frontend/src/lib/reportEndpoints.js`**：用精确 `moduleId` / `componentId` 连接 Endpoint、模块和 Controller，集中派生筛选选项、可搜索文本、条件状态、handler 来源、Evidence 分组和不可用/已知空语义；`ANY` 只作为精确 method 值，不作为通配符。
- **`frontend/src/lib/reportSources.js` / `sourceNavigationHost.js`**：前者用精确身份连接选中实体、模块、SourceFile、Evidence、同文件实体、诊断和确认依赖邻居；后者隔离 Clipboard 宿主能力，并统一返回 copied / unavailable / failed 状态。
- **`frontend/src/styles`**：`tokens.css` 集中视觉变量，`base.css` 提供全局与无障碍基础，`shell.css` 负责通用组件，`overview.css`、`modules.css`、`architecture.css`、`api.css` 和 `source.css` 分别定义业务页面及响应式降级。
- **前端构建与测试**：Vue 3 + JavaScript + Vite；Vitest + happy-dom 测试在 Maven `generate-resources` 阶段随锁文件安装、测试和生产构建，产物复制到 JAR classpath。

## 5. 重要技术决策

- **ADR-0001 至 ADR-0003**：Java 21、Maven + Maven Wrapper、Picocli CLI。
- **ADR-0004 至 ADR-0006**：受限离线 Maven Model Builder、JavaParser、仅确认项目内可证明的类型关系。
- **ADR-0007 至 ADR-0009**：公共模型保持最小且框架中立；分析器与展示边界明确；依赖为有类型、有方向、带状态和证据的边。
- **ADR-0010**：报告使用 UTF-8、版本化 JSON、显式 DTO、稳定排序和受限反序列化；当前 schema `1.2`，兼容读取 `1.0` / `1.1`。
- **ADR-0011**：本地服务使用 JDK `HttpServer` 并只绑定 loopback；默认使用系统分配端口，固定只读路由，不提供任意文件读取。
- **ADR-0012**：前端使用 Vue 3 + JavaScript + Vite，测试使用 Vitest，组件图使用 Cytoscape.js；所有生产资源随 JAR 提供，不使用运行时 CDN。开发/发布构建需要 Node，最终用户不需要。
- **ADR-0013 至 ADR-0015**：Start Here 采用可解释确定性启发式；测试按 unit → fixture → integration → real repository 分层；局部失败优先返回部分成功。
- **ADR-0016 / ADR-0017**：V0.1 交付 JAR 和启动脚本，保持 Web-first、Desktop-ready；桌面容器、安装器和自带 Runtime 延后到 V0.2 候选。
- V0.1 继续只支持 Java + Maven + Spring Boot，不引入 LLM、RAG、云服务、数据库、遥测或自动改码能力。

## 6. 重要变更

- 项目已从架构设计推进到完整静态分析链、统一报告边界和本地 Web 运行闭环。
- Maven 分析由简单 POM 读取演进为受限离线模型解析；Java/Spring 分析演进为模块感知、证据驱动且明确保留不确定性的 facts。
- API mapping 声明与 Endpoint 组合分离；组件依赖只为唯一确认的项目内目标生成确认边。
- 序列化通过显式 DTO 与 Core Model 隔离，Summary 从实体即时派生而不是保存第二份可变计数。
- T-0701 已把原先独立存在的分析、报告、序列化和展示边界连接起来；CLI 不再停留在纯文本摘要，而是在保留终端输出后启动本地只读 UI。
- T-0702 将临时单文件样式替换为集中视觉系统和可复用 Shell 组件，后续业务页面必须复用这些边界，而不是自建竞争样式。
- T-0703 将空白工作区替换为第一个正式业务页面；Overview 保持仓库级摘要，不展示模块技术版本，也不从 Maven 坐标或名称推断。
- T-0704 根据 UI Data Contract Audit 只补充产品明确需要的最小公共 facts：Maven 聚合父 ID、通用语言/框架版本及唯一 GAV 确认的内部模块目标；Stitch 中的示例状态、计数和路由没有被硬编码。
- T-0705 未为贴合原型而扩展后端模型；Architecture 直接消费现有 Component、Dependency、Diagnostic、SourceLocation 和 Evidence。图只呈现确定事实，未确认关系以覆盖提示和计数表达，超预算范围整体停止绘制。
- T-0706 延续同一公共报告边界；搜索只影响替代列表，筛选和邻域确定图范围，预算在组合筛选后判定。宽屏紧凑高度默认折叠列表以保留图画布，窄屏隐藏图并以完整组件列表作为主入口。
- T-0707 数据契约审计确认 schema `1.2` 已足以实现 API Map，无需扩展后端。Endpoint 的 final path、method、conditions、handler location 与两级 mapping Evidence 可直接展示；公共报告没有 class / method 原始 path 片段、方法签名、请求响应模型、鉴权或调用链，设计不得补造。
- T-0707 实现严格排除了 Stitch 原型中的示例仓库状态、固定计数、伪方法签名、Copy Path、Open IDE 和 Jump to Symbol 等无数据或越过 T-0708 边界的内容；宽屏保留可扫描表格与 Inspector，窄屏切换为 Endpoint 卡片和顺序详情。
- T-0708 数据契约审计确认 schema `1.2` 足以实现 Component / Endpoint 的 Source / Evidence Detail，无需增加源码读取路由或后端模型。V0.1 Source Navigation 只展示并复制扫描根相对 path、真实 1-based 位置、可选 symbol、module、Evidence 与可确认 related facts；它不是文件树、源码编辑器或 IDE 集成。Related Components 只允许 exact ID / source path joins 和两端可确认的 Dependency，Endpoint 上下文必须说明组件关系不等于 handler 调用链。
- T-0708 实现保留 Stitch 的轻量详情页和右侧 Related Facts 结构，同时排除示例仓库数据、运行状态、源码预览、绝对路径、Git 信息、Open / IDE / file 动作和 handler 调用链推断。Clipboard 通过最小宿主边界调用；失败或不可用时明确反馈，文本始终可手动选择。
- T-0709 将本地 Web 安全假设转换为可执行边界：Host / Origin、规范路由、CSP、缓存、CORS 缺省、内容注入、并发读取、JAR 资源闭包、schema 和关闭释放均有自动化证据。Cytoscape 容器定位也在真实浏览器 warning 暴露后修正并加入回归检查。
- T-0804 数据契约审计确认 T-0801 至 T-0803 的 Java 模型已经具备完整推荐顺序、原因、Evidence、计数和覆盖说明，但 `/api/report` 尚未暴露这份派生结果。后续实现应从同一不可变报告快照生成独立、版本化、固定只读的 `/api/start-here` projection；公共报告 schema `1.2` 保持不变，Vue 不重复实现启发式。推荐文件以路径为主身份，各原因分别展示 Evidence location；只有 supporting entity ID 与公共报告精确连接时才提供 Component / Endpoint 跨页面导航。
- 后续前端页面统一采用“Codex 数据契约审计与 Stitch 方案 → 用户 Stitch 设计 → Codex Vue 实现 → Browser Validation”，用户设计完成前不提前编码页面。
- README 已提供中英文版本和语言切换，并更新为当前本地 UI 启动方式。

## 7. 已知限制 / 技术债

- **Mapper 未实现**：T-0404 的 MyBatis / MyBatis-Plus Mapper 专用识别仍为可选延后项。
- **分析范围有限**：不支持 Java/Maven/Spring Boot 以外的生态；Maven 不联网、不执行插件/生命周期、不计算传递依赖；Java/Spring 采用保守静态分析，不覆盖运行时代理、反射和动态注册。
- **后续业务视图尚未实现**：Start Here 尚未实现；Source Navigation 已作为 Architecture / API 的跨页面辅助表面交付，不是独立一级导航。
- **图规模受限**：筛选后的范围超过 60 个组件或 120 条确认关系时，页面明确显示匹配与隐藏数量并停止绘图；用户可继续通过完整搜索列表选择组件，或用组件类型与所选组件一阶邻域缩小范围。
- **构建环境**：从源码构建目前需要兼容锁定 Vite 工具链的 Node.js；发布产物的最终用户不需要 Node。
- **Windows Wrapper 启动缺陷**：当前 `mvnw.cmd` 在本机 `.m2` 目录不是链接时会读取空的 `Target[0]` 并提前失败；本轮使用 Wrapper 已下载且校验过的 Maven 3.9.16 完成全量验证，启动脚本本身仍需单独修复。
- **前端包体警告**：单一生产 JavaScript 产物约 602 kB（gzip 约 190 kB），Vite 会提示超过 500 kB；T-0709 已确认离线 JAR 可正确加载，是否拆包应在真实仓库性能数据表明必要时再决定。
- **发布尚未就绪**：当前仍为 `0.1.0-SNAPSHOT`，安装体验、许可、演示资源和正式 V0.1 Release 尚未完成。

## 8. 未完成任务

- **可选延后**：T-0404 — Mapper Detection。
- **M8**：T-0804，完成 Start Here UI；T-0801 至 T-0803 已完成。
- **M9**：T-0901 至 T-0907，完成综合 fixture、真实仓库和 onboarding 价值验证。
- **M10**：T-1001 至 T-1008，完成安装、错误体验、发布文档、演示、License、GitHub 清理和 V0.1 发布。
- **V0.2 候选**：Desktop Application 技术试验与打包。

## 9. 下一步

下一项开发任务：

> **T-0804 — Start Here UI**

页面级 Data Contract Audit 与 Stitch Design Brief 已完成。下一步由用户据此生成、审阅并导入 Stitch 原型；导入后先审计设计中的示例值、原因、Evidence、状态和导航动作，再实现只读 `/api/start-here` projection 与 Vue 页面。设计不得加入前端重算排名、AI 业务摘要、阅读进度、源码正文、虚假 Open / IDE 动作、Settings 或语言切换。

## 10. 给下一次开发会话的上下文

RepoOnboard 是本地优先、确定性、可解释的陌生代码库理解工具；V0.1 只支持 Java 21 + Maven + Spring Boot，不使用 LLM、云服务或数据库。工作前按 `PROJECT.md` → `DECISIONS.md` → `TODO.md` → `AGENTS.md` → `STATE.md` → 当前代码阅读。M0–M7 和 T-0801 至 T-0803 已完成，公共报告为 schema `1.2` 且兼容读取 `1.0` / `1.1`。本地 Vue UI 已有响应式 Shell、Repository Overview、Module Explorer、Architecture Workspace、API Map，以及从 Component / Endpoint 进入的 Source & Evidence Detail；Start Here 后端已有稳定重要度信号、默认 10 项且可展开的文件级阅读路径，以及携带报告实体、Source Evidence、准确计数和 PARTIAL 覆盖说明的结构化解释。当前任务是 **T-0804 — Start Here UI**：Data Contract Audit 与 Stitch Design Brief 已完成，等待用户生成、审阅并导入原型；导入前不编码页面。后续使用同一不可变报告快照生成独立只读 `/api/start-here` projection，保持报告 schema `1.2` 和 Java 单一启发式来源。每个 T 应独立测试、更新 TODO/STATE、提交并推送 GitHub。
