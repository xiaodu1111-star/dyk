# M2 · SOP 库（核心差异化模块）

> 状态：draft · 并行窗口 2（后端 8082 / 前端 5176，需 RIce 把端口加进 CORS 白名单）
> **前置必读：`docs/dev-workflow.md`（流程、共享资产清单、git 规则、环境速查）**
> Flyway 号段：**V15 – V19** · 错误码段：**10100 – 10199**（work 大段内的子段，与 M1 约定互斥）

## 1. 概览

SOP 库是本项目的差异化价值：小杜的核心诉求是"沉淀工作流程"——任务是消耗、SOP 是复利。交付 SOP 创建/编辑、版本留底、按步执行、执行历史。用户故事见 `docs/requirements/p1-requirements.md` §2.B（B1–B7）。

## 2. 范围内 / 范围外

**做**：四张表（sop/step/version/log）、创建/编辑/列表/详情、按步执行与勾选、执行历史与统计、版本自动快照、置顶/最近使用排序。
**不做**：富文本与附件（Markdown 纯文本即可）、版本恢复（只回看不恢复，P2）、SOP 分享/导入导出、从任务日志自动生成 SOP（"3 次提示"只是提示，落点是带标题打开创建页，提示本体在首页聚合，归集成阶段）。

## 3. 数据库（V15–V16）

```sql
-- V15__work_sop.sql
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
  pinned         TINYINT DEFAULT 0 COMMENT '置顶',
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

-- V16__work_sop_version_log.sql
CREATE TABLE work_sop_version (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  sop_id      BIGINT NOT NULL,
  version     VARCHAR(20) NOT NULL,
  content_json JSON COMMENT '该版本的完整快照（含步骤）',
  change_note VARCHAR(500) COMMENT '本次优化了什么',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  KEY idx_sop (sop_id)
) COMMENT 'SOP 版本历史';

CREATE TABLE work_sop_log (
  id          BIGINT PRIMARY KEY AUTO_INCREMENT,
  sop_id      BIGINT NOT NULL,
  started_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
  finished_at DATETIME,
  cost_min    INT,
  stuck_step  INT COMMENT '卡在第几步',
  deviation   VARCHAR(500) COMMENT '执行中发现的问题/需要改进的点',
  KEY idx_sop (sop_id)
) COMMENT 'SOP 使用记录';
```

（相对设计稿两处微调：`work_sop_log` 增加 `started_at/finished_at/stuck_step`；`work_sop` 增加 `pinned`。`user_id` 恒当前登录用户。）

## 4. 后端

**包**：`com.xiaodu.personalos.sop`。**错误码**（`SopErrorCode` 枚举放自己包）：
`10101 SOP_NOT_FOUND` · `10102 STEP_INVALID` · `10103 RUN_ALREADY_FINISHED` · `10104 PARAM_INVALID`

**接口契约**：

| 方法 | 路径 | 入参 | 说明 |
|---|---|---|---|
| POST | `/api/sop` | `{ title*, category?, triggerScene?, goal?, steps?[{ title*, detail?, tip?, estimateMin? }] }` | 创建含步骤 |
| GET | `/api/sop?keyword=&category=&page&size` | — | 排序：pinned 优先 → last_used_at → use_count |
| GET | `/api/sop/{id}` | — | 详情含步骤 + 版本列表（不含 content_json 全量，列表给摘要） |
| PUT | `/api/sop/{id}` | 同创建 | **若该 SOP 已有执行记录 → 先写 work_sop_version 快照再更新**，版本号 +0.1 |
| DELETE | `/api/sop/{id}` | — | 逻辑删 |
| GET | `/api/sop/{id}/versions/{versionId}` | — | 版本完整快照（回看） |
| POST | `/api/sop/{id}/runs` | — | 发起执行：建 `work_sop_log`（started_at=now）返回 runId |
| PUT | `/api/sop/runs/{runId}` | `{ finished: true, costMin?, stuckStep?, deviation? }` | 结束执行：回写 `work_sop.use_count+1`、`avg_minutes` 重算、`last_used_at=now`；写活动流 |
| GET | `/api/sop/{id}/runs?page&size` | — | 执行历史（时间、耗时、卡点、偏差） |

**关键规则**：
- 结束执行在同一事务里更新聚合字段（use_count / avg_minutes / last_used_at）
- 结束执行调 `ActivityLogService.log(dimension="work", bizType="sop_run", title=SOP标题, durationMin=costMin)`
- 创建 SOP 时若带 `sourceTaskId` 则落库（"从任务沉淀"入口）

## 5. 前端

**只动**：`src/views/sop/`（替换占位 `index.vue`）、`src/api/sop.ts`、`src/stores/sopStore.ts`（如需）。

**页面**（建议三视图态，都在 `views/sop/` 内用路由子态或组件切换）：
1. **列表**：卡片流；置顶钉、分类筛选、搜索；每卡显示使用次数/平均耗时
2. **创建/编辑**：表单（标题/分类/场景/目标）+ 步骤编辑器（增删、上下移、每步标题+说明+避坑+预估）；编辑已有记录的 SOP 提示"将保存版本快照"
3. **详情**：步骤清单 + 版本时间线（可展开回看）+ 执行历史（次数、平均、卡点统计）
4. **执行模式**：全屏/抽屉逐步打勾，当前步高亮；结束弹窗记"实际耗时/卡在第几步/发现什么问题"

**样式**：iOS 26 玻璃，同 M1 要求。

## 6. 文件边界（白名单）

```
允许新增/修改：
  personal-os-server/src/main/java/com/xiaodu/personalos/sop/**
  personal-os-server/src/main/resources/db/migration/V1[5-9]__*.sql
  personal-os-server/src/test/java/com/xiaodu/personalos/sop/**
  personal-os-web/src/views/sop/**
  personal-os-web/src/api/sop.ts
  personal-os-web/src/stores/sopStore.ts
禁止：见 dev-workflow.md §3.2；特别强调错误码用 10100-10199，别占 M1 的 10000-10099
```

## 7. 测试与验收

**测试**：
- 创建含 3 步的 SOP → 详情返回 3 步且 step_no 连续
- 编辑**未执行过**的 SOP → 不产生版本；发起执行并结束后再编辑 → `work_sop_version` 落快照、版本号 +0.1
- 结束执行 → use_count+1、avg_minutes 正确、`act_activity_log` 落一条
- 对已结束的 run 再次 PUT → 10103
- 列表排序：pinned > last_used_at > use_count

**验收清单**：
- [ ] 创建 SOP（含 3 步）流畅，步骤可拖/移排序
- [ ] 执行模式逐步打勾、结束记耗时与卡点
- [ ] 执行历史能看到"跑过几次、平均多久、常卡哪步"
- [ ] 编辑用过的 SOP 自动留版本，版本时间线可回看
- [ ] 置顶与最近使用排序生效
- [ ] 明暗主题 + 390px 移动端无破版
- [ ] `git diff --name-only` 无越界文件
