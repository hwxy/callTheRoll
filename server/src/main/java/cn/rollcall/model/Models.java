package cn.rollcall.model;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;

public final class Models {
    private Models() {
    }

    @TableName("app_user")
    public static class User {
        @TableId(type = IdType.AUTO)
        public Long id;
        public String name;
        @TableField(updateStrategy = FieldStrategy.ALWAYS)
        public String studentNo;
        @TableField(updateStrategy = FieldStrategy.ALWAYS)
        public String phone;
        @JsonIgnore
        public String passwordHash;
        public String role;
        @TableField(updateStrategy = FieldStrategy.ALWAYS)
        public Long ownerTeacherId;
        public Boolean enabled;
        @JsonIgnore
        public Integer authVersion;
        @Version
        public Integer version;
        public LocalDateTime createdAt;
    }

    @TableName("activity")
    public static class Activity {
        @TableId(type = IdType.AUTO)
        public Long id;
        public String name;
        public Long creatorId;
        public Boolean repeatDraw;
        public Integer roundNo;
        @Version
        public Integer version;
        @TableLogic
        public Integer deleted;
        public LocalDateTime createdAt;
    }

    @TableName("draw_record")
    public static class Draw {
        @TableId(type = IdType.AUTO)
        public Long id;
        public Long activityId;
        public Long studentId;
        public Long teacherId;
        public Integer roundNo;
        public String requestKey;
        public String status;
        public Long resolvedBy;
        public LocalDateTime createdAt;
        public LocalDateTime resolvedAt;
    }

    @TableName("site_setting")
    public static class SiteSetting {
        @TableId(type = IdType.INPUT)
        public String settingKey;
        public String settingValue;
        public Long updatedBy;
        public LocalDateTime updatedAt;
        @Version
        public Integer version;
    }
}
