RepoOnboard 自动分析当前项目，并打开一个本地 Web 页面。

用户应该能够快速回答：

这是一个什么项目？
使用了哪些主要技术？
项目由哪些 Maven 模块组成？
Spring Boot 应用入口在哪里？
有哪些 Controller、Service、Repository、Mapper？
项目暴露了哪些 HTTP API？
主要组件之间是什么关系？
哪些代码值得优先阅读？
如果我要理解某个功能，应该从哪里开始？
4. Target Users
   Primary Users
   刚接手陌生项目的开发者
   实习生
   新员工
   阅读 GitHub 开源项目的开发者
   学习 Spring Boot 项目的学生
   想参与开源项目但不知道如何开始阅读源码的人
   Possible Future Users

未来可以考虑：

AI Coding Agent
Code Review 工具
软件架构分析工具
IDE 插件

这些不属于 V0.1 范围。

5. V0.1 Scope

V0.1 只支持：

Java + Maven + Spring Boot

第一阶段的目标不是支持所有语言和框架，而是把 Spring Boot 项目分析做好。

6. Core Features
   6.1 Project Overview

识别：

项目名称
Maven groupId
Maven artifactId
Java 版本
Spring Boot 版本
Maven modules
主要 dependencies
Spring Boot Application 启动入口

示例：

6.2 Spring Component Discovery

识别主要 Spring 组件：

@Controller
@RestController
@Service
@Repository
@Component

如项目使用常见 Mapper 模式，可以支持：

MyBatis Mapper
MyBatis-Plus Mapper

但 Mapper 支持不能影响核心 Spring 项目分析能力。

6.3 API Map

识别 Controller 中的 HTTP API。

至少支持：

@RequestMapping
@GetMapping
@PostMapping
@PutMapping
@DeleteMapping
@PatchMapping

每个 API 至少记录：

HTTP Method
完整 Path
Controller
Java Method
Source Location

例如：

需要正确处理：

例如：

加：

最终得到：

6.4 Dependency Map

建立主要 Spring 组件之间的静态依赖关系。

例如：

V0.1 重点分析：

Controller → Service
Service → Service
Service → Repository
Service → Mapper
Component → Component

V0.1 不要求实现完整的方法级 Call Graph。

6.5 Architecture View

把主要组件关系转换成可视化结构。

例如：

重点是帮助用户理解：

项目主要模块之间是怎么连接的。

6.6 Start Here

自动生成推荐源码阅读顺序。

例如：

每个推荐项尽量给出理由：

第一版允许采用可解释的启发式规则。

不能只输出一个没有说明原因的排序结果。

7. Local Web UI

RepoOnboard 的分析结果通过本地 Web 页面展示。

V0.1 暂定四个核心页面。

Overview

回答：

这是什么项目？

展示：

技术栈
Maven modules
Spring Boot 版本
Java 版本
Component 数量
API 数量
Application 入口
Architecture

回答：

主要代码是怎么组织和关联的？

展示：

Controller
Service
Repository
Mapper
Component
Dependency Graph
API Map

回答：

项目提供了哪些 API？

展示：

点击 API 可以看到：

Controller
Method
Source file
Source line
Start Here

回答：

如果第一次看这个项目，我应该从哪里开始？

展示推荐阅读路径和原因。

8. Expected User Experience

理想使用方式：

输出：

然后自动打开：

用户第一次使用 RepoOnboard 时，不应该被要求：

注册账号
登录
配置数据库
配置 Redis
配置云服务
配置 API Key
安装复杂基础设施
9. Product Principles
   9.1 Useful Before Intelligent

优先保证：

分析结果真实
稳定
可验证
对用户有实际帮助

不要为了“看起来聪明”而生成无法从代码中验证的结论。

9.2 Static Analysis First

V0.1 优先通过：

Maven metadata
Java source code
AST
Spring annotations
Source structure

获得分析结果。

9.3 Local First

RepoOnboard 默认本地运行。

项目源码不应默认上传到任何服务器。

9.4 Explainable Results

重要分析结果应尽可能提供来源。

例如：

用户应该能够追溯：

RepoOnboard 为什么得出这个结论。

9.5 Fast First Experience

理想体验：

就能开始分析。

尽量减少配置。

9.6 Avoid Overengineering

V0.1 不为未来可能存在的需求设计复杂系统。

优先：

简单
清晰
可测试
可维护
可以尽快在真实项目上验证
10. Explicit Non-Goals for V0.1

V0.1 明确不做：

通用 AI 聊天
RAG
向量数据库
AI Agent
MCP Server
自动修改代码
自动重构
Code Review
Bug 自动修复
技术债评分
完整方法级 Call Graph
Git 历史分析
Contributor 分析
Star 分析
用户账号
云端同步
SaaS
团队协作
VS Code 插件
JetBrains 插件
Python 支持
JavaScript 支持
Go 支持
Rust 支持
C/C++ 支持

除非 PROJECT.md 被明确修改，否则不要擅自加入这些功能。

11. AI Policy

V0.1 不依赖大语言模型。

未来可以考虑使用 AI：

解释已经分析出的项目结构
根据真实代码关系回答问题
生成自然语言 onboarding guide

但 AI 必须建立在可靠的结构化分析结果之上。

RepoOnboard 不应变成：

把整个仓库交给 LLM，然后让 LLM 自己猜项目结构。

12. Quality Requirements

核心分析逻辑必须：

可测试
尽量确定性
不依赖网络即可完成基础分析
对常见 Spring Boot 项目结构具有合理兼容性
分析失败时提供明确错误信息
单个模块分析失败时尽量不要导致整个扫描失败

核心分析模块应有自动化测试。

13. V0.1 Success Criteria

给定一个真实 Spring Boot Maven 项目：

RepoOnboard 至少能够正确生成：

Project Overview
Maven Module Map
Spring Component List
HTTP API Map
主要组件 Dependency Graph
Start Here 阅读建议
本地可视化页面

最核心的验收标准不是功能数量。

而是：

一个第一次接触该项目的开发者，使用 RepoOnboard 后，能够比完全手动翻源码更快建立对项目结构的基本理解。

14. Development Strategy

RepoOnboard 的开发顺序遵循：

每个开发阶段都应遵循：

不要一次性开发完整产品。

15. Project Management Files

RepoOnboard 长期维护以下三个核心文档。

PROJECT.md

保存稳定的：

产品目标
产品边界
V0.1 Scope
产品原则
非目标

除非产品方向发生变化，否则不要频繁修改。