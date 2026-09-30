package cn.rollcall.service;

import cn.rollcall.api.Problem;
import cn.rollcall.mapper.*;
import cn.rollcall.model.Models.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.constraints.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.security.crypto.password.PasswordEncoder;

@Service
public class Activities {
    public record Input(@NotBlank @Size(max=100) String name,@NotNull Boolean repeatDraw,
        @NotNull @Size(max=1000) List<@NotNull Long> studentIds,@NotNull @Size(max=100) List<@NotNull Long> teacherIds,
        @Size(max=200) List<@NotBlank @Size(max=80) String> studentNames,Integer version){
        public Input(String name,Boolean repeatDraw,List<Long> studentIds,List<Long> teacherIds,Integer version){
            this(name,repeatDraw,studentIds,teacherIds,null,version);
        }
    }
    private static final List<String> PETS=List.of("cat","rabbit","dragon");
    private final ActivityMapper activities;private final UserMapper users;private final DrawMapper draws;private final PasswordEncoder passwords;
    private final SecureRandom random=new SecureRandom();
    public Activities(ActivityMapper activities,UserMapper users,DrawMapper draws,PasswordEncoder passwords){this.activities=activities;this.users=users;this.draws=draws;this.passwords=passwords;}
    public Activity authorize(User actor,Long id,boolean lock) {
        Activity a=lock?activities.lock(id):activities.selectById(id);Problem.require(a!=null,404,"活动不存在或已删除");
        if("STUDENT".equals(actor.role))Problem.require(activities.isMember(id,actor.id)>0,403,"你尚未加入此活动");
        else Problem.require(Access.admin(actor)||a.creatorId.equals(actor.id)||activities.shared(id,actor.id)>0,403,"没有此活动的访问权限");
        return a;
    }
    public Object list(User actor,int page,int size,String scope) {
        Problem.require(Set.of("all","mine","shared").contains(scope),400,"活动筛选条件不正确");
        var q=new QueryWrapper<Activity>();
        if("STUDENT".equals(actor.role)) q.inSql("id","SELECT activity_id FROM activity_student WHERE active=1 AND student_id="+actor.id);
        else if(!Access.admin(actor))q.and(w->w.eq("creator_id",actor.id).or().inSql("id","SELECT activity_id FROM activity_teacher WHERE teacher_id="+actor.id));
        if(!"STUDENT".equals(actor.role)) {
            if(scope.equals("mine"))q.eq("creator_id",actor.id);
            if(scope.equals("shared"))q.ne("creator_id",actor.id).inSql("id","SELECT activity_id FROM activity_teacher WHERE teacher_id="+actor.id);
        }
        return activities.selectPage(new Page<>(Math.max(1,page),Math.clamp(size,1,100)),q.orderByDesc("id"));
    }
    public Object detail(User actor,Long id) {
        Access.staff(actor);Activity a=authorize(actor,id,false);
        for(Long studentId:activities.studentIds(id))activities.assignPetIfMissing(id,studentId,randomPet());
        var members=activities.members(id);
        members.forEach(member->{int points=((Number)member.get("points")).intValue();member.put("level",1+points/10);});
        var result=new LinkedHashMap<String,Object>();result.put("activity",a);result.put("teacherIds",activities.teacherIds(id));
        result.put("studentIds",activities.studentIds(id));result.put("members",members);result.put("pending",pending(id));
        result.put("remaining",activities.candidates(id,a.roundNo,a.repeatDraw).size());return result;
    }
    private Set<Long> teacherPool(Activity a,List<Long> shared) {
        var pool=new HashSet<Long>();pool.add(a.creatorId);pool.addAll(shared);return pool;
    }
    public Object candidates(User actor,Long id,List<Long> proposedTeachers) {
        Access.staff(actor);Set<Long> pool;
        if(id==null) {pool=new HashSet<>(proposedTeachers);pool.add(actor.id);}
        else {var a=authorize(actor,id,false);pool=teacherPool(a,proposedTeachers);}
        Problem.require(pool.size()<=101,400,"最多共享给 100 位老师");
        return users.selectMaps(new QueryWrapper<User>().select("id","name","student_no AS studentNo","owner_teacher_id AS ownerTeacherId")
            .eq("role","STUDENT").eq("enabled",true)
            .and(w->w.isNull("student_no").or().notLike("student_no",Accounts.ROSTER_STUDENT_PREFIX+"%"))
            .in("owner_teacher_id",pool).orderByAsc("id"));
    }
    private String randomPet(){return PETS.get(random.nextInt(PETS.size()));}
    private List<Long> createRosterStudents(User actor,List<String> names){
        if(names==null||names.isEmpty())return List.of();
        Problem.require("TEACHER".equals(actor.role),403,"只有老师可以直接录入活动名单");
        Problem.require(names.size()<=200,400,"名单最多支持 200 位同学");
        String inaccessiblePassword=passwords.encode(UUID.randomUUID().toString());
        var ids=new ArrayList<Long>(names.size());
        for(String raw:names){
            String name=Accounts.clean(raw);
            Problem.require(name!=null&&name.length()<=80,400,"学生姓名不能为空且最多 80 个字符");
            var student=new User();student.name=name;
            student.studentNo=Accounts.ROSTER_STUDENT_PREFIX+actor.id+":"+UUID.randomUUID().toString().replace("-","");
            student.passwordHash=inaccessiblePassword;student.role="STUDENT";student.ownerTeacherId=actor.id;
            student.enabled=true;student.authVersion=0;student.version=0;
            users.insert(student);ids.add(student.id);
        }
        return ids;
    }
    private void relations(User actor,Activity a,Input input,boolean creating) {
        var teachers=new HashSet<>(input.teacherIds());teachers.remove(a.creatorId);
        for(Long id:teachers) {User t=users.selectById(id);Problem.require(t!=null&&t.enabled&&"TEACHER".equals(t.role),400,"只能共享给有效的老师账号");}
        var pool=teacherPool(a,new ArrayList<>(teachers));var old=creating?Set.<Long>of():new HashSet<>(activities.studentIds(a.id));
        var students=new HashSet<>(input.studentIds());
        for(Long id:students) {
            if(old.contains(id))continue;
            User s=users.selectById(id);
            Problem.require(s!=null&&s.enabled&&"STUDENT".equals(s.role)&&pool.contains(s.ownerTeacherId),403,"新增学生必须来自参与老师名下");
        }
        Draw p=creating?null:pending(a.id);
        Problem.require(p==null||students.contains(p.studentId),409,"请先处理待确认的点名结果，再移除此学生");
        activities.clearTeachers(a.id);for(Long id:teachers)activities.addTeacher(a.id,id);
        activities.clearStudents(a.id);for(Long id:students){activities.addStudent(a.id,id);activities.assignPetIfMissing(a.id,id,randomPet());}
        activities.audit(actor.id,"ACTIVITY_MEMBERS_AND_SHARING",a.id);
    }
    @Transactional public Object create(User actor,Input input) {
        Access.staff(actor);var a=new Activity();a.name=input.name().strip();a.creatorId=actor.id;a.repeatDraw=input.repeatDraw();a.roundNo=1;a.version=0;a.deleted=0;
        activities.insert(a);
        var studentIds=new LinkedHashSet<>(input.studentIds());studentIds.addAll(createRosterStudents(actor,input.studentNames()));
        var resolved=new Input(input.name(),input.repeatDraw(),new ArrayList<>(studentIds),input.teacherIds(),input.studentNames(),input.version());
        relations(actor,a,resolved,true);activities.audit(actor.id,"ACTIVITY_CREATE",a.id);return a;
    }
    @Transactional public Object update(User actor,Long id,Input input) {
        Access.staff(actor);var a=authorize(actor,id,true);
        Problem.require(input.version()!=null&&input.version().equals(a.version),409,"活动已被修改，请刷新后重试");
        if(!a.repeatDraw.equals(input.repeatDraw())) {Problem.require(pending(id)==null,409,"请先处理待确认的点名结果");a.roundNo++;}
        Input resolved=input;
        if(input.studentNames()!=null) {
            var reusable=new HashMap<String,ArrayDeque<Long>>();
            for(var member:activities.members(id)) {
                String memberName=String.valueOf(member.get("name"));
                reusable.computeIfAbsent(memberName,k->new ArrayDeque<>()).add(((Number)member.get("id")).longValue());
            }
            var desired=new LinkedHashSet<Long>(input.studentIds());var newNames=new ArrayList<String>();
            for(String name:input.studentNames()) {
                var matches=reusable.get(name.strip());
                if(matches!=null&&!matches.isEmpty())desired.add(matches.removeFirst());
                else newNames.add(name);
            }
            desired.addAll(createRosterStudents(actor,newNames));
            resolved=new Input(input.name(),input.repeatDraw(),new ArrayList<>(desired),input.teacherIds(),input.studentNames(),input.version());
        }
        relations(actor,a,resolved,false);a.name=input.name().strip();a.repeatDraw=input.repeatDraw();
        Problem.require(activities.updateById(a)==1,409,"活动已被修改，请刷新");activities.audit(actor.id,"ACTIVITY_UPDATE",id);return a;
    }
    @Transactional public void delete(User actor,Long id) {
        Access.staff(actor);authorize(actor,id,true);activities.deleteById(id);activities.audit(actor.id,"ACTIVITY_DELETE",id);
    }
    private Draw pending(Long id) {return draws.selectOne(new QueryWrapper<Draw>().eq("activity_id",id).eq("status","PENDING").last("LIMIT 1"));}
    @Transactional public Object draw(User actor,Long id,String key) {
        Access.staff(actor);var a=authorize(actor,id,true);
        Problem.require(key!=null&&key.matches("[A-Za-z0-9_-]{8,80}"),400,"无效的点名请求标识");
        Draw previous=draws.selectOne(new QueryWrapper<Draw>().eq("activity_id",id).eq("request_key",key));if(previous!=null)return previous;
        Draw pending=pending(id);Problem.require(pending==null,409,"已有待确认结果，请刷新后先处理");
        var candidates=activities.candidates(id,a.roundNo,a.repeatDraw);Problem.require(!candidates.isEmpty(),409,"本轮已抽完或没有有效学生，请重置轮次或添加学生");
        var d=new Draw();d.activityId=id;d.teacherId=actor.id;d.studentId=candidates.get(random.nextInt(candidates.size()));d.roundNo=a.roundNo;d.requestKey=key;d.status="PENDING";
        draws.insert(d);activities.audit(actor.id,"DRAW",d.id);return d;
    }
    @Transactional public Object resolve(User actor,Long id,Long drawId,boolean award) {
        Access.staff(actor);authorize(actor,id,true);Draw d=draws.selectById(drawId);
        Problem.require(d!=null&&d.activityId.equals(id),404,"点名记录不存在");
        String status=award?"AWARDED":"SKIPPED";
        if(!d.status.equals("PENDING")) {Problem.require(d.status.equals(status),409,"该结果已被其他老师处理，请刷新");return d;}
        if(award) {
            activities.ledger(d.id,id,d.studentId,actor.id);
            Problem.require(activities.addPoint(id,d.studentId)==1,409,"学生已被移出活动");
        }
        d.status=status;d.resolvedBy=actor.id;d.resolvedAt=LocalDateTime.now();draws.updateById(d);activities.audit(actor.id,award?"AWARD":"SKIP",d.id);return d;
    }
    @Transactional public Object reset(User actor,Long id) {
        Access.staff(actor);var a=authorize(actor,id,true);Problem.require(pending(id)==null,409,"请先处理待确认的点名结果");a.roundNo++;activities.updateById(a);activities.audit(actor.id,"ROUND_RESET",id);return a;
    }
    public Object history(User actor,Long id) {Access.staff(actor);authorize(actor,id,false);return draws.history(id);}
    public Object growth(User actor,Long id) {
        Problem.require("STUDENT".equals(actor.role),403,"仅学生可访问本人宠物");Activity a=authorize(actor,id,false);
        var g=new LinkedHashMap<>(activities.growth(id,actor.id));int points=((Number)g.get("points")).intValue();
        g.put("level",1+points/10);g.put("progress",points%10);g.put("activity",a);g.put("records",activities.scores(id,actor.id));return g;
    }
    @Transactional public Object pet(User actor,Long id,String pet) {
        Problem.require("STUDENT".equals(actor.role),403,"仅学生可选择自己的宠物");authorize(actor,id,true);
        Problem.require(Set.of("cat","rabbit","dragon").contains(pet),400,"宠物类型不正确");
        var current=activities.growth(id,actor.id);Object existing=current.get("pet");
        Problem.require(existing==null||pet.equals(existing),409,"宠物已选定，暂不支持更换");
        if(existing==null) {activities.choosePet(id,actor.id,pet);activities.audit(actor.id,"PET_CHOOSE",id);}
        return growth(actor,id);
    }
}
