package cn.rollcall.api;

import cn.rollcall.service.*;
import cn.rollcall.mapper.UserMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

@RestController
@RequestMapping("/api/v1")
public class AccountController {
    private final Accounts accounts;
    private final ExcelImports imports;
    private final UserMapper users;
    private final Sessions sessions;

    public AccountController(Accounts accounts, ExcelImports imports, UserMapper users, Sessions sessions) {
        this.accounts = accounts;
        this.imports = imports;
        this.users = users;
        this.sessions = sessions;
    }

    @GetMapping("/accounts")
    public Object list(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size, @RequestParam(defaultValue = "") String search) {
        return accounts.list(Access.user(), page, size, search);
    }

    @PostMapping("/accounts")
    public Object create(@Valid @RequestBody Accounts.Input input) {
        return accounts.create(Access.user(), input);
    }

    @PutMapping("/accounts/{id}")
    public Object update(@PathVariable Long id, @Valid @RequestBody Accounts.Input input) {
        return accounts.update(Access.user(), id, input);
    }

    public record Password(@NotBlank String password) {
    }

    @PostMapping("/accounts/{id}/password")
    public void reset(@PathVariable Long id, @Valid @RequestBody Password input) {
        accounts.reset(Access.user(), id, input.password());
    }

    @GetMapping("/teachers")
    public Object teachers() {
        Access.staff(Access.user());
        return users.teachers();
    }

    @GetMapping("/roles")
    public Object roles() {
        Access.adminOnly(Access.user());
        return List.of(Map.of("role", "ADMIN", "name", "系统管理员", "permission", "全部菜单、账号与活动"), Map.of("role", "TEACHER", "name", "老师", "permission", "自己创建的学生、自己及共享活动"), Map.of("role", "STUDENT", "name", "学生", "permission", "本人活动、宠物及积分"));
    }

    @GetMapping("/accounts/import/template")
    public ResponseEntity<byte[]> template() throws IOException {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=students.xlsx")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")).body(imports.template(Access.user()));
    }

    @PostMapping(value = "/accounts/import/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Object preview(@RequestPart("file") MultipartFile file) throws IOException {
        var u = Access.user();
        sessions.throttle("import:" + u.id, 10);
        return imports.preview(u, file);
    }

    public record Confirm(@NotBlank String token) {
    }

    @PostMapping("/accounts/import/confirm")
    public Object confirm(@Valid @RequestBody Confirm body) throws IOException {
        return imports.confirm(Access.user(), body.token());
    }
}
