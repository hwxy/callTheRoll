package cn.rollcall.api;

import cn.rollcall.mapper.UserMapper;
import cn.rollcall.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final UserMapper users;
    private final Sessions sessions;
    private final Accounts accounts;
    private final PasswordEncoder encoder;
    private final String dummyHash;

    public AuthController(UserMapper users, Sessions sessions, Accounts accounts, PasswordEncoder encoder) {
        this.users = users;
        this.sessions = sessions;
        this.accounts = accounts;
        this.encoder = encoder;
        this.dummyHash = encoder.encode(java.util.UUID.randomUUID().toString());
    }

    public record Login(@NotBlank @Size(max = 64) String login, @NotBlank @Size(max = 72) String password) {
    }

    public record TeacherRegistration(@NotBlank @Size(max = 80) String name,
            @NotBlank @Pattern(regexp = "1[3-9][0-9]{9}", message = "请输入 11 位中国大陆手机号") String phone,
            @NotBlank @Size(min = 6, max = 72) String password) {
    }
    public record ConsoleTicket(@NotBlank @Size(max = 128) String ticket) {
    }

    @PostMapping("/console-ticket")
    @Operation(summary = "为当前老师签发一次性管理后台跳转凭证")
    public Object createConsoleTicket() {
        return Map.of("ticket", sessions.createConsoleTicket(Access.user()));
    }

    @PostMapping("/console-ticket/exchange")
    @Operation(summary = "兑换一次性管理后台跳转凭证")
    public Object exchangeConsoleTicket(@Valid @RequestBody ConsoleTicket body) {
        return sessions.exchangeConsoleTicket(body.ticket());
    }

    @PostMapping("/register/teacher")
    @Operation(summary = "公开注册老师账号；角色固定为老师，不接受客户端角色参数")
    public Object registerTeacher(@Valid @RequestBody TeacherRegistration body, HttpServletRequest request) {
        sessions.throttle("teacher-register-ip:" + request.getRemoteAddr(), 8);
        var user = accounts.registerTeacher(body.name(), body.phone(), body.password());
        return Map.of("token", sessions.create(user, "app"), "user", user);
    }

    @PostMapping("/login")
    @Operation(summary = "统一登录，角色由服务端账号确定；X-Client 为 console 或 app")
    public Object login(@Valid @RequestBody Login body, @RequestHeader("X-Client") String client, HttpServletRequest request) {
        Problem.require(client.equals("app") || client.equals("console"), 400, "无效的客户端入口");
        String alias = body.login().strip();
        sessions.throttle("ip:" + request.getRemoteAddr(), 100);
        sessions.checkFailures(alias);
        var user = users.byAlias(alias);
        boolean matches = encoder.matches(body.password(), user == null ? dummyHash : user.passwordHash);
        if (user == null || !matches || !Boolean.TRUE.equals(user.enabled)) {
            sessions.failedLogin(alias);
            throw new Problem(401, "账号或密码错误，或账号已停用");
        }
        Problem.require(!client.equals("console") || !user.role.equals("STUDENT"), 403, "学生不能登录管理后台");
        Problem.require(!client.equals("app") || !user.role.equals("ADMIN"), 403, "系统管理员请使用管理后台");
        sessions.successfulLogin(alias);
        return Map.of("token", sessions.create(user, client), "user", user);
    }

    @GetMapping("/me")
    @Operation(summary = "获取当前用户及真实角色")
    public Object me() {
        return Access.user();
    }

    @PostMapping("/logout")
    public void logout(@RequestHeader("Authorization") String header) {
        sessions.revoke(header.substring(7));
    }
}
