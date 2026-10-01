package com.xiaodu.personalos.sop;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
 * M2 SOP 库 · 独立 QA 复核测试（QA / Edward）。
 *
 * <p>与 {@code SopLibraryFlowTest} 互补，专攻「工程师自测可能没覆盖」的边界与错误路径：
 * costMin 自动计算（向上取整、最小 1）、avg_minutes 多轮平均、版本号小数递增语义、
 * 用户隔离（伪造 user_id=999）、跨 SOP / 越权 run 拒绝、删除后不可见与删除后发起执行、
 * keyword / category 筛选是否真正生效。</p>
 *
 * @author Edward (QA)
 */
@SpringBootTest(properties = "server.port=8091")
@AutoConfigureMockMvc
class SopLibraryQaTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // =====================================================================
    // A1. costMin 为空 → 按 started_at → now 自动算，向上取整、最小 1
    // =====================================================================

    /** started_at 回拨 ~5 分钟，结束不带 costMin → cost_min 应为 5 左右（向上取整，≥1）。 */
    @Test
    void finishWithoutCostMinAutoCalculatesFromStartedAt() throws Exception {
        String token = loginAndGetToken();
        long sopId = createSop(token, "自动计时-" + suffix(), "分类A",
                List.of(step("唯一一步", null, null, 5)));
        long runId = startRun(token, sopId);
        try {
            // 回拨 started_at = now - 305s（5 分 5 秒）→ 向上取整应为 6
            LocalDateTime backdated = LocalDateTime.now().minusSeconds(305);
            jdbcTemplate.update("UPDATE work_sop_log SET started_at = ? WHERE id = ?",
                    Timestamp.valueOf(backdated), runId);

            // 不带 costMin 结束
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("finished", true);
            body.put("stuckStep", null);
            body.put("deviation", null);
            mockMvc.perform(put("/api/sop/runs/{runId}", runId)
                            .header("token", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(body)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));

            Integer costMin = jdbcTemplate.queryForObject(
                    "SELECT cost_min FROM work_sop_log WHERE id = ?", Integer.class, runId);
            assertNotNull(costMin, "cost_min 应被自动回填");
            assertTrue(costMin >= 5 && costMin <= 7,
                    "305s 向上取整应约 6 分钟，实际=" + costMin);

            // 该 SOP 的 avg_minutes 应同步为该值
            Integer avg = jdbcTemplate.queryForObject(
                    "SELECT avg_minutes FROM work_sop WHERE id = ?", Integer.class, sopId);
            assertEquals(costMin, avg, "avg_minutes 应为自动算出的 cost_min");
        } finally {
            cleanup(sopId);
        }
    }

    /** started_at 与 now 相差不足 1 分钟 → 最小 1 分钟。 */
    @Test
    void finishWithoutCostMinClampsToMinimumOneMinute() throws Exception {
        String token = loginAndGetToken();
        long sopId = createSop(token, "最小耗时-" + suffix(), "分类A",
                List.of(step("快步骤", null, null, 1)));
        long runId = startRun(token, sopId);
        try {
            // started_at = now - 3 秒 → 向上取整后仍应钳到 1
            jdbcTemplate.update("UPDATE work_sop_log SET started_at = ? WHERE id = ?",
                    Timestamp.valueOf(LocalDateTime.now().minusSeconds(3)), runId);

            mockMvc.perform(put("/api/sop/runs/{runId}", runId)
                            .header("token", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"finished\":true}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));

            Integer costMin = jdbcTemplate.queryForObject(
                    "SELECT cost_min FROM work_sop_log WHERE id = ?", Integer.class, runId);
            assertEquals(1, costMin.intValue(), "不足 1 分钟应钳为最小值 1");
        } finally {
            cleanup(sopId);
        }
    }

    /** started_at 晚于 now（时钟异常）→ 不出现 0 / 负数，最小 1。 */
    @Test
    void finishWithFutureStartedAtStillYieldsAtLeastOne() throws Exception {
        String token = loginAndGetToken();
        long sopId = createSop(token, "未来时钟-" + suffix(), "分类A",
                List.of(step("步骤", null, null, 1)));
        long runId = startRun(token, sopId);
        try {
            jdbcTemplate.update("UPDATE work_sop_log SET started_at = ? WHERE id = ?",
                    Timestamp.valueOf(LocalDateTime.now().plusMinutes(10)), runId);

            mockMvc.perform(put("/api/sop/runs/{runId}", runId)
                            .header("token", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"finished\":true}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));

            Integer costMin = jdbcTemplate.queryForObject(
                    "SELECT cost_min FROM work_sop_log WHERE id = ?", Integer.class, runId);
            assertTrue(costMin != null && costMin >= 1, "异常时钟下 cost_min 仍应 ≥1，实际=" + costMin);
        } finally {
            cleanup(sopId);
        }
    }

    // =====================================================================
    // A2. avg_minutes 多轮平均：5 / 10 / 20 → round(11.667) = 12
    // =====================================================================

    @Test
    void avgMinutesAveragesAcrossThreeRuns() throws Exception {
        String token = loginAndGetToken();
        long sopId = createSop(token, "多轮平均-" + suffix(), "分类A",
                List.of(step("步骤", null, null, 5)));
        try {
            for (int cost : new int[] {5, 10, 20}) {
                long runId = startRun(token, sopId);
                finishRun(token, runId, Map.of("finished", true, "costMin", cost));
            }
            Map<String, Object> sop = jdbcTemplate.queryForMap(
                    "SELECT use_count, avg_minutes FROM work_sop WHERE id = ?", sopId);
            assertEquals(3, ((Number) sop.get("use_count")).intValue(), "use_count 应为 3");
            assertEquals(12, ((Number) sop.get("avg_minutes")).intValue(),
                    "(5+10+20)/3 = 11.67 四舍五入应为 12");

            // 详情 stats 亦应一致
            mockMvc.perform(get("/api/sop/{id}", sopId).header("token", token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.stats.runCount").value(3))
                    .andExpect(jsonPath("$.data.stats.avgMinutes").value(12));
        } finally {
            cleanup(sopId);
        }
    }

    // =====================================================================
    // A3. 版本号递增语义：连续编辑 3 次（均有执行记录）→ v1.1 / v1.2 / v1.3
    // =====================================================================

    @Test
    void versionBumpsSequentiallyNeverV110() throws Exception {
        String token = loginAndGetToken();
        long sopId = createSop(token, "版本递增-" + suffix(), "分类A",
                List.of(step("步骤", null, null, 5)));
        try {
            // 先制造一条执行记录
            long runId = startRun(token, sopId);
            finishRun(token, runId, Map.of("finished", true, "costMin", 6));

            List<String> expected = List.of("v1.1", "v1.2", "v1.3");
            for (String want : expected) {
                updateSop(token, sopId, "改-" + want + "-" + suffix(),
                        List.of(step("新步骤", null, null, 3)));
                String actual = jdbcTemplate.queryForObject(
                        "SELECT version FROM work_sop WHERE id = ?", String.class, sopId);
                assertEquals(want, actual, "版本号应按 0.1 递增，不应出现 v1.10 之类");
            }

            Integer snapshotCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM work_sop_version WHERE sop_id = ?", Integer.class, sopId);
            assertEquals(3, snapshotCount.intValue(), "三次编辑应各留一条快照");
        } finally {
            cleanup(sopId);
        }
    }

    // =====================================================================
    // A4. 用户隔离：伪造 user_id=999 的 SOP，对 admin 不可见
    // =====================================================================

    @Test
    void foreignUserSopIsInvisibleViaListAndDetail() throws Exception {
        String token = loginAndGetToken();
        Long adminId = adminUserId();
        String foreignTitle = "越界可见性-" + suffix();

        // 直接插一条属于 user_id=999 的 SOP（绕过接口）
        jdbcTemplate.update(
                "INSERT INTO work_sop (user_id, title, category, version, use_count, avg_minutes, "
                        + "status, pinned, deleted) VALUES (?, ?, ?, 'v1.0', 0, 0, 'draft', 0, 0)",
                999L, foreignTitle, "分类A");
        Long foreignId = jdbcTemplate.queryForObject(
                "SELECT id FROM work_sop WHERE title = ? AND user_id = 999", Long.class, foreignTitle);
        try {
            assertNotNull(foreignId, "前置：伪造 SOP 应插入成功");

            // 列表：keyword 精确命中标题，仍不应出现 999 的记录
            MvcResult res = mockMvc.perform(get("/api/sop").header("token", token)
                            .param("keyword", foreignTitle).param("size", "50"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andReturn();
            JsonNode records = objectMapper.readTree(
                    res.getResponse().getContentAsString(StandardCharsets.UTF_8))
                    .path("data").path("records");
            List<Long> ids = new ArrayList<>();
            records.forEach(n -> ids.add(n.path("id").asLong()));
            assertTrue(ids.stream().noneMatch(id -> id.equals(foreignId)),
                    "列表不得泄露 user_id=999 的 SOP");

            // 详情：不存在的归属 → 10101
            mockMvc.perform(get("/api/sop/{id}", foreignId).header("token", token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(10101));

            // 执行历史同样 10101
            mockMvc.perform(get("/api/sop/{id}/runs", foreignId).header("token", token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(10101));

            // 发起执行也应被拒（10101）
            mockMvc.perform(post("/api/sop/{id}/runs", foreignId).header("token", token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(10101));

            // 删除也应被拒（10101）+ 伪造记录仍在
            mockMvc.perform(delete("/api/sop/{id}", foreignId).header("token", token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(10101));
            Integer still = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM work_sop WHERE id = ? AND deleted = 0", Integer.class, foreignId);
            assertEquals(1, still.intValue(), "越权删除不得生效");

            assertNotNull(adminId, "前置：admin 用户应存在");
        } finally {
            jdbcTemplate.update("DELETE FROM work_sop_step WHERE sop_id = ?", foreignId);
            jdbcTemplate.update("DELETE FROM work_sop WHERE id = ?", foreignId);
        }
    }

    // =====================================================================
    // A5. 跨 SOP / 越权 run 结束拒绝
    // =====================================================================

    /** 用 A 的 runId 结束 B 的 run 语义：这里用「A 的 runId + 归属校验」验证不串写。
     *  由于 finishRun 只按 runId 定位 run，再校验其 SOP 归属当前用户，
     *  我们验证：结束一个属于别人的 SOP 的 run → 10101（不静默写坏数据）。 */
    @Test
    void finishingRunOfForeignSopIsRejected() throws Exception {
        String token = loginAndGetToken();
        String foreignTitle = "越权run-" + suffix();
        jdbcTemplate.update(
                "INSERT INTO work_sop (user_id, title, version, use_count, avg_minutes, status, pinned, deleted) "
                        + "VALUES (999, ?, 'v1.0', 0, 0, 'draft', 0, 0)", foreignTitle);
        Long foreignSopId = jdbcTemplate.queryForObject(
                "SELECT id FROM work_sop WHERE title = ?", Long.class, foreignTitle);
        // 为伪造 SOP 造一条未结束 run
        jdbcTemplate.update("INSERT INTO work_sop_log (sop_id, started_at, cost_min) VALUES (?, ?, NULL)",
                foreignSopId, Timestamp.valueOf(LocalDateTime.now().minusMinutes(5)));
        Long foreignRunId = jdbcTemplate.queryForObject(
                "SELECT id FROM work_sop_log WHERE sop_id = ? ORDER BY id DESC LIMIT 1",
                Long.class, foreignSopId);
        try {
            mockMvc.perform(put("/api/sop/runs/{runId}", foreignRunId)
                            .header("token", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"finished\":true,\"costMin\":7}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(10101));

            // 伪造 run 不应被写入 finished_at
            Timestamp finished = jdbcTemplate.queryForObject(
                    "SELECT finished_at FROM work_sop_log WHERE id = ?", Timestamp.class, foreignRunId);
            assertNull(finished, "越权结束不得回写 finished_at");
        } finally {
            jdbcTemplate.update("DELETE FROM work_sop_log WHERE sop_id = ?", foreignSopId);
            jdbcTemplate.update("DELETE FROM work_sop WHERE id = ?", foreignSopId);
        }
    }

    /** 不存在的 runId → 10104（参数非法），而非 500。 */
    @Test
    void finishingNonexistentRunReturnsParamInvalid() throws Exception {
        String token = loginAndGetToken();
        mockMvc.perform(put("/api/sop/runs/{runId}", 987654321L)
                        .header("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"finished\":true,\"costMin\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(10104));
    }

    /** finished 非 true → 10104；且不应把 run 标为结束。 */
    @Test
    void finishingWithFinishedFalseIsRejected() throws Exception {
        String token = loginAndGetToken();
        long sopId = createSop(token, "未完成标记-" + suffix(), "分类A",
                List.of(step("步骤", null, null, 5)));
        long runId = startRun(token, sopId);
        try {
            mockMvc.perform(put("/api/sop/runs/{runId}", runId)
                            .header("token", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"finished\":false}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(10104));

            Timestamp finished = jdbcTemplate.queryForObject(
                    "SELECT finished_at FROM work_sop_log WHERE id = ?", Timestamp.class, runId);
            assertNull(finished, "finished=false 时不得结束执行");
            Integer useCount = jdbcTemplate.queryForObject(
                    "SELECT use_count FROM work_sop WHERE id = ?", Integer.class, sopId);
            assertEquals(0, useCount.intValue(), "被拒绝的结束请求不得累加 use_count");
        } finally {
            cleanup(sopId);
        }
    }

    // =====================================================================
    // A6. 删除后不可见：列表 / 详情 / run 查询；删除后发起执行不 500
    // =====================================================================

    @Test
    void afterDeleteRunsInvisibleAndStartRunHandledGracefully() throws Exception {
        String token = loginAndGetToken();
        String title = "删除后-" + suffix();
        long sopId = createSop(token, title, "分类A", List.of(step("步骤", null, null, 5)));
        long runId = startRun(token, sopId);
        finishRun(token, runId, Map.of("finished", true, "costMin", 6));
        try {
            mockMvc.perform(delete("/api/sop/{id}", sopId).header("token", token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));

            // 列表：keyword 命中也不应出现
            MvcResult res = mockMvc.perform(get("/api/sop").header("token", token)
                            .param("keyword", title))
                    .andExpect(status().isOk())
                    .andReturn();
            JsonNode records = objectMapper.readTree(
                    res.getResponse().getContentAsString(StandardCharsets.UTF_8))
                    .path("data").path("records");
            List<Long> ids = new ArrayList<>();
            records.forEach(n -> ids.add(n.path("id").asLong()));
            assertTrue(ids.stream().noneMatch(id -> id.equals(sopId)), "删除后列表不应出现该 SOP");

            // 详情 → 10101
            mockMvc.perform(get("/api/sop/{id}", sopId).header("token", token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(10101));

            // 该 SOP 的 run 查询 → 10101（而不是空列表 200）
            mockMvc.perform(get("/api/sop/{id}/runs", sopId).header("token", token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(10101));

            // 删除后发起执行 → 应被 10101 拒绝，绝不能 500
            mockMvc.perform(post("/api/sop/{id}/runs", sopId).header("token", token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(10101));

            // 版本回看也应 10101
            mockMvc.perform(get("/api/sop/{id}/versions/{vid}", sopId, 1L).header("token", token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(10101));
        } finally {
            cleanup(sopId);
        }
    }

    // =====================================================================
    // A7. keyword / category 筛选真正生效
    // =====================================================================

    @Test
    void keywordAndCategoryFiltersActuallyWork() throws Exception {
        String token = loginAndGetToken();
        String tag = suffix();
        String kw = "筛选命中-" + tag;
        // 两条：一条标题命中关键词 + 分类 B；一条标题不含关键词 + 分类 C
        long hitId = createSop(token, kw, "分类B-" + tag, List.of(step("步骤", null, null, 5)));
        long missId = createSop(token, "无关标题-" + tag, "分类C-" + tag, List.of(step("步骤", null, null, 5)));
        try {
            // keyword 命中：应只返回 hitId
            MvcResult r1 = mockMvc.perform(get("/api/sop").header("token", token)
                            .param("keyword", kw).param("size", "50"))
                    .andExpect(status().isOk()).andReturn();
            List<Long> kwIds = idsOf(r1);
            assertTrue(kwIds.contains(hitId), "keyword 应命中标题包含关键词的 SOP");
            assertTrue(kwIds.stream().noneMatch(id -> id.equals(missId)), "keyword 不应命中不相关 SOP");

            // category 命中：应只返回该分类的 SOP
            MvcResult r2 = mockMvc.perform(get("/api/sop").header("token", token)
                            .param("category", "分类C-" + tag).param("size", "50"))
                    .andExpect(status().isOk()).andReturn();
            List<Long> catIds = idsOf(r2);
            assertTrue(catIds.contains(missId), "category 应筛选出该分类的 SOP");
            assertTrue(catIds.stream().noneMatch(id -> id.equals(hitId)),
                    "category 不应返回其它分类的 SOP");
        } finally {
            cleanup(hitId);
            cleanup(missId);
        }
    }

    /** 分页 page/size 边界：size 超过上限应被钳制，page<=0 归 1，不报错。 */
    @Test
    void paginationBoundaryIsSafe() throws Exception {
        String token = loginAndGetToken();
        mockMvc.perform(get("/api/sop").header("token", token)
                        .param("page", "0").param("size", "9999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.page").value(1));
    }

    // =====================================================================
    // 辅助方法
    // =====================================================================

    private String loginAndGetToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("data").path("token").asText();
    }

    private Long adminUserId() {
        return jdbcTemplate.queryForObject("SELECT id FROM sys_user WHERE username = 'admin'", Long.class);
    }

    private long createSop(String token, String title, String category, List<Map<String, Object>> steps)
            throws Exception {
        MvcResult result = mockMvc.perform(post("/api/sop")
                        .header("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sopBody(title, category, steps))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("data").path("id").asLong();
    }

    private void updateSop(String token, long sopId, String title, List<Map<String, Object>> steps)
            throws Exception {
        mockMvc.perform(put("/api/sop/{id}", sopId)
                        .header("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sopBody(title, "分类A", steps))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    private long startRun(String token, long sopId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/sop/{id}/runs", sopId).header("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("data").path("runId").asLong();
    }

    private void finishRun(String token, long runId, Map<String, Object> body) throws Exception {
        mockMvc.perform(put("/api/sop/runs/{runId}", runId)
                        .header("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    private List<Long> idsOf(MvcResult res) throws Exception {
        JsonNode records = objectMapper.readTree(
                res.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("data").path("records");
        List<Long> ids = new ArrayList<>();
        records.forEach(n -> ids.add(n.path("id").asLong()));
        return ids;
    }

    private Map<String, Object> sopBody(String title, String category, List<Map<String, Object>> steps) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("title", title);
        body.put("category", category);
        body.put("triggerScene", "QA 场景");
        body.put("goal", "QA 目标");
        body.put("steps", steps);
        body.put("pinned", 0);
        return body;
    }

    private Map<String, Object> step(String title, String detail, String tip, Integer estimateMin) {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("title", title);
        s.put("detail", detail);
        s.put("tip", tip);
        s.put("estimateMin", estimateMin);
        return s;
    }

    private String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private void cleanup(long sopId) {
        jdbcTemplate.update("DELETE FROM work_sop_step WHERE sop_id = ?", sopId);
        jdbcTemplate.update("DELETE FROM work_sop_version WHERE sop_id = ?", sopId);
        jdbcTemplate.update("DELETE FROM work_sop_log WHERE sop_id = ?", sopId);
        jdbcTemplate.update("DELETE FROM act_activity_log WHERE ref_type = 'sop' AND ref_id = ?", sopId);
        jdbcTemplate.update("DELETE FROM work_sop WHERE id = ?", sopId);
    }
}
