package cn.rollcall.service;

import cn.rollcall.api.Problem;
import cn.rollcall.mapper.*;
import cn.rollcall.model.Models.User;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.constraints.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.*;
import java.nio.charset.StandardCharsets;

@Service
public class Accounts {
    public record Input(@NotBlank @Size(max=80) String name,@Size(max=64) String studentNo,
        @Size(max=20) String phone,String password,@NotBlank String role,Long ownerTeacherId,
        @NotNull Boolean enabled,Integer version) {}
    private final UserMapper users;private final ActivityMapper audit;private final PasswordEncoder encoder;
    public Accounts(UserMapper users,ActivityMapper audit,PasswordEncoder encoder) {this.users=users;this.audit=audit;this.encoder=encoder;}
    public static String clean(String s) {return s==null||s.isBlank()?null:s.strip();}
    public static void password(String s) {
        Problem.require(s!=null&&s.length()>=6&&s.getBytes(StandardCharsets.UTF_8).length<=72,400,"密码至少 6 位，且不超过 72 字节");
    }
    public Object list(User actor,int page,int size,String search) {
        Access.staff(actor);var q=new QueryWrapper<User>();
        if(!Access.admin(actor)) q.eq("role","STUDENT").eq("owner_teacher_id",actor.id);
        if(clean(search)!=null) q.and(w->w.like("name",search).or().like("student_no",search).or().like("phone",search));
        return users.selectPage(new Page<>(Math.max(page,1),Math.clamp(size,1,100)),q.orderByDesc("id"));
    }
    public User prepare(User actor,Input input,Long editing) {
        Access.staff(actor);
        Problem.require(Set.of("ADMIN","TEACHER","STUDENT").contains(input.role()),400,"角色不正确");
        Problem.require(Access.admin(actor)||input.role().equals("STUDENT"),403,"老师只能创建学生");
        User u=new User();u.name=clean(input.name());u.studentNo=clean(input.studentNo());u.phone=clean(input.phone());
        Problem.require(u.name!=null&&u.name.length()<=80,400,"姓名不能为空且最多 80 字");
        Problem.require(u.studentNo!=null||u.phone!=null,400,"学号和手机号至少填写一个");
        Problem.require(u.studentNo==null||u.studentNo.matches("[A-Za-z0-9_-]{1,64}"),400,"学号仅支持字母、数字、下划线和短横线");
        Problem.require(u.phone==null||u.phone.matches("1[3-9][0-9]{9}"),400,"请输入 11 位中国大陆手机号");
        u.role=input.role();u.enabled=input.enabled();u.ownerTeacherId=null;
        if(u.role.equals("STUDENT")) {
            u.ownerTeacherId=Access.admin(actor)?input.ownerTeacherId():actor.id;
            Problem.require(!Objects.equals(u.ownerTeacherId,editing),400,"学生不能归属自己，请选择其他老师");
            User owner=u.ownerTeacherId==null?null:users.selectById(u.ownerTeacherId);
            Problem.require(owner!=null&&owner.role.equals("TEACHER")&&owner.enabled,400,"请选择有效的归属老师");
        }
        for(String alias:aliases(u)) {User existing=users.byAlias(alias);Problem.require(existing==null||Objects.equals(existing.id,editing),409,"学号或手机号已被使用");}
        return u;
    }
    private Set<String> aliases(User u) {var a=new HashSet<String>();if(u.studentNo!=null)a.add(u.studentNo);if(u.phone!=null)a.add(u.phone);return a;}
    private User insert(User actor,User u) {
        u.authVersion=0;u.version=0;users.insert(u);
        for(String alias:aliases(u)) users.addAlias(alias,u.id);
        audit.audit(actor.id,"ACCOUNT_CREATE",u.id);return u;
    }
    @Transactional public User create(User actor,Input input) {
        var u=prepare(actor,input,null);password(input.password());u.passwordHash=encoder.encode(input.password());return insert(actor,u);
    }
    @Transactional public User update(User actor,Long id,Input input) {
        User previous=users.lock(id);Access.account(actor,previous);
        Problem.require(input.version()!=null&&input.version().equals(previous.version),409,"账号已被修改，请刷新后重试");
        Problem.require(!actor.id.equals(id)||(previous.role.equals(input.role())&&input.enabled()),400,"不能停用自己或改变自己的角色");
        if(!previous.role.equals(input.role())) Problem.require(users.ownedCount(id)==0&&users.relationCount(id)==0,409,"账号已有业务关联，不能改变角色");
        var u=prepare(actor,input,id);u.id=id;u.version=previous.version;u.authVersion=previous.authVersion;
        if(!Objects.equals(previous.enabled,u.enabled)||!previous.role.equals(u.role))u.authVersion++;
        u.passwordHash=previous.passwordHash;
        Problem.require(users.updateById(u)==1,409,"账号已被修改，请刷新");
        users.clearAliases(id);for(String alias:aliases(u))users.addAlias(alias,id);
        audit.audit(actor.id,"ACCOUNT_UPDATE",id);return users.selectById(id);
    }
    @Transactional public void reset(User actor,Long id,String password) {
        var u=users.lock(id);Access.account(actor,u);password(password);
        u.passwordHash=encoder.encode(password);u.authVersion++;users.updateById(u);audit.audit(actor.id,"PASSWORD_RESET",id);
    }
    public String hashPassword(String value) {password(value);return encoder.encode(value);}
    public record ImportRow(String name,String studentNo,String phone,Long ownerTeacherId,String passwordHash) {}
    @Transactional public int importRows(User actor,List<ImportRow> rows) {
        Problem.require(!rows.isEmpty()&&rows.size()<=500,400,"每次导入 1～500 名学生");
        for(var row:rows) {
            var input=new Input(row.name(),row.studentNo(),row.phone(),null,"STUDENT",row.ownerTeacherId(),true,null);
            User u=prepare(actor,input,null);u.passwordHash=row.passwordHash();insert(actor,u);
        }
        audit.audit(actor.id,"ACCOUNT_IMPORT",(long)rows.size());return rows.size();
    }
}
