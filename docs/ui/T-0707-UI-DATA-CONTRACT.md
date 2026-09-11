# T-0707 — API Map UI Data Contract Audit

> 页面级 design handoff。本文连接 T-0707 TODO、公共 `AnalysisReport`、现有产品 Shell、Stitch 设计与后续 Vue 实现，不是长期架构决策；最终实现状态以 `STATE.md` 为准。

## 1. 审计结论

当前 schema `1.2` 已足以实现 T-0707 的 HTTP method、path、controller、handler、source、module / method / path 过滤、mapping conditions 和两级 mapping Evidence，不需要扩展后端公共模型。

API Map 必须直接消费 `endpoints[]`，并通过稳定 ID 连接已有事实：

- Endpoint 所属模块只通过 `endpoint.moduleId == module.id` 连接。
- Controller 只通过 `endpoint.componentId == component.id` 连接。
- HTTP method 直接使用 `endpoint.httpMethod`；`ANY` 与 `UNRESOLVED` 都是显式事实，不能合并。
- 已解析 path 只来自 `endpoint.path`；`unresolvedPath = true` 时展示明确的 unresolved 状态，不能猜测表达式结果。
- Handler 只展示 `endpoint.handlerMethod` 和 Endpoint 自身的 `location`；公共报告没有方法签名、参数、返回类型或源码正文。
- Mapping conditions 只来自 `endpoint.conditions` 的 `params`、`headers`、`consumes`、`produces` 与 `unresolved`。
- Endpoint Evidence 中 `SPRING_MVC_TYPE_MAPPING` 和 `SPRING_MVC_METHOD_MAPPING` 可分组为类级与方法级 mapping 证据；它们证明来源位置，但不能恢复公共报告没有保存的原始注解参数。

主要代码依据：

- `core.model.AnalysisReport`
- `core.model.Endpoint`
- `core.model.EndpointConditions`
- `core.model.Component`
- `core.model.Module`
- `core.model.SourceLocation`
- `core.model.Evidence`
- `serialization.ReportJsonDocument`
- `analysis.ProjectModelAssembler`
- `analyzer.spring.SpringMvcMappingAnalyzer`
- `analyzer.spring.SpringEndpointAnalyzer`
- `frontend/src/App.vue`

## 2. 前提核对与风险

### 2.1 `ANY` 不是 unknown

`ANY` 表示 Spring mapping 没有限定 HTTP method。它是已确认的路由事实，不能显示成 `Unknown`，也不能为了方便被展开为 GET / POST / PUT 等多行。

`UNRESOLVED` 表示 method condition 含无法静态确定的值。它不能与 `ANY` 合并，也不能被默认成 GET。

### 2.2 Unresolved path 不是空字符串

公共模型强制 Endpoint 在以下状态中二选一：

```text
path present + unresolvedPath false
path absent  + unresolvedPath true
```

因此 UI 必须把 unresolved path 作为独立状态展示，例如 `Unresolved path`。不能显示 `/`、空白、`null`、`Unknown route` 或从 handler / annotation 名称生成路径。

### 2.3 Endpoint 已经是组合结果

Endpoint path 是类级与方法级 mapping 组合后的最终 path。一个 handler 可能因为多个 class path、method path 或 HTTP method 组合生成多条 Endpoint。列表必须以 `endpoint.id` 为身份，不能用 path、handler 或两者拼接作为唯一 key。

公共 Endpoint 不保存独立的 class-level path 与 method-level path 字段。Evidence 可以打开两级 mapping 的位置和规则，但 UI 不能声称知道每一级贡献的原始 path 片段。

### 2.4 Conditions 是路由身份的一部分

相同 method、path 和 handler 仍可能因 `params`、`headers`、`consumes` 或 `produces` 不同而成为不同 Endpoint。列表可以折叠视觉重复，但不得合并掉条件差异；默认应保留逐 Endpoint 行并提供 condition 标记。

`conditions.unresolved = true` 表示至少一个条件表达式未完全解析。已确认的条件列表仍可展示，同时必须附加 `Some conditions unresolved`，不能把剩余未知部分当作不存在。

### 2.5 Controller 连接必须保守

正常报告中 `componentId` 指向真实 Controller / REST Controller Component。若报告兼容性、ID 冲突或损坏导致连接失败：

- Endpoint 本身仍可按报告字段展示。
- Controller 显示 `Controller unavailable`。
- 不得用 source path、symbol、handler 名或 Endpoint ID 猜测 Component。

### 2.6 Source 与“打开源码”属于不同范围

T-0707 可以展示 Endpoint handler 的 scan-root-relative path、1-based 行列和 symbol，并显示 Evidence 的位置。Copy Path / Copy Symbol、宿主文件打开和 IDE 集成属于 T-0708，本任务不应提前加入不可工作的按钮。

## 3. 字段审计

| UI Field | Semantic Meaning | AnalysisReport / Public Model 来源 | Fact Type | 当前支持 | Evidence / SourceLocation | 处理意见 |
| --- | --- | --- | --- | --- | --- | --- |
| Endpoint ID | 路由稳定身份 | `endpoints[].id` | Direct Fact | 是 | Endpoint evidence | 仅作 key / selection，不作主标签 |
| Module | Endpoint 所属模块 | `endpoint.moduleId` 精确连接 `modules[].id` | Deterministic Derived Fact | 是 | Endpoint location；Module 有 POM evidence | 支持 All modules / 单模块过滤；连接失败不推断 |
| HTTP method | 最终 method 条件 | `endpoint.httpMethod` | Direct Fact | 是 | Mapping evidence | 精确值包括 GET、HEAD、POST、PUT、PATCH、DELETE、OPTIONS、TRACE、ANY、UNRESOLVED |
| Resolved path | 组合后的最终路由 path | `endpoint.path` | Direct Fact | 是，可为空 | Mapping evidence | 仅在存在时显示；保留 `/` 与 path variable 原值 |
| Unresolved path | path 无法完全静态确定 | `endpoint.unresolvedPath` | Direct Fact | 是 | Mapping evidence + diagnostics | 显式警告标签；绝不生成伪 path |
| Handler | Controller 中的方法名称 | `endpoint.handlerMethod` | Direct Fact | 是 | Endpoint location | 仅方法名；无签名、参数、返回类型 |
| Controller | Handler 所属 Controller | `endpoint.componentId` 精确连接 `components[].id` | Deterministic Derived Fact | 是 | Component location / evidence | 展示 Component `qualifiedName` 与精确 kind；连接失败明确 unavailable |
| Framework | Endpoint 框架 | `endpoint.framework` | Direct Fact | 是 | Endpoint evidence | Inspector 次级字段；当前通常为 Spring Boot |
| Handler source | Handler 声明位置 | `endpoint.location` | Direct Fact | 是 | 即 SourceLocation | 展示相对路径、存在时的 1-based 行列和 symbol；不显示伪 0 |
| Params conditions | 请求参数条件 | `endpoint.conditions.params[]` | Direct Fact | 是 | 两级 mapping evidence | 保留每个字符串，不解释业务语义 |
| Header conditions | 请求头条件 | `endpoint.conditions.headers[]` | Direct Fact | 是 | 两级 mapping evidence | 可能只是 header 名或 Spring condition 表达式；原样安全显示 |
| Consumes conditions | 可消费 media types | `endpoint.conditions.consumes[]` | Direct Fact | 是 | 两级 mapping evidence | 使用列表 / chips，不推断 request body type |
| Produces conditions | 可产生 media types | `endpoint.conditions.produces[]` | Direct Fact | 是 | 两级 mapping evidence | 不推断 response DTO 或 status code |
| Unresolved conditions | 至少一个 mapping condition 未完全解析 | `endpoint.conditions.unresolved` | Direct Fact | 是 | Mapping evidence + diagnostics | 与已知条件同时显示，不把未知部分当空 |
| Type mapping evidence | Controller class mapping 的证据 | `evidence[].type = SPRING_MVC_TYPE_MAPPING` | Direct Fact | 是，可为 0 条 | Evidence location / relatedLocations / ruleId | Inspector 单独分组；0 条表示未报告 class-level mapping evidence |
| Method mapping evidence | Handler method mapping 的证据 | `evidence[].type = SPRING_MVC_METHOD_MAPPING` | Direct Fact | 是 | Evidence location / relatedLocations / ruleId | Inspector 单独分组；Endpoint 正常至少有方法级证据 |
| Evidence related locations | 与 mapping 证据关联的声明位置 | `evidence[].relatedLocations[]` | Direct Fact | 是，可为空 | SourceLocation | 次级展开；不把关联位置说成新路由 |
| Endpoint count | 报告 Endpoint 总数或过滤结果数 | `summary.endpointCount` / 对 `endpoints[]` 确定性计数 | Direct summary / Derived Fact | 是 | 每个 Endpoint 有 evidence | PARTIAL 时使用 `reported endpoints` |
| Search / filter counts | 当前匹配、过滤数量 | 当前 endpoints 与 filter state | Deterministic UI State | 是 | 无独立 Evidence | 显示 `{shown} of {reported}`，不改变报告 |
| Analysis status | 报告 SUCCESS / PARTIAL / FAILED | `report.status`、`summary.coverageLimited` | Direct Fact | 是 | diagnostics | 页面上下文展示；不生成 endpoint health |
| Endpoint diagnostics | mapping / endpoint 覆盖问题 | `diagnostics[]` 中相关 stage / code / file / module | Direct Fact | 是 | 可选 location | 只展示显式诊断；无法可靠关联单 Endpoint 时保持 module / repository-level |
| Class-level raw path | 原始类注解 path 片段 | 公共 Endpoint 未单独保存 | Unavailable | 否 | Evidence 只有位置 / ruleId | 不重建、不从 final path 拆分 |
| Method-level raw path | 原始方法注解 path 片段 | 公共 Endpoint 未单独保存 | Unavailable | 否 | Evidence 只有位置 / ruleId | 不重建、不从 final path 拆分 |
| Java method signature | 参数、返回值、重载签名 | 公共 Endpoint 只有 handler name | Unavailable | 否 | 无 | 删除；同名重载靠稳定 Endpoint ID 与 location 区分 |
| Request / response schema | DTO、字段、状态码 | 公共报告无此模型 | Unavailable | 否 | 无 | 删除，不生成 OpenAPI 风格假数据 |
| Authentication / authorization | 权限与安全规则 | 公共报告无此模型 | Unavailable | 否 | 无 | 删除 |
| Runtime traffic / latency | 请求量、性能 | 静态报告无运行时数据 | Unavailable | 否 | 无 | 删除 |
| Endpoint call chain | Handler 到 Service / Repository 的调用链 | 公共报告无 method call graph | Unavailable | 否 | 无 | 不把组件注入关系冒充调用链 |
| API group / business domain | 用户、订单等业务分组 | 公共报告无正式规则 | Speculative | 否 | 无 | 不按 path / controller 名推断 |

## 4. 过滤与搜索契约

T-0707 的过滤顺序固定为：

```text
Reported endpoints
    → exact module filter
    → exact HTTP method filter
    → case-insensitive path query
    → optional general text search
```

最小实现可以把 path query 与一般搜索合并成一个输入，但必须保证它至少匹配：

- resolved `path`
- 明确文本 `Unresolved path`
- `handlerMethod`
- 已连接 Controller 的 `qualifiedName`
- module label
- source `sourceFileId`

规则：

- Module 使用精确 ID 匹配；`All modules` 是 UI state，不写回报告。
- HTTP method 使用精确值匹配；选择 `ANY` 只显示 `ANY`，不是显示所有方法。
- `UNRESOLVED` 使用精确值匹配。
- Path search 只过滤，不做正则、不执行 glob、不解释 `{id}`。
- Search 不修改 Endpoint identity，也不将多条组合结果合并。
- Reset 恢复 All modules、All methods、空 query，并清除 selection。
- 默认稳定排序建议为 module label → resolved path（unresolved 最后）→ method order → controller qualifiedName → handler → endpoint ID。

## 5. 状态语义

- **Known 0**：`endpoints[]` 存在且为空，显示 `No HTTP endpoints were reported.`；PARTIAL 时附加覆盖提示。
- **Unavailable**：前端收到的报告没有 `endpoints` collection，显示 `Endpoint data unavailable`，不能当作 0。
- **Filtered empty**：报告有 Endpoint，但当前组合筛选无结果，显示筛选条件与 Reset。
- **ANY**：已确认无 method 限制，不是 unknown。
- **UNRESOLVED method**：method condition 未完全解析，保持单独 warning 状态。
- **Unresolved path**：`path` 缺失且 `unresolvedPath = true`，保留 Endpoint 和 Evidence。
- **Unresolved conditions**：已知条件继续显示，同时明确仍有条件无法解析。
- **PARTIAL**：可确认 Endpoint 继续展示，计数使用 reported，诊断保持可见。
- **FAILED**：保留能读取的 Endpoint 与 diagnostics，但不得宣称列表完整。
- **Broken controller link**：Endpoint 仍存在，Controller 显示 unavailable；不猜测目标。

## 6. 页面展示分级

### A — 主列表直接展示

- HTTP method badge
- resolved path 或 `Unresolved path`
- Controller 简称 / qualified name
- Handler method
- Module label
- condition / unresolved 状态标记
- Handler source path 与可用行号

### B — Inspector 展示

- Endpoint ID、framework
- 完整 Controller qualified name 与 component kind
- params / headers / consumes / produces 全量值
- conditions unresolved 状态
- Handler SourceLocation 全字段
- 分组后的 type-level / method-level mapping Evidence
- Evidence ruleId、location、relatedLocations
- 与范围相关的显式 diagnostics

### C — T-0707 需要但当前数据契约缺失

- **无。**

验收要求的 method、path、module 过滤、ANY / unresolved / conditions、handler source 与两级 mapping Evidence 均可由当前报告实现。

### D — 不应出现在 T-0707

- 请求 / 响应 DTO、字段、示例 payload、HTTP status code
- Java method signature、参数类型、返回类型、源码片段
- authentication / authorization
- runtime traffic、latency、error rate
- OpenAPI completeness 或 API quality score
- 根据 path / controller 名推断的业务分组、REST maturity 或 ownership
- Handler → Service → Repository 调用链
- 把组件注入关系当作 endpoint 调用关系
- 从 final path 反拆 class-level / method-level 原始 path
- T-0708 的 Copy Path / Copy Symbol、IDE / 文件打开操作

## 7. 实现边界建议

后续 Vue 实现建议新增独立的纯展示转换层，例如：

```text
frontend/src/lib/reportEndpoints.js
```

它负责：

- 验证 collection 可用性。
- 以 exact ID 连接 module / component。
- 规范化 method、path、conditions、location 与 Evidence。
- 生成稳定排序和纯前端 filter state。
- 区分 known 0、unavailable、unresolved 和 PARTIAL。

组件建议保持：

```text
ApiMapView
ApiInspector
```

本任务不需要图形库、不需要后端 schema 变更，也不应复用 Architecture relationship graph。

## 8. 设计交接结论

Stitch 原型应围绕“可筛选的已报告 HTTP Endpoint 索引 + evidence-first Inspector”设计，而不是 OpenAPI 文档门户。示例内容只能表达布局；后续 Vue 实现只能绑定本文 A / B 类字段，并必须为 ANY、unresolved path、unresolved method、unresolved conditions、known 0、unavailable、PARTIAL 和 Controller 连接失败提供诚实状态。
