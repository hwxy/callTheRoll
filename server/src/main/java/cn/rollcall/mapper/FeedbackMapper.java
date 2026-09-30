package cn.rollcall.mapper;

import cn.rollcall.model.Models.Feedback;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface FeedbackMapper extends BaseMapper<Feedback> {
    @Select("SELECT * FROM user_feedback ORDER BY created_at DESC, id DESC LIMIT #{limit} OFFSET #{offset}")
    List<Feedback> page(@Param("limit") int limit, @Param("offset") int offset);

    @Select("SELECT COUNT(*) FROM user_feedback")
    long countAll();
}
