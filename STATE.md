# 当前项目状态

## 1. 当前阶段

RepoOnboard 已完成 **M9 — Regression & Real Repository Validation**，当前处于 **M10 — Release Preparation**。

- M0 至 M7 已完成。
- **T-0701 至 T-0709** 已完成，本地 UI 运行闭环、视觉系统、Repository Overview、Module Explorer、Architecture Workspace、探索筛选能力、API Map、跨页面 Source Navigation，以及本地 Web / 打包安全边界已建立。
- **T-0801 至 T-0804** 已完成，确定性重要度信号、文件级推荐阅读路径、来源可回溯解释、只读 projection 和正式 Start Here 页面已经建立，M8 已关闭。
- **T-0901** 已完成；最小 Spring fixture 的 Controller、Service、Repository、API、基础确认依赖链、位置、Evidence 和稳定 ID 已纳入端到端回归。
- **T-0902** 已完成；多模块 fixture 已覆盖 inheritance / aggregation 分离、模块源码归属、声明依赖控制的跨模块类型可见性、稳定内部模块边，以及缺失外部 parent/BOM 的 `PARTIAL` 路径。
- **T-0903** 已完成；复杂 Spring fixture 已覆盖多 Controller、Configuration、多路径与条件 mapping、Service-to-Service 链和循环，以及多构造器、接口多实现和 unsupported/ambiguous 场景的非猜测结果；UI 报告与 Start Here projection 固定来自同一报告快照。
- **T-0904** 已完成；固定 Spring Petclinic commit 的小型真实仓库验证已核对项目、组件、全部 17 个 Endpoint 和 6 个构造器依赖，并记录 3 个 Spring Data Repository 与连带 6 条确认依赖漏报、10 条可解释诊断、9.302 秒冷启动及 130.14 MiB 峰值工作集。
- **T-0905** 已完成；固定 JHipster Sample Application commit 的中型真实仓库验证已核对 30 个报告组件、24 个 Endpoint、7 条确认依赖和 19 条诊断，修复合法空 Maven 属性造成的整次分析崩溃，并记录 wildcard import 连带漏报、9.761 秒冷启动、181.53 MiB 峰值工作集和筛选后的图可读性。
- **T-0906** 已完成；固定 ThingsBoard `v4.3.1.5` commit 的大型仓库试验完成 59 个 active module、3,834 个 main Java file 的分析，无 parse failure、OOM 或 server crash；记录 25.498 秒冷启动、648.23 MiB 进程峰值，以及离线模型、Spring 语义、诊断规模和图/API UI 的实际上限，不承诺完整支持。
- **T-0907** 已完成；固定 `joyheros/realworld` commit 的 5 项首次接触任务记录了 5–10 分钟总耗时和混合结果：API 定位最快，Start Here 能导航到前三个文件，但入口、模块、依赖和推荐原因仍存在理解障碍。Overview 已统一为浅色主题，Start Here 已在卡片内显式展示全部原因。
- **T-1001** 已完成；构建产出包含运行依赖与前端资源的 `repoonboard.jar` 和 SHA-256，Windows/POSIX 启动脚本校验 Java 21、定位 JAR 并透传参数，中英文 README 已给出源码候选的安装、运行与校验步骤。
- **T-1002** 已完成；参数错误、FAILED、PARTIAL 和常见不支持场景使用稳定退出码与可操作文案，CLI 与 UI 展示同一组报告 Diagnostic，异常边界不回显内部错误或配置值。
- **T-1003** 已完成；中英文 README 已按当前 `0.1.0-SNAPSHOT` 状态重写，覆盖源码安装、内置 fixture 演示、实际功能、支持范围、PARTIAL 语义、验证证据、已知限制、路线及贡献/License 边界，并明确尚无 Release、release tag 或项目 License。
- **T-1004** 已完成；V0.1 公开 Demo 固定为 Apache License 2.0 的 `jhipster/jhipster-sample-app` commit `e06e87abe0be8a3a194381ce651164a734811b3f`。演示指南记录可复现 checkout、1 module / 81 source file / 30 component / 24 endpoint / 7 confirmed edge / 24 Start Here item 的真实 `PARTIAL` 快照，以及 Overview、Architecture、API、Start Here 四页面路径；分析前后第三方 checkout 均无源码变更。
- **T-1005** 已完成；README 已嵌入 960 × 600、5 帧、14.4 秒的 Demo GIF，使用固定 JHipster commit 和 SHA-256 已记录的当前打包候选，依次展示真实扫描摘要、Overview、Architecture、API 与 Start Here。`PARTIAL`、coverage warning、unresolved 计数及确定性 Evidence 均保留，媒体文档明确压缩时长不代表扫描耗时。
- 当前下一任务是 **T-1006 — License**。
- Java 21 / Maven 3.9.16 当前运行 82 项前端测试和 168 项 Java 测试并全部通过；T-0901 至 T-0907 均不构建或执行 fixture/目标应用。`clean verify` 会从最终可执行 JAR 启动独立进程，用空 Maven 缓存离线分析固定 fixture，读取本地报告路由并生成 SHA-256；它仍不构建或执行被分析项目。

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

### M9 — Regression & Real Repository Validation

- **T-0901**：审计 `spring-analysis-project` 最小 Spring fixture，为真实源码增加单一 `GET /users/{id}` 与 Controller → Service → Repository 构造器注入链；新增端到端回归，固定公共报告中的项目、模块、组件、Endpoint 和依赖稳定 ID，以及 1-based SourceLocation、原始 Evidence 和规则 ID。回归使用空本地仓库直接调用静态分析器，重复结果一致，fixture 不生成构建产物且文件集合保持不变。
- **T-0902**：审计 `multi-module-project`，补充 `api/code/java` 与 `library/src/main/java` 的最小源码，使 `api` 通过精确 Maven 依赖解析 `LibraryType`；端到端回归分别固定 Maven parent 与 aggregation、模块/源码稳定 ID、source root 归属、跨模块可见性和确认内部模块边。新增 `multi-module-partial-project`，确认空本地仓库下缺失外部 parent 与 BOM 时两个聚合子模块及 raw facts 仍保留，报告返回带文件和行号的 `PARTIAL` 诊断。
- **T-0903**：新增 `spring-complex-project` 组合 fixture 和端到端回归，固定 13 个组件、12 个 Endpoint、确认的 Controller → Service / Service → Service / Service → Repository 关系及双向 Service 循环。多构造器歧义、PaymentPort 多实现、unsupported setter 注入、本地同名 mapping 注解、未解析 path/condition 均保留诊断或不确定状态且不生成猜测边；公共报告和 Start Here JSON 重复序列化一致、项目/schema/status 对齐，推荐路径只引用同一报告中的确认实体。
- **T-0904**：固定 `spring-projects/spring-petclinic` commit `818c4136ea971c21674525f9053de0d9c7ad8cfe`，在空本地 Maven 模型仓库下完成真实仓库离线验证。项目检测、10 个直接标注组件及全部 17 个 Endpoint 与源码一致；3 个无直接注解的 Spring Data Repository 和连带 6 条确认注入边记录为漏报，分析仍保留 6 条 unresolved 事实和解释性诊断，不生成猜测边。完整记录见 `docs/validation/T-0904-SPRING-PETCLINIC.md`。
- **T-0905**：固定 `jhipster/jhipster-sample-app` commit `e06e87abe0be8a3a194381ce651164a734811b3f`，在空本地 Maven 模型仓库下完成中型真实仓库离线验证。30 个报告组件、24 个 Endpoint 和 7 条确认依赖均为真实事实，9 个外部注入目标保持 unresolved；修复空 Maven 属性导致的分析崩溃。3 个 wildcard-import Controller 及连带 13 个 Endpoint、7 条确认依赖记录为漏报。Architecture 按类型筛选和一阶邻域后可读，隐藏/未解析计数保持可见。完整记录见 `docs/validation/T-0905-JHIPSTER-SAMPLE-APP.md`。
- **T-0906**：固定 `thingsboard/thingsboard` release `v4.3.1.5` commit `2cd31ac359214f94b945546830da503ddf80070f`，在空本地 Maven 模型仓库下完成大型真实仓库边界试验。59 个 active module、3,834 个 main Java file 和 581 个 Endpoint 成功进入报告；无 Java parse failure、OOM 或 server crash。记录 25.498 秒冷启动、648.23 MiB 进程峰值、1,719 条诊断，以及 Maven/Spring 语义覆盖、重复模块名、API 全量列表和图预算内仍可能不可读的限制。完整记录见 `docs/validation/T-0906-THINGSBOARD-LARGE-TRIAL.md`。
- **T-0907**：固定 `joyheros/realworld` commit `2d944dbce5e89c6efdf59026f9b38ba014e73288` 进行 5 项首次接触任务。参与者报告总耗时 5–10 分钟，API 定位最快，能找到 Start Here 前三个文件但未发现原因；其余任务缺少可验证答案。本任务修复 Overview 暗色与其他页面不一致、Start Here 原因不易发现两个直接问题；完整记录见 `docs/validation/T-0907-ONBOARDING-VALUE.md`。

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
- 集中 tokens 管理色彩、字体、间距、边框、圆角、阴影和布局尺寸；Overview 与其他主页面统一使用浅色工作区，响应式布局覆盖宽屏、紧凑桌面与窄屏模式。
- 浏览器无法自动打开时服务保持运行并给出地址；Ctrl+C 后服务线程和端口释放。

## 4. 当前架构与核心模块

项目仍是单模块、单进程 Java 应用，前端在构建时打包进 JAR：

```text
RepoOnboard
├── repoonboard / repoonboard.cmd
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
- **发布构建**：Maven Shade 生成带 CLI `Main-Class` 的单一 `target/repoonboard.jar`；verify 阶段验证 JAR 内 UI、运行依赖、离线 fixture 报告路由和 SHA-256。根目录 Windows/POSIX 启动脚本要求 Java 21，可用于发布目录或源码 checkout。

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
- T-0804 从同一不可变报告快照生成独立、版本化、固定只读的 `/api/start-here` projection；公共报告 schema `1.2` 保持不变，Vue 不重复实现启发式。正式页面展示稳定文件顺序、全部原因、模块与 Source Evidence，支持默认/完整列表、PARTIAL/空结果说明，并只在 supporting entity ID 与公共报告精确连接时提供 Component / Endpoint 跨页面导航。实现保留 Stitch 的编号路径与 Inspector 信息层级，同时移除远程 CDN、示例运行状态、硬编码仓库值和不可证实动作。
- T-0901 将早期分散的 Spring 组件与 API fixture 覆盖收束为一个最小端到端报告回归；不再依赖手工构造 Endpoint 来证明最小 fixture 的公共输出，并明确验证离线、非执行式测试边界。
- T-0902 将原本只有 POM 的多模块 fixture 扩展为可验证源码归属和跨模块类型可见性的回归基线，并把缺失外部 parent/BOM 的容错场景隔离到独立 companion fixture；成功与 `PARTIAL` 语义不再混合。
- T-0903 将此前分散在 injection、dependency、MVC 和 Start Here 单元 fixture 中的复杂模式组合为同一端到端报告回归；确认关系、循环和条件 Endpoint 可进入公共输出，不确定关系只影响覆盖状态，不会进入 Architecture 确认边或 Start Here 排名。
- T-0904 首次用固定真实仓库量化 V0.1 的准确性和成本：Spring Petclinic 的直接注解与 Endpoint 结果准确，离线外部 parent 和 Spring Data 接口限制均通过 `PARTIAL`/unresolved 明示；冷启动与本地 UI ready 用时 9.302 秒，峰值工作集 130.14 MiB。
- T-0905 用中型 JHipster 仓库验证失败容忍度、诊断噪声、性能和图筛选。Maven 属性读取现在把空值视为未提供的报告 metadata，同时仍允许空值参与其他属性插值，避免合法 POM 使分析整体失败；冷启动与本地 UI ready 用时 9.761 秒，峰值工作集 181.53 MiB。
- T-0906 用大型 ThingsBoard 仓库量化当前边界：3,834 个 main Java file 均完成解析，但离线 Maven model 和 Spring 语义解析产生 1,719 条诊断；冷启动与本地 UI ready 用时 25.498 秒，浏览器核验后进程峰值工作集 648.23 MiB。最大模块被图预算安全暂停，但 52 节点/77 关系的预算内图仍可能形成 hairball，说明数值门槛不能保证语义可读性。
- T-0907 用固定的 realworld 仓库和 5 项任务完成首次接触验证。参与者在 5–10 分钟内最快找到 API，并找到前三个推荐文件，但未发现推荐原因，对入口、模块和依赖仍困惑。Overview 已统一为浅色，Start Here 卡片已直接显示全部原因；没有手工阅读对照组，因此结果只证明部分可观察价值，不证明整体提速。
- T-1001 将原有瘦 JAR 改为带 CLI 入口、运行依赖与离线前端的单一可执行 JAR，新增 Java 21 门禁和参数透传一致的 Windows/POSIX 启动脚本。完整 verify 直接运行该 JAR 离线分析 fixture、访问报告 API 并生成 SHA-256；README 中英文版本记录当前源码安装与未来 release 文件布局。Windows Maven Wrapper 对普通 `.m2` 目录读取空链接目标的启动问题也已修复。
- T-1002 为 CLI 参数、项目识别、分析状态和本地 UI 启动建立稳定错误码、上下文与下一步动作；非 Maven 或确认无 Spring Boot 的项目明确失败，无法确认 Spring Boot 的不完整 Maven 模型保持 `PARTIAL`。终端现在从公共报告读取与 UI 相同的 Diagnostic 字段，前端保留 stage、module 和 source，异常消息只暴露安全分类而不回显原始内部内容。
- T-1003 将 README 从混合“计划能力/开发状态”的长说明整理为可直接执行的双语 V0.1 入口。干净源码构建明确区分运行与构建依赖及首次联网成本；真实仓库误报、性能、UI 密度和 onboarding 证据边界均公开说明。由于项目 License 尚未选择，文档不再把项目称为 open source。
- T-1004 选定固定 JHipster Sample Application commit 作为公开 Demo 输入，并将许可证校验值、确定性获取方式、空 Maven cache 的预期 `PARTIAL` 报告及四页面讲解路径固化到文档。实测报告和 Start Here API 均返回 HTTP 200；分析只读取第三方 checkout，未修改、构建或运行目标应用。
- T-1005 从当前 `target/repoonboard.jar` 和 T-1004 固定输入捕获本地 UI，将扫描摘要与四个主要页面压缩为 14.4 秒循环 GIF。Architecture 聚焦 `UserService` 的两条确认边，API 选中 `POST /api/bank-accounts`，Start Here 显示推荐原因与 Evidence；所有页面保留 `PARTIAL`，录制记录固定源码/JAR/GIF 哈希并声明不是性能基准。
- 后续前端页面统一采用“Codex 数据契约审计与 Stitch 方案 → 用户 Stitch 设计 → Codex Vue 实现 → Browser Validation”，用户设计完成前不提前编码页面。
- README 已提供中英文版本和语言切换，并更新为当前本地 UI 启动方式。

## 7. 已知限制 / 技术债

- **Mapper 未实现**：T-0404 的 MyBatis / MyBatis-Plus Mapper 专用识别仍为可选延后项。
- **分析范围有限**：不支持 Java/Maven/Spring Boot 以外的生态；Maven 不联网、不执行插件/生命周期、不计算传递依赖；Java/Spring 采用保守静态分析，不覆盖运行时代理、反射和动态注册。
- **Source Navigation 边界**：Source Navigation 已作为 Architecture / API 的跨页面辅助表面交付，不是独立一级导航，也不读取源码正文或提供 IDE 动作。
- **图规模受限**：筛选后的范围超过 60 个组件或 120 条确认关系时，页面明确显示匹配与隐藏数量并停止绘图；用户可继续通过完整搜索列表选择组件，或用组件类型与所选组件一阶邻域缩小范围。
- **Spring Data Repository 识别缺口**：当前组件规则不把仅继承 Spring Data repository 基类且无直接 `@Repository` 的接口识别为 Repository；Spring Petclinic 因此漏报 3 个组件，6 条真实构造器注入边只保留为 unresolved。
- **Wildcard import 组件漏报**：当前注解解析器在同一文件存在多个 wildcard import 时保守判为 ambiguous，即使只有一个 wildcard 包能提供已知 Spring 注解。JHipster Sample Application 因此漏报 3 个直接 `@RestController`、13 个 Endpoint 和 7 条项目内确认依赖；每个文件均有明确诊断，不生成误报。
- **真实仓库 UI 密度**：中型 JHipster 仓库的 30 节点 Architecture 默认图较密且外侧标签可能裁切；按组件类型筛选会持续显示 hidden/unresolved 计数，选中组件后一阶邻域可生成清晰的小图。API 宽表的 source 列在 1280 px 下仍可能需要横向滚动。当前均不阻塞发布，但应写入 V0.1 使用说明。
- **大型仓库边界**：ThingsBoard 试验在 3,834 个 main Java file 上完成但进程峰值达到 648.23 MiB，并产生 1,719 条诊断；当前诊断视图不适合大规模逐项 triage。重复 Maven leaf module 名在选择器中没有路径区分，581 个 API row 一次性渲染；52 节点/77 关系的预算内图仍可能不可读。V0.1 不承诺完整支持同规模仓库。
- **Onboarding 证据有限**：首次接触者能较快定位 API 和 Start Here 前三个文件，但入口、模块和 unresolved 依赖任务仍令人困惑；没有逐项计时、完整答案记录或手工源码对照组。英语产品文案可能是干扰变量，选择性界面翻译继续留在 V0.2 候选。V0.1 不宣称已经量化证明比手工阅读更快。
- **构建环境**：从源码构建目前需要兼容锁定 Vite 工具链的 Node.js；发布产物的最终用户不需要 Node。
- **原生平台发布检查尚未完成**：Windows 启动脚本与 Git Bash 下的 POSIX 启动脚本已实际执行；M10 出口仍要求在原生 macOS/Linux 环境完成 release smoke checks。
- **前端包体警告**：单一生产 JavaScript 产物约 602 kB（gzip 约 190 kB），Vite 会提示超过 500 kB；T-0709 已确认离线 JAR 可正确加载，是否拆包应在真实仓库性能数据表明必要时再决定。
- **发布尚未就绪**：当前仍为 `0.1.0-SNAPSHOT`；项目 License、GitHub 展示清理、原生跨平台 smoke checks 和正式 V0.1 Release 尚未完成。

## 8. 未完成任务

- **可选延后**：T-0404 — Mapper Detection。
- **M10**：T-1001 至 T-1008，完成安装、错误体验、发布文档、演示、License、GitHub 清理和 V0.1 发布。
- **V0.2 候选**：Desktop Application 技术试验与打包；Settings 中提供 English / 中文界面切换，只翻译产品导航、说明、状态、空结果和错误提示，代码标识符、文件路径、类名、API、框架术语及原始 Evidence 保持原文。该候选不进入当前 M9。

## 9. 下一步

下一项开发任务：

> **T-1006 — License**

选择项目 License，补齐根目录许可文件并同步中英文 README 的复用、贡献和 License 说明。

## 10. 给下一次开发会话的上下文

RepoOnboard 是本地优先、确定性、可解释的陌生代码库理解工具；V0.1 只支持 Java 21 + Maven + Spring Boot，不使用 LLM、云服务或数据库。工作前按 `PROJECT.md` → `DECISIONS.md` → `TODO.md` → `AGENTS.md` → `STATE.md` → 当前代码阅读。M0–M9 与 T-1001 至 T-1005 已完成，公共报告为 schema `1.2` 且兼容读取 `1.0` / `1.1`。本地 Vue UI 已有统一浅色 Shell、Repository Overview、Module Explorer、Architecture Workspace、API Map、Start Here，以及从 Component / Endpoint 进入的 Source & Evidence Detail。发布构建生成带运行依赖和离线 UI 的 `target/repoonboard.jar`、SHA-256，并由 Windows/POSIX 启动脚本提供 Java 21 门禁；verify 会从 JAR 离线分析 fixture。CLI 退出码为成功 0、失败 1、参数错误 2、部分成功 3，终端与 UI 消费相同的报告 Diagnostic。公开 Demo 固定到 Apache License 2.0 的 JHipster Sample Application commit，并公开可复现的 `PARTIAL` 结果与四页面路径；README 已嵌入 14.4 秒演示 GIF，录制记录固定打包输入与媒体哈希且不声称扫描耗时。尚无 Release 或项目 License。V0.2 候选新增 Settings English / 中文界面切换，且只翻译产品文案、不翻译代码与 Evidence。下一任务是 **T-1006 — License**。每个 T 应独立测试、更新 TODO/STATE、提交并推送 GitHub。
