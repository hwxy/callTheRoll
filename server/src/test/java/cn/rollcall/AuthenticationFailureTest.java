package cn.rollcall;

import cn.rollcall.api.AuthController;
import cn.rollcall.config.SecurityConfig;
import cn.rollcall.mapper.UserMapper;
import cn.rollcall.service.Sessions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.data.redis.RedisConnectionFailureException;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers=AuthController.class,excludeAutoConfiguration=org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class)
@Import(SecurityConfig.class)
class AuthenticationFailureTest {
    @Autowired MockMvc mvc;
    @MockitoBean Sessions sessions;
    @MockitoBean UserMapper users;
    @MockitoBean cn.rollcall.mapper.ActivityMapper activities;
    @MockitoBean cn.rollcall.mapper.DrawMapper draws;
    @MockitoBean cn.rollcall.mapper.SiteSettingMapper siteSettings;
    @Test void redisOutageFailsClosed() throws Exception {
        when(sessions.resolve("outage","/api/v1/auth/me")).thenThrow(new RedisConnectionFailureException("test outage"));
        mvc.perform(get("/api/v1/auth/me").header("Authorization","Bearer outage"))
            .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.message").value("认证服务暂不可用"));
    }
}
