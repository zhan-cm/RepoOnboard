# T-1302 — Settings UI Data Contract and Stitch Handoff

> 页面级 design handoff。本文连接 M13 TODO、T-1301 文案边界、现有 Shell / theme preference 与后续 Stitch/Vue 实现。用户审阅并导入 Stitch 原型前，不开始 Settings Vue 实现或 T-1303+。

## 1. Data Contract Audit 结论

Settings 的语言设置是纯前端本地偏好，不是 repository analysis fact。页面不需要读取或修改 `/api/report`、`/api/start-here` 或公共 model，也不需要新增 Java transport。

最小状态契约：

```text
supportedLanguages
  - id: en
    selfName: English
  - id: zh-CN
    selfName: 中文

selectedLanguage: en | zh-CN
defaultLanguage: en
persistence: host-wide cookie + localStorage fallback
applyMode: immediate
networkRequired: false
```

语言的 self-name 是固定产品元数据；repository facts 不进入 Settings 页面。

## 2. 页面职责

Settings 回答：

1. 当前产品界面使用哪种语言？
2. 用户切换后何时生效？
3. 偏好保存在哪里，是否需要网络或账号？

页面必须：

- 作为 Sidebar 的 utility destination 提供，不与 Overview / Modules / Architecture / APIs / Start Here 等 repository perspective 混为一组。
- 在 report loading 或 report error 时仍可访问。
- 以 radio group 或等价的单选语义展示 `English` 与 `中文`。
- 选择后立即生效，不使用没有必要的 Save / Apply 按钮。
- 说明偏好只保存在本机浏览器环境，不上传 repository 数据。
- 保持现有 theme toggle 的行为和位置；T-1302 不借机重做 Appearance 设置。

## 3. 字段与动作

| UI Field / Action | 来源 | 类型 | 处理 |
| --- | --- | --- | --- |
| Page title / description | localization resources | Product Copy | 翻译 |
| Current language | preference adapter | Local Preference | 显示 self-name；不显示浏览器推断值 |
| Language options | static allowlist | Product Metadata | 仅 `en`、`zh-CN` |
| Selected state | `selectedLanguage` | Local State | 使用原生 single-select semantics |
| Immediate effect notice | localization resources | Product Copy | 翻译 |
| Persistence notice | localization resources | Product Copy | 明确 local-only、跨随机 loopback port |
| Offline/privacy notice | localization resources | Product Copy | 明确 no account / no upload / no network required |
| Theme | existing `themePreference.js` | Existing Preference | 不改变契约；继续由 topbar 控制 |
| Repository/report facts | 无 | Out of Scope | Settings 不展示虚假项目状态或统计 |

唯一写动作是保存合法语言 ID。非法值不写入；读取到非法值时回退 English。

## 4. 页面状态

- **English selected**：English card selected，页面立即以英文显示。
- **中文 selected**：中文 card selected，页面立即以中文显示。
- **Persistence unavailable**：如果 cookie/localStorage 都不可用，本次内存选择仍立即生效；显示非阻塞的本地化提示，不影响分析页面。
- **Invalid stored value**：静默回退 English，不把损坏值展示给用户。
- **Report loading/error**：Settings 正常可用；不得显示 report loading panel 代替设置。
- **Narrow viewport**：选项纵向堆叠，完整标签和说明可读，不产生横向滚动。

不需要 loading skeleton、远程保存进度、success toast 或 destructive confirmation。

## 5. Stitch 页面方案

请在现有 RepoOnboard workbench Shell 中设计一个 Settings 页面，保持当前 Sidebar、topbar、tokens、圆角、边框、状态色和 light/dark 方向。页面只设计设置体验，不重新设计其他业务页面。

### 5.1 页面结构

```text
Existing Sidebar
  Perspectives
    Overview
    Modules
    Architecture
    APIs
    Start Here
  Utility destination
    Settings

Existing Topbar
  Repository / Settings breadcrumb
  Existing theme toggle

Main content
  Kicker: Preferences
  H1: Settings
  Intro: Control how RepoOnboard presents product guidance on this device.

  Language section
    Heading: Product language
    Explanation: Changes navigation, guidance, states, and accessibility text.
    Radio card: English
      Self-name: English
      Supporting copy: Use the English product interface.
    Radio card: 中文
      Self-name: 中文
      Supporting copy: 使用中文产品界面。
    Inline state: Changes apply immediately.

  Local preference note
    Saved on this device
    Works offline
    No account, cloud sync, or repository upload
```

Stitch 中的英文只是设计样例。最终 Vue 会由资源层提供 English / 中文文案；语言 self-name 必须始终可识别。

### 5.2 交互与无障碍

- 两个语言选项形成一个带可见 legend 的 radio group。
- 整张 option card 可点击，但必须保持原生键盘选择语义、focus ring 和 selected indicator。
- 当前选择同时用 radio state、边框/背景和文字表达，不能只依赖颜色。
- 切换后页面 copy 立即更新，focus 保留在刚选择的 option。
- Settings nav item 提供 `aria-current="page"`。
- 宽屏内容宽度适中，不因没有 Inspector 而拉成超长行。
- 窄屏保留 Sidebar 的既有横向/堆叠降级，不发明 mobile drawer。
- light/dark 都使用现有语义 token，不引入独立色板。

### 5.3 禁止加入

- 账号、登录、profile、workspace/team、cloud sync 或 telemetry。
- Save / Apply / Reset all、语言自动检测或 System language 选项。
- 第三种语言、下载 language pack、在线翻译或远程字体/CDN。
- repository name、module/API 数量、分析进度或伪造的运行状态。
- 绝对路径、源码内容、Evidence 改写或 analyzer 设置。
- Desktop-only 控件、native file picker、更新器或安装器设置。
- 新的主题模式或对现有 theme preference 的重设计。

## 6. Stitch 交付要求

用户完成并审阅设计后，将资产导入：

```text
docs/ui/stitch/T-1302/
  DESIGN.md
  screen.png
  code.html
```

`DESIGN.md` 至少记录：

- 原型来源与导入日期。
- desktop viewport；如另有 narrow variant，记录其 viewport。
- light/dark 中实际提供的 variant。
- 与本 Data Contract 的已知差异。
- 设计中的示例文字/图标哪些仅供视觉参考。

`code.html` 仅作为视觉交付参考，不直接作为生产代码，不得把远程资源或 mock data 复制进 Vue。

## 7. 原型审阅清单

导入后，开始 T-1303 前应确认：

- [ ] Settings 是 utility destination，不是 repository perspective。
- [ ] 页面在没有 report facts 时仍完整成立。
- [ ] English / 中文是唯一语言选项，且是明确单选。
- [ ] 没有不必要的 Save / account / cloud / online language pack。
- [ ] local-only、offline 与 immediate apply 说明可见。
- [ ] keyboard focus、selected state、ARIA 语义可实现。
- [ ] 宽屏、窄屏和现有 light/dark 视觉方向可落地。
- [ ] 无需要修改 report schema、Java API 或分析器的设计元素。

## 8. 后续实现边界

用户导入原型后：

- T-1303 选择最小 localization 实现并建立资源、adapter 与 preference tests。
- T-1304 实现 Settings 及所有已审计 product surfaces 的翻译。
- T-1305 完成浏览器、无障碍、跨端口持久化和生产 JAR 离线验证。

在此门禁完成前，不创建 Settings component、navigation route、语言资源或 preference implementation。

