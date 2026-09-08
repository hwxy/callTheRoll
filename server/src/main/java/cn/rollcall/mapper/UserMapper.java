package cn.rollcall.mapper;

import cn.rollcall.model.Models.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;

import java.util.List;

public interface UserMapper extends BaseMapper<User> {
    @Select("SELECT u.* FROM app_user u JOIN login_alias a ON a.user_id=u.id WHERE a.alias=#{alias}")
    User byAlias(String alias);

    @Select("SELECT * FROM app_user WHERE id=#{id} FOR UPDATE")
    User lock(Long id);

    @Insert("INSERT INTO login_alias(alias,user_id) VALUES(#{alias},#{userId})")
    void addAlias(@Param("alias") String alias, @Param("userId") Long userId);

    @Delete("DELETE FROM login_alias WHERE user_id=#{id}")
    void clearAliases(Long id);

    @Select("SELECT COUNT(*) FROM app_user WHERE owner_teacher_id=#{id}")
    long ownedCount(Long id);

    @Select("SELECT (SELECT COUNT(*) FROM activity WHERE creator_id=#{id}) + (SELECT COUNT(*) FROM activity_teacher WHERE teacher_id=#{id}) + (SELECT COUNT(*) FROM activity_student WHERE student_id=#{id})")
    long relationCount(Long id);

    @Select("SELECT id,name FROM app_user WHERE role='TEACHER' AND enabled=1 ORDER BY id DESC")
    List<java.util.Map<String, Object>> teachers();
}
