# T-0708 — Source Navigation UI Data Contract Audit

> 页面级 design handoff。本文连接 T-0708 TODO、公共 `AnalysisReport`、现有 Architecture / API 选择状态、Stitch 设计与后续 Vue 实现，不是长期架构决策；最终实现状态以 `STATE.md` 为准。

## 1. 审计结论

当前 schema `1.2` 足以实现 T-0708 验收要求，不需要扩展后端公共模型，也不需要增加源码读取路由。

T-0708 在 V0.1 中应被定义为：

> 从已选择的 Component 或 Endpoint 打开一个跨页面的 Source / Evidence Detail，展示扫描根相对路径、真实 1-based 位置、symbol、所属 module、Evidence 和可确认的相关 facts，并提供安全复制。

它不是源码浏览器、文件树、代码编辑器或 IDE 集成。公共报告不包含源码正文，当前 loopback 服务也有意不提供任意文件读取。设计不得用示例代码、绝对路径或不可工作的 Open / IDE 按钮填补这个边界。

数据连接必须使用稳定字段：

- Component 来源使用 `component.location`，所属模块只通过 `component.moduleId == module.id` 连接。
- Endpoint handler 来源使用 `endpoint.location`，Controller 只通过 `endpoint.componentId == component.id` 连接，模块只通过 `endpoint.moduleId == module.id` 连接。
- Source File 只通过 `location.sourceFileId == sourceFile.id` 或 `location.sourceFileId == sourceFile.path` 精确连接；不能按文件名或路径前缀猜 module。
- Evidence 使用实体自身的 `evidence[]`，保留 `type`、`ruleId`、primary `location` 和 `relatedLocations[]` 原值。
- Related Components 只来自精确实体连接、同一 Source File 的 Component，或有真实 Component 源/目标的确认 Dependency；不能把 Endpoint 与组件注入关系包装成方法调用链。
- Diagnostic 只在 `diagnostic.fileId` 或 `diagnostic.location.sourceFileId` 精确匹配当前 path 时归入当前文件。

主要代码依据：

- `core.model.AnalysisReport`
- `core.model.SourceFile`
- `core.model.SourceLocation`
- `core.model.Component`
- `core.model.Endpoint`
- `core.model.EntryPoint`
- `core.model.Dependency`
- `core.model.Evidence`
- `core.model.Diagnostic`
- `serialization.ReportJsonDocument`
- `frontend/src/lib/reportArchitecture.js`
- `frontend/src/lib/reportEndpoints.js`
- `frontend/src/App.vue`

## 2. 前提核对与风险

### 2.1 Source Navigation 不等于源码查看器

`AnalysisReport` 只保存来源身份和位置，不保存文件内容。`SourceFile` 只有：

```text
id
moduleId
path
language
location
```

因此 T-0708 可以帮助用户定位和复制来源，但不能展示：

- 源码正文或语法高亮代码片段
- 文件目录树或仓库文件全文搜索
- 上下文行、方法体、imports 或注释
- Git blame、提交历史或修改状态

若 Stitch 生成代码预览，应在实现前删除，而不是请求后端临时读取用户文件。

### 2.2 Path 是扫描根相对路径，不是本机绝对路径

`SourceLocation.sourceFileId` 是规范化的 scan-root-relative path，使用 `/` 分隔。报告没有扫描根绝对路径，前端不能构造 `C:\...`、`file://...` 或 IDE URI。

Copy Path 必须复制报告中的相对路径原值。仓库名、module base directory 或浏览器地址都不能用来拼接绝对路径。

### 2.3 位置字段可部分缺失

行列是 1-based 且可独立缺失，但约束为：

- `startColumn` 只有在 `startLine` 存在时才可能存在。
- `endLine` 只有在 `startLine` 存在时才可能存在。
- `endColumn` 只有在 `endLine` 存在时才可能存在。
- 未知值序列化为 `null`，不能显示 `0`、`line 0` 或空范围。

展示和 Copy Location 使用存在的最小信息：

```text
path
path:startLine
path:startLine:startColumn
```

完整 range 可在详情中展示，但复制定位值优先使用 start position，以保持常见编辑器可识别格式。不能从 end position 或 symbol 推算缺失的 start position。

### 2.4 Symbol 不是方法签名

`symbol` 是分析器报告的可选文本。它可能是 qualified class、`Class#method`、注解名或构建字段，不保证是 Java 完整签名，也不保证全局唯一。

UI 应原样显示和复制；不能补参数、返回类型、括号或包名。symbol 缺失时显示 `Symbol unavailable`，不要从实体名称生成。

### 2.5 Related Components 必须说明关系来源

T-0708 的 Related Components 有三种可证明来源：

1. 当前 Endpoint 通过 `componentId` 精确连接的 Controller。
2. 与当前来源 path 完全相同的 Component 声明。
3. 以当前 Component / Controller 为 source 或 target、且两端均精确连接真实 Component 的 `CONFIRMED` Dependency 邻居。

第三类必须显示 dependency kind、方向和 Evidence。对 Endpoint 上下文应称为 `Controller dependency neighbors`，不能称为 `Endpoint call chain`。`AMBIGUOUS` / `UNRESOLVED` 关系可以作为 coverage 信息显示，但不能生成一个已确认的 Related Component。

### 2.6 Source File 与实体 location 可能连接失败

Component / Endpoint location 本身仍是可展示事实。若 `sourceFiles[]` 缺失、为空或没有 exact match：

- 保留 path、位置、symbol 和实体 Evidence。
- Source File metadata 显示 unavailable。
- Module 优先使用实体自身 `moduleId` 精确连接；不要从 path 猜模块。
- 把 broken link 作为数据完整性状态，不隐藏整个详情。

### 2.7 复制能力不是打开文件能力

T-0708 是首个真实 source action 调用方，可以建立最小 `SourceNavigationHost` 前端边界，但只暴露当前实际需要的复制能力：

```text
copyText(value) -> copied | unavailable | failed
```

Web 实现使用用户手势触发的 Clipboard API。若权限、浏览器环境或 API 不可用：

- 保持目标文本可选择。
- 聚焦只读文本区域或明确指出可手动复制。
- 用可访问 live status 报告 `Copy unavailable — select the text manually.`。
- 不静默失败，不自动复制，不使用隐藏的绝对路径。

当前没有真实 `openSourceLocation` / `revealInFileManager` 能力，不应显示 Open File、Open IDE 或 Reveal 按钮。未来桌面宿主出现后可扩展同一边界，但不属于本任务。

## 3. 字段审计

| UI Field | Semantic Meaning | AnalysisReport 来源 | Fact Type | 当前支持 | 处理意见 |
| --- | --- | --- | --- | --- | --- |
| Selection type | 当前从 Component 或 Endpoint 进入 | 当前页面 selection state | Deterministic UI State | 是 | 只支持本任务要求的两种入口 |
| Entity identity | 被查看实体的稳定身份 | `component.id` / `endpoint.id` | Direct Fact | 是 | 仅用于 identity / state，不作为来源 path |
| Entity label | Component 名称或 Endpoint method + path | Component / Endpoint 原值 | Direct / deterministic display | 是 | unresolved path 保持显式 |
| Source path | 扫描根相对文件路径 | `entity.location.sourceFileId` | Direct Fact | 是 | 原样显示和复制，不生成绝对路径 |
| Start position | 起始行列 | `location.startLine/startColumn` | Direct Fact | 是，可缺失 | 1-based；存在才显示 |
| End position | 结束行列 | `location.endLine/endColumn` | Direct Fact | 是，可缺失 | 仅作 range 详情；不补值 |
| Symbol | 来源符号 | `location.symbol` | Direct Fact | 是，可缺失 | 原样显示 / 复制；不补签名 |
| Source File metadata | 文件 ID、path、language | exact matched `sourceFiles[]` | Deterministic join | 是，可连接失败 | missing collection 与 no match 都不得伪装为已知文件 metadata |
| Module | 来源所属模块 | entity `moduleId` exact join `modules[].id` | Deterministic join | 是，可连接失败 | 显示 module label、base directory、POM；不按 path 推断 |
| Framework / kind | 所选实体上下文 | Component / Endpoint fields | Direct Fact | 是 | 次级 identity 信息 |
| Evidence type | 证据语义类型 | `entity.evidence[].type` | Direct Fact | 是，可为空 | 原样安全显示 |
| Evidence rule | 证据规则 ID | `entity.evidence[].ruleId` | Direct Fact | 是，可为空 | 原样安全显示，不转换成源码 |
| Evidence primary location | 证据主位置 | `evidence[].location` | Direct Fact | 是 | 使用同一位置格式和复制规则 |
| Evidence related locations | 证据关联位置 | `evidence[].relatedLocations[]` | Direct Fact | 是，可为空 | 逐项展示；不解释为调用关系 |
| Owning Controller | Endpoint 所属 Component | `endpoint.componentId` exact join | Deterministic join | 是，可连接失败 | broken link 显示 unavailable |
| Same-file Components | 同一 source path 的 Component | exact `component.location.sourceFileId` match | Deterministic derived fact | 是 | 显示 exact kind / qualifiedName |
| Dependency neighbors | 当前 Component / Controller 的确认邻居 | exact Component IDs + `dependencies[]` | Deterministic derived fact | 是 | 必须保留方向、kind、status；只把 CONFIRMED 两端实体列为 related |
| Same-controller Endpoints | 与当前 Controller 精确关联的 Endpoint | exact `endpoint.componentId` match | Deterministic derived fact | 是 | 展示 reported method/path；不称调用链 |
| Same-file Entry Points | 声明在相同 path 的 EntryPoint | exact location path match | Deterministic derived fact | 是 | 仅作 related fact |
| File diagnostics | 明确关联当前文件的诊断 | exact `fileId` / location path match | Deterministic derived fact | 是 | 原样显示 severity/code/stage/message |
| Copy Path | 复制相对路径 | source path | User action | 是 | Clipboard API + 明确回退 |
| Copy Symbol | 复制 symbol | optional symbol | User action | 有条件 | symbol 缺失时不可复制，并解释原因 |
| Copy Location | 复制 path + start position | location existing fields | Deterministic display + user action | 是 | 不包含伪 0 或 end position 推断 |
| Source code body | 文件内容 | 公共报告无字段 | Unavailable | 否 | 不展示代码预览 |
| Absolute path | 本机完整路径 | 公共报告无扫描根路径 | Unavailable | 否 | 不拼接、不猜测 |
| Open in IDE / file manager | 宿主操作 | 当前无 host capability | Unavailable | 否 | 不显示不可工作动作 |
| Method signature | 完整参数与返回类型 | 公共报告无字段 | Unavailable | 否 | 不从 symbol 补造 |
| Call hierarchy | 方法调用关系 | 公共报告无 method call graph | Unavailable | 否 | 不用组件注入边冒充 |
| File ownership / author | 维护者或 Git 信息 | 公共报告无字段 | Unavailable | 否 | 不展示 |

## 4. 选择、连接与派生契约

### 4.1 Component 入口

```text
Selected Component
    → component.location
    → exact moduleId join
    → exact sourceFileId join
    → component.evidence
    → confirmed Component dependency neighbors
    → same-file facts and exact file diagnostics
```

### 4.2 Endpoint 入口

```text
Selected Endpoint
    → endpoint.location (handler source)
    → exact moduleId join
    → exact componentId join (Controller)
    → exact sourceFileId join
    → endpoint.evidence
    → Controller confirmed dependency neighbors
    → same-controller endpoints and exact file diagnostics
```

Endpoint 的相关依赖必须清楚标注来自 Controller，不代表该 handler 在运行时调用了这些组件。

### 4.3 Stable identity 与返回上下文

- Source Detail selection 使用 `{ entityType, entityId, locationRole, evidenceIndex? }`，不使用 label/path 作为唯一 identity。
- 从 Architecture 进入时保留 module、filters、search、neighborhood 和原 selection。
- 从 API Map 进入时保留 module、method、search 和原 Endpoint selection。
- Back / Close 返回原页面及其 selection，不把 Source Detail 添加成一级导航项。
- Evidence location 可在同一 detail 中切换为当前 location，但 entity context 不丢失。

## 5. 复制契约

允许复制的 payload 只有当前报告可见文本：

| Action | Payload | 可用条件 | 失败回退 |
| --- | --- | --- | --- |
| Copy Path | `sourceFileId` | location 存在 | 可选择文本 + live status |
| Copy Symbol | `symbol` 原值 | symbol 存在 | 显示 `Symbol unavailable`，不生成 |
| Copy Location | `path[:line[:column]]` | location 存在 | 可选择文本 + live status |

行为要求：

- 每次复制都必须来自明确用户点击或键盘激活。
- 成功反馈短暂但可被辅助技术读取，例如 `Path copied.`。
- 失败不覆盖字段内容，不显示笼统成功 toast。
- 文本通过 Vue text interpolation / DOM `textContent` 渲染，不用 `v-html`。
- 不把 path 放入 URL、命令行、shell 字符串或可执行链接。

## 6. 状态语义

- **Ready — Component**：展示 Component declaration、module、source file、Evidence、same-file facts 与确认 dependency neighbors。
- **Ready — Endpoint**：展示 handler declaration、Controller、module、mapping Evidence、same-controller endpoints 与 Controller dependency neighbors。
- **File-only location**：只有 path；显示 `Exact line and column were not reported.`，Copy Path / Location 仍工作。
- **Partial position**：只显示存在的行列，不补齐 range。
- **Symbol unavailable**：位置仍可使用；Copy Symbol 明确不可用。
- **Source File link unavailable**：保留 entity location；file metadata 显示 unavailable。
- **Module link unavailable**：保留 source；module 显示 unavailable，不从 path 推断。
- **Known 0 related facts**：相关 collection 存在且无匹配，显示 `No related facts were reported.`。
- **Related data unavailable**：相关 collection 缺失，显示 unavailable，不能当 0。
- **Clipboard unavailable / denied**：保留可选择文本并提示手动复制。
- **PARTIAL / FAILED**：已确认 source facts 继续显示，文件相关 diagnostics 与 overall status 保持可发现。
- **Stale selection**：当前报告找不到 entity ID 时关闭详情并回到来源页面的明确 unavailable 状态，不按 path 猜实体。

## 7. 页面展示分级

### A — Source Detail 主信息

- 返回当前 Architecture / API 上下文
- entity type、identity label、kind / framework
- scan-root-relative source path
- 真实 1-based start / end position
- symbol 或明确 unavailable
- module exact join
- Copy Path / Copy Symbol / Copy Location
- 复制结果的可访问反馈

### B — 证据与相关事实

- Evidence type、ruleId、primary location、related locations
- owning Controller（Endpoint）
- same-file Components / Entry Points
- same-controller Endpoints
- confirmed dependency neighbors，含方向、kind 和 Evidence
- exact file diagnostics

### C — T-0708 验收所需但当前契约缺失

- **无。**

所有验收项均可由当前报告和 Web Clipboard API 的显式能力检测实现。

### D — 不应出现在 T-0708

- 源码正文、伪代码片段、语法高亮编辑器
- 仓库文件树、全文搜索、方法跳转历史
- 本机绝对路径、`file://` / IDE deep link
- 没有真实宿主能力的 Open File / Open IDE / Reveal
- Git author、blame、commit history、dirty status
- 方法签名、参数、返回类型、源码注释
- Endpoint 调用链或把组件注入关系包装成调用关系
- 根据同文件或路径相似度推断业务关系
- 自动复制、shell command 生成或可执行项目文本

## 8. 实现边界建议

后续 Vue 实现建议增加纯展示层与最小宿主适配：

```text
frontend/src/lib/reportSources.js
frontend/src/lib/sourceNavigationHost.js
frontend/src/components/SourceDetail.vue
frontend/src/components/SourceLocationCard.vue
```

- `reportSources.js` 负责 exact joins、location normalization、related facts、状态与稳定排序。
- `sourceNavigationHost.js` 只封装 `copyText` 和 capability result，不依赖桌面容器。
- `SourceDetail.vue` 是 Architecture / API Inspector 可进入的跨页面辅助表面，不新增一级导航。
- `SourceLocationCard.vue` 复用路径、位置、symbol 和复制反馈，不自行读取文件。

不需要修改 Java model、schema、loopback routes 或 ADR。若后续用户明确要求真实 Open Source，再单独评估宿主能力和安全边界。

## 9. 设计交接结论

Stitch 应围绕“保留原探索上下文的 Source / Evidence Detail”设计：用户从 Component 或 Endpoint 的 Inspector 进入，查看可靠 location、module、evidence、related facts，并复制路径 / symbol / location。设计必须让 file-only、symbol unavailable、broken joins、known empty、data unavailable、clipboard failure 与 PARTIAL 状态都清晰可辨，同时明确避免代码编辑器和虚假宿主动作。
