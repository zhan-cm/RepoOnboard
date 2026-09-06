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

> **Phase 0 — Technical Architecture Design**

Current Product Version:

```text
V0.1 — Not Started
```

Current Implementation Status:

```text
Product Definition       ✓
Architecture Constraints ✓
Technical Decisions      ○
Project Scaffold         ○
Core Analysis            ○
Web UI                   ○
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

> **完成 V0.1 Technical Architecture Proposal，并确认核心技术决策。**

正式编码开始前，至少需要确认：

```text
Java Runtime
Build Strategy
CLI Strategy
Maven Analysis
Java AST
Symbol Resolution Scope
Generic Project Model
Analyzer Boundaries
Dependency Model
Serialization
Local Web Architecture
Frontend Strategy
Visualization Strategy
Testing Strategy
Packaging Strategy
```

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
M6 — Unified Project Model
        ↓
M7 — Local Web UI
        ↓
M8 — Start Here
        ↓
M9 — Real Repository Validation
        ↓
M10 — Release Preparation
        ↓
V0.1 Release
```

---

# 10. M0 — Technical Architecture

Goal:

> 在正式编码之前确定 V0.1 必需的核心技术方案。

Status:

```text
◐ IN PROGRESS
```

---

## T-0001 — Complete Technical Architecture Proposal

Status:

```text
[ ]
```

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
[ ] 每项核心技术问题均有明确推荐方案
[ ] 至少比较关键候选方案
[ ] 明确最主要技术风险
[ ] 未扩大 PROJECT.md V0.1 Scope
[ ] 未开始正式业务代码实现
```

---

## T-0002 — Architecture Review

Status:

```text
[ ]
```

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
[ ] 所有关键技术选择经过 Review
[ ] 明确哪些方案接受
[ ] 明确哪些方案拒绝
[ ] 明确哪些问题延后
```

---

## T-0003 — Create Initial ADRs

Status:

```text
[ ]
```

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
[ ] 所有关键 ACCEPTED 技术方案已形成 ADR
[ ] PROPOSED 和 ACCEPTED 状态没有混淆
[ ] DECISIONS.md Decision Index 已更新
```

---

## T-0004 — Define V0.1 Milestone Boundaries

Status:

```text
[ ]
```

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

---

# 11. M1 — Project Foundation

Goal:

> 建立最小、可靠、可测试的 RepoOnboard 工程基础。

Status:

```text
○ NOT STARTED
```

---

## T-0101 — Create Repository Scaffold

Status:

```text
[ ]
```

Depends On:

```text
M0 Complete
```

Goal:

根据 ACCEPTED ADR 创建正式工程结构。

Acceptance Criteria:

```text
[ ] 项目可以正常构建
[ ] 基础目录结构清晰
[ ] 没有不必要的模块
[ ] README / PROJECT / DECISIONS / TODO 保留
[ ] 基础测试可以运行
```

---

## T-0102 — Configure Build

Status:

```text
[ ]
```

Goal:

完成 RepoOnboard 自身 Build 配置。

Acceptance Criteria:

```text
[ ] clean build 成功
[ ] test 可以执行
[ ] 构建环境可重复
[ ] 不依赖 IDE 才能构建
```

---

## T-0103 — Establish Testing Foundation

Status:

```text
[ ]
```

Goal:

建立最基本自动化测试结构。

Acceptance Criteria:

```text
[ ] 至少一个 unit test
[ ] 至少一个 fixture test
[ ] CI 前可以本地运行
```

---

## T-0104 — Create Minimal CLI Entry

Status:

```text
[ ]
```

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
[ ] 可以从 CLI 启动
[ ] "." 能解析为当前目录
[ ] 非法路径有明确错误信息
[ ] CLI 有基础 help
```

---

# 12. M2 — Maven Project Analysis

Goal:

> 可靠识别和解析 Maven Project。

Status:

```text
○ NOT STARTED
```

---

## T-0201 — Maven Project Detection

Status:

```text
[ ]
```

Goal:

判断目标目录是否为 Maven 项目。

Acceptance Criteria:

```text
[ ] 能识别 pom.xml
[ ] 非 Maven 项目有明确结果
[ ] 不因为目录中存在无关文件而失败
```

---

## T-0202 — Basic Maven Metadata Extraction

Status:

```text
[ ]
```

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
[ ] 普通单模块项目解析正确
[ ] 缺失字段能够合理处理
```

---

## T-0203 — Maven Property Resolution

Status:

```text
[ ]
```

Goal:

正确处理常见：

```xml
<!--${...}-->
```

属性。

Acceptance Criteria:

```text
[ ] Java version property 可解析
[ ] Spring Boot version property 可解析
[ ] 常见 parent property 可处理
```

---

## T-0204 — Multi-module Maven Analysis

Status:

```text
[ ]
```

Goal:

识别：

```text
Parent
Child Module
Module Hierarchy
```

Acceptance Criteria:

```text
[ ] 能识别多模块项目
[ ] Module tree 正确
[ ] 子模块 source root 可确定
```

---

## T-0205 — Dependency Extraction

Status:

```text
[ ]
```

Goal:

提取 Maven dependencies。

Acceptance Criteria:

```text
[ ] dependency groupId
[ ] artifactId
[ ] version（如可解析）
[ ] scope
```

---

## T-0206 — Spring Boot Detection

Status:

```text
[ ]
```

Goal:

判断项目是否使用 Spring Boot。

Acceptance Criteria:

```text
[ ] parent-based Spring Boot 项目可识别
[ ] dependency-based Spring Boot 项目尽量可识别
[ ] Spring Boot version 尽量正确解析
```

---

# 13. M3 — Java Source Analysis

Goal:

> 建立 RepoOnboard 的 Java Source 基础分析能力。

Status:

```text
○ NOT STARTED
```

---

## T-0301 — Source Root Discovery

Status:

```text
[ ]
```

Goal:

识别：

```text
src/main/java
```

以及多模块项目中的 Source Root。

Acceptance Criteria:

```text
[ ] 单模块项目正确
[ ] 多模块项目正确
[ ] 不扫描 target / generated output 等无关目录
```

---

## T-0302 — Java File Discovery

Status:

```text
[ ]
```

Goal:

发现目标项目中的 Java Source File。

Acceptance Criteria:

```text
[ ] 能枚举 Java files
[ ] Source path 可追踪
[ ] Module ownership 可识别
```

---

## T-0303 — Basic AST Parsing

Status:

```text
[ ]
```

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
[ ] Fixture Java 项目解析正确
[ ] Source Location 可追踪
[ ] 单个文件解析失败不会导致整个项目失败
```

---

## T-0304 — Import and Type Reference Extraction

Status:

```text
[ ]
```

Goal:

提取基础：

```text
Imports
Declared Types
Referenced Types
```

为后续 dependency analysis 准备。

---

## T-0305 — Java Entry Point Detection

Status:

```text
[ ]
```

Goal:

识别基础 Java / Spring Boot application entry point。

至少识别：

```java
//@SpringBootApplication
```

Acceptance Criteria:

```text
[ ] Application class 能识别
[ ] Source Location 正确
```

---

# 14. M4 — Spring Boot Analysis

Goal:

> 从 Java AST 中识别 Spring Boot 软件语义。

Status:

```text
○ NOT STARTED
```

---

## T-0401 — Spring Component Detection

Status:

```text
[ ]
```

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
[ ] Component Type 分类正确
[ ] Component Name 正确
[ ] Source Location 正确
```

---

## T-0402 — Configuration Class Detection

Status:

```text
[ ]
```

识别：

```java
//@Configuration
```

以及主要配置类。

---

## T-0403 — Dependency Injection Detection

Status:

```text
[ ]
```

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
[ ] 常见 Constructor Injection 正确
[ ] 常见 @Autowired Field 正确
```

---

## T-0404 — Mapper Detection

Status:

```text
[ ]
```

Priority:

```text
OPTIONAL FOR EARLY V0.1
```

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
○ NOT STARTED
```

---

## T-0501 — Spring MVC Controller Mapping

Status:

```text
[ ]
```

识别：

```text
@RequestMapping
@GetMapping
@PostMapping
@PutMapping
@DeleteMapping
@PatchMapping
```

---

## T-0502 — Full API Path Resolution

Status:

```text
[ ]
```

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
[ ] HTTP method 正确
[ ] Path 正确
[ ] Controller 正确
[ ] Handler method 正确
[ ] Source Location 正确
```

---

## T-0503 — Component Dependency Extraction

Status:

```text
[ ]
```

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
[ ] Fixture 项目关系正确
[ ] 无法确认的关系不应伪造
```

---

## T-0504 — Dependency Evidence

Status:

```text
[ ]
```

每条关键 dependency 尽量记录：

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

---

# 16. M6 — Unified Project Model

Goal:

> 将 Maven、Java、Spring、API、Dependency 分析结果统一转换成稳定项目模型。

Status:

```text
○ NOT STARTED
```

---

## T-0601 — Project Model

Status:

```text
[ ]
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
```

必须符合：

```text
Generic enough for future languages
Simple enough for V0.1
```

---

## T-0602 — Stable Identifiers

Status:

```text
[ ]
```

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

---

## T-0603 — Serialization

Status:

```text
[ ]
```

将统一 Project Model 导出为选定格式。

Acceptance Criteria:

```text
[ ] Human-readable
[ ] Debuggable
[ ] Frontend friendly
[ ] Source locations retained
```

---

## T-0604 — Analysis Summary

Status:

```text
[ ]
```

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

---

# 17. M7 — Local Web UI

Goal:

> 把静态分析结果真正转换成用户能快速理解的项目地图。

Status:

```text
○ NOT STARTED
```

---

## T-0701 — Local UI Bootstrap

Status:

```text
[ ]
```

完成：

```bash
repoonboard .
```

之后打开本地页面的完整路径。

Acceptance Criteria:

```text
[ ] CLI 可启动 UI
[ ] UI 可以读取当前分析结果
[ ] 默认不需要账号或外部服务器
```

---

## T-0702 — Overview Page

Status:

```text
[ ]
```

展示：

```text
Project
Language
Framework
Java Version
Spring Boot Version
Modules
Components
Endpoints
Entry Point
```

目标：

> 用户 30 秒内知道项目基本情况。

---

## T-0703 — Module View

Status:

```text
[ ]
```

展示：

```text
Module Hierarchy
Module Components
Module Statistics
```

---

## T-0704 — Architecture View

Status:

```text
[ ]
```

展示主要：

```text
Controller
Service
Repository
Mapper
Component
```

以及 Dependency。

重点：

> 可读性优先于节点数量。

---

## T-0705 — Architecture Filtering

Status:

```text
[ ]
```

至少支持：

```text
By Module
By Component Type
```

防止大项目直接生成“毛线球”。

---

## T-0706 — API Map

Status:

```text
[ ]
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

---

## T-0707 — Source Navigation Information

Status:

```text
[ ]
```

点击 Component / Endpoint 后展示：

```text
Source File
Line
Module
Related Components
```

V0.1 不要求直接集成 IDE。

---

# 18. M8 — Start Here

Goal:

> 让 RepoOnboard 从“项目结构查看器”升级成真正的 Onboarding Tool。

Status:

```text
○ NOT STARTED
```

---

## T-0801 — Reading Importance Heuristics

Status:

```text
[ ]
```

根据技术方案实现第一版可解释规则。

可能参考：

```text
Entry Point
Component Role
Endpoint Exposure
Dependency Centrality
Configuration Importance
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

---

## T-0804 — Start Here UI

Status:

```text
[ ]
```

在 Web UI 中形成完整：

> Start Here

页面。

---

# 19. M9 — Fixture & Real Repository Validation

Goal:

> 证明 RepoOnboard 不只是 Demo 项目能运行。

Status:

```text
○ NOT STARTED
```

---

## T-0901 — Minimal Spring Fixture

Status:

```text
[ ]
```

包含：

```text
Controller
Service
Repository
API
```

---

## T-0902 — Multi-module Fixture

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

---

## T-0903 — Complex Spring Fixture

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

---

# 20. M10 — Release Preparation

Goal:

> 将“能运行的开发项目”变成“陌生开发者愿意安装的开源项目”。

Status:

```text
○ NOT STARTED
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

---

## T-1004 — Demo Repository

Status:

```text
[ ]
```

准备一个可以稳定展示 RepoOnboard 效果的公开项目。

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

---

## T-1006 — License

Status:

```text
[ ]
```

确定开源 License。

需要在正式公开前完成。

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
Improve Java / Spring analysis

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
None — implementation has not started.
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
T-0001
Complete Technical Architecture Proposal
```

本阶段：

> 不开始正式业务代码开发。

完成：

```text
T-0001
↓
T-0002
↓
T-0003
↓
T-0004
```

后，才能进入：

```text
M1 — Project Foundation
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
░░░░░░░░░░░░░░░░░░░░   0%

Technical Decisions
░░░░░░░░░░░░░░░░░░░░   0%

Implementation
░░░░░░░░░░░░░░░░░░░░   0%

Validation
░░░░░░░░░░░░░░░░░░░░   0%

Release
░░░░░░░░░░░░░░░░░░░░   0%
```

Next:

> **Design the architecture before writing the product.**

````

这样你现在三个核心文件就完整了：

```text
RepoOnboard/
│
├── PROJECT.md
│   └── 我们做什么 / 为什么做
│
├── DECISIONS.md
│   └── 我们决定怎么做 / 为什么这么做
│
└── TODO.md
    └── 现在做到哪 / 下一步具体做什么
````

而且现在 `TODO.md` 已经明确告诉 Work：**下一步只有 `T-0001 — Complete Technical Architecture Proposal`，目前不应该开始写正式业务代码。**
