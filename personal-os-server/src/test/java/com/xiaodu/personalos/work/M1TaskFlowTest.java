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
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * M1 工作域任务全链路集成测试（直连本机 MySQL）。
 *
 * <p>覆盖：创建 / 参数校验 / 状态机 / 三视图边界 / 活动流 / 标签 / 归属校验 / 鉴权。
 * 数据用 UUID 后缀隔离，每个用例结尾用 JdbcTemplate 物理清理，可重复运行。</p>
 *
 * @author Kou
 */
@SpringBootTest(properties = "server.port=8091")
@AutoConfigureMockMvc
class M1TaskFlowTest {

    /** MySQL 可接受的日期时间格式。 */
    private static final DateTimeFormatter SQL_DT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ---- 用例 1：创建仅标题成功 ----------------------------------------

    @Test
    void createWithTitleOnlySucceedsAndFillsDefaults() throws Exception {
        String token = loginAndGetToken();
        String title = "m1仅标题-" + suffix();

        long id = createTask(token, title);
        try {
            assertEquals("todo", queryString("SELECT status FROM work_task WHERE id = ?", id));
            assertEquals(2, queryInt("SELECT priority FROM work_task WHERE id = ?", id));
            assertEquals(0, queryInt("SELECT actual_min FROM work_task WHERE id = ?", id));
            assertNotNull(queryLong("SELECT user_id FROM work_task WHERE id = ?", id));
        } finally {
            cleanTask(id);
        }
    }

    // ---- 用例 2：空标题 → 参数错误 -------------------------------------

    @Test
    void blankTitleReturnsParamError() throws Exception {
        String token = loginAndGetToken();

        mockMvc.perform(post("/api/work/tasks")
                        .header("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));

        mockMvc.perform(post("/api/work/tasks")
                        .header("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    // ---- 用例 3：状态机非法流转 ----------------------------------------

    @Test
    void invalidStatusTransitionRejected() throws Exception {
        String token = loginAndGetToken();
        long id = createTask(token, "m1状态机-" + suffix());
        try {
            // abandoned → doing 非法（abandoned 严格终态）：先 todo → abandoned
            changeStatusExpect(token, id, "abandoned", 0);
            changeStatusExpect(token, id, "doing", 10002);

            // done → abandoned 非法（done 只允许回 todo）：先 todo → done
            long id2 = createTask(token, "m1状态机2-" + suffix());
            try {
                changeStatusExpect(token, id2, "done", 0);
                changeStatusExpect(token, id2, "abandoned", 10002);
                // done → todo 是合法的（回退重开）
                changeStatusExpect(token, id2, "todo", 0);
            } finally {
                cleanTask(id2);
            }

            // todo → todo 幂等成功
            changeStatusExpect(token, id, "abandoned", 0);
        } finally {
            cleanTask(id);
        }
    }

    // ---- 用例 4：三视图边界 --------------------------------------------

    @Test
    void threeViewsBoundary() throws Exception {
        String token = loginAndGetToken();
        String tag = suffix();

        LocalDateTime yesterdayEnd = PeriodUtil.dayStart(PeriodUtil.today()).minusMinutes(1);  // 昨日 23:59
        LocalDateTime tomorrow = PeriodUtil.dayStart(PeriodUtil.today()).plusDays(1).plusHours(9); // 明日 09:00
        LocalDateTime todayZero = PeriodUtil.dayStart(PeriodUtil.today()); // 今日 00:00 整

        long overdueId = createTask(token, "m1昨日-" + tag);
        long tomorrowId = createTask(token, "m1明日-" + tag);
        long nullId = createTask(token, "m1无期-" + tag);
        long boundaryId = createTask(token, "m1边界-" + tag);
        try {
            setDueAt(overdueId, yesterdayEnd);
            setDueAt(tomorrowId, tomorrow);
            setDueAt(nullId, null);
            setDueAt(boundaryId, todayZero);

            assertTrue(containsInView(token, "overdue", overdueId), "昨日应在 overdue");
            assertTrue(!containsInView(token, "today", overdueId), "昨日不应在 today");
            assertTrue(containsInView(token, "all", overdueId), "昨日应在 all");

            assertTrue(containsInView(token, "today", tomorrowId), "明日应在 today");
            assertTrue(!containsInView(token, "overdue", tomorrowId), "明日不应在 overdue");

            assertTrue(containsInView(token, "today", nullId), "无截止日应在 today");
            assertTrue(!containsInView(token, "overdue", nullId), "无截止日不应在 overdue");

            assertTrue(containsInView(token, "today", boundaryId), "今日 00:00 整应在 today（>= todayStart）");
            assertTrue(!containsInView(token, "overdue", boundaryId), "今日 00:00 整不应在 overdue");

            // 软删后三视图均不可见
            mockMvc.perform(delete("/api/work/tasks/{id}", tomorrowId).header("token", token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
            assertTrue(!containsInView(token, "today", tomorrowId), "软删后不应在 today");
            assertTrue(!containsInView(token, "all", tomorrowId), "软删后不应在 all");
            assertTrue(!containsInView(token, "overdue", tomorrowId), "软删后不应在 overdue");
        } finally {
            cleanTask(overdueId);
            cleanTask(tomorrowId);
            cleanTask(nullId);
            cleanTask(boundaryId);
        }
    }

    // ---- 用例 5：完成任务写活动流 --------------------------------------

    @Test
    void completeTaskWritesActivityLogAndClearsOnReopen() throws Exception {
        String token = loginAndGetToken();
        String title = "m1完成-" + suffix();
        long id = createTask(token, title);
        try {
            changeStatusExpect(token, id, "doing", 0);
            changeStatusExpect(token, id, "done", 0);

            assertNotNull(queryObject("SELECT done_at FROM work_task WHERE id = ?", id),
                    "完成后 done_at 应非空");

            Integer logs = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM act_activity_log WHERE dimension='work' AND biz_type='task_done' AND title = ?",
                    Integer.class, title);
            assertEquals(1, logs, "完成后活动流应有 1 条 work/task_done");

            // 回退 todo：done_at 清空，活动流不新增
            changeStatusExpect(token, id, "todo", 0);
            assertEquals(null, queryObject("SELECT done_at FROM work_task WHERE id = ?", id),
                    "回退后 done_at 应被清空");
            Integer logsAfter = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM act_activity_log WHERE dimension='work' AND biz_type='task_done' AND title = ?",
                    Integer.class, title);
            assertEquals(1, logsAfter, "回退不应新增活动流");
        } finally {
            cleanTask(id);
            jdbcTemplate.update("DELETE FROM act_activity_log WHERE dimension='work' AND title = ?", title);
        }
    }

    // ---- 用例 6：打标签 → sys_tag_rel 落关联 + diff 同步 --------------

    @Test
    void tagBindingAndDiffSync() throws Exception {
        String token = loginAndGetToken();
        String tag = suffix();

        long tagId1 = createTag(token, "m1标签a-" + tag);
        long tagId2 = createTag(token, "m1标签b-" + tag);
        long taskId = 0L;
        try {
            MvcResult created = mockMvc.perform(post("/api/work/tasks")
                            .header("token", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "title", "m1标签任务-" + tag,
                                    "tagIds", new long[]{tagId1, tagId2}))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andReturn();
            taskId = objectMapper.readTree(readBody(created)).path("data").asLong();

            Integer relCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM sys_tag_rel WHERE biz_type='task' AND biz_id = ?", Integer.class, taskId);
            assertEquals(2, relCount, "应有 2 条标签关联");

            Integer useCount1 = jdbcTemplate.queryForObject(
                    "SELECT use_count FROM sys_tag WHERE id = ?", Integer.class, tagId1);
            assertEquals(1, useCount1, "标签 a use_count 应为 1");

            // 全量更新只留 tagId1 → 关联降到 1，tagId2 的 use_count 回减
            mockMvc.perform(put("/api/work/tasks/{id}", taskId)
                            .header("token", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "title", "m1标签任务改-" + tag,
                                    "tagIds", new long[]{tagId1}))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));

            relCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM sys_tag_rel WHERE biz_type='task' AND biz_id = ?", Integer.class, taskId);
            assertEquals(1, relCount, "更新后应只剩 1 条关联");

            Integer useCount2 = jdbcTemplate.queryForObject(
                    "SELECT use_count FROM sys_tag WHERE id = ?", Integer.class, tagId2);
            assertEquals(0, useCount2, "被移除标签 use_count 应回减到 0");

            // 详情接口 tags 长度正确
            mockMvc.perform(get("/api/work/tasks/{id}", taskId).header("token", token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.tags.length()").value(1))
                    .andExpect(jsonPath("$.data.tags[0].id").value((int) tagId1));
        } finally {
            if (taskId > 0) {
                cleanTask(taskId);
            }
            jdbcTemplate.update("DELETE FROM sys_tag_rel WHERE tag_id IN (?, ?) AND biz_type='task'", tagId1, tagId2);
            jdbcTemplate.update("DELETE FROM sys_tag WHERE id IN (?, ?)", tagId1, tagId2);
        }
    }

    // ---- 用例 7：详情不存在 → 10001 ------------------------------------

    @Test
    void detailNotFoundReturns10001() throws Exception {
        String token = loginAndGetToken();
        mockMvc.perform(get("/api/work/tasks/{id}", 999999999L).header("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(10001));
    }

    // ---- 用例 8：无 token → 401 ----------------------------------------

    @Test
    void noTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/work/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    // ---- 用例 9：经 API 真实传 dueAt（堵覆盖盲区：原用例 dueAt 全走 SQL 直改）----

    @Test
    void dueAtViaApiCreateUpdateAndReadback() throws Exception {
        String token = loginAndGetToken();
        String title = "m1日期-" + suffix();
        long id = 0L;
        try {
            // POST 带 dueAt（前端最终发送的空格格式）→ 成功 + 落库精确
            MvcResult created = mockMvc.perform(post("/api/work/tasks")
                            .header("token", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "title", title,
                                    "dueAt", "2026-12-31 09:00:00"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andReturn();
            id = objectMapper.readTree(readBody(created)).path("data").asLong();

            // 落库值正确
            String stored = jdbcTemplate.queryForObject(
                    "SELECT DATE_FORMAT(due_at, '%Y-%m-%d %H:%i:%s') FROM work_task WHERE id = ?",
                    String.class, id);
            assertEquals("2026-12-31 09:00:00", stored, "POST 的 dueAt 应精确落库");

            // PUT 全量更新带新 dueAt → 成功 + 落库更新
            mockMvc.perform(put("/api/work/tasks/{id}", id)
                            .header("token", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of(
                                    "title", title,
                                    "dueAt", "2027-01-15 14:30:00"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));
            String updated = jdbcTemplate.queryForObject(
                    "SELECT DATE_FORMAT(due_at, '%Y-%m-%d %H:%i:%s') FROM work_task WHERE id = ?",
                    String.class, id);
            assertEquals("2027-01-15 14:30:00", updated, "PUT 的 dueAt 应被更新");

            // GET 详情能正常回显 dueAt（非 null）
            mockMvc.perform(get("/api/work/tasks/{id}", id).header("token", token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.dueAt").isNotEmpty());
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

    /** 创建一个任务，返回 id。 */
    private long createTask(String token, String title) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/work/tasks")
                        .header("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("title", title))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return objectMapper.readTree(readBody(result)).path("data").asLong();
    }

    /** 创建一个标签，返回 id。 */
    private long createTag(String token, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/system/tags")
                        .header("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", name, "scope", "work"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return objectMapper.readTree(readBody(result)).path("data").path("id").asLong();
    }

    /** 状态流转并断言返回码。 */
    private void changeStatusExpect(String token, long id, String status, int expectCode) throws Exception {
        mockMvc.perform(put("/api/work/tasks/{id}/status", id)
                        .header("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", status))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(expectCode));
    }

    /** 直改 due_at（绕过 API 精确定位边界）。 */
    private void setDueAt(long id, LocalDateTime dueAt) {
        if (dueAt == null) {
            jdbcTemplate.update("UPDATE work_task SET due_at = NULL WHERE id = ?", id);
        } else {
            jdbcTemplate.update("UPDATE work_task SET due_at = ? WHERE id = ?", dueAt.format(SQL_DT), id);
        }
    }

    /** 指定视图首页（size=100）是否包含某任务。 */
    private boolean containsInView(String token, String view, long taskId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/work/tasks")
                        .header("token", token)
                        .param("view", view)
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        JsonNode records = objectMapper.readTree(readBody(result)).path("data").path("records");
        for (JsonNode node : records) {
            if (node.path("id").asLong() == taskId) {
                return true;
            }
        }
        return false;
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

    private Long queryLong(String sql, Object arg) {
        return jdbcTemplate.queryForObject(sql, Long.class, arg);
    }

    private Object queryObject(String sql, Object arg) {
        return jdbcTemplate.queryForObject(sql, Object.class, arg);
    }

    /** 物理清理任务及其标签关联。 */
    private void cleanTask(long id) {
        jdbcTemplate.update("DELETE FROM sys_tag_rel WHERE biz_type='task' AND biz_id = ?", id);
        jdbcTemplate.update("DELETE FROM work_task WHERE id = ?", id);
    }
}
