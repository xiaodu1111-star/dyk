# P1 验收报告 · M1 工作域 / M2 SOP 库 / M3 生活域 + 首页聚合

> 报告人：RIce（软件总设计师） · 日期：2026-10-01 · 代码版本：`e7257fa`
> **验证方式说明**：本轮原计划由 QA 工程师（严过关）做独立验证，但其运行时触发模型频率限制（429，2026-10-02 10:28 重置）而失败。
> 为不阻塞验收，改由**主设计师自验**（脚本化 44 项 + 空库迁移亲验），并在下方如实标注未覆盖项。QA 复测可在限制重置后补做。

## 1. 交付范围

| 模块 | 后端 | 前端 | 迁移 | 错误码段 | 独立测试 |
|---|---|---|---|---|---|
| M1 工作域 | `com.xiaodu.personalos.work` | `views/work/` | V10 | 10001-10003 | 22 |
| M2 SOP 库 | `com.xiaodu.personalos.sop` | `views/sop/` | V15-V16 | 10101-10104 | 20 |
| M3 生活域 | `com.xiaodu.personalos.life` | `views/life/` | **零建表** | 14001-14004 | 12 |
| 集成 | `system/` 聚合接线 | `views/home/` | — | — | 1（新增） |

后端全量 **76/76 测试通过**；前端 `type-check` / `build` 均 exit 0。

## 2. 自验结果（44 项，逐条命令可复现）

### 2.1 M1 工作域
| 项 | 结论 | 证据 |
|---|---|---|
| todo→doing→done 合法 | PASS | code=0 |
| 同状态重复设置幂等 | PASS | code=0，不报错 |
| done 写入 done_at | PASS | `2026-10-01 22:34:33` |
| 非法状态值被拒 | PASS | code=400「状态取值非法」 |
| done→todo 重开允许且清空 done_at | PASS | doneAt=null |
| **重开不重复写活动流** | PASS | 该任务 `task_done` 活动流仅 1 条 |
| abandoned 严格终态 | PASS | abandoned→done / →todo 均 code=10002 |
| 逾期任务不进 today 视图 | PASS | today 不含该 id |
| 逾期任务进 overdue 视图 | PASS | overdue 含该 id |
| 删除后 all / overdue 均不可见 | PASS | 两视图均不含 |
| 空标题被拒 | PASS | code=400 |
| 标签多态关联写入 | PASS | rel id=87 |
| 重复绑定幂等、use_count 不叠加 | PASS | useCount=1 |
| 任务详情带出标签 | PASS | `['自验标签']` |

### 2.2 M2 SOP 库
| 项 | 结论 | 证据 |
|---|---|---|
| finished≠true 拒绝结束 | PASS | code=10104 |
| finished=true 结束成功 | PASS | code=0 |
| 结束后 useCount 回写 | PASS | useCount=1 |
| 结束后 avgMinutes 重算 | PASS | avgMinutes=20 |
| 结束后 lastUsedAt 更新 | PASS | 有值 |
| **版本快照（已有执行记录后编辑才留）** | PASS | v1.0→v1.1；旧版本可回看，内容为编辑前的 2 个步骤 |

> 口径澄清：**未执行过的 SOP 编辑不留快照**（避免堆积无用版本）。自验第一轮因顺序为「改后跑」误判为 FAIL，按「建→跑→改」重测通过。

### 2.3 M3 生活域
| 项 | 结论 | 证据 |
|---|---|---|
| 计数型累加 | PASS | 2+1 → value=3 |
| 计数型达标标记 done | PASS | done=true |
| 打卡型重复打卡幂等 | PASS | code=14002「今日已打卡，无需重复」，值仍为 1 |
| streak 计算 | PASS | streak=1 |
| 快捷记录解析 | PASS | 「喝水 2」→ metricId=77, value=2 |
| **零建表** | PASS | migration 目录仅 V1-V4/V10/V15/V16，无 life 迁移；习惯落 `sys_metric_def`(dimension=life) |

> 口径澄清：快捷记录里的习惯名**必须是已定义习惯**；未定义返回 14003（刻意防乱建指标）。已写入 `m3-life-domain.md`。

### 2.4 集成与安全
| 项 | 结论 | 证据 |
|---|---|---|
| dashboard 契约字段齐全 | PASS | work/life/sop/today 全在 |
| dashboard 数据为真值而非 0 | PASS | life 5/7 打卡、sop.top 3 条、streak=3 |
| checkinDone ≤ checkinTotal | PASS | 5/7 |
| sop.top 不超过 3 条 | PASS | 3 |
| **sopHints 阈值命中** | PASS | 同名任务完成 3 次 → `count=3` |
| 无 token 访问被拒 | PASS | dashboard / 任务列表均 401「未登录」 |

### 2.5 环境级亲验
| 项 | 结论 | 证据 |
|---|---|---|
| **空库一键起（全量迁移）** | PASS | 新建 `personal_os_fresh` → Flyway V1,V2,V3,V4,V10,V15,V16 共 7 个迁移 `success=1`，建表 12 张，登录 200 |
| 前端 type-check / build | PASS | 均 exit 0 |
| 四页 UI 渲染（登录后实拍） | PASS | 首页 / 工作台 / SOP 库 / 生活页，与过审原型一致（截图见验收消息） |

## 3. 未覆盖项（诚实标注）

| 项 | 原因 | 建议 |
|---|---|---|
| 并发写入、竞态 | QA 429 失败，本轮未测 | 429 重置后补 QA 复测 |
| 跨用户越权 | 当前库仅 admin 一个用户 | 需先造第二个用户 |
| 超长字符串 / 负数 / 极端值 | 同上 | 同上 |
| UI 交互级 e2e（点击打卡、拖拽排序等） | 本轮只验到页面渲染 + 接口链路 | 同上 |
| Redis 会话 | P1 决策为不引入，内存会话 | 无需测 |

## 4. 交小杜验收的清单（请逐条勾）

- [ ] 首页三卡显示真实数据，点击能进对应模块
- [ ] 工作台：15 秒内只输标题+回车建成一条任务
- [ ] 工作台：三 Tab（今日/全部/逾期）切换正确，逾期任务有红标且不进今日
- [ ] 工作台：任务可打标签、可改期、可重开（done 点回 todo 有二次确认）
- [ ] SOP 库：能建 SOP、步骤可增删排序、执行模式逐步打勾、结束后统计更新
- [ ] SOP 库：跑过一次的 SOP 编辑后能在版本时间线回看旧内容
- [ ] 生活页：打卡型大圆钮 / 计数型大数字+进度条可用，重复打卡提示「今日已打卡」
- [ ] 生活页：快捷记录「喝水 2」弹出确认卡，确认后落库
- [ ] 明暗主题切换正常、390px 移动端无破版
- [ ] 整体：登录态下刷新页面不掉登录（内存会话，重启后端会掉）

## 5. 验收通过后的收尾动作（由 RIce 执行）

1. 四份模块文档状态 `dev` → `done`，更新 `personal-os-design.md` 实施进度
2. 清空演示数据（当前 4 任务 / 2 习惯 / 2 SOP），恢复干净库供正式使用
3. 429 重置后补 QA 复测（并发 / 越权 / 边界对抗 / UI e2e），结果追加进本报告
