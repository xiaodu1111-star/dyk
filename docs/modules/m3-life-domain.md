# M3 · 生活域（习惯打卡 + 快捷记录）

> 状态：dev · 并行窗口 3（后端 8083 / 前端 5177）
> **前置必读：`docs/dev-workflow.md`（流程、共享资产清单、git 规则、环境速查）**
> Flyway 号段：**V50 – V59** · 错误码段：**14000 – 14099**
> **依赖**：M0 指标引擎（`sys_metric_def` / `sys_metric_record` + `MetricService`）已就位；**不建业务专表**（`life_habit` 等表不用，这是总设计的落地验证）

## 1. 概览

生活域建立节奏感。习惯 = 指标引擎的一条定义，打卡 = 一条记录——**新增习惯不改表不改代码**，这是三大引擎原则的第一次实战。用户故事见 `docs/requirements/p1-requirements.md` §2.C（C1–C5）。

## 2. 范围内 / 范围外

**做**：习惯定义（打卡型/计数型）、每日打卡、streak 计算、快捷记录条（解析 → 预填 → 确认）、生活页今日视图。
**不做**：life_goal / life_diary（P2）、提醒通知、习惯统计图表（P4）、单位换算、多时段打卡（一天一记）。

## 3. 数据库

**零建表**。习惯存 `sys_metric_def`（M0 的 V3 已建），映射约定：

| 指标字段 | 打卡型 | 计数型 |
|---|---|---|
| `dimension` | `life` | `life` |
| `value_type` | `bool` | `number` |
| `unit` | 空 | 如 `杯` / `公里` |
| `agg_type` | `last` | `sum` |
| `target_value` | 1 | 如 8 |
| `code` | `life_habit_<拼音或英文>` | 同左 |

打卡记录存 `sys_metric_record`：`record_date`=归属日（PeriodUtil 自然日口径）、`value_num`=1（打卡型）或增量值（计数型）。

> 本模块号段 V50–V59 预留做种子数据或字段微调；**若 M0 未建引擎表则本模块阻塞**（先确认 M0 完成）。

## 4. 后端

**包**：`com.xiaodu.personalos.life`。**错误码**（`LifeErrorCode` 枚举）：`14001 HABIT_NOT_FOUND` · `14002 ALREADY_CHECKED` · `14003 PARSE_FAILED` · `14004 PARAM_INVALID`

**接口契约**：

| 方法 | 路径 | 入参 | 说明 |
|---|---|---|---|
| POST | `/api/life/habits` | `{ name*, type: 'checkin'\|'count', unit?, targetValue? }` | 建 `sys_metric_def`；code 自动生成且查重 |
| GET | `/api/life/habits` | — | 列表 + 今日完成状态（done/value/目标） |
| PUT | `/api/life/habits/{id}` | 同创建 | 改名/目标 |
| DELETE | `/api/life/habits/{id}` | — | 逻辑删 def |
| POST | `/api/life/habits/{id}/checkin` | `{ value? }`（计数型增量） | 打卡型幂等：**今日已打 → 14002**；写 record + 活动流 |
| GET | `/api/life/habits/{id}/streak` | — | 连续天数（详见下） |
| POST | `/api/life/quick-record` | `{ text }` | 解析 `{ metricId, metricName, value, date }` **只解析不落库**；解析不了 → 14003 |
| POST | `/api/life/records` | `{ metricId, value, date? }` | 确认后落库（快捷记录与手动的统一入口） |

**streak 算法**（自然日 00:00 口径，用 `PeriodUtil`）：
- 打卡型：从今天（或昨天，若今天未打）向前逐日查 `value_num >= 1`，连续计数
- 计数型：`SUM(value) > 0` 视为当日有效
- SQL 用 `record_date` 分组，**一次查询取最近 90 天**再内存计算，别 N 次单查

**快捷记录解析规则（P1 最简版，写死在 service 里）**：
- 格式：`<指标名前缀> [数字]`，如「跑步 5」「喝 8」
- 匹配：`sys_metric_def` 中 `name` 以输入首词开头（dimension=life，可扩 common）取第一个命中
- 无数字 → 计数型默认 1 / 打卡型忽略
- **永远返回预填结果让前端确认，不直接写库**（总设计红线）

**活动流**：打卡成功 → `ActivityLogService.log(dimension="life", bizType="checkin", title=习惯名, occurredAt=now)`。

## 5. 前端

**只动**：`src/views/life/`（替换占位 `index.vue`）、`src/api/life.ts`、`src/stores/lifeStore.ts`（如需）。

**页面**：
1. **今日打卡**：习惯卡横排/网格；打卡型 = 大圆形打卡按钮（打完变实心）；计数型 = 显示 `3/8` + 「+1」；streak 数字角标（🔥 不许用 emoji，用自绘 SVG 火焰或红点数字）
2. **习惯管理**：增删改（名称/类型/单位/目标）；新建表单极简
3. **快捷记录条**：顶部常驻输入框，输入「跑步 5」→ 弹出预填确认卡（指标名/数值/日期）→ 确认落库

**样式**：iOS 26 玻璃，同 M1。打卡完成的轻反馈（缩放/波纹）注意 `prefers-reduced-motion`。

### 5.1 原型视觉规格（小杜已过审，实现须对齐）

原型：M3 生活页（2026-10-01 过审）。关键视觉决策：

- **打卡型**：习惯卡中央大圆形打卡钮（直径约 72px 玻璃描边圆），未打卡空心，打卡后实心填充 + 缩放波纹反馈；streak 角标在卡片右上（自绘 SVG 火焰 + 数字，禁 emoji）
- **计数型**：中央大数字 `3/8`（数值大、目标小），下方细进度条（已计比例），「+1」玻璃按钮；满额后数字变主色 + 对勾角标
- **快捷记录预填确认卡**：顶部输入「跑步 5」后弹出浮层确认卡，展示解析出的指标名/数值/日期三字段（可改），确认落库、取消关闭；卡片带玻璃模糊背景
- **今日网格**：习惯卡横排自适应网格（390px 下单列），卡片间距紧凑，整页无滚动或少滚动为佳

## 6. 文件边界（白名单）

```
允许新增/修改：
  personal-os-server/src/main/java/com/xiaodu/personalos/life/**
  personal-os-server/src/main/resources/db/migration/V5[0-9]__*.sql（仅在需要种子/微调时）
  personal-os-server/src/test/java/com/xiaodu/personalos/life/**
  personal-os-web/src/views/life/**
  personal-os-web/src/api/life.ts
  personal-os-web/src/stores/lifeStore.ts
禁止：见 dev-workflow.md §3.2；引擎表结构一律不改（要改 = 提变更申请给 RIce）
```

## 7. 测试与验收

**测试**：
- 建打卡型习惯 → `sys_metric_def` 落一条（dimension=life, value_type=bool）；再建一个 → code 不重复
- 打卡 → record 落 `record_date`=今天；**重复打卡 → 14002**
- 计数型 +1 两次 → 今日 SUM=2
- streak：造 3 天记录（昨天/前天断档）→ 断档处停止
- 快捷记录「跑步 5」→ 预填（metricId=跑步, value=5）；「乱码 xyz」→ 14003；**确认后才落库**
- 打卡 → 活动流落一条 life/checkin

**验收清单**：
- [ ] 新建一个习惯不改任何表结构、不发版（指标引擎的意义）
- [ ] 打卡型点一下完成、重复点被拒
- [ ] 计数型累计到目标（3/8）
- [ ] streak 数字正确
- [ ] 快捷记录「跑步 5」→ 确认 → 落库
- [ ] 明暗主题 + 390px 移动端无破版
- [ ] `git diff --name-only` 无越界文件
