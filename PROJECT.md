# RepoOnboard

> Turn unfamiliar repositories into interactive codebase maps.

---

## 1. Project Overview

**RepoOnboard** 是一个面向多语言代码仓库的 **Codebase Comprehension（代码库理解）与 Developer Onboarding（开发者上手）平台**。

它通过静态分析项目源码、构建配置、框架元数据以及代码之间的结构关系，将一个陌生代码仓库转换为可交互的项目理解地图，帮助开发者快速回答：

- 这是一个什么项目？
- 项目使用了哪些技术？
- 项目由哪些模块组成？
- 应用入口在哪里？
- 核心组件有哪些？
- HTTP API 在哪里？
- 主要组件之间如何依赖？
- 一个功能通常从哪里进入系统？
- 第一次阅读源码应该从哪里开始？

RepoOnboard 的长期目标不是成为某一种语言或框架的专用分析器，而是：

> **建立一个可扩展到多语言、多框架的软件项目理解平台。**

---

## 2. Core Problem

开发者第一次接触陌生代码仓库时，通常需要进行大量人工探索。

典型流程包括：

- 阅读 README
- 查看目录结构
- 阅读构建配置
- 判断项目技术栈
- 找应用启动入口
- 找核心模块
- 查找 Controller / Router
- 查找 Service / Provider
- 查找 Repository / Mapper
- 查找 HTTP API
- 理解模块之间的依赖
- 顺着代码调用关系不断跳转
- 判断哪些文件更重要
- 决定源码阅读顺序
- 向熟悉项目的开发者询问系统结构

对于中大型项目，这个过程可能需要数小时甚至数天。

RepoOnboard 希望自动完成其中大量机械性的“项目侦察工作”，缩短：

> **Developer Time-to-Understanding**

即开发者从“完全不了解项目”到“形成基本系统认知”所需要的时间。

---

## 3. Product Positioning

RepoOnboard 的核心定位是：

> **Codebase Understanding Infrastructure for Developers**

它不是：

- 通用 AI Coding Agent
- AI 代码生成器
- IDE 替代品
- 普通代码搜索工具
- 单纯的代码可视化工具
- 单纯的代码聊天机器人
- 静态代码质量评分工具

RepoOnboard 首先关注：

> **理解一个项目。**

而不是：

> **修改一个项目。**

---

## 4. Long-Term Vision

RepoOnboard 的长期目标是支持多种主流编程语言和应用框架。

可能包括但不限于：

### Java

- Spring Boot
- Quarkus
- Micronaut
- Maven
- Gradle

### JavaScript / TypeScript

- Node.js
- Express
- NestJS
- Next.js

### Python

- FastAPI
- Django
- Flask

### Go

- net/http
- Gin
- Fiber
- Echo

### Rust

- Axum
- Actix Web
- Rocket

未来还可以根据需求考虑：

- C#
- .NET
- PHP
- Laravel
- Ruby
- Rails
- C / C++

---

## 5. Initial Implementation Strategy

虽然长期目标是多语言，但 **RepoOnboard V0.1 不追求多语言支持**。

首个版本只实现：

> **Java + Maven + Spring Boot**

原因：

1. 控制初始实现复杂度。
2. 优先把一个生态分析准确。
3. 验证 RepoOnboard 的核心产品价值。
4. 验证统一项目模型是否合理。
5. 避免每一种语言都只支持一小部分，导致分析结果没有实际价值。

因此：

> **Multi-language 是产品愿景，而不是 V0.1 的功能要求。**

---

## 6. Architecture Extensibility Principle

V0.1 虽然只支持 Spring Boot，但核心设计应避免与 Spring Boot 强耦合。

例如 RepoOnboard Core 更应该理解：

- Project
- Module
- Component
- Endpoint
- EntryPoint
- Dependency
- SourceLocation

而不是将所有核心模型直接设计成：

- SpringController
- SpringService
- SpringRepository
- SpringMapper

Spring Boot Analyzer 应负责将：

```java
@RestController
public class UserController {
}
```

转换成通用的软件语义，例如：

```text
Component

name:
UserController

kind:
CONTROLLER

language:
JAVA

framework:
SPRING_BOOT
```

未来其他语言同样可以转换成类似模型：

```text
Component

name:
UserRouter

kind:
ROUTER

language:
TYPESCRIPT

framework:
EXPRESS
```

但是必须遵循：

> Extensible, not overengineered.

V0.1 不应该为了未来多语言能力提前建立复杂插件系统、DSL、动态模块加载系统或过度抽象架构。

## 7. Product Goal

用户进入一个受支持的项目目录后：

`repoonboard .`

RepoOnboard 应自动：

```text
Detect Project
      ↓
Analyze Build System
      ↓
Analyze Source Code
      ↓
Detect Framework
      ↓
Extract Components
      ↓
Extract APIs
      ↓
Analyze Dependencies
      ↓
Build Project Model
      ↓
Generate Onboarding Map
      ↓
Open Local Web UI
```

最终帮助用户快速建立：

Mental Model of the Codebase

即对整个项目形成一张基本的“脑内地图”。

## 8. Target Users

### 8.1 New Developers

刚加入一个现有项目的开发者。

典型问题：

项目从哪里开始看？

### 8.2 Interns and Junior Developers

刚进入团队的实习生或初级开发者。

典型问题：

Controller、Service、数据库到底是怎么串起来的？

### 8.3 Open Source Contributors

第一次准备参与某个 GitHub 开源项目的人。

典型问题：

我想提交第一个 PR，但是整个仓库太大，不知道从哪里开始。

### 8.4 Students

正在学习真实项目、软件架构或框架源码的学生。

典型问题：

一个真实 Spring Boot 项目到底是怎么组织的？

### 8.5 Developers Taking Over Legacy Systems

接手旧项目或历史项目的开发者。

典型问题：

为什么这个项目现在会长成这样？

### 8.6 Future AI Tooling

未来 RepoOnboard 生成的结构化项目模型也可以作为：

```text
AI Coding Agent
Code Review Agent
Architecture Analysis Agent
Refactoring Agent
```

的可靠上下文来源。

但这不属于 V0.1。

## 9. V0.1 Scope

V0.1 支持：

```text
Language:
Java

Build System:
Maven

Framework:
Spring Boot
```

V0.1 的目标不是功能数量，而是：

第一次真正让一个陌生 Spring Boot 项目更容易理解。

## 10. V0.1 Core Capabilities

### 10.1 Project Detection

RepoOnboard 应能够识别：

- 是否为 Maven 项目
- 是否为 Spring Boot 项目
- 单模块 / 多模块项目
- 项目根目录
- Source directories

### 10.2 Project Overview

提取基础项目元数据：

- Project Name
- groupId
- artifactId
- Java Version
- Spring Boot Version
- Maven Modules
- Major Dependencies
- Application Entry Point

示例：

```text
Project

demo-shop

Java:
21

Spring Boot:
3.5

Build:
Maven

Modules:
4

Controllers:
18

Services:
27

Repositories:
12

HTTP APIs:
83
```

## 11. Maven Module Analysis

支持：

- 单模块 Maven 项目
- 多模块 Maven 项目
- parent / child module 关系
- dependency 信息
- Java version
- Spring Boot version
- Module hierarchy

例如：

```text
demo-shop

├── shop-common
├── shop-user
├── shop-order
└── shop-payment
```

## 12. Spring Component Discovery

V0.1 至少识别：

- `@Controller`
- `@RestController`
- `@Service`
- `@Repository`
- `@Component`

可以根据实现成本逐步支持：

- MyBatis Mapper
- MyBatis-Plus Mapper

但 Mapper 支持不能阻塞核心 Spring Boot 分析能力。

## 13. API Map

RepoOnboard 应识别 Spring MVC HTTP Endpoint。

至少支持：

- `@RequestMapping`
- `@GetMapping`
- `@PostMapping`
- `@PutMapping`
- `@DeleteMapping`
- `@PatchMapping`

每个 Endpoint 至少包含：

- HTTP Method
- Path
- Controller
- Java Method
- Source File
- Source Location

例如：

```text
POST /api/users

UserController.createUser()

src/main/java/.../UserController.java:73
```

必须正确处理：

```text
Class-level RequestMapping
+
Method-level Mapping
```

例如：

```java
@RequestMapping("/users")
public class UserController {

    @GetMapping("/{id}")
    public User findById(...) {
    }
}
```

最终应识别为：

`GET /users/{id}`

## 14. Dependency Map

RepoOnboard 应建立主要组件之间的静态依赖关系。

例如：

```text
UserController
      ↓
UserService
      ↓
UserRepository
```

V0.1 重点关注：

- `Controller → Service`
- `Service → Service`
- `Service → Repository`
- `Service → Mapper`
- `Component → Component`

主要来源可以包括：

- Constructor Injection
- Field Injection
- Type references
- Framework metadata

具体实现策略由技术设计阶段确定。

## 15. Architecture Map

RepoOnboard 应将主要组件关系转换成可交互的架构视图。

例如：

```text
                UserController
                      │
                      ▼
                 UserService
                 /         \
                ▼           ▼
       UserRepository     RoleService
             │                │
             ▼                ▼
          Database       RoleRepository
```

该视图的目标不是展示所有类。

而是帮助开发者理解：

系统最主要的软件结构。

## 16. Entry Point Detection

RepoOnboard 应识别重要应用入口。

V0.1 至少包括：

- Spring Boot Application Class
- HTTP Controller
- Major Configuration Classes

例如：

```java
@SpringBootApplication
public class DemoApplication {
}
```

应识别为：

`APPLICATION_ENTRY_POINT`

## 17. Start Here

RepoOnboard 应根据项目结构生成一个：

Recommended Reading Path

即推荐源码阅读顺序。

示例：

```text
Start Here

1. pom.xml
2. DemoApplication.java
3. SecurityConfig.java
4. LoginController.java
5. LoginService.java
6. UserService.java
7. UserRepository.java
```

每个推荐项应尽量附带解释。

例如：

```text
LoginController.java

Why read this?

- Main authentication entry point
- Exposes POST /login
- Depends on LoginService
```

V0.1 可以使用：

Explainable Heuristics

即简单但可以解释的启发式算法。

不要求使用 AI。

## 18. Source Traceability

RepoOnboard 的重要结论应尽可能能够回到真实源码。

例如：

```text
POST /login
      ↓
LoginController.login()
      ↓
src/main/java/.../LoginController.java:42
```

用户应该能够知道：

RepoOnboard 为什么得出这个结论。

这是 RepoOnboard 与纯自然语言代码总结工具的重要区别。

## 19. Local Web UI

分析结果通过本地 Web UI 展示。

V0.1 采用 **Web-first, Desktop-ready** 的实现边界：

- 主要产品页面作为与宿主无关的 Web 前端实现，具体技术栈由 `DECISIONS.md` 约束。
- 前端页面不直接访问本地文件系统，不依赖浏览器地址栏完成主要工作流。
- 需要宿主能力时，在首个真实调用方出现时建立最小、可替换的前端适配边界，不提前建立未使用的 Desktop API。
- Repository Analysis 和受控文件访问仍属于 Java Application / Core 的责任。

V0.1 的产品交付仍是本地 Web UI、可执行 JAR 和启动脚本。Desktop Application、原生安装器与自带 Java Runtime 属于 V0.2 候选范围，应在 V0.1 产品价值经真实仓库和首次发布验证后再通过技术试验决定桌面容器。

V0.1 至少包含：

### Overview

回答：

这是什么项目？

展示：

- Tech Stack
- Maven Modules
- Java Version
- Spring Boot Version
- Component Statistics
- API Statistics
- Application Entry Point

### Architecture

回答：

项目主要代码是如何组织的？

展示：

- Modules
- Controllers
- Services
- Repositories
- Mappers
- Components
- Major Dependencies

### API Map

回答：

项目暴露了哪些 HTTP API？

例如：

```text
GET      /users
POST     /users
GET      /users/{id}
PUT      /users/{id}
DELETE   /users/{id}
```

点击 Endpoint 后可以查看：

- Controller
- Method
- Source File
- Source Location

### Start Here

回答：

第一次阅读这个项目，应该从哪里开始？

展示：

- Recommended Reading Path
- 推荐原因
- 关键入口文件

## 20. Expected User Experience

理想使用流程：

`cd some-project`

`repoonboard .`

终端：

```text
RepoOnboard

Detecting project...

✓ Maven project detected
✓ Spring Boot detected
✓ Java 21 detected

Analyzing...

✓ 4 modules
✓ 18 controllers
✓ 27 services
✓ 12 repositories
✓ 83 API endpoints

Building dependency graph...

✓ Project model generated

Opening RepoOnboard...
```

浏览器自动打开：

`http://localhost:<port>`

用户第一次使用不应该要求：

- 注册
- 登录
- API Key
- MySQL
- Redis
- Docker
- 云服务器
- SaaS 账号
- 外部数据库
- 向量数据库

## 21. Product Principles

### 21.1 Useful Before Intelligent

RepoOnboard 首先必须有用。

优先：

- Correct
- Reliable
- Explainable
- Useful

而不是：

- Looks Intelligent

### 21.2 Static Analysis First

核心信息优先从：

- Source Code
- AST
- Build Metadata
- Framework Metadata
- Project Structure

获得。

### 21.3 Deterministic Before Generative

如果一个结论可以通过确定性程序分析获得，则优先使用程序分析。

例如：

`@Controller`

不应该让 LLM 猜它是不是 Controller。

### 21.4 Local First

RepoOnboard 默认本地运行。

用户源码不应默认上传到任何服务器。

### 21.5 Explainable

重要结果尽量能够回答：

Why?

例如：

```text
Why is UserService important?

- Used by 4 Controllers
- Depends on 3 Repositories
- Central component in user module
```

### 21.6 Fast First Experience

理想情况下：

`repoonboard .`

即可开始使用。

尽量减少安装和配置成本。

### 21.7 Progressive Depth

RepoOnboard 不应该第一屏展示所有代码细节。

信息展示应遵循：

```text
Overview
   ↓
Architecture
   ↓
Module
   ↓
Component
   ↓
Source
```

即：

从整体逐渐深入细节。

### 21.8 Avoid Overengineering

不要为了未来可能存在的需求提前构建：

- 复杂插件系统
- 微服务架构
- 分布式系统
- DSL
- 云平台
- 多租户系统
- 复杂账号系统

优先：

- 简单
- 清晰
- 可测试
- 可维护
- 快速验证

## 22. Multi-Language Design Principle

RepoOnboard 长期支持多语言，但不能强迫所有语言使用完全相同的软件架构概念。

例如：

```text
Spring Boot：

Controller
Service
Repository

FastAPI：

Router
Service
Repository

NestJS：

Controller
Provider
Repository

Go：

Handler
Service
Repository
```

RepoOnboard 应允许不同语言和框架保留自己的语义，同时映射到统一项目模型。

目标是：

> Unified where useful, framework-specific where necessary.

## 23. Core Conceptual Model

长期核心模型至少应考虑以下概念：

- `Project`
- `Module`
- `SourceFile`
- `Component`
- `Endpoint`
- `EntryPoint`
- `Dependency`
- `SourceLocation`

未来可能扩展：

- `Function`
- `Method`
- `Class`
- `Interface`
- `DatabaseEntity`
- `Event`
- `Queue`
- `ExternalService`

但 V0.1 不需要一次性实现所有类型。

## 24. AI Policy

### V0.1

不依赖大语言模型。

### Future

未来 AI 可以用于：

- Explain Architecture
- Explain Module
- Explain Business Flow
- Generate Onboarding Guide
- Answer Codebase Questions
- Summarize complex structures

但是 AI 必须建立在 RepoOnboard 已经提取出的：

```text
Project Model
+
Dependency Graph
+
Source Evidence
```

之上。

AI 不应该成为项目结构分析的唯一来源。

RepoOnboard 不应退化成：

“把整个仓库扔给 LLM，让它总结。”

## 25. Explicit Non-Goals for V0.1

V0.1 明确不做：

- 多语言同时支持
- Python Analyzer
- TypeScript Analyzer
- Go Analyzer
- Rust Analyzer
- 通用 AI Chat
- RAG
- Vector Database
- AI Agent
- MCP Server
- 自动写代码
- 自动修改代码
- 自动重构
- Code Review
- Bug 自动修复
- 完整 Code Quality Platform
- 技术债评分
- 完整 Method-level Call Graph
- 动态 Runtime Tracing
- Git 历史分析
- Contributor Analysis
- Star Analysis
- 用户系统
- 团队协作
- Cloud Sync
- SaaS
- VS Code Extension
- JetBrains Plugin
- Desktop Application
- Native Installer
- Bundled Java Runtime

除非 PROJECT.md 被明确修改，否则 Work / Codex 不应擅自将这些功能加入 V0.1。

## 26. Quality Requirements

核心分析能力必须尽量满足：

- Deterministic
- Testable
- Reproducible
- Explainable
- Offline-capable
- Failure-tolerant

具体要求：

- 不依赖网络即可完成核心源码分析。
- 核心 Analyzer 应具有自动化测试。
- 单个文件解析失败不应导致整个项目分析直接崩溃。
- 分析失败应提供明确错误信息。
- 用户能够追踪关键结果对应的源码位置。
- 应持续使用真实开源项目进行验证。
- 不应为了“看起来完整”输出大量低可信度关系。

## 27. Performance Expectations

RepoOnboard 的首要目标不是极致性能。

但是应避免明显不可接受的扫描体验。

V0.1 应：

- 支持普通中小型 Spring Boot 项目
- 避免不必要的全量重复解析
- 能够提供扫描进度
- 分析失败时尽量继续处理其他文件

未来可以根据真实数据再考虑：

- Incremental Analysis
- Cache
- Parallel Parsing

V0.1 不提前实现复杂增量分析系统。

## 28. V0.1 Success Criteria

RepoOnboard V0.1 可以认为基本成立，需要至少满足：

给定一个真实 Spring Boot Maven 项目：

`repoonboard .`

能够生成：

- Project Overview
- Maven Module Map
- Spring Component Map
- HTTP API Map
- Major Dependency Graph
- Application Entry Points
- Start Here Reading Guide
- Local Interactive Web UI

最重要的验收标准不是：

支持了多少功能。

而是：

一个第一次接触该项目的开发者，使用 RepoOnboard 后，能否明显比纯手动翻源码更快理解项目。

## 29. Product Validation

RepoOnboard 必须持续通过真实项目验证价值。

不能只针对人为构造的 Demo Project 工作。

验证过程应至少包括：

```text
Fixture Projects
↓
Small Real Project
↓
Medium Real Project
↓
Large / Complex Project
```

需要关注：

- 分析准确率
- 信息噪音
- Architecture Map 是否真的可读
- API Map 是否完整
- Start Here 是否真正帮助阅读
- 用户是否更快理解项目

## 30. Development Strategy

RepoOnboard 的开发顺序：

```text
Analyze Correctly
↓
Model Clearly
↓
Present Clearly
↓
Validate on Real Repositories
↓
Release Web-first V0.1
↓
Evaluate Desktop Application
↓
Increase Analysis Depth
↓
Add More Languages
↓
Consider AI
```

而不是：

```text
Add AI
↓
Add More Features
↓
Add More Languages
↓
最后再考虑准确性
```

## 31. Development Workflow

每个功能遵循：

```text
Design
↓
Implement
↓
Unit Test
↓
Fixture Validation
↓
Real Repository Validation
↓
Fix
↓
Document
```

不要一次性实现整个 V0.1。

每个开发任务应：

- 范围明确
- 可以测试
- 可以独立验收
- 尽量避免修改无关代码

## 32. Project Management Files

RepoOnboard 长期维护三个核心项目文件。

### PROJECT.md

记录稳定的：

- Project Vision
- Product Positioning
- Product Scope
- Product Principles
- V0.1 Boundaries
- Long-Term Direction

PROJECT.md 不记录具体短期实现任务。

除非产品方向发生变化，否则不要频繁修改。

### DECISIONS.md

记录已经确认的重要技术决策。

每项至少记录：

- Date
- Decision
- Reason
- Alternatives Considered
- Consequences

例如未来可能记录：

- Java Version
- CLI Framework
- Java AST Parser
- Maven Parsing Strategy
- Graph Representation
- Web UI Technology
- Visualization Library

已经确定的技术问题不应在后续任务中反复重新设计。

### TODO.md

记录：

- Current Milestone
- Current Task
- Pending Tasks
- Completed Tasks
- Known Issues
- Deferred Work

每次 Work / Codex 开发时，只处理明确指定的 TODO。

如果发现其他问题：

记录到 TODO，而不是顺手全部修复。

## 33. Work / Codex Collaboration Rules

在处理 RepoOnboard 时：

- 首先阅读 PROJECT.md。
- 如果存在 DECISIONS.md，则遵守已经确认的技术决策。
- 如果存在 TODO.md，则只处理当前明确指定的任务。
- 不擅自扩大 Scope。
- 不提前实现未来功能。
- 不因为未来多语言目标而过度设计 V0.1。
- 不随意替换已经确认的技术栈。
- 不进行与当前任务无关的大规模重构。
- 新发现的问题优先记录，而不是自动展开处理。
- 重要分析结果必须尽量来自可验证的源码信息。

## 34. Current Stage

RepoOnboard 当前处于：

> **M7 — Local Web UI**

M0 至 M6 及 **T-0701 — Local UI Bootstrap & Minimal App Shell** 已完成。CLI 现已把统一报告交给仅绑定 loopback 的本地服务，并可启动宿主无关的 Vue 最小外壳。当前下一任务是 **T-0702 — Visual System & Product Shell**；该任务只正式落地共用视觉系统和产品 Shell，不提前实现 Overview 或其他业务页面。

## 35. Project Direction Summary

RepoOnboard 的长期方向：

```text
Any Repository
↓
Language / Framework Analyzer
↓
Unified Project Model
↓
Dependency / Architecture Graph
↓
Codebase Understanding Engine
↓
Interactive Onboarding Map
```

V0.1：

```text
Spring Boot Repository
↓
Java + Maven + Spring Analyzer
↓
Project Model
↓
Architecture / API / Dependency Analysis
↓
Interactive Onboarding Map
```

核心原则始终保持：

> Understand first. Modify later.

> Static analysis before generative AI.

> Accurate before impressive.

> One ecosystem done well before many ecosystems done poorly.

这版我更建议作为真正长期使用的 `PROJECT.md`：**产品层面明确是多语言，工程层面 V0.1 明确只打 Java/S
