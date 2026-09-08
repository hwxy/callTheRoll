package cn.rollcall.config;

import cn.rollcall.model.Models.User;
import cn.rollcall.mapper.UserMapper;
import cn.rollcall.service.Accounts;
import cn.rollcall.api.Problem;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;

@Component
public class Bootstrap implements CommandLineRunner {
    private final UserMapper users;
    private final PasswordEncoder encoder;
    private final String login;
    private final String password;

    public Bootstrap(UserMapper users, PasswordEncoder encoder, @Value("${app.bootstrap.login}") String login, @Value("${app.bootstrap.password}") String password) {
        this.users = users;
        this.encoder = encoder;
        this.login = login;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (login.isBlank()) return;
        if (users.selectCount(new QueryWrapper<User>().eq("role", "ADMIN")) > 0) return;
        Problem.require(login.matches("[A-Za-z0-9_-]{1,64}"), 400, "初始化管理员账号格式不正确");
        Accounts.password(password);
        var u = new User();
        u.name = "系统管理员";
        u.studentNo = login;
        u.passwordHash = encoder.encode(password);
        u.role = "ADMIN";
        u.enabled = true;
        u.authVersion = 0;
        u.version = 0;
        users.insert(u);
        users.addAlias(login, u.id);
    }
}
