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

> **Technical Architecture Design**

目前尚未进入完整编码阶段。

---

# 5. Current Decision Status

当前正式接受的具体技术栈决策：

> **None yet.**

以下内容目前仍待技术方案阶段确认：

* Java Runtime Version
* Build Configuration
* CLI Framework
* Maven Parsing Strategy
* Java AST Parser
* Symbol Resolution Strategy
* Generic Project Model
* Dependency Graph Representation
* Spring Boot Analyzer Architecture
* Serialization Format
* Local Web Server
* Frontend Architecture
* Graph Visualization Library
* Testing Stack
* Packaging Strategy
* Distribution Strategy

在完成第一次 Technical Architecture Review 前：

> **不得把候选方案写成已确认方案。**

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

| ID | Decision                             | Status | Date |
| -- | ------------------------------------ | ------ | ---- |
| —  | No technical decisions finalized yet | —      | —    |

当技术方案确认以后，在这里维护索引。

例如未来可能变成：

| ID       | Decision               | Status   | Date       |
| -------- | ---------------------- | -------- | ---------- |
| ADR-0001 | Java Runtime Version   | ACCEPTED | YYYY-MM-DD |
| ADR-0002 | CLI Framework          | ACCEPTED | YYYY-MM-DD |
| ADR-0003 | Java AST Parser        | ACCEPTED | YYYY-MM-DD |
| ADR-0004 | Maven Parsing Strategy | ACCEPTED | YYYY-MM-DD |

---

# 11. Open Technical Decisions

以下内容目前是：

> **Decision Queue**

它们不是最终方案。

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
OPEN
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
OPEN
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
OPEN
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
OPEN
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
OPEN
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
OPEN
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
OPEN
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
OPEN
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
OPEN
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
OPEN
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
OPEN
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
OPEN
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
OPEN
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
OPEN
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
OPEN
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
OPEN
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
OPEN
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
OPEN
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
OPEN
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

# 21. Current Summary

当前阶段：

```text
Product Direction
✓ Defined

V0.1 Scope
✓ Defined

Architecture Constraints
✓ Defined

Technical Stack
○ Not finalized

Architecture Decisions
○ Pending

Implementation
○ Not started
```

下一步：

> 完成 RepoOnboard V0.1 Technical Architecture Proposal。

技术方案经过 Review 后：

1. 将确认的技术选择转换成正式 ADR。
2. 更新 Decision Index。
3. 创建 / 更新 TODO.md。
4. 再开始正式项目骨架和编码工作。

````

这版 `DECISIONS.md` 有一个很重要的设计：**现在没有假装我们已经选好了技术栈。**

所以等你把 `PROJECT.md + DECISIONS.md` 交给 Work 后，它应该先帮我们比较：

```text
JavaParser vs Spoon vs JDT
Picocli 是否合适
React/Vue/纯前端哪个合适
JSON还是其他模型
怎么做 Maven resolution
Symbol Resolution 做到什么程度
Core 和 Spring Analyzer 怎么分
````

真正决定以后，我们才把：

```text
D-05 — Java AST Strategy
```

转化成类似：

```text
ADR-0003 — Use JavaParser for Java Source Analysis

Status: ACCEPTED
```

这样以后 Work 或 Codex 就不能每开一个任务又问一次：

> “要不要换成 Spoon？”

这恰恰是 `DECISIONS.md` 最能帮你**省 Work 额度、防止 AI 反复重新设计项目**的地方。
