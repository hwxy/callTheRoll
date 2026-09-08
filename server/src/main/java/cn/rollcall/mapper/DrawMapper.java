package cn.rollcall.mapper;

import cn.rollcall.model.Models.Draw;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;
import java.util.*;

public interface DrawMapper extends BaseMapper<Draw> {
    @Select("SELECT d.id,d.student_id AS studentId,u.name AS studentName,d.teacher_id AS teacherId,t.name AS teacherName,d.round_no AS roundNo,d.status,d.created_at AS createdAt FROM draw_record d JOIN app_user u ON u.id=d.student_id JOIN app_user t ON t.id=d.teacher_id WHERE d.activity_id=#{id} ORDER BY d.id DESC LIMIT 100")
    List<Map<String,Object>> history(Long id);
}
