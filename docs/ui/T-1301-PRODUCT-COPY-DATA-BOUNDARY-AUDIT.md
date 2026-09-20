# T-1301 — Product Copy and Data-boundary Audit

> M13 的产品文案与数据边界审计。本文只定义 English / 中文界面的翻译范围、事实保真边界与后续实现验收依据，不修改公共报告、分析规则或当前 Vue 行为。

## 1. 审计结论

M13 可以在现有 Vue 应用内完成，不需要修改 `AnalysisReport` schema `1.2`，也不需要新增后端接口、数据库、账号、云同步或运行时网络依赖。

正确边界是：

> 翻译 RepoOnboard 控制的产品界面；保留仓库、分析器和 Evidence 提供的原始事实。

语言切换只改变展示文案，不改变：

- 报告 JSON 与 Start Here projection。
- 实体 ID、连接、筛选、排序、计数和分析状态。
- 路径、符号、类名、方法名、package、module 名、HTTP API 和源码位置。
- Evidence 的 `type`、`ruleId`、location 与 related locations。
- Java、Maven、Spring Boot、Controller、Service、Repository 等技术术语。

默认语言保持 English，以保持当前产品行为；用户可显式切换为中文。语言选择必须本地、离线、确定性生效。

## 2. 当前产品文案清单

文案同时存在于 Vue template、`App.vue` 的运行状态、展示模型 helper 以及浏览器宿主反馈中。后续实现不能只替换页面标题。

| Surface | 当前主要来源 | 必须纳入语言资源的产品文案 |
| --- | --- | --- |
| Product Shell | `App.vue`、`AppShell.vue`、`SidebarNav.vue`、`ContextHeader.vue`、`PageLayout.vue`、`InspectorPanel.vue`、`StatePanel.vue` | 导航、面包屑、仓库上下文标签、统计单位、主题按钮、skip link、状态说明、loading / empty / error / ready 文案、ARIA labels |
| Overview | `OverviewView.vue`、`MetricCard.vue`、`reportOverview.js` | 页面说明、指标标题/说明、确认元数据、入口点、覆盖提示、缺失/空结果、诊断区标签 |
| Modules | `ModuleExplorerView.vue`、`ModuleInspector.vue`、`reportModules.js` | 搜索/筛选、层级说明、metadata/source roots/dependency/content tabs、选择提示、空值与 Inspector 说明 |
| Architecture | `ArchitectureWorkspaceView.vue`、`ArchitectureInspector.vue`、`ArchitectureGraph.vue`、`reportArchitecture.js` | 页面说明、筛选器、图控制、规模预算、关系覆盖、列表替代入口、选择/空/错误状态、图 ARIA summary |
| API Map | `ApiMapView.vue`、`ApiInspector.vue`、`reportEndpoints.js` | 页面说明、module/method/search 筛选、结果计数、conditions、handler、mapping Evidence 分组说明、空/不可用状态 |
| Start Here | `StartHereView.vue`、`StartHereInspector.vue`、`reportStartHere.js`、`App.vue` | 阅读路线说明、范围/覆盖提示、展开/收起、推荐原因的受控显示模板、exact navigation actions、加载与失败状态 |
| Source Detail | `SourceDetailView.vue`、`SourceRelatedInspector.vue`、`reportSources.js`、`sourceNavigationHost.js` | 关闭/复制操作、复制反馈、位置/模块/相关事实说明、空/不可用状态、Inspector headings |
| Settings | T-1302 设计交付；后续新增页面 | Settings 导航、语言名称与说明、当前选择、即时生效、本地持久化与无网络说明 |

样式文件中的可见文字目前只来自组件；CSS class、token 和 selector 不属于翻译资源。

## 3. 翻译分类

### 3.1 必须翻译：RepoOnboard 控制的产品文案

- 一级/二级导航、页面标题、eyebrow、section heading 和说明。
- 按钮、链接、搜索 placeholder、筛选器标签与 option 的产品部分。
- loading、empty、warning、error、ready 状态的标题、说明和恢复动作。
- `Unavailable`、`Unknown`、`Not available`、`None reported` 等展示占位语义。
- count 的单位和语法，例如 modules、components、endpoints、files、facts、relations。
- theme、language、copy、retry、show more、reset、fit 等操作文案。
- tooltip、`title`、`aria-label`、screen-reader-only 文案、live-region 反馈与 skip link。
- 前端控制的网络/兼容错误，例如 report 请求失败、projection 请求失败、schema major 不兼容、图渲染失败。
- 可从结构化字段确定生成的说明，例如 status、reason kind、fact count、dependency distance 和 coverage state 的展示模板。

### 3.2 保持原文：仓库与分析事实

以下值不得翻译、音译、大小写正规化或按文本内容改写：

- repository/project/module/artifact 名称和 Maven coordinates。
- 文件与目录 path、source root、POM path、sourceFile ID。
- package、qualified name、class/interface、method、field、symbol 和 handler 名称。
- HTTP method、route path、mapping condition 的 name/value 与 API 参数。
- Java、Maven、Spring Boot 以及 Controller、REST Controller、Service、Repository、Component、Configuration、Endpoint、Evidence、Source Location 等技术术语。
- schema version、projection version、端口、计数和 1-based 行列。
- 原始枚举/状态值，例如 `SUCCESS`、`PARTIAL`、`FAILED`、`ANY`、component kind、dependency kind 和 diagnostic stage/code。
- Evidence 的 `type`、`ruleId`、primary location 和 related locations。
- 后端 diagnostic 的原始 `message`，因为其中可能包含 path、symbol、Maven 坐标或解析器信息。

原始值可以放在本地化标签之后，例如 `分析状态: PARTIAL`；不能把 `PARTIAL` 本身替换为中文并丢失原值。

### 3.3 结构化本地化：不解析英文原句

部分当前英文文案由展示模型或 Java projection 生成。M13 应使用稳定字段产生本地化展示，而不是搜索/替换英文句子：

| 当前来源 | 本地化依据 | 回退规则 |
| --- | --- | --- |
| Analysis status/coverage heading | `status`、diagnostic count、limitation codes | 显示本地化说明，同时保留 raw status/code |
| Component/relationship labels | enum kind | 已知 kind 使用受控 label；未知 kind 显示原值 |
| Endpoint method/path state | `method`、`unresolvedMethod`、`path`、`unresolvedPath` | 只翻译 unavailable/unresolved 包装，不改 method/path |
| Start Here reason | `kind`、`factCount`、`dependencyDistance`、supporting IDs | 已知 kind 用资源模板；未知 kind 回退原始 `message` |
| Start Here coverage | `analysisStatus`、`coverageLimited`、limitation codes | 翻译产品说明；code 与原始 projection 值保留 |
| Diagnostic | code/stage/severity 可用于本地化标签 | 原始 `message` 始终原样显示，不按英文文本匹配 |
| Clipboard feedback | `copied` / `unavailable` / `failed` result | 翻译反馈；复制内容保持原文 |

这条边界避免两种错误：把 repository fact 当 UI copy 翻译，或为了中文界面在前端复制分析逻辑。

## 4. 建议的语言资源边界

后续 T-1303 应建立一个最小、静态、类型可检查的资源层，建议命名空间如下：

```text
shell.*
navigation.*
common.action.*
common.state.*
common.count.*
overview.*
modules.*
architecture.*
api.*
startHere.*
source.*
settings.*
accessibility.*
```

约束：

- 只支持 `en` 与 `zh-CN`，不建立动态 locale 下载或插件系统。
- 静态资源随生产 bundle 进入 JAR，不在运行时请求 CDN、翻译服务或远程字体。
- 支持变量插值和 count 选择，但不引入对分析数据的二次推理。
- 未知 translation key 在测试中失败；生产回退到 English，不显示 key 本身。
- HTML 不进入翻译字符串；动态数据继续使用 Vue 文本绑定，防止注入。
- 文案层不持有 report，不改变实体 identity，也不决定业务筛选/排序。

是否采用现成 i18n 库由 T-1303 在实现时以依赖重量、离线闭包、plural 支持和可测试性评估；T-1301 不提前锁定新依赖。

## 5. 语言偏好边界

语言偏好应与现有主题偏好采用相同的随机 loopback 端口语义：

- cookie 名：`repoonboard-language`。
- localStorage key：`repoonboard-language`。
- 合法值：`en`、`zh-CN`。
- 默认值：`en`。
- 读取顺序：host-wide cookie → 当前 origin localStorage → default。
- 写入：`Max-Age=31536000; Path=/; SameSite=Strict` cookie 与 localStorage fallback。
- 不保存到分析报告，不上传，不记录 telemetry。
- 不依赖浏览器 `Accept-Language` 自动改变默认值，避免在无显式用户选择时改变当前行为。

这与 `themePreference.js` 的现有策略一致：cookie 可跨随机 loopback port，localStorage 保留当前 origin 的兼容回退。

## 6. 状态、错误与可访问性要求

- 切换语言应立即更新当前页面、Sidebar、topbar、Inspector、dialog/state panel 和 document language；不要求重新分析或刷新。
- `document.documentElement.lang` 分别为 `en` / `zh-CN`。
- live region 的状态反馈必须使用当前语言，但其中的 path、symbol、status/code 保持原样。
- button 的可见文字与 accessible name 必须同步，不能只翻译屏幕文字。
- count、按钮和提示不能通过固定英文拼接，须使用完整资源模板。
- 切换语言不能清除当前 page、module/component/endpoint selection、filters、theme 或 Start Here expanded state。
- report 加载失败时 Settings 仍应可进入，因为语言偏好不依赖 report。
- 未知 backend 文本应原样安全渲染，并由本地化标题说明其上下文。

## 7. 测试清单

T-1303 至 T-1305 至少验证：

- English / 中文资源 key 完整且无空值。
- 默认 English、显式切换、刷新、同 port 和跨随机 port 持久化。
- invalid/corrupt cookie 或 localStorage 值回退到 English。
- 每个现有 surface 的 ready、loading、empty、PARTIAL、FAILED 和 unavailable 文案。
- raw project/module/path/class/method/API/status/code/Evidence 在切换前后逐字一致。
- Start Here 已知 reason kind 可本地化，未知 kind 回退原始 message。
- diagnostic 原始 message 与 Evidence 保持不变。
- `lang`、ARIA labels、live regions、键盘焦点和窄屏导航。
- light/dark 两主题、宽屏/窄屏及生产 JAR 离线资源闭包。
- 不出现远程请求、动态翻译资源、账号/云同步或 telemetry。

## 8. 非目标

M13 不包含：

- 翻译 CLI 输出、README、源码或第三方 repository 内容。
- 修改 analyzer、公共模型、report schema 或 Start Here projection schema。
- 机器翻译、LLM 翻译、在线词典或运行时 locale 下载。
- 更多语言、区域日期/数字系统、用户账号或多设备同步。
- 将 framework/code term 强制中文化。

CLI 本地化可在未来独立任务中评估；当前 M13 的承诺是产品 Web UI。

