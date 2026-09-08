package cn.rollcall.mapper;

import cn.rollcall.model.Models.SiteSetting;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.*;

public interface SiteSettingMapper extends BaseMapper<SiteSetting> {
    @Insert("""
            INSERT INTO site_setting(setting_key,setting_value,updated_by,version)
            VALUES('about_us',#{content},#{userId},0)
            ON DUPLICATE KEY UPDATE setting_value=VALUES(setting_value),updated_by=VALUES(updated_by),
            updated_at=CURRENT_TIMESTAMP,version=version+1
            """)
    void saveAbout(@Param("content") String content, @Param("userId") Long userId);

    @Insert("INSERT INTO audit_log(actor_id,action,target_id) VALUES(#{userId},'SITE_ABOUT_UPDATE',0)")
    void auditAbout(Long userId);
}
