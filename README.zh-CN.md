# RepoOnboard

[English](./README.md) | **简体中文**

> 将陌生代码仓库转化为可交互的代码库地图。

RepoOnboard 是一个开源、本地优先的**代码库理解与开发者上手工具**，帮助开发者在修改陌生项目之前，先建立清晰、可靠的整体认知。

> **状态：早期开发 / 本地 Web UI 开发中**
>
> Maven、Java、Spring、API、组件依赖和稳定报告分析已经可用。CLI 会通过只绑定 loopback 的本地端点，把每次分析报告提供给随包发布的 Vue 应用。响应式产品 Shell 现在已包含基于报告事实的 Repository Overview、Module Explorer 与 Architecture Workspace；其余产品页面仍在开发中，首个版本尚未发布。

## 为什么需要 RepoOnboard？

理解一个陌生代码库，通常需要反复手动回答同一组问题：

- 这个项目是做什么的？
- 应用从哪里启动？
- 哪些模块和组件最重要？
- 项目暴露了哪些 HTTP API？
- Controller、Service 和 Repository 之间是什么关系？
- 应该从哪些文件开始阅读？

RepoOnboard 希望把这些探索过程整理成结构化、可追溯的项目地图。

## V0.1 范围

首个版本有意聚焦于一个技术生态：

| 领域 | V0.1 目标 |
| --- | --- |
| 语言 | Java |
| 构建系统 | Maven |
| 框架 | Spring Boot |
| 运行环境 | Java 21 或更高版本 |
| 界面 | 本地只读 Web UI |

多语言支持是长期方向，不是 V0.1 的功能要求。

## V0.1 计划能力

- 检测 Maven 项目及模块。
- 提取项目元数据和 Maven 依赖。
- 发现 Spring Boot 应用入口。
- 识别 Controller、Service、Repository、Component 和常见 Mapper 模式。
- 提取 Spring MVC HTTP 接口。
- 构建经过确认的组件依赖关系。
- 为重要分析结果保留源码位置和证据。
- 在本地展示概览、架构、API 地图和 Start Here 页面。
- 为代码库生成可解释的推荐阅读路径。

计划中的分析流程为：

```text
代码仓库
    ↓
Maven 分析
    ↓
Java 源码分析
    ↓
Spring Boot 分析
    ↓
统一项目模型
    ↓
本地交互式代码库地图
```

## 预期使用体验

当前命令行使用方式为：

```bash
cd unfamiliar-project
repoonboard .
```

分析完成后，RepoOnboard 会在系统分配的 `127.0.0.1` 端口启动只读本地 UI，并打开默认浏览器。使用 `--no-open` 可改为手动打开；在按 Ctrl+C 停止进程前，终端输出的地址会保持可用。

```bash
repoonboard . --no-open
```

该命令会检测根目录的 `pom.xml`，显示解析后的坐标，并在启动 UI 前输出模块层级、源码目录、发现的 Java 源码根/文件和依赖。每条依赖保留 groupId、artifactId、version、scope、原始/解析值与字段来源位置。支持可用 parent 或导入 BOM 提供的版本和 scope；缺失版本保留未知状态并产生诊断。仅包含声明、继承及激活 profile 的依赖，不下载构件或计算传递依赖图。未使用的 dependencyManagement 条目不会列为项目依赖。

模块聚合与 parent 继承分别记录，支持嵌套模块与激活 profile 中的模块。模块路径与 `build.sourceDirectory` 必须解析在扫描根内；源码目录可尚未存在。模块缺失、损坏、越界、重复或循环时生成诊断，并继续处理其他模块。默认 Java 源码目录为各模块下的 `src/main/java`；自定义目录保留声明值和解析值，供后续源码分析使用。

Maven 模型解析完全离线。相对 parent 必须位于扫描根内；外部 parent 和导入的 BOM 可从本地 Maven 仓库读取。缺少模型时保留可用事实并返回 `PARTIAL`。导入 BOM 的属性不会被项目继承。

```bash
repoonboard . --profile dev --local-repository /path/to/local/maven/repository
```

`--profile` 支持重复指定或逗号分隔的 ID。只采用显式选中与 `activeByDefault` profile，不使用宿主 OS/JDK/文件/属性隐式激活或 Maven settings。默认缓存为 `~/.m2/repository`。分析不执行 Maven 插件、扩展、生命周期，也不下载远程 POM。

所有 POM 来源均拒绝 DTD/外部实体及逃逸允许根目录的路径。默认限制为单份 POM 1 MiB、64 个不同 POM 来源、XML 嵌套深度 128。外部来源证据使用 `local-repository/` 标识，不暴露缓存绝对路径。CLI 退出码：`0` 成功、`1` 分析失败、`2` 参数错误、`3` 部分成功；诊断输出到标准错误。

RepoOnboard 将分析代码仓库，并生成类似以下的信息：

```text
项目：demo-shop
构建：Maven
Java：21
框架：Spring Boot 3.x

模块：4
Controller：18
Service：27
Repository：12
HTTP 接口：83
```

重要结果应当可以追溯到对应源码：

```text
POST /users
    ↓
UserController.createUser()
    ↓
src/main/java/.../UserController.java:73
```

## 计划界面

Spring Boot 构建识别使用模块声明的 `org.springframework.boot:spring-boot-starter-parent`、导入的 `spring-boot-dependencies` BOM，或已解析/继承的 Boot 核心与 starter 依赖。每条证据保留来源和版本位置。检测与版本解析分别表达；已知版本冲突时返回未知版本和诊断。构建证据不等于已发现应用入口。没有可识别依赖的间接 parent/BOM 链暂不分类。

### 项目概览

汇总技术栈、模块、组件、HTTP 接口和应用入口。

### 架构地图

以模块为范围，在交互式图中展示经过确认的 Spring 组件注入关系。节点和连线可打开基于报告事实的 Inspector，查看源码位置和原始 Evidence；未解析或有歧义的关系保持显式状态，不会画成确定事实。模块、组件类型、关系类型筛选可与所选节点的一阶邻域组合使用；可搜索组件列表始终提供替代入口，包括匹配范围超过图可读性预算时。

### API 地图

展示 HTTP 方法、路径、Controller、处理方法和源码证据。

### Start Here

根据构建文件、应用入口、配置、公开 API 和已确认依赖，提供可解释的推荐阅读顺序。

## 设计原则

- **静态分析优先**——优先从源码、AST、构建元数据和框架元数据中提取可靠事实。
- **准确优先于炫酷**——宁可提供少量可信信息，也不生成推测性的关系。
- **本地优先**——核心分析不应要求把用户源码上传到远程服务。
- **结果可解释**——重要结论应尽可能附带源码证据。
- **部分成功优于整体失败**——单个损坏文件不应导致其余有效分析结果全部丢失。
- **先做好一个生态**——先完成 Java、Maven 和 Spring Boot 支持，再考虑更多语言。

RepoOnboard V0.1 不依赖大语言模型。如果未来引入 AI，也应让 AI 使用经过验证的结构化分析结果，而不是替代静态分析。

## 开发路线

```text
M0  技术架构                         ✓
M1  工程基础                         ✓
M2  Maven 分析                       ✓
M3  Java 源码分析                    ✓
M4  Spring Boot 分析                 ✓
M5  API 与依赖分析                  ✓
M6  报告组装与序列化             ✓
M7  本地 Web UI                         ◐
M8  Start Here
M9  回归测试与真实仓库验证
M10 发布准备
```

详细任务和验收标准维护在 [TODO.md](./TODO.md) 中。

V0.1 继续采用 Web-first 交付：发布包为可执行 JAR 及 Windows/POSIX 启动脚本。Vue UI 保持宿主无关，以便未来复用；原生桌面容器、安装器和自带 Java Runtime 延后到 V0.2 技术试验，不阻塞首次发布。

## 开发构建

RepoOnboard 开发构建当前要求 JDK 21 或更高版本，以及 Node.js `^20.19.0` 或 `>=22.12.0`。无需单独安装 Maven，也不依赖 IDE：仓库中的 Maven Wrapper 会下载并校验固定版本的 Maven、安装锁定的前端依赖，并构建随包发布的 UI。最终用户使用发布产物时不需要 Node.js。

Windows：

```powershell
git clone https://github.com/zhan-cm/RepoOnboard.git
cd RepoOnboard
.\mvnw.cmd clean verify
```

macOS 或 Linux：

```bash
git clone https://github.com/zhan-cm/RepoOnboard.git
cd RepoOnboard
./mvnw clean verify
```

当前 CLI 已提供 Maven 与 Java 源码分析，以及带来源证据的 Spring 组件、配置类、应用入口、注入、HTTP Endpoint 和组件依赖 facts。类级/方法级路径、HTTP method 与 mapping conditions 均被保留，`ANY` 和未知路径会明确表达。只有唯一确认的项目内组件目标才生成确定边；重复 Evidence 会合并，歧义或缺失目标保留为诊断。MyBatis/MyBatis-Plus Mapper 专用分类仍按计划延后。稳定报告组装和 schema `1.2` JSON 序列化现已接入 loopback 服务、响应式产品 Shell、Repository Overview、Module Explorer 与 Architecture Workspace。模块页面使用显式 Maven 聚合关系、精确坐标唯一确认的内部模块依赖、模块级统计、元数据、源码根、版本事实、诊断与证据。架构页面使用 Cytoscape.js，仅在单一模块范围内绘制确认的组件注入边，并提供可组合筛选、搜索、所选节点一阶邻域、缩放、平移、适配、选择、源码证据、响应式可搜索列表回退，以及明确的部分/不可用/超预算状态。API 和源码导航等视图仍属于后续 M7 任务。

## 项目文档

- [PROJECT.md](./PROJECT.md)——产品愿景、范围和原则
- [DECISIONS.md](./DECISIONS.md)——已接受的架构与技术决策
- [TODO.md](./TODO.md)——开发路线和当前任务状态
- [AGENTS.md](./AGENTS.md)——编码代理的仓库执行说明
- [前端产品需求文档](./docs/product/FRONTEND-PRODUCT-REQUIREMENTS.md)——页面结构、交互、视觉方向、响应式规则与未来桌面体验

## 目标用户

RepoOnboard 面向加入现有项目的开发者、开源贡献者、学生、初级开发者，以及正在接手陌生或遗留代码库的维护者。

## 参与贡献

RepoOnboard 仍处于早期开发阶段。等实现适合外部参与后，项目会补充贡献指南。在此之前，所有变更都应遵守项目范围和已经接受的技术决策。

## 许可证

项目许可证尚未最终确定，将在首个公开版本发布前完成选择。

## 项目理念

> **先理解，再修改。**
>
> **静态分析优先于生成式 AI。**
>
> **准确优先于炫酷。**
>
> **先把一个技术生态做好，再扩展到更多生态。**
