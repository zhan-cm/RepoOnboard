````markdown
# RepoOnboard — Technical Decisions

> Architecture and engineering decisions for RepoOnboard.

---

# 1. Purpose

本文件用于记录 RepoOnboard 已经确认的重要：

- Architecture Decisions
- Technology Decisions
- Engineering Decisions
- Data Model Decisions
- Dependency Decisions
- Build and Distribution Decisions

其目的不是记录开发任务，而是保存：

> **为什么 RepoOnboard 采用当前实现方式。**

这样后续：

- 开发者
- Work
- Codex
- Contributor

都不需要反复重新讨论已经解决的问题。

---

# 2. Relationship with Other Project Files

RepoOnboard 使用三个核心项目管理文件：

```text
PROJECT.md
DECISIONS.md
TODO.md
````

它们的职责严格区分。

---

## PROJECT.md

回答：

> What are we building, and why?

记录：

* Product Vision
* Product Positioning
* Target Users
* V0.1 Scope
* Long-Term Direction
* Product Principles
* Explicit Non-Goals

---

## DECISIONS.md

回答：

> How have we decided to build it, and why?

记录：

* Technical Architecture
* Technology Selection
* Data Model
* Analyzer Strategy
* CLI Strategy
* Parsing Strategy
* Serialization
* Web UI Architecture
* Testing Strategy
* Packaging Strategy

---

## TODO.md

回答：

> What are we working on now?

记录：

* Milestones
* Tasks
* Progress
* Known Issues
* Deferred Tasks

---

# 3. Source of Truth Priority

如果不同文档之间发生冲突，优先级如下：

```text
PROJECT.md
    ↓
DECISIONS.md
    ↓
TODO.md
    ↓
Implementation
```

即：

### PROJECT.md

拥有最高产品约束优先级。

技术方案不能违反已经明确的产品边界。

---

### DECISIONS.md

在不违反 PROJECT.md 的前提下，是技术实现的主要依据。

已经被标记为 `ACCEPTED` 的技术决策，不应被后续任务随意改变。

---

### TODO.md

必须在现有产品约束和技术决策范围内执行。

TODO 不能自行改变架构。

---

# 4. Current Project Stage

当前 RepoOnboard 处于：

> **Maven Project Analysis**

M0 技术架构设计与 M1 工程基础已经完成。T-0101 至 T-0105 已建立工程骨架、可重复构建、分层测试底座、最小 CLI，以及符合 ADR-0007、ADR-0015 的 Evidence 与 Diagnostic 契约。当前下一任务是 T-0201 Maven Project Detection。

---

# 5. Current Decision Status

当前正式接受的具体技术栈决策：

> **ADR-0001 至 ADR-0016 已于 2026-09-08 接受。**

这些决策来自 T-0001 技术方案、T-0002 架构评审及随后明确的继续执行确认。具体约束、验证门槛和重新评估条件见 Decision Index 与正式 ADR。

Release and Distribution 仍为 DEFERRED；精确依赖补丁版本、图阈值等可逆实现细节由对应任务在 ADR 约束内确定。

---

# 6. Product Constraints Inherited from PROJECT.md

以下内容属于已经确定的产品约束。

它们不属于本文件中的可自由技术选型。

---

## 6.1 Long-Term Product Direction

RepoOnboard 长期定位为：

> **Multi-language Codebase Comprehension and Developer Onboarding Platform**

因此核心架构应允许未来支持：

```text
Java
TypeScript
Python
Go
Rust
...
```

---

## 6.2 V0.1 Scope

V0.1 仅支持：

```text
Java
+
Maven
+
Spring Boot
```

未来多语言能力不能成为 V0.1 过度设计的理由。

---

## 6.3 Static Analysis First

V0.1 核心分析优先基于：

* Source Code
* AST
* Build Metadata
* Framework Metadata
* Project Structure

---

## 6.4 No LLM Dependency in V0.1

V0.1 不依赖：

* LLM
* RAG
* Vector Database
* AI Agent
* MCP

---

## 6.5 Local First

核心代码分析必须能够：

> **Locally and Offline**

运行。

基础项目分析不应依赖外部服务器。

---

## 6.6 Explainability

关键分析结果应尽可能关联：

```text
Source File
+
Source Location
+
Analysis Evidence
```

---

## 6.7 Avoid Overengineering

V0.1 不应提前构建：

* Complex Plugin Framework
* Microservices
* Distributed Architecture
* Dynamic Extension Runtime
* Cloud Infrastructure
* Multi-Tenant Architecture

---

# 7. Decision Philosophy

RepoOnboard 的技术决策遵循以下原则。

---

## 7.1 Solve the Current Problem First

优先解决：

> V0.1 当前真实存在的问题。

而不是：

> 为未来可能存在的问题设计系统。

---

## 7.2 Mature Before Novel

如果成熟解决方案能够满足需求：

> 优先采用成熟方案。

不要为了技术炫技：

* 自己写 Java Parser
* 自己实现 Maven Parser
* 自己实现 Graph Layout Engine
* 自己实现 CLI Framework

除非确实存在无法满足需求的理由。

---

## 7.3 Deterministic Before Generative

能够通过确定性程序分析获得的信息，不交给生成式模型推断。

例如：

```java
//@RestController
```

应该通过 AST / Annotation 分析识别。

而不是询问 LLM：

> “这个类是不是 Controller？”

---

## 7.4 Simple Before Flexible

优先：

```text
Simple Architecture
```

而不是：

```text
Highly Flexible Architecture
```

除非灵活性已经存在明确需求。

---

## 7.5 Evidence Before Assumption

技术决策应该基于：

* 实验
* Prototype
* Benchmark
* Documentation
* Real Repository Validation

而不是：

> “感觉这个库应该不错。”

---

## 7.6 Real Repositories Before Synthetic Success

一个 Analyzer 在 Fixture Project 上运行成功：

> 不代表真正可用。

关键技术方案必须逐步在真实 Spring Boot Repository 上验证。

---

## 7.7 Reversible Decisions Can Stay Simple

如果一个决策以后容易替换：

> 不需要为它投入过多架构成本。

例如某些：

* UI Components
* Serialization details
* Styling tools

---

## 7.8 Hard-to-Reverse Decisions Require More Review

如果一个决策会影响整个项目：

例如：

* Core Project Model
* Analyzer Interface
* Graph Representation
* Module Boundary

则必须更加谨慎。

---

# 8. Decision Status Model

每一个 Decision 必须有明确状态。

允许的状态如下：

---

## PROPOSED

已经提出方案，但还没有正式采用。

---

## ACCEPTED

已经确认并进入项目实施。

后续 Work / Codex 必须遵守。

---

## REJECTED

经过评估但决定不采用。

保留原因，以防未来重复讨论。

---

## DEFERRED

目前暂不决定。

等待：

* 更多需求
* Prototype
* Benchmark
* 后续阶段

---

## SUPERSEDED

曾经采用，但后来被新的 Decision 替代。

旧 Decision 不删除。

需要链接新的 Decision ID。

---

# 9. Decision ID Convention

每个正式技术决策使用：

```text
ADR-XXXX
```

例如：

```text
ADR-0001
ADR-0002
ADR-0003
```

ADR：

> Architecture Decision Record

即：

> 架构决策记录。

---

# 10. Decision Index

当前 Decision Index：

| ID | Decision | Status | Date |
| --- | --- | --- | --- |
| ADR-0001 | Use Java 21 for the RepoOnboard Runtime | ACCEPTED | 2026-09-08 |
| ADR-0002 | Build with Maven and Maven Wrapper | ACCEPTED | 2026-09-08 |
| ADR-0003 | Use Picocli for the CLI | ACCEPTED | 2026-09-08 |
| ADR-0004 | Parse Maven Projects through a Restricted Model Builder | ACCEPTED | 2026-09-08 |
| ADR-0005 | Use JavaParser for Java Source Analysis | ACCEPTED | 2026-09-08 |
| ADR-0006 | Limit Symbol Resolution to Confirmed Project-local Types | ACCEPTED | 2026-09-08 |
| ADR-0007 | Use a Minimal Generic Project Model with Source Evidence | ACCEPTED | 2026-09-08 |
| ADR-0008 | Keep Explicit Analyzer Boundaries in One Local Process | ACCEPTED | 2026-09-08 |
| ADR-0009 | Represent Dependencies as Typed Directed Edges | ACCEPTED | 2026-09-08 |
| ADR-0010 | Serialize Versioned Reports as JSON | ACCEPTED | 2026-09-08 |
| ADR-0011 | Serve the Local UI with JDK HttpServer | ACCEPTED | 2026-09-08 |
| ADR-0012 | Build the UI with Vue, Vite, and Cytoscape.js | ACCEPTED | 2026-09-08 |
| ADR-0013 | Generate Start Here with Explainable Heuristics | ACCEPTED | 2026-09-08 |
| ADR-0014 | Use Layered Fixture and Real-repository Testing | ACCEPTED | 2026-09-08 |
| ADR-0015 | Preserve Partial Results and Structured Diagnostics | ACCEPTED | 2026-09-08 |
| ADR-0016 | Package V0.1 as a JAR with Launcher Scripts | ACCEPTED | 2026-09-08 |

---

# 11. Technical Decision Queue and Resolution

以下条目保留最初问题及评估背景。T-0003 后，它们的当前结论由对应 ADR 决定：

> **D-01 至 D-19 已解决；D-20 仍为 DEFERRED。**

---

## D-01 — Java Runtime Version

需要决定：

> RepoOnboard 自身最低运行在哪个 Java 版本？

候选可能包括：

* Java 17
* Java 21

需要考虑：

* LTS
* Library Compatibility
* Distribution
* Contributor Environment
* Target User Environment

注意：

> RepoOnboard 自己使用的 Java 版本，与被分析项目使用的 Java 版本不是同一个概念。

RepoOnboard 应尽可能分析不同 Java Version 的目标项目。

Status:

```text
RESOLVED — ADR-0001
```

---

## D-02 — Project Build Strategy

需要确定 RepoOnboard 自身：

* Maven
* Gradle

或其他构建策略。

考虑：

* Java Ecosystem Compatibility
* Contributor Familiarity
* Packaging
* Release
* Dependency Management

Status:

```text
RESOLVED — ADR-0002
```

---

## D-03 — CLI Framework

需要确定：

```bash
repoonboard .
```

如何实现。

候选方案需要比较：

* CLI Parsing
* Subcommands
* Help Generation
* Error Handling
* Packaging
* Cross-platform Behavior

Status:

```text
RESOLVED — ADR-0003
```

---

## D-04 — Maven Project Analysis

需要确定如何解析：

```text
pom.xml
```

并获取：

* groupId
* artifactId
* Parent
* Modules
* Java Version
* Spring Boot Version
* Dependencies
* Dependency Management

必须考虑：

* Parent POM
* Properties
* Multi-module
* Inherited values

Status:

```text
RESOLVED — ADR-0004
```

---

## D-05 — Java AST Strategy

需要选择 Java Source Analysis 方案。

需要评估：

* JavaParser
* Spoon
* Eclipse JDT
* Other mature alternatives

评价指标：

```text
Parsing Accuracy
Symbol Resolution
Source Location Support
Java Version Support
API Complexity
Maintenance
Performance
Integration Complexity
```

Status:

```text
RESOLVED — ADR-0005
```

---

## D-06 — Symbol Resolution Scope

需要明确：

> V0.1 到底需要多深的 Symbol Resolution？

可能包括：

```text
No Resolution
↓
Project-local Resolution
↓
Dependency-aware Resolution
↓
Near-compiler-level Resolution
```

原则：

> 不为了追求完整编译器级语义分析，让 V0.1 复杂度失控。

Status:

```text
RESOLVED — ADR-0006
```

---

## D-07 — Generic Project Model

这是一个高影响决策。

需要确定 RepoOnboard Core 如何表示：

```text
Project
Module
SourceFile
Component
Endpoint
EntryPoint
Dependency
SourceLocation
```

目标：

> 支持当前 Spring Boot，同时不与 Spring Boot 语义强绑定。

但禁止为了未来几十种语言建立复杂的 Universal AST。

Status:

```text
RESOLVED — ADR-0007
```

---

## D-08 — Language / Framework Analyzer Boundary

需要明确：

```text
RepoOnboard Core
```

与：

```text
Java Analyzer
Spring Boot Analyzer
Maven Analyzer
```

之间的边界。

需要解决：

> 哪些逻辑属于 Core？

以及：

> 哪些逻辑属于语言 / 框架适配层？

Status:

```text
RESOLVED — ADR-0008
```

---

## D-09 — Dependency Graph Representation

需要决定如何表示：

```text
Component
      ↓
Dependency
      ↓
Component
```

V0.1 重点支持：

```text
Controller → Service
Service → Service
Service → Repository
Service → Mapper
Component → Component
```

需要确定：

* 内部对象模型
* Graph representation
* Graph algorithm requirements
* 是否真正需要第三方 graph library

Status:

```text
RESOLVED — ADR-0009
```

---

## D-10 — API Endpoint Model

需要设计 HTTP Endpoint 的统一模型。

至少包括：

```text
HTTP Method
Path
Handler Component
Handler Method
Source Location
Framework
```

应尽量避免模型被 Spring MVC API 完全锁死。

Status:

```text
RESOLVED — ADR-0007
```

---

## D-11 — Source Traceability Model

关键分析结果需要追踪：

```text
Source File
Line
Column
Symbol
Evidence
```

需要确定：

> 哪些信息必须进入 Core Model？

Status:

```text
RESOLVED — ADR-0007
```

---

## D-12 — Serialization Format

需要确定 Analyzer 到 UI 之间的数据交换形式。

候选可能包括：

```text
JSON
```

或其他简单结构。

原则：

* Human Inspectable
* Debuggable
* Stable
* Easy for Frontend
* No unnecessary database

Status:

```text
RESOLVED — ADR-0010
```

---

## D-13 — Local Web Architecture

需要确定：

```text
repoonboard .
```

之后如何展示 UI。

候选方向可能包括：

```text
Embedded HTTP Server
```

或：

```text
Generated Static Site
```

等方案。

需要考虑：

* User Experience
* Distribution
* Security
* Cross-platform
* Implementation Complexity

Status:

```text
RESOLVED — ADR-0011
```

---

## D-14 — Frontend Strategy

需要确定 V0.1 Web UI 是否使用：

* Vanilla HTML / JS
* React
* Vue
* Other lightweight solution

决策不能只依据：

> “哪个框架更流行。”

应根据：

* Interactive Graph Requirements
* Bundle Size
* Development Speed
* Maintainability
* Distribution Complexity

选择。

Status:

```text
RESOLVED — ADR-0012
```

---

## D-15 — Architecture Visualization

需要选择适合展示：

```text
Module Graph
Component Graph
Dependency Graph
```

的技术方案。

需要重点验证：

> 中等规模项目是否依然可读。

不应只看 Demo 是否漂亮。

Status:

```text
RESOLVED — ADR-0012
```

---

## D-16 — Start Here Algorithm

需要决定：

> 推荐阅读顺序如何产生。

V0.1 优先考虑：

```text
Explainable Heuristic
```

而不是 AI。

可能使用：

* Entry Point
* API Exposure
* Dependency Centrality
* Component Role
* Module Importance
* Configuration Importance

最终每个推荐结果必须能够提供：

```text
Why this file?
```

Status:

```text
RESOLVED — ADR-0013
```

---

## D-17 — Testing Strategy

需要明确至少四层测试：

```text
Unit Tests

Fixture Project Tests

Integration Tests

Real Repository Validation
```

需要定义：

* Fixture structure
* Expected outputs
* Regression strategy
* Real repository validation process

Status:

```text
RESOLVED — ADR-0014
```

---

## D-18 — Error Handling Strategy

RepoOnboard 分析真实仓库时必然遇到：

* Broken Java Source
* Unsupported Syntax
* Missing Dependencies
* Invalid POM
* Partial Modules
* Generated Code
* Unknown Framework Pattern

需要明确：

> 一个文件失败，不应该轻易让整个分析失败。

Status:

```text
RESOLVED — ADR-0015
```

---

## D-19 — Packaging Strategy

需要确定用户最终如何获得：

```bash
repoonboard
```

需要研究：

* Executable JAR
* Native Binary
* Script Wrapper
* Package Manager Distribution

V0.1 应优先：

> 简单、可靠、可维护。

Native Image 等复杂方案不应默认成为首选。

Status:

```text
RESOLVED — ADR-0016
```

---

## D-20 — Release and Distribution

未来需要确定：

* GitHub Releases
* Maven Central
* Homebrew
* Scoop
* Winget
* Other package managers

但 V0.1 开发早期：

> 不需要立即同时支持所有分发渠道。

Status:

```text
DEFERRED
```

---

# 12. Architecture Decision Record Template

每次确认正式技术决策时，使用下面模板。

---

## ADR-XXXX — Decision Title

### Status

```text
PROPOSED | ACCEPTED | REJECTED | DEFERRED | SUPERSEDED
```

### Date

```text
YYYY-MM-DD
```

### Context

描述：

> 为什么需要做这个决定？

包括：

* 当前问题
* 项目约束
* 技术背景
* 为什么现在必须决定

---

### Decision

明确写出最终决定：

> We will use ...

避免模糊描述。

---

### Alternatives Considered

列出真正考虑过的方案。

例如：

```text
Option A
Option B
Option C
```

不要为了填模板加入没有认真考虑过的方案。

---

### Evaluation

比较关键维度，例如：

| Criterion   | Option A | Option B | Option C |
| ----------- | -------: | -------: | -------: |
| Complexity  |          |          |          |
| Reliability |          |          |          |
| Maintenance |          |          |          |
| Performance |          |          |          |
| Ecosystem   |          |          |          |
| V0.1 Fit    |          |          |          |

只有在表格确实有帮助时才使用。

---

### Rationale

说明：

> 为什么最终选择这个方案？

重点记录未来开发者无法从代码中直接看出的原因。

---

### Consequences

记录决策带来的：

#### Positive

* ...

#### Negative

* ...

#### Constraints

* ...

---

### Validation

如果决策需要实际验证，记录：

```text
Prototype:
Benchmark:
Fixture:
Real Repository:
```

---

### Revisit Conditions

说明什么情况下应该重新评估这个决策。

例如：

```text
Revisit if:

- JavaParser cannot correctly parse target repositories.
- Symbol resolution accuracy falls below acceptable level.
- Java language support becomes a blocking issue.
```

---

### Related Decisions

```text
ADR-XXXX
ADR-YYYY
```

---

# 13. Example Decision

以下仅展示格式。

**不是当前已确认技术决策。**

---

## ADR-EXAMPLE — Java AST Library

### Status

```text
PROPOSED
```

### Date

```text
YYYY-MM-DD
```

### Context

RepoOnboard 需要解析 Java 源码，以识别：

* Classes
* Methods
* Annotations
* Fields
* Constructors
* Source Locations
* Type References

自行实现 Java Parser 不符合 V0.1 的工程目标。

---

### Decision

Pending technical evaluation.

---

### Alternatives Considered

* JavaParser
* Spoon
* Eclipse JDT

---

### Evaluation Criteria

重点比较：

* Java Syntax Support
* Symbol Resolution
* Source Position
* API Complexity
* Documentation
* Performance
* Maintenance Activity
* Integration Cost

---

### Rationale

Pending.

---

### Consequences

Pending.

---

# 14. Decision Change Policy

已经 `ACCEPTED` 的 Decision：

> 不应直接编辑成另外一个结论。

如果未来技术方案改变：

创建一个新的 ADR。

例如：

```text
ADR-0003
JavaParser selected
```

后来改为 Spoon：

不要删除 ADR-0003。

应该：

```text
ADR-0017
Replace JavaParser with Spoon
```

并把：

```text
ADR-0003

Status:
SUPERSEDED

Superseded by:
ADR-0017
```

这样项目可以保留真实技术演化历史。

---

# 15. What Should Not Be Recorded Here

DECISIONS.md 不记录普通开发细节。

例如：

```text
Fix null pointer exception
```

不需要 ADR。

```text
Rename UserAnalyzer to ComponentAnalyzer
```

通常不需要 ADR。

```text
Add one test fixture
```

不需要 ADR。

---

只有满足类似以下条件时才值得记录：

### High Impact

影响多个模块。

### Hard to Reverse

以后替换成本较高。

### Architecturally Important

影响系统结构。

### Repeated Debate Likely

以后很可能再次被讨论。

### Non-obvious

无法单纯从代码看出为什么这么做。

---

# 16. Work / Codex Decision Rules

Work / Codex 在进行 RepoOnboard 开发时必须遵守：

1. 首先阅读 `PROJECT.md`。
2. 然后阅读本 `DECISIONS.md`。
3. `ACCEPTED` Decision 默认不可被当前普通任务修改。
4. 如果认为现有 Decision 有问题，不应直接重写实现。
5. 应明确提出：

    * Existing Decision
    * Problem
    * Evidence
    * Proposed Alternative
6. 只有经过明确确认以后才能建立新的 ADR 并修改架构。
7. 不应该因为某个库“更现代”就自动替换已经工作的方案。
8. 不应该为了未来多语言目标推翻当前简单可用的 V0.1 设计。
9. 未决技术问题不能擅自伪装成已经确定的决定。
10. 新的重大技术选择完成后，应更新本文件。

---

# 17. Decision Review Checklist

在把某个 Decision 标记为 `ACCEPTED` 之前，应检查：

### Product Fit

是否符合 PROJECT.md？

### V0.1 Fit

是否真正服务当前版本？

### Complexity

是否引入不必要复杂度？

### Reliability

方案是否成熟可靠？

### Maintainability

后续维护成本是否合理？

### Testability

是否容易测试？

### Portability

是否影响 Windows / Linux / macOS？

### Offline Capability

是否破坏核心本地分析能力？

### Extensibility

是否把 Core 与 Spring Boot 不必要地锁死？

### Overengineering

是否只是为了“以后也许需要”？

### Evidence

有没有文档、Prototype 或真实 Repository 验证支持？

---

# 18. Current Technical Decision Roadmap

技术架构设计阶段建议按以下顺序做决定：

```text
1. Java Runtime
       ↓
2. Build Strategy
       ↓
3. Maven Analysis
       ↓
4. Java AST
       ↓
5. Symbol Resolution
       ↓
6. Generic Project Model
       ↓
7. Analyzer Boundaries
       ↓
8. Dependency Graph
       ↓
9. Serialization
       ↓
10. CLI
       ↓
11. Local Web Architecture
       ↓
12. Frontend
       ↓
13. Graph Visualization
       ↓
14. Testing Strategy
       ↓
15. Packaging
```

这里的顺序不是绝对开发顺序。

它表示：

> 哪些基础技术决策会影响后续决策。

---

# 19. Current Architecture Assumptions

当前允许在技术设计阶段使用以下假设：

```text
RepoOnboard
      ↓
CLI
      ↓
Project Detection
      ↓
Build Analyzer
      +
Source Analyzer
      +
Framework Analyzer
      ↓
Unified Project Model
      ↓
Analysis Engine
      ↓
Presentation Model
      ↓
Local Web UI
```

注意：

> 这只是当前架构假设，不是正式 ADR。

技术方案可以提出更简单、更合理的结构。

但任何替代方案都必须符合 PROJECT.md 中的产品原则。

---

# 20. Decision Quality Standard

一个好的 RepoOnboard Technical Decision 应该能够让未来开发者回答：

```text
What did we choose?

Why did we choose it?

What alternatives did we reject?

What did we gain?

What did we give up?

When should we reconsider it?
```

如果一个 Decision 无法回答这些问题：

> 它还不足以成为一个完整的 Architecture Decision Record.

---

# 21. Accepted Architecture Decision Records

以下 ADR 由 [V0.1 技术架构方案](docs/architecture/V0.1-TECHNICAL-PROPOSAL.md) 和 [T-0002 架构评审](docs/architecture/T-0002-ARCHITECTURE-REVIEW.md) 产生。状态变更遵循本文件第 14 节；实现中的具体补丁版本和低影响细节不能改变 ADR 的边界。

## ADR-0001 — Use Java 21 for the RepoOnboard Runtime

### Status

```text
ACCEPTED
```

### Date

```text
2026-09-08
```

### Context and Decision

RepoOnboard 需要统一的本地开发、测试和发布运行时。工具自身最低使用 Java 21，并以 `--release 21` 编译。该版本只约束 RepoOnboard；被分析项目的 Java 版本由解析器兼容矩阵决定。

### Alternatives and Rationale

Java 17 的安装覆盖更广，但会限制工具内部可用语言能力；高于 21 的版本会进一步提高用户门槛。Java 21 作为 LTS 在能力和可获得性之间更适合当前项目。

### Consequences and Validation

最终用户需要 Java 21，发布前必须在 Windows、macOS、Linux 验证启动。Java 8/11/17/21 目标源码通过 fixture 验证，不把运行 JDK 当作目标项目版本。若 Java 21 成为明显安装阻力或关键依赖要求更高版本，创建新 ADR 复审。

---

## ADR-0002 — Build with Maven and Maven Wrapper

### Status

```text
ACCEPTED
```

### Date

```text
2026-09-08
```

### Context and Decision

RepoOnboard 使用 Maven 3.9 系列和 Maven Wrapper 构建。V0.1 初期保持一个 Java 构建模块，通过包边界组织代码；前端发布构建由 Maven 打包阶段显式整合，不要求普通 Java 单测每次构建前端。

### Alternatives and Rationale

Gradle 能完成同样任务，但引入第二套构建心智模型没有当前收益。多个 Maven 模块能强化编译隔离，也会增加依赖和发布配置。当前规模下 Maven 单模块最简单。

### Consequences and Validation

命令行构建必须可重复，Wrapper 版本和校验信息需要提交。M1 验证 clean/test/verify，前端加入后验证锁文件和资源打包。Maven 4 迁移、拆分模块或替换构建工具需要证据和新 ADR。

---

## ADR-0003 — Use Picocli for the CLI

### Status

```text
ACCEPTED
```

### Date

```text
2026-09-08
```

### Context and Decision

命令 `repoonboard .` 需要稳定处理路径、帮助、版本、输出选项、端口、错误消息和退出码。CLI 使用 Picocli 4 系列；CLI 层只负责输入、进度和生命周期，不包含分析规则。

### Alternatives and Rationale

手写参数解析在单参数原型中更小，但随着选项和错误场景增加会形成重复基础设施。Spring Shell 超出当前非交互 CLI 的需要。Picocli 足够轻量且便于测试。

### Consequences and Validation

M1 验证 `.`、非法路径、help、version 和退出码；M7 再验证服务生命周期。若 CLI 永久保持单参数且 Picocli 显著影响包体或启动，才重新评估。

---

## ADR-0004 — Parse Maven Projects through a Restricted Model Builder

### Status

```text
ACCEPTED
```

### Date

```text
2026-09-08
```

### Context and Decision

Maven 元数据涉及 parent、属性、profile、dependencyManagement 和多模块关系。分析器使用 Maven 3.9 Model Reader/Model Builder，但所有根 POM、相对 parent、BOM 和本地仓库 POM 都必须经过统一受限读取层。核心扫描不调用目标项目 Maven、不执行 lifecycle、插件、扩展或网络解析。

Profile 只支持显式选择和 activeByDefault；不注册宿主 OS、JDK、文件或属性隐式 activator。模型构建失败时保留 raw model 和可用字段，字段带原始值、解析状态和来源。

### Alternatives and Rationale

纯 XML 适合读取简单字段，却无法可靠重建完整 Maven 继承语义。调用 `mvn help:effective-pom` 会依赖目标环境并可能执行不受控扩展。受限 Model Builder 能复用成熟语义，同时守住本地静态分析边界。

### Consequences and Validation

T-0002 实验验证了宿主 OS profile 和 `relativePath` 绕过解析器的风险，但只使用本地 Maven 3.6.1 API。M1/M2 必须用最终锁定的 3.9 版本验证有界 ModelSource、拒绝型 resolver、XXE/DTD、资源上限、缺 parent/BOM、profile 和无网络行为。无法实现这些约束时重新评审本 ADR。

---

## ADR-0005 — Use JavaParser for Java Source Analysis

### Status

```text
ACCEPTED
```

### Date

```text
2026-09-08
```

### Context and Decision

Java 分析需要类、方法、字段、构造器、注解、类型引用和源码位置。使用 JavaParser 3 系列生成 AST，并立刻转成 RepoOnboard 内部 Java facts；第三方 AST 不进入公共模型或序列化结果。

### Alternatives and Rationale

Spoon 提供更丰富的分析与转换模型，但 V0.1 不修改源码。Eclipse JDT binding 能力强，环境配置也更接近编译器。当前只读、部分成功的提取范围下，JavaParser API 更直接。

### Consequences and Validation

T-0303 前用锁定版本验证 Java 8/11/17/21 常见语法、位置、损坏文件和解析恢复。若常见目标仓库出现系统性语法或位置错误，使用同一 fixture 比较 Spoon/JDT，并通过替代 ADR 决策。

---

## ADR-0006 — Limit Symbol Resolution to Confirmed Project-local Types

### Status

```text
ACCEPTED
```

### Date

```text
2026-09-08
```

### Context and Decision

V0.1 建立模块可见的源码声明索引，按全限定名、显式 import、同包、唯一可确定的通配 import 和确认的 Maven 模块依赖解析类型。JavaSymbolSolver 只在共享 AST 上按需使用。歧义和缺失目标保留为 unresolved，不创建确定关系。

### Alternatives and Rationale

完全按简单类名匹配会产生误报；下载完整 classpath 或追求编译器级解析会扩大离线、性能和容错成本。项目内确认解析能支持主要组件关系，并符合准确优先原则。

### Consequences and Validation

接口注入不绑定到猜测实现，外部类型不创建仓库内节点。T-0304/T-0503 覆盖同名类型、嵌套声明、跨模块可见性、多实现和缺依赖。只有明确的用户价值被此范围阻塞时才提高解析深度。

---

## ADR-0007 — Use a Minimal Generic Project Model with Source Evidence

### Status

```text
ACCEPTED
```

### Date

```text
2026-09-08
```

### Context and Decision

核心模型包含 Project、Module、SourceFile、Component、Endpoint、EntryPoint、Dependency、SourceLocation、Evidence、Diagnostic 和 AnalysisReport。模型使用 Java records/enums 表达稳定值，实体通过稳定结构身份引用；Spring 语义以 kind/framework 标识映射，不创建通用 AST。

Endpoint 保存方法、路径、handler、条件、解析状态和类/方法级证据。源码位置使用 1-based 行列，未知值为空。默认报告使用扫描根相对路径，不包含整份源码或用户绝对路径。

### Alternatives and Rationale

Spring 专用核心会阻碍长期方向；通用语言类型系统会让 V0.1 复杂化；直接序列化解析器对象会形成技术锁定。最小语义模型服务当前界面，同时保留证据。

### Consequences and Validation

T-0601 固化字段，T-0602 用 fixture 确定 ID 编码、排序、冲突和重命名行为。ADR 只要求稳定身份不依赖内存地址或遍历顺序，暂不固定 SHA-256 等编码细节。新实体必须由当前产品需求驱动。

---

## ADR-0008 — Keep Explicit Analyzer Boundaries in One Local Process

### Status

```text
ACCEPTED
```

### Date

```text
2026-09-08
```

### Context and Decision

RepoOnboard 在一个本地 Java 进程中按顺序编排 Maven、Java、Spring、关系整理、序列化和展示。代码按 `core.model`、`analyzer.maven`、`analyzer.java`、`analyzer.spring`、`analysis`、`serialization`、`web`、`cli` 边界组织。Core 不依赖框架/解析器类型，Spring 分析器消费 Java facts。

### Alternatives and Rationale

动态插件、多服务或多个发布单元会增加部署和错误边界；将所有逻辑放入一个扫描器又难以测试与扩展。单进程加显式包边界满足 V0.1。

### Consequences and Validation

M1 可增加架构依赖测试以防反向依赖。只有包边界持续无法约束耦合、构建时间或独立发布需求出现时，才考虑拆分 Maven 模块；动态插件留到第二生态出现后评估。

---

## ADR-0009 — Represent Dependencies as Typed Directed Edges

### Status

```text
ACCEPTED
```

### Date

```text
2026-09-08
```

### Context and Decision

组件关系以带类型、解析状态和证据的有向边列表存储，查询时建立邻接表。Maven 模块/构建依赖与组件依赖分别建图。循环合法保存，未解析目标保留名称和诊断，默认不画为确定边。

### Alternatives and Rationale

图数据库和通用 Java 图算法库可以提供复杂查询，但 V0.1 只需要筛选、统计、邻域和基础中心性。简单集合模型更容易序列化和解释。

### Consequences and Validation

T-0503/T-0604 验证去重、多证据、循环、未解析关系和稳定排序。出现确需复杂算法且自有实现开始膨胀时，再评估 JGraphT 等图库。

---

## ADR-0010 — Serialize Versioned Reports as JSON

### Status

```text
ACCEPTED
```

### Date

```text
2026-09-08
```

### Context and Decision

分析器与 UI 使用 UTF-8 JSON 交换数据，外层报告包含 `schemaVersion`。Java 端使用 Jackson 3 系列序列化显式 DTO；禁用任意多态类型反序列化，集合按稳定键输出。

### Alternatives and Rationale

数据库、二进制协议和专用图格式增加迁移及调试成本。JSON 可人工检查、适合浏览器并支持离线产物。Gson 可行，但 Jackson 更适合明确 DTO 和后续兼容配置。

### Consequences and Validation

T-0603 定义主/次版本兼容、快照和未知版本错误。M1 用最终版本验证 Java 21 兼容。若报告规模使解析或内存不可接受，应先测量和分片，再决定格式变化。

---

## ADR-0011 — Serve the Local UI with JDK HttpServer

### Status

```text
ACCEPTED
```

### Date

```text
2026-09-08
```

### Context and Decision

CLI 分析完成后，用 JDK `HttpServer` 在 `127.0.0.1` 提供打包静态资源和当前只读报告接口，默认使用系统分配端口。服务不提供任意文件读取、不运行目标代码，浏览器打开失败时输出地址。

### Alternatives and Rationale

`file://` 对资源和数据加载限制较多；嵌入 Spring Boot 服务端会引入容器和额外生命周期。当前路由很少，JDK 自带服务器足够。

### Consequences and Validation

M7 验证路径穿越、Host/同源限制、内容安全策略、并发、端口、关闭和资源释放。若只读需求超出可维护范围，或 HttpServer 行为成为跨平台阻塞，再评估轻量服务器库。

---

## ADR-0012 — Build the UI with Vue, Vite, and Cytoscape.js

### Status

```text
ACCEPTED
```

### Date

```text
2026-09-08
```

### Context and Decision

本地 UI 使用 Vue 3 + JavaScript + Vite，提供 Overview、Architecture、API Map、Start Here 和诊断入口。图交互使用 Cytoscape.js，默认先按模块、类型和组件邻域缩小结果，并始终提供列表视图。全部资源随 JAR 发布，不使用运行时 CDN。

### Alternatives and Rationale

原生 JavaScript 对静态页面足够，但四个视图的筛选和详情状态会分散；React/Vue 都可满足。Vue 的模板和响应式组织适合当前小团队。D3 和自绘 SVG 更底层，Cytoscape 提供现成图交互。

### Consequences and Validation

开发和发布构建需要 Node，最终用户不需要。M7/M9 用真实图验证可读性、节点预算和恶意文本渲染。复杂布局插件、TypeScript 和图阈值延后，以实际数据决定。

---

## ADR-0013 — Generate Start Here with Explainable Heuristics

### Status

```text
ACCEPTED
```

### Date

```text
2026-09-08
```

### Context and Decision

Start Here 使用确定规则生成阅读路径：根 POM、应用入口、直接识别的配置、公开 API Controller，以及与前述组件存在确定边的 Service/Repository。推荐按文件去重并附带来源事实和计数，partial 分析必须标注依据不完整。

### Alternatives and Rationale

AI 排名、业务语义猜测或不透明分数不符合 V0.1。可解释启发式能说明为什么推荐，并便于 fixture 回归。

### Consequences and Validation

T-0801/T-0907 验证排序稳定性和真实上手价值。只有用户验证显示规则无帮助时才调整权重；任何新规则仍需可解释证据。

---

## ADR-0014 — Use Layered Fixture and Real-repository Testing

### Status

```text
ACCEPTED
```

### Date

```text
2026-09-08
```

### Context and Decision

Java 端使用 JUnit Jupiter 6 系列。每个分析任务从单元测试和最小 fixture 开始，随后做集成、CLI 和固定 commit 的真实仓库验证。前端使用 Vitest；关键浏览流程在 M7 后用 Playwright。Fixture 不运行目标应用，不依赖远端下载目标依赖。

### Alternatives and Rationale

只测人工 fixture 无法代表真实 Maven/Spring 结构；只测真实仓库又难以定位回归。分层测试兼顾确定性和实际覆盖。

### Consequences and Validation

测试依赖随功能引入，不在 M1 一次搭完。真实仓库记录人工期望、误报、漏报、耗时和内存。若测试运行时间增长，先分层执行而不是删除关键准确性验证。

---

## ADR-0015 — Preserve Partial Results and Structured Diagnostics

### Status

```text
ACCEPTED
```

### Date

```text
2026-09-08
```

### Context and Decision

每个阶段返回事实与结构化 Diagnostic，整体状态为 SUCCESS、PARTIAL 或 FAILED。单文件、子模块、缺 parent/BOM、未解析类型等局部问题产生可定位诊断并继续；只有输入无效或无法确定分析范围等情况才终止。

### Alternatives and Rationale

任何解析失败即终止会让真实仓库体验脆弱；吞掉错误又会把不完整结果伪装成准确结论。部分结果加显式覆盖状态更符合产品目标。

### Consequences and Validation

CLI 和 UI 必须显著展示 partial 状态，退出码区分参数错误、失败和部分成功。各 analyzer fixture 验证继续条件、诊断位置和不可继续边界；不允许用异常兜底掩盖编程缺陷。

---

## ADR-0016 — Package V0.1 as a JAR with Launcher Scripts

### Status

```text
ACCEPTED
```

### Date

```text
2026-09-08
```

### Context and Decision

V0.1 提供包含前端资源的可执行 JAR，以及 Windows 和 POSIX 启动脚本。发布物包含校验和和必要依赖许可信息；运行时要求 Java 21。具体公开渠道由发布阶段决定。

### Alternatives and Rationale

Native Image、自带 JRE、平台安装器和多个包管理器会扩大构建矩阵。JAR 与脚本是进入真实验证的最小可靠交付方式。

### Consequences and Validation

M10 在 Windows、macOS、Linux 做离线分析和启动冒烟测试。若 Java 安装成为主要采用障碍，再通过新 ADR 评估 jlink/jpackage、Native Image 或自带运行时。

---

# 22. Current Summary

当前阶段：

```text
Product Direction
✓ Defined

V0.1 Scope
✓ Defined

Architecture Constraints
✓ Defined

Technical Stack
✓ Accepted for V0.1

Architecture Decisions
✓ ADR-0001 through ADR-0016 accepted

Implementation
✓ M1 Project Foundation complete
○ M2 Maven Project Analysis next
```

下一步：

> 完成 T-0201 — Maven Project Detection。

M0、M1 已关闭；后续按 TODO.md 校准后的里程碑和任务依赖推进。

````
