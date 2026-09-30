package cn.rollcall.service;

import cn.rollcall.api.Problem;
import cn.rollcall.mapper.FeedbackMapper;
import cn.rollcall.model.Models.Feedback;
import cn.rollcall.model.Models.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class Feedbacks {
    private final FeedbackMapper feedback;
    private final Sessions sessions;

    public Feedbacks(FeedbackMapper feedback, Sessions sessions) {
        this.feedback = feedback;
        this.sessions = sessions;
    }

    @Transactional
    public Map<String, Object> submit(String title, String content, String contact, String remoteAddress) {
        sessions.throttle("feedback:" + remoteAddress, 5);
        var row = new Feedback();
        row.title = title.strip();
        row.content = content.strip();
        row.contact = contact == null || contact.isBlank() ? null : contact.strip();
        feedback.insert(row);
        return Map.of("id", row.id, "message", "问题已提交，感谢你的反馈");
    }

    public Map<String, Object> page(User actor, int page, int size) {
        Access.adminOnly(actor);
        Problem.require(page >= 1 && page <= 10000, 400, "页码超出范围");
        Problem.require(size >= 1 && size <= 100, 400, "每页数量需在 1 到 100 之间");
        int offset = (page - 1) * size;
        var result = new LinkedHashMap<String, Object>();
        result.put("records", feedback.page(size, offset));
        result.put("total", feedback.countAll());
        return result;
    }

    public Feedback detail(User actor, long id) {
        Access.adminOnly(actor);
        var row = feedback.selectById(id);
        Problem.require(row != null, 404, "问题记录不存在");
        return row;
    }
}
