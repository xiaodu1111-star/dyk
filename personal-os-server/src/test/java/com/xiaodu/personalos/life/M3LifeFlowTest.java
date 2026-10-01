package com.xiaodu.personalos.life;

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

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * M3 生活域全链路集成测试（直连本机 MySQL）。
 *
 * <p>覆盖《m3-life-domain.md》§7 测试与验收清单的全部测试点：</p>
 * <ul>
 *   <li>建打卡型习惯落 sys_metric_def（dimension=life, value_type=bool, agg_type=last, target_value=1）</li>
 *   <li>再建一个 → code 不重复（generateUniqueCode 生效）</li>
 *   <li>打卡 → sys_metric_record 落一条 record_date=今天</li>
 *   <li>重复打卡 → 14002（幂等拒绝）</li>
 *   <li>计数型 +1 两次 → 当日 SUM=2（累加语义）</li>
 *   <li>streak：3 天连续 → 3；断档场景 → 断档处停止</li>
 *   <li>快捷记录「跑步 5」→ 预填 metricId/value=5；「乱码 xyz」→ 14003；只解析不落库、确认后才落库</li>
 *   <li>打卡 → act_activity_log 落一条 life/checkin</li>
 * </ul>
 *
 * <p><b>零建表红线</b>：习惯 = 一条 sys_metric_def，打卡 = 一条 sys_metric_record，
 * 全程不新建业务表。测试数据用 UUID 后缀隔离，运行后可重复执行（幂等）。</p>
 *
 * @author Edward
 */
@SpringBootTest(properties = "server.port=8090")
@AutoConfigureMockMvc
class M3LifeFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // ---- §7-1：建打卡型习惯 → sys_metric_def 落一条，字段映射正确 --------

    /** 建打卡型习惯 → def 落一条且 dimension=life / value_type=bool / agg_type=last / target_value=1。 */
    @Test
    void createCheckinHabitLandsMetricDefWithCorrectMapping() throws Exception {
        String token = loginAndGetToken();
        String name = "m3打卡-" + uuid();

        MvcResult result = mockMvc.perform(post("/api/life/habits")
                        .header("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", name, "type", "checkin"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.name").value(name))
                .andExpect(jsonPath("$.data.type").value("checkin"))
                .andReturn();

        long habitId = readId(result);

        // 落 sys_metric_def 一行，字段映射与 §3 映射表一致
        Map<String, Object> row = jdbcTemplate.queryForMap(
                "SELECT dimension, value_type, agg_type, target_value, unit FROM sys_metric_def WHERE id = ?",
                habitId);
        assertEquals("life", row.get("dimension"));
        assertEquals("bool", row.get("value_type"));
        assertEquals("last", row.get("agg_type"));
        assertEquals(0, new BigDecimal("1").compareTo((BigDecimal) row.get("target_value")),
                "打卡型 target_value 必须为 1");
        // 打卡型 unit 应为空
        assertTrue(row.get("unit") == null || ((String) row.get("unit")).isEmpty(),
                "打卡型 unit 应为空");

        cleanupHabit(habitId);
    }

    // ---- §7-1：再建一个 → code 不重复（generateUniqueCode 生效） ---------

    /** 同名再建一个习惯 → code 不重复（查重后追加后缀）。 */
    @Test
    void createSameNameTwiceGeneratesUniqueCode() throws Exception {
        String token = loginAndGetToken();
        String name = "m3重复名-" + uuid();

        long firstId = readId(mockMvc.perform(post("/api/life/habits")
                        .header("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", name, "type", "checkin"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn());

        long secondId = readId(mockMvc.perform(post("/api/life/habits")
                        .header("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", name, "type", "checkin"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn());

        String code1 = jdbcTemplate.queryForObject(
                "SELECT code FROM sys_metric_def WHERE id = ?", String.class, firstId);
        String code2 = jdbcTemplate.queryForObject(
                "SELECT code FROM sys_metric_def WHERE id = ?", String.class, secondId);

        assertNotNull(code1);
        assertNotNull(code2);
        assertTrue(!code1.equals(code2),
                "同名习惯 code 必须唯一，generateUniqueCode 应生效: " + code1 + " / " + code2);

        cleanupHabit(firstId);
        cleanupHabit(secondId);
    }

    // ---- §7-2：打卡 → record 落 record_date=今天；重复打卡 → 14002 ------

    /** 打卡型打卡落一条今日记录；重复打卡被拒 14002（幂等拒绝，不覆盖）。 */
    @Test
    void checkinLandsTodayRecordAndDuplicateRejected() throws Exception {
        String token = loginAndGetToken();
        long habitId = createCheckinHabit(token, "m3打卡落库-" + uuid());
        LocalDate today = PeriodUtil.today();

        // 首次打卡成功
        mockMvc.perform(post("/api/life/habits/{id}/checkin", habitId)
                        .header("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.metricId").value(habitId))
                .andExpect(jsonPath("$.data.date").value(PeriodUtil.formatDate(today)))
                .andExpect(jsonPath("$.data.value").value(1));

        // 落库一条，record_date = 今天
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_metric_record WHERE metric_id = ? AND record_date = ?",
                Integer.class, habitId, today);
        assertEquals(1, count, "打卡成功应落一条今日记录");

        // 重复打卡 → 14002
        mockMvc.perform(post("/api/life/habits/{id}/checkin", habitId)
                        .header("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(14002));

        // 被拒后记录数不变（未覆盖、未新增）
        Integer countAfter = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_metric_record WHERE metric_id = ? AND record_date = ?",
                Integer.class, habitId, today);
        assertEquals(1, countAfter, "重复打卡不得产生第二条记录");

        cleanupHabit(habitId);
    }

    // ---- §7-3：计数型 +1 两次 → 今日 SUM=2（累加语义，不是覆盖） --------

    /** 计数型 +1 两次 → 当日该指标 SUM(value_num) = 2。 */
    @Test
    void countHabitAccumulatesValueOnRepeatedCheckin() throws Exception {
        String token = loginAndGetToken();
        String name = "m3计数-" + uuid();

        long habitId = readId(mockMvc.perform(post("/api/life/habits")
                        .header("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", name, "type", "count", "unit", "杯", "targetValue", 8))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.type").value("count"))
                .andReturn());

        LocalDate today = PeriodUtil.today();

        // 第一次 +1
        mockMvc.perform(post("/api/life/habits/{id}/checkin", habitId)
                        .header("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.value").value(1));

        // 第二次 +1 → 累加
        mockMvc.perform(post("/api/life/habits/{id}/checkin", habitId)
                        .header("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.value").value(2));

        // 当日 SUM(value_num) = 2（累加语义，不是覆盖为 1）
        BigDecimal sum = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(value_num),0) FROM sys_metric_record WHERE metric_id = ? AND record_date = ?",
                BigDecimal.class, habitId, today);
        assertEquals(0, new BigDecimal("2").compareTo(sum),
                "计数型两次 +1 后当日 SUM 应为 2（累加而非覆盖），实际=" + sum);

        cleanupHabit(habitId);
    }

    // ---- §7-4：streak — 3 天连续 → 3；断档 → 断档处停止 ----------------

    /** streak：造今天/昨天/前天 3 天连续打卡 → 返回 3。 */
    @Test
    void streakCountsThreeConsecutiveDays() throws Exception {
        String token = loginAndGetToken();
        long habitId = createCheckinHabit(token, "m3连续-" + uuid());
        LocalDate today = PeriodUtil.today();

        insertRecord(habitId, today, 1);
        insertRecord(habitId, today.minusDays(1), 1);
        insertRecord(habitId, today.minusDays(2), 1);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/life/habits/{id}/streak", habitId).header("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.streak").value(3));

        cleanupHabit(habitId);
    }

    /** streak：断档场景 —— 今天/昨天有效，前天有效但大前天断档，缺口在更早处停止。 */
    @Test
    void streakStopsAtGap() throws Exception {
        String token = loginAndGetToken();
        long habitId = createCheckinHabit(token, "m3断档-" + uuid());
        LocalDate today = PeriodUtil.today();

        // 今天、昨天有效，前天断档（缺），大前天有效 → streak 应为 2（在前天缺口处停止）
        insertRecord(habitId, today, 1);
        insertRecord(habitId, today.minusDays(1), 1);
        // today.minusDays(2) 故意缺失 —— 断档
        insertRecord(habitId, today.minusDays(3), 1);
        insertRecord(habitId, today.minusDays(4), 1);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/life/habits/{id}/streak", habitId).header("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.streak").value(2));

        cleanupHabit(habitId);
    }

    /** streak：今天未打则从昨天起算（今天不算断）。 */
    @Test
    void streakCountsFromYesterdayWhenTodayMissing() throws Exception {
        String token = loginAndGetToken();
        long habitId = createCheckinHabit(token, "m3今日未打-" + uuid());
        LocalDate today = PeriodUtil.today();

        // 今天未打，昨天/前天有效 → 从昨天起算 = 2
        insertRecord(habitId, today.minusDays(1), 1);
        insertRecord(habitId, today.minusDays(2), 1);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/life/habits/{id}/streak", habitId).header("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.streak").value(2));

        cleanupHabit(habitId);
    }

    /** streak：无任何记录 → 0。 */
    @Test
    void streakIsZeroWithoutRecords() throws Exception {
        String token = loginAndGetToken();
        long habitId = createCheckinHabit(token, "m3无记录-" + uuid());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/life/habits/{id}/streak", habitId).header("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.streak").value(0));

        cleanupHabit(habitId);
    }

    // ---- §7-5：快捷记录 ------------------------------------------------

    /** 「跑步 5」→ 返回预填 metricId 指向「跑步」指标、value=5，且不落库。 */
    @Test
    void quickRecordParsesRunningAndDoesNotPersist() throws Exception {
        String token = loginAndGetToken();
        // 「跑步」建为计数型（公里），带唯一后缀避免与并行数据冲突
        String name = "跑步" + uuid();
        long habitId = readId(mockMvc.perform(post("/api/life/habits")
                        .header("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", name, "type", "count", "unit", "公里", "targetValue", 5))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn());

        LocalDate today = PeriodUtil.today();
        long recordsBefore = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_metric_record", Long.class);

        // 解析「跑步 5」：注意首词必须匹配 name 前缀
        MvcResult qr = mockMvc.perform(post("/api/life/quick-record")
                        .header("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("text", name + " 5"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.metricId").value(habitId))
                .andExpect(jsonPath("$.data.metricName").value(name))
                .andExpect(jsonPath("$.data.value").value(5))
                .andExpect(jsonPath("$.data.date").value(PeriodUtil.formatDate(today)))
                .andReturn();

        // 只返回预填，不落库
        long recordsAfterParse = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_metric_record", Long.class);
        assertEquals(recordsBefore, recordsAfterParse,
                "quick-record 只解析不落库，记录数不应变化");

        // 确认后才落库
        long metricId = objectMapper.readTree(qr.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("data").path("metricId").asLong();
        BigDecimal value = objectMapper.readTree(qr.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("data").path("value").decimalValue();

        mockMvc.perform(post("/api/life/records")
                        .header("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "metricId", metricId, "value", value))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.metricId").value(habitId))
                .andExpect(jsonPath("$.data.value").value(5));

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_metric_record WHERE metric_id = ? AND record_date = ?",
                Integer.class, habitId, today);
        assertEquals(1, count, "确认后应落一条今日记录");

        cleanupHabit(habitId);
    }

    /** 「乱码 xyz」→ 14003（无法命中任何指标定义）。 */
    @Test
    void quickRecordUnmatchedTextReturns14003() throws Exception {
        String token = loginAndGetToken();

        // 用极不可能命中的乱码前缀
        String gibberish = "zz乱码" + uuid();

        mockMvc.perform(post("/api/life/quick-record")
                        .header("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("text", gibberish + " xyz"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(14003));
    }

    // ---- §7-6：打卡 → 活动流落一条 life/checkin ------------------------

    /** 打卡成功后 act_activity_log 新增一条 dimension=life、biz_type=checkin 的记录。 */
    @Test
    void checkinLogsLifeActivity() throws Exception {
        String token = loginAndGetToken();
        String name = "m3活动流-" + uuid();
        long habitId = createCheckinHabit(token, name);

        long before = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM act_activity_log WHERE dimension='life' AND biz_type='checkin'",
                Long.class);

        mockMvc.perform(post("/api/life/habits/{id}/checkin", habitId)
                        .header("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        long after = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM act_activity_log WHERE dimension='life' AND biz_type='checkin'",
                Long.class);
        assertEquals(before + 1, after, "打卡成功应落一条 life/checkin 活动流");

        // title = 习惯名
        String title = jdbcTemplate.queryForObject(
                "SELECT title FROM act_activity_log WHERE dimension='life' AND biz_type='checkin' "
                        + "ORDER BY id DESC LIMIT 1", String.class);
        assertEquals(name, title, "活动流 title 应为习惯名");

        cleanupHabit(habitId);
    }

    // ---- 附加：重复打卡不得重复写活动流 --------------------------------

    /** 重复打卡被拒 14002，且不额外写活动流（活动流只在成功分支写）。 */
    @Test
    void duplicateCheckinDoesNotLogAgain() throws Exception {
        String token = loginAndGetToken();
        long habitId = createCheckinHabit(token, "m3去重流-" + uuid());

        mockMvc.perform(post("/api/life/habits/{id}/checkin", habitId)
                        .header("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(jsonPath("$.code").value(0));

        long afterFirst = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM act_activity_log WHERE dimension='life' AND biz_type='checkin'",
                Long.class);

        mockMvc.perform(post("/api/life/habits/{id}/checkin", habitId)
                        .header("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(jsonPath("$.code").value(14002));

        long afterSecond = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM act_activity_log WHERE dimension='life' AND biz_type='checkin'",
                Long.class);
        assertEquals(afterFirst, afterSecond, "被拒打卡不应写活动流");

        cleanupHabit(habitId);
    }

    // ---- 通用辅助 ------------------------------------------------------

    /** 建一个打卡型习惯，返回 habitId。 */
    private long createCheckinHabit(String token, String name) throws Exception {
        return readId(mockMvc.perform(post("/api/life/habits")
                        .header("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", name, "type", "checkin"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn());
    }

    /** 从 MvcResult 的响应体 data.id 读取主键。 */
    private long readId(MvcResult result) throws Exception {
        JsonNode data = objectMapper.readTree(
                result.getResponse().getContentAsString(StandardCharsets.UTF_8)).path("data");
        long id = data.path("id").asLong();
        assertTrue(id > 0, "创建应返回有效 id");
        return id;
    }

    /** 直接插入一条指标记录（用于构造 streak 历史，绕过当日幂等限制）。 */
    private void insertRecord(long metricId, LocalDate date, int valueNum) {
        jdbcTemplate.update(
                "INSERT INTO sys_metric_record (metric_id, record_date, record_time, value_num, value_text, deleted, create_time) "
                        + "VALUES (?, ?, NOW(), ?, ?, 0, NOW())",
                metricId, date, new BigDecimal(valueNum), String.valueOf(valueNum));
    }

    /** 清理习惯定义及其记录，保证测试可重复运行。 */
    private void cleanupHabit(long habitId) {
        jdbcTemplate.update("DELETE FROM sys_metric_record WHERE metric_id = ?", habitId);
        jdbcTemplate.update("DELETE FROM act_activity_log WHERE title = "
                + "(SELECT name FROM sys_metric_def WHERE id = ?)", habitId);
        jdbcTemplate.update("DELETE FROM sys_metric_def WHERE id = ?", habitId);
    }

    private static String uuid() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

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
