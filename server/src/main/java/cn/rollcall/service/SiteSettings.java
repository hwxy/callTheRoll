package cn.rollcall.service;

import cn.rollcall.mapper.SiteSettingMapper;
import cn.rollcall.model.Models.User;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SiteSettings {
    private final SiteSettingMapper settings;
    public SiteSettings(SiteSettingMapper settings){this.settings=settings;}

    public Map<String,Object> about(){
        var value=settings.selectById("about_us");
        var result=new LinkedHashMap<String,Object>();
        result.put("content",value==null?"":value.settingValue);
        result.put("updatedAt",value==null?null:value.updatedAt);
        return result;
    }

    @Transactional
    public Map<String,Object> updateAbout(User actor,String content){
        Access.adminOnly(actor);
        String normalized=content==null?"":content.strip();
        settings.saveAbout(normalized,actor.id);
        settings.auditAbout(actor.id);
        return about();
    }
}
