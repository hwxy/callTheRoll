package cn.rollcall.api;

import cn.rollcall.service.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/activities")
public class ActivityController {
    private final Activities service;

    public ActivityController(Activities service) {
        this.service = service;
    }

    @GetMapping
    public Object list(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int size, @RequestParam(defaultValue = "all") String scope) {
        return service.list(Access.user(), page, size, scope);
    }

    @GetMapping("/{id}")
    public Object detail(@PathVariable Long id) {
        return service.detail(Access.user(), id);
    }

    @GetMapping("/candidates")
    public Object candidates(@RequestParam(required = false) Long activityId, @RequestParam(required = false) List<Long> teacherIds) {
        return service.candidates(Access.user(), activityId, teacherIds == null ? List.of() : teacherIds);
    }

    @PostMapping
    public Object create(@Valid @RequestBody Activities.Input input) {
        return service.create(Access.user(), input);
    }

    @PutMapping("/{id}")
    public Object update(@PathVariable Long id, @Valid @RequestBody Activities.Input input) {
        return service.update(Access.user(), id, input);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(Access.user(), id);
    }

    public record DrawInput(@NotBlank String requestKey) {
    }

    @PostMapping("/{id}/draws")
    public Object draw(@PathVariable Long id, @Valid @RequestBody DrawInput input) {
        return service.draw(Access.user(), id, input.requestKey());
    }

    public record Resolve(@NotNull Boolean award) {
    }

    @PostMapping("/{id}/draws/{drawId}/resolve")
    public Object resolve(@PathVariable Long id, @PathVariable Long drawId, @Valid @RequestBody Resolve input) {
        return service.resolve(Access.user(), id, drawId, input.award());
    }

    @PostMapping("/{id}/reset")
    public Object reset(@PathVariable Long id) {
        return service.reset(Access.user(), id);
    }

    @GetMapping("/{id}/draws")
    public Object history(@PathVariable Long id) {
        return service.history(Access.user(), id);
    }

    @GetMapping("/{id}/growth")
    public Object growth(@PathVariable Long id) {
        return service.growth(Access.user(), id);
    }

    public record Pet(@NotBlank String pet) {
    }

    @PostMapping("/{id}/pet")
    public Object pet(@PathVariable Long id, @Valid @RequestBody Pet input) {
        return service.pet(Access.user(), id, input.pet());
    }
}
