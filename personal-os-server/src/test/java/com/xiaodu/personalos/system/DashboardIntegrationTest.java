package com.xiaodu.personalos.system;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
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
 * 首页聚合接线集成测试（P1 集成阶段，RIce 定）。
 *
 * <p>验证 {@code /api/dashboard/home} 不再是骨架值，而是**真实读取 M1 工作域 / M2 SOP 库 / M3 生活域**的数据：</p>
 * <ol>
 *   <li>建任务 → work.todayTotal 增量 +1；完成任务 → work.todayDone 增量 +1</li>
 *   <li>建习惯 → life.checkinTotal 增量 +1；打卡 → life.checkinDone 增量 +1 且该习惯 done=true、streakDays >= 1</li>
 *   <li>建 SOP → 高频榜条目数不超过 3</li>
 *   <li>同名任务完成 3 次 → sopHints 出现该标题且 count >= 3（跨模块读活动流的证明）</li>
 * </ol>
 *
 * <p>数据用 UUID 隔离，测试尾部清理，重复运行幂等。</p>
 *
 * @author RIce
 */
@SpringBootTest(properties = "server.port=8090")
@AutoConfigureMockMvc
class DashboardIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /** 跨模块接线：任务 / 习惯 / SOP 全部反映到首页聚合。 */
    @Test
    void dashboardAggregatesAllThreeDomains() throws Exception {
        String token = loginAndGetToken();
        String tag = "dash" + UUID.randomUUID().toString().substring(0, 8);

        JsonNode before = dashboard(token);
        int workTotalBefore = before.path("work").path("todayTotal").asInt();
        int workDoneBefore = before.path("work").path("todayDone").asInt();
        int lifeTotalBefore = before.path("life").path("checkinTotal").asInt();
        int lifeDoneBefore = before.path("life").path("checkinDone").asInt();

        Long taskId = null;
        Long habitId = null;
        Long sopId = null;
        try {
            // ---- M1 工作域：建任务 -----------------------------------
            taskId = createTask(token, "集成校验任务-" + tag);
            JsonNode afterCreate = dashboard(token);
            assertEquals(workTotalBefore + 1, afterCreate.path("work").path("todayTotal").asInt(),
                    "新建任务后首页 todayTotal 应 +1（M1 接线）");

            // ---- M1 工作域：完成任务 ---------------------------------
            mockMvc.perform(put("/api/work/tasks/" + taskId + "/status")
                            .header("token", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\":\"done\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));

            JsonNode afterDone = dashboard(token);
            assertEquals(workDoneBefore + 1, afterDone.path("work").path("todayDone").asInt(),
                    "完成任务后首页 todayDone 应 +1（M1 接线）");

            // ---- M3 生活域：建习惯 + 打卡 ----------------------------
            habitId = createHabit(token, "集成校验习惯-" + tag);
            JsonNode afterHabit = dashboard(token);
            assertEquals(lifeTotalBefore + 1, afterHabit.path("life").path("checkinTotal").asInt(),
                    "新建习惯后首页 checkinTotal 应 +1（M3 接线）");

            mockMvc.perform(post("/api/life/habits/" + habitId + "/checkin")
                            .header("token", token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"value\":1}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value(0));

            JsonNode afterCheckin = dashboard(token);
            assertEquals(lifeDoneBefore + 1, afterCheckin.path("life").path("checkinDone").asInt(),
                    "打卡后首页 checkinDone 应 +1（M3 接线）");
            assertTrue(afterCheckin.path("streakDays").asInt() >= 1, "打卡后 streakDays 应 >= 1");

            boolean habitFound = false;
            for (JsonNode h : afterCheckin.path("life").path("habits")) {
                if (h.path("id").asLong() == habitId) {
                    habitFound = true;
                    assertTrue(h.path("done").asBoolean(), "已打卡习惯在首页应标记 done=true");
                }
            }
            assertTrue(habitFound, "新建的习惯应出现在首页 habits[] 中");

            // ---- M2 SOP 库：建 SOP，高频榜不超限 ----------------------
            sopId = createSop(token, "集成校验SOP-" + tag);
            JsonNode afterSop = dashboard(token);
            assertTrue(afterSop.path("sop").path("top").size() <= 3,
                    "首页高频 SOP 榜最多 3 条（M2 接线）");

            // ---- 跨模块：同名任务完成 3 次 → SOP 提示 -----------------
            String repeated = "重复劳动任务-" + tag;
            for (int i = 0; i < 3; i++) {
                Long id = createTask(token, repeated);
                mockMvc.perform(put("/api/work/tasks/" + id + "/status")
                                .header("token", token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"status\":\"done\"}"))
                        .andExpect(status().isOk());
                deleteTask(token, id);
            }

            JsonNode afterHints = dashboard(token);
            boolean hintFound = false;
            for (JsonNode hint : afterHints.path("sopHints")) {
                if (repeated.equals(hint.path("taskTitle").asText())) {
                    hintFound = true;
                    assertTrue(hint.path("count").asInt() >= 3,
                            "重复完成 3 次的标题，提示次数应 >= 3");
                }
            }
            assertTrue(hintFound, "重复完成 3 次的任务标题应出现在 sopHints 中（跨模块读活动流）");
        } finally {
            // ---- 清理（活动流按设计保留，不清理） ----
            if (sopId != null) {
                mockMvc.perform(delete("/api/sop/" + sopId).header("token", token));
            }
            if (habitId != null) {
                mockMvc.perform(delete("/api/life/habits/" + habitId).header("token", token));
            }
            if (taskId != null) {
                deleteTask(token, taskId);
            }
        }
    }

    // ---- 辅助 ----------------------------------------------------------

    private JsonNode dashboard(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/dashboard/home").header("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("data");
    }

    private Long createTask(String token, String title) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/work/tasks")
                        .header("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("data");
        assertNotNull(data.asLong(), "新建任务应返回 id");
        return data.asLong();
    }

    private void deleteTask(String token, Long id) throws Exception {
        mockMvc.perform(delete("/api/work/tasks/" + id).header("token", token));
    }

    private Long createHabit(String token, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/life/habits")
                        .header("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"type\":\"checkin\",\"unit\":\"次\",\"targetValue\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("data").path("id").asLong();
    }

    private Long createSop(String token, String title) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/sop")
                        .header("token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\",\"category\":\"集成校验\",\"steps\":["
                                + "{\"title\":\"步骤一\"},{\"title\":\"步骤二\"}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .path("data").path("id").asLong();
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
