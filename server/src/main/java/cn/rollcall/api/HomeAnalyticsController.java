package cn.rollcall.api;

import cn.rollcall.service.Access;
import cn.rollcall.service.HomeAnalytics;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class HomeAnalyticsController {
    private final HomeAnalytics analytics;

    public HomeAnalyticsController(HomeAnalytics analytics) {
        this.analytics = analytics;
    }

    public record VisitInput(@NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{20,80}") String visitorId) {
    }

    @PostMapping("/analytics/home-visit")
    public Map<String, Object> recordHomeVisit(@Valid @RequestBody VisitInput input) {
        analytics.recordHomeVisit(input.visitorId());
        return Map.of("recorded", true);
    }

    @GetMapping("/admin/analytics/home")
    public Map<String, Object> homeSummary(@RequestParam(defaultValue = "30") int days) {
        return analytics.summary(Access.user(), days);
    }
}
