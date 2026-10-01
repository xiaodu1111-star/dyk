package com.xiaodu.personalos.work;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaodu.personalos.common.util.PeriodUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * M1 工作域任务「边界与回归」补充测试（QA 独立验证，不复用工程师的用例）。
 *
 * <p>覆盖工程师 {@code M1TaskFlowTest} 未触及或断言不足的边界：
 * PUT 全量覆盖置空语义 / 状态机终态与回退 / 同状态幂等 / 分页截断 /
 * view 非法兜底 / view×status 交集 / 跨用户隔离 / 删除连带清标签 /
 * 活动流精确计数 / 标签 id 不存在返回业务码而非 500。</p>
 *
 * <p>数据用 UUID 后缀隔离，结尾用 JdbcTemplate 物理清理，可重复运行。</p>
 *
 * @author Edward (QA)
 */
@SpringBootTest(properties = "server.port=8092")
@AutoConfigureMockMvc
class M1TaskEdgeTest {

    /** MySQL 可接受的日期时间格式。 */
    private static final DateTimeFormatter SQL_DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** admin 用户 id（登录响应里带，用于构造隔离数据时交叉校验）。 */
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ================================================================
    // 用例 1：PUT 全量覆盖 —— 传 null 必须真的把列置 NULL（回归工程师修过的 bug）
    // ================================================================

    @Test
    void fullUpdateClearsNullableFieldsToNull() throws Exception {
        String token = loginAndGetToken();
        String title = "m1覆盖-" + suffix();
        long id = createTaskWithBody(token, Map.of(
                "title", title,
                "description", "原始描述",
                "priority", 3,
                "dueAt", "2026-12-31 09:00:00",
                "estimateMin", 45));
        try {
            // 前置断言：字段确实写入了
            assertEquals("原始描述", queryString("SELECT description FROM work_task WHERE id = ?", id));
            assertNotNull(queryObject("SELECT due_at FROM work_task WHERE id = ?", id));
            assertEquals(45, queryInt("SELECT estimate_min FROM work_task WHERE id = ?", id));

            // 全量覆盖：description / dueAt / estimateMin 全传 null
            Map<String, Object> payload = new HashMap<>();
            payload.put("title", title + "-改");
            payload.put("description", null);
            payload.put("dueAt", null);
            payload.put("estimateMin", null);
            payload.put("priority", 1);
            // 注意：Map.of 不接受 null，故用 HashMap 序列化（ObjectMapper 默认保留 null key）
            mockMvc.perform(put("/api/work/tasks/{id}", id)
                            .header("token", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payload)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));

            assertNull(queryObject("SELECT description FROM work_task WHERE id = ?", id),
                    "PUT 传 null 后 description 应为 NULL（全量覆盖语义）");
            assertNull(queryObject("SELECT due_at FROM work_task WHERE id = ?", id),
                    "PUT 传 null 后 due_at 应为 NULL");
            assertNull(queryObject("SELECT estimate_min FROM work_task WHERE id = ?", id),
                    "PUT 传 null 后 estimate_min 应为 NULL");
            assertEquals(1, queryInt("SELECT priority FROM work_task WHERE id = ?", id),
                    "PUT 后 priority 应被覆盖为 1");
        } finally {
            cleanTask(id);
        }
    }

    // ================================================================
    // 用例 2：done → abandoned 拒绝（10002）；done → todo 成功
    // ================================================================

    @Test
    void doneToAbandonedRejectedAndDoneToTodoAllowed() throws Exception {
        String token = loginAndGetToken();
        long id = createTask(token, "m1终态-" + suffix());
        try {
            changeStatusExpect(token, id, "done", 0);
            changeStatusExpect(token, id, "abandoned", 10002); // done 不能转 abandoned
            changeStatusExpect(token, id, "todo", 0);           // 合法回退
        } finally {
            cleanTask(id);
        }
    }

    // ================================================================
    // 用例 3：abandoned 严格终态 —— 到 todo/doing/done 全 10002
    // ================================================================

    @Test
    void abandonedTerminalStateIrreversible() throws Exception {
        String token = loginAndGetToken();
        long id = createTask(token, "m1不可逆-" + suffix());
        try {
            changeStatusExpect(token, id, "abandoned", 0);
            changeStatusExpect(token, id, "todo", 10002);
            changeStatusExpect(token, id, "doing", 10002);
            changeStatusExpect(token, id, "done", 10002);
            assertEquals("abandoned", queryString("SELECT status FROM work_task WHERE id = ?", id),
                    "所有非法流转后任务应仍为 abandoned");
        } finally {
            cleanTask(id);
        }
    }

    // ================================================================
    // 用例 4：同状态幂等 —— todo→todo 返回 0，且库内容不被破坏
    // ================================================================

    @Test
    void sameStatusIsIdempotentAndDoesNotCorruptRow() throws Exception {
        String token = loginAndGetToken();
        String title = "m1幂等-" + suffix();
        long id = createTask(token, title);
        try {
            String beforeStatus = queryString("SELECT status FROM work_task WHERE id = ?", id);
            Object beforeDoneAt = queryObject("SELECT done_at FROM work_task WHERE id = ?", id);

            changeStatusExpect(token, id, "todo", 0); // 同状态

            assertEquals(beforeStatus, queryString("SELECT status FROM work_task WHERE id = ?", id),
                    "同状态流转不应改变 status");
            assertEquals(beforeDoneAt, queryObject("SELECT done_at FROM work_task WHERE id = ?", id),
                    "同状态流转不应改动 done_at");
            assertEquals(title, queryString("SELECT title FROM work_task WHERE id = ?", id),
                    "同状态流转不应破坏其他列");
        } finally {
            cleanTask(id);
        }
    }

    // ================================================================
    // 用例 5：分页边界 —— size=1000 截断为 100；page<=0 纠正为 1
    // ================================================================

    @Test
    void paginationSizeCappedAndPageFloor() throws Exception {
        String token = loginAndGetToken();
        String tag = suffix();
        List<Long> ids = new ArrayList<>();
        final int count = 105;
        try {
            for (int i = 0; i < count; i++) {
                ids.add(createTask(token, "m1分页-" + tag + "-" + i));
            }

            // size=1000 应被截断为 100；返回的 size 字段应为 100，records 至多 100
            MvcResult r = mockMvc.perform(get("/api/work/tasks")
                            .header("token", token)
                            .param("view", "all")
                            .param("size", "1000"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andReturn();
            JsonNode data = objectMapper.readTree(readBody(r)).path("data");
            assertEquals(100, data.path("size").asLong(), "size=1000 应被截断为 100");
            assertTrue(data.path("records").size() <= 100, "单页记录数不应超过 100");

            // page=0 应被纠正为 1
            MvcResult r0 = mockMvc.perform(get("/api/work/tasks")
                            .header("token", token)
                            .param("view", "all")
                            .param("page", "0")
                            .param("size", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andReturn();
            assertEquals(1, objectMapper.readTree(readBody(r0)).path("data").path("page").asLong(),
                    "page=0 应被纠正为 1");

            // page=-3 应被纠正为 1
            MvcResult rn = mockMvc.perform(get("/api/work/tasks")
                            .header("token", token)
                            .param("view", "all")
                            .param("page", "-3")
                            .param("size", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andReturn();
            assertEquals(1, objectMapper.readTree(readBody(rn)).path("data").path("page").asLong(),
                    "page=-3 应被纠正为 1");
        } finally {
            for (Long id : ids) {
                cleanTask(id);
            }
        }
    }

    // ================================================================
    // 用例 6：view 非法/缺失兜底为 all（不报错）
    // ================================================================

    @Test
    void illegalOrMissingViewFallsBackToAll() throws Exception {
        String token = loginAndGetToken();
        String tag = suffix();
        long id = createTask(token, "m1兜底-" + tag);
        try {
            // 构造一条已完成任务，all 视图能查到它（today 视图查不到）
            changeStatusExpect(token, id, "done", 0);

            // view=banana → 兜底 all（应能查到已完成任务）
            assertTrue(containsInView(token, "banana", id), "view=banana 应兜底 all 口径");
            // 不传 view → 兜底 all
            assertTrue(containsInViewWithoutParam(token, id), "缺失 view 应兜底 all 口径");
        } finally {
            cleanTask(id);
        }
    }

    // ================================================================
    // 用例 7：view × status 交集 —— today&status=done 空；all&status=done 有
    // ================================================================

    @Test
    void viewAndStatusIntersection() throws Exception {
        String token = loginAndGetToken();
        String tag = suffix();
        long id = createTask(token, "m1交集-" + tag);
        try {
            changeStatusExpect(token, id, "done", 0);

            // today 口径只含未完成 → 与 status=done 交集为空
            assertTrue(!containsInViewWithStatus(token, "today", "done", id),
                    "view=today&status=done 应为空（today 只含未完成）");
            // all + status=done 应能返回已完成任务
            assertTrue(containsInViewWithStatus(token, "all", "done", id),
                    "view=all&status=done 应能返回已完成任务");
        } finally {
            cleanTask(id);
        }
    }

    // ================================================================
    // 用例 8：跨用户隔离 —— 他人任务列表不可见、详情/PUT/DELETE 均 10001
    // ================================================================

    @Test
    void crossUserIsolationReturns10001() throws Exception {
        String token = loginAndGetToken();
        String tag = suffix();
        long foreignId = insertForeignTask("m1他人任务-" + tag);
        try {
            // 列表看不到
            assertTrue(!containsInView(token, "all", foreignId), "不应看到他用户任务");
            // 详情 10001
            mockMvc.perform(get("/api/work/tasks/{id}", foreignId).header("token", token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(10001));
            // PUT 10001
            mockMvc.perform(put("/api/work/tasks/{id}", foreignId)
                            .header("token", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("title", "劫持"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(10001));
            // DELETE 10001
            mockMvc.perform(delete("/api/work/tasks/{id}", foreignId).header("token", token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(10001));

            // 确认未被篡改 / 未被删
            assertEquals("m1他人任务-" + tag, queryString("SELECT title FROM work_task WHERE id = ?", foreignId));
            assertEquals(0, queryInt("SELECT deleted FROM work_task WHERE id = ?", foreignId));
        } finally {
            cleanTask(foreignId);
        }
    }

    // ================================================================
    // 用例 9：删除连带清理标签关联 + use_count 回减
    // ================================================================

    @Test
    void deleteClearsTagRelsAndDecrementsUseCount() throws Exception {
        String token = loginAndGetToken();
        String tag = suffix();
        long tagId1 = createTag(token, "m1删标a-" + tag);
        long tagId2 = createTag(token, "m1删标b-" + tag);
        long taskId = 0L;
        try {
            taskId = createTaskWithBody(token, Map.of(
                    "title", "m1删标任务-" + tag,
                    "tagIds", new long[]{tagId1, tagId2}));

            assertEquals(2, countRel(taskId), "创建后应有 2 条标签关联");
            assertEquals(1, queryInt("SELECT use_count FROM sys_tag WHERE id = ?", tagId1));
            assertEquals(1, queryInt("SELECT use_count FROM sys_tag WHERE id = ?", tagId2));

            // 删除
            mockMvc.perform(delete("/api/work/tasks/{id}", taskId).header("token", token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));

            assertEquals(0, countRel(taskId), "删除后标签关联应清空（物理表无逻辑删）");
            assertEquals(0, queryInt("SELECT use_count FROM sys_tag WHERE id = ?", tagId1),
                    "标签 a use_count 应回减到 0");
            assertEquals(0, queryInt("SELECT use_count FROM sys_tag WHERE id = ?", tagId2),
                    "标签 b use_count 应回减到 0");
        } finally {
            if (taskId > 0) {
                cleanTask(taskId);
            }
            jdbcTemplate.update("DELETE FROM sys_tag_rel WHERE tag_id IN (?, ?) AND biz_type='task'", tagId1, tagId2);
            jdbcTemplate.update("DELETE FROM sys_tag WHERE id IN (?, ?)", tagId1, tagId2);
        }
    }

    // ================================================================
    // 用例 10：活动流精确性 —— todo→doing→done 写 1 条；回退不写；再 done 再写 1 条
    // ================================================================

    @Test
    void activityLogWrittenOncePerFirstDoneEntry() throws Exception {
        String token = loginAndGetToken();
        String title = "m1活动流-" + suffix();
        long id = createTask(token, title);
        try {
            changeStatusExpect(token, id, "doing", 0);
            changeStatusExpect(token, id, "done", 0);
            assertEquals(1, countActivityLog(title), "首次 done 应写 1 条活动流");

            // 回退不新增
            changeStatusExpect(token, id, "todo", 0);
            assertEquals(1, countActivityLog(title), "回退不应新增活动流");
            assertNull(queryObject("SELECT done_at FROM work_task WHERE id = ?", id), "回退后 done_at 应清空");

            // 再次完成 → 再写 1 条（共 2），done_at 被重写
            changeStatusExpect(token, id, "done", 0);
            assertEquals(2, countActivityLog(title), "第二次 done 应再写 1 条");
            assertNotNull(queryObject("SELECT done_at FROM work_task WHERE id = ?", id), "再次完成后 done_at 应非空");
        } finally {
            cleanTask(id);
            jdbcTemplate.update("DELETE FROM act_activity_log WHERE dimension='work' AND title = ?", title);
        }
    }

    // ================================================================
    // 用例 11：tagIds 含不存在标签 id → 业务码（15101 或 10001），不应 500
    // ================================================================

    @Test
    void nonExistentTagIdReturnsBizCodeNotServerError() throws Exception {
        String token = loginAndGetToken();
        long bogusTagId = 888_888_888L;
        MvcResult r = mockMvc.perform(post("/api/work/tasks")
                        .header("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "m1坏标签-" + suffix(),
                                "tagIds", new long[]{bogusTagId}))))
                .andExpect(status().isOk())
                .andReturn();
        int code = objectMapper.readTree(readBody(r)).path("code").asInt();
        assertTrue(code == 15101 || code == 10001,
                "不存在标签 id 应返回 15101(TagService) 或 10001，实际=" + code);
    }

    // ================================================================
    // 用例 12：契约 —— POST 返回 data 为 id（数字），列表 PageResult 字段名
    // ================================================================

    @Test
    void contractShapeChecks() throws Exception {
        String token = loginAndGetToken();
        // POST 返回 data = id（数字）
        MvcResult created = mockMvc.perform(post("/api/work/tasks")
                        .header("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", "m1契约-" + suffix()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.msg").exists())
                .andReturn();
        JsonNode cdata = objectMapper.readTree(readBody(created)).path("data");
        assertTrue(cdata.isNumber() || cdata.isObject(),
                "POST 的 data 应为 id（数字）或 {id}，实际节点类型=" + cdata.getNodeType());
        long id = cdata.isNumber() ? cdata.asLong() : cdata.path("id").asLong();
        assertTrue(id > 0, "POST 应返回有效 id");
        try {
            // GET 列表 PageResult 字段名 records/total/page/size
            MvcResult list = mockMvc.perform(get("/api/work/tasks")
                            .header("token", token)
                            .param("view", "all")
                            .param("size", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andReturn();
            JsonNode data = objectMapper.readTree(readBody(list)).path("data");
            assertTrue(data.has("records"), "PageResult 应含 records");
            assertTrue(data.has("total"), "PageResult 应含 total");
            assertTrue(data.has("page"), "PageResult 应含 page");
            assertTrue(data.has("size"), "PageResult 应含 size");
        } finally {
            cleanTask(id);
        }
    }

    // ================================================================
    // 用例 13：日期格式契约 —— 后端接受 'yyyy-MM-dd HH:mm:ss'（前端发送前统一转此格式）
    // ================================================================
    //
    // 口径说明（团队拍板）：
    //   后端全局 LocalDateTime 反序列化（common/config/JacksonConfig）要求精确匹配
    //   'yyyy-MM-dd HH:mm:ss'，不容 `T` 分隔的 ISO 串。
    //   datetime-local 控件产出 `2026-12-31T09:00`（ISO T），仅用于喂控件本身；
    //   前端在调用 API 前统一用 dayjs 转成 'yyyy-MM-dd HH:mm:ss' 再发送
    //   （WorkQuickAdd / WorkTaskDrawer / index.handleReschedule 三处）。
    //   故本用例断言：后端接受空格格式。
    //   （原断言「ISO T 应被接受」与选定修复路线冲突，已按口径改写。）

    @Test
    void spaceSeparatedDateTimeFromFrontendIsAccepted() throws Exception {
        String token = loginAndGetToken();
        // 前端发送前会把 datetime-local 的 ISO T 串转成 'yyyy-MM-dd HH:mm:ss'
        MvcResult r = mockMvc.perform(post("/api/work/tasks")
                        .header("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "title", "m1日期格式-" + suffix(),
                                "dueAt", "2026-12-31 09:00:00"))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode root = objectMapper.readTree(readBody(r));
        int code = root.path("code").asInt();
        long id = 0L;
        if (code == 0) {
            JsonNode data = root.path("data");
            id = data.isNumber() ? data.asLong() : data.path("id").asLong();
        }
        try {
            assertEquals(0, code,
                    "前端发送的 'yyyy-MM-dd HH:mm:ss' 应被后端接受，实际 code=" + code + "，响应=" + readBody(r));
            assertTrue(id > 0, "创建成功应返回有效 id");
            // 落库值精确（用 DATE_FORMAT 取字符串，避免 JDBC 类型差异）
            assertEquals("2026-12-31 09:00:00", jdbcTemplate.queryForObject(
                            "SELECT DATE_FORMAT(due_at, '%Y-%m-%d %H:%i:%s') FROM work_task WHERE id = ?",
                            String.class, id),
                    "dueAt 应精确落库");
        } finally {
            if (id > 0) {
                cleanTask(id);
            }
        }
    }

    // ---- 辅助方法 ------------------------------------------------------

    private String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private String loginAndGetToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return objectMapper.readTree(readBody(result)).path("data").path("token").asText();
    }

    /** 仅标题创建，返回 id。 */
    private long createTask(String token, String title) throws Exception {
        return createTaskWithBody(token, Map.of("title", title));
    }

    /** 指定 body 创建（复用调用方 token，避免单点登录把旧 token 顶掉），返回 id。 */
    private long createTaskWithBody(String token, Map<String, Object> body) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/work/tasks")
                        .header("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode root = objectMapper.readTree(readBody(result));
        int code = root.path("code").asInt();
        assertEquals(0, code, "创建任务失败，响应=" + readBody(result));
        JsonNode data = root.path("data");
        return data.isNumber() ? data.asLong() : data.path("id").asLong();
    }

    private long createTag(String token, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/system/tags")
                        .header("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", name, "scope", "work"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        JsonNode data = objectMapper.readTree(readBody(result)).path("data");
        return data.isNumber() ? data.asLong() : data.path("id").asLong();
    }

    private void changeStatusExpect(String token, long id, String status, int expectCode) throws Exception {
        mockMvc.perform(put("/api/work/tasks/{id}/status", id)
                        .header("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", status))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(expectCode));
    }

    /** 指定 view 首页（size=100）是否包含某任务。 */
    private boolean containsInView(String token, String view, long taskId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/work/tasks")
                        .header("token", token)
                        .param("view", view)
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return recordsContainId(result, taskId);
    }

    /** 不传 view 参数，是否包含某任务。 */
    private boolean containsInViewWithoutParam(String token, long taskId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/work/tasks")
                        .header("token", token)
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return recordsContainId(result, taskId);
    }

    /** 指定 view + status 是否包含某任务。 */
    private boolean containsInViewWithStatus(String token, String view, String status, long taskId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/work/tasks")
                        .header("token", token)
                        .param("view", view)
                        .param("status", status)
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return recordsContainId(result, taskId);
    }

    private boolean recordsContainId(MvcResult result, long taskId) throws Exception {
        JsonNode records = objectMapper.readTree(readBody(result)).path("data").path("records");
        for (JsonNode node : records) {
            if (node.path("id").asLong() == taskId) {
                return true;
            }
        }
        return false;
    }

    /** 直插一条属于他人的任务（user_id=99999），返回 id。 */
    private long insertForeignTask(String title) {
        jdbcTemplate.update(
                "INSERT INTO work_task (user_id, title, status, priority, actual_min, sort_no, deleted) "
                        + "VALUES (99999, ?, 'todo', 2, 0, 0, 0)", title);
        return jdbcTemplate.queryForObject(
                "SELECT id FROM work_task WHERE user_id = 99999 AND title = ? ORDER BY id DESC LIMIT 1",
                Long.class, title);
    }

    private int countRel(long taskId) {
        Integer c = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_tag_rel WHERE biz_type='task' AND biz_id = ?", Integer.class, taskId);
        return c == null ? 0 : c;
    }

    private int countActivityLog(String title) {
        Integer c = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM act_activity_log WHERE dimension='work' AND biz_type='task_done' AND title = ?",
                Integer.class, title);
        return c == null ? 0 : c;
    }

    private String readBody(MvcResult result) throws Exception {
        return result.getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private String queryString(String sql, Object arg) {
        return jdbcTemplate.queryForObject(sql, String.class, arg);
    }

    private Integer queryInt(String sql, Object arg) {
        return jdbcTemplate.queryForObject(sql, Integer.class, arg);
    }

    private Object queryObject(String sql, Object arg) {
        return jdbcTemplate.queryForObject(sql, Object.class, arg);
    }

    /** 物理清理任务及其标签关联、活动流。 */
    private void cleanTask(long id) {
        jdbcTemplate.update("DELETE FROM sys_tag_rel WHERE biz_type='task' AND biz_id = ?", id);
        jdbcTemplate.update("DELETE FROM work_task WHERE id = ?", id);
    }
}
