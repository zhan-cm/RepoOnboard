# T-0002 — RepoOnboard V0.1 架构评审

日期：2026-09-08

评审对象：[V0.1 技术架构方案](V0.1-TECHNICAL-PROPOSAL.md)

结论：APPROVED FOR ADR DRAFTING WITH VALIDATION GATES

后续状态：用户于 2026-09-08 确认继续执行；T-0003 已将通过评审的方向写为 ACCEPTED ADR。

这里的“接受”表示该技术方向通过 T-0002 评审，可以在 T-0003 中形成 PROPOSED ADR；它不把 DECISIONS.md 中的选择自动标记为 ACCEPTED。最终架构状态仍以 DECISIONS.md 的 ADR 为准。

## 1. 范围审查

评审确认方案保持在 Java + Maven + Spring Boot V0.1 内，使用本地静态分析，不引入 LLM、远端服务、数据库、动态插件系统、运行目标应用或完整方法调用图。核心模型不暴露 Maven、JavaParser 或 Spring 类型，框架规则位于专用分析边界，未把未来多语言目标实现成当前插件框架。

方案是一体化本地应用：一个 Java 进程、初期一个 Java 构建模块、一个随包发布的前端。包级边界足以支持当前阶段。实际代码出现反向依赖时，应在实现任务内修正；目前没有拆成多构建模块的证据。

## 2. 逐项评审结论

| 技术问题 | 评审结论 | 理由与约束 | ADR 处理建议 |
| --- | --- | --- | --- |
| Java Runtime 21 | 接受 | LTS 基线，与当前测试栈匹配；只约束工具自身 | 建立 ADR；被分析项目版本另列兼容矩阵 |
| Maven + Wrapper | 接受 | 与首个目标生态一致，贡献者命令简单 | 建立 ADR；Maven 4 迁移延后 |
| Picocli | 接受 | CLI 有帮助、退出码、路径与后续选项需求，手写解析收益低 | 可与 CLI/打包合并成 ADR |
| Maven Model Reader + Builder | 有条件接受 | 正确处理继承与插值所需；必须通过受限来源、受控 profile 和 raw 回退保证离线与部分成功 | 单独建立 ADR，并写入 M1/M2 验证门槛 |
| JavaParser | 接受方向，需实现验证 | AST 和源码位置满足主要需要；当前未对目标 fixture 运行实际库 | 建立 PROPOSED ADR；T-0303 前锁版本并验证语法/位置/容错 |
| 受限项目内符号解析 | 接受 | 符合准确优先；全 classpath 与编译器级解析会扩大范围 | 与 Java AST 分开记录，以便单独重审深度 |
| 最小通用模型 | 接受 | 实体直接服务 V0.1，且未做通用 AST | 单独建立 ADR；字段在 T-0601 固化 |
| 单模块、分包边界 | 接受 | 当前没有多模块构建的即时收益；显式编排便于测试 | 可和分析器边界合并成 ADR |
| 边列表 + 按需邻接表 | 接受 | V0.1 只需筛选、统计、邻域和基础中心性 | 记录不引入 Java 图数据库/图库；复杂算法出现时重审 |
| JSON + Jackson | 接受方向 | 人可读、UI 直接消费、适合版本化 DTO | ADR 固定 JSON 契约；Jackson 精确版本在 M1 核验 |
| JDK HttpServer | 接受方向，需冒烟验证 | 本地只读路由需求很小，嵌入完整服务框架偏重 | 建立 ADR；M7 验证并发、关闭、路径和安全头 |
| Vue + JavaScript + Vite | 接受 | 四个交互视图、筛选与详情状态使小型 UI 框架合理 | 可和本地 Web 架构分开记录；不引入 TypeScript 分析能力 |
| Cytoscape.js | 有条件接受 | 图交互与选择布局成熟，但真实图可读性尚未证明 | 标为 PROPOSED；M7/M9 以局部图可读性作为门槛 |
| JUnit + Vitest + Playwright | 接受分层策略 | 各层随对应功能引入；避免 M1 一次加入所有前端测试依赖 | 测试策略 ADR；精确版本由构建锁定 |
| 可执行 JAR + 脚本 | 接受 | 最小可交付方式；与 Java 21 运行要求一致 | 与 CLI/构建形成打包 ADR；正式渠道延后 |

## 3. 评审中接受的设计修订

Maven 读取边界原本只强调 ModelResolver。最小实验确认 `relativePath` 本地父 POM可以经 ModelSource2 直接读取，绕过外部坐标解析器。因此所有根 POM、相对父 POM、本地仓库 POM 和 BOM 必须进入同一个受限读取层，并在来源关联和实际打开两个时刻校验真实路径。

最小实验也确认，仅清空 `systemProperties` / `userProperties` 不会禁用宿主 OS profile 激活。方案改为显式控制 ProfileSelector：不注册 OS、JDK、属性和文件激活器，同时保留显式 profile 与 activeByDefault。此模式不是 Maven CLI 环境的完整复现，所以报告必须标出受限 profile 语义和 PARTIAL 状态。

输出安全边界保留，但实现应从最小可验证规则开始：绑定 loopback、固定只读路由、路径归一化、源数据按文本渲染和 CSP。随机会话路径是否必要留到 M7 威胁验证，不作为当前核心架构承诺。

稳定 ID 的需求接受，具体“规范化 JSON + SHA-256”算法暂不固化为架构决策。T-0602 应用 fixture 验证碰撞、排序和重命名行为后再确定编码；ADR 只要求 ID 来源于稳定结构身份，不依赖内存地址或遍历顺序。

## 4. 明确拒绝的方案

- 拒绝调用被分析项目的 `mvn help:effective-pom` 或生命周期作为核心解析路径。它依赖项目环境，也可能加载扩展或插件。
- 拒绝纯手写 XML 解析来复刻完整 Maven 继承、插值、profile 和 dependencyManagement 语义。
- 拒绝 V0.1 默认下载完整依赖 classpath，或追求近编译器级符号/调用解析。
- 拒绝把接口注入静态绑定到唯一实现；候选实现不是已证实的运行时注入边。
- 拒绝将第三方 AST、Maven Model 或 Spring 类型放入公共项目模型。
- 拒绝为未来语言建立动态插件运行时或现在拆成多个发布服务。
- 拒绝默认展示全仓库组件图。架构视图必须先按模块和邻域缩小。
- 拒绝为 V0.1 首发采用 Native Image、自带 JRE、数据库或多包管理器同时发布。

## 5. 明确延后的问题

- Maven 4 模型 API 迁移、Gradle 支持、远端 POM 下载和与用户 Maven settings 的完整一致性。
- 完整 classpath、外部库符号、方法调用图、运行时 Bean 选择、Qualifier/Primary/条件 Bean、XML Bean 和生成代码语义。
- 自定义 Spring 组合注解、AliasFor、插件动态源码根、Spring Data 自动注册和 Mapper 的扩展覆盖。
- 精确稳定 ID 哈希格式、JSON 次版本兼容规则、图节点阈值和复杂布局插件。
- Native Image、自带 JRE、安装器、包管理器和云/协作分发。
- 缓存、增量分析、并行解析和复杂图算法，等待性能数据。

## 6. 实验与证据

评审增加了隔离的 [Maven API 实验](experiments/README.md)，不是产品实现。实测环境为 Windows 11、Java 21.0.6 和本地 Apache Maven 3.6.1 模型库。运行方式未执行 Maven lifecycle，也没有配置网络 resolver。

实验 8 项断言通过：基础插值、缺父模型诊断/raw 读取、宿主 OS profile 反例、受控 activeByDefault、显式 profile、relativePath 绕过解析器的反例、有界 ModelSource2 拒绝逃逸，以及 DTD 拒绝。该实验验证 API 行为边界，尚未验证最终推荐的 Maven 3.9 版本、完整本地坐标 resolver 或生产级 XML 安全。

本机环境核对：Java 21.0.6、Maven CLI 3.6.1、Node 24.12.0、npm 11.6.2。它只说明当前开发环境可以运行 Java 21 和未来前端工具，不替代 Windows/macOS/Linux 发布验证。

## 7. 必须保留的验证门槛

| 时点 | 必须完成 |
| --- | --- |
| M1 固定依赖时 | 用锁定的 Maven 3.9、JavaParser、Jackson、JUnit 版本建立最小构建；重跑对应 API 冒烟测试 |
| T-0202/T-0203 | 完整受限读取层、XXE/DTD/资源上限、parent/BOM/profile/local repository fixture，无网络且不执行目标项目 |
| T-0303 | Java 8/11/17/21 fixture 的语法、位置和单文件容错；据结果确认或重审 JavaParser |
| T-0403/T-0503 | 同名类型、跨模块可见性、接口多实现和多构造器，确保不产生猜测边 |
| T-0701/T-0704 | loopback HTTP、路径穿越、内容注入、关闭流程和局部图节点预算 |
| T-0904 以后 | 固定真实仓库 commit，人工核对事实样本、误报、漏报、耗时、内存和可读性 |

如果锁定 Maven 3.9 版本无法复现当前受限模型构建方法，Maven 解析方案回到 T-0002 重新评审；如果 JavaParser 在目标语法或位置准确性上形成系统性阻塞，则以同一 fixture 比较 Spoon 与 JDT；如果 Cytoscape 局部图在中型真实仓库仍不可读，先调整信息分层，再评估额外布局库。

## 8. T-0002 验收结果

- [x] 15 项核心技术选择均已评审。
- [x] 可进入 ADR 的方案及验证条件已明确。
- [x] 拒绝方案及理由已明确。
- [x] 延后问题及重新评估时点已明确。
- [x] 方案未扩大 V0.1 范围，未创建正式产品实现。

评审结论允许进入 T-0003 的 ADR 起草阶段。后续明确确认已取得，正式决策见根目录 DECISIONS.md；技术方案文档本身继续保持 PROPOSED，正式约束以 ADR 为准。
