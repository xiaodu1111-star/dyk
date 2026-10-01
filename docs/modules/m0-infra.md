# M0 · 基础设施（三大引擎 + 首页聚合骨架）

> 状态：dev · 负责窗口：**RIce 主窗口**（共享资产只准本窗口改，故不并行）
> 前置必读：`docs/dev-workflow.md`
> Flyway 号段：**V2 – V4**

## 1. 职责

在 M1/M2/M3 并行开工前，把三个模块共同依赖的共享资产铺好。**这是 P1 能并行的前提。**

## 2. 交付清单

| # | 内容 | 说明 |
|---|---|---|
| 1 | V2 标签引擎 | `sys_tag` + `sys_tag_rel` 两表（DDL 见 `docs/personal-os-design.md` 5.1）+ `TagService` + `POST/GET /api/system/tags` |
| 2 | V3 指标引擎 | `sys_metric_def` + `sys_metric_record` 两表 + `MetricService` + `POST/GET /api/system/metrics`、`POST /api/system/metrics/{code}/records` |
| 3 | V4 活动流 | `act_activity_log` 表 + `ActivityLogService`（**旁路写入 API，不暴露 REST**；供各模块 Service 调用） |
| 4 | `PeriodUtil` | 时间口径唯一来源：**自然日 00:00 分界**、周一为周首（`WeekFields.ISO`）、周 key `2026-W40`、月 key `2026-10`。预留口径切换常量位（不实现 04:00） |
| 5 | 首页聚合接口骨架 | `GET /api/dashboard/home` 契约先定死（见下），P1 集成阶段由各模块数据接线 |
| 6 | 前端路由与占位 | `views/work/index.vue`、`views/sop/index.vue`、`views/life/index.vue` 三个占位组件 + `router/index.ts` 注册 `/work` `/sop` `/life`。**模块窗口只替换占位组件内容，不碰 router** |
| 7 | `ErrorCode` 可扩展化 | 检查 `common/result/ErrorCode.java`：若是枚举则重构为接口（或保留通用枚举 + 允许各模块实现），确保各模块能在自己包里定义错误码而**不改 common** |

## 3. `GET /api/dashboard/home` 契约（先定死，各模块往里填）

```json
{
  "code": 0,
  "data": {
    "today": { "date": "2026-10-01", "week": "周四" },
    "work":  { "todayTotal": 0, "todayDone": 0, "overdue": 0 },
    "life":  { "checkinDone": 0, "checkinTotal": 0, "habits": [ { "id": 1, "name": "...", "done": false } ] },
    "sop":   { "top": [ { "id": 1, "title": "...", "useCount": 0 } ] },
    "streakDays": 0,
    "sopHints": [ { "taskTitle": "...", "count": 3 } ]
  }
}
```

M0 交付时所有数值为 0 / 空数组（骨架）。集成阶段接线。

## 4. 验收

1. 空库一键启动：Flyway V1–V4 全部执行成功
2. `/api/system/metrics` 新增一个指标 → `/api/system/metrics/{code}/records` 写一条值 → 查询能读回
3. `/api/system/tags` 增/查正常，`sys_tag_rel` 多态关联可写入
4. `ActivityLogService.log(...)` 调用后 `act_activity_log` 落一条
5. `/api/dashboard/home` 返回上述契约结构（骨架值）
6. 前端三个路由可达（占位页）；`npm run type-check` / `build` exit 0
