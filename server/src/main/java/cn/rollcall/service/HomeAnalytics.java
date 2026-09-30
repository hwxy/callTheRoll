package cn.rollcall.service;

import cn.rollcall.api.Problem;
import cn.rollcall.mapper.HomeAnalyticsMapper;
import cn.rollcall.model.Models.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class HomeAnalytics {
    private static final ZoneId REPORT_ZONE = ZoneId.of("Asia/Shanghai");
    private final HomeAnalyticsMapper analytics;

    public HomeAnalytics(HomeAnalyticsMapper analytics) {
        this.analytics = analytics;
    }

    @Transactional
    public void recordHomeVisit(String visitorId) {
        LocalDate today = LocalDate.now(REPORT_ZONE);
        analytics.removePreviousVisitorHashes(today.minusDays(89));
        analytics.incrementPv(today);
        String visitorHash = hash(visitorId);
        if (analytics.recordVisitor(today, visitorHash) == 1) analytics.incrementUv(today);
    }

    public Map<String, Object> summary(User actor, int days) {
        Access.adminOnly(actor);
        Problem.require(List.of(7, 30, 90).contains(days), 400, "统计范围仅支持 7、30 或 90 天");
        LocalDate to = LocalDate.now(REPORT_ZONE);
        LocalDate from = to.minusDays(days - 1L);
        List<Map<String, Object>> rows = analytics.daily(from, to);
        long totalPv = rows.stream().mapToLong(row -> ((Number) row.get("pv")).longValue()).sum();
        long totalUv = analytics.countUniqueVisitors(from, to);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("days", days);
        result.put("from", from.toString());
        result.put("to", to.toString());
        result.put("totalPv", totalPv);
        result.put("totalUv", totalUv);
        result.put("records", rows);
        return result;
    }

    private String hash(String value) {
        try {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is unavailable", e);
        }
    }
}
