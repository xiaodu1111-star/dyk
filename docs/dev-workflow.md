# Personal OS · 开发流程与并行协作规范

> 版本：v1.0 · 2026-10-01 · 作者：RIce（软件总设计师）
> 定稿人：小杜（2026-10-01 口头定流程，本文落地）
> 性质：**铁律**。所有窗口（包括零上下文的新智能体）开工前必须读完本文。

---

## 0. 角色

| 角色 | 谁 | 职责 |
|---|---|---|
| 软件总设计师 | **RIce** | 需求报告、功能拆分、模块契约、共享资产维护、合并与集成验证 |
| 需求确认人 / 验收人 | **小杜** | 需求报告确认、最终验收。没他点头，任何阶段不进下一步 |
| 并行开发窗口 | 小杜开的多个 WorkBuddy 窗口 | 按模块文档开发 + 自测 |
| 本窗口执行团队 | 许清楚 / 高见远 / 寇豆码 / 严过关 | 本窗口的开发与测试力量、集成回归 |

## 1. 流程总览（顺序不可乱）

```
① 需求报告 ──→ ② 小杜确认 ──→ ③ 功能拆分（一模块一文档）
                                    │
                    ┌───────────────┼───────────────┐
                    ▼               ▼               ▼
              ④ 并行开发窗口A   并行开发窗口B    并行开发窗口C
                    │               │               │
                    └───────┬───────┴───────────────┘
                            ▼
                ⑤ 测试完成 ──→ ⑥ 小杜验收 ──→ ⑦ 更新主文档
```

| 步 | 产出物 | 谁做 | 通过标准 |
|---|---|---|---|
| ① 需求报告 | `docs/requirements/<期>-requirements.md` | RIce | 覆盖目标、范围、用户故事、非目标、开放决策点 |
| ② 需求确认 | 小杜一句"没问题" | 小杜 | 需求报告里所有开放决策点全部有答案 |
| ③ 功能拆分 | `docs/modules/<模块>.md` × N | RIce | 每个文档**自包含**：零上下文智能体读完即可开工 |
| ④ 并行开发 | 各模块代码 + 自测 | 各窗口 | 边界内文件、构建绿、模块文档验收清单全过 |
| ⑤ 测试 | 测试报告 | 各窗口 + 严过关 | 单测/集成/手测清单全绿 |
| ⑥ 验收 | 小杜逐项验收 | 小杜 | 验收标准逐条勾掉 |
| ⑦ 更新文档 | 主文档同步 + 模块文档标记 accepted | RIce | 只有这一步之后才动 `docs/` 主文档 |

## 2. 文档体系

```
docs/
├── personal-os-design.md     # 总设计（主文档，仅 RIce 维护）
├── backend-architecture.md  # 后端架构（主文档，仅 RIce 维护）
├── ui-design-system.md       # UI 设计系统（主文档，仅 RIce 维护）
├── dev-workflow.md           # 本规范
├── requirements/             # 需求报告，一期一份
│   └── p1-requirements.md
└── modules/                  # 模块拆分文档，一模块一份（并行开发的输入）
    ├── work-domain.md
    └── ...
```

**模块文档必备章节（模板，缺一不算合格拆分）：**
1. 模块概览（一句话价值）与用户故事
2. 范围内 / 明确不做
3. 环境速查（§5 已有，模块特殊点补充）
4. 数据库：表 DDL、**分配到的 Flyway 版本号区间**、种子数据
5. 后端：包路径、接口契约表（方法/路径/入参/出参/错误码）、事务边界、三大引擎接入点
6. 前端：页面与路由、组件、API 客户端、样式约束（沿用 iOS 26 玻璃，见 ui-design-system.md）
7. **文件边界白名单**（能新增/修改的路径 + 禁止清单）
8. 测试要求
9. 验收标准（可勾选清单）
10. 状态字段（draft → dev → test → accepted → documented）

## 3. 多窗口并行安全规则（本文的核心）

多窗口 = 同一个 `F:\dyk` 工作区 + 同一个 `.git`。**并行安全不靠自觉，靠边界。**

### 3.1 文件路径边界（各窗口只动自己的地盘）

| 模块 | 后端包 | 前端目录 | API 前缀 | 前端路由 |
|---|---|---|---|---|
| 工作域（任务） | `com.xiaodu.personalos.work` | `src/views/work/`、`src/api/work.ts` | `/api/work` | `/work` |
| SOP 库 | `com.xiaodu.personalos.sop` | `src/views/sop/`、`src/api/sop.ts` | `/api/sop` | `/sop` |
| 学习域 | `...learn` | `src/views/learn/` … | `/api/learn` | `/learn` |
| 运动域 | `...fit` | `src/views/fit/` … | `/api/fit` | `/fit` |
| 理财域 | `...finance` | `src/views/finance/` … | `/api/finance` | `/finance` |
| 生活域 | `...life` | `src/views/life/` … | `/api/life` | `/life` |

### 3.2 共享资产管制清单（**只准 RIce 改**，其他窗口禁止碰）

| 资产 | 原因 |
|---|---|
| `pom.xml`、`application*.yml` | 依赖与配置全局生效，改坏全体瘫痪 |
| `common/**`（R/ErrorCode/异常/工具/Sa-Token/CORS） | 全局契约 |
| Flyway 迁移文件版本号 | 撞号 = 启动失败 |
| `src/router/index.ts`、`src/main.ts`、`src/styles/**`、`src/layouts/**` | 全局装配 |
| `docs/` 全部主文档 | 验收通过后才更新（流程第 ⑦ 步） |
| `.gitignore`、`README-dev.md` | 全局 |
| **git `main` 分支的 reset / checkout / 强推类操作** | 会毁掉其他窗口未提交的半成品 |

窗口开发中发现需要改共享资产（如要加依赖、加路由）：**在模块文档里写"共享资产变更申请"章节，停下等 RIce 改完再继续**，不要自己动手。

### 3.3 Flyway 版本号预分配（防止撞车）

| 域 | 版本号区间 |
|---|---|
| system / 三大引擎（标签·指标·活动流） | V2 – V9 |
| 工作域（任务/计划） | V10 – V14 |
| SOP 库 | V15 – V19 |
| 学习域 | V20 – V29 |
| 运动域 | V30 – V39 |
| 理财域 | V40 – V49 |
| 生活域 | V50 – V59 |
| 跨域 / RIce 修订 | V90 – V99 |

V1 已用（sys_user）。每个模块文档写死自己分到的号段，窗口内自增，**不得越界**。

### 3.4 错误码分段（沿用总设计，不得占别人的段）

`0` 成功 / `400·401·403·404·500` 通用 / `1000-1099` 通用业务 / `10000+` work / `11000+` learn / `12000+` fit / `13000+` finance / `14000+` life / `15000+` system

### 3.5 命名冲突预防

- Vue/Pinia：组件 `name`、store `id` 加模块前缀（如 `workTaskStore`）
- 非 scoped 的全局 CSS 类加模块前缀；scoped 样式随意
- 枚举 / 常量 / DTO 类名带上模块语义，不要叫 `Type`、`Status` 这种裸名
- 菜单 / i18n key（未来引入时）同理

### 3.6 git 操作规则（同一 checkout 生存法则）

1. **只 add 自己模块的路径**：`git add personal-os-server/src/main/java/com/xiaodu/personalos/work/ personal-os-web/src/views/work/ ...`
2. **禁止 `git add -A`、`git commit -a`** —— 会把其他窗口写到一半的文件一起提交
3. **禁止 `git checkout .`、`git reset --hard`、切分支** —— 会清掉其他窗口的未提交工作
4. `.git/index.lock` 报错 = 另一窗口正在 commit，等几秒重试
5. push 被拒就 `git pull --rebase` 再 push；push 后跑 `git status -sb`，出现 `[gone]` 见 §5 沙箱坑
6. 一个模块收口 = commit + push，信息格式：`<模块>: <做了什么>`（如 `sop: SOP 库表结构与 CRUD`）

### 3.7 运行端口分配（并行时各自换端口）

| 窗口 | 后端 | 前端 dev |
|---|---|---|
| 窗口 1（本窗口） | 8080 | 5173 |
| 窗口 2 | 8081 | 5175 |
| 窗口 3 | 8082 | 5176 |
| 窗口 4 | 8083 | 5177 |

- 后端：`--server.port=8081` **必须显式指定**（宿主环境变量会抢 `server.port`）
- 新端口要进 CORS 白名单：报给 RIce，由 RIce 改 `application.yml`（共享资产）
- **尽量同一时间只有一个窗口跑 Flyway 启动**（多窗口同时首启会有迁移锁竞争，能避开就避开）

## 4. 环境速查（零上下文窗口必读）

每次 Bash 调用第一行：`export PATH="/usr/bin:/bin:$PATH"`（本机 PATH 残缺）。

| 项 | 值 / 用法 |
|---|---|
| JDK | `export JAVA_HOME='C:\Users\Lenovo\.jdks\temurin-24'`。**默认 java 是 1.8，跑不了本项目** |
| Maven | **只用** `/d/maven/bin/mvn-bash`（原生 `mvn` 在 Git Bash 下必挂） |
| MySQL | 26.7.0，`localhost:3306`，库 `personal_os`，账号密码见 `README-dev.md` |
| npm | 慢，**加** `--registry=https://registry.npmmirror.com` |
| 前端 dev | `npm run dev -- --host 127.0.0.1 --port 5173`（Vite 默认只绑 IPv6，必须 `--host 127.0.0.1`） |
| Redis | **未安装**，当前 Sa-Token 走内存会话，别引 Redis 连接 |
| git 身份 | 全局已配（码农xiaokai），remote `origin = github.com/xiaodu1111-star/dyk.git`，分支 `main` |
| 默认账号 | `admin` / `admin123` |
| UI 基调 | iOS 26 液态玻璃，规范见 `docs/ui-design-system.md`，登录页与首页是参照实现 |

**沙箱 git 坑**（2026-10-01 实测）：沙箱内 `fetch`/`pull` 对 `.git/refs/remotes/` 的写入会静默丢失 → `git status -sb` 显示 `[gone]`。修复：让 RIce 处理（非沙箱写入该文件）。fetch/pull 之后**必须**检查一次 `git status -sb`。

## 5. 状态机与文档更新纪律

```
模块状态：draft（RIce 写拆分）→ dev（窗口开发中）→ test（测试中）
        → accepted（小杜验收通过）→ documented（主文档已同步）
```

- `draft/dev/test` 阶段**禁止**修改 `docs/` 主文档（那是第 ⑦ 步的事）
- 模块文档自身的状态字段由该窗口与 RIce 共同维护（窗口改 dev/test，RIce 改 accepted/documented）
- 验收通过后 RIce 负责：主文档同步、给模块文档盖 `accepted`、必要时出增量架构修订

## 6. 验收标准（模块文档必须给出可勾选项）

1. 构建绿：后端 `mvn-bash clean package -DskipTests` exit 0；前端 `npm run type-check` + `npm run build` exit 0
2. 接口契约与模块文档一致（路径/入参/出参/错误码逐条对）
3. Flyway 迁移可重复执行（新库空库能一键起）
4. 界面符合 iOS 26 玻璃基调（明暗两套 + 移动端）
5. 无越界：`git diff --name-only` 不出现 §3.2 清单里的任何路径
