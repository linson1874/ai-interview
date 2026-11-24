---
name: AI 面试系统
description: 动物园导览图世界的 AI 面试练习界面——奶油水域与苹果绿陆地由一条 SVG 海岸线独家分界，两档字标锁排，控制件只有胶囊，主操作只有黄。
colors:
  water: "#f5f1e4"
  land: "#8ed462"
  ink: "#2c2e2a"
  ink-soft: "#5b616b"
  sun: "#f5e211"
  sun-deep: "#e5d200"
  sun-press: "#d4c300"
  coral: "#ff705d"
  sky: "#2ba0ff"
  lilac: "#ebc1ff"
  alert: "#c0392b"
  field-surface: "#ffffff"
  legacy-bg: "#f7f8fa"
  legacy-ink: "#101828"
typography:
  wordmark-latin:
    fontFamily: "'Zoo Display', 'PingFang SC', 'HarmonyOS Sans SC', 'Microsoft YaHei', system-ui, -apple-system, 'Segoe UI', sans-serif"
    fontSize: "46px"
    fontWeight: 700
    lineHeight: 0.95
    letterSpacing: "-0.01em"
  wordmark-cn:
    fontFamily: "'Zoo Display', 'PingFang SC', 'HarmonyOS Sans SC', 'Microsoft YaHei', system-ui, -apple-system, 'Segoe UI', sans-serif"
    fontSize: "24px"
    fontWeight: 700
    lineHeight: 1.2
    letterSpacing: "0.3em"
  lede:
    fontFamily: "'PingFang SC', 'HarmonyOS Sans SC', 'Microsoft YaHei', system-ui, -apple-system, 'Segoe UI', sans-serif"
    fontSize: "15px"
    fontWeight: 400
    lineHeight: 1.6
  label:
    fontFamily: "'PingFang SC', 'HarmonyOS Sans SC', 'Microsoft YaHei', system-ui, -apple-system, 'Segoe UI', sans-serif"
    fontSize: "13px"
    fontWeight: 500
    lineHeight: 1.4
  tab:
    fontFamily: "'PingFang SC', 'HarmonyOS Sans SC', 'Microsoft YaHei', system-ui, -apple-system, 'Segoe UI', sans-serif"
    fontSize: "13px"
    fontWeight: 500
    lineHeight: 1
  tab-active:
    fontFamily: "'PingFang SC', 'HarmonyOS Sans SC', 'Microsoft YaHei', system-ui, -apple-system, 'Segoe UI', sans-serif"
    fontSize: "13px"
    fontWeight: 600
    lineHeight: 1
  field:
    fontFamily: "'PingFang SC', 'HarmonyOS Sans SC', 'Microsoft YaHei', system-ui, -apple-system, 'Segoe UI', sans-serif"
    fontSize: "14px"
    fontWeight: 400
    lineHeight: 1.6
  button:
    fontFamily: "'PingFang SC', 'HarmonyOS Sans SC', 'Microsoft YaHei', system-ui, -apple-system, 'Segoe UI', sans-serif"
    fontSize: "15px"
    fontWeight: 600
    lineHeight: 1
    letterSpacing: "0.02em"
  chip:
    fontFamily: "'PingFang SC', 'HarmonyOS Sans SC', 'Microsoft YaHei', system-ui, -apple-system, 'Segoe UI', sans-serif"
    fontSize: "13px"
    fontWeight: 600
    lineHeight: 1
  note:
    fontFamily: "'PingFang SC', 'HarmonyOS Sans SC', 'Microsoft YaHei', system-ui, -apple-system, 'Segoe UI', sans-serif"
    fontSize: "12px"
    fontWeight: 400
    lineHeight: 1.6
  foot:
    fontFamily: "'PingFang SC', 'HarmonyOS Sans SC', 'Microsoft YaHei', system-ui, -apple-system, 'Segoe UI', sans-serif"
    fontSize: "11px"
    fontWeight: 400
    lineHeight: 1.6
  error:
    fontFamily: "'PingFang SC', 'HarmonyOS Sans SC', 'Microsoft YaHei', system-ui, -apple-system, 'Segoe UI', sans-serif"
    fontSize: "12px"
    fontWeight: 400
    lineHeight: 1.4
rounded:
  cap: "999px"
  square: "0"
  dot: "50%"
components:
  wordmark-latin:
    textColor: "{colors.ink}"
    typography: "{typography.wordmark-latin}"
  wordmark-cn:
    textColor: "{colors.ink}"
    typography: "{typography.wordmark-cn}"
  tab:
    textColor: "{colors.ink}"
    typography: "{typography.tab}"
    rounded: "{rounded.cap}"
    height: "34px"
    padding: "0 18px"
  tab-active:
    backgroundColor: "{colors.ink}"
    textColor: "{colors.water}"
    typography: "{typography.tab-active}"
    rounded: "{rounded.cap}"
    height: "34px"
    padding: "0 18px"
  input:
    backgroundColor: "{colors.field-surface}"
    textColor: "{colors.ink}"
    typography: "{typography.field}"
    rounded: "{rounded.cap}"
    height: "46px"
    width: "100%"
    padding: "0 18px"
  input-placeholder:
    textColor: "{colors.ink-soft}"
  input-error:
    backgroundColor: "{colors.field-surface}"
    textColor: "{colors.ink}"
    rounded: "{rounded.cap}"
    height: "46px"
    width: "100%"
  button-primary:
    backgroundColor: "{colors.sun}"
    textColor: "{colors.ink}"
    typography: "{typography.button}"
    rounded: "{rounded.cap}"
    height: "46px"
    width: "100%"
    padding: "0 18px"
  button-primary-hover:
    backgroundColor: "{colors.sun-deep}"
  button-primary-active:
    backgroundColor: "{colors.sun-press}"
  form-label:
    textColor: "{colors.ink}"
    typography: "{typography.label}"
    padding: "0 0 6px"
  legend-chip:
    backgroundColor: "{colors.coral}"
    textColor: "{colors.ink}"
    typography: "{typography.chip}"
    rounded: "{rounded.cap}"
    height: "30px"
    width: "72px"
  legend-chip-sky:
    backgroundColor: "{colors.sky}"
    textColor: "{colors.ink}"
    typography: "{typography.chip}"
    rounded: "{rounded.cap}"
    height: "30px"
    width: "72px"
  error-note:
    backgroundColor: "{colors.water}"
    textColor: "{colors.alert}"
    typography: "{typography.error}"
    rounded: "{rounded.cap}"
    padding: "3px 10px"
  foot-note:
    textColor: "{colors.ink-soft}"
    typography: "{typography.foot}"
---

# Design System: AI 面试系统

> **已废除**：本文件此前记录的「品类标准」世界（冷白纸底、纯白居中卡、深蓝主色 `#1d4ed8`）已整体替换为动物园导览图世界，旧令牌表不再保留；下文只描述现行系统。

## Overview

**Creative North Star: "走进一张已经画好的导览图"（The Guide-Map You Walk Into）**

整个界面是一张 100dvh 的园区导览图，不是一个摆在背景前的卡片。奶油色是水域（`--zoo-water`），苹果绿是陆地（`--zoo-land`），两者唯一的分界是一条手绘感的平滑 SVG 海岸线；陆地中段偏右开一个水洞（`--zoo-line` 勾 1px 墨边），让水域从陆地内部透出来。登录入口站在岸上：左侧陆地是两档字标锁排（`AI` / `面试系统`）、一句承诺与表单，右侧水域右对齐地摆图例，告诉访客园区里有简历和面试。世界来源是 `aww2-zoo-gardens-guide-map`（2026-11-24 定稿，全站唯一视觉世界，换页不换世界）。明确拒绝两件事：深色渐变 + 居中卡片的 AI 产品皮；把导览图当贴纸贴在表单背后、界面其余部分照旧。

这是一个 Operate 面，一屏只做一件事：认出「这是练习区」→ 在陆地上填两个字段 → 按下唯一一枚黄色胶囊。层级几乎全部由字号、字重、2px 墨边与留白建立；颜色严格分工——绿与奶油负责「哪边是水、哪边是地」，墨负责写字，黄负责「点这里」，珊瑚 / 蓝 / 淡紫只负责「园区里有这种东西」，红只负责「这里错了」。状态反馈也只有墨边与外框，没有卡片、没有投影阶梯、没有玻璃、没有发光。

**当前边界（2026-11-24 终审后实查代码）：**
- 唯一已设计页面只有登录/注册一页（`/login`，`frontend/src/views/auth/LoginRegisterView.vue`）；`/resumes` 仍是 `el-empty` 占位壳（`ResumeListView.vue`），没有第二个已设计 surface 可记录。
- 没有深色模式：`tokens.css` 固定 `color-scheme: light`，全仓没有 `prefers-color-scheme` 分支。
- 没有 logo、图形字标或任何页面图片：产品名是两档字标锁排，`frontend/public/` 只有 `favicon.ico`（浏览器标签图标，不是页面素材）。
- 字体请求只有一套自托管显示子集：**Zoo Display**（LXGW WenKai 1.7.0 子集，OFL-1.1，许可证在 `frontend/src/assets/fonts/OFL.txt`）——只含 `A I 面 试 系 统` 六个字形，拆成 4 个 woff2 共 3820 字节，四条 `@font-face` 只写在 `base.css` 第 3–37 行（仓库里没有第二份字体 CSS），`font-weight: 700`、`font-display: swap`、各带 `unicode-range`。正文与控件仍走系统栈 `--ai-font-sans`，不引任何外部/在线字体。
- Element Plus **全量引入**（`main.ts`：`app.use(ElementPlus)` + `element-plus/dist/index.css`，EP 2.14.8），没有按需引入插件；所有表单、tab、消息、按钮行为都来自 EP，令牌只负责覆写外观。
- 状态零阴影：全页没有任何带偏移的 box-shadow，状态只用 inset 墨边与 2px outline；`--ai-shadow-card` 仍声明但已无任何选择器使用。
- 没有渐变、没有发光：`frontend/src` 里没有 `linear-gradient` / `radial-gradient` / filter 光晕。
- 间距没有令牌层：`tokens.css` 不定义 spacing 变量，布局常数直接写在页面里（见 Layout）。
- 全局层已迁到新世界：选区、光标、全局键盘焦点环全部改用 `--zoo-ink`；Element Plus 的 primary 指向墨色，light-3..9 是手写灰阶，`--el-border-radius-base` 指向胶囊。只有 `body` 的底色与字色还挂在 `--ai-bg` / `--ai-ink` 上（未迁移页兜底），其余 `--ai-*` 只剩声明、无人引用。

### 未实现的世界层面（下一轮，不是遗留缺陷）

导览图的信息层还没做：园路、分区与分区标签、入口标记、指北针、比例尺、图例与图面编号的对应关系，以及"你在这里"的定位点。现在落地的是**图的地基**——两片色场、一条海岸线、一个水洞、两档字标、表单与两枚图例胶囊，不是一张完整的导览系统。

Authored motion 也还没做：海岸线没有绘制动画、水洞没有波纹、tab 没有滑动指示条、页面没有入场或状态编排；动效只有 Element Plus 默认的按钮过渡与输入边 2px→3px 的过渡，`prefers-reduced-motion` 只关闭后者。

这些是下一轮 world-building 的工作，不是本轮验收的遗留缺陷。新页面不要为了"看起来更像导览图"自行发明园路、指北针或比例尺；等信息层由方向层定义后再做。

**Key Characteristics:**
- 两张色场定天下：奶油水域（`--zoo-water`）+ 苹果绿陆地（`--zoo-land`），没有第三种底色。
- 分界只由一条 SVG 海岸线画：宽屏从左涌入、窄屏自上而下，页面别处不画分界。
- 形状只有两档：胶囊 999px 与方角 0；没有第三档圆角，EP 的基础圆角也已改成胶囊。
- 主操作只有黄：`--zoo-sun` 三阶（默认 / hover / 按下），一屏一块黄色实心。
- 图例三色只做图例：珊瑚、蓝、淡紫不上按钮、不上正文、不做边框。
- 文字只有两级墨：`--zoo-ink` 与 `--zoo-ink-soft`，主次靠字号与 400/500/600（字标 700），不靠第二套灰。
- 线只有墨线：海岸线/水洞 1px（`--zoo-line`）、输入静止 2px 与 hover 3px、tab/按钮 1px、焦点外框 2px，没有双层描边。
- 显示声部只有字标锁排：`AI` 46px/700 与 `面试系统` 24px/700（0.3em 字距）上下锁排，两档都用自托管的 Zoo Display 子集，不靠图片或字体特效。
- 平的一层：没有带偏移的投影，没有零模糊彩色光晕；状态靠墨边与实心 outline。

## Colors

一句话：两片色场画一张图，墨写字，黄指路，三色做图例，红报错。

### Primary

- **主操作黄 Sun**（`#f5e211`）：唯一主按钮的填充色，配墨色文字（对比度 10.3:1）。它是页面上唯一允许出现的黄色实心块，一屏只有一块。
- **Hover 深黄 Sun Deep**（`#e5d200`）：主按钮 hover 底色，墨字 8.85:1；边框仍是墨，不换色。
- **按下黄 Sun Press**（`#d4c300`）：主按钮 active 底色，墨字 7.59:1；按下只压暗黄，不做位移、不做投影。
- 三阶黄都只服务主操作。tab 选中态用墨底奶油字，不用黄——黄色一旦出现在非提交控件上，主操作就不再唯一。

### Secondary

图例三色，只承担「这是什么」的标签语义：

- **珊瑚 Coral**（`#ff705d`）：图例胶囊「简历」的底色，墨字 5.05:1。
- **蓝 Sky**（`#2ba0ff`）：图例胶囊「面试」的底色，墨字 4.95:1。
- **淡紫 Lilac**（`#ebc1ff`）：已在 `tokens.css` 声明为图例色，当前页面没有任何元素使用；保留给后续 surface 的第三类图例，用法与珊瑚、蓝完全同级（墨字、胶囊、不上主操作）。

### Neutral

- **水域奶油 Water**（`#f5f1e4`）：页面底色、错误胶囊底色、水洞填充。它同时是「水域」与「纸面」两个角色，靠海岸线区分语义。
- **陆地苹果绿 Land**（`#8ed462`）：海岸线以内的填充色，只由 SVG path 上色，页面别处没有绿色块。
- **墨 Ink**（`#2c2e2a`）：标题锁排、正文、标签、输入值、输入边、tab 未选文字与选中底色、按钮文字与边框、海岸线与水洞描边、全局焦点环与选区字色。一种墨走全场，不做第二种深色。
- **次级墨 Ink Soft**（`#5b616b`）：水域一侧的说明文字（12px 图例说明、11px 页脚）、图例小标题、输入占位符。它只许出现在水域（5.52:1）与白底输入框（6.24:1）上，**不许出现在绿地上**（3.49:1，未过 4.5:1）。
- **输入白 Field Surface**（`#ffffff`）：胶囊输入框底色，让字段从绿地中浮出来；页面里写的是裸值 `#ffffff`（`tokens.css` 没有对应的 `--zoo-*` 白），新页面若要复用请先补一个水域之外的中性面令牌。
- **错误红 Alert**（`#c0392b`）：只用于错误文字、错误圆点与错误外框；不做强调、不做装饰。作为非文本指示，红框压绿地 3.04:1 刚过 3:1 门槛，所以它只做边、不做大面积。

### 全局层与 Element Plus 映射（现状）

- **选区**：底色 `rgba(44, 46, 42, 0.16)`、字色 `--zoo-ink`（`base.css`）。这个 rgba 与自动填充的 `#ffffff` 是全局层仅有的两个裸值。
- **光标**：`input` / `textarea` 的 `caret-color` 与自动填充态都用 `--zoo-ink`；自动填充用 `0 0 0 1000px #ffffff inset` 压回白底（白也是裸值）。
- **全局键盘焦点**：`:focus-visible { outline: 2px solid var(--zoo-ink); outline-offset: 2px }`。蓝环已经不存在。
- **Element Plus**：`--el-color-primary: var(--zoo-ink)`；`--el-color-primary-dark-2: #1d1f1c`；light-3/5/7/8/9 是手写的灰阶 `#6f726c` / `#969a93` / `#bdbeb8` / `#d0d1cc` / `#e4e4e0`（不是从主色推导）；`--el-color-danger: var(--zoo-alert)`；文字 primary=墨、regular/secondary/placeholder=次级墨；`--el-border-color`（含 light/lighter）=墨；`--el-border-radius-base: var(--zoo-cap)`；`--el-font-family: var(--ai-font-sans)`。
- **后果**：EP 的 tab hover、消息、密码可见性图标、默认按钮的焦点/悬停都落到墨 / 次级墨与胶囊，不再从缝里漏蓝。

### 旧层残留（只剩 body 兜底）

- `--ai-bg`（`#f7f8fa`）与 `--ai-ink`（`#101828`）仍是 `body` 的底色与字色；登录页用 `.zoo` 覆盖了这两项，所以它们只在未迁移页（`/resumes` 占位）与页面之外露出来。
- `--ai-surface` / `--ai-surface-sunken` / `--ai-border` / `--ai-border-strong` / `--ai-ink-2` / `--ai-ink-3` / `--ai-primary` / `--ai-primary-hover` / `--ai-primary-active` / `--ai-primary-soft` / `--ai-danger` / `--ai-danger-soft` / `--ai-shadow-card` / `--ai-radius-sm` / `--ai-radius-md` 仍在 `tokens.css` 声明，但已没有任何选择器或 EP 变量引用它们（深蓝 `#1d4ed8` 只活在声明里）。`tokens.css` 的文件头也写明这些"仅为未迁移页面保留，不要再新增使用"。
- `--ai-font-sans` 与 `--ai-text-*` 仍是活的共用基础设施，继续被页面与 `base.css` 使用。

### Named Rules

**The Yellow-Only-Action Rule（主操作只能是黄）.** 主按钮填充只用 `--zoo-sun` / `--zoo-sun-deep` / `--zoo-sun-press` 三阶，文字与边框始终墨色。珊瑚、蓝、淡紫只做图例与状态说明，永远不上按钮、链接、提交或表单控件。

**The Legend-Not-Action Rule（图例色不做动作）.** 珊瑚 / 蓝 / 淡紫表达「园区里有这种东西」，不表达「点这里」。它们不做出边框、不做正文色、不做 focus 环，也不允许为了"丰富一点"而落到主操作上。

**The Ink-On-Land Rule（陆地上只许用墨）.** 绿地上的文字只能用 `--zoo-ink`（7.67:1）；`--zoo-ink-soft` 在绿地上只有 3.49:1，不够正文标准，只许待在水域一侧或白底输入框里。需要更弱的层级就降字号或加字距，不要给绿地配浅灰。

**The Cream-Under-Red Rule（错误文字坐在奶油上）.** 红字压绿只有 3.04:1，不合格；错误文字必须坐在奶油色胶囊里（红压奶油 4.81:1），并配一枚红色圆点，语义不只靠颜色。

## Typography

**Display Font:** Zoo Display（自托管子集，LXGW WenKai 1.7.0 / OFL-1.1；仅 6 字形、4 个 woff2 共 3820 字节、只有 700 一档）
**Body Font:** PingFang SC → HarmonyOS Sans SC → Microsoft YaHei → system-ui → -apple-system → Segoe UI（`--ai-font-sans`，正文与控件）
**Label/Mono Font:** 无独立标签字体，也没有等宽字体

**Character:** 正文与控件不下载字体，字形随操作系统变化；显示声部由**自托管子集 Zoo Display**承担——LXGW WenKai（OFL-1.1）用 fontTools 裁到 6 个字形，按原分片的 unicode-range 拆成 4 个 woff2（合计 3820 字节），四条 `@font-face` 只在 `base.css` 一处，`font-weight: 700`、`font-display: swap`。两个字标 span 都用 `font-family: 'Zoo Display', var(--ai-font-sans)`：`AI` 46px/700、行高 0.95、`-0.01em`；`面试系统` 24px/700、行高 1.2、`0.3em` 宽字距；两行在同一个 `<h1>` 里纵向锁排、间距 2px，整块高约 74px。运行时 4 条字体全部 loaded、`document.fonts.check("700 46px 'Zoo Display'")` 为 true。正文与控件的层级仍靠字号与 400/500/600，字距只在短标签上放大：水域小标题「导览图 · 园区里有这些」`0.08em`、按钮文字 `0.02em`。

### Hierarchy

- **Wordmark Latin**（Zoo Display，700，46px/0.95，`-0.01em`）：`AI` 一行，字标的上档。
- **Wordmark Chinese**（Zoo Display，700，24px/1.2，`0.3em`）：`面试系统` 一行，字标的下档；宽字距是这一档的识别点。
- **Lede**（400，15px/1.6，`max-width: 44ch`）：字标下一句承诺。
- **Label**（500，13px/1.4）：字段标签，位于输入框上方（EP 顶部布局自带 8px 下边距，页面再加 6px 内边距）；tab 标签 13px/500，选中加粗到 600。
- **Field**（400，14px/1.6）：输入值；占位符同字号、`--zoo-ink-soft`。
- **Button**（600，15px/1，`0.02em`）：主按钮文字，墨色。
- **Chip**（600，13px/1）：图例胶囊文字，墨色。
- **Note**（400，12px/1.6）：水域小标题（0.08em 字距）、图例说明、错误文字（错误另用 1.4 行高）。
- **Foot**（400，11px/1.6）：水域右下角账号用途一行。

### Named Rules

**The Two-Ink Rule（两级墨）.** 文字只有 `--zoo-ink` 与 `--zoo-ink-soft` 两级，且第二级不许上绿地（见 Colors 的 Ink-On-Land Rule）。层级用 11/12/13/14/15/24/46 与 400/500/600 建立（字标另用 700），不引入第三级灰、不用黄色或图例色写正文。

**The Subset-Display Rule（显示字只给六个字形）.** 唯一的 webfont 是 Zoo Display：LXGW WenKai 1.7.0 的 OFL-1.1 子集，4 个 woff2 共 3820 字节，只含 `A I 面 试 系 统`，只有 700 一档，`font-display: swap`，许可证随字体入仓（`assets/fonts/OFL.txt`）。它只许用在两个字标 span 上；正文、标签、控件一律 `--ai-font-sans` 系统栈。不许加字形、加字重/斜体，或把它用到别的文案上——缺字会静默回退成系统字，看起来像字体坏了。

**The Lockup-Not-Logo Rule（字标是排出来的，不是画出来的）.** 显示声部只能由两档字标锁排承担：`AI` 46/700 + `面试系统` 24/700/0.3em，字体是 Zoo Display 子集 + 系统栈回退。不引入 logo 图片、图标字标或第二套展示字体；改字号可以，改结构或换字体要重开方向。

## Layout

整页就是那张图：`.zoo` 撑满 `100dvh`、`overflow: hidden`、底色奶油，内部是一个 56% / 44% 的两列 grid（`.zoo__grid`，同样 `min-height: 100dvh`）。**这两列只是排版网格，不是水陆分界**——分界由 SVG 画（见下）。

- **海岸线层**：两条 `<svg class="zoo__coast">` 绝对定位铺满整页宽度（`inset: 0`、`width: 100%`、`height: 100dvh`、`pointer-events: none`、`aria-hidden="true"`），`preserveAspectRatio="none"`：path 只定义拓扑，比例随视口拉伸。高度钉死 `100dvh`，不再随内容拉伸——否则海岸线会被拽出视口。宽屏一条 `viewBox="0 0 1440 900"`，窄屏一条 `viewBox="0 0 390 844"`，用媒体查询互斥显示。
- **宽屏几何实况**（1440×900 下按 path 采样）：岸线从页顶 x=520（36%）起，y≈300 摆到 619（43%），y≈500 在 620（43%），y≈648 到 668（46%），最右点 **x≈761、y≈768**（曲线控制点 780），页底收到 700（49%）。它不对应 56% 的栅格线（1440 下为 806px）。任何按栅格边对齐的内容都不会"正好站在岸上"，要按 path 留安全距（表单右缘止于 452px）。
- **左侧陆地（`.zoo__plot`）**：`align-self: start`，宽 100%、`max-width: 520px`，内边距 `56px 0 56px 72px`（内容宽 448px）。表单再收窄到 `max-width: 380px`，左对齐。
- **陆地垂直节奏**（自上而下）：字标锁排（约 74px 高）→ 承诺句（上 10px）→ tabs（上 34px；tab 34px；表头下 22px）→ 字段（标签行 18px + 内边距 6px + EP 标签下边距 8px + 输入 46px + 项下 16px）→ 主按钮（上 6px，高 46px）→ 表单结束。整块从 56px 开始，顶部对齐，不垂直居中。
- **右侧水域（`.zoo__water`，桌面）**：flex 纵向、`align-items: flex-end`、`justify-content: center`、`text-align: right`、`gap: 22px`，内边距 `56px 72px 96px 32px`；图例块定宽 292px、两列 `72px 1fr`、列间隔 12px、`justify-items: start`，整块右靠——块右缘 1368，胶囊 1076–1147，说明从 1161 起。页脚 `.zoo__foot` 绝对定位在 `right: 72px; bottom: 32px`（相对水域列）。
- **间距没有令牌层**：`tokens.css` 没有 spacing 变量。实际复用值为 6 / 8 / 10 / 12 / 16 / 18 / 22 / 32 / 34 / 40 / 56 / 72 / 96 px；新页面先复用这组，抽 `--zoo-space-*` 是后续动作，不是现状。
- **响应式只有一档**：`max-width: 899px`。窄屏把 `.zoo__grid` 从 grid 改成 flex 列（陆地在上），`.zoo__plot` padding 变 `36px 24px 32px`；`.zoo__water` 改为**绝对定位** `top: max(84.1dvh, 620px)`、`left/right: 0`、`padding: 6px 24px 12px`、`gap: 8px`、左对齐；图例块宽 100%、胶囊高 24、说明左对齐；`.zoo` 改 `overflow: visible`，矮视口改成页面滚动，不再裁掉水域。
- **窄屏的定位耦合（实查）**：`84.1dvh` 来自窄屏 path 的陆地底边 `710/844`；水域绝对定位在它下面，并在矮视口用 `620px` 兜底。390×844 下：注册 CTA 底缘 590、水域顶 710（+6px 内边距后首行 716），中间约 120px 是绿地；即使视口矮到 620px 以下，水域仍从 620 起、页面可滚，不会压到 CTA（最小间隔 30px）。代价是海岸线高度钉死 `100dvh`、水域不参与文档流——改窄屏结构时必须同时复核 `84.1dvh` 与 `620px` 这两个数。
- **窄屏没有水洞**：窄屏 path 只有陆地轮廓，水洞（pond circle）只存在于宽屏 SVG。

### 与契约的偏差（2026-11-24 复核）

方向契约的 FIRST VIEWPORT 是草图，以下是代码与草图仍不一致的地方（以代码为准）：

- **海岸线摆幅**：契约写"在 x≈520–700 之间摆动"，实现在下段明显外扩，最右点 x≈761、y≈768（控制点 780），比 700 多出约 60px。
- **陆地占宽**：契约写"约占 58%"，实际栅格 56/44，岸线顶部只到 36%、最右 53%；56% 只是列宽，不是岸的位置。
- **产品名与印章**：契约写"18×18 墨方 + 「导览图」12px → 34px/600 产品名"，实现已删除印章与墨方，产品名改成 `AI` 46/700 + `面试系统` 24/700/0.3em 的两档锁排。
- 其余仍与契约一致：表单 380px、输入 46px/白底/2px 墨边/胶囊、黄色胶囊主按钮、水域两枚图例胶囊、页脚右下 11px、海岸线 1px 墨边、水洞 ⌀200、无渐变无阴影。

### Named Rules

**The Coast-Is-The-Only-Boundary Rule（分界只由海岸线定义）.** 水与陆的分界只能来自那条 SVG path（连同它的 1px 墨边）。禁止用 border、背景色块、伪元素、卡片边缘或栅格线伪造第二条海岸线；水域里的图例、陆地里的输入框可以有各自底色，但那只是元素底色，不是水陆分界。

**The Grid-Is-Not-The-Coast Rule（栅格不是岸）.** 56/44 是排版列，不是水陆线；内容必须按 path 的摆动留距，不许把陆地列宽当成"岸的位置"，也不许为了对齐栅格而移动 path。

## Elevation & Depth

**平的，而且这次是字面意义上的平。** 页面没有任何带偏移的 box-shadow：没有卡片、没有悬浮层、没有渐变、没有模糊光晕、没有彩色发光。状态只由两种东西表达——**墨色 inset 边**（输入框的 2px 静止边、hover 加到 3px）与**实心 outline**（焦点、错误、tab、按钮，2px、零模糊、offset 2–3px）。没有 3px 半透明彩色环，也没有用 blur 假冒的"柔光"。

### Edge & Outline Vocabulary

- **输入静止边**（`inset 0 0 0 2px var(--zoo-ink)`）：白底胶囊的边，用 inset 而不是 border，状态变化不改变布局尺寸。
- **输入 hover**（`inset 0 0 0 3px var(--zoo-ink)`）：边从 2px 加厚到 3px，仍然只是墨，没有新增图层、没有偏移。
- **输入聚焦**（`inset 0 0 0 2px var(--zoo-ink)` + `outline: 2px solid var(--zoo-ink); outline-offset: 2px`）：墨边保持 2px，外面再加一圈实心墨色外框；外框跟随胶囊圆角。
- **输入错误**（`inset 0 0 0 2px var(--zoo-ink)` + `outline: 2px solid var(--zoo-alert); outline-offset: 2px`）：墨边不动，外框换红；错误优先于聚焦，两者不会同时出现。
- **tab 焦点**（`outline: 2px solid var(--zoo-ink); outline-offset: 2px`）：与全局 `:focus-visible` 同一语言，不用彩色。
- **主按钮焦点**（`outline: 2px solid var(--zoo-ink); outline-offset: 3px`）：比输入外框再离远 1px，避开按钮自身 1px 边。
- **已退役**（`--ai-shadow-card` = `0 1px 3px rgba(16, 24, 40, 0.06)`）：旧卡片投影仍声明在 `tokens.css`，已无使用者，不属于本世界。

**为什么不是环**：2px 的 `outline` 是实心线，不是 `box-shadow` 扩散；它不会在边外制造渐隐，屏幕上看到的就是一条线。形状上像"第二圈边"，语义上仍是一层边界，所以不违反"不描双层边"——两层之间隔着 2–3px 的空隙。

### Motion

- 输入边从 2px 到 3px 的过渡继承 Element Plus 的 `--el-transition-box-shadow`：`box-shadow 0.2s cubic-bezier(0.645, 0.045, 0.355, 1)`；`prefers-reduced-motion: reduce` 下页面把它关成 `transition: none`。
- outline 的出现/消失没有过渡，是即时切换。
- 按钮的 hover/active 过渡沿用 EP 默认，页面没有自定义 motion；tab 的 active-bar 已被隐藏，没有滑动指示条。

### Named Rules

**The Edge-Not-Glow Rule（边不是光）.** 状态反馈只用实心边与零模糊 outline：2px / 3px 的墨边、2px 的红/墨外框。不许用 `box-shadow` 模糊扩散、`filter: drop-shadow`、渐变描边或半透明彩色环制造光晕。

**The Flat-By-Default Rule（默认平）.** 页面在静止与状态变化时都没有带偏移的投影；需要厚度时加粗墨边或加一圈 outline，不引入 elevation 阶梯。

## Shapes

两档形状，没有第三档：

- **胶囊**（`--zoo-cap: 999px`）：tab、输入框、主按钮、图例胶囊、错误提示胶囊——所有"能按、能填、能读成一枚标签"的东西都是完整胶囊。`--el-border-radius-base` 也指向它，所以 EP 的消息、按钮、输入默认就是胶囊。
- **方角**（`0`）：SVG 陆地的岸线、页面与列的边界、字标锁排（无容器）。页面里已经没有带直角的色块。
- **圆**（`50%`）：唯一的例外是错误提示前的 6px 红点。它是标点，不是形状档位，不要因此给控件加圆形变体。

其余形状语言：

- **线宽按职责分，不按"统一细线"**：海岸线与水洞 **1px**（`stroke-width: var(--zoo-line)` + `vector-effect: non-scaling-stroke`，视口怎么拉伸都保持 1px）；输入静止 **2px**、hover **3px**（都是 inset）；焦点/错误外框 **2px** 实心 outline；tab 与按钮 **1px** 边框。`--zoo-line` 现在是真值（1px），不再是"声明未用"。
- **代码里没有 2px 圆角**。方角常被口头写成「0/2px」，实查全部 `border-radius` 只有 `var(--zoo-cap)`、`0` 与红点的 `50%`；本文的"2px"一律指输入框边宽，不是圆角。
- **旧圆角不再漏**：`--ai-radius-sm: 8px` / `--ai-radius-md: 12px` 仍在 `tokens.css` 声明，但 `--el-border-radius-base` 已改指胶囊，EP 组件不会再取到 8px。
- **水洞**：宽屏 SVG 里 `<circle cx="500" cy="720" r="100">`，奶油填充 + 1px `--zoo-line` 墨边，画在陆地之后，形成"陆地中段透出水域"的洞；1440×900 下 ⌀200。洞的外接框右缘 600 与同高岸线（y=620/660/720 约 642/679/747）之间仍有 42–147px 绿地，所以洞是闭合的，不与海相通。
- **不描双层边**：同一条边只允许一层墨线；焦点用边外 2px 的 outline，不是把边框加粗成两层。

### Named Rules

**The Two-Shapes Rule（胶囊或方角）.** 自定义形状只有 999px 胶囊与 0 方角；不新增 2/4/8/12px 圆角，不给个别控件做"半胶囊"。6px 错误圆点是标点，不算第三档。

**The 2px-Ink-Edge Rule（输入框的墨边）.** 输入框静止边是 `inset 0 0 0 2px var(--zoo-ink)`，hover 加到 3px；焦点与错误都不许把这条墨边换成彩色，彩色只出现在外面的 2px outline 上。

## Components

### Buttons

页面只有一种按钮：整宽黄色胶囊主按钮（`.zoo__submit`，EP `el-button type="primary" size="large"`）。

- **Shape**：高 46px、整宽（100%）、`border-radius: 999px`、1px `--zoo-ink` 边框、`margin-top: 6px`。
- **Default**：底 `--zoo-sun`，字墨 15px/600、`0.02em` 字距，内边距 `0 18px`（EP 的 `size="large"` 本身是 40px 高，46px 是页面覆写）。
- **Hover / Active**：hover 底 `--zoo-sun-deep`，active 底 `--zoo-sun-press`；边框与文字始终墨色，不位移、不加投影。页面把 EP 的 `--el-button-bg-color` / `-border-color` / `-text-color` 与 `-hover-*` / `-active-*` 共九个变量钉在黄阶与墨上，避免落回 EP 的派生阶。
- **Focus**：`outline: 2px solid var(--zoo-ink); outline-offset: 3px`。
- **Loading**：用 EP 的 `:loading`（内联转圈 + 禁用），页面另用 `Submitting` 布尔防连点；没有自定义 loading 视觉。
- **Secondary / Ghost**：不存在。次操作是另一枚 tab，不是第二个按钮。

### Tabs

登录与注册是两枚胶囊 tab（`.zoo__tabs`，EP `el-tabs`），也是页面上唯一的"次操作"。

- **Shape**：高 34px、`display: inline-flex`、`align-items/justify-content: center`、`padding: 0 18px !important`、`border-radius: 999px`、1px `--zoo-ink` 边框、两枚之间 `margin-right: 8px`。
- **Default**：透明底、墨字 13px/500、行高 1；EP 的底部下划线（`nav-wrap::after`）与滑动指示条（`active-bar`）都关掉。
- **Active**：底 `--zoo-ink`、字 `--zoo-water`、字重 600——反色，不用黄，黄色只留给提交。
- **Hover**：未选中项 hover 时补 `background: rgba(44, 46, 42, 0.1)` 的墨色淡底。EP 自带的 `:hover { color: var(--el-color-primary) }` 会被页面的墨色文字规则盖掉，所以这个底是页面显式补回来的触觉反馈。
- **Focus**：`outline: 2px solid var(--zoo-ink); outline-offset: 2px`，与全局焦点同一语言（蓝环已不存在）。
- **组件级坑位（必记）**：EP 有一条 `.el-tabs--top > .el-tabs__header .el-tabs__item:nth-child(2) { padding-left: 0 }`（四个类/伪类，优先级 0,4,0），会专门把第二枚 tab 的左内边距清零，导致「登录 / 注册」一胖一瘦。页面的 scoped 规则只有 0,3,0，必须用 `!important` 才能压住它——删掉 `!important` 就会复现这个坑。
- **Header**：tab 列表下外边距 22px；整块上方 34px。

### Inputs / Fields

字段是白底胶囊，坐在绿地上（`.zoo__form`，EP `el-input size="large"` + `el-form-item label-position="top"`）。

- **Shape**：高 46px、`border-radius: 999px`、白底、`inset 0 0 0 2px var(--zoo-ink)` 的 2px 墨边、内边距 `0 18px`；输入文字 14px 墨色，占位符 14px `--zoo-ink-soft`。表单宽 380px，`el-form-item` 每项下 16px。
- **Label**：在输入框上方、13px/500 墨色、`padding-bottom: 6px`；EP 顶部布局另带 `margin-bottom: 8px` 与 22px 行高（页面把行高压到 1.4）。必填星号用 `hide-required-asterisk` 隐藏，必填性由校验文案表达。
- **Hover**：墨边从 `2px` 加厚到 `3px`（`inset 0 0 0 3px var(--zoo-ink)`），没有偏移阴影。
- **Focus**：墨边回到 `2px`，外圈 `outline: 2px solid var(--zoo-ink); outline-offset: 2px`；没有半透明环、没有变蓝。
- **Error**：`.el-form-item.is-error` 下保持 `2px` 墨边，外圈换 `outline: 2px solid var(--zoo-alert)`；错误文字见「错误提示胶囊」。
- **Disabled**：当前页面没有使用禁用态，没有可记录的样式。
- **密码框**：`show-password` 使用 EP 自带的可见性切换图标；这是页面上唯一的图标，来自组件库而非图标包。图标颜色取 EP 的 `--el-input-icon-color`（默认接 `--el-text-color-placeholder`，现在指向次级墨）。

### Chips（图例胶囊）

水域一侧的图例是两行「胶囊 + 说明」。桌面整块右靠、文字右对齐；窄屏改成绝对定位后左对齐。图例块宽 292px、两列 `72px 1fr`、列间隔 12px、`justify-items: start`——胶囊列定宽 72px，两行说明的起点才对得齐；说明列 `text-align: left`。桌面胶囊 x=1076–1147、说明从 1161 起；窄屏胶囊从 24 起、说明从 109 起。胶囊本身是纯标签，不可点。

- **Style**：宽 72px、高 30px（窄屏 24px）、`padding: 0`、`border-radius: 999px`、无边框、墨字 13px/600；珊瑚 `--zoo-coral` 是「简历」，蓝 `--zoo-sky` 是「面试」。
- **State**：静态标签，没有 hover/active/focus，也没有选中态；不要把它们做成筛选器或按钮。
- **说明文字**：胶囊右侧 12px 间隔，12px `--zoo-ink-soft`（在水域上 5.52:1），左对齐；桌面在块内换行时也从同一列起。图例两行之间间距 16px（窄屏 8px）。
- **小标题**：12px `--zoo-ink-soft`、`letter-spacing: 0.08em`，文案是「导览图 · 园区里有这些」。
- **淡紫**：`--zoo-lilac` 已备好但暂无第三行图例。

### Containers / Page Shell

没有卡片容器。页面壳就是那张图：

- **`.zoo`**：`position: relative`、`min-height: 100dvh`、`overflow: hidden`、底 `--zoo-water`、字 `--zoo-ink`、字体 `--ai-font-sans`。
- **`.zoo__grid`**：`grid-template-columns: minmax(0, 56%) minmax(0, 44%)`、`min-height: 100dvh`；没有外框、没有背景、没有投影。
- **陆地列**：`max-width: 520px`、`padding: 56px 0 56px 72px`、`align-self: start`。
- **水域列（桌面）**：flex 纵向、`align-items: flex-end`、`justify-content: center`、`text-align: right`、`gap: 22px`、`padding: 56px 72px 96px 32px`；图例块 292px 定宽、两列 72px/1fr。
- **页脚**：桌面绝对定位 `right: 72px; bottom: 32px`；窄屏 static、`margin: 4px 0 0`、左对齐（因为窄屏水域整体左对齐）。
- **窄屏（≤899px）**：`.zoo__grid` 改 flex 列；`.zoo__plot` padding `36px 24px 32px`；`.zoo__water` 绝对定位 `top: max(84.1dvh, 620px)`、左右 0、`padding: 6px 24px 12px`、`gap: 8px`、`align-items: flex-start`、`text-align: left`；图例块宽 100%、胶囊高 24。
- **水洞**：只在宽屏 SVG 里，不是 DOM 容器，不要用 div 复刻。

### Signature: 海岸线（Coastline）

这是全系统唯一的图形资产，也是水陆分界的唯一出口。两条 path，互斥显示：

```html
<!-- 宽屏 -->
<svg class="zoo__coast zoo__coast--wide" viewBox="0 0 1440 900" preserveAspectRatio="none" aria-hidden="true" focusable="false">
  <path class="zoo__land" d="M0 0 H520 C470 120 560 180 600 260 C650 350 640 430 620 500 C590 600 680 660 740 710 C780 745 760 850 700 900 H0 Z" />
  <circle class="zoo__pond" cx="500" cy="720" r="100" />
</svg>

<!-- 窄屏 -->
<svg class="zoo__coast zoo__coast--narrow" viewBox="0 0 390 844" preserveAspectRatio="none" aria-hidden="true" focusable="false">
  <path class="zoo__land" d="M0 0 H390 V695 C320 705 250 690 180 700 C110 710 50 700 0 692 Z" />
</svg>
```

- **样式**：`.zoo__coast` 高度 `100dvh`；`.zoo__land { fill: var(--zoo-land); stroke: var(--zoo-ink); stroke-width: var(--zoo-line); vector-effect: non-scaling-stroke; }`；`.zoo__pond { fill: var(--zoo-water); stroke: var(--zoo-ink); stroke-width: var(--zoo-line); vector-effect: non-scaling-stroke; }`。
- **显示切换**：`max-width: 899px` 时 `.zoo__coast--wide` 隐藏、`.zoo__coast--narrow` 显示；宽屏时窄屏那条 `display: none`。
- **协议**：`preserveAspectRatio="none"` 是有意的——path 只保证拓扑，比例交给视口；因此海岸线的摆幅随宽高比变化，关键内容不要压在 36%–53% 的摆动带里。
- **水洞**：宽屏 `circle cx=500 cy=720 r=100` 画在陆地之后，1440×900 下 ⌀200；外接框右缘 600，同高岸线 y=620/660/720 约 642/679/747，绿地净距 42–147px，洞是闭合的。注册 CTA 底缘 610、右缘约 450，洞的上缘 620——两者水平投影重叠，但垂直净距 10px，不相交（这是当前最紧的一处，改表单高度或洞位置时要重新量）。`cx/cy` 是 SVG 用户单位，非 16:10 视口里洞会拉成椭圆——这是 `preserveAspectRatio="none"` 的必然结果，不要改成 `<ellipse>` 或加 clip。
- **装饰豁免**：这是页面上唯一的装饰图形，但它承担结构职责（分界），不是贴纸。

### Error Note（错误提示胶囊）

- **容器**：`.el-form-item__error` 被改成 `position: static` 的 inline-flex，参与排版、把下面的字段推开，不浮层覆盖。
- **样式**：奶油底 `--zoo-water`、`border-radius: 999px`、`padding: 3px 10px`、`margin-top: 8px`、12px 红字 `--zoo-alert`、行高 1.4、`gap: 6px`；`::before` 是 6px 红色圆点（`border-radius: 50%`）。
- **为什么是奶油**：红压绿地 3.04:1，不合格；压奶油 4.81:1，合格。这枚胶囊不是装饰，是让红字达标的底。
- **文案**：只来自 `src/constants/error-map.ts` 与表单规则，页面不新造措辞。

### Wordmark Lockup（字标锁排）

显示声部只有这一块，没有 logo 图、没有图标字标：

- **结构**：`<h1 class="zoo__title">` 内两个 span：`.zoo__title-latin`（`AI`）与 `.zoo__title-cn`（`面试系统`）；`display: flex`、`flex-direction: column`、`gap: 2px`、`margin: 0`。
- **字体**：两个 span 都写 `font-family: 'Zoo Display', var(--ai-font-sans)`，`font-weight: 700`；子集只覆盖这 6 个字形，缺字会静默回退到系统栈。
- **上档**：`AI` 46px/700、`line-height: 0.95`、`letter-spacing: -0.01em`。
- **下档**：`面试系统` 24px/700、`line-height: 1.2`、`letter-spacing: 0.3em`；宽字距是识别点，不是装饰。
- **整块高度**：约 74px（43.7 + 2 + 28.8），从陆地 56px 处开始。
- **窄屏**：字号不缩小——两个 span 都是固定字号，窄屏没有标题断点；水域绝对定位的位置才是窄屏要复核的量。
- **禁止**：不要把它做成图片、SVG 字标或第二套展示字体；锁排本身就是字标，显示字只来自 Zoo Display 子集。

### Element Plus 接缝（现状）

- **引入顺序**：`element-plus/dist/index.css` → `styles/tokens.css` → `styles/base.css`；EP 基础样式先落地，令牌后覆盖 EP 变量，`base.css` 最后铺浏览器表面。
- **全量引入**：`app.use(ElementPlus)` + 全量 CSS，没有按需插件；页面用 `el-tabs / el-form / el-form-item / el-input / el-button`，消息用 `ElMessage`。
- **全局映射已换皮**：`--el-color-primary` = 墨、`--el-color-danger` = 错误红、文字与边框都指向墨/次级墨、`--el-border-radius-base` = 胶囊；EP 的 hover、焦点、消息、密码图标不会再漏蓝或漏 8px 圆角。
- **页面钉住的 EP 变量**：主按钮的九个 `--el-button-*` 全部钉在 `--zoo-sun*` 与 `--zoo-ink`；表单错误只改 `.el-form-item__error` 与 `.el-input__wrapper` 的 box-shadow/outline，不碰 EP 主题变量。
- **仍需注意**：EP 的 tab 第二项 padding 规则需要页面 `!important` 压住（见 Tabs）；EP 的 form label 自带 8px 下边距与 22px 行高，页面只改行高与内边距，做新表单时要按这个盒模型算高度。
- **未映射 success / warning / info**：`ElMessage.success`（注册成功、欢迎回来）仍用 EP 默认的绿色系与浅绿底，没有接到 `--zoo-land` 或任何 zoo 令牌。这是本世界目前最明显的一条接缝；要收口就把 `--el-color-success` 等映射到墨或另立一个语义色，但当前没有做。

### Named Rules

**The Pinned-EP-Variables Rule（EP 变量钉在黄与墨）.** 每出现一个 EP 主按钮，必须把 `--el-button-bg-color` / `-border-color` / `-text-color` 及 `-hover-*` / `-active-*` 钉到 `--zoo-sun*` 与 `--zoo-ink`；否则 EP 的派生阶会接手按钮状态。

**The Cream-Error-Capsule Rule（错误文字必须坐在奶油胶囊里）.** 错误提示永远是"奶油胶囊 + 红字 + 6px 红点 + `position: static`"；不许裸红字直接压在绿地上，也不许改成 toast 或浮层。

**The Tab-Padding Pin Rule（第二枚 tab 必须钉住 padding）.** tab 的 `padding: 0 18px` 必须保留 `!important`：EP 的 `nth-child(2)` 规则会清零第二枚 tab 的左内边距，没有 `!important` 就一胖一瘦。

## Do's and Don'ts

### Do:

- **Do** 用 `--zoo-*` 令牌；页面里仅有的裸值是输入框白底 `#ffffff` 与 tab hover 的 `rgba(44,46,42,.1)`，新页面优先补令牌再写。
- **Do** 把水陆分界交给那条 SVG path：宽屏 `viewBox 1440 900`、窄屏 `viewBox 390 844`，`preserveAspectRatio="none"`，`stroke-width: var(--zoo-line)`（1px）+ `vector-effect: non-scaling-stroke`。
- **Do** 控制件一律 999px 胶囊：tab 34px 高、输入 46px 高、按钮 46px 高、图例胶囊 30px 高；边界一律墨色。
- **Do** 主按钮整宽、46px、黄底墨字，并把 EP 的九个 `--el-button-*` 变量钉死；hover 深黄、active 按下黄，不位移。
- **Do** 输入框用 `inset 2px` 墨边；hover 加厚到 `3px`；聚焦/错误时墨边保持 2px，外面加 `outline: 2px solid` 的墨/红外框、`offset: 2px`。
- **Do** 错误文字放进奶油胶囊（`3px 10px`、12px、6px 红点、`position: static`），并把错误文案的出口留给 `error-map.ts` 与表单规则。
- **Do** 绿地上的字只用 `--zoo-ink`；水域说明与占位符才用 `--zoo-ink-soft`。
- **Do** 水域块桌面右对齐（`align-items: flex-end`、`text-align: right`、块宽 292px 两列 72px/1fr、页脚 `right: 72px`）；窄屏由绝对定位接管、左对齐。
- **Do** 字标两档都用 `'Zoo Display', var(--ai-font-sans)`、700（`AI` 46/700 + `面试系统` 24/700/0.3em）；正文与控件保持系统栈，不写 Zoo Display。
- **Do** 窄屏把海岸线换成自上而下那条 path，水域绝对定位在 `top: max(84.1dvh, 620px)`；矮视口靠页面滚动，不裁切水域。

### Don't:

- **Don't** 用黄色以外的颜色做提交按钮，也不要把珊瑚 / 蓝 / 淡紫放到按钮、链接、边框或 focus 上。
- **Don't** 画第二条海岸线：不用 border、背景色块、伪元素、卡片边缘或栅格线伪造水陆分界；分界只认那条 path。
- **Don't** 用卡片、投影、渐变、玻璃或发光来"做出层次"；层次靠字号、字重、墨边与留白。
- **Don't** 用 `box-shadow` 做状态：不许有带偏移的投影、模糊光晕或半透明彩色环；状态只用 inset 墨边与 2px 实心 outline。
- **Don't** 加第三档圆角，也不要写 8/12px 圆角（代码里没有 2px 圆角；`--ai-radius-*` 已不接 EP）。
- **Don't** 让红字直接压在绿地上（3.04:1），也不要为了达标而把绿地调浅或把红字加粗——换奶油底。
- **Don't** 把 `--zoo-ink-soft` 放到陆地上（3.49:1）；需要弱化就降字号。
- **Don't** 引入图片、logo 图、图标包或第二套字体；唯一 webfont 是 3820 字节的 Zoo Display 子集，不能加字形、加字重/斜体、换成 CDN 字体或用到正文上。
- **Don't** 做深色模式或 `prefers-color-scheme` 分支；`color-scheme` 固定为 light。
- **Don't** 把导览图当贴纸：不要在同一屏里既保留居中卡片又贴一条海岸线；要么整页是图，要么不是这个世界。
