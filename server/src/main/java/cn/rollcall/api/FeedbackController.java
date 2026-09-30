package cn.rollcall.api;

import cn.rollcall.model.Models.User;
import cn.rollcall.service.Access;
import cn.rollcall.service.Feedbacks;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class FeedbackController {
    private final Feedbacks feedbacks;

    public FeedbackController(Feedbacks feedbacks) {
        this.feedbacks = feedbacks;
    }

    public record SubmitInput(
            @NotBlank @Size(max = 120) String title,
            @NotBlank @Size(max = 4000) String content,
            @Size(max = 120) String contact) {
    }

    @PostMapping("/feedback")
    public Object submit(@Valid @RequestBody SubmitInput input, HttpServletRequest request) {
        return feedbacks.submit(input.title(), input.content(), input.contact(), request.getRemoteAddr());
    }

    @GetMapping("/admin/feedback")
    public Object page(@RequestParam(defaultValue = "1") int page,
                       @RequestParam(defaultValue = "20") int size) {
        User actor = Access.user();
        return feedbacks.page(actor, page, size);
    }

    @GetMapping("/admin/feedback/{id}")
    public Object detail(@PathVariable long id) {
        return feedbacks.detail(Access.user(), id);
    }
}
