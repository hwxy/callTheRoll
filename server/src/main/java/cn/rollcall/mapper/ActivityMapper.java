package cn.rollcall.mapper;

import cn.rollcall.model.Models.Activity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;
import java.util.*;

public interface ActivityMapper extends BaseMapper<Activity> {
    @Select("SELECT * FROM activity WHERE id=#{id} AND deleted=0 FOR UPDATE") Activity lock(Long id);
    @Select("SELECT COUNT(*) FROM activity_teacher WHERE activity_id=#{activityId} AND teacher_id=#{userId}")
    long shared(@Param("activityId") Long activityId,@Param("userId") Long userId);
    @Select("SELECT teacher_id FROM activity_teacher WHERE activity_id=#{id}") List<Long> teacherIds(Long id);
    @Delete("DELETE FROM activity_teacher WHERE activity_id=#{id}") void clearTeachers(Long id);
    @Insert("INSERT INTO activity_teacher(activity_id,teacher_id) VALUES(#{id},#{teacher})")
    void addTeacher(@Param("id") Long id,@Param("teacher") Long teacher);
    @Select("SELECT student_id FROM activity_student WHERE activity_id=#{id} AND active=1") List<Long> studentIds(Long id);
    @Update("UPDATE activity_student SET active=0 WHERE activity_id=#{id}") void clearStudents(Long id);
    @Insert("INSERT INTO activity_student(activity_id,student_id) VALUES(#{id},#{student}) ON DUPLICATE KEY UPDATE active=1")
    void addStudent(@Param("id") Long id,@Param("student") Long student);
    @Select("SELECT COUNT(*) FROM activity_student WHERE activity_id=#{id} AND student_id=#{student} AND active=1")
    long isMember(@Param("id") Long id,@Param("student") Long student);
    @Select("SELECT u.id,u.name,u.student_no AS studentNo,u.enabled FROM activity_student s JOIN app_user u ON u.id=s.student_id WHERE s.activity_id=#{id} AND s.active=1 ORDER BY u.id")
    List<Map<String,Object>> members(Long id);
    @Select("<script>SELECT u.id FROM activity_student s JOIN app_user u ON u.id=s.student_id WHERE s.activity_id=#{id} AND s.active=1 AND u.enabled=1 AND u.role='STUDENT' <if test='!repeat'>AND NOT EXISTS(SELECT 1 FROM draw_record d WHERE d.activity_id=#{id} AND d.round_no=#{round} AND d.student_id=u.id)</if> ORDER BY u.id</script>")
    List<Long> candidates(@Param("id") Long id,@Param("round") int round,@Param("repeat") boolean repeat);
    @Select("SELECT points,pet FROM activity_student WHERE activity_id=#{id} AND student_id=#{student} AND active=1")
    Map<String,Object> growth(@Param("id") Long id,@Param("student") Long student);
    @Update("UPDATE activity_student SET points=points+1 WHERE activity_id=#{id} AND student_id=#{student} AND active=1")
    int addPoint(@Param("id") Long id,@Param("student") Long student);
    @Update("UPDATE activity_student SET pet=#{pet} WHERE activity_id=#{id} AND student_id=#{student} AND active=1 AND pet IS NULL")
    int choosePet(@Param("id") Long id,@Param("student") Long student,@Param("pet") String pet);
    @Insert("INSERT INTO score_ledger(draw_id,activity_id,student_id,teacher_id) VALUES(#{draw},#{id},#{student},#{teacher})")
    void ledger(@Param("draw") Long draw,@Param("id") Long id,@Param("student") Long student,@Param("teacher") Long teacher);
    @Select("SELECT l.id,l.delta,l.created_at AS createdAt,u.name AS teacherName FROM score_ledger l JOIN app_user u ON u.id=l.teacher_id WHERE l.activity_id=#{id} AND l.student_id=#{student} ORDER BY l.id DESC LIMIT 100")
    List<Map<String,Object>> scores(@Param("id") Long id,@Param("student") Long student);
    @Insert("INSERT INTO audit_log(actor_id,action,target_id) VALUES(#{actor},#{action},#{target})")
    void audit(@Param("actor") Long actor,@Param("action") String action,@Param("target") Long target);
}
