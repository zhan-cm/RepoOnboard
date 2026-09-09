# 当前项目状态

## 1. 当前阶段

RepoOnboard 当前进入 **M7 — Local Web UI（本地 Web 界面）**。

- M0 至 M6 已完成。
- T-0601 至 T-0604 已完成，稳定报告、身份、版本化 JSON 与 Overview summary 已形成完整边界。
- M7 尚未开始，当前下一任务是 **T-0701 — Local UI Bootstrap & Minimal App Shell**。

当前验证状态：

- Java 21 离线完整 `clean verify` 通过，共 127 个测试，JAR 构建成功。
- T-0604 验证了按稳定身份去重的 Summary、Controller 类型合并、PARTIAL 覆盖限制、JSON 一致性门禁及 schema `1.0` 向 `1.1` 的兼容读取。

## 2. 已完成任务

### M0 — Technical Architecture

- **T-0001 — Complete Technical Architecture Proposal**：完成 V0.1 技术架构提案，明确本地静态分析流水线、公共模型和本地展示方向。
- **T-0002 — Architecture Review**：完成架构评审，确认范围、风险、边界和验证策略。
- **T-0003 — Create Initial ADRs**：建立并接受 ADR-0001 至 ADR-0016。
- **T-0004 — Define V0.1 Milestone Boundaries**：确定 M1 至 M10 的依赖、交付物和出口条件。

### M1 — Project Foundation

- **T-0101 — Create Repository Scaffold**：建立单模块 Java 项目及 `analysis`、`analyzer`、`cli`、`core`、`serialization`、`web` 边界。
- **T-0102 — Configure Build**：配置 Java 21、Maven Wrapper、固定插件版本、编码和可复现构建时间戳。
- **T-0103 — Establish Testing Foundation**：建立 JUnit 6 测试基础和可复用 fixture 路径约定。
- **T-0104 — Create Minimal CLI Entry**：使用 Picocli 建立 `repoonboard` CLI 入口、参数校验和退出码语义。
- **T-0105 — Establish Core Evidence and Diagnostic Contracts**：实现 `SourceLocation`、`Evidence`、`Diagnostic`、严重级别及 `SUCCESS / PARTIAL / FAILED` 状态模型；路径为扫描根相对路径，位置为 1-based。

### M2 — Maven Project Analysis

- **T-0201 — Maven Project Detection**：仅在扫描根存在 `pom.xml` 时确认 Maven 项目，并保留来源证据。
- **T-0202 — Basic Maven Metadata Extraction**：提取 groupId、artifactId、version、packaging 的原始值、解析值、状态和来源位置。
- **T-0203 — Restricted Maven Model and Property Resolution**：通过受限 Maven Model Builder 离线解析项目、相对 parent、本地仓库 parent/BOM、属性和受控 profile；拒绝网络、DTD/外部实体、越界路径和不受控模型来源。
- **T-0204 — Multi-module Maven Analysis**：分析嵌套与 profile 模块、聚合关系、parent 继承和源码目录；损坏、缺失、重复、循环或越界模块产生诊断并尽量继续。
- **T-0205 — Dependency Extraction**：提取声明/继承依赖及 groupId、artifactId、version、scope 的解析状态和字段证据；不计算传递依赖。
- **T-0206 — Spring Boot Build Metadata Detection**：依据 Boot parent、导入 BOM 或确认的 Boot 依赖识别构建信号，并保留版本与来源证据；构建信号与应用入口分离。

### M3 — Java Source Analysis

- **T-0301 — Source Root Discovery**：从 Maven 模块和 `build.sourceDirectory` 发现模块归属明确的 Java 源码根。
- **T-0302 — Java File Discovery**：发现 `.java` 文件并赋予扫描根相对、模块感知的稳定文件身份。
- **T-0303 — Basic AST Parsing**：用 JavaParser 提取 compilation unit、package、import、类型、字段、构造器、方法、参数、注解和源码位置；单文件损坏时保留其他结果。
- **T-0304 — Import and Type Reference Extraction**：提取并解析项目内可确认的类型引用；歧义或未知引用保持明确状态，不猜测外部语义。
- **T-0305 — Java Declaration Index**：建立模块感知的类型声明索引，支持跨模块确认与歧义检测。

### M4 — Spring Boot Analysis

- **T-0401 — Spring Component Detection**：识别标准及项目内组合注解形成的 Controller、RestController、Service、Repository、Component，并保留证据。
- **T-0402 — Configuration and Application Entry Point Detection**：识别 `@Configuration`、`@SpringBootApplication` 配置类与应用入口，构建元数据不冒充源码入口。
- **T-0403 — Dependency Injection Detection**：提取受支持的构造器、字段等注入候选及其解析状态和来源证据。
- 后续修正已加入项目内组合注解解析，避免只识别直接标准注解。
- **T-0404 — Mapper Detection** 是可选且非阻塞任务，尚未实现，不计入 M4 出口阻塞项。

### M5 — API & Dependency Analysis

- **T-0501 — Spring MVC Controller Mapping**：提取类级和方法级 mapping 声明、HTTP 方法、路径、params、headers、consumes、produces 及未知条件。
- **T-0502 — Full API Path Resolution**：组合类级/方法级路径和方法，生成明确的 HTTP Endpoint；`ANY`、多路径、多方法和未解析路径保持显式状态。
- **T-0503 — Component Dependency Extraction**：从注入候选生成有类型、有方向的组件依赖事实；仅唯一确认的项目内目标形成确认边，循环被合法保留。
- **T-0504 — Dependency Ambiguity and Evidence Consolidation**：合并同一确认边的重复证据；歧义注入、歧义目标和缺失目标不被猜测，作为未确认事实及诊断保留；提供稳定的 confirmed adjacency 查询。

### M6 — Report Assembly & Serialization

- **T-0601 — Project Model Assembly**：新增 analyzer-neutral 的公共模型：`Project`、`Module`、`SourceFile`、`Component`、`Endpoint`、`EntryPoint`、`Dependency`、`AnalysisReport` 及相关枚举/条件对象；新增 `ProjectAnalysisInput` 与 `ProjectModelAssembler`，可将 M2 至 M5 的 Maven、Java、Spring、Endpoint、Dependency facts、evidence 和 diagnostics 组装为统一报告，并保留整体分析状态。Core 模型不依赖 Maven、JavaParser 或 Spring 类型。
- **T-0602 — Stable Identifiers**：新增 `StableIdentifiers`，用扫描根相对模块路径、组件限定名和 Endpoint/Dependency 结构签名生成稳定身份；公共模型的集合和 evidence 使用确定排序。报告装配时重复语义键不会被覆盖，而是保留全部实体、为后续项分配稳定后缀并生成 `REPORT_ID_COLLISION` 诊断。组件重命名、Endpoint handler/HTTP 签名及 dependency target 变化会产生新 ID。
- **T-0603 — Serialization**：使用 Jackson 3.1.4 和显式 `ReportJsonDocument` DTO 实现 `AnalysisReport` 的可读 UTF-8 JSON 序列化、反序列化及文件读写；同一主版本内允许加法式次版本兼容，未知主版本通过专用异常明确失败。输出固定使用 LF，保留 SourceLocation，并且不包含源码正文、扫描根绝对路径或多态类型元数据。
- **T-0604 — Analysis Summary**：新增由 `AnalysisReport` 实体即时派生的 `AnalysisSummary`，按稳定 ID 去重统计模块、源码文件、组件及主要组件角色、Endpoint、EntryPoint 和 Dependency。PARTIAL/FAILED 结果显式携带覆盖受限标记与相关诊断代码。JSON schema 当前为 `1.1`，Summary 作为加法字段输出并在读取时与底层实体重新核对；旧 `1.0` 报告继续可读。

## 3. 当前实现能力

项目当前已经能够在本地、离线优先地分析 Java 21 / Maven / Spring Boot 仓库：

- 从 CLI 接收仓库路径、显式 profile 和本地 Maven 仓库位置。
- 检测根 Maven 项目，读取受限模型、项目坐标、多模块层级、源码目录、依赖和 Spring Boot 构建信号。
- 发现模块所属 Java 文件，解析源码结构、注解、声明和项目内类型引用；局部错误产生诊断并尽可能继续分析。
- 识别 Spring 组件、配置类、应用入口和注入候选，包括项目内可确认的组合注解。
- 提取 Spring MVC mapping 并组合成 HTTP Endpoint，保留 HTTP 方法、路径、mapping conditions、handler 和类/方法级来源证据。
- 从注入信息建立组件依赖，区分 confirmed、ambiguous 和 unresolved；确认边支持邻接查询并允许循环。
- 将上述分析 facts 映射为统一公共 `AnalysisReport`；报告使用扫描根相对路径，不包含源码正文。
- 为 Project、Module、SourceFile、Component、EntryPoint、Endpoint 和 Dependency 生成稳定结构身份；报告顶层实体、诊断、mapping conditions 和 evidence 均规范化为确定顺序。
- 重复实体键会产生结构化诊断且所有声明继续保留，不依赖对象地址或分析器遍历顺序。
- 通过 `AnalysisReportJson` 将统一报告转换为稳定、可调试、前端友好的 schema `1.1` JSON，也可按 UTF-8 写入和读取文件；读取时重新执行 Core Model 与 Summary 一致性校验。
- 通过 `AnalysisReport.summary()` 生成可直接供 Overview 使用的稳定统计；计数不会因重复实体身份膨胀，Controller 同时覆盖 MVC Controller 与 RestController，分析不完整时不会伪装成完整统计。
- CLI 目前仍直接打印分析摘要和诊断；尚未把统一 `AnalysisReport` 和 JSON codec 接入 CLI 输出，也没有 Web UI。

## 4. 当前架构与核心模块

项目仍是一个 Maven 单模块、单进程应用，按包保持清晰边界：

```text
io.github.zhancm.repoonboard
├── cli
│   └── RepoOnboardCommand
├── analyzer
│   ├── maven
│   ├── java
│   └── spring
├── analysis
├── core.model
├── serialization
└── web
```

- **`cli`**：`RepoOnboardCommand` 负责参数、执行现有 Maven → Java → Spring → API/Dependency 流水线、文本输出、诊断和退出码。
- **`analyzer.maven`**：`MavenProjectDetector`、`MavenProjectMetadataReader`、`RestrictedModelResolver`、`MavenModuleAnalyzer`、`MavenDependencyExtractor`、`SpringBootBuildDetector` 等负责受限、离线的构建系统分析。
- **`analyzer.java`**：`JavaSourceRootDiscoverer`、`JavaFileDiscoverer`、`JavaSourceParser`、`JavaTypeReferenceResolver`、`JavaDeclarationIndex` 负责源码发现、JavaParser facts 和仅项目内确认的引用解析。
- **`analyzer.spring`**：`SpringComponentAnalyzer`、`SpringConfigurationAnalyzer`、`SpringInjectionAnalyzer`、`SpringMvcMappingAnalyzer`、`SpringEndpointAnalyzer`、`SpringComponentDependencyAnalyzer` 负责框架层 facts；`SpringComposedAnnotationResolver` 负责项目内组合注解。
- **`core.model`**：保存跨分析器公共契约，包括位置、证据、诊断、状态、统一项目实体、`StableIdentifiers` 与 `AnalysisSummary`。`AnalysisReport` 和相关值对象负责确定性集合顺序，`summary()` 从当前实体派生 Overview，避免重复存储失真。此包禁止依赖 Maven、JavaParser、Spring、CLI 或 Web 实现类型。
- **`analysis`**：`ProjectAnalysisInput` 汇集各分析器输出，`ProjectModelAssembler` 将其转换成公共 `AnalysisReport`，处理实体键冲突和稳定后缀。当前该装配器是 API 边界，尚未接入 CLI 的最终输出流程。
- **`serialization`**：`AnalysisReportJson` 提供字符串及 UTF-8 文件 codec；`ReportJsonDocument` 是与 Core Model 双向映射的显式 DTO，并包含派生 Summary；`ReportSchemaVersion` 当前为 `1.1`，定义同主版本加法兼容规则；读取 `1.1+` 报告时会拒绝与实体不一致的 Summary。Jackson 未启用默认多态类型处理。
- **`web`**：目前仅有包边界说明；M7 才会实现 loopback 本地只读服务和前端。
- **测试资源**：包含 minimal Maven、multi-module、Java syntax、Spring analysis、Spring API、Spring injection、Spring dependency 等最小确定性 fixtures。

## 5. 重要技术决策

当前仍有效的决策如下：

- **ADR-0001 / 0002 / 0003**：Java 21；Maven + Maven Wrapper；Picocli CLI。
- **ADR-0004**：Maven 使用受限 Model Builder；不执行生命周期、插件或扩展，不联网解析，外部模型只允许本地仓库受控读取。
- **ADR-0005 / 0006**：JavaParser 提取 Java facts；符号解析只确认项目内可证明的类型，未知和歧义不得猜测。
- **ADR-0007**：公共模型保持最小且框架中立，实体保留 `SourceLocation`、`Evidence`、`Diagnostic`；默认不包含源码正文或用户绝对路径。
- **ADR-0008**：Maven、Java、Spring、Core、Presentation 保持显式边界，但 V0.1 仍为一个本地进程，不引入插件运行时或微服务。
- **ADR-0009**：依赖采用有类型、有方向、带解析状态和证据的边；Maven 依赖与组件依赖分图；循环合法，未解析目标保留但不冒充确认边。
- **ADR-0010**：报告使用 UTF-8、带 `schemaVersion` 的 JSON；Java 端采用 Jackson 3 和显式 DTO，禁止任意多态反序列化，集合稳定排序。当前 schema `1.1` 对同主版本的加法式字段变化保持兼容，未知主版本明确失败，`1.0` 报告仍可读取。
- **ADR-0011 / 0012**：本地 UI 使用 JDK `HttpServer` 绑定 loopback；前端使用 Vue、Vite、Cytoscape.js，资源随离线产物提供，不依赖运行时 CDN。
- **ADR-0013**：Start Here 使用可解释、确定性的启发式，不使用不透明 AI 排名。
- **ADR-0014**：测试采用 unit → fixture → integration → real repository 的分层策略；分析器能力实现时即增加最小 fixture，M9 再做综合回归和真实仓库验证。
- **ADR-0015**：优先部分成功；单文件或局部结构失败应保留可用结果，并返回结构化诊断和 `PARTIAL` 状态。
- **ADR-0016**：V0.1 最终打包为 JAR 和启动脚本。
- **ADR-0017**：V0.1 采用 Web-first、Desktop-ready 边界；页面保持宿主无关，但宿主适配只在首个真实调用方出现时最小引入。Desktop Application、原生安装器和自带 Java Runtime 延后为 V0.2 候选，不阻塞 V0.1。
- 产品级约束继续有效：V0.1 只支持 Java + Maven + Spring Boot；静态、确定性、本地、可解释优先；不引入 LLM、RAG、云服务、数据库或自动改码能力。

## 6. 重要变更

- 项目从纯架构文档阶段进入可执行分析器阶段，并按 M1 → M5 建立了完整的 Maven、Java、Spring、API 和组件依赖事实链。
- README 已拆分为 `README.md`（英文）与 `README.zh-CN.md`（中文），两份文档顶部可互相切换。
- Maven 分析从简单 POM 读取升级为受限、离线 Model Builder：支持相对 parent、本地仓库 parent/BOM、显式/默认 profile、多模块与安全边界，同时明确不模拟完整 Maven 执行环境。
- Java 分析从文件发现升级为模块感知的 AST facts、声明索引和保守的项目内类型解析；解析失败改为局部诊断而非全局终止。
- Spring 识别补充了项目内组合注解解析，避免只支持直接标准 stereotype。
- API 分析从 mapping 声明拆分为“声明提取”和“Endpoint 组合”，从而保留类级/方法级证据以及无法解析的路径/条件。
- 组件依赖从注入候选升级为确认边与不确定事实分离，并加入重复 evidence 合并和稳定邻接查询。
- T-0601 新增统一公共模型和装配边界，后续 JSON、UI、Start Here 将消费 `AnalysisReport`，而不直接依赖 analyzer-specific 类型。
- T-0603 将序列化从 Core Model 中隔离到显式 DTO 边界，避免 Jackson 注解或解析器对象进入核心契约；同时固定跨平台 JSON 换行、版本门禁和 UTF-8 文件行为。
- T-0604 选择从报告实体派生 Summary 而不是保存第二份可变计数，保证 Overview 与底层模型一致；schema 通过兼容的 `1.1` 次版本新增 Summary。
- M7 已重新校准为宿主无关的本地 Web 产品：T-0701 只建立运行闭环和最小 App Shell，T-0702 正式落地视觉系统，后续再依次实现 Overview、Module Explorer、Architecture、API Map 和 Source Navigation。

## 7. 已知限制 / 技术债

- **Mapper 未实现**：T-0404 的 MyBatis / MyBatis-Plus Mapper 专用识别仍为可选延后项。
- **范围限制**：V0.1 不支持 Java/Maven/Spring Boot 之外的语言、构建系统或框架。
- **Maven 语义有限**：不下载远程 POM/构件，不执行插件/扩展/生命周期，不读取 Maven settings，不采用宿主 OS/JDK/文件/属性隐式 profile 激活，不计算传递依赖；导入 BOM 的属性不继承到项目；没有可识别依赖的间接 Boot parent/BOM 链可能不分类。
- **Java 解析保守**：不是完整编译器或全功能符号求解器；只确认项目内可证明引用，外部类型、复杂泛型或歧义保持未知。
- **Spring 静态分析有限**：只覆盖当前列明的 stereotype、配置、入口、注入和 MVC 模式；动态注册、运行时代理、反射或复杂条件配置不做推断。
- **依赖图有限**：目前是 Maven 声明关系和组件注入关系，不是方法级调用图、运行时调用图或传递依赖图。
- **报告接入尚未完成**：统一报告、Summary 和版本化 JSON codec 已存在，但 CLI 尚未使用该完整输出链；T-0701 将开始接入本地 UI 生命周期。
- **展示尚未实现**：没有本地 HTTP 服务、前端页面、图谱交互、源码跳转或 Start Here。
- **发布尚未就绪**：当前是 `0.1.0-SNAPSHOT`，没有安装体验、发布许可清理、演示资源或 V0.1 release。

## 8. 未完成任务

### 可选延后任务

- **T-0404 — Mapper Detection**：MyBatis / MyBatis-Plus Mapper 分类，非阻塞。

### M7 — Local Web UI

- **T-0701 至 T-0709**：Local UI Bootstrap & Minimal App Shell、Visual System & Product Shell、Repository Overview、Module Explorer、Architecture Workspace、Architecture Exploration & Filtering、API Map、Source Navigation、Local Web Boundary and Packaged UI Validation。

### M8 — Start Here

- **T-0801 至 T-0804**：Reading Importance Heuristics、Recommended Reading Path、Recommendation Explanation、Start Here UI。

### M9 — Regression & Real Repository Validation

- **T-0901 至 T-0907**：审计最小 Spring、多模块、复杂 Spring fixtures；完成小型、中型、大型真实仓库验证和 onboarding 价值验证。

### M10 — Release Preparation

- **T-1001 至 T-1008**：安装体验、错误消息、V0.1 README、演示仓库、GIF/视频、License、GitHub 清理和 V0.1 Release。

### V0.2 候选

- **Desktop Application**：在 V0.1 真实价值得到验证后，再进行桌面容器、Java 生命周期、宿主集成、Windows 打包与自带运行时的技术试验。

## 9. 下一步

下一项应继续开发：

> **T-0701 — Local UI Bootstrap & Minimal App Shell**

下一次只实现本地 UI 启动闭环和最小 App Shell：按 ADR-0011 将只读报告通过绑定 `127.0.0.1` 的 JDK `HttpServer` 提供给 Vue 页面，处理系统分配端口、`--no-open`、浏览器失败与关闭生命周期，并提供只能显示仓库上下文、导航占位及 loading/error 回退的最小外壳。不要提前落地 T-0702 视觉系统、Overview 页面或 Desktop runtime。

## 10. 给下一次开发会话的上下文

RepoOnboard 是本地优先、确定性、可解释的陌生代码库理解工具。V0.1 只支持 Java 21 + Maven + Spring Boot，不使用 LLM、云服务或数据库；以可执行 JAR、启动脚本和宿主无关的本地 Vue Web UI 交付，Desktop Application 延后为 V0.2 候选。工作前按 `PROJECT.md` → `DECISIONS.md` → `TODO.md` → `AGENTS.md` → `STATE.md` → 当前代码阅读。M0–M6 已完成：受限离线 Maven 分析、模块感知 JavaParser facts 与项目内类型解析、Spring 组件/配置/入口/注入/组合注解、MVC Endpoint、带证据和不确定状态的组件依赖，以及统一 `AnalysisReport`。公共模型通过 `StableIdentifiers`、确定排序和冲突诊断保持稳定；`AnalysisReport.summary()` 按稳定 ID 派生 Overview 计数并说明覆盖限制。`AnalysisReportJson` 使用 Jackson 3.1.4 和显式 DTO 提供固定 LF 的 UTF-8 schema `1.1` JSON、`1.0` 兼容读取、未知主版本错误和 Summary 一致性校验。Java 21 离线 `clean verify` 共 127 项测试通过，JAR 构建成功。CLI 尚未接入完整报告输出，也没有 Web UI。当前下一任务是 **T-0701 — Local UI Bootstrap & Minimal App Shell**：实现 loopback JDK HttpServer、只读固定路由、Vue 启动、报告读取、`--no-open`、端口/浏览器失败与关闭生命周期，以及只显示仓库上下文、导航占位和 loading/error 回退的最小 App Shell。不要提前实现 T-0702 视觉系统、Overview 页面或 Desktop runtime。每个 T 独立测试、更新 TODO/STATE、提交并推送 GitHub。
