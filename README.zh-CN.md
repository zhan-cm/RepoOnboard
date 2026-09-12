# RepoOnboard

[English](./README.md) | **简体中文**

> 将陌生代码仓库转化为可交互的代码库地图。

RepoOnboard 是一个开源、本地优先的**代码库理解与开发者上手工具**，帮助开发者在修改陌生项目之前，先建立清晰、可靠的整体认知。

> **状态：早期开发 / 发布准备中**
>
> Maven、Java、Spring、API、组件依赖、稳定报告和可解释阅读路径分析已经可用。CLI 会通过只绑定 loopback 的本地端点，把每次分析报告提供给随包发布的 Vue 应用。fixture、真实仓库、规模边界与首次接触验证结果均已记录；后续仍需完成发布准备，首个版本尚未发布。首次接触验证只观察到部分价值，尚未证明相对手工阅读有可量化的提速。

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

## 安装与运行

RepoOnboard V0.1 尚未正式发布。当前发布候选请从本仓库源码构建可执行 JAR。实际运行要求 Java 21 或更高版本；从源码构建还需要 Node.js `^20.19.0` 或 `>=22.12.0`。Maven 无需另行安装，仓库已提供 Wrapper。

Windows：

```powershell
git clone https://github.com/zhan-cm/RepoOnboard.git
cd RepoOnboard
.\mvnw.cmd clean verify
.\repoonboard.cmd C:\path\to\spring-project
```

macOS 或 Linux：

```bash
git clone https://github.com/zhan-cm/RepoOnboard.git
cd RepoOnboard
./mvnw clean verify
chmod +x repoonboard
./repoonboard /path/to/spring-project
```

构建会生成 `target/repoonboard.jar` 与 `target/repoonboard.jar.sha256`。可执行 JAR 已包含 Java 运行依赖和完整离线 Web UI。发布目录中，启动脚本使用同目录的 `repoonboard.jar`；源码仓库中则自动回退到 `target/repoonboard.jar`。只有需要显式指定其他 JAR 时才设置 `REPOONBOARD_JAR`。

运行下载的产物前应核对校验和。Windows PowerShell：

```powershell
$expectedHash = (Get-Content .\target\repoonboard.jar.sha256).Split()[0]
$actualHash = (Get-FileHash .\target\repoonboard.jar -Algorithm SHA256).Hash.ToLowerInvariant()
if ($actualHash -ne $expectedHash) { throw "RepoOnboard checksum mismatch" }
```

Linux：

```bash
(cd target && sha256sum -c repoonboard.jar.sha256)
```

macOS：

```bash
(cd target && shasum -a 256 -c repoonboard.jar.sha256)
```

Windows 使用 `repoonboard.cmd --help`，POSIX 系统使用 `./repoonboard --help` 查看全部选项。未来 GitHub Release 会直接提供 JAR、校验和与两种启动脚本；使用这些发布文件不需要 Node.js 或 Maven。

## 预期使用体验

当前命令行使用方式为：

```bash
cd unfamiliar-project
repoonboard .
```

分析完成后，RepoOnboard 会在系统分配的 `127.0.0.1` 端口启动只读本地 UI，并打开默认浏览器。使用 `--no-open` 可改为手动打开；在按 Ctrl+C 停止进程前，终端输出的地址会保持可用。

服务只接受精确 loopback Host 与同源浏览器请求，仅通过固定 `GET` / `HEAD` 路由提供随包 UI、当前报告和 Start Here projection；非规范路径、外部 Origin、写请求和任意文件访问都会被拒绝。全部响应带有限制性 Content Security Policy、`no-store` 及相关浏览器安全头。生产构建还会直接验证 JavaScript / CSS 已进入 JAR、不依赖运行时 CDN，并与 Java 应用声明相同的报告 schema。

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

展示报告中的 HTTP 方法、路径、Controller、处理方法、源码位置和 mapping conditions。模块、method 与文本筛选可以组合，且不会把 `ANY` 当成通配符；未解析 path 或 conditions 会保持显式。选中 Endpoint 后可查看 handler source，以及方法级与 Controller 类级 mapping Evidence。

### 来源与证据详情

从选中的 Architecture 组件或 API Endpoint 进入，不作为独立一级页面。它展示扫描根相对源码路径、真实 1-based 位置、可选 symbol、所属模块、SourceFile 元数据、原始 Evidence，以及仅通过精确身份连接得到的相关事实。浏览器 Clipboard 可用时可以复制 path、symbol 和格式化位置；不可用或失败时仍保留可选文本并明确反馈。RepoOnboard 不读取源码正文、不构造绝对路径、不推断 handler 调用链，也不展示没有真实宿主能力的 IDE 动作。

### Start Here

根据构建文件、应用入口、配置、公开 API 和已确认依赖，提供可解释且确定性的推荐阅读顺序。每个推荐项展示所属模块、全部原因和 Source Evidence；只有精确连接的组件与 Endpoint 才能进入现有 Architecture 或 API Inspector，PARTIAL 覆盖与空结果保持明确。排序规则只在 Java 中执行，通过独立、版本化、只读的 projection 交给前端，不修改公共报告 schema。

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
M7  本地 Web UI                         ✓
M8  Start Here                         ✓
M9  回归测试与真实仓库验证           ✓
M10 发布准备                         ◐
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
.\repoonboard.cmd --help
```

macOS 或 Linux：

```bash
git clone https://github.com/zhan-cm/RepoOnboard.git
cd RepoOnboard
./mvnw clean verify
./repoonboard --help
```

`verify` 还会用空 Maven 缓存从打包后的可执行 JAR 启动固定 Spring fixture，确认本地报告路由可用，并写出 SHA-256 校验和；它不会构建或执行被分析的 fixture 应用。

当前 CLI 已提供 Maven 与 Java 源码分析，以及带来源证据的 Spring 组件、配置类、应用入口、注入、HTTP Endpoint 和组件依赖 facts。类级/方法级路径、HTTP method 与 mapping conditions 均被保留，`ANY` 和未知路径会明确表达。只有唯一确认的项目内组件目标才生成确定边；重复 Evidence 会合并，歧义或缺失目标保留为诊断。MyBatis/MyBatis-Plus Mapper 专用分类仍按计划延后。稳定报告组装和 schema `1.2` JSON 序列化现已接入 loopback 服务、响应式产品 Shell、Repository Overview、Module Explorer、Architecture Workspace、API Map 与共用的 Source & Evidence Detail。模块页面使用显式 Maven 聚合关系、精确坐标唯一确认的内部模块依赖、模块级统计、元数据、源码根、版本事实、诊断与证据。架构页面使用 Cytoscape.js，仅在单一模块范围内绘制确认的组件注入边，并提供可组合筛选、搜索、所选节点一阶邻域、缩放、平移、适配、选择、源码证据、响应式可搜索列表回退，以及明确的部分/不可用/超预算状态。API 页面提供模块/method/文本组合筛选、明确的未解析状态、handler 源码位置和两级 mapping Evidence。组件与 Endpoint Inspector 现在可以进入共用、由报告驱动的来源详情，并提供安全复制反馈和精确相关事实。

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
