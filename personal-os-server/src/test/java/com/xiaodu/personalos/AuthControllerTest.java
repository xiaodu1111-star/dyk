package com.xiaodu.personalos;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 登录闭环接口测试。
 *
 * <p>直接连本地 MySQL（本机 MySQL 常驻）。依赖 {@code DataInitializer} 播种的
 * {@code admin / admin123}；DataInitializer 带存在性判断，重复运行幂等、不污染库。</p>
 *
 * @author Kou
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    private static final String LOGIN_URL = "/api/auth/login";
    private static final String ME_URL = "/api/auth/me";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /** 用例 1：admin/admin123 登录成功，返回 token。 */
    @Test
    void loginWithCorrectCredentialsReturnsToken() throws Exception {
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.token").isNotEmpty())
                .andExpect(jsonPath("$.data.username").value("admin"));
    }

    /** 用例 2：密码错误 → 15003。 */
    @Test
    void loginWithWrongPasswordReturns15003() throws Exception {
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(15003));
    }

    /** 用例 3：用户不存在 → 15002。 */
    @Test
    void loginWithUnknownUserReturns15002() throws Exception {
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"no-such-user\",\"password\":\"whatever\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(15002));
    }

    /** 用例 4：不带 token 访问 /me → 401（HTTP 仍为 200）。 */
    @Test
    void meWithoutTokenReturns401() throws Exception {
        mockMvc.perform(get(ME_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    /** 用例 5：先登录拿 token，再带 token 访问 /me → 返回昵称。 */
    @Test
    void meWithTokenReturnsNickname() throws Exception {
        String token = loginAndGetToken();

        mockMvc.perform(get(ME_URL).header("token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.nickname").value("小杜"))
                .andExpect(jsonPath("$.data.city").value("嘉兴"));
    }

    /** 用例 6（CORS 安全回归）：非白名单来源的预检被拒，且不得回显 Access-Control-Allow-Origin。 */
    @Test
    void corsPreflightFromDisallowedOriginIsNotAllowed() throws Exception {
        mockMvc.perform(options(LOGIN_URL)
                        .header(HttpHeaders.ORIGIN, "http://evil.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    /** 用例 7（畸形 JSON 回归）：body 非法 JSON → 400。 */
    @Test
    void malformedJsonBodyReturns400() throws Exception {
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{bad json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    /** 登录并取出 token。 */
    private String loginAndGetToken() throws Exception {
        MvcResult result = mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andReturn();

        String body = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode node = objectMapper.readTree(body);
        return node.path("data").path("token").asText();
    }
}
