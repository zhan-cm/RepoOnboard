# T-0704 — Module Explorer UI Data Contract Audit

> 页面级 design handoff，记录 T-0704 编码前的第一次公共数据契约审计，用于连接公共模型、Stitch UI 设计与 Vue 实现。本文保留审计时点结论；最终实现状态以 `STATE.md` 为准。
>
> 本文不是长期架构决策；长期产品和技术约束仍以 `PROJECT.md`、`DECISIONS.md`、`TODO.md` 和 `STATE.md` 为准。

## 1. 审计结论

当前 `AnalysisReport` 足以实现模块平铺、模块元数据、模块统计和 Inspector，但不足以可靠实现模块层级与内部 Maven 模块依赖。Java / Spring Boot 版本也没有进入公共报告。

当前前端只有 Overview，Modules 导航仍禁用。下文的“支持”指公共数据契约是否支持，不代表 Module Explorer UI 已经实现。

主要代码依据：

- `core.model.AnalysisReport`
- `core.model.Module`
- `analysis.ProjectModelAssembler`
- `serialization.ReportJsonDocument`
- `frontend/src/App.vue`
- `frontend/src/lib/reportOverview.js`

## 2. 字段审计

| UI Field | Semantic Meaning | Repository-level / Module-level | AnalysisReport / Public Model 来源 | Direct Fact / Deterministic Derived Fact | 当前是否真实支持 | Evidence / SourceLocation | 不支持时的处理意见 |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Module hierarchy | Maven `<modules>` 聚合关系；不是 Maven parent 继承关系 | Repository-level | `MavenModule.children` 仅存在于分析器；公共 `modules[]` 已拍平 | Direct analyzer fact | 否 | 当前没有关系级来源；不能用路径或 `parentCoordinates` 重建 | T-0704 必需。最小补充显式 `aggregationParentModuleId`，并保留父 POM 中 `<module>` 声明证据 |
| groupId | 模块 Maven groupId | Module-level | `Module.groupId` | Direct Fact | 是，值可选 | 只有通用 `MAVEN_MODULE` POM 证据；字段原始位置和解析状态已丢失 | 可展示；缺失显示 Unknown，不得声称 Unresolved |
| artifactId | 模块 Maven artifactId | Module-level | `Module.artifactId` | Direct Fact | 是，值可选 | 同上 | 可作为模块主名称；缺失时使用稳定 module ID，不从目录名猜测 |
| version | 模块 Maven version | Module-level | `Module.version` | Direct Fact | 部分支持 | 字段级来源和 `RESOLVED / UNRESOLVED / MISSING` 状态未进入公共模型 | 可按 “Reported version” 展示；空值为 Unknown，不得自行判断变量是否未解析 |
| packaging | Maven packaging | Module-level | `Module.packaging` | Direct Fact | 是，值可选 | 仅通用 POM 证据 | 可展示；空值为 Unknown |
| GAV | groupId、artifactId、version 的组合显示 | Module-level | 由三个 `Module` 字段组成 | Deterministic Derived Fact | 有条件支持 | 继承三个字段的有限证据 | 仅三项都有值时生成 “reported coordinate”；否则展示分项，不制造占位 GAV |
| POM path | 扫描根相对的模块 POM 标识 | Module-level | `Module.pomFileId` | Direct Fact | 是 | `Module.evidence.location` 指向同一 POM，通常只有文件级位置 | 放 Inspector / 次级区域 |
| Base directory | 扫描根相对模块目录 | Module-level | `Module.baseDirectory` | Direct Fact | 是 | 没有专用字段证据；由 POM 所在目录确定 | 放 Inspector；不用于推断层级 |
| Source roots | 报告中被 Java compilation unit 实际使用的源码根 | Module-level | `Module.sourceRoots` | Direct report fact | 是 | 没有 root 专用 Evidence；可由所属 `SourceFile.location` 间接追溯 | 可展示。空列表表示报告中没有对应源码根，不一定表示 Maven 未配置源码目录 |
| Source file count | 进入公共报告、属于该模块的 Java 文件数 | Module-level | 按 `sourceFiles[].moduleId` 过滤计数 | Deterministic Derived Fact | 是 | 每个 `SourceFile` 有 `SourceLocation` | 可直接展示；PARTIAL 时应理解为 “reported files” |
| Component count | 该模块已确认的 Spring 组件数 | Module-level | 按 `components[].moduleId` 过滤计数 | Deterministic Derived Fact | 是 | 每个 Component 有 location 和 Evidence | 可直接展示 |
| Endpoint count | 该模块已报告的 Endpoint 数，包括 path 未解析的 Endpoint | Module-level | 按 `endpoints[].moduleId` 过滤计数 | Deterministic Derived Fact | 是 | 每个 Endpoint 有 location 和 Evidence | 可直接展示；路径未解析不等于 Endpoint 不存在 |
| Entry point count | 该模块已确认的应用入口数 | Module-level | 按 `entryPoints[].moduleId` 过滤计数 | Deterministic Derived Fact | 是 | 每个 EntryPoint 有 location 和 Evidence | 可直接展示 |
| Java version | 模块的有效 Java 编译目标 / 语言版本 | Module-level | 分析器仅保留 Maven properties，例如 `java.version`；公共模型无字段 | 当前不是规范化公共事实 | 否 | 内部 property 可能有 origin，但未进入报告；且单一 property 不足以覆盖 compiler release/source/toolchain | T-0704 模块元数据有明确价值。需要先形成确定性优先级，再补通用 language-version fact；不能直接暴露某个属性 |
| Spring Boot version | 模块检测到的 Spring Boot 版本 | Module-level | `MavenModule.springBoot.version()` 内部存在；`Module.frameworks` 只保留枚举 | Direct analyzer fact，公共边界丢失 | 否 | `SpringBootBuild.Signal.versionOrigin` 内部存在，公共报告无 | T-0704 需要。建议补通用 `FrameworkInfo(framework, version/status, evidence)`，不要加入 Spring 专用 UI 猜测 |
| Framework information | 模块确认使用的框架名称 | Module-level | `Module.frameworks` | Direct Fact | 是，限框架枚举 | 只有通用模块证据；Spring Boot build signal 未进入公共报告 | 框架名称可直接展示；版本属于缺失字段 |
| Internal module dependencies | 一个 Maven 模块显式依赖另一个仓库内模块 | Repository-level / Module-level relation | `dependencies[]` 有 Maven declaration，但当前 target 始终是 external-artifact ID | Direct declaration；内部目标需确定性解析 | 否 | 依赖声明有 POM `SourceLocation`；“目标是内部模块”没有公共关系 | T-0704 必需。最小方案是在唯一确认时令 Maven dependency 的 `targetId` 指向真实 `Module.id`；歧义 / 未解析必须保留状态，不能由 UI 匹配 GAV |
| Component list | 模块所属组件的名称、类型、框架及位置 | Module-level | 按 `components[].moduleId` 选择 | Deterministic Derived Fact | 是 | 每项有 location 和 Evidence | 适合选中模块后的 Inspector / 次级列表；不要在此构建 Architecture 图 |
| Routes | 模块 Endpoint 的 method、path、handler | Module-level | 按 `endpoints[].moduleId` 选择 | Deterministic Derived Fact | 是 | 每项有 location、Evidence、`unresolvedPath` | 详细路由列表属于 T-0707 API Map；T-0704 只保留 endpoint count |
| Analysis status | 整次分析的 SUCCESS / PARTIAL / FAILED | Repository-level | `AnalysisReport.status`、`summary.coverageLimited` | Direct Fact | 仓库级支持；模块级不支持 | 由全局 diagnostics 决定 | 仓库状态继续放 Shell / Header；删除模块级 “Healthy / Success” 徽标 |
| Diagnostics | 分析警告、错误及覆盖限制 | Repository-level，部分可关联 Module | `diagnostics[]` 的可选 `moduleId / fileId / location` | Direct Fact；模块筛选为 derived | 部分支持 | location 可选；并非所有 Maven diagnostic 都带 moduleId | Inspector 展示显式关联项；未关联项必须保持 repository-level |
| Source evidence | 某个分析事实为何成立及其源码位置 | Entity-level / Module-level | `Evidence`、`SourceLocation` | Direct Fact | 支持，但粒度不一致 | Component、Endpoint、EntryPoint、Dependency 较完整；Module 通常只有 POM 文件级证据 | 放 Inspector。不得把 Module 的通用 POM evidence 宣称为 groupId/version 的字段级证据 |

## 3. 状态语义

Module Explorer 必须统一使用以下含义：

- **0**：相关报告数组存在，按明确 `moduleId` 过滤后数量为零。若报告是 PARTIAL，应理解为“已报告 0 项”。
- **Unknown**：公共字段存在，但该模块的值为空。
- **Unavailable**：当前 schema 根本没有这个字段，例如 Java version、Spring Boot version、module parent。
- **Unresolved**：只有报告显式提供 `ResolutionStatus.UNRESOLVED`、`unresolvedPath = true` 或对应诊断时才能使用。
- 不得通过 `${...}`、目录名、artifactId、类名或 ID 前缀自行判断 Unresolved 或建立关系。

## 4. 字段分类

### A — 应直接展示

- groupId、artifactId、reported version、packaging
- Source roots
- Source file count
- Component count
- Endpoint count
- Entry point count
- Framework 名称
- Repository-level analysis status

### B — 有数据，但适合 Inspector / 次级区域

- 有条件生成的 reported GAV
- POM path
- Base directory
- Component list
- 显式关联的 diagnostics
- Source evidence

### C — T-0704 需要，但当前数据契约缺失

- Maven aggregation module hierarchy
- 显式 internal module dependencies
- Java version
- Spring Boot version

其中真正阻塞 Module Explorer 核心结构的是前两项。技术版本可以在同一最小模型补充中处理，但不能用 Maven property 或 Stitch 展示文案代替正式语义。

### D — 不应该出现在 T-0704

- 详细 routes / API 列表
- Module-level analysis status 或 “Healthy” 状态
- Component dependency graph、Architecture graph
- 根据路径、GAV、名称或类名推断出的层级与依赖

## 5. Design-to-Implementation Handoff

T-0704 及后续页面采用以下交付顺序：

```text
TODO / AnalysisReport
        ↓
Codex Data Contract Audit
        ↓
Stitch UI Design
        ↓
Codex Vue Implementation
        ↓
Browser Validation
```

对于 T-0705，应在 Stitch 设计开始前按同一流程审计 Architecture Workspace 所需节点、边、解析状态、Evidence 和 SourceLocation。审计不得为匹配视觉设计而制造不存在的事实，也不得提前实现后续任务。
