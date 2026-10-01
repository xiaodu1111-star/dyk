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
 * M2 SOP 库全链路集成测试（直连本机 MySQL）。
 *
 * <p>覆盖任务书 §6 的 7 个必测用例：创建含 3 步、未执行不留版本、
 * 执行后编辑留版本 +0.1、结束执行聚合、重复结束 10103、列表排序、
 * 10101 / 10102 错误码。数据用 UUID 后缀隔离，用例内自行清理，可重复运行。</p>
 *
 * @author Alex
 */
@SpringBootTest(properties = "server.port=8090")
@AutoConfigureMockMvc
class SopLibraryFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ---- 用例 1：创建含 3 步的 SOP → 详情返回 3 步且 step_no 为 1/2/3 ----

    @Test
    void createWithThreeStepsThenDetailReturnsContinuousStepNo() throws Exception {
        String token = loginAndGetToken();
        String title = "评审流程-" + suffix();

        long sopId = createSop(token, title, "需求评审",
                List.of(step("拉需求", "读 PRD", "别只看标题", 15),
                        step("写方案", "画时序", null, 30),
                        step("评审", "过会", "提前预约", 45)));
        try {
            MvcResult detailRes = mockMvc.perform(get("/api/sop/{id}", sopId).header("token", token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andExpect(jsonPath("$.data.id").value(sopId))
                    .andExpect(jsonPath("$.data.steps.length()").value(3))
                    .andExpect(jsonPath("$.data.steps[0].stepNo").value(1))
                    .andExpect(jsonPath("$.data.steps[1].stepNo").value(2))
                    .andExpect(jsonPath("$.data.steps[2].stepNo").value(3))
                    .andExpect(jsonPath("$.data.version").value("v1.0"))
                    .andReturn();

            // DB 层复验 step_no 连续
            List<Integer> stepNos = jdbcTemplate.queryForList(
                    "SELECT step_no FROM work_sop_step WHERE sop_id = ? ORDER BY step_no",
                    Integer.class, sopId);
            assertEquals(List.of(1, 2, 3), stepNos, "step_no 必须为 1/2/3 连续");

            // 详情 stats 结构齐全
            JsonNode stats = objectMapper.readTree(
                    detailRes.getResponse().getContentAsString(StandardCharsets.UTF_8))
                    .path("data").path("stats");
            assertTrue(stats.has("runCount") && stats.has("avgMinutes")
                    && stats.has("topStuckStep") && stats.has("lastUsedAt"),
                    "stats 必须含 runCount/avgMinutes/topStuckStep/lastUsedAt");
        } finally {
            cleanup(sopId);
        }
    }

    // ---- 用例 2：编辑未执行过的 SOP → work_sop_version 不新增 ----------

    @Test
    void editNeverRunSopProducesNoVersionSnapshot() throws Exception {
        String token = loginAndGetToken();
        long sopId = createSop(token, "未执行SOP-" + suffix(), "故障处理",
                List.of(step("第一步", null, null, 5)));
        try {
            Integer before = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM work_sop_version WHERE sop_id = ?", Integer.class, sopId);
            assertEquals(0, before, "新建不应有版本快照");

            updateSop(token, sopId, "改标题-" + suffix(), List.of(step("改后步骤", null, null, 8)));

            Integer after = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM work_sop_version WHERE sop_id = ?", Integer.class, sopId);
            assertEquals(0, after, "未执行过的 SOP 编辑不得留版本");

            String version = jdbcTemplate.queryForObject(
                    "SELECT version FROM work_sop WHERE id = ?", String.class, sopId);
            assertEquals("v1.0", version, "版本号应保持 v1.0");
        } finally {
            cleanup(sopId);
        }
    }

    // ---- 用例 3：执行后再编辑 → 落快照 + 版本号 v1.0 → v1.1 ------------

    @Test
    void editAfterRunWritesSnapshotAndBumpsVersion() throws Exception {
        String token = loginAndGetToken();
        long sopId = createSop(token, "执行后编辑-" + suffix(), "周报",
                List.of(step("收集数据", null, null, 10), step("成文", null, null, 20)));
        try {
            // 发起 + 结束执行
            long runId = startRun(token, sopId);
            finishRun(token, runId, Map.of("finished", true, "costMin", 25));

            // 编辑 → 应落一条快照（旧内容 v1.0），主表版本 v1.1
            updateSop(token, sopId, "执行后改版-" + suffix(),
                    List.of(step("新步骤A", null, null, 12)));

            List<Map<String, Object>> versions = jdbcTemplate.queryForList(
                    "SELECT version, content_json FROM work_sop_version WHERE sop_id = ?", sopId);
            assertEquals(1, versions.size(), "执行后编辑应落一条快照");
            assertEquals("v1.0", versions.get(0).get("version"), "快照版本应为旧版本 v1.0");

            String snapshot = String.valueOf(versions.get(0).get("content_json"));
            assertTrue(snapshot.contains("收集数据") && snapshot.contains("成文"),
                    "快照应为旧内容（含旧步骤）");

            String version = jdbcTemplate.queryForObject(
                    "SELECT version FROM work_sop WHERE id = ?", String.class, sopId);
            assertEquals("v1.1", version, "主表版本号应 v1.0 → v1.1");

            // 再次编辑 → v1.2（验证不是字符串拼接出 v1.10）
            updateSop(token, sopId, "再改版-" + suffix(), List.of(step("新步骤B", null, null, 9)));
            String version2 = jdbcTemplate.queryForObject(
                    "SELECT version FROM work_sop WHERE id = ?", String.class, sopId);
            assertEquals("v1.2", version2, "小数递增语义应为 v1.2 而非 v1.10");
        } finally {
            cleanup(sopId);
        }
    }

    // ---- 用例 4：结束执行 → use_count+1、avg_minutes 正确、活动流落一条 ----

    @Test
    void finishRunUpdatesAggregatesAndWritesActivityLog() throws Exception {
        String token = loginAndGetToken();
        long sopId = createSop(token, "聚合校验-" + suffix(), "例会",
                List.of(step("准备", null, null, 5)));
        try {
            long runId1 = startRun(token, sopId);
            finishRun(token, runId1, Map.of("finished", true, "costMin", 10));

            long runId2 = startRun(token, sopId);
            finishRun(token, runId2, Map.of("finished", true, "costMin", 21));

            // use_count = 2，avg = round((10+21)/2) = 16（15.5 四舍五入 → 16）
            Map<String, Object> sop = jdbcTemplate.queryForMap(
                    "SELECT use_count, avg_minutes, last_used_at FROM work_sop WHERE id = ?", sopId);
            assertEquals(2, ((Number) sop.get("use_count")).intValue(), "use_count 应为 2");
            assertEquals(16, ((Number) sop.get("avg_minutes")).intValue(), "avg_minutes 应为 round(15.5)=16");
            assertNotNull(sop.get("last_used_at"), "last_used_at 应回填");

            // 活动流落一条 work/sop_run
            Integer logs = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM act_activity_log "
                            + "WHERE ref_type = 'sop' AND ref_id = ? AND biz_type = 'sop_run' AND dimension = 'work'",
                    Integer.class, sopId);
            assertEquals(2, logs, "活动流应为两次执行各落一条 work/sop_run");

            // 详情 stats：runCount=2，topStuckStep 随卡点统计
            mockMvc.perform(get("/api/sop/{id}", sopId).header("token", token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.stats.runCount").value(2))
                    .andExpect(jsonPath("$.data.stats.avgMinutes").value(16));

            // 列表卡直接带 useCount / avgMinutes
            mockMvc.perform(get("/api/sop").header("token", token)
                            .param("keyword", extractTitle(sopId)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.records[0].id").value(sopId))
                    .andExpect(jsonPath("$.data.records[0].useCount").value(2))
                    .andExpect(jsonPath("$.data.records[0].avgMinutes").value(16));
        } finally {
            cleanup(sopId);
        }
    }

    // ---- 用例 5：已结束的 run 再次 PUT → 10103 -------------------------

    @Test
    void finishFinishedRunReturns10103() throws Exception {
        String token = loginAndGetToken();
        long sopId = createSop(token, "重复结束-" + suffix(), "故障处理",
                List.of(step("排障", null, null, 5)));
        try {
            long runId = startRun(token, sopId);
            finishRun(token, runId, Map.of("finished", true, "costMin", 12));

            // 再次结束 → 10103
            mockMvc.perform(put("/api/sop/runs/{runId}", runId)
                            .header("token", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    Map.of("finished", true, "costMin", 99))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(10103));
        } finally {
            cleanup(sopId);
        }
    }

    // ---- 用例 6：列表排序 pinned > last_used_at > use_count ------------

    @Test
    void listOrderingRespectsPinnedLastUsedAndUseCount() throws Exception {
        String token = loginAndGetToken();
        String tag = suffix();
        // 三条同关键词 SOP，用 keyword 精确定位本用例数据
        String keyword = "排序-" + tag;

        long pinnedId = createSop(token, keyword + "-置顶", "分类A",
                List.of(step("s1", null, null, 1)));
        long recentId = createSop(token, keyword + "-最近", "分类A",
                List.of(step("s1", null, null, 1)));
        long oldId = createSop(token, keyword + "-最旧", "分类A",
                List.of(step("s1", null, null, 1)));
        try {
            // recent 跑 1 次 → use_count=1 + last_used_at=now；old 手工设较早 last_used_at
            long runId = startRun(token, recentId);
            finishRun(token, runId, Map.of("finished", true, "costMin", 5));
            jdbcTemplate.update("UPDATE work_sop SET last_used_at = ? WHERE id = ?",
                    java.sql.Timestamp.valueOf(java.time.LocalDateTime.now().minusDays(3)), oldId);

            // 置顶 pinnedId
            mockMvc.perform(put("/api/sop/{id}", pinnedId)
                            .header("token", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    sopBody(keyword + "-置顶", "分类A", List.of(step("s1", null, null, 1)), 1))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));

            MvcResult res = mockMvc.perform(get("/api/sop").header("token", token)
                            .param("keyword", keyword).param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0))
                    .andReturn();
            JsonNode records = objectMapper.readTree(
                    res.getResponse().getContentAsString(StandardCharsets.UTF_8)).path("data").path("records");

            List<Long> ids = new ArrayList<>();
            records.forEach(n -> ids.add(n.path("id").asLong()));
            assertEquals(3, ids.size(), "本用例应有 3 条记录");
            assertEquals(pinnedId, ids.get(0), "置顶应排第一");
            assertEquals(recentId, ids.get(1), "有 last_used_at 的应排第二");
            assertEquals(oldId, ids.get(2), "last_used_at 较旧的应排最后");
        } finally {
            cleanup(pinnedId);
            cleanup(recentId);
            cleanup(oldId);
        }
    }

    // ---- 用例 7：不存在的 SOP → 10101；无步骤/无标题 → 10102 ------------

    @Test
    void notFoundAndStepInvalidErrorCodes() throws Exception {
        String token = loginAndGetToken();

        // 不存在的 SOP id → 10101
        mockMvc.perform(get("/api/sop/{id}", 999999999L).header("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(10101));

        // 创建步骤无标题 → 10102（步骤标题为空）
        Map<String, Object> badStepBody = sopBody("非法步骤-" + suffix(), "分类A",
                List.of(step(" ", null, null, 5)), 0);
        mockMvc.perform(post("/api/sop")
                        .header("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badStepBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(10102));

        // 创建无步骤 → 10102
        Map<String, Object> noStepBody = sopBody("无步骤-" + suffix(), "分类A", List.of(), 0);
        mockMvc.perform(post("/api/sop")
                        .header("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(noStepBody)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(10102));

        // 详情也不存在 → 10101
        mockMvc.perform(get("/api/sop/{id}/runs", 999999999L).header("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(10101));
    }

    // ---- 附加用例：逻辑删除后不可见（保障边界语义） --------------------

    @Test
    void deleteIsLogicalAndHidesRecord() throws Exception {
        String token = loginAndGetToken();
        long sopId = createSop(token, "待删-" + suffix(), "分类A",
                List.of(step("s1", null, null, 1)));
        try {
            mockMvc.perform(delete("/api/sop/{id}", sopId).header("token", token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));

            // 删除后详情 → 10101
            mockMvc.perform(get("/api/sop/{id}", sopId).header("token", token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(10101));

            // 逻辑删标记置位
            Integer deleted = jdbcTemplate.queryForObject(
                    "SELECT deleted FROM work_sop WHERE id = ?", Integer.class, sopId);
            assertEquals(1, deleted.intValue(), "应为逻辑删除 deleted=1");
        } finally {
            cleanup(sopId);
        }
    }

    // ---- 辅助方法 -------------------------------------------------------

    /** 登录拿 token（复用 DataInitializer 播种的 admin/admin123）。 */
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

    /** 创建 SOP 并返回 id。 */
    private long createSop(String token, String title, String category, List<Map<String, Object>> steps)
            throws Exception {
        MvcResult result = mockMvc.perform(post("/api/sop")
                        .header("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sopBody(title, category, steps, 0))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("data").path("id").asLong();
    }

    /** 编辑 SOP。 */
    private void updateSop(String token, long sopId, String title, List<Map<String, Object>> steps)
            throws Exception {
        mockMvc.perform(put("/api/sop/{id}", sopId)
                        .header("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sopBody(title, "分类A", steps, 0))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    /** 发起执行并返回 runId。 */
    private long startRun(String token, long sopId) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/sop/{id}/runs", sopId).header("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("data").path("runId").asLong();
    }

    /** 结束执行。 */
    private void finishRun(String token, long runId, Map<String, Object> body) throws Exception {
        mockMvc.perform(put("/api/sop/runs/{runId}", runId)
                        .header("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    /** 构造 SOP 请求体。 */
    private Map<String, Object> sopBody(String title, String category,
                                        List<Map<String, Object>> steps, int pinned) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("title", title);
        body.put("category", category);
        body.put("triggerScene", "测试场景");
        body.put("goal", "测试目标");
        body.put("steps", steps);
        body.put("pinned", pinned);
        return body;
    }

    /** 构造步骤。 */
    private Map<String, Object> step(String title, String detail, String tip, Integer estimateMin) {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("title", title);
        s.put("detail", detail);
        s.put("tip", tip);
        s.put("estimateMin", estimateMin);
        return s;
    }

    /** 随机后缀，隔离用例数据。 */
    private String suffix() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    /** 按 id 取标题（用于关键词过滤）。 */
    private String extractTitle(long sopId) throws Exception {
        MvcResult res = mockMvc.perform(get("/api/sop/{id}", sopId)
                        .header("token", loginAndGetToken()))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(res.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("data").path("title").asText();
    }

    /** 清理本用例数据（步骤 / 版本 / 执行记录 / 活动流 / 主表）。 */
    private void cleanup(long sopId) {
        jdbcTemplate.update("DELETE FROM work_sop_step WHERE sop_id = ?", sopId);
        jdbcTemplate.update("DELETE FROM work_sop_version WHERE sop_id = ?", sopId);
        jdbcTemplate.update("DELETE FROM work_sop_log WHERE sop_id = ?", sopId);
        jdbcTemplate.update("DELETE FROM act_activity_log WHERE ref_type = 'sop' AND ref_id = ?", sopId);
        jdbcTemplate.update("DELETE FROM work_sop WHERE id = ?", sopId);
    }
}
