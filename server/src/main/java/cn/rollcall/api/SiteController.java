package cn.rollcall.api;

import cn.rollcall.service.Access;
import cn.rollcall.service.SiteSettings;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/site")
public class SiteController {
    private final SiteSettings settings;

    public SiteController(SiteSettings settings) {
        this.settings = settings;
    }

    @GetMapping("/about")
    public Object about() {
        return settings.about();
    }

    public record AboutInput(@NotNull @Size(max = 10000) String content) {
    }

    @PutMapping("/about")
    public Object updateAbout(@Valid @RequestBody AboutInput input) {
        return settings.updateAbout(Access.user(), input.content());
    }
}
