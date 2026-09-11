# T-0804 — Start Here UI Data Contract Audit

> 页面级 design handoff。本文连接 T-0804 TODO、T-0801 至 T-0803 的推荐模型、公共 `AnalysisReport`、现有产品 Shell、Stitch 设计与后续 Vue 实现，不是长期架构决策；最终实现状态以 `STATE.md` 为准。

## 1. 审计结论

T-0801 至 T-0803 已具备生成 Start Here 页面所需的排序、文件去重、默认展示边界、可回溯原因、准确计数和覆盖限制，但这些结果目前只存在于 Java `analysis` 层，尚未进入 `/api/report` 或其他前端可读取的 transport。

因此，当前公共 `AnalysisReport` **不足以让前端直接消费已经完成的推荐结果**。前端若根据 `modules[]`、`entryPoints[]`、`components[]`、`endpoints[]` 和 `dependencies[]` 重新实现同一套启发式，会产生两份排序和解释逻辑，违反 T-0801 至 T-0803 已建立的单一事实来源。

T-0804 的最小正确边界是：

> Local UI 从同一个不可变 `AnalysisReport` 快照生成一份只读 Start Here projection，通过固定 `/api/start-here` 路由交给 Vue；公共报告 schema `1.2` 保持不变，前端不重新排名或重写原因。

该 projection 应直接序列化 `RecommendationExplanationGenerator.generate(report)` 的结果，并只补充 transport identity。建议顶层包含：

```text
schemaVersion          Start Here projection schema，初始为 1.0
reportSchemaVersion    与同一服务中的 /api/report 对应，当前为 1.2
projectId              用于确认两个响应属于同一不可变报告
analysisStatus
coverageLimited
coverageLimitationCodes[]
coverageNotice
defaultLimit
totalItemCount
expandable
hiddenItemCount
items[]
```

每个 `items[]` 只需要：

```text
sourceFileId
moduleId
reasons[]
```

每条 `reasons[]` 保留：

```text
kind
message
factCount
dependencyDistance
supportingEntityIds[]
evidence[]
```

`evidence[]` 继续使用公共报告已有的 Evidence / SourceLocation JSON 形状。不要把内部 `ReadingImportanceAssessment`、排序权重或算法中间状态暴露给 UI。

主要代码依据：

- `analysis.RecommendedReadingPath`
- `analysis.RecommendedReadingItem`
- `analysis.RecommendationExplanationGenerator`
- `analysis.ExplainedReadingPath`
- `analysis.RecommendationExplanation`
- `analysis.RecommendationReason`
- `analysis.RecommendationReasonKind`
- `core.model.AnalysisReport`
- `serialization.AnalysisReportJson`
- `serialization.ReportJsonDocument`
- `web.LocalUiServer`
- `frontend/src/App.vue`
- `frontend/src/lib/reportArchitecture.js`
- `frontend/src/lib/reportEndpoints.js`
- `frontend/src/lib/reportSources.js`

## 2. 前提核对与风险

### 2.1 Start Here 不是前端重新评分

推荐顺序必须来自 `RecommendedReadingPath.items()` 的现有顺序。Vue 只能：

- 按数组顺序显示 1-based 序号。
- 在默认和完整列表之间切换。
- 根据 exact IDs 连接展示信息和导航目标。

Vue 不能根据 Endpoint 数量、依赖度、文件名、模块名或用户点击重新排序，也不能生成第二套 importance score。

### 2.2 公共报告 schema 不应为派生页面强行升级

`AnalysisReport` 是稳定、框架中立的分析事实边界；Start Here 是从该事实快照生成的 onboarding projection。把推荐结果直接写回 `AnalysisReport` 会把派生展示结果混入核心事实，并触发没有必要的 schema `1.3` 兼容工作。

固定 `/api/start-here` 路由更符合当前本地 UI 边界：

- 只在服务启动时从同一个 report 生成一次。
- 与 `/api/report` 一样只允许 GET / HEAD。
- 使用相同 Host / Origin 校验、安全响应头和 `no-store`。
- 不接受查询参数、路径或仓库输入。
- 不读取文件系统，不暴露源码正文。

这不是新的远程服务或持久化 API，只是当前 loopback session 的第二个不可变只读响应。

### 2.3 文件级推荐不等于单一声明位置

推荐项按 `sourceFileId` 去重。同一文件可能同时包含：

- Application Entry Point
- Configuration Component
- API Controller
- 多个 Endpoint
- 多条确认依赖原因

因此页面不应为每个文件制造一个唯一“primary line”。文件卡片的稳定主位置是 scan-root-relative `sourceFileId`；具体行列和 symbol 应在各条 reason 的 Evidence 中浏览。若 exact supporting entity 在 `/api/report` 中可连接，可额外显示其真实 entity location，但不能任选一条位置冒充整个文件的唯一来源。

### 2.4 SourceFile 连接可能不存在

根 `pom.xml` 是合法推荐项，但不一定存在于 Java `sourceFiles[]`。因此：

- `sourceFileId` 本身始终是页面可展示的文件路径。
- `moduleId` 通过 `modules[].id` 精确连接模块。
- SourceFile metadata 只在 `sourceFile.id` 或 `sourceFile.path` 与 `sourceFileId` 精确匹配时显示。
- 没有 SourceFile match 不影响根 POM、Evidence 或推荐原因展示。
- 不得按扩展名、目录前缀或 package 猜 module / language。

### 2.5 原因文案来自后端，不由 UI 改写事实

`RecommendationReason.message` 已由 T-0803 根据准确事实生成，例如：

```text
Application entry point
Exposes 2 HTTP endpoints
Used by 1 confirmed component
Depends on 1 confirmed component
Reached from a higher-priority component at confirmed dependency distance 2
```

UI 可以为 `kind` 提供稳定图标或短标签，但必须原样显示 `message`。`factCount` 和 `dependencyDistance` 是结构化核对值，不应用于生成百分比、分数、受欢迎度、业务中心性或阅读时长。

### 2.6 API / Component 跳转只能使用 exact supporting IDs

页面可从 `supportingEntityIds[]` 与 `/api/report` 建立以下精确连接：

- ID 精确等于 `components[].id`：提供 `Explore component`，进入 Architecture 并选择该 Component。
- ID 精确等于 `endpoints[].id`：提供 `Explore endpoint`，进入 API Map 并选择该 Endpoint。
- ID 精确等于 `modules[].id`、`entryPoints[].id` 或 `dependencies[].id`：用于解释和 Evidence，不自动伪装成 Component / Endpoint。

跳转时必须清除会隐藏目标的旧筛选：

- Architecture：切换到目标 component.moduleId，恢复默认类型 / 关系 / 搜索范围，再选择 Component。
- APIs：切换到目标 endpoint.moduleId 或 All modules，HTTP method / search 恢复为能显示目标的状态，再选择 Endpoint。

同一推荐项可能连接多个 Endpoint。设计应逐项显示真实目标，不用一个含糊的“Open related API”随机选择第一条。

### 2.7 Entry Point 和 POM 没有独立详情页

当前 Source Detail 只支持 Component / Endpoint，Modules 页面也没有通用 file selection。T-0804 不应为根 POM 或 Entry Point 添加虚假 `Open source`：

- 它们仍显示文件 path、reason 和 Evidence location。
- 若同一文件还存在 exact supporting Component / Endpoint，可显示对应导航。
- 没有可连接目标时不显示禁用的 IDE、Source 或 Details 动作。

### 2.8 PARTIAL 不是低质量分数

`ExplainedReadingPath.coverageNotice` 和 `coverageLimitationCodes[]` 是页面覆盖说明的唯一来源。PARTIAL / FAILED 时：

- 已确认推荐仍可显示。
- 使用 `reported`、`based on available facts` 等诚实文案。
- 不生成 completeness percentage、confidence score 或预计遗漏数量。
- 不把诊断代码转译为未验证的业务解释。

### 2.9 页面是只读引导，不是学习进度追踪器

当前无账号、保存状态或持久化契约。Stitch 不应加入：

- Mark as read / completed checkbox
- 阅读进度百分比
- 收藏、笔记、协作、分享
- 每文件阅读时长或总耗时估计

本任务只帮助用户决定和理解阅读顺序。

## 3. Transport 字段审计

| UI Field | Semantic Meaning | 来源 | Fact Type | 当前支持 | 处理意见 |
| --- | --- | --- | --- | --- | --- |
| Guide schema | Start Here projection 版本 | 新 transport 常量 | Transport Fact | 缺失 | 新增独立 `1.0`，不改 AnalysisReport schema |
| Report schema | 同一快照的公共报告版本 | `AnalysisReportJson.currentSchemaVersion()` | Transport Fact | 可提供 | 用于前端一致性检查 |
| Project identity | 两个响应属于同一项目 | `report.project.id` | Direct Fact | 可提供 | 与 `/api/report.project.id` 精确比较 |
| Recommended order | 文件阅读顺序 | `ExplainedReadingPath.path.items()` | Deterministic Derived Fact | Java 已支持，transport 缺失 | 由后端序列化；前端不得重排 |
| Display index | 1-based 阅读序号 | `items[]` array index | Deterministic UI Fact | 是 | 前端派生，不序列化重复值 |
| File path | 推荐文件 identity | `item.sourceFileId` | Direct path identity | Java 已支持 | 原样显示；scan-root-relative |
| File label | 路径最后一个 segment | `sourceFileId` | Deterministic Display | 是 | 仅作短标题，不改变 identity |
| Module identity | 文件所属 module | `item.moduleId` | Verified Derived Fact | Java 已支持 | 可为 null；只 exact join |
| Module label | 可读模块名 | exact `moduleId == module.id` | Deterministic Join | 是，可失败 | artifactId 优先、否则 module ID |
| Reasons | 为什么推荐该文件 | `explanation.reasons[]` | Deterministic Derived Fact | Java 已支持，transport 缺失 | 原样显示 message 与 kind |
| Fact count | 原因使用的事实数量 | `reason.factCount` | Verified Count | Java 已支持 | 可作为技术详情，不另算 |
| Dependency distance | 与高优先组件的确认边距离 | `reason.dependencyDistance` | Verified Count | 有条件支持 | 只在 confirmed path reason 显示 |
| Supporting IDs | 原因支持实体 | `reason.supportingEntityIds[]` | Direct Identity References | Java 已支持 | exact join；不解析 ID 文本语义 |
| Reason Evidence | 原因来源证据 | `reason.evidence[]` | Direct Evidence | Java 已支持 | 显示 type、ruleId、location、related locations |
| Source Location | 原因对应的实际位置 | `reason.evidence[].location` | Direct Fact | 是 | 每条原因分别浏览，不造 primary line |
| Related Component | 可导航的组件 | supporting ID exact join `components[]` | Deterministic Join | 是，可为空 | 进入 Architecture selection |
| Related Endpoint | 可导航的 Endpoint | supporting ID exact join `endpoints[]` | Deterministic Join | 是，可为空 | 进入 API selection |
| Default limit | 首屏最大项目数 | `path.defaultLimit` | Deterministic Rule Output | Java 已支持 | 当前最大 10，由 transport 提供 |
| Total count | 完整推荐文件数 | `path.items.size` | Deterministic Count | 可派生 | transport 可显式提供并校验 |
| Expandable | 是否存在隐藏项 | `path.expandable()` | Deterministic Derived Fact | Java 已支持 | 控制 Show more |
| Hidden count | 默认视图隐藏数量 | `path.hiddenItemCount()` | Deterministic Count | Java 已支持 | 按真实数量写按钮文案 |
| Analysis status | 推荐所基于报告的状态 | `explained.analysisStatus` | Direct Fact | Java 已支持 | SUCCESS / PARTIAL / FAILED |
| Coverage limited | 推荐是否受覆盖限制 | `explained.coverageLimited()` | Deterministic Derived Fact | Java 已支持 | 决定提示，不作分数 |
| Limitation codes | 覆盖诊断代码 | `coverageLimitationCodes[]` | Direct / Derived Fact | Java 已支持 | 技术详情，可折叠或放 Inspector |
| Coverage notice | 用户可读覆盖说明 | `coverageNotice` | Deterministic Explanation | Java 已支持 | PARTIAL / FAILED 必须明显展示 |
| Importance score | 推荐强度评分 | 无 | Unavailable | 否 | 不生成 |
| Business summary | 文件业务职责 | 无 | Unavailable | 否 | 不从名称或 path 猜测 |
| Reading time | 预计阅读耗时 | 无 | Unavailable | 否 | 不显示 |
| Completion state | 用户是否读过 | 无持久化 | Unavailable | 否 | 不设计 checkbox / progress |
| Source body | 文件内容 | 公共报告无字段 | Unavailable | 否 | 不显示代码预览 |

## 4. Projection 契约

建议 JSON 形状：

```json
{
  "schemaVersion": "1.0",
  "reportSchemaVersion": "1.2",
  "projectId": "project:sample",
  "analysisStatus": "SUCCESS",
  "coverageLimited": false,
  "coverageLimitationCodes": [],
  "coverageNotice": null,
  "defaultLimit": 10,
  "totalItemCount": 5,
  "expandable": false,
  "hiddenItemCount": 0,
  "items": [
    {
      "sourceFileId": "src/main/java/sample/OrderController.java",
      "moduleId": "module:sample",
      "reasons": [
        {
          "kind": "HTTP_ENDPOINT_EXPOSURE",
          "message": "Exposes 2 HTTP endpoints",
          "factCount": 2,
          "dependencyDistance": null,
          "supportingEntityIds": ["component:order-controller", "endpoint:list", "endpoint:create"],
          "evidence": []
        }
      ]
    }
  ]
}
```

上例只说明字段形状；生产 fixture 的 Evidence 不得为空，因为 `RecommendationReason` 已强制每条原因有 Evidence。

Transport 验证要求：

- `items.length == totalItemCount`。
- `hiddenItemCount == max(0, totalItemCount - defaultLimit)`。
- `expandable == (hiddenItemCount > 0)`。
- items 顺序与 `ExplainedReadingPath.path.items()` 完全一致。
- 每个 item 恰好对应同 index 的 `RecommendationExplanation`。
- 每条 reason 至少有一个 supporting ID 和一条 Evidence。
- 非 SUCCESS 状态必须有 coverage notice。
- `projectId`、`analysisStatus`、`reportSchemaVersion` 必须与 `/api/report` 匹配。

## 5. 前端连接与展示模型

```text
/api/report + /api/start-here
        ↓ verify project/report identity
Start Here projection order
        ↓ exact moduleId join
Module label
        ↓ exact supportingEntityIds join
Related Components / Endpoints
        ↓ reason evidence
Source locations and traceability
```

建议新增纯展示模块：

```text
frontend/src/lib/reportStartHere.js
```

职责：

- 校验 projection 的最小结构和与 report 的 identity 一致性。
- 保持 items 原始顺序。
- 按 `sourceFileId` 派生短文件名，但保留完整 path。
- exact join module、component、endpoint 和 SourceFile。
- 对 supporting IDs 去重并稳定排序导航目标。
- 生成默认 / expanded 可见列表和 selected item model。
- 区分 unavailable、known empty、PARTIAL 与 broken join。

它不负责：

- 重新运行启发式。
- 重写 reason message。
- 按名称推断 component / endpoint。
- 生成 business role、score、reading time 或 progress。

## 6. 页面状态语义

- **Ready / default**：显示最多 `defaultLimit` 项，完整顺序保留；若 expandable，显示准确隐藏数量。
- **Ready / expanded**：显示完整列表，序号不变，可收起回默认数量。
- **Known empty**：projection 可用且 `items[]` 为空，显示 `No recommended files could be produced from the confirmed facts.`；PARTIAL 时同时显示 coverage notice。
- **PARTIAL**：保留已有项目和原因，顶部明显显示后端 coverage notice 与限制代码数量。
- **FAILED with facts**：若仍有项目，使用 `available facts` 文案继续展示；不声称完整。
- **FAILED empty**：显示失败和空 guide，不使用成功式 empty illustration。
- **Guide unavailable**：`/api/report` 正常但 `/api/start-here` 失败或 shape 不兼容；Start Here 页面单独报错，Overview / Modules / Architecture / APIs 继续可用。
- **Report unavailable**：沿用全局 report error；不能单独展示 guide，因为相关实体无法安全连接。
- **Broken module join**：显示 module details unavailable，文件和原因继续可用。
- **Broken supporting-entity join**：原因和 Evidence 继续显示；不显示对应导航动作，不按文本寻找替代实体。
- **No related navigation target**：合法状态；POM 或 Entry Point 项仍可阅读，不显示虚假 action。
- **Stale selection**：expanded/collapse 或响应更新后找不到 selected sourceFileId 时，清除选择并显示选择提示。

## 7. 页面信息分级

### A — 首屏阅读路径

- Start Here 标题与一句产品目的
- analysis status / coverage notice
- reported recommendation count
- 1-based 稳定顺序
- 文件名、完整相对 path、module
- 每个文件的主要原因 message
- 默认 10 项与准确 Show more

### B — 选中项解释与来源

- 该文件全部 reasons
- kind、message、factCount、dependencyDistance
- Evidence type、ruleId、primary / related locations
- exact related Components / Endpoints
- Explore component / endpoint 导航
- SourceFile / module exact metadata（可用时）

### C — T-0804 所需最小缺口

- 独立、版本化的只读 Start Here projection JSON。
- `/api/start-here` 固定 loopback route。
- Vue Start Here 展示模型、页面与 Inspector。
- App 内到 Architecture / API 的 exact selection 导航。

不需要修改 `AnalysisReport`、公共 report schema、核心 analyzer 或 ADR。

### D — 不应出现在 T-0804

- 前端重算 ranking、importance score 或权重
- AI 摘要、业务职责、学习计划或自然语言项目讲解
- Mark as read、进度、收藏、笔记或持久化
- 文件正文、代码预览、语法高亮或 arbitrary file read
- Open IDE、Reveal、file URI、命令行片段
- Git author / history / blame / ownership
- Endpoint 调用链、runtime flow 或架构猜测
- module / component / endpoint 的模糊文本匹配
- 语言设置、Settings 页面或中文翻译扩展（另行排期，不插入本任务）

## 8. 实现边界建议

后续实现建议最小增加：

```text
src/main/java/.../serialization/StartHereJson.java
src/main/java/.../serialization/StartHereJsonDocument.java
src/main/java/.../web/LocalUiServer.java
frontend/src/lib/reportStartHere.js
frontend/src/components/StartHereView.vue
frontend/src/components/StartHereInspector.vue
frontend/src/styles/start-here.css
```

- Java transport 显式映射字段，不开启 polymorphic/default typing。
- Local server 在启动时从同一个 report 生成 report JSON 与 Start Here JSON，并固定为不可变 bytes。
- 前端分别处理 report error 和 guide error，不能因 guide route 失败破坏已有页面。
- `App.vue` 启用 Start Here 导航，维护 selected recommendation，并提供 exact component / endpoint navigation handlers。
- 复用现有 Shell、StatePanel、Inspector、status badge、focus 与 responsive tokens。
- 不需要新依赖、远程资源、数据库或宿主文件能力。

## 9. 设计交接结论

Stitch 应围绕“有证据的第一条阅读路线”设计，而不是把 Start Here 做成评分榜、教程播放器或任务清单。页面主视觉是稳定编号的文件路径；原因、module 和 coverage 解释它为什么在这里；右侧 Inspector 展开每条 Evidence，并只对 exact supporting Component / Endpoint 提供可工作的跨页面导航。
