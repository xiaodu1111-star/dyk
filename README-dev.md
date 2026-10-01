# Personal OS · 开发说明（README-dev）

> 自托管「个人操作系统」。**当前版本只交付「登录闭环」**（P0 第一刀），主页面业务功能后续迭代。
> 技术栈：Vue 3 + TypeScript + Vite / Spring Boot 3.5 + MyBatis-Plus / MySQL 8（**本期不使用 Redis**）。

---

## 1. 目录结构

```
F:\dyk\
├── docs\                    设计文档（不要改动）
│   ├── personal-os-design.md
│   ├── backend-architecture.md
│   ├── class-diagram.mermaid
│   └── sequence-diagram.mermaid
├── personal-os-server\      Spring Boot 后端
├── personal-os-web\         Vue 3 前端
└── README-dev.md            本文件
```

后端包名：`com.xiaodu.personalos`。

---

## 2. 环境要求（本机已就绪）

| 项 | 版本 / 路径 | 备注 |
|---|---|---|
| JDK | Temurin **24.0.1**，`C:\Users\Lenovo\.jdks\temurin-24` | **本机默认 `java` 是 1.8，必须显式指定 JAVA_HOME** |
| Maven | 3.9.14，`D:\maven` | **不要用 `mvn`**，用包装脚本 `/d/maven/bin/mvn-bash` |
| MySQL | 8.x，`127.0.0.1:3306` | 账号 `root` / `123456`，库 `personal_os` 已建好 |
| Node | 22.22.2（npm 10.9.7） | `node` / `npm` 已在 PATH |
| Redis | **未安装** | 本期完全不用 Redis，未引入任何 Redis 依赖与配置 |

> 本机 Git Bash 的 PATH 残缺，外部命令（`dirname`/`ls` 等）需先 `export PATH="/usr/bin:/bin:$PATH"`。

---

## 3. 依赖版本（后端）

| 组件 | 版本 |
|---|---|
| Spring Boot parent | 3.5.16 |
| Java 编译目标 | 17（用 JDK24 编译出 17 字节码，`maven.compiler.release=17`） |
| MyBatis-Plus | 3.5.17（`mybatis-plus-spring-boot3-starter`，另引 `mybatis-plus-jsqlparser`） |
| Sa-Token | 1.46.0（`sa-token-spring-boot3-starter`，**未引入 sa-token-redis-jackson**） |
| Knife4j | 4.5.0（`knife4j-openapi3-jakarta-spring-boot-starter`） |
| SpringDoc | 2.8.6（显式覆盖，兼容 Spring Boot 3.5） |
| Flyway | `flyway-core` + `flyway-mysql` |
| Hutool | 5.8.47 |
| MySQL 驱动 | `com.mysql:mysql-connector-j` |
| BCrypt | `spring-security-crypto`（仅此一个，不含 spring-boot-starter-security） |
| Lombok | Boot BOM 自带，`provided` |
| MapStruct | **未引入**（登录场景不需要，且 JDK24 注解处理器有风险） |

---

## 4. 启动后端

```bash
export PATH="/usr/bin:/bin:$PATH"
cd /f/dyk/personal-os-server
export JAVA_HOME='C:\Users\Lenovo\.jdks\temurin-24'
/d/maven/bin/mvn-bash clean package -DskipTests

# 启动（默认 dev profile，端口 8080）
"$JAVA_HOME/bin/java" -jar target/personal-os-server-0.1.0.jar
```

首次启动时 Flyway 会执行 `V1__init_sys_user.sql` 建 `sys_user` 表；应用启动完成后
`DataInitializer` 会在表为空时播种默认账号。接口文档：<http://localhost:8080/doc.html>。

### 运行测试

```bash
export PATH="/usr/bin:/bin:$PATH"
export JAVA_HOME='C:\Users\Lenovo\.jdks\temurin-24'
export DB_PASSWORD=123456          # 测试直连本地 MySQL
cd /f/dyk/personal-os-server
/d/maven/bin/mvn-bash clean test   # 期望：Tests run: 7, Failures: 0, Errors: 0
```

`AuthControllerTest` 覆盖：登录成功 / 密码错误 / 用户不存在 / 未登录 / 带 token 取昵称 /
CORS 非白名单来源被拒 / 畸形 JSON 返回 400。

### 可覆盖的配置项（环境变量）

| 环境变量 | 默认值 | 说明 |
|---|---|---|
| `DB_HOST` | `127.0.0.1` | MySQL 主机 |
| `DB_PORT` | `3306` | MySQL 端口 |
| `DB_USER` | `root` | MySQL 用户名 |
| `DB_PASSWORD` | `123456` | MySQL 密码（**生产务必用环境变量注入，勿硬编码**） |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://127.0.0.1:5173` | CORS 来源白名单，逗号分隔 |
| `PROFILE` | `dev` | Spring profile |

---

## 5. 启动前端

```bash
cd F:\dyk\personal-os-web
npm install --registry=https://registry.npmmirror.com
npm run dev -- --host 127.0.0.1
```

打开 `http://127.0.0.1:5173/login`。

| 命令 | 作用 |
|---|---|
| `npm run dev` | 开发服务，热更新 |
| `npm run build` | 生产构建，产物在 `dist/` |
| `npm run preview` | 本地预览 `dist/` 构建产物 |
| `npm run type-check` | TypeScript 类型检查（`vue-tsc --noEmit`） |

### 两个必须注意的点

**1. `npm install` 必须加 `--registry=https://registry.npmmirror.com`。**
直连 npmjs.org 在这台机器上会跑到几乎零产出（实测 9 分钟无结果），换国内镜像后 6 分钟装完。
若想一劳永逸，可执行 `npm config set registry https://registry.npmmirror.com`。

**2. 启动要加 `--host 127.0.0.1`。**
Vite 默认只监听 IPv6 的 `::1`，此时 `http://127.0.0.1:5173` 连不上（用 `localhost` 也可能因解析到 IPv4 而失败）。
加 `--host 127.0.0.1` 才会绑定 IPv4。想同时被局域网其他设备访问，用 `--host 0.0.0.0`。

前端通过 Vite 代理把 `/api` 转发到 `http://localhost:8080`，因此**先起后端再起前端**。

---

## 5.1 常见问题

| 现象 | 原因 / 解法 |
|---|---|
| `127.0.0.1:5173` 打不开，但终端显示 `ready` | Vite 只绑了 IPv6，加 `--host 127.0.0.1` 重启 |
| 前端起来但登录报网络错误 | 后端没起，或不在 8080。检查 `http://127.0.0.1:8080/api/auth/login` |
| 后端启动抢错端口 / 启动失败 | 宿主环境注入了 `SERVER__PORT` / `SERVER__HOST`，启动参数显式加 `--server.port=8080` 覆盖。**不要用 `unset`**，会让某些终端丢失输出 |
| 登录返回 `code:401` | token 过期或未携带。清掉浏览器 localStorage 里的 `token` 重新登录 |
| MySQL 连接失败 `1045` | 检查 `DB_PASSWORD` 环境变量是否覆盖成了错误值 |

---

## 6. 默认账号

| 用户名 | 密码 | 昵称 | 城市 |
|---|---|---|---|
| `admin` | `admin123` | 小杜 | 嘉兴 |

> 由后端 `DataInitializer` 在 `sys_user` 表为空时用 BCrypt 现算写入，重复启动不会重复插入。

---

## 7. 接口速览

| 方法 | 路径 | 鉴权 | 说明 |
|---|---|---|---|
| POST | `/api/auth/login` | 否 | 登录，返回 token |
| POST | `/api/auth/logout` | 是 | 登出 |
| GET  | `/api/auth/me` | 是 | 当前用户信息 |

- token 传输：请求头 `token: xxx`（Sa-Token 默认名）。
- 统一响应体：`{"code":0,"msg":"ok","data":...}`；**HTTP 状态码恒为 200**，业务结果看 `code`。
- 关键错误码：`0` 成功 / `400` 参数错误 / `401` 未登录 / `403` 无权限 / `500` 服务端异常 /
  `15002` 用户不存在 / `15003` 用户名或密码错误。

命令行自测：

```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

curl -s http://localhost:8080/api/auth/me -H "token: <上一步拿到的 token>"
```

---

## 8. 本机环境注意事项（踩坑备忘）

1. **JAVA_HOME 必须指向 JDK24**：本机默认 `java` 是 1.8，Spring Boot 3.5 跑不起来。
2. **不要用 `mvn`**：用 `/d/maven/bin/mvn-bash`（脚本内部已固定 classworlds 与 JDK 路径）。
3. **Redis 未装**：本期禁用，未引入相关依赖；后续接入会话持久化/缓存时再引入。
4. **每行 Bash 先 `export PATH="/usr/bin:/bin:$PATH"`**，否则 `dirname`/`ls` 等命令找不到。
5. **JDK23+ 注解处理**：`javac` 默认不再自动运行 classpath 上的注解处理器，`pom.xml` 已显式
   配置 Lombok 的 `annotationProcessorPaths` 并加了 `-proc:full`。
6. **MyBatis-Plus 3.5.9+**：分页插件所需的 JSqlParser 已从 starter 剥离，需额外引入
   `mybatis-plus-jsqlparser`。
7. **CORS 安全**：来源白名单 **只能写显式 origin**（`personal-os.cors.allowed-origins`），
   严禁 `addAllowedOriginPattern("*")` 搭配 `allowCredentials(true)`——那等于允许任意站点携带凭证跨域调用。
8. **不要硬编码 DB 密码**：统一走 `${DB_PASSWORD:...}` 占位符，仓库已加 `.gitignore` 防误提交。
9. **端口被抢占**：若当前 shell 存在 `SERVER__PORT` / `SERVER__HOST` 环境变量（Agent 宿主可能注入，
   指向它自己的 MCP 代理端口），Spring 松散绑定会把它当成 `server.port` 从而抢错端口；
   启动前 `unset SERVER__PORT SERVER__HOST` 即可。

---

## 9. 数据库变更约定

- 所有表结构变更走 Flyway，新增脚本放 `personal-os-server/src/main/resources/db/migration/`，
  命名 `V{n}__xxx.sql`，**禁止手工改库**。
- 当前仅 `V1__init_sys_user.sql`（只建 `sys_user` 一张表）。
- 统一字段：`id`（自增主键）、`create_time`、`update_time`、`deleted`（逻辑删除，0 正常 / 1 删除）。
