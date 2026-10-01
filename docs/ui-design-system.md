# Personal OS · UI 设计基调（iOS 26 Liquid Glass 液态玻璃）

> 版本：v1.0 · 2026-10-01 · 归属：Personal OS · 前端（personal-os-web）
> 目标读者：后续所有页面开发者。本文是**唯一视觉基准**，新增页面请一律引用这里的 token 与玻璃配方，不要另起一套。
> 代码位置：`personal-os-web/src/styles/`（`tokens.scss` / `glass.scss` / `element-override.scss` / `index.scss`），主题逻辑 `src/composables/useTheme.ts`。

---

## 0. 一句话原则

**液态玻璃不是"半透明卡片"，而是「背景色 → 模糊取色 → 顶部镜面高光 → 边缘折射」四件事同时成立。**
少了高光与边缘，玻璃就退化成灰色蒙版；少了彩色壁纸，玻璃就无从"透"。

---

## 1. 设计基调（Design Tone）

| 维度 | 基调 |
|---|---|
| 材质 | 磨砂玻璃 + 镜面高光 + 边缘折射，三层叠加 |
| 光感 | 光源恒定来自**上方**（高光在顶部、暗边在底部） |
| 形态 | 大圆角、胶囊化按钮/输入框，连续曲率 |
| 色彩 | 浅色以冷调白玻璃为主，强调色用 iOS 系统蓝 `#007AFF`；深色用中性深灰玻璃 + `#0A84FF` |
| 留白 | 卡片内边距 26–40px，元素间距 14–16px 起 |
| 动效 | 短促、带弹性（spring），位移量小（≤12px），全部尊重 `prefers-reduced-motion` |

---

## 2. 字体栈（Font Stack）

```css
--font-sans: -apple-system, BlinkMacSystemFont, "SF Pro Text", "SF Pro Display",
             "PingFang SC", "Helvetica Neue", "Microsoft YaHei", sans-serif;
```

- 全部使用系统字体，**不引任何外部字体 CDN**（要离线可用）。
- 中文回落：`PingFang SC`（macOS/iOS） → `Microsoft YaHei`（Windows）。
- 常用字号：页面标题 28px/600，卡片标题 17px/600，正文 15px/400，辅助 13–14px，脚注 11–12px。

---

## 3. 颜色 Token

### 3.1 浅色（默认，`:root`）

| Token | 值 | 用途 |
|---|---|---|
| `--accent` | `#007AFF` | 主色（iOS 系统蓝），替换旧 `#185FA5` |
| `--accent-press` | `#0062CC` | 主色按下/渐变末端 |
| `--danger` | `#FF3B30` | 错误、危险 |
| `--success` | `#34C759` | 成功 |
| `--text-primary` | `rgba(0,0,0,0.92)` | 主要文字 |
| `--text-secondary` | `rgba(60,60,67,0.62)` | 次要文字 |
| `--text-tertiary` | `rgba(60,60,67,0.34)` | 占位符 / 图标 / 脚注 |
| `--separator` | `rgba(60,60,67,0.14)` | 分隔线、开关底 |

### 3.2 深色（`html[data-theme="dark"]`）

| Token | 值 |
|---|---|
| `--accent` | `#0A84FF` |
| `--accent-press` | `#0A6FD6` |
| `--danger` | `#FF453A` |
| `--success` | `#30D158` |
| `--text-primary` | `rgba(255,255,255,0.94)` |
| `--text-secondary` | `rgba(235,235,245,0.62)` |
| `--text-tertiary` | `rgba(235,235,245,0.34)` |
| `--separator` | `rgba(84,84,88,0.5)` |

> 深色下**禁止**浅底浅字：所有文字必须取 `--text-*`，不要写死颜色。

---

## 4. 玻璃配方（核心）

### 4.1 参数

| Token | 浅色 | 深色 |
|---|---|---|
| `--glass-blur` | `24px` | 同上 |
| `--glass-saturate` | `180%` | 同上 |
| `--glass-bg-thin` | `rgba(255,255,255,0.38)` | `rgba(40,40,44,0.42)` |
| `--glass-bg` | `rgba(255,255,255,0.58)` | `rgba(38,38,42,0.58)` |
| `--glass-bg-thick` | `rgba(255,255,255,0.74)` | `rgba(38,38,42,0.74)` |
| `--glass-border` | `rgba(255,255,255,0.62)` | `rgba(255,255,255,0.14)` |
| `--glass-shadow` | `0 10px 40px rgba(17,24,39,.14), 0 2px 8px rgba(17,24,39,.06)` | `0 10px 40px rgba(0,0,0,.5), 0 2px 8px rgba(0,0,0,.3)` |
| `--glass-inner` | `inset 0 1px 0 rgba(255,255,255,0.85)` | `inset 0 1px 0 rgba(255,255,255,0.10)` |

### 4.2 三层结构（必须齐全）

| 层 | 实现 | 作用 |
|---|---|---|
| ① 取色 | `backdrop-filter: blur(24px) saturate(180%)` + `-webkit-` 前缀 | 让背景色透上来 |
| ② 镜面高光 | `::before` 的 180° 白色渐变（顶 0.52 → 34% 处 0.10 → 62% 处 0） | **液态玻璃的灵魂**（顶部反光） |
| ③ 边缘折射 | `::after` 的 `inset 0 0 0 0.5px` 亮边 + 底部极淡暗边 | 制造"玻璃有厚度" |

附加：`position: relative; isolation: isolate; border: 1px solid var(--glass-border); box-shadow: var(--glass-shadow), var(--glass-inner);`

### 4.3 用法

```html
<div class="glass">标准层（默认）</div>
<div class="glass glass--thin">薄层：输入框、胶囊、图标按钮</div>
<div class="glass glass--thick">厚层：主卡片、导航条、弹窗</div>
```

SCSS 侧也可直接引 mixin：

```scss
@use '@/styles/glass' as *;

.my-panel {
  @include glass-surface(var(--glass-bg-thick));
}
```

> 深色下 `--glass-hi-top/--glass-hi-mid` 会被压暗（0.16 / 0.05），避免"奶白糊顶"；亮色严格采用设计基准值。

### 4.4 厚度选型

| 层级 | 场景 | 例子 |
|---|---|---|
| thin | 元素级、面积小 | 输入框、领域胶囊、圆形图标按钮、徽标容器 |
| 标准 | 中等容器 | 通用卡片 |
| thick | 大卡片 / 悬浮条 / 模态 | 登录卡、导航条、问候卡、MessageBox |

---

## 5. 圆角阶梯

| Token | 值 | 用途 |
|---|---|---|
| `--r-sm` | `12px` | 小控件 |
| `--r-md` | `18px` | 内联提示条 |
| `--r-lg` | `26px` | 导航条、次级卡片 |
| `--r-xl` | `34px` | 主卡片（登录卡、问候卡） |
| `--r-pill` | `999px` | 按钮、输入框、开关、圆形图标按钮 |

---

## 6. 运动曲线

| Token | 值 | 用途 |
|---|---|---|
| `--spring` | `cubic-bezier(0.22, 1, 0.36, 1)` | 默认缓动（出场快、收尾缓） |
| `--dur-fast` | `160ms` | 按压反馈 |
| `--dur` | `260ms` | 颜色/边框/显隐 |
| `--dur-slow` | `420ms` | 进场、布局变化 |

约定：

1. 所有可点元素按压：`:active { transform: scale(0.97) }`，`transition: transform var(--dur-fast) var(--spring)`。
2. 进场：`translateY(12px)` + `opacity 0 → 1`，用 `--dur-slow`。
3. **所有动效必须包在 `@media (prefers-reduced-motion: no-preference)` 内**；`reduce` 下静止。
4. 加载 spinner 在 `reduce` 下不旋转（静态环 + "登录中…" 文字仍能表达状态）。

---

## 7. 壁纸（Liquid Backdrop）

没有彩色背景，玻璃就是灰的。壁纸用**纯 CSS 多层 `radial-gradient` mesh**（非图片）。

| 主题 | 底色 | 色斑 |
|---|---|---|
| 浅色 | `#EEF3FA` | `#A8C7FF`（淡蓝）、`#E0C3FC`（淡紫）、`#B5F2EA`（淡青）、`#FFD6E8`（淡粉） |
| 深色 | `#0B1020` | `#1B2A6B`、`#3A1B5C`、`#0E3A3A`、`#2A1B4A` |

用法：布局根容器里放一个 `<div class="wallpaper" aria-hidden="true" />`，内容容器设 `position: relative; z-index: 1`。

```html
<div class="layout">
  <div class="wallpaper" aria-hidden="true"></div>
  <main class="layout__content"> ... </main>
</div>
```

可选的极慢漂浮（40s 循环 `transform`），`prefers-reduced-motion` 下静止。

---

## 8. 主题切换

- 模式：`auto → light → dark` 循环，存 `localStorage['theme']`。
- 生效值写到 `<html data-theme="light|dark">`（`auto` 时读 `prefers-color-scheme` 并监听其变化）。
- **防闪白**：`index.html` 内联脚本先套一次，`main.ts` 里 `initTheme()` 再做权威初始化（挂载前完成）。
- 切换按钮：`src/components/ThemeToggle.vue`（36px 圆形玻璃按钮，图标随模式变化）。

```ts
import { initTheme, useTheme } from '@/composables/useTheme'

initTheme()                    // main.ts，挂载前调用
const { mode, cycle } = useTheme()   // 组件内
```

---

## 9. 组件规范

| 组件 | 规格 |
|---|---|
| 主按钮 | 高 52px，胶囊，accent 实心 + 顶部白高光渐变（`#3D9BFF → accent → accent-press`），文字 17px/600 白，`shadow: 0 8px 20px rgba(0,122,255,.3)`；`disabled` 时 `opacity .7` + `not-allowed` |
| 输入框 | 高 52px，胶囊，`glass--thin`，左侧 20px 线性图标 `--text-tertiary`，placeholder 同色；**focus**：`border-color: accent` + `box-shadow: 0 0 0 4px rgba(0,122,255,.18)` |
| 开关 | 44×26 胶囊，`aria-checked` 驱动；关闭态 `--separator`，开启态 `--accent`；旋钮 20px 白，`translateX(18px)`，spring 过渡 |
| 图标按钮 | 36px 圆形，`glass--thin` |
| 头像 | 40px 圆形，`linear-gradient(135deg, #4AA3FF, accent, #0062CC)`，首字白色 17px/600 |
| 内联提示条 | `glass`（或浅红底）+ `--r-md`，错误用 `rgba(255,59,48,.12)` 底 + `--danger` 字；`role="alert"` + `aria-live` |
| 导航条 | `glass--thick`，`--r-lg`，sticky `top:16px`，左右外边距 16px，高 64px |

---

## 10. 无障碍基线

1. 输入框必须有 `<label>`（可 `.sr-only` 视觉隐藏）+ `autocomplete`；校验失败加 `aria-invalid`。
2. 错误信息容器 `role="alert"` / `aria-live="assertive"`，从上方滑入。
3. 焦点**必须可见**：全局 `:focus-visible { outline: 2px solid var(--accent); outline-offset: 2px }`；输入框的焦点环由外层容器 `:focus-within` 提供（避免双环）。
4. 文字对比度达标（`--text-primary` 在玻璃底上 ≥ 4.5:1 量级）。
5. 所有动效尊重 `prefers-reduced-motion`。
6. 纯装饰元素 `aria-hidden="true"`；禁用态用 `aria-disabled` + 视觉弱化（`opacity .55; cursor: not-allowed`）。

---

## 11. 响应式

| 断点 | 规则 |
|---|---|
| ≤ 480px | 卡片宽度 `calc(100vw - 24px)`、内边距收到 24px；导航条贴边（`margin: 12px`）、`top: 12px`；主标题降到 24px |

---

## 12. Element Plus 处理

- **保留依赖**（后续管理页要用），登录/主页**不使用其默认视觉**。
- `element-override.scss` 把 `--el-color-primary` 等对齐 `--accent`，并把 `ElMessage` / `ElMessageBox` / `.el-overlay` 玻璃化。
- 判断某组件难以调到玻璃风格时，**直接用原生元素实现**，不要硬凑。

---

## 13. 新增页面的最小清单

1. 布局根节点放 `.wallpaper`，内容容器 `position: relative; z-index: 1`。
2. 容器用 `.glass` / `.glass--thin` / `.glass--thick`，圆角取阶梯 token。
3. 颜色只用 `--text-*` / `--accent` / `--glass-*`，禁止写死色值。
4. 可点元素加按压反馈并包 `no-preference`。
5. 表单加 label / `aria-invalid` / 错误 `aria-live`。
6. 深色模式自测一遍（文字清晰、无浅底浅字）。
