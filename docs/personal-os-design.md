# Personal OS · 个人操作系统 设计方案

> 技术栈：Vue 3 + TypeScript + Vite / Spring Boot 3 + MyBatis-Plus / MySQL 8 + Redis
> 定位：单用户自托管的「个人操作系统」，把工作、学习、运动、理财、生活收敛到一个入口。
> 版本：v1.0 设计稿 · 2026-10-01

---

## 0. 先定基调：不做「又一个待办清单」

市面上个人管理工具的失败原因几乎一致：**记录很爽，复盘为零**。用户记了 3 周就弃坑，因为数据没有回流成决策。

本系统的第一性目标是跑通这个闭环：

```
低摩擦记录  →  自动度量  →  周期复盘  →  调整行为
   Log           Measure        Review        Adjust
     ↑                                          │
     └──────────────────────────────────────────┘
```

三条不可妥协的设计原则：

| 原则 | 含义 | 反面教材 |
|---|---|---|
| **记录 ≤15 秒** | 任何一条记录必须能在 15 秒内完成，否则不会坚持 | 填 12 个字段的记账表单 |
| **指标可配置** | 新增追踪项不改表、不改代码，配一条数据即可 | 每加个习惯就要发版 |
| **复盘自动化** | 周报/月报由系统生成，人只做批注 | 手动导 Excel 统计 |

**差异化模块（本系统真正的价值点）**：工作域的 **SOP / Skill 库**。
大多数开源项目只做「任务管理」，但你的核心诉求是「沉淀工作流程、建立自己的工作 skill」——
任务是一次性的，SOP 是复利的。系统要把「重复做过 3 次的事」自动提示你沉淀成 SOP，并记录版本演进与使用频次。

---

## 1. 开源参考与取舍

调研了 6 个同赛道项目，取长补短：

| 项目 | 技术栈 | 值得抄的点 | 不采用的原因 |
|---|---|---|---|
| [MSabbirHossen/Life-OS](https://github.com/MSabbirHossen/Life-OS) | React+Express+Mongo | **统一 Dashboard 聚合模型**、时区安全的 streak 计算、12 周热力图、全量 JSON 导出 | Mongo 无强 Schema，长期演进易失控 |
| [zxc7563598/life-board](https://github.com/zxc7563598/life-board) | Vue3+PHP Webman | **模块化卡片式首屏**（可拖拽增删）、邮件自动解析支付宝/微信账单 | 后端非 Java 栈 |
| [afifrohul/personal-life-tracker](https://github.com/afifrohul/personal-life-tracker) | Laravel+Vue | **Daily Summary 单页聚合**当天全部维度 | 无指标自定义能力 |
| [djianxxd/PMS](https://github.com/djianxxd/PMS) | Go+MySQL | 成就/徽章系统做正反馈、多用户数据隔离 | 无 SOP 概念 |
| [NekoDreamSensei/TodoList-Manager](https://github.com/NekoDreamSensei/TodoList-Manager) | Vue3+Spring Boot+JPA | **专题→任务→清单三层进度模型**、多视图（卡片/层级/图表） | JPA 而非 MyBatis；无跨域统计 |
| [txxxxz/task-management](http://github.org/txxxxz/task-management) | Vue3+Spring Boot+MyBatis-Plus | **技术栈与本项目一致，可直接参考工程骨架**（含看板、甘特图、ECharts） | 只覆盖任务域 |

**结论**：借鉴 Life-OS 的聚合思路 + life-board 的模块化卡片 + TodoList-Manager 的多视图，技术骨架直接对标 `txxxxz/task-management`，但**架构上做一层抽象升级**（见第 3 节），避免陷入「每加一个需求就加一张表」的泥潭。

---

## 2. 总体架构

```
┌─────────────────────────────────────────────────────────────┐
│  前端  Vue 3 + TS + Vite                                     │
│  ┌──────────┐ ┌──────────────────────────────────────────┐  │
│  │ 驾驶舱    │ │ 工作 / 学习 / 运动 / 理财 / 生活  六大域   │  │
│  └──────────┘ └──────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ 通用组件：热力图 · 雷达图 · 时间轴 · 指标卡片 · 快捷记录 │   │
│  └──────────────────────────────────────────────────────┘   │
└───────────────────────────┬─────────────────────────────────┘
                            │ REST / JSON  (统一 Result 包装 + Sa-Token 鉴权)
┌───────────────────────────▼─────────────────────────────────┐
│  后端  Spring Boot 3 + MyBatis-Plus                          │
│  ┌────────────────────────────────────────────────────────┐ │
│  │ Controller  →  Service  →  Mapper  (严格三段式)          │ │
│  └────────────────────────────────────────────────────────┘ │
│  ┌────────────────────────────────────────────────────────┐ │
│  │ ★ 通用引擎层 (system 域)  ← 全系统复用                    │ │
│  │   标签引擎 · 指标引擎 · 活动流引擎 · 复盘引擎 · 附件引擎    │ │
│  └────────────────────────────────────────────────────────┘ │
│  ┌──────────┬──────────┬──────────┬──────────┬──────────┐   │
│  │ work     │ learn    │ fit      │ finance  │ life     │   │
│  └──────────┴──────────┴──────────┴──────────┴──────────┘   │
│  ┌────────────────────────────────────────────────────────┐ │
│  │ stats 跨域统计 · 定时任务(日汇总/周报/提醒)               │ │
│  └────────────────────────────────────────────────────────┘ │
└──────┬────────────────────────────────────────┬─────────────┘
       │                                        │
┌──────▼──────────────┐              ┌──────────▼────────────┐
│  MySQL 8            │              │  Redis                │
│  业务表 + 通用引擎表  │              │  会话/缓存/番茄钟状态   │
│  Flyway 版本化迁移    │              │  /今日统计 缓存        │
└─────────────────────┘              └───────────────────────┘
```

**部署形态**：个人使用，单机 Docker Compose（mysql + redis + server + web/nginx）即可，无需 K8s。

---

## 3. 核心抽象：三个通用引擎 ★

**这是整个方案最关键的设计。** 直接映射建表（每个需求一张表）在个人项目里是灾难：加个「冥想打卡」要写表、写 Mapper、写接口、写前端页面。三个月后表 40 张，代码没法维护。

解法：抽出三个与业务无关的通用引擎 + 各域只保留自己的「强语义」表。

### 3.1 标签引擎（统一分类）
所有实体共用 `sys_tag` + `sys_tag_rel`（多态关联）。
好处：一个标签「#深度工作」可以同时挂在工作任务、学习笔记、运动记录上，跨域检索天然打通。

### 3.2 指标引擎（可配置量化追踪）★
```
sys_metric_def     指标定义：编码、名称、单位、值类型(number/duration/enum/bool)、
                   归属维度、是否计入首页、目标值、聚合方式(sum/avg/max/last)
sys_metric_record  指标记录：metric_id + 日期 + 数值 + 备注
```
**新增任何量化追踪项 = 插入 1 条 def**。冥想分钟数、喝水杯数、静息心率、背单词数……全部复用。
首页卡片、趋势图、周报聚合都从 `sys_metric_record` 自动生成 —— 这是「后续可重复优化」的技术底座。

### 3.3 活动流引擎（统一时间轴）
```
act_activity_log   全系统原子事件：维度 + 类型 + 标题 + 发生时间 + 耗时 + 关联实体 + 数值
```
所有域的业务动作在 Service 层**旁路写入**一条活动流。
好处：
- 首页「当天时间轴」自动成型，无需各域适配
- 跨域统计（本周工作 32h / 学习 8h / 运动 4h 占比）一次查询搞定
- 未来接 AI 周报，只需喂这一张表

### 3.4 各域职责边界
| 域 | 强语义表 | 举例 |
|---|---|---|
| work | 项目 / 任务 / **SOP** / 工时 | 任务有父子层级和状态机，必须专表 |
| learn | 学习计划 / 学习记录 / 笔记 | 笔记要 SRS 复习算法，专表 |
| fit | 训练记录 / 动作明细 / 身体指标 | 训练有组数次数，结构固定 |
| finance | 账户 / 分类 / 流水 / 预算 | 金额精度和分类树必须专表 |
| life | 习惯 / 打卡 / 目标OKR / 日记 | 打卡需要 streak 计算 |

**判断规则**：有复杂状态机 / 层级 / 专有字段 → 建专表；只是「某天记了个数」→ 走指标引擎。

---

## 4. 功能模块拆解

### 4.1 驾驶舱 Dashboard（默认首页）
- **今日焦点**：今日任务 TOP3 + 已排期的学习/训练
- **状态条**：连续打卡天数、今日番茄数、今日专注时长、本月结余
- **12 周热力图**：跨域合并的活动密度（借鉴 Life-OS）
- **五维雷达**：工作 / 学习 / 运动 / 理财 / 生活 的投入均衡度（借鉴 LifeOS 2.0 的 HUD）
- **时间轴**：当天活动流倒序
- **模块卡片**：可拖拽增删排序（借鉴 life-board），持久化到用户偏好
- **快捷记录条**：底部固定，一句话自然语言记录 → 路由到对应域（如「跑步5公里」→ 运动域）

### 4.2 工作域 Work
| 功能 | 说明 |
|---|---|
| 项目 Project | 目标、周期、状态、颜色标识（借鉴 TodoList-Manager 的专题层） |
| 任务 Task | 父子层级、优先级、预估/实际工时、看板+列表+甘特三视图 |
| **SOP / Skill 库** ★ | 步骤化流程、版本历史、使用次数、平均耗时、关联标签 |
| 工时 Time Log | 番茄钟计时 + 手动补录，按项目/标签聚合 |
| **沉淀提醒** ★ | 同一类任务完成 ≥3 次 → 系统提示「是否沉淀为 SOP」 |
| 周报 | 自动汇总本周完成、工时分布、SOP 复用情况 |

### 4.3 学习域 Learn
| 功能 | 说明 |
|---|---|
| 学习计划 Plan | 目标 → 阶段 → 主题，带进度百分比 |
| 学习记录 Session | 时长、主题、来源（书/课/文章/视频）、收获摘要 |
| 笔记 Note | Markdown 编辑、双向标签，带 **SRS 复习**（SM-2 简化版：next_review_at + interval + ease） |
| 阅读 Book | 页数/进度/摘录 |
| 技能树 Skill Tree | 技能节点 + 熟练度等级，关联 SOP |
| 学习热力图 | 按主题分别统计 |

### 4.4 运动域 Fit
| 功能 | 说明 |
|---|---|
| 训练记录 Workout | 类型（力量/有氧/球类）、时长、消耗、主观强度 RPE |
| 动作明细 Item | 动作名、组数、次数、重量、组间休息 |
| 训练模板 | 常用训练一键复用（复用 SOP 的步骤模型） |
| 身体指标 | 体重/体脂/围度 → 走指标引擎 |
| 训练计划 | 周计划 + 完成率 |
| 趋势图 | 容量曲线（重量×次数）、体重 7 日移动平均 |

### 4.5 理财域 Finance
| 功能 | 说明 |
|---|---|
| 账户 Account | 银行卡/支付宝/微信/券商/现金，余额 |
| 分类 Category | 两级分类树，收/支分离 |
| 流水 Transaction | 金额、分类、账户、时间、备注、标签，支持批量导入 CSV |
| 预算 Budget | 月度总预算 + 分类预算，超支预警 |
| 资产快照 Snapshot | 每月末自动/手动快照，生成资产净值曲线 |
| 定投计划 | 周期性投入提醒 |
| 报表 | 收支趋势、分类占比、同比环比 |

### 4.6 生活域 Life
| 功能 | 说明 |
|---|---|
| 习惯 Habit | 频次（每日/每周N次）、目标周期、streak、12 周热力图 |
| 打卡 Log | 一键打卡（来自快捷记录条） |
| 目标 OKR | 目标 → 关键结果 → 进度自动关联习惯/指标 |
| 日记 Diary | Markdown + 心情打分 + 天气，自动关联当天活动流 |
| 番茄钟 Pomodoro | 服务端计时状态存 Redis，防刷新丢失 |
| 复盘 Review | 周报/月报：自动数据 + 人工批注（3 问：做得好/待改进/下步动作） |

### 4.7 系统域 System
用户与鉴权、标签管理、指标定义管理、活动流查询、附件、数据导入导出（全量 JSON）、定时任务管理、操作日志。

---

## 5. 数据库设计

命名规范：`sys_` 系统域，`act_` 活动流，`work_` / `learn_` / `fit_` / `fin_` / `life_` 各业务域。
统一字段（所有业务表）：`id BIGINT PK AUTO_INCREMENT`、`create_time`、`update_time`、`deleted TINYINT DEFAULT 0`（逻辑删除）。
字符集 `utf8mb4_general_ci`，引擎 `InnoDB`。

### 5.1 通用引擎表（核心）

```sql
-- 系统域 ------------------------------------------------------------
CREATE TABLE sys_user (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  username      VARCHAR(50)  NOT NULL UNIQUE,
  password      VARCHAR(100) NOT NULL COMMENT 'BCrypt',
  nickname      VARCHAR(50),
  avatar        VARCHAR(255),
  city          VARCHAR(50),
  daily_target_minutes INT DEFAULT 480 COMMENT '每日目标投入分钟',
  last_login_at DATETIME,
  create_time   DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted       TINYINT DEFAULT 0
) COMMENT '用户';

CREATE TABLE sys_tag (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  name        VARCHAR(50) NOT NULL,
  color       VARCHAR(20) DEFAULT '#639922',
  scope       VARCHAR(20) COMMENT '所属域，common=通用',
  use_count   INT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  deleted     TINYINT DEFAULT 0,
  UNIQUE KEY uk_name_scope (name, scope)
) COMMENT '统一标签';

CREATE TABLE sys_tag_rel (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  tag_id      BIGINT NOT NULL,
  biz_type    VARCHAR(30) NOT NULL COMMENT 'task/note/workout/transaction/sop...',
  biz_id      BIGINT NOT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_tag_biz (tag_id, biz_type, biz_id),
  KEY idx_biz (biz_type, biz_id)
) COMMENT '标签多态关联';

-- ★ 指标引擎 --------------------------------------------------------
CREATE TABLE sys_metric_def (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  code          VARCHAR(50) NOT NULL UNIQUE COMMENT 'weight / water_cup / mood',
  name          VARCHAR(50) NOT NULL,
  dimension     VARCHAR(20) NOT NULL COMMENT 'work/learn/fit/finance/life',
  value_type    VARCHAR(20) NOT NULL DEFAULT 'number' COMMENT 'number/duration/enum/bool',
  unit          VARCHAR(20) COMMENT 'kg / 分钟 / 杯',
  options_json  VARCHAR(500) COMMENT 'enum 类型的可选值',
  agg_type      VARCHAR(10) DEFAULT 'last' COMMENT 'sum/avg/max/min/last',
  target_value  DECIMAL(12,2) COMMENT '目标值，用于进度条',
  show_on_home  TINYINT DEFAULT 1,
  sort_no       INT DEFAULT 0,
  create_time   DATETIME DEFAULT CURRENT_TIMESTAMP,
  deleted       TINYINT DEFAULT 0
) COMMENT '指标定义 - 新增追踪项只需插一条';

CREATE TABLE sys_metric_record (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  metric_id   BIGINT NOT NULL,
  record_date DATE   NOT NULL,
  record_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  value_num   DECIMAL(12,2),
  value_text  VARCHAR(200),
  ref_type    VARCHAR(30) COMMENT '可选：来源业务实体',
  ref_id      BIGINT,
  remark      VARCHAR(255),
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  deleted     TINYINT DEFAULT 0,
  KEY idx_metric_date (metric_id, record_date),
  KEY idx_date (record_date)
) COMMENT '指标记录';

-- ★ 活动流引擎 ------------------------------------------------------
CREATE TABLE act_activity_log (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id      BIGINT NOT NULL,
  dimension    VARCHAR(20) NOT NULL COMMENT 'work/learn/fit/finance/life',
  biz_type     VARCHAR(30) NOT NULL COMMENT 'task_done/study/workout/expense/checkin',
  title        VARCHAR(200) NOT NULL,
  content      VARCHAR(1000),
  duration_min INT DEFAULT 0,
  value_num    DECIMAL(12,2),
  ref_type     VARCHAR(30),
  ref_id       BIGINT,
  occurred_at  DATETIME NOT NULL COMMENT '事件发生时间（非创建时间）',
  activity_date DATE NOT NULL COMMENT '冗余，用于分组统计',
  create_time  DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY idx_user_date (user_id, activity_date),
  KEY idx_dim_date (dimension, activity_date)
) COMMENT '统一活动流 - 全系统时间轴';

-- 复盘 --------------------------------------------------------------
CREATE TABLE sys_review (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id      BIGINT NOT NULL,
  review_type  VARCHAR(10) NOT NULL COMMENT 'week/month/year',
  period_key   VARCHAR(20) NOT NULL COMMENT '2026-W40 / 2026-10',
  start_date   DATE NOT NULL,
  end_date     DATE NOT NULL,
  auto_data    JSON COMMENT '系统自动统计结果快照',
  good_text    TEXT   COMMENT '做得好的',
  bad_text     TEXT   COMMENT '待改进的',
  action_text  TEXT   COMMENT '下步动作',
  score        TINYINT COMMENT '自评 1-5',
  create_time  DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_period (user_id, review_type, period_key)
) COMMENT '周期复盘';
```

### 5.2 工作域

```sql
CREATE TABLE work_project (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id     BIGINT NOT NULL,
  name        VARCHAR(100) NOT NULL,
  description VARCHAR(500),
  color       VARCHAR(20),
  status      VARCHAR(20) DEFAULT 'active' COMMENT 'active/paused/done/archived',
  start_date  DATE,
  end_date    DATE,
  sort_no     INT DEFAULT 0,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted     TINYINT DEFAULT 0
) COMMENT '项目/专题';

CREATE TABLE work_task (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id       BIGINT NOT NULL,
  project_id    BIGINT,
  parent_id     BIGINT DEFAULT 0 COMMENT '父任务，0=顶层',
  title         VARCHAR(200) NOT NULL,
  description   TEXT,
  status        VARCHAR(20) DEFAULT 'todo' COMMENT 'todo/doing/blocked/done',
  priority      TINYINT DEFAULT 2 COMMENT '1高 2中 3低',
  plan_start    DATETIME,
  due_at        DATETIME,
  estimate_min  INT COMMENT '预估工时',
  actual_min    INT DEFAULT 0 COMMENT '实际工时',
  done_at       DATETIME,
  sort_no       INT DEFAULT 0,
  create_time   DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted       TINYINT DEFAULT 0,
  KEY idx_user_status (user_id, status),
  KEY idx_due (due_at)
) COMMENT '任务';

-- ★ 本系统的差异化模块
CREATE TABLE work_sop (
  id             BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id        BIGINT NOT NULL,
  title          VARCHAR(200) NOT NULL,
  category       VARCHAR(50) COMMENT '分类，如 需求评审/故障处理/周报',
  trigger_scene  VARCHAR(500) COMMENT '什么场景下用这个 SOP',
  goal           VARCHAR(500) COMMENT '产出什么结果',
  version        VARCHAR(20) DEFAULT 'v1.0',
  use_count      INT DEFAULT 0 COMMENT '被复用次数',
  avg_minutes    INT DEFAULT 0 COMMENT '平均耗时，自动统计',
  last_used_at   DATETIME,
  status         VARCHAR(20) DEFAULT 'draft' COMMENT 'draft/active/deprecated',
  source_task_id BIGINT COMMENT '由哪个任务沉淀而来',
  create_time    DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time    DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted        TINYINT DEFAULT 0
) COMMENT 'SOP / 工作技能库';

CREATE TABLE work_sop_step (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT,
  sop_id       BIGINT NOT NULL,
  step_no      INT NOT NULL,
  title        VARCHAR(200) NOT NULL,
  detail       TEXT COMMENT '操作说明/Markdown',
  tip          VARCHAR(500) COMMENT '避坑提示',
  estimate_min INT,
  KEY idx_sop (sop_id, step_no)
) COMMENT 'SOP 步骤';

CREATE TABLE work_sop_version (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  sop_id      BIGINT NOT NULL,
  version     VARCHAR(20) NOT NULL,
  content_json JSON COMMENT '该版本的完整快照（含步骤）',
  change_note VARCHAR(500) COMMENT '本次优化了什么',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY idx_sop (sop_id)
) COMMENT 'SOP 版本历史 - 记录流程如何一步步变好';

CREATE TABLE work_sop_log (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  sop_id      BIGINT NOT NULL,
  used_at     DATETIME DEFAULT CURRENT_TIMESTAMP,
  cost_min    INT,
  deviation   VARCHAR(500) COMMENT '执行中发现的问题/需要改进的点',
  KEY idx_sop (sop_id)
) COMMENT 'SOP 使用记录 - 支撑「使用次数」和「平均耗时」';

CREATE TABLE work_time_log (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id     BIGINT NOT NULL,
  task_id     BIGINT,
  sop_id      BIGINT,
  start_at    DATETIME NOT NULL,
  end_at      DATETIME,
  minutes     INT,
  note        VARCHAR(255),
  source      VARCHAR(20) DEFAULT 'manual' COMMENT 'pomodoro/manual',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  deleted     TINYINT DEFAULT 0,
  KEY idx_user_date (user_id, start_at)
) COMMENT '工时流水';
```

### 5.3 学习域

```sql
CREATE TABLE learn_plan (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id      BIGINT NOT NULL,
  title        VARCHAR(200) NOT NULL,
  subject      VARCHAR(50) COMMENT '主题，如 分布式/英语/算法',
  goal         VARCHAR(500),
  target_hours INT COMMENT '目标小时数',
  done_hours   DECIMAL(8,1) DEFAULT 0,
  start_date   DATE,
  deadline     DATE,
  status       VARCHAR(20) DEFAULT 'active' COMMENT 'active/paused/done',
  create_time  DATETIME DEFAULT CURRENT_TIMESTAMP,
  deleted      TINYINT DEFAULT 0
) COMMENT '学习计划';

CREATE TABLE learn_session (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id     BIGINT NOT NULL,
  plan_id     BIGINT,
  subject     VARCHAR(50),
  source_type VARCHAR(20) COMMENT 'book/course/article/video/practice',
  source_name VARCHAR(200),
  minutes     INT NOT NULL,
  study_date  DATE NOT NULL,
  summary     VARCHAR(1000) COMMENT '本次收获',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  deleted     TINYINT DEFAULT 0,
  KEY idx_user_date (user_id, study_date),
  KEY idx_subject (subject)
) COMMENT '学习记录';

CREATE TABLE learn_note (
  id             BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id        BIGINT NOT NULL,
  title          VARCHAR(200) NOT NULL,
  content        LONGTEXT COMMENT 'Markdown',
  note_type      VARCHAR(20) DEFAULT 'note' COMMENT 'note/excerpt/summary',
  source         VARCHAR(200),
  -- SRS 间隔重复
  review_count   INT DEFAULT 0,
  next_review_at DATE COMMENT '下次复习日期',
  interval_days  INT DEFAULT 1 COMMENT '当前间隔',
  ease_factor    DECIMAL(4,2) DEFAULT 2.50 COMMENT 'SM-2 难度系数',
  mastered       TINYINT DEFAULT 0,
  create_time    DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time    DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted        TINYINT DEFAULT 0,
  KEY idx_review (user_id, next_review_at)
) COMMENT '笔记 - 内置 SRS 复习调度';
```

### 5.4 运动域

```sql
CREATE TABLE fit_workout (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id      BIGINT NOT NULL,
  workout_type VARCHAR(30) NOT NULL COMMENT 'strength/cardio/ball/mobility',
  title        VARCHAR(100),
  workout_date DATE NOT NULL,
  start_time   DATETIME,
  duration_min INT,
  calories     INT,
  rpe          TINYINT COMMENT '主观强度 1-10',
  feeling      VARCHAR(200),
  template_id  BIGINT,
  create_time  DATETIME DEFAULT CURRENT_TIMESTAMP,
  deleted      TINYINT DEFAULT 0,
  KEY idx_user_date (user_id, workout_date)
) COMMENT '训练记录';

CREATE TABLE fit_workout_item (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  workout_id  BIGINT NOT NULL,
  exercise    VARCHAR(50) NOT NULL COMMENT '动作名，如 深蹲',
  set_no      INT,
  reps        INT,
  weight_kg   DECIMAL(6,2),
  distance_km DECIMAL(6,2),
  rest_sec    INT,
  KEY idx_workout (workout_id)
) COMMENT '训练动作明细';

CREATE TABLE fit_template (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id      BIGINT NOT NULL,
  name         VARCHAR(100) NOT NULL,
  workout_type VARCHAR(30),
  items_json   JSON COMMENT '动作预设，一键复用',
  use_count    INT DEFAULT 0,
  create_time  DATETIME DEFAULT CURRENT_TIMESTAMP,
  deleted      TINYINT DEFAULT 0
) COMMENT '训练模板';
```

### 5.5 理财域

```sql
CREATE TABLE fin_account (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id      BIGINT NOT NULL,
  name         VARCHAR(50) NOT NULL,
  account_type VARCHAR(20) NOT NULL COMMENT 'bank/alipay/wechat/securities/cash/credit',
  balance      DECIMAL(14,2) DEFAULT 0,
  currency     VARCHAR(10) DEFAULT 'CNY',
  include_asset TINYINT DEFAULT 1 COMMENT '是否计入总资产',
  icon         VARCHAR(50),
  sort_no      INT DEFAULT 0,
  create_time  DATETIME DEFAULT CURRENT_TIMESTAMP,
  deleted      TINYINT DEFAULT 0
) COMMENT '账户';

CREATE TABLE fin_category (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id     BIGINT NOT NULL,
  parent_id   BIGINT DEFAULT 0,
  name        VARCHAR(50) NOT NULL,
  direction   VARCHAR(10) NOT NULL COMMENT 'income/expense',
  icon        VARCHAR(50),
  budget_amount DECIMAL(12,2) COMMENT '该分类月预算',
  sort_no     INT DEFAULT 0,
  deleted     TINYINT DEFAULT 0
) COMMENT '收支分类（两级树）';

CREATE TABLE fin_transaction (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id      BIGINT NOT NULL,
  account_id   BIGINT NOT NULL,
  category_id  BIGINT,
  direction    VARCHAR(10) NOT NULL COMMENT 'income/expense/transfer',
  amount       DECIMAL(12,2) NOT NULL COMMENT '正数存储，方向由 direction 决定',
  trade_time   DATETIME NOT NULL,
  trade_date   DATE NOT NULL,
  counterparty VARCHAR(100) COMMENT '交易对方',
  remark       VARCHAR(255),
  bill_month   VARCHAR(7) COMMENT '2026-10，便于月度统计',
  source       VARCHAR(20) DEFAULT 'manual' COMMENT 'manual/csv/import',
  create_time  DATETIME DEFAULT CURRENT_TIMESTAMP,
  deleted      TINYINT DEFAULT 0,
  KEY idx_user_date (user_id, trade_date),
  KEY idx_month (user_id, bill_month),
  KEY idx_category (category_id)
) COMMENT '收支流水';

CREATE TABLE fin_asset_snapshot (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id     BIGINT NOT NULL,
  snapshot_month VARCHAR(7) NOT NULL COMMENT '2026-10',
  total_asset DECIMAL(14,2) NOT NULL,
  total_debt  DECIMAL(14,2) DEFAULT 0,
  net_asset   DECIMAL(14,2) NOT NULL,
  detail_json JSON COMMENT '各账户明细快照',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_month (user_id, snapshot_month)
) COMMENT '资产快照 - 生成净值曲线';
```

### 5.6 生活域

```sql
CREATE TABLE life_habit (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id       BIGINT NOT NULL,
  name          VARCHAR(50) NOT NULL,
  icon          VARCHAR(50),
  color         VARCHAR(20),
  freq_type     VARCHAR(20) DEFAULT 'daily' COMMENT 'daily/weekly',
  freq_target   INT DEFAULT 1 COMMENT '每周目标次数',
  target_days   INT DEFAULT 21 COMMENT '养成周期',
  current_streak INT DEFAULT 0,
  best_streak   INT DEFAULT 0,
  total_count   INT DEFAULT 0,
  sort_no       INT DEFAULT 0,
  archived      TINYINT DEFAULT 0,
  create_time   DATETIME DEFAULT CURRENT_TIMESTAMP,
  deleted       TINYINT DEFAULT 0
) COMMENT '习惯';

CREATE TABLE life_habit_log (
  id           BIGINT PRIMARY KEY AUTO_INCREMENT,
  habit_id     BIGINT NOT NULL,
  user_id      BIGINT NOT NULL,
  check_date   DATE NOT NULL,
  value_num    DECIMAL(10,2) COMMENT '如饮水 8 杯，可空',
  remark       VARCHAR(255),
  create_time  DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_habit_date (habit_id, check_date),
  KEY idx_user_date (user_id, check_date)
) COMMENT '习惯打卡';

CREATE TABLE life_goal (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id     BIGINT NOT NULL,
  title       VARCHAR(200) NOT NULL,
  dimension   VARCHAR(20) COMMENT '归属域',
  goal_type   VARCHAR(10) DEFAULT 'quarter' COMMENT 'year/quarter/month',
  period_key  VARCHAR(20) COMMENT '2026-Q4',
  progress    TINYINT DEFAULT 0 COMMENT '自动计算 0-100',
  status      VARCHAR(20) DEFAULT 'active',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  deleted     TINYINT DEFAULT 0
) COMMENT '目标 OKR';

CREATE TABLE life_goal_kr (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  goal_id     BIGINT NOT NULL,
  content     VARCHAR(300) NOT NULL,
  target_value DECIMAL(12,2),
  current_value DECIMAL(12,2) DEFAULT 0,
  unit        VARCHAR(20),
  link_metric_id BIGINT COMMENT '关联指标，value 自动同步',
  progress    TINYINT DEFAULT 0,
  KEY idx_goal (goal_id)
) COMMENT '关键结果';

CREATE TABLE life_diary (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id     BIGINT NOT NULL,
  diary_date  DATE NOT NULL,
  title       VARCHAR(200),
  content     LONGTEXT COMMENT 'Markdown',
  mood        TINYINT COMMENT '1-5',
  weather     VARCHAR(20),
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted     TINYINT DEFAULT 0,
  UNIQUE KEY uk_user_date (user_id, diary_date)
) COMMENT '日记';

CREATE TABLE life_pomodoro (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id     BIGINT NOT NULL,
  task_id     BIGINT,
  sop_id      BIGINT,
  start_at    DATETIME NOT NULL,
  end_at      DATETIME,
  minutes     INT DEFAULT 25,
  status      VARCHAR(20) DEFAULT 'running' COMMENT 'running/done/aborted',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY idx_user_date (user_id, start_at)
) COMMENT '番茄钟';
```

### 5.7 索引与性能要点
- 所有「按日期统计」的查询走 `(user_id, *_date)` 复合索引，覆盖首页聚合。
- 首页统计数据缓存到 Redis，key `home:{userId}:{yyyy-MM-dd}`，TTL 到当天 24:00；写操作后主动失效。
- `act_activity_log` 增长最快，按月归档到 `act_activity_log_202610` 历史表（定时任务）。
- 单用户数据量级很小（十年也就几十万行），**不需要分库分表**，把索引和缓存做对即可。

---

## 6. 后端工程设计

### 6.1 技术选型

| 组件 | 选型 | 理由 |
|---|---|---|
| JDK | 17（LTS） | Spring Boot 3 最低要求，21 亦可 |
| Spring Boot | 3.2.x | 当前稳定版 |
| ORM | **MyBatis-Plus 3.5.x** | 比裸 MyBatis 省 60% 样板代码；分页插件、逻辑删除、自动填充一步到位 |
| 连接池 | HikariCP（Boot 内置） | 默认即可 |
| 鉴权 | **Sa-Token 1.37+** | 个人项目比 Spring Security 简单一个数量级，注解式鉴权 `@SaCheckLogin` |
| 密码 | BCrypt | `spring-security-crypto` 单独引入即可 |
| DB 版本管理 | **Flyway** | ★ 实现「后续可重复优化」的关键：每次改表结构写一个 `V{n}__xxx.sql` |
| 接口文档 | Knife4j 4.x（基于 SpringDoc） | 中文友好，调试方便 |
| 缓存 | Redis（Spring Data Redis） | 番茄钟状态、首页统计缓存 |
| 定时任务 | Spring `@Scheduled` | 个人项目无需 XXL-Job |
| 对象存储 | 本地磁盘 / MinIO | 头像、附件 |
| 构建 | Maven | |
| 参数校验 | Jakarta Validation | |
| 工具库 | Hutool + MapStruct | MapStruct 做 Entity↔VO 转换 |

### 6.2 工程结构（单模块分包，后期可拆）

个人项目**不要一上来就 Maven 多模块**，会增加心智负担。先用单模块分包，等某一块复杂到需要独立部署再拆。

```
personal-os-server/
└── src/main/java/com/yourname/personalos/
    ├── PersonalOsApplication.java
    ├── common/
    │   ├── result/          R.java / PageResult.java
    │   ├── exception/       BizException / GlobalExceptionHandler
    │   ├── config/          MybatisPlusConfig / RedisConfig / SaTokenConfig / WebMvcConfig / CorsConfig
    │   ├── constant/        DimensionEnum / BizTypeEnum / MetricCodeConst
    │   ├── util/            DateUtil / PeriodUtil(周/月区间计算)
    │   └── annotation/      @LogActivity (AOP 自动写活动流)
    ├── system/              ★ 通用引擎
    │   ├── controller/      UserController / TagController / MetricController
    │   │                    ActivityController / ReviewController / DashboardController
    │   ├── service/         MetricEngineService / ActivityStreamService / TagService
    │   ├── mapper/  entity/  dto/  vo/
    ├── work/                controller / service / mapper / entity / dto / vo
    ├── learn/               同上
    ├── fit/                 同上
    ├── finance/             同上
    ├── life/                同上
    ├── stats/
    │   ├── service/         DashboardAggregateService / CrossDimensionStatService
    │   │                    WeeklyReportService
    │   └── job/             DailySummaryJob / WeeklyReportJob / HabitStreakJob / ArchiveJob
    └── infra/               storage(文件) / mail(可选，解析账单邮件)

src/main/resources/
├── application.yml / application-dev.yml / application-prod.yml
├── mapper/                 XML 放复杂 SQL，简单查询用 MP 注解
└── db/migration/           Flyway: V1__init.sql, V2__add_sop.sql ...
```

### 6.3 关键约定（团队/未来的自己要遵守）

**统一响应体**
```java
public class R<T> {
    private int code;      // 0 成功，非 0 失败
    private String msg;
    private T data;
    public static <T> R<T> ok(T data) { ... }
    public static <T> R<T> fail(String msg) { ... }
}
```
分页统一 `PageResult<T>{ records, total, page, size }`。

**活动流旁路写入（核心机制）**
```java
// 业务 Service 里，动作完成后一行代码接入统一时间轴
activityStreamService.log(ActivityLog.builder()
    .dimension(Dimension.WORK)
    .bizType("task_done")
    .title(task.getTitle())
    .durationMin(cost)
    .refType("work_task").refId(task.getId())
    .occurredAt(now)
    .build());
```
或更优雅：自定义 `@LogActivity(dimension=WORK, bizType="task_done")` 注解 + AOP，从返回值里提取标题和 id。

**指标写入统一入口**
```java
metricEngineService.record("weight", LocalDate.now(), new BigDecimal("72.4"), "晨起");
// 内部：查 def -> 校验 value_type -> 落 sys_metric_record -> 触发相关 KR 进度重算
```

**SOP 沉淀提醒逻辑**
```java
// task 完成时：按 title 相似度 + 标签，统计同类已完成任务数
// ≥3 且尚无关联 SOP → Dashboard 返回一条「建议沉淀」提示
// 点击即可把当前任务转为 SOP 草稿（步骤从 description 的列表项解析）
```

**日期与周期工具**
`PeriodUtil` 统一提供 `currentWeekKey()`（ISO 周，如 `2026-W40`）、`currentMonthKey()`、`weekRangeOf(LocalDate)`。所有周/月统计都走它，避免各处各写一套导致口径不一致。

### 6.4 API 设计规范

```
基础路径：/api
认证：Header  token: xxx        （Sa-Token）
命名：小写中划线，资源复数     /api/work/tasks
      GET    /api/work/tasks?status=doing&projectId=1&page=1&size=20
      POST   /api/work/tasks
      PUT    /api/work/tasks/{id}
      DELETE /api/work/tasks/{id}
      PATCH  /api/work/tasks/{id}/status     （状态流转单独接口）
```

关键接口清单（节选）：

| 模块 | 接口 | 说明 |
|---|---|---|
| 驾驶舱 | `GET /api/dashboard/home` | 一次返回今日焦点+状态条+雷达+热力图+时间轴 |
| 驾驶舱 | `POST /api/dashboard/quick-log` | 快捷记录，body 为自然语言，后端路由到对应域 |
| 指标 | `GET /api/system/metrics` | 指标定义列表 |
| 指标 | `POST /api/system/metrics/{code}/records` | 写指标值 |
| 指标 | `GET /api/system/metrics/{code}/trend?range=90d` | 趋势数据 |
| 活动流 | `GET /api/system/activities?date=&dimension=` | 时间轴 |
| SOP | `GET /api/work/sops` | SOP 列表（含使用次数/平均耗时） |
| SOP | `POST /api/work/sops/{id}/apply` | 复用一次，记录使用日志 |
| SOP | `GET /api/work/sops/sediment-suggestions` | ★ 沉淀建议 |
| 复盘 | `POST /api/system/reviews/generate?type=week` | 自动生成周报数据 |
| 学习 | `GET /api/learn/notes/review-today` | 今日待复习笔记（SRS） |
| 理财 | `POST /api/finance/transactions/import` | CSV 批量导入 |
| 理财 | `GET /api/finance/reports/overview?month=2026-10` | 月度收支报表 |
| 习惯 | `POST /api/life/habits/{id}/checkin` | 打卡（自动重算 streak） |
| 番茄钟 | `POST /api/life/pomodoro/start` / `/stop` | 服务端计时 |
| 系统 | `GET /api/system/export/all` | 全量 JSON 导出 |

---

## 7. 前端工程设计

### 7.1 技术选型

| 组件 | 选型 |
|---|---|
| 框架 | Vue 3.4（`<script setup>` + Composition API）+ TypeScript |
| 构建 | Vite 5 |
| 路由 | Vue Router 4（懒加载 + 路由守卫鉴权） |
| 状态 | Pinia（`useUserStore` / `useMetricStore` / `useDashboardStore`） |
| UI 库 | **Element Plus**（后台型界面最高效，其表格/表单/日期选择器覆盖 80% 需求） |
| 图表 | ECharts 5（折线/柱状/饼/雷达） + 自绘 SVG 热力图（ECharts 日历图不够灵活） |
| 日期 | dayjs |
| 请求 | Axios（统一拦截：注入 token、拆 Result、错误 Toast、401 跳登录） |
| 样式 | SCSS + 设计变量；可选 UnoCSS 做原子类 |
| 拖拽 | vuedraggable（首页卡片排序） |
| Markdown | md-editor-v3（笔记/日记/SOP 详情） |
| 代码规范 | ESLint + Prettier |

### 7.2 目录结构

```
personal-os-web/
├── src/
│   ├── main.ts / App.vue
│   ├── api/                    按域分文件，与后端一一对应
│   │   ├── request.ts           axios 封装
│   │   ├── dashboard.ts  work.ts  learn.ts  fit.ts  finance.ts  life.ts  system.ts
│   ├── router/
│   │   ├── index.ts
│   │   └── routes.ts            路由表（下方）
│   ├── stores/                 user / metric / dashboard / dict
│   ├── layouts/
│   │   ├── DefaultLayout.vue   侧边导航 + 顶栏 + 快捷记录条
│   │   └── BlankLayout.vue     登录页
│   ├── components/
│   │   ├── common/             MetricCard / HeatmapChart / RadarChart / TimeLine
│   │   │                       TrendChart / TagSelect / EmptyState / ProgressRing
│   │   ├── dashboard/          TodayFocus / StatusBar / QuickLogBar / ModuleCardGrid
│   │   ├── work/               TaskKanban / TaskList / TaskGantt / SopStepEditor
│   │   └── finance/            AmountInput / CategoryTreeSelect / AccountPicker
│   ├── views/
│   │   ├── dashboard/Index.vue
│   │   ├── work/               Project.vue Task.vue Sop.vue SopDetail.vue TimeLog.vue
│   │   ├── learn/              Plan.vue Session.vue Note.vue NoteDetail.vue Review.vue
│   │   ├── fit/                Workout.vue Body.vue Template.vue Trend.vue
│   │   ├── finance/            Account.vue Transaction.vue Budget.vue Report.vue Snapshot.vue
│   │   ├── life/               Habit.vue Goal.vue Diary.vue Pomodoro.vue Review.vue
│   │   └── system/             User.vue Tag.vue Metric.vue Activity.vue Setting.vue
│   ├── composables/            useHeatmap / useMetricTrend / usePomodoro / useQuickLog
│   ├── utils/                  date.ts(formatPeriod) / format.ts / color.ts / storage.ts
│   └── styles/                 variables.scss / mixins.scss / global.scss
```

### 7.3 路由表

```
/login                            登录
/                                 DefaultLayout
  /dashboard                       驾驶舱（默认）
  /work        /work/project
               /work/task
               /work/sop            SOP 库
               /work/sop/:id        SOP 详情
               /work/timelog
  /learn       /learn/plan
               /learn/session
               /learn/note
               /learn/review        今日复习（SRS）
  /fit         /fit/workout
               /fit/template
               /fit/trend           身体指标趋势
  /finance     /finance/account
               /finance/transaction
               /finance/budget
               /finance/report
  /life        /life/habit
               /life/goal
               /life/diary
               /life/pomodoro
               /life/review         周报/月报
  /system      /system/tag
               /system/metric       指标配置
               /system/activity     活动流全览
               /system/setting      偏好/导出
```

### 7.4 交互设计要点

**快捷记录条（降低记录摩擦的核心）**
底部固定输入框，支持自然语言：
```
"跑步 5km 30min"     → 运动域 fit_workout
"番茄 25min 写方案"   → work_time_log + 活动流
"买咖啡 28"          → finance transaction（识别金额 → 预填分类，待确认）
"背单词 30min"       → learn_session
"打卡 早起"          → life_habit_log
```
实现：前端做轻量正则匹配 → 命中的直接弹确认卡（预填好字段，一键提交）；未命中则弹出「选择维度」快捷表单。
**关键：默认走"预填 + 确认"而非"空白表单"，这是能坚持记录的分水岭。**

**首页卡片化**
`ModuleCardGrid` 支持拖拽排序 / 显隐，偏好存 `sys_user.preference_json`（或 `localStorage` 兜底）。

**热力图组件**
统一 `HeatmapChart`，接受 `[{date, value}]`，自动渲染 12 周（或在移动端渲染 4 周）。
习惯、学习、运动、活动流四处复用同一组件，只是数据源不同。

**移动端**
第一期只做响应式适配（Element Plus 栅格 + 媒体查询）；第二期再考虑 PWA（可添加到主屏、离线打卡）。打卡类操作在手机上完成频率最高。

---

## 8. 迭代路线图

分 4 期，**每期结束系统都能独立跑起来并产生价值**，不做「全做完才能用」的大爆炸式交付。

### P0 · 地基（骨架 + 通用引擎）
**目标**：跑通登录 + 布局 + 三大引擎，为后续所有功能铺路。
- [ ] 前后端工程初始化、Flyway V1 建表、Docker Compose 起 MySQL/Redis
- [ ] Sa-Token 登录鉴权、统一响应体、全局异常、Axios 拦截
- [ ] 标签引擎（列表/新增/多态关联）
- [ ] 指标引擎（定义管理 + 记录写入 + 趋势接口）
- [ ] 活动流引擎（写入接口 + 时间轴查询）
- [ ] 布局框架：侧边导航 + 顶栏 + 快捷记录条（先只支持指标类记录）
- [ ] 驾驶舱 V1：状态条 + 时间轴 + 指标卡片

**验收**：能登录，能配置一个自定义指标并在首页看到今日值和 7 日趋势。

### P1 · 工作 + 生活（价值最高的一期）★
**目标**：直接解决「完善工作流程 + 建立工作 skill」。
- [ ] 项目 / 任务 CRUD + 列表视图 + 看板视图
- [ ] **SOP 库**：增删改查、步骤编辑、Markdown 详情
- [ ] SOP 使用记录 + 使用次数/平均耗时自动统计
- [ ] **沉淀建议**：同类任务完成 ≥3 次触发提示，一键转 SOP
- [ ] SOP 版本历史与「本次优化了什么」记录
- [ ] 习惯 CRUD + 一键打卡 + streak 计算 + 热力图
- [ ] 番茄钟（服务端计时 + Redis）+ 工时流水
- [ ] 日记（Markdown + 心情）+ 当天活动流自动关联
- [ ] 驾驶舱 V2：今日焦点（今日任务 TOP3）+ 习惯热力图

**验收**：一周内的工作任务、习惯打卡、番茄钟全部在系统里跑；至少沉淀出 2 条自己的 SOP。

### P2 · 学习 + 运动
- [ ] 学习计划 → 学习记录（时长/主题/收获）
- [ ] 笔记 Markdown 编辑 + 标签 + **SRS 复习队列**（今日待复习页 + 评级按钮）
- [ ] 阅读进度记录
- [ ] 训练记录（含动作明细）+ 训练模板复用
- [ ] 身体指标（体重/体脂）走指标引擎 + 趋势图（7 日移动平均）
- [ ] 目标 OKR + 关键结果，KR 关联指标自动算进度
- [ ] 学习/运动热力图

**验收**：每天有学习记录，运动训练有明细，笔记有复习提醒。

### P3 · 理财 + 跨域统计
- [ ] 账户 / 分类树 / 流水 CRUD
- [ ] 快速记账（快捷记录条金额识别 + 记账浮窗）
- [ ] CSV 批量导入 + 分类自动匹配
- [ ] 月度预算 + 超支预警
- [ ] 资产快照 + 净值曲线
- [ ] 收支报表（趋势 / 分类占比 / 环比同比）
- [ ] **跨域统计页**：五维雷达、时间分配占比、相关性分析（如"运动周 vs 专注时长"）
- [ ] 周报/月报自动生成 + 人工批注三问

**验收**：月度报表能替代手工 Excel；周报一键生成。

### P4 · 打磨（可选，长期演进）
- [ ] AI 周报：把 `act_activity_log` + 本周统计喂给 LLM，生成复盘建议
- [ ] PWA / 移动端优化（打卡场景）
- [ ] 通知提醒（定时任务 + 站内信/邮件：该复习了 / 该记账了 / 习惯要断了）
- [ ] 全量数据导出 / 导入（防锁定，JSON + CSV）
- [ ] 成就徽章系统（正反馈，借鉴 PMS）
- [ ] 账单邮件自动解析（借鉴 life-board，IMAP 监听支付宝/微信账单）
- [ ] 数据备份定时任务

**优先级建议**：如果时间有限，**P0 + P1 先做到能用且好用**，其余按需推进。不要四期并行。

---

## 9. 风险与取舍

| 风险 | 应对 |
|---|---|
| **过度设计**：一上来搞微服务、多模块、DDD 分层 | 单模块分包 + 三层架构；只在 system 域做抽象。砍掉一切「以后可能会用」的东西 |
| **指标引擎被滥用**：所有东西都走指标，导致复杂业务（任务、流水）没法表达 | 严守第 3.4 节的判断规则：有状态机/层级/专有字段 → 建专表 |
| **记录摩擦大导致弃坑** | 快捷记录条 + 预填确认 + 首页一屏可达；每个新功能必须回答"记录要几步" |
| **数据孤岛**：各域各统计，无法跨域复盘 | 活动流引擎强制所有域写入；跨域统计只查这一张表 |
| **改表结构丢数据** | Flyway 版本化迁移，禁止手工改生产库；改表前先备份 |
| **数据丢失** | MySQL 每日自动备份 + 每周全量 JSON 导出到本地/网盘 |
| **只做记录不做复盘** | 周报由定时任务自动生成并推送，把复盘变成"批注"而非"创作" |
| **技术栈学习成本** | MyBatis-Plus + Sa-Token + Element Plus 均为文档最完善、最省心的组合；直接对标 `txxxxz/task-management` 骨架 |

---

## 10. 下一步动作

1. **确认范围**：是否按 P0 → P1 顺序推进？P1 是否以「SOP 库」为核心优先做？
2. **定名与初始化**：确定项目名、包名、Git 仓库结构（建议 `personal-os-server` / `personal-os-web` 双仓或 monorepo）
3. **搭骨架**：我可以直接生成 P0 的完整工程骨架（后端 Maven 工程 + Flyway V1 建表脚本 + Vue3+Vite 工程 + 登录闭环 + 三大引擎接口）
4. **先跑通再迭代**：骨架跑通后按 P1 逐个模块推进

> 附：如果希望少写代码，`sys_metric_def` + `act_activity_log` 这两张表可以在 P0 阶段就支撑起一个「能记、能看」的最小可用版本 —— 甚至不需要等 P1 完成，你就能开始用起来。
