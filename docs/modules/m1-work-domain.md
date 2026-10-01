# M1 · 工作域（任务）

> 状态：draft · 并行窗口 1（后端 8081 / 前端 5175，需 RIce 把端口加进 CORS 白名单）
> **前置必读：`docs/dev-workflow.md`（流程、共享资产清单、git 规则、环境速查）**
> Flyway 号段：**V10 – V14** · 错误码段：**10000 – 10099** · 已拍板决策：逾期**只标记不顺延**、自然日 00:00 口径

## 1. 概览

工作域是 Personal OS 每天打开的理由。交付任务 CRUD、状态流转、三视图（今日/全部/逾期）、标签。用户故事见 `docs/requirements/p1-requirements.md` §2.A（A1–A6）。

**15 秒新增是硬指标**：创建任务只有标题必填。

## 2. 范围内 / 范围外

**做**：work_task 单表 CRUD、状态机、三视图查询、标签关联（调 M0 的 TagService）、完成时写活动流。
**不做**：work_project（P2）、parent_id 子任务（P1 留字段不实现 UI）、任务依赖、甘特/日历视图、番茄钟、时间日志（work_time_log 表不建）。

## 3. 数据库（V10）

```sql
CREATE TABLE work_task (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id       BIGINT NOT NULL,
  project_id    BIGINT,
  parent_id     BIGINT DEFAULT 0 COMMENT '父任务，0=顶层；P1 不实现 UI',
  title         VARCHAR(200) NOT NULL,
  description   TEXT,
  status        VARCHAR(20) DEFAULT 'todo' COMMENT 'todo/doing/done/abandoned',
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
```

迁移文件名：`V10__work_task.sql`。单用户，`user_id` 恒写当前登录用户（取自 Sa-Token），**查询强制拼 user_id**。无种子数据。

## 4. 后端

**包**：`com.xiaodu.personalos.work`（controller / service / mapper / entity / dto / vo，按 P0 既有分层）

**错误码**（在自己包建 `WorkErrorCode` 枚举实现 common 接口，**不改 common/result/ErrorCode.java**）：
- `10001 TASK_NOT_FOUND` · `10002 INVALID_STATUS_TRANSITION` · `10003 PARAM_INVALID`

**接口契约**：

| 方法 | 路径 | 入参 | 出参/错误 |
|---|---|---|---|
| POST | `/api/work/tasks` | `{ title*, description?, priority?, dueAt?, estimateMin?, tagIds?[] }` | `{ id }`；10003 |
| GET | `/api/work/tasks?view=today\|all\|overdue&status?&page&size` | 分页 | `PageResult<TaskVO>` |
| GET | `/api/work/tasks/{id}` | — | `TaskVO`（含 tags）；10001 |
| PUT | `/api/work/tasks/{id}` | 同创建（全量覆盖） | — |
| PUT | `/api/work/tasks/{id}/status` | `{ status }` | 状态机校验失败 10002 |
| DELETE | `/api/work/tasks/{id}` | — | 逻辑删 |

**状态机**：`todo→doing→done`；`todo/doing→abandoned`；`done`/`abandoned` 终态不可再流转。完成时写 `done_at`，并调用 `ActivityLogService.log(dimension="work", bizType="task_done", title=任务标题, occurredAt=now)`。

**三视图口径（用 M0 的 `PeriodUtil`，禁自己算日期）**：
- `today`：未完成 且（`due_at` 为空 或 `due_at` >= 今日 00:00）——**不含逾期**（逾期不自动进今日，小杜拍板）
- `overdue`：未完成 且 `due_at` < 今日 00:00
- `all`：全部未删除

**事务边界**：Service 方法 `rollbackFor = Exception.class`；Controller 不碰 Entity、不 try/catch 业务异常。

## 5. 前端

**只动**：`src/views/work/`（替换 M0 留下的占位 `index.vue`，子组件随意加）、`src/api/work.ts`、`src/stores/workStore.ts`（如需）。

**页面**：单页三 Tab（今日 / 全部 / 逾期）：
- 顶部"快速添加"输入框：回车即建（15 秒硬指标的落点）；展开可填优先级/截止/标签
- 任务行：勾选框（流转 done）、标题、优先级点、标签、截止日（逾期红标）
- 编辑：点行内联或抽屉，玻璃风格
- 逾期 Tab 行首红色"逾期"徽标 + "改期"快捷按钮

**样式**：iOS 26 液态玻璃，token 全部用现有 `--glass-*` / `--text-*`（见 `docs/ui-design-system.md` 与 Login.vue 参照实现）。明暗两套 + 移动端可用。

## 6. 文件边界（白名单）

```
允许新增/修改：
  personal-os-server/src/main/java/com/xiaodu/personalos/work/**
  personal-os-server/src/main/resources/db/migration/V1[0-4]__*.sql
  personal-os-server/src/test/java/com/xiaodu/personalos/work/**
  personal-os-web/src/views/work/**
  personal-os-web/src/api/work.ts
  personal-os-web/src/stores/workStore.ts
禁止：见 dev-workflow.md §3.2 全部共享资产（pom / yml / common / router / styles / layouts / …）
```

## 7. 测试与验收

**测试**（`@SpringBootTest` 参照 P0 `AuthControllerTest` 直连本机 MySQL）：
- 创建仅标题 → 成功；空标题 → 10003
- 状态机非法流转（done→todo）→ 10002
- 三视图边界：due_at=昨日 → 只出现在 overdue；due_at=明日+无日期 → today；删除后任何视图不可见
- 完成任务 → `act_activity_log` 落一条 work/task_done
- 打标签 → `sys_tag_rel` 落关联

**验收清单**（`npm run type-check`+`build`、`mvn-bash clean package` 全绿后逐条勾）：
- [ ] 15 秒新增一条任务（只输入标题+回车）
- [ ] 今日/全部/逾期三视图切换正确
- [ ] 逾期任务红色标记且不进今日视图
- [ ] 勾完成任务有反馈，活动流可查
- [ ] 任务可打标签、按标签在"全部"里过滤
- [ ] 明暗主题 + 390px 移动端无破版
- [ ] `git diff --name-only` 无越界文件
