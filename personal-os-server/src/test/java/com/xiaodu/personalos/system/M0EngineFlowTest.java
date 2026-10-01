package com.xiaodu.personalos.system;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiaodu.personalos.common.util.PeriodUtil;
import com.xiaodu.personalos.system.mapper.ActivityLogMapper;
import com.xiaodu.personalos.system.service.ActivityLogService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * M0 基础设施全链路集成测试（直连本机 MySQL）。
 *
 * <p>覆盖《m0-infra.md》§4 验收：Flyway V2-V4、标签增查/多态关联、
 * 指标定义→写值→读回→周期汇总、活动流落库、首页聚合骨架。
 * 数据用 UUID 后缀隔离，重复运行幂等。</p>
 *
 * @author Kou
 */
@SpringBootTest(properties = "server.port=8090")
@AutoConfigureMockMvc
class M0EngineFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ActivityLogService activityLogService;

    @Autowired
    private ActivityLogMapper activityLogMapper;

    // ---- 验收 1：Flyway V2-V4 执行成功 --------------------------------

    /** flyway_schema_history 中 V2/V3/V4 均成功。 */
    @Test
    void flywayV2ToV4AppliedSuccessfully() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "SELECT version, success FROM flyway_schema_history WHERE version IN ('2','3','4')");
        assertEquals(3, rows.size());
        assertTrue(rows.stream().allMatch(r -> isSuccess(r.get("success"))),
                "V2-V4 必须全部 success=1: " + rows);
    }

    /** MySQL TINYINT(1) 经 JDBC 可能映射 Boolean，也可能 Number，双兼容。 */
    private static boolean isSuccess(Object value) {
        if (value instanceof Boolean b) {
            return b;
        }
        return value instanceof Number n && n.intValue() == 1;
    }

    // ---- 验收 3：标签增查 + 多态关联 ----------------------------------

    /** 标签：建 → 查 → 绑定（use_count+1）→ 幂等再绑 → 解绑（use_count 回减）。 */
    @Test
    void tagCreateListBindUnbindFlow() throws Exception {
        String token = loginAndGetToken();
        String name = "m0测试-" + UUID.randomUUID().toString().substring(0, 8);

        // 建
        MvcResult created = mockMvc.perform(post("/api/system/tags")
                        .header("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", name, "scope", "work", "color", "#e6f1fb"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.name").value(name))
                .andExpect(jsonPath("$.data.scope").value("work"))
                .andReturn();
        long tagId = objectMapper.readTree(created.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("data").path("id").asLong();

        // 查（scope 过滤命中）
        mockMvc.perform(get("/api/system/tags").header("token", token).param("scope", "work"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[?(@.id==" + tagId + ")].name").value(name));

        // 绑定多态关联
        mockMvc.perform(post("/api/system/tags/{id}/rels", tagId)
                        .header("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("bizType", "task", "bizId", 1001))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isNumber());

        // 幂等再绑：不产生第二条关联，use_count 仍为 1
        mockMvc.perform(post("/api/system/tags/{id}/rels", tagId)
                        .header("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("bizType", "task", "bizId", 1001))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        Integer relCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_tag_rel WHERE tag_id = ? AND biz_type = 'task' AND biz_id = 1001",
                Integer.class, tagId);
        assertEquals(1, relCount, "幂等绑定不得产生重复关联");
        Integer useCount = jdbcTemplate.queryForObject(
                "SELECT use_count FROM sys_tag WHERE id = ?", Integer.class, tagId);
        assertEquals(1, useCount, "use_count 应为 1（幂等重复绑定不重复计数）");

        // 解绑：use_count 回减到 0
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .delete("/api/system/tags/{id}/rels", tagId)
                        .header("token", token)
                        .param("bizType", "task").param("bizId", "1001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
        useCount = jdbcTemplate.queryForObject(
                "SELECT use_count FROM sys_tag WHERE id = ?", Integer.class, tagId);
        assertEquals(0, useCount);

        // 同名重复建 → 15100
        mockMvc.perform(post("/api/system/tags")
                        .header("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", name, "scope", "work"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(15100));

        // 清理本用例数据，保证可重复运行
        jdbcTemplate.update("DELETE FROM sys_tag_rel WHERE tag_id = ?", tagId);
        jdbcTemplate.update("DELETE FROM sys_tag WHERE id = ?", tagId);
    }

    // ---- 验收 2：指标定义 → 写值 → 读回 → 周期汇总 --------------------

    /** 指标全链路：定义 → 记录写读 → 同日 upsert → 周 key 汇总正确性。 */
    @Test
    void metricDefineRecordReadbackSummaryFlow() throws Exception {
        String token = loginAndGetToken();
        String code = "m0_metric_" + UUID.randomUUID().toString().substring(0, 8);

        // 定义（sum 聚合，便于验证周期汇总）
        mockMvc.perform(post("/api/system/metrics")
                        .header("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", code, "name", "M0测试指标", "dimension", "life",
                                "valueType", "number", "unit", "杯", "aggType", "sum"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.code").value(code));

        // 查询定义能读回
        mockMvc.perform(get("/api/system/metrics").header("token", token).param("dimension", "life"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[?(@.code=='" + code + "')].name").value("M0测试指标"));

        LocalDate today = PeriodUtil.today();
        String todayStr = PeriodUtil.formatDate(today);

        // 写一条值
        mockMvc.perform(post("/api/system/metrics/{code}/records", code)
                        .header("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("valueNum", 5))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.valueNum").value(5))
                .andExpect(jsonPath("$.data.recordDate").value(todayStr));

        // 同日再写 → upsert 更新为 8（同指标同日仍一条）
        mockMvc.perform(post("/api/system/metrics/{code}/records", code)
                        .header("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("valueNum", 8))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.valueNum").value(8));

        // 记录查询读回
        mockMvc.perform(get("/api/system/metrics/{code}/records", code)
                        .header("token", token)
                        .param("start", todayStr).param("end", todayStr))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].valueNum").value(8));

        // 周期汇总：本周 sum = 8，周期 key 与 PeriodUtil 口径一致
        String weekKey = PeriodUtil.weekKey(today);
        mockMvc.perform(get("/api/system/metrics/{code}/summary", code)
                        .header("token", token).param("period", weekKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.aggType").value("sum"))
                .andExpect(jsonPath("$.data.value").value(8))
                .andExpect(jsonPath("$.data.count").value(1));

        // 周期 key 非法 → 15203
        mockMvc.perform(get("/api/system/metrics/{code}/summary", code)
                        .header("token", token).param("period", "2026-W99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(15203));

        // 编码重复 → 15200；不存在编码写值 → 15201
        mockMvc.perform(post("/api/system/metrics")
                        .header("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", code, "name", "重复", "dimension", "life"))))
                .andExpect(jsonPath("$.code").value(15200));
        mockMvc.perform(post("/api/system/metrics/{code}/records", "no_such_metric")
                        .header("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("valueNum", 1))))
                .andExpect(jsonPath("$.code").value(15201));

        // 清理
        Long defId = jdbcTemplate.queryForObject(
                "SELECT id FROM sys_metric_def WHERE code = ?", Long.class, code);
        jdbcTemplate.update("DELETE FROM sys_metric_record WHERE metric_id = ?", defId);
        jdbcTemplate.update("DELETE FROM sys_metric_def WHERE id = ?", defId);
    }

    // ---- 验收 4：活动流落库 -------------------------------------------

    /** log(...) 后 act_activity_log 落一条，activity_date 与口径一致。 */
    @Test
    void activityLogLandsRow() {
        String title = "m0活动流-" + UUID.randomUUID().toString().substring(0, 8);
        Long before = activityLogMapper.selectCount(null);

        Long id = activityLogService.log(1L, "work", "task_done", title);
        assertTrue(id != null && id > 0, "落库后应返回 id");

        Long after = activityLogMapper.selectCount(null);
        assertEquals(before + 1, after, "act_activity_log 应新增一条");

        var row = activityLogMapper.selectById(id);
        assertEquals("work", row.getDimension());
        assertEquals(title, row.getTitle());
        assertEquals(PeriodUtil.effectiveDate(row.getOccurredAt()), row.getActivityDate());
    }

    // ---- 验收 5：首页聚合骨架 -----------------------------------------

    /** /api/dashboard/home 返回契约结构（P1 集成后为真实聚合值）。 */
    @Test
    void dashboardHomeReturnsContractStructure() throws Exception {
        String token = loginAndGetToken();

        MvcResult result = mockMvc.perform(get("/api/dashboard/home").header("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();

        JsonNode data = objectMapper.readTree(
                result.getResponse().getContentAsString(StandardCharsets.UTF_8)).path("data");

        // 契约结构齐全
        assertTrue(data.has("today") && data.path("today").has("date")
                        && data.path("today").has("week"), "today{date,week} 必须存在");
        assertTrue(data.has("work") && data.path("work").has("todayTotal")
                        && data.path("work").has("todayDone") && data.path("work").has("overdue"),
                "work{todayTotal,todayDone,overdue} 必须存在");
        assertTrue(data.has("life") && data.path("life").has("checkinDone")
                        && data.path("life").has("checkinTotal") && data.path("life").path("habits").isArray(),
                "life{checkinDone,checkinTotal,habits[]} 必须存在");
        assertTrue(data.has("sop") && data.path("sop").path("top").isArray(),
                "sop{top[]} 必须存在");
        assertTrue(data.has("streakDays"), "streakDays 必须存在");
        assertTrue(data.path("sopHints").isArray(), "sopHints[] 必须存在");

        // today 值走 PeriodUtil 口径
        assertEquals(PeriodUtil.formatDate(PeriodUtil.today()), data.path("today").path("date").asText());
        assertEquals(PeriodUtil.dayOfWeekCn(PeriodUtil.today()), data.path("today").path("week").asText());

        // P1 集成后数值为真实聚合值，此处只断言非负 + 数组形态
        // （接线正确性由 DashboardIntegrationTest 独立证明）
        assertTrue(data.path("work").path("todayTotal").asInt() >= 0);
        assertTrue(data.path("life").path("checkinDone").asInt() >= 0);
        assertTrue(data.path("streakDays").asInt() >= 0);
        assertTrue(data.path("sopHints").isArray());
    }

    // ---- 通用 ----------------------------------------------------------

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
}
