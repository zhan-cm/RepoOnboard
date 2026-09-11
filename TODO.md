````markdown
# RepoOnboard — Development Roadmap & TODO

> Execution roadmap, milestone tracking, and current development state for RepoOnboard.

---

# 1. Purpose

本文件用于记录 RepoOnboard 当前和未来的：

- Milestones
- Development Tasks
- Task Dependencies
- Acceptance Criteria
- Progress
- Known Issues
- Deferred Work
- Validation Tasks

本文件回答：

> **What should we work on next?**

它不负责定义产品方向，也不负责重新进行技术选型。

---

# 2. Relationship with Project Files

RepoOnboard 使用三个核心项目文件：

```text
PROJECT.md
DECISIONS.md
TODO.md
````

职责如下：

```text
PROJECT.md
    ↓
What are we building?
Why are we building it?

DECISIONS.md
    ↓
How have we decided to build it?
Why did we choose those technologies?

TODO.md
    ↓
What are we doing now?
What should happen next?
```

---

# 3. Source of Truth Priority

如果不同信息发生冲突，优先级：

```text
PROJECT.md
    ↓
DECISIONS.md
    ↓
TODO.md
    ↓
Implementation
```

因此：

* TODO 不得违反 PROJECT.md
* TODO 不得擅自推翻 ACCEPTED ADR
* Work / Codex 不得通过完成普通 TODO 修改产品方向
* 技术方案发生重大变化时，应先更新 DECISIONS.md，再更新 TODO.md

---

# 4. Current Project Status

Current Phase:

> **Phase 7 — Local Web UI**

Current Product Version:

```text
V0.1 — In Progress
```

Current Implementation Status:

```text
Product Definition       ✓
Architecture Constraints ✓
Technical Decisions      ✓
Project Scaffold         ✓
Core Analysis            ✓
Web UI                   ◐
Real-world Validation    ○
Public Release           ○
```

Legend:

```text
✓ Completed
◐ In Progress
○ Not Started
⚠ Blocked
✗ Failed / Rejected
```

---

# 5. Current Priority

当前唯一最高优先级任务：

> **T-0802 — Recommended Reading Path。**

M0 至 M7 和 T-0801 已经完成。Start Here 现有一组只使用公共报告可验证 facts 的确定性重要度信号，下一步只执行 T-0802，将这些信号按文件去重并形成稳定、默认最多 10 项且可展开的阅读路径。后续任务必须遵守 DECISIONS.md 中 ADR-0001 至 ADR-0017，并按本文件的里程碑出口逐项推进。

---

# 6. Development Principles

所有 TODO 必须遵守以下执行原则：

```text
Small Task
    ↓
Clear Scope
    ↓
Implementation
    ↓
Automated Test
    ↓
Validation
    ↓
Commit
    ↓
Next Task
```

禁止：

```text
Large Vague Task
    ↓
Implement Everything
    ↓
Massive Refactor
    ↓
Unknown Result
```

---

# 7. Task Status Convention

每个任务使用：

```text
[ ] Not Started
[-] In Progress
[x] Completed
[!] Blocked
```

任务完成必须满足其 Acceptance Criteria。

仅仅“写了代码”不能认为任务已经完成。

---

# 8. Task ID Convention

任务 ID 格式：

```text
T-XXXX
```

例如：

```text
T-0001
T-0002
T-0003
```

Milestone 使用：

```text
M0
M1
M2
...
```

---

# 9. V0.1 Roadmap Overview

RepoOnboard V0.1 计划分为：

```text
M0 — Architecture
        ↓
M1 — Project Foundation
        ↓
M2 — Maven Analysis
        ↓
M3 — Java Source Analysis
        ↓
M4 — Spring Boot Analysis
        ↓
M5 — API & Dependency Analysis
        ↓
M6 — Report Assembly & Serialization
        ↓
M7 — Local Web UI
        ↓
M8 — Start Here
        ↓
M9 — Regression & Real Repository Validation
        ↓
M10 — Release Preparation
        ↓
V0.1 Release
```

## 9.1 Milestone Boundaries

| Milestone | Entry dependency | Deliverable | Exit gate |
| --- | --- | --- | --- |
| M0 Architecture | Product scope defined | Reviewed proposal and ACCEPTED ADRs | T-0001 through T-0004 complete |
| M1 Project Foundation | M0 complete | Reproducible build, minimal CLI, test base, core evidence/diagnostic contracts | Wrapper build and tests pass; CLI and core contracts have focused tests |
| M2 Maven Analysis | M1 complete | Offline-capable Maven project/module/build metadata facts | Controlled Maven fixtures pass; no target lifecycle or network resolver is used |
| M3 Java Source Analysis | M2 complete | Java source facts and module-aware declaration index | Java version/location/error fixtures pass; a broken file yields partial results |
| M4 Spring Boot Analysis | M3 complete | Spring component, configuration, entry-point and injection facts | Supported annotations and injection patterns pass fixtures; T-0404 Mapper remains optional |
| M5 API & Dependency Analysis | M4 required tasks complete | HTTP endpoint facts and confirmed typed component edges with evidence | Mapping/ambiguity fixtures pass; uncertain relationships remain unresolved |
| M6 Report Assembly & Serialization | M5 complete | Stable AnalysisReport, identifiers, summary and versioned JSON | Deterministic snapshot and compatibility tests pass |
| M7 Local Web UI | M6 complete | Host-independent local read-only UI with a unified product shell for Overview, Modules, Architecture and APIs | Browser flow, visual system, evidence display, loopback server and local security checks pass |
| M8 Start Here | M7 complete | Explainable reading guide and UI | Stable heuristic fixtures and explanation checks pass |
| M9 Regression & Real Repository Validation | M8 complete | Audited fixture suite plus small/medium/large repository findings | Regression suite passes; real-repository accuracy, limits and onboarding results recorded |
| M10 Release Preparation | M9 complete | Installable V0.1 release candidate, documentation and demo | Cross-platform smoke checks, license, cleanup and release checklist pass |

## 9.2 Dependency and Test Rules

Unless a task states an exception, tasks inside a milestone execute in task-ID order and depend on the previous task; the first task of each milestone depends on the previous milestone exit gate. A milestone closes only when all required tasks and its exit gate pass.

Every analyzer task adds the smallest relevant automated fixture while the behavior is implemented. M9 audits and combines those fixtures and performs real-repository validation; it is not the first point at which analyzer behavior is tested. Optional tasks explicitly marked non-blocking do not prevent the next milestone.

---

# 10. M0 — Technical Architecture

Goal:

> 在正式编码之前确定 V0.1 必需的核心技术方案。

Status:

```text
✓ COMPLETED
```

---

## T-0001 — Complete Technical Architecture Proposal

Status:

```text
[x]
```

Completed: 2026-09-08

Deliverable: [V0.1 Technical Architecture Proposal](docs/architecture/V0.1-TECHNICAL-PROPOSAL.md)

Validation: 文档完整性、范围和变更检查通过；未实现业务代码。技术方案为 PROPOSED，待 T-0002 评审；尚未执行原型、性能或真实仓库验证。

Goal:

根据：

```text
PROJECT.md
DECISIONS.md
```

生成 RepoOnboard V0.1 技术架构方案。

必须覆盖：

```text
Java Runtime
Build Tool
CLI
Maven Parsing
Java AST
Symbol Resolution
Generic Project Model
Analyzer Boundary
Dependency Graph
Serialization
Local Web Architecture
Frontend
Visualization
Testing
Packaging
```

Acceptance Criteria:

```text
[x] 每项核心技术问题均有明确推荐方案
[x] 至少比较关键候选方案
[x] 明确最主要技术风险
[x] 未扩大 PROJECT.md V0.1 Scope
[x] 未开始正式业务代码实现
```

---

## T-0002 — Architecture Review

Status:

```text
[x]
```

Completed: 2026-09-08

Deliverable: [T-0002 Architecture Review](docs/architecture/T-0002-ARCHITECTURE-REVIEW.md)

Validation: 15 项技术选择已逐项评审；Maven 隔离实验 8 项断言通过。评审结论允许进入 ADR 起草，正式 ACCEPTED 状态仍需明确确认。

Depends On:

```text
T-0001
```

Goal:

审核技术方案。

重点检查：

```text
是否过度设计
是否强耦合 Spring Boot
是否为了多语言提前做复杂插件系统
Java AST 方案是否现实
Symbol Resolution 是否过深
Web UI 是否过重
Graph 技术是否必要
模块拆分是否合理
```

Acceptance Criteria:

```text
[x] 所有关键技术选择经过 Review
[x] 明确哪些方案接受
[x] 明确哪些方案拒绝
[x] 明确哪些问题延后
```

---

## T-0003 — Create Initial ADRs

Status:

```text
[x]
```

Completed: 2026-09-08

Deliverable: [Accepted Architecture Decision Records](DECISIONS.md#21-accepted-architecture-decision-records)

Validation: ADR-0001 至 ADR-0016 均为 ACCEPTED；Decision Index、原始决策队列映射和当前摘要已同步更新。

Depends On:

```text
T-0002
```

Goal:

将技术方案中已经确认的重要选择写入：

```text
DECISIONS.md
```

至少应考虑建立：

```text
ADR-0001 — Java Runtime
ADR-0002 — Build Strategy
ADR-0003 — CLI Framework
ADR-0004 — Maven Analysis
ADR-0005 — Java AST
ADR-0006 — Symbol Resolution Scope
ADR-0007 — Generic Project Model
ADR-0008 — Analyzer Boundary
ADR-0009 — Local Web Architecture
ADR-0010 — Testing Strategy
```

具体数量以实际技术方案为准。

Acceptance Criteria:

```text
[x] 所有关键 ACCEPTED 技术方案已形成 ADR
[x] PROPOSED 和 ACCEPTED 状态没有混淆
[x] DECISIONS.md Decision Index 已更新
```

---

## T-0004 — Define V0.1 Milestone Boundaries

Status:

```text
[x]
```

Completed: 2026-09-08

Deliverable: 本文件第 9.1、9.2 节及校准后的 M1–M10 任务边界。

Validation: 里程碑入口、交付物、出口门槛、默认任务依赖和非阻塞例外均已明确；路线与 ADR-0001 至 ADR-0016 一致。

Depends On:

```text
T-0003
```

Goal:

根据最终技术方案修正本 TODO 中：

```text
M1 ~ M10
```

确保每个 Milestone：

```text
范围足够小
可以测试
可以独立验收
依赖关系清晰
```

Acceptance Criteria:

```text
[x] M1 ~ M10 均有明确入口、交付物和出口门槛
[x] 同一 Milestone 内的默认任务依赖规则明确
[x] 最小 Evidence / Diagnostic 契约前置到 M1
[x] 自动化 fixture 随功能交付，M9 聚焦审计与真实仓库验证
[x] Optional Mapper 不阻塞 M5
[x] 未扩大 V0.1 Scope
```

---

# 11. M1 — Project Foundation

Goal:

> 建立最小、可靠、可测试的 RepoOnboard 工程基础。

Status:

```text
✓ COMPLETED
```

Boundary:

> 只建立可重复工程、最小 CLI、测试底座和跨分析器共享的 Evidence / Diagnostic 契约；不实现 Maven、Java 或 Spring 分析。

Exit Criteria:

```text
[x] Maven Wrapper clean test 通过
[x] CLI 路径/help/错误码测试通过
[x] SourceLocation / Evidence / Diagnostic / AnalysisStatus 契约测试通过
```

---

## T-0101 — Create Repository Scaffold

Status:

```text
[x]
```

Completed: 2026-09-08

Deliverable: `src/main/java/io/github/zhancm/repoonboard/` 的单模块包骨架及根 `.gitignore`。

Validation: 9 个 package descriptor 使用 Java 21 `javac -Xlint:all -proc:none` 编译通过；结构检查确认没有产品实现类或多余模块。

Depends On:

```text
M0 Complete
```

Goal:

根据 ACCEPTED ADR 创建正式工程结构。

Acceptance Criteria:

```text
[x] 单 Java module 的基础目录结构清晰
[x] ADR-0008 的包边界有对应目录/包规划
[x] 没有不必要的模块
[x] README / PROJECT / DECISIONS / TODO 保留
[x] 未提前实现 Maven / Java / Spring 分析能力
```

---

## T-0102 — Configure Build

Status:

```text
[x]
```

Completed: 2026-09-08

Deliverable: Java 21 单模块 `pom.xml`、Maven 3.9.16 only-script Wrapper、发行包 SHA-256 校验配置及跨平台脚本换行约束。

Validation: Wrapper 自举成功；`validate`、`compile`、`test`、`clean test`、`clean verify` 均通过；连续两次 `clean package` 生成相同 SHA-256 的 JAR。

Goal:

完成 RepoOnboard 自身 Build 配置。

Acceptance Criteria:

```text
[x] Maven Wrapper 固定 Maven 3.9 系列并包含校验配置
[x] validate / compile / test 命令可以执行
[x] 构建环境可重复
[x] 不依赖 IDE 才能构建
```

---

## T-0103 — Establish Testing Foundation

Status:

```text
[x]
```

Completed: 2026-09-08

Deliverable: JUnit Jupiter 6.1.3 测试依赖、可复用 fixture 路径工具、最小 Maven 项目 fixture，以及可按 `fixture` 标签分层运行的测试结构。

Validation: `clean test` 共运行 4 个测试并通过；非 fixture 单元层 3 个测试通过；fixture 层 1 个测试通过；无失败、错误或跳过。

Goal:

建立最基本自动化测试结构。

Acceptance Criteria:

```text
[x] 至少一个 unit test
[x] 至少一个 fixture test
[x] CI 前可以本地运行
```

---

## T-0104 — Create Minimal CLI Entry

Status:

```text
[x]
```

Completed: 2026-09-08

Deliverable: 基于 Picocli 4.7.7 的 `RepoOnboardCommand`，支持必填目标目录、`--help`、`--version`、路径校验、基础状态输出和稳定退出码。

Validation: `clean test` 与 `clean verify` 共运行 10 个测试并通过，其中 6 个覆盖 CLI；真实 Java 进程验证 `.`、help、version 均以 0 退出，不存在路径输出明确错误并以 2 退出。

Goal:

实现：

```bash
repoonboard .
```

的最小 CLI 入口。

当前只需要：

```text
读取目标路径
验证路径存在
输出基础 CLI 信息
```

不要提前实现分析能力。

Acceptance Criteria:

```text
[x] 可以从 CLI 启动
[x] "." 能解析为当前目录
[x] 非法路径有明确错误信息
[x] CLI 有基础 help
```

---

## T-0105 — Establish Core Evidence and Diagnostic Contracts

Status:

```text
[x]
```

Completed: 2026-09-08

Deliverable: 无框架依赖的 `SourceLocation`、`Evidence`、`Diagnostic`、`DiagnosticSeverity` 与 `AnalysisStatus` 契约，包含扫描根相对路径、1-based 可选位置和诊断状态聚合规则。

Validation: Java 21 `clean verify` 共运行 24 个测试并通过，其中 14 个聚焦核心契约；无失败、错误或跳过。

Goal:

按照 ADR-0007、ADR-0015 建立后续 analyzer 共同使用的最小类型：

```text
SourceLocation
Evidence
Diagnostic
AnalysisStatus
```

不在此任务提前实现完整 Project / Component / Endpoint 模型。

Acceptance Criteria:

```text
[x] Core 类型不依赖 Maven / JavaParser / Spring / Web 类型
[x] 行列号、未知位置和相对路径语义明确
[x] SUCCESS / PARTIAL / FAILED 与诊断严重级别有测试
```

---

# 12. M2 — Maven Project Analysis

Goal:

> 可靠识别和解析 Maven Project。

Status:

```text
✓ COMPLETED
```

Boundary:

> 只生成 Maven 项目、模块、构建坐标、源码根、依赖和 Spring Boot 构建证据；不解析 Java AST 或识别源码组件。

Exit Criteria:

```text
[x] 单模块、多模块、parent/BOM/profile/local repository fixtures 通过
[x] 所有 POM 来源经过受限读取层，恶意/越界输入有诊断
[x] 不执行目标 Maven lifecycle，不配置网络 resolver
[x] 缺失外部模型时保留 raw facts 和 PARTIAL 状态
```

---

## T-0201 — Maven Project Detection

Status:

```text
[x]
```

Completed: 2026-09-08

Deliverable: 根目录 `pom.xml` 检测器与显式 `MavenProjectDetection` 结果；检测证据使用扫描根相对位置，CLI 可区分 Maven 与非 Maven 目录。

Validation: Java 21 `clean verify` 共运行 31 个测试并通过，其中 8 个覆盖检测器及 CLI 的 Maven/非 Maven 行为；真实 CLI 对两类目录均输出明确结果并以 0 退出。

Goal:

判断目标目录是否为 Maven 项目。

Acceptance Criteria:

```text
[x] 能识别 pom.xml
[x] 非 Maven 项目有明确结果
[x] 不因为目录中存在无关文件而失败
```

---

## T-0202 — Basic Maven Metadata Extraction

Status:

```text
[x]
```

Completed: 2026-09-08

Deliverable: 基于 Maven Model 3.9.16 的根 POM 基础元数据读取器；`groupId`、`artifactId`、`version`、`packaging` 均保留 raw/resolved 值、解析状态和 SourceLocation，缺失、属性待解析和读取失败生成结构化 Diagnostic。

Validation: Java 21 `clean verify` 共运行 36 个测试并通过，其中 6 个覆盖基础元数据、默认 packaging、缺失字段、属性表达式、损坏 POM 及 CLI 输出；真实 CLI 正确显示当前项目坐标并以 0 退出。

Goal:

提取：

```text
groupId
artifactId
version
packaging
```

Acceptance Criteria:

```text
[x] 普通单模块项目解析正确
[x] rawValue、resolvedValue、resolutionStatus 和 origin 可表达
[x] 缺失字段产生明确的未知状态或诊断
```

---

## T-0203 — Restricted Maven Model and Property Resolution

Status:

```text
[x]
```

Completed: 2026-09-08

Deliverable: Maven 3.9.16 受限 Model Builder；统一有界 POM 来源、离线 parent/BOM、属性来源保留与可控 profile；CLI 支持 `--profile`、`--local-repository` 及 PARTIAL/FAILED 诊断和退出码。

Validation: Java 21 离线 `clean verify` 53 项测试通过；原 T-0002 API 实验以 3.9.16 依赖复跑，8 项断言全部通过；真实 CLI 扫描自身成功。新增测试覆盖 BOM 属性隔离、拒绝 parent、profile 来源、DTD、输入/来源/深度上限及零远程请求。

Goal:

通过 ADR-0004 的受限读取层构建可用 Maven model，并正确处理常见：

```xml
<!--${...}-->
```

属性。

Acceptance Criteria:

```text
[x] Java version property 可解析
[x] Spring Boot version property 可解析
[x] 常见 parent property 可处理
[x] relativePath、local repository POM 和 BOM 均经过路径/来源校验
[x] DTD/外部实体、过大输入和越界来源被拒绝并产生诊断
[x] 显式 profile 与 activeByDefault 可控，宿主隐式 profile 不参与
[x] 缺失 parent/BOM 时保留 raw facts 和 PARTIAL 状态
[x] 使用最终锁定的 Maven 3.9 版本重跑 T-0002 API 行为测试
```

---

## T-0204 — Multi-module Maven Analysis

Status:

```text
[x]
```

Completed: 2026-09-08

Deliverable: 共享受限 POM 快照的递归模块分析器；区分聚合层级与 parent 坐标，保留每个模块的元数据、归一化 base directory 及源码目录声明/解析值和位置；CLI 展示模块树并汇总部分成功诊断。

Validation: Java 21 离线 `clean verify` 62 项测试通过。新增固定多模块 fixture 与 9 项回归，覆盖嵌套/独立 parent、profile、缺失/损坏/越界模块、重复与循环、源码目录继承、位置证据和 CLI。

Goal:

识别：

```text
Parent
Child Module
Module Hierarchy
```

Acceptance Criteria:

```text
[x] 能识别多模块项目
[x] Module tree 正确
[x] 子模块 base directory 归一化且不逃逸扫描根
[x] build.sourceDirectory 的声明/解析值可供 M3 使用
```

---

## T-0205 — Dependency Extraction

Status:

```text
[x]
```

Completed: 2026-09-08

Deliverable: 各模块依赖提取与 CLI 展示；groupId/artifactId/version/scope 保留原始值、解析值、状态及字段来源，支持离线 parent/BOM 管理和激活 profile，缺失版本保留诊断。

Validation: Java 21 离线 `clean verify` 66 项测试通过；新增 4 项测试覆盖属性版本、默认/显式 scope、BOM 来源、管理条目隔离、缺失 parent、继承和 profile。CLI 自扫描正确输出 4 条依赖。

Goal:

提取 Maven dependencies。

Acceptance Criteria:

```text
[x] dependency groupId
[x] artifactId
[x] version（如可解析）
[x] scope
```

---

## T-0206 — Spring Boot Build Metadata Detection

Status:

```text
[x]
```

Completed: 2026-09-09

Deliverable: Spring Boot parent、导入 BOM、核心/starter dependency 构建证据识别，检测与版本解析独立表达，保留来源和版本位置；CLI 展示证据与未知/冲突版本诊断。

Validation: Java 21 离线 `clean verify` 72 项测试通过。新增 6 项回归覆盖缺失 parent/BOM、属性版本、版本冲突、同名/普通 Spring 反例、未激活 profile 和 CLI。

Goal:

判断项目是否使用 Spring Boot。

此任务只使用 Maven parent、BOM 和 dependency 等构建证据；源码注解证据属于 M4。

Acceptance Criteria:

```text
[x] parent-based Spring Boot 项目可识别
[x] dependency-based Spring Boot 项目尽量可识别
[x] Spring Boot version 尽量正确解析
[x] “检测到 Spring Boot”与“版本已解析”分别表示
[x] 每个结论保留构建来源证据
```

---

# 13. M3 — Java Source Analysis

Goal:

> 建立 RepoOnboard 的 Java Source 基础分析能力。

Status:

```text
✓ COMPLETED
```

Boundary:

> 发现 Java 主源码并生成与框架无关的声明、成员、注解表达式和类型引用 facts；不解释 Spring 注解或构造组件依赖图。

Exit Criteria:

```text
[x] Java 8 / 11 / 17 / 21 常见语法 fixture 通过
[x] 源码位置、嵌套/同名类型和模块归属可追踪
[x] 单文件解析失败不终止其他文件并返回 PARTIAL
[x] 声明索引不因简单名称相同而误解析
```

---

## T-0301 — Source Root Discovery

Status:

```text
[x]
```

Completed: 2026-09-09

Deliverable: 基于 Maven 模块分析结果发现现存 Java 主源码根，保留模块归属、扫描根相对路径和 POM 声明位置；默认与自定义 sourceDirectory 均支持，并排除 target、generated output、`.git` 和 `.repoonboard`。

Validation: Java 21 `test` 共 76 项通过；4 项新增测试覆盖单模块、多模块、自定义目录、缺失目录及构建/生成目录排除。

Goal:

识别：

```text
src/main/java
```

以及多模块项目中的 Source Root。

Acceptance Criteria:

```text
[x] 单模块项目正确
[x] 多模块项目正确
[x] 不扫描 target / generated output 等无关目录
```

---

## T-0302 — Java File Discovery

Status:

```text
[x]
```

Completed: 2026-09-09

Deliverable: 在确认的 Java 主源码根内稳定枚举 `.java` 文件，保留扫描根相对路径、模块 POM 与模块目录归属；跳过构建/生成目录与符号链接，并在源码根不可用或模块归属重叠时返回可追踪的部分结果。

Validation: Java 21 `test` 共 82 项通过；5 项 Java 文件发现测试覆盖稳定枚举、模块归属、构建/生成目录排除、不可用源码根恢复与重叠模块归属歧义。

Goal:

发现目标项目中的 Java Source File。

Acceptance Criteria:

```text
[x] 能枚举 Java files
[x] Source path 可追踪
[x] Module ownership 可识别
```

---

## T-0303 — Basic AST Parsing

Status:

```text
[x]
```

Completed: 2026-09-09

Deliverable: 使用锁定的 JavaParser 3.28.2 将 Java 8/11/17/21 常见源码语法转换为项目自有的 package、类型、字段、方法、参数和未解释注解 facts；支持嵌套类型与 1-based 来源位置，单文件语法问题不阻断其他文件。

Validation: Java 21 `test` 共 85 项通过；新增 Java 语法 fixture 和 3 项解析测试覆盖跨版本语法、类型/成员/注解、嵌套限定名、来源位置与损坏文件 PARTIAL 恢复。

Goal:

从 Java 文件提取：

```text
Package
Class
Interface
Enum
Record
Method
Field
Annotation
```

Acceptance Criteria:

```text
[x] Fixture Java 项目解析正确
[x] Source Location 可追踪
[x] 单个文件解析失败不会导致整个项目失败
```

---

## T-0304 — Import and Type Reference Extraction

Status:

```text
[x]
```

Completed: 2026-09-09

Deliverable: 提取 import 与每个类型使用位置，并按全限定名、显式 import、同包、唯一通配 import 的保守优先级解析项目内声明；只允许当前模块及明确 Maven 编译可见模块，外部、不可见和歧义引用保持 unresolved/ambiguous。

Validation: Java 21 `test` 共 88 项通过；3 项新增测试覆盖 import 位置、解析优先级、通配歧义、外部类型、已确认跨模块依赖，以及 runtime/test scope 不可见性。

Goal:

提取基础：

```text
Imports
Declared Types
Referenced Types
```

为后续 dependency analysis 准备。

Acceptance Criteria:

```text
[x] 全限定名、显式 import、同包和唯一通配 import 可解析
[x] 无法确认或存在歧义的类型保持 unresolved
[x] 跨模块解析只使用确认的 Maven 可见关系
```

---

## T-0305 — Java Declaration Index

Status:

```text
[x]
```

Completed: 2026-09-09

Deliverable: 建立稳定的 module-aware qualified-name/simple-name 声明索引，条目保留类型种类、模块 POM 与源码位置；同模块重复声明产生 WARNING，跨模块同限定名产生 INFO，所有候选均保留，并由解析器按 Maven 可见性筛选。

Validation: Java 21 `test` 共 90 项通过；2 项新增索引测试覆盖嵌套类型、跨包同名、同模块重复、跨模块同限定名，以及不可见模块/外部类型不误解析。

Goal:

将每个模块中的 declared types 建立为 module-aware qualified-name index，为 M4/M5 提供受限项目内类型解析。

Acceptance Criteria:

```text
[x] qualified-name 索引包含模块和源码位置
[x] 同名、嵌套和重复声明不会静默覆盖
[x] 不可见模块或外部类型不会被错误连边
```

---

# 14. M4 — Spring Boot Analysis

Goal:

> 从 Java AST 中识别 Spring Boot 软件语义。

Status:

```text
✓ COMPLETED
```

Completed: 2026-09-09

Validation: Java 21 `clean verify` 通过；共 100 项测试，覆盖标准/项目内组合 Spring 注解、配置与应用入口、构造器/字段注入，以及歧义与不支持模式的可解释诊断。

Boundary:

> 把 M3 Java facts 映射成 Spring 组件、配置、应用入口和注入候选；不生成最终 HTTP Endpoint 或 Dependency graph。

Exit Criteria:

```text
[x] 标准 Spring 组件、配置和应用入口 fixture 通过
[x] 单构造器、@Autowired 构造器和字段注入候选可识别
[x] 多构造器、多实现和未知组合注解产生明确限制/歧义
[x] T-0401 至 T-0403 完成；T-0404 可延后
```

---

## T-0401 — Spring Component Detection

Status:

```text
[x]
```

Completed: 2026-09-09

Deliverable: 将确认的 Spring `@Controller`、`@RestController`、`@Service`、`@Repository`、`@Component` 映射为带模块、组件名称、类型位置和注解 Evidence 的 Spring facts；仅接受全限定名或无歧义 import，显式名称只读取确定字符串字面量。

Validation: Java 21 `test` 共 93 项通过；固定 Spring fixture 与 3 项新增测试覆盖五类组件、默认/显式名称、位置证据、同名伪注解、import 歧义、角色冲突和无法读取的名称表达式。

至少支持：

```text
@Controller
@RestController
@Service
@Repository
@Component
```

Acceptance Criteria:

```text
[x] Component Type 分类正确
[x] Component Name 正确
[x] Source Location 正确
```

---

## T-0402 — Configuration and Application Entry Point Detection

Status:

```text
[x]
```

Completed: 2026-09-09

Deliverable: 识别确认的 `org.springframework.context.annotation.Configuration` 与 `org.springframework.boot.autoconfigure.SpringBootApplication`；后者同时生成配置类和应用入口 facts，均保留模块、类型位置和注解 Evidence，同名伪注解及歧义 import 不命中。

Validation: Java 21 `test` 共 95 项通过；2 项新增测试和扩展 Spring fixture 覆盖配置类、Boot 应用入口、位置证据、同名伪注解、通配 import 歧义及 CLI 统计。

识别：

```java
//@Configuration
//@SpringBootApplication
```

以及 Spring Boot application entry point。

Acceptance Criteria:

```text
[x] Configuration 与 application entry point 分类正确
[x] Source Location 和注解证据正确
[x] 只使用确认的 Spring 全限定注解，不按简单名称猜测
```

---

## T-0403 — Dependency Injection Detection

Status:

```text
[x]
```

Completed: 2026-09-09

Deliverable: 从已确认的 Spring 组件与配置类中提取构造器和 `@Autowired` 字段注入候选；单构造器与唯一标注构造器可确认，多构造器歧义保留全部候选，项目内接口类型只解析到接口声明而不猜测实现。Setter、Lombok 生成构造器、静态字段和泛型容器注入会产生明确的范围诊断。

Validation: Java 21 `test` 共 98 项通过；固定注入 fixture 与 3 项相关测试覆盖单构造器、唯一 `@Autowired` 构造器、字段注入、多构造器、多实现接口、同名伪注解、Setter、Lombok、静态字段和泛型限制。

至少分析：

```text
Constructor Injection
Field Injection
```

根据技术方案再决定是否支持：

```text
Setter Injection
Lombok generated constructor
```

Acceptance Criteria:

```text
[x] 常见 Constructor Injection 正确
[x] 常见 @Autowired Field 正确
[x] 接口多实现不直接绑定某个实现
[x] 多构造器无法确认时保留候选和诊断
```

---

## T-0404 — Mapper Detection

Status:

```text
[-]
```

Design Handoff:

```text
[x] UI Data Contract Audit
[x] Stitch Design Brief
[ ] User-reviewed Stitch prototype imported
[ ] Vue implementation and browser validation
```

Priority:

```text
OPTIONAL FOR EARLY V0.1
```

Dependency Exception:

> 本任务不阻塞 M4 完成或 M5 开始。若延后，必须在 Known Issues / Deferred Work 中记录覆盖限制。

Deferred: 2026-09-09

Reason: 当前 M4 所需的组件、配置、入口与注入 facts 已满足出口；MyBatis-Plus `BaseMapper<T>` 的可靠识别还需要结构化的继承/实现类型 facts。该可选任务保留到相关 Java facts 可复用时处理，避免为非阻塞能力提前扩大 M3 模型。

支持：

```text
MyBatis Mapper
MyBatis-Plus Mapper
```

如果实现成本过高：

> 可以推迟，但不得阻塞核心 Spring Component Analysis。

---

# 15. M5 — API & Dependency Analysis

Goal:

> 建立 RepoOnboard 第一批真正有用户价值的结构关系。

Status:

```text
✓ COMPLETED
```

Completed: 2026-09-09

Validation: Java 21 `clean verify` 通过；共 108 项测试，覆盖 Spring MVC 方法、路径和条件组合，以及确定、重复、歧义、未解析和循环组件依赖关系。

Boundary:

> 基于 M3/M4 facts 生成 HTTP Endpoint 和已确认的组件依赖边；不负责最终报告 DTO、JSON 或 UI。

Exit Criteria:

```text
[x] 标准 Spring MVC method/path/condition fixture 通过
[x] 类级与方法级映射组合正确，ANY 和 unresolvedPath 明确
[x] 每条确定依赖边包含 evidence，歧义关系不伪造
[x] 循环、重复证据和 unresolved target 测试通过
```

---

## T-0501 — Spring MVC Controller Mapping

Status:

```text
[x]
```

Completed: 2026-09-09

Deliverable: 从已确认的 Controller/RestController 中提取标准 Spring MVC 类级与方法级 mapping facts，保留路径数组、HTTP method、params/headers/consumes/produces 条件、源码位置和 Evidence；同名伪注解及歧义 import 不会被猜测。

Validation: Java 21 `test` 共 102 项通过；固定 API fixture 与 2 项相关测试覆盖六类标准 mapping、类/方法层级、多路径、多 method、条件、同名伪注解及无法读取的表达式。

识别：

```text
@RequestMapping
@GetMapping
@PostMapping
@PutMapping
@DeleteMapping
@PatchMapping
```

Acceptance Criteria:

```text
[x] 只识别可确认的 Spring MVC 标准注解
[x] class-level 和 method-level 声明均保留 Source Evidence
[x] path 数组、HTTP method 和 mapping conditions 不被丢失
```

---

## T-0502 — Full API Path Resolution

Status:

```text
[x]
```

Completed: 2026-09-09

Deliverable: 将类级与方法级 mapping facts 做笛卡尔路径组合，并按 Spring method 条件求交集，生成带 Controller、handler、条件、方法位置及两级 Evidence 的 endpoint facts；无 method 限制显式表示为 `ANY`，未知路径与 method 分别保留未解析状态。

Validation: Java 21 `test` 共 103 项通过；固定 API fixture 与新增 endpoint 测试覆盖多级多路径、GET/POST/ANY、路径规范化、未知路径、条件合并以及同路径不同条件 handler。

正确组合：

```text
Class Mapping
+
Method Mapping
```

例如：

```text
/users
+
/{id}
=
/users/{id}
```

Acceptance Criteria:

```text
[x] HTTP method 正确
[x] Path 正确
[x] Controller 正确
[x] Handler method 正确
[x] Source Location 正确
[x] 无 method 限制的 RequestMapping 表示为 ANY
[x] 无法解析的表达式表示为 unresolvedPath
[x] params / headers / consumes / produces 可区分同路径 handler
```

---

## T-0503 — Component Dependency Extraction

Status:

```text
[x]
```

Completed: 2026-09-09

Deliverable: 将已确认的 Spring 注入候选映射为 `source component → target component` 的有向 `COMPONENT_INJECTION` 边，保留源/目标模块、声明类型、注入位置与 Evidence；只有目标在组件集合中唯一确认时才创建确定边。

Validation: Java 21 `test` 共 106 项通过；固定 dependency fixture 与 3 项相关测试覆盖 Controller→Service、Service→Service、Service→Repository、Component→Component、构造器/字段 Evidence，以及接口多实现不误连边。

建立：

```text
Controller → Service
Service → Service
Service → Repository
Service → Mapper
Component → Component
```

Acceptance Criteria:

```text
[x] Fixture 项目关系正确
[x] 每条确定关系包含类型、方向和 Source Evidence
[x] 无法确认的关系不应伪造
[x] Maven 模块依赖与组件依赖分别表示
```

---

## T-0504 — Dependency Ambiguity and Evidence Consolidation

Status:

```text
[x]
```

Completed: 2026-09-09

Deliverable: 按稳定的源、目标、关系类型与状态合并重复组件依赖，Evidence 去重并稳定排序；未解析类型、非组件类型、歧义构造器及重复组件目标保留为非确定 dependency facts 和结构化诊断。确认边可生成模块感知的稳定邻接表，循环关系原样保存。

Validation: Java 21 `clean verify` 共 108 项通过；dependency fixture 与 4 项专项测试覆盖构造器/字段重复证据、接口多实现、缺失类型、同名组件冲突、确定环与循环安全遍历。

合并同一关系的多个证据，并为无法确认的 dependency 记录 unresolved target、位置和原因。

每条 dependency 记录：

```text
Source File
Source Location
Evidence Type
```

例如：

```text
CONSTRUCTOR_INJECTION
FIELD_INJECTION
TYPE_REFERENCE
```

Acceptance Criteria:

```text
[x] 重复边按稳定身份合并，Evidence 保留为集合
[x] 接口多实现、缺失类型和同名冲突不会生成猜测边
[x] 循环关系可保存且不会导致遍历失败
```

---

# 16. M6 — Report Assembly & Serialization

Goal:

> 将 Maven、Java、Spring、API、Dependency facts 装配成稳定、可序列化的 AnalysisReport。

Status:

```text
✓ COMPLETED
```

Boundary:

> 定义完整公开报告、稳定身份、统计和 JSON 契约；不实现 HTTP server 或页面。

Exit Criteria:

```text
[x] AnalysisReport 完整承载支持的 facts、evidence、diagnostics 和 coverage status
[x] IDs、集合顺序和语义快照在相同输入下稳定
[x] schemaVersion 兼容与未知主版本行为有测试
[x] JSON 不包含源码正文、默认绝对路径或未允许的配置属性
```

---

## T-0601 — Project Model Assembly

Status:

```text
[x]
```

至少包含：

```text
Project
Module
SourceFile
Component
Endpoint
EntryPoint
Dependency
SourceLocation
Evidence
Diagnostic
AnalysisReport
```

必须符合：

```text
Generic enough for future languages
Simple enough for V0.1
```

Acceptance Criteria:

```text
[x] M2 ~ M5 facts 可转换到公共模型
[x] Core model 不暴露 Maven / JavaParser / Spring 类型
[x] SUCCESS / PARTIAL / FAILED 与 diagnostics 一同保留
```

---

## T-0602 — Stable Identifiers

Status:

```text
[x]
```

Completed: 2026-09-09

Deliverable: `StableIdentifiers` 定义 Project、Module、SourceFile、Component、EntryPoint、Endpoint、Dependency 的结构身份；`AnalysisReport` 与核心集合执行确定排序，报告装配器对重复语义键保留全部实体、分配稳定后缀并生成 `REPORT_ID_COLLISION` 诊断。

Validation: Java 21 离线 `clean verify` 共 113 项测试通过；固定 Spring fixture 验证输入倒序时报告、ID 和排序不变，重复组件键不被静默覆盖，组件重命名、Endpoint handler/HTTP 签名及 dependency target 变化会产生新 ID。

为：

```text
Module
Component
Endpoint
Dependency
```

建立稳定 ID 规则。

目标：

> UI 不依赖对象内存地址或不稳定排序。

Acceptance Criteria:

```text
[x] 相同语义输入得到相同 ID 与排序
[x] 重复声明和键冲突产生诊断，不静默覆盖
[x] 重命名和签名变化行为由 fixture 明确
```

---

## T-0603 — Serialization

Status:

```text
[x]
```

Completed: 2026-09-09

Deliverable: 基于 Jackson 3.1.4 的显式 `ReportJsonDocument` DTO 与 `AnalysisReportJson` UTF-8 codec；输出固定 LF 的可读 JSON，支持文件读写、Core Model 往返、`major.minor` schemaVersion 校验以及未知主版本专用错误。

Validation: Java 21 离线 `clean verify` 共运行 122 个测试并通过，其中 9 个覆盖稳定快照、完整实体与 SourceLocation 往返、UTF-8、同主版本新增字段兼容、未知主版本失败、尾随 JSON、相对路径再校验，以及源码正文、用户绝对路径和任意多态元数据边界。

将统一 Project Model 导出为选定格式。

Acceptance Criteria:

```text
[x] Human-readable
[x] Debuggable
[x] Frontend friendly
[x] Source locations retained
[x] schemaVersion 存在且未知主版本明确失败
[x] 默认无源码正文、用户绝对路径或任意多态反序列化
```

---

## T-0604 — Analysis Summary

Status:

```text
[x]
```

Completed: 2026-09-09

Deliverable: `AnalysisSummary` 作为 `AnalysisReport` 的确定性派生 Overview，按稳定实体 ID 去重统计 Module、SourceFile、Component、Controller、Service、Repository、Configuration、Endpoint、EntryPoint 和 Dependency；PARTIAL/FAILED 通过 `analysisStatus`、`coverageLimited` 与去重排序的诊断代码标明覆盖限制。JSON schema 以加法式次版本升级到 `1.1`，包含 Summary 并校验其与底层实体一致，仍可读取不含 Summary 的 `1.0` 报告。

Validation: Java 21 离线 `clean verify` 共运行 127 个测试并通过。新增 5 个测试覆盖稳定身份去重、Controller 类型合并、PARTIAL 覆盖限制、空报告、非法计数、Summary JSON 快照/往返、一致性拒绝及 `1.0` 向后兼容；JAR 构建成功。

生成：

```text
Module count
Controller count
Service count
Repository count
Endpoint count
Dependency count
```

等 Overview 数据。

Acceptance Criteria:

```text
[x] 统计按稳定实体身份去重
[x] PARTIAL 分析标出统计覆盖限制
[x] Summary 与底层实体数量一致
```

---

# 17. M7 — Local Web UI

Goal:

> 把静态分析结果真正转换成用户能快速理解的项目地图。

Status:

```text
● COMPLETE
```

Boundary:

> 只读取 M6 AnalysisReport，在 loopback 本地服务中展示核心信息视图；前端不重新分析仓库、不直接访问本地文件系统，服务不提供任意文件读取。页面保持宿主无关，但不为 V0.2 提前引入 Desktop runtime 或无调用方的抽象。

Exit Criteria:

```text
[x] 最小 App Shell 与统一视觉系统已建立
[x] Overview、Module、Architecture、API 和 Evidence 浏览流程可用
[x] Architecture 默认使用筛选/邻域并有列表回退
[x] 服务绑定 loopback，路径/内容注入/关闭流程测试通过
[x] 前端资源随离线 JAR 构建产物提供，无运行时 CDN
[x] 所有用户可见事实来自 AnalysisReport 或明确派生值，缺失数据不伪造
```

---

## T-0701 — Local UI Bootstrap & Minimal App Shell

Status:

```text
[x]
```

Completed: 2026-09-09

Deliverable: CLI 将 Maven、Java、Spring、Endpoint 和组件依赖分析结果组装为统一 `AnalysisReport`，由仅绑定 `127.0.0.1` 且使用系统分配端口的 JDK `HttpServer` 提供固定静态资源和 `/api/report`。新增 Vue 3 + Vite 生产构建和最小 App Shell，只展示仓库上下文、导航占位、报告加载/错误与连接就绪状态。CLI 支持 `--no-open`，浏览器打开失败时保留服务并输出手动地址，Ctrl+C 关闭时释放端口与线程。

Validation: Node 24 下 Vite production build 通过；Java 21 / Maven 3.9.16 `clean verify` 共运行 136 个测试并通过，JAR 含完整 HTML、CSS 和 JavaScript 生产资源。新增 9 个测试覆盖 CLI 报告交接、`--no-open`、UI 启动失败、浏览器失败回退、loopback 绑定、固定只读路由、报告 JSON 往返、端口占用与关闭释放。真实 CLI 会话在 `127.0.0.1` 的系统分配端口成功返回 HTML 与 schema `1.1` 报告，Ctrl+C 后端口已释放。

完成：

```bash
repoonboard .
```

之后打开本地页面的完整路径。

Scope:

```text
CLI analysis
    ↓
AnalysisReport
    ↓
loopback read-only service
    ↓
Vue application bootstrap
    ↓
minimal App Shell
```

最小 App Shell 只包含：

```text
Application frame
Navigation placeholders
Repository context
Content outlet
Basic loading / error fallback
```

不在此任务中完成正式 Design System、Overview 内容或 Desktop host integration。

Acceptance Criteria:

```text
[x] CLI 可启动 UI
[x] UI 可以读取当前分析结果
[x] 默认不需要账号或外部服务器
[x] JDK HttpServer 仅绑定 127.0.0.1 并使用只读固定路由
[x] --no-open、端口占用、浏览器打开失败和 Ctrl+C 关闭行为明确
[x] 最小 App Shell 可显示仓库上下文与报告加载/错误状态
[x] production frontend build 可由 Java 应用加载
[x] UI 不直接访问文件系统，不存在 Tauri、Electron 或其他 Desktop-specific dependency
```

---

## T-0702 — Visual System & Product Shell

Status:

```text
[x]
```

Completed: 2026-09-10

Deliverable: 将 T-0701 的单文件最小样式升级为集中 design tokens、全局基础样式和响应式 Shell 布局；新增可复用 `AppShell`、`SidebarNav`、`ContextHeader`、`PageLayout`、`InspectorPanel` 与 `StatePanel`，统一 loading、empty、error、ready 状态和语义层级。产品 Shell 在宽屏使用 Sidebar / Workspace / Inspector 三栏，在紧凑桌面尺寸将 Inspector 移至内容下方，在窄屏切换为横向可滚动导航；保留 skip link、可见键盘焦点和 reduced-motion 支持，不包含 Overview 业务内容。

Validation: Vitest 4 + happy-dom 的 3 个测试文件共 19 项通过，覆盖报告 loading/ready/error、可复用 Shell/slot 契约、导航禁用与键盘焦点、live-region 语义、集中 tokens、WCAG 4.5:1 文本色彩组合和响应式断点。Chrome 实际渲染检查覆盖 1440×900、1024×768 和 500×844 CSS viewport；Java 21 / Maven 3.9.16 `clean verify` 同时运行 19 项前端测试和 136 项 Java 测试并成功打包前端资源。

正式建立后续所有页面共用的视觉系统和产品 Shell。

Scope:

```text
Design tokens
Typography
Color
Spacing
Border and radius
Sidebar and page layout
Context header
Inspector foundation
Loading / empty / error patterns
Keyboard focus and basic accessibility
Desktop-ready viewport behavior
```

不在此任务中实现 Overview 或其他业务页面。

Acceptance Criteria:

```text
[x] Design tokens 集中定义，页面不自建竞争的视觉体系
[x] Sidebar、Context Header、Page Layout 和 Inspector 基础结构可复用
[x] loading、empty、error 使用统一组件和文案层级
[x] 键盘焦点、对比度和常用尺寸通过自动化/人工验收
[x] 在普通浏览器和桌面应用尺寸的 viewport 下布局稳定
[x] 不引入 Desktop runtime 或与 Vue/Vite/Cytoscape.js ADR 冲突的技术栈
```

---

## T-0703 — Repository Overview

Status:

```text
[x]
```

Completed: 2026-09-10

Deliverable: 在 T-0702 产品 Shell 中启用 Overview 主视图，直接消费 schema `1.1` `AnalysisReport` 与 Summary；展示仓库身份、构建系统、已报告语言/框架、模块/源码根摘要、组件角色统计、Endpoint/Dependency/EntryPoint 计数、应用入口以及警告/错误摘要。缺失字段统一显示 `Not available` 或明确空状态，SUCCESS、PARTIAL、FAILED 使用不同状态说明；派生展示规则集中在 `reportOverview.js` 并保持可重现，不从名称、路径或文案猜测技术事实。

Validation: Vitest 5 个测试文件共 31 项通过，覆盖 Summary/实体派生、技术枚举显示、SUCCESS/PARTIAL/FAILED、缺失值、空状态、诊断预览和完整 App 组合。Java 21 / Maven 3.9.16 `clean verify` 同时运行 31 项前端测试与 136 项 Java 测试并成功打包；真实 `spring-analysis-project` CLI 报告在 Chrome 1440×900、1024×768 和 500×900 CSS viewport 完成实际渲染检查，服务停止后 loopback 端口释放。

展示当前 AnalysisReport 可支持的真实结果：

```text
Repository identity
Build system
Detected languages and frameworks
Module and source-root summary
Component statistics
Endpoint and dependency statistics
Application entry points
Analysis status and warnings
```

版本、运行时或其他字段只有在公共报告提供明确事实与 Evidence 时才展示；前端不从名称、路径或文案自行猜测。

目标：

> 用户 30 秒内知道项目基本情况。

Acceptance Criteria:

```text
[x] 页面使用 T-0702 的视觉系统
[x] 所有字段可映射到 AnalysisReport 或可重现的派生规则
[x] SUCCESS、PARTIAL 和 FAILED 状态与覆盖限制清晰展示
[x] 空值显示为明确未知/不可用，不生成伪造值
```

---

## T-0704 — Module Explorer

Status:

```text
[x]
```

Deliverable: 基于本地 Stitch 高保真原型实现浅色 workbench Module Explorer；模块索引支持搜索与筛选，主区展示显式聚合层级、模块统计、Maven 元数据、源码根、Java / Spring Boot 版本、确认的内部模块依赖、组件与入口列表，Inspector 展示次级元数据、模块诊断和来源证据。公共报告 schema 加法式升级到 `1.2`：`Module` 新增显式 `aggregationParentModuleId`、带 Evidence 的通用 language/framework version facts；Maven dependency 仅在完整 GAV 唯一匹配时指向真实 Module ID。旧 `1.0` / `1.1` 报告继续可读，UI 明确区分 0、Unavailable、Unresolved 和未知状态。

Acceptance Criteria:

```text
[x] Module hierarchy 来自显式聚合父 ID，不从路径或 Maven parent 推断
[x] Internal module dependency 仅由完整且唯一匹配的 Maven GAV 确认
[x] Module metadata、统计、源码根、组件和入口直接来自报告或确定性分组
[x] Java / Spring Boot version 具有公共模型字段和 Source Evidence
[x] 搜索、筛选、空状态、不可用状态、Inspector 与响应式布局已验证
[x] 页面不展示 T-0707 routes，也不生成模块级伪状态
```

展示：

```text
Module Hierarchy
Module Components
Module Statistics
Module Metadata
Source Roots
Inter-module Dependencies
```

如果公共报告尚不包含显式父子身份或内部模块依赖，应先在本任务内以可测试的模型变更补齐，不得由 UI 根据路径或坐标猜测。

---

## T-0705 — Architecture Workspace

Status:

```text
[x]
```

Design Handoff:

```text
[x] UI Data Contract Audit
[x] Stitch Design Brief
[x] User-reviewed Stitch prototype imported
[x] Vue implementation and browser validation
```

展示主要：

```text
Controller
REST Controller
Service
Repository
Component
Configuration
```

以及 Dependency。

Mapper 仅在可选 T-0404 提供公共 `ComponentKind` 后进入页面；当前 T-0705 不得显示推断的 Mapper 或 `0 Mappers`。

重点：

> 可读性优先于节点数量。

支持基础 zoom、pan、fit view、node/edge selection、Inspector 和 Source Evidence。复杂探索与筛选留给 T-0706。

Acceptance Criteria:

```text
[x] Module graph 与 Component graph 不混用边类型
[x] 默认视图不会一次渲染无界全仓库图
[x] 图节点可回到详情和 Source Evidence
```

---

## T-0706 — Architecture Exploration & Filtering

Status:

```text
[x]
```

Design Handoff:

```text
[x] UI Data Contract Audit
[x] Stitch Design Brief
[x] User-reviewed Stitch prototype imported
[x] Vue implementation and browser validation
```

至少支持：

```text
By Module
By Component Type
By Relationship Type
Search Node
Selected-node Neighborhood
Reset / Focus / Fit Selection
```

防止大项目直接生成“毛线球”。

Acceptance Criteria:

```text
[x] 模块、组件类型和一阶邻域筛选可组合
[x] 超过节点预算时显示隐藏数量并要求缩小范围
[x] 始终提供可搜索列表作为图的替代入口
```

---

## T-0707 — API Map

Status:

```text
[x]
```

Design Handoff:

```text
[x] UI Data Contract Audit
[x] Stitch Design Brief
[x] User-reviewed Stitch prototype imported
[x] Vue implementation and browser validation
```

展示：

```text
HTTP Method
Path
Controller
Handler
Source
```

至少支持简单搜索 / 过滤。

Acceptance Criteria:

```text
[x] 可按 HTTP method、path 和 module 过滤
[x] ANY、unresolvedPath 和 mapping conditions 清晰展示
[x] Endpoint 可打开 handler 和两级 mapping Evidence
```

---

## T-0708 — Source Navigation

Status:

```text
[x]
```

Design Handoff:

```text
[x] UI Data Contract Audit
[x] Stitch Design Brief
[x] User-reviewed Stitch prototype imported
[x] Vue implementation and browser validation
```

点击 Component / Endpoint 后展示：

```text
Source File
Line
Module
Related Components
```

V0.1 不要求直接集成 IDE。支持安全复制 path 和 symbol；如首次出现宿主操作需求，只为实际调用方建立最小 `SourceNavigationHost` 边界。

Acceptance Criteria:

```text
[x] 位置使用扫描根相对路径和 1-based 行列
[x] 未知位置不显示伪造的 0 行
[x] Source/Diagnostic 内容以文本方式安全渲染
[x] Copy Path / Copy Symbol 在可用时工作，不可用时有明确回退
```

---

## T-0709 — Local Web Boundary and Packaged UI Validation

Status:

```text
[x]
```

Completed: 2026-09-11

Deliverable: 本地服务将绑定严格限制为 `127.0.0.1`，校验精确 Host 和可选同源 Origin，拒绝编码/非规范路径、query、任意文件与非只读请求，并为成功和错误响应统一发送 CSP、no-store、nosniff、same-origin resource policy、no-referrer 和 frame denial。生产 JAR 的 verify 阶段直接检查 HTML、JavaScript、CSS、离线资源引用及 Java / HTML / 前端 schema 契约；前端同时拒绝不兼容 major schema，不使用浏览器地址栏、filesystem API 或 Desktop runtime。报告组装补齐源码发现、注入和 MVC mapping 阶段诊断，保证 CLI 与 UI 的 PARTIAL 状态及覆盖提示一致。

Validation: Java 21 / Maven 3.9.16 `clean verify` 运行 74 项前端测试和 142 项 Java 测试并通过，随后从生成的 JAR 直接完成离线资源校验。真实 loopback 服务返回预期安全响应头，外部 Origin 返回 403；Repository Overview、Module Explorer、Architecture Workspace 与 API Map 在 1280×800 和 1024×640 viewport 可用。最终浏览器报告正确显示 PARTIAL 和 2 条 MVC mapping 诊断；修正 Cytoscape 容器定位后，干净浏览器会话无 warning / error，Ctrl+C 后服务端口释放。

Goal:

验证 ADR-0011、ADR-0012 的本地服务和前端发布边界。

Acceptance Criteria:

```text
[x] 路径穿越、错误 Host/Origin 和任意文件读取被拒绝
[x] CSP、no-store 和无通配 CORS 配置有效
[x] 恶意项目名、路径和诊断文本不会执行为页面内容
[x] 生产前端资源从 JAR 离线加载并与 schemaVersion 匹配
[x] 服务关闭后端口和资源释放
[x] UI 不依赖浏览器地址栏，不直接访问 filesystem，不包含 Desktop-specific dependency
[x] 在桌面应用尺寸的 viewport 下主要布局可用
```

---

# 18. M8 — Start Here

Goal:

> 让 RepoOnboard 从“项目结构查看器”升级成真正的 Onboarding Tool。

Status:

```text
◐ IN PROGRESS
```

Boundary:

> 基于已经确认的 entry point、endpoint 和 dependency evidence 生成可解释阅读路径；不推断业务职责或使用 AI 排名。

Exit Criteria:

```text
[ ] 推荐顺序由确定规则产生且相同输入稳定
[ ] 每个推荐项包含可核对理由和 Source Evidence
[ ] 按文件去重并限制默认展示数量
[ ] PARTIAL 分析明确提示推荐依据不完整
```

---

## T-0801 — Reading Importance Heuristics

Status:

```text
[x]
```

Completed: 2026-09-11

Deliverable: `ReadingImportanceHeuristics` 从公共 `AnalysisReport` 生成未按文件去重的稳定重要度信号，显式区分根 POM、应用入口、配置、拥有 Endpoint 的 Controller 和沿确认组件注入边可达的 Service / Repository。排序仅使用规则优先级、Endpoint 数量、确认边数量、依赖距离及稳定身份；每个依赖邻域信号保留支持它的确认 dependency ID，不生成业务语义或不透明分数。

Validation: Java 21 / Maven 3.9.16 `clean verify` 通过，共运行 74 项前端测试和 146 项 Java 测试。新增 `start-here-project` fixture 与 4 项聚焦测试，覆盖根 POM、应用入口、配置、Controller Endpoint 数量、Controller → Service → Repository 距离、输入顺序稳定性，以及对未确认关系、Maven 关系和仅凭显眼类名的排除。

根据技术方案实现第一版可解释规则。

可能参考：

```text
Entry Point
Component Role
Endpoint Exposure
Dependency Centrality
Configuration Importance
```

Acceptance Criteria:

```text
[x] 规则只使用可验证 facts 和明确计数
[x] 根 POM、应用入口、配置、Controller、确定依赖邻域优先级有 fixture
[x] 不从类名生成业务语义结论
```

---

## T-0802 — Recommended Reading Path

Status:

```text
[ ]
```

生成：

```text
1.
2.
3.
...
```

推荐源码阅读顺序。

Acceptance Criteria:

```text
[ ] 按文件去重，模块间顺序稳定
[ ] 默认最多展示 10 项并可展开
[ ] unresolved dependency 不参与确定排名
```

---

## T-0803 — Recommendation Explanation

Status:

```text
[ ]
```

每个推荐项说明：

> Why should I read this?

例如：

```text
Main application entry point

Exposes 8 HTTP endpoints

Central service used by 5 controllers
```

Acceptance Criteria:

```text
[ ] 每个原因可回溯到 entry point、endpoint、role 或 dependency evidence
[ ] 计数与 AnalysisReport 一致
[ ] PARTIAL 情况包含覆盖限制说明
```

---

## T-0804 — Start Here UI

Status:

```text
[ ]
```

在 Web UI 中形成完整：

> Start Here

页面。

Acceptance Criteria:

```text
[ ] 阅读顺序、原因、模块和 Source Location 可浏览
[ ] 可从推荐项进入相关 API/组件详情
[ ] 空结果和 PARTIAL 结果有明确说明
```

---

# 19. M9 — Regression & Real Repository Validation

Goal:

> 证明 RepoOnboard 不只是 Demo 项目能运行。

Status:

```text
○ NOT STARTED
```

Boundary:

> 审计 M2 ~ M8 随功能建立的 fixture，并在固定 commit 的真实 Spring Boot Maven 仓库上验证准确性、性能和理解价值；不在此阶段首次补齐核心单元测试。

Exit Criteria:

```text
[ ] 最小、多模块和复杂 fixture regression suite 全部通过
[ ] 小型和中型真实仓库有人工核对的组件/API/依赖样本
[ ] 大型试验记录解析、内存、耗时和图可读性限制
[ ] Onboarding value 有可复现任务与观察结果
[ ] 发现的问题已修复、标为 blocker 或记录为已知限制
```

---

## T-0901 — Audit Minimal Spring Regression Fixture

Status:

```text
[ ]
```

审计在早期 analyzer 任务中建立的最小 Spring fixture，确保至少包含：

```text
Controller
Service
Repository
API
```

Acceptance Criteria:

```text
[ ] 预期输出覆盖位置、Evidence 和稳定 ID
[ ] regression suite 不执行 fixture 应用或访问网络
[ ] 所有支持行为由自动化断言验证
```

---

## T-0902 — Audit Multi-module Regression Fixture

Status:

```text
[ ]
```

用于验证：

```text
Maven Multi-module
Module ownership
Cross-module dependency
```

Acceptance Criteria:

```text
[ ] parent 与 aggregator 关系分别验证
[ ] 模块可见性、源码归属和跨模块边有明确期望
[ ] 缺失外部 parent/BOM 的 PARTIAL 路径被覆盖
```

---

## T-0903 — Audit Complex Spring Regression Fixture

Status:

```text
[ ]
```

加入：

```text
Multiple Controllers
Service-to-Service dependency
Configuration
Multiple API paths
```

Acceptance Criteria:

```text
[ ] 多构造器、接口多实现、条件映射和循环关系被覆盖
[ ] unsupported/ambiguous 情况不产生猜测结果
[ ] UI 和 Start Here 使用同一稳定报告快照
```

---

## T-0904 — Small Real Repository Validation

Status:

```text
[ ]
```

选择真实开源 Spring Boot 项目。

检查：

```text
Project Detection
Components
Endpoints
Dependencies
UI readability
```

Acceptance Criteria:

```text
[ ] 仓库 URL 与 commit 固定
[ ] 人工核对一组 Component / Endpoint / Dependency 事实
[ ] 记录误报、漏报、诊断、耗时和峰值内存
```

---

## T-0905 — Medium Real Repository Validation

Status:

```text
[ ]
```

重点验证：

```text
Noise
Performance
Graph readability
Failure tolerance
```

Acceptance Criteria:

```text
[ ] 仓库 URL、commit 和环境固定
[ ] 图筛选后仍可读，隐藏/未解析数量可见
[ ] 关键误报必须修复或成为发布 blocker
```

---

## T-0906 — Large Repository Trial

Status:

```text
[ ]
```

目标不是保证大型项目完美支持。

而是发现：

```text
Performance limits
Parser limitations
Graph usability issues
Memory problems
```

Acceptance Criteria:

```text
[ ] 记录文件/模块规模、耗时和峰值内存
[ ] 记录 parser、model 和 graph usability 的实际上限
[ ] 不为试验仓库承诺完整支持
```

---

## T-0907 — Onboarding Value Validation

Status:

```text
[ ]
```

这是 V0.1 非常关键的产品验证。

测试：

> 一个第一次接触项目的人，使用 RepoOnboard 后是否更快建立基本项目认知？

至少观察：

```text
寻找启动入口的时间
寻找 API 的时间
理解模块的时间
识别 Controller → Service 的时间
找到推荐阅读入口的时间
```

Acceptance Criteria:

```text
[ ] 使用固定任务和首次接触者场景
[ ] 记录完成时间、正确性和主观理解反馈
[ ] 明确 Start Here 是否带来可观察价值
```

---

# 20. M10 — Release Preparation

Goal:

> 将“能运行的开发项目”变成“陌生开发者愿意安装的开源项目”。

Status:

```text
○ NOT STARTED
```

Boundary:

> 把已验证功能整理为可安装、可说明、可演示的 V0.1 发布候选；不增加新的分析能力。

Exit Criteria:

```text
[ ] Windows/macOS/Linux 的 JAR 与启动脚本 smoke checks 通过
[ ] README、安装、用法、支持范围和已知限制准确
[ ] License、依赖许可、校验和和仓库清理完成
[ ] Demo 与发布 checklist 可由陌生用户复现
```

---

## T-1001 — Installation Experience

Status:

```text
[ ]
```

确保：

```text
安装步骤足够简单
```

目标最终趋向：

```bash
install RepoOnboard

cd project

repoonboard .
```

Acceptance Criteria:

```text
[ ] 可执行 JAR 包含前端资源并可离线分析 fixture
[ ] Windows 和 POSIX 启动脚本验证通过
[ ] Java 21 要求、校验和和安装步骤明确
```

---

## T-1002 — Error Messages

Status:

```text
[ ]
```

完善常见错误：

```text
Not a Maven project
Unsupported project
Invalid source
No Spring Boot detected
Partial analysis
```

此任务审计并统一前面各阶段已有的 Diagnostic，不在发布阶段首次补做容错。

Acceptance Criteria:

```text
[ ] 参数错误、FAILED、PARTIAL 使用一致退出码和文案
[ ] 错误包含可操作上下文且不泄露配置秘密
[ ] CLI 与 UI 对同一 Diagnostic 的含义一致
```

---

## T-1003 — README V0.1

Status:

```text
[ ]
```

README 至少包含：

```text
One-line Value Proposition
Demo
Installation
Usage
Features
Supported Stack
Limitations
Roadmap
Contribution
```

Acceptance Criteria:

```text
[ ] 安装和命令可由干净环境复现
[ ] 支持范围、PARTIAL 语义和已知限制准确
[ ] Demo 链接与当前发布产物一致
```

---

## T-1004 — Demo Repository

Status:

```text
[ ]
```

准备一个可以稳定展示 RepoOnboard 效果的公开项目。

Acceptance Criteria:

```text
[ ] 使用固定 commit，许可允许公开演示
[ ] 覆盖 Overview、Architecture、API 和 Start Here
[ ] 不为演示修改第三方仓库源码
```

---

## T-1005 — Demo GIF / Video

Status:

```text
[ ]
```

目标：

```text
Clone
↓
repoonboard .
↓
Scan
↓
Architecture appears
↓
Click API
↓
Start Here
```

尽量让用户：

> 10～20 秒就理解 RepoOnboard 是什么。

Acceptance Criteria:

```text
[ ] 使用真实发布候选和固定 demo 输入录制
[ ] 展示扫描、Architecture、API 和 Start Here 主路径
[ ] 不隐藏影响理解的 PARTIAL/限制提示
```

---

## T-1006 — License

Status:

```text
[ ]
```

确定开源 License。

需要在正式公开前完成。

Acceptance Criteria:

```text
[ ] 根 License 文件存在
[ ] 分发依赖和前端资产许可已核对
[ ] README 中的许可说明准确
```

---

## T-1007 — GitHub Repository Cleanup

Status:

```text
[ ]
```

确保：

```text
No secrets
No local paths
No temporary files
No unnecessary generated files
Clean commit history where appropriate
```

Acceptance Criteria:

```text
[ ] Secret、本地绝对路径和临时产物检查通过
[ ] 发布文件、Wrapper、前端锁文件和许可文件完整
[ ] git status 仅包含预期发布变更
```

---

## T-1008 — V0.1 Release

Status:

```text
[ ]
```

Acceptance Criteria:

```text
[ ] Clean build
[ ] Tests pass
[ ] Installation documented
[ ] Demo works
[ ] Real repository tested
[ ] Known limitations documented
[ ] GitHub release created
```

---

# 21. V0.1 Definition of Done

RepoOnboard V0.1 只有在以下条件全部基本满足时才算完成：

```text
[ ] Can analyze real Spring Boot Maven repositories

[ ] Can detect Maven modules

[ ] Can detect Spring components

[ ] Can detect HTTP APIs

[ ] Can build major component dependencies

[ ] Can identify application entry points

[ ] Can generate Project Overview

[ ] Can generate Architecture Map

[ ] Can generate API Map

[ ] Can generate Start Here guide

[ ] Results contain source traceability

[ ] Local Web UI works

[ ] Basic automated tests exist

[ ] Real repositories have been validated

[ ] README and demo are usable by strangers

[ ] No external AI service is required
```

---

# 22. Explicitly Deferred Beyond V0.1

以下能力当前不进入主动开发队列：

```text
Python Analyzer
TypeScript Analyzer
Go Analyzer
Rust Analyzer

Gradle deep support

Full Method-level Call Graph

Runtime Tracing

Database Schema Analysis

Git History Analysis

AI Chat

RAG

Vector Database

MCP

Coding Agent

Automatic Refactoring

Automatic Code Modification

Code Review

Technical Debt Score

VS Code Extension

JetBrains Plugin

Cloud Service

Team Workspace

User Accounts

Desktop Application

Native Installer

Bundled Java Runtime
```

这些功能只有在：

```text
V0.1 核心价值被验证
```

以后才重新评估。

---

# 23. Future Candidate Milestones

以下不是当前 Commitments。

仅作为未来候选：

```text
V0.2
Desktop Application technical spike and Windows-first packaging

V0.2+
Improve Java / Spring analysis based on V0.1 validation

V0.3
Gradle support

V0.4
Business flow analysis

V0.5
Second language ecosystem

V0.6
Change impact analysis

V0.7
AI explanation layer

V1.0
Stable multi-language architecture
```

具体版本规划应根据真实用户反馈重新决定。

---

# 24. Known Risks

当前已知高层风险：

```text
R-01
Java symbol resolution complexity

R-02
Real-world Spring Boot conventions vary widely

R-03
Dependency graph may become unreadable on larger repositories

R-04
Multi-module Maven inheritance can become complex

R-05
"Start Here" recommendations may become subjective

R-06
Future multi-language ambition may cause premature overengineering

R-07
Static analysis cannot perfectly represent runtime behavior
```

技术缓解方案由：

```text
DECISIONS.md
```

中的 ADR 管理。

---

# 25. Known Issues

当前：

```text
ISSUE-0404

Status:
OPEN / DEFERRED

Description:
MyBatis 与 MyBatis-Plus Mapper 专用分类尚未实现。

Impact:
Mapper 接口目前只作为普通 Java 声明出现，不会生成 Mapper 组件 fact；不影响标准 Spring 组件、配置、应用入口、注入候选或 M5 的核心分析。

Related Task:
T-0404
```

以后格式：

```text
ISSUE-XXXX

Status:
OPEN

Description:
...

Impact:
...

Related Task:
T-XXXX
```

---

# 26. Work / Codex Execution Rules

Work / Codex 每次处理 RepoOnboard 时必须：

```text
1. Read PROJECT.md
2. Read DECISIONS.md
3. Read TODO.md
4. Identify the explicitly assigned task
5. Work only on that task
6. Run relevant tests
7. Update TODO.md if the task status changes
8. Update DECISIONS.md only for approved architectural decisions
```

---

## Do Not Expand Scope

如果当前任务是：

```text
T-0201 Maven Project Detection
```

禁止顺手实现：

```text
Maven dependency resolution
Java parser
Spring analyzer
Frontend
AI
```

即使这些功能“顺便做很方便”。

---

## Do Not Perform Unrelated Refactors

除非当前任务明确要求：

> 不进行与当前目标无关的大规模重构。

---

## Do Not Pre-implement Future Tasks

如果发现后续需要某项能力：

```text
Add it to TODO
```

不要自动实现。

---

## Do Not Re-decide Accepted Architecture

如果 `DECISIONS.md` 中存在：

```text
Status: ACCEPTED
```

则普通 TODO 不得重新进行技术选型。

如果发现问题：

```text
Report the problem
Provide evidence
Propose a new ADR
Wait for approval
```

---

# 27. Task Completion Rules

Work / Codex 不应仅因为：

```text
Code written
```

就将任务标记为完成。

任务完成至少需要：

```text
Implementation complete

Relevant tests pass

Acceptance Criteria satisfied

No known blocker remains
```

如果测试失败：

```text
Status remains IN PROGRESS or BLOCKED
```

---

# 28. Final Report Format for AI Tasks

每次 Work / Codex 完成任务以后，最终回复尽量保持简短。

默认只报告：

```text
Task:
T-XXXX

Completed:
...

Key Changes:
...

Tests:
PASS / FAIL

Remaining Issues:
...
```

不要默认输出：

* 长篇教程
* 每个文件逐行解释
* 所有代码重复展示
* 与任务无关的改进建议

除非用户明确要求。

---

# 29. Current Next Action

当前下一步：

```text
T-0802
Recommended Reading Path
```

本阶段：

> **M8 — Start Here**

M0 至 M7 和 T-0801 已完成；可选 T-0404 继续按非阻塞规则延后。接下来只执行：

```text
T-0802 Recommended Reading Path
```

---

# 30. Current Summary

```text
RepoOnboard

Product Definition
████████████████████ 100%

Architecture Constraints
████████████████████ 100%

Technical Architecture
████████████████████ 100%

Technical Decisions
████████████████████ 100%

Implementation
M1 Project Foundation — complete
M2 Maven Project Analysis — complete
M3 Java Source Analysis — complete
M4 Spring Boot Analysis — complete
M5 API & Dependency Analysis — complete
M6 Report Assembly & Serialization — complete
M7 Local Web UI — complete
M8 Start Here — in progress (T-0801 complete, T-0802 next)

Validation
░░░░░░░░░░░░░░░░░░░░   0%

Release
░░░░░░░░░░░░░░░░░░░░   0%
```

Next:

> **T-0802 — Generate the stable, file-deduplicated recommended reading path.**

````
