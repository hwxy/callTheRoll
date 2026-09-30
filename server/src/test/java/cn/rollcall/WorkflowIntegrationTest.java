package cn.rollcall;

import cn.rollcall.api.Problem;
import cn.rollcall.mapper.*;
import cn.rollcall.model.Models.*;
import cn.rollcall.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"app.bootstrap.login=","app.bootstrap.password="})
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named="RUN_INTEGRATION_TESTS",matches="true")
class WorkflowIntegrationTest {
    @Autowired Accounts accounts;@Autowired Activities activities;@Autowired UserMapper users;
    @Autowired ActivityMapper mapper;@Autowired DrawMapper draws;@Autowired ExcelImports imports;
    @Autowired SiteSettings siteSettings;
    @Autowired PasswordEncoder passwords;@Autowired Sessions sessions;@Autowired MockMvc mvc;
    @Autowired ObjectMapper json;@Autowired JdbcTemplate jdbc;@Autowired StringRedisTemplate redis;
    User admin,teacher,other,student;String prefix;
    @BeforeEach void fixture(){
        String database=jdbc.queryForObject("SELECT DATABASE()",String.class);
        assertTrue(database.endsWith("_test"),"Integration tests only run against a dedicated *_test database");
        prefix="t"+UUID.randomUUID().toString().replace("-","").substring(0,12);
        admin=new User();admin.name="测试管理员";admin.studentNo=prefix+"admin";admin.role="ADMIN";admin.passwordHash=passwords.encode("Study123");admin.enabled=true;admin.version=0;admin.authVersion=0;users.insert(admin);users.addAlias(admin.studentNo,admin.id);
        teacher=account(admin,"老师","TEACHER",null,"teacher");other=account(admin,"共享老师","TEACHER",null,"other");student=account(teacher,"小满","STUDENT",teacher.id,"student");
    }
    User account(User actor,String name,String role,Long owner,String suffix){return accounts.create(actor,new Accounts.Input(name,prefix+suffix,null,"Study123",role,owner,true,null));}
    Activity activity(boolean repeat,Long...students){return (Activity)activities.create(teacher,new Activities.Input("测试课堂",repeat,List.of(students),List.of(other.id),null));}
    Activities.Input input(Activity a,List<Long> students,List<Long> teachers,boolean repeat){return new Activities.Input(a.name,repeat,students,teachers,a.version);}
    Draw draw(User actor,Activity a,String suffix){return (Draw)activities.draw(actor,a.id,prefix+suffix);}
    int points(Activity a,User s){return ((Number)mapper.growth(a.id,s.id).get("points")).intValue();}

    @Test void unifiedLoginReturnsRealRoleAndEnforcesEntry() throws Exception {
        String body=json.writeValueAsString(Map.of("login",student.studentNo,"password","Study123","role","TEACHER"));
        mvc.perform(post("/api/v1/auth/login").header("X-Client","app").contentType("application/json").content(body)).andExpect(status().isOk()).andExpect(jsonPath("$.user.role").value("STUDENT")).andExpect(jsonPath("$.user.passwordHash").doesNotExist());
        mvc.perform(post("/api/v1/auth/login").header("X-Client","console").contentType("application/json").content(body)).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/auth/login").header("X-Client","app").contentType("application/json").content(json.writeValueAsString(Map.of("login",admin.studentNo,"password","Study123")))).andExpect(status().isForbidden());
        String token=sessions.create(student,"app");
        mvc.perform(get("/api/v1/accounts").header("Authorization","Bearer "+token)).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/auth/logout").header("Authorization","Bearer "+token)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/auth/me").header("Authorization","Bearer "+token)).andExpect(status().isUnauthorized());
    }
    @Test void disableAndPasswordResetInvalidateSessions(){
        String token=sessions.create(student,"app");accounts.reset(teacher,student.id,"Newpass123");assertThrows(Problem.class,()->sessions.resolve(token,"/api/v1/auth/me"));
        var s=users.selectById(student.id);String nextToken=sessions.create(s,"app");
        accounts.update(teacher,s.id,new Accounts.Input(s.name,s.studentNo,s.phone,null,s.role,s.ownerTeacherId,false,s.version));
        assertThrows(Problem.class,()->sessions.resolve(nextToken,"/api/v1/auth/me"));
    }
    @Test void aliasesAreOneNamespaceAndTeacherCannotEditOtherAccounts(){
        String phone="139"+String.format("%08d",Math.floorMod(System.nanoTime(),100000000));
        accounts.create(teacher,new Accounts.Input("手机号占位",phone,null,"Study123","STUDENT",teacher.id,true,null));
        assertThrows(Problem.class,()->accounts.create(teacher,new Accounts.Input("冲突",null,phone,"Study123","STUDENT",teacher.id,true,null)));
        assertThrows(Problem.class,()->accounts.reset(other,student.id,"Newpass123"));
    }
    @Test void shareRevocationRemovesAccessAndRetainsMembers(){
        User s2=account(other,"另一同学","STUDENT",other.id,"s2");Activity a=activity(false,student.id,s2.id);
        assertNotNull(activities.detail(other,a.id));
        activities.update(teacher,a.id,input(a,List.of(student.id,s2.id),List.of(),false));
        assertThrows(Problem.class,()->activities.detail(other,a.id));assertEquals(1,mapper.isMember(a.id,s2.id));
        assertThrows(Problem.class,()->activities.growth(student,activity(false).id));
    }
    @Test void sharedTeacherHasFullManagementAndDeletionRights(){
        Activity a=activity(false,student.id);
        activities.update(other,a.id,new Activities.Input("共享老师修改",false,List.of(student.id),List.of(other.id),a.version));
        assertEquals("共享老师修改",mapper.selectById(a.id).name);
        activities.delete(other,a.id);assertNull(mapper.selectById(a.id));assertThrows(Problem.class,()->activities.growth(student,a.id));
    }
    @Test void noRepeatPendingResetAndIdempotentScore(){
        Activity a=activity(false,student.id);Draw d=draw(teacher,a,"draw0001");
        assertEquals(d.id,draw(teacher,a,"draw0001").id);
        assertThrows(Problem.class,()->draw(other,a,"draw0002"));assertThrows(Problem.class,()->activities.reset(other,a.id));
        assertThrows(Problem.class,()->activities.update(teacher,a.id,input(a,List.of(),List.of(other.id),false)));
        activities.resolve(other,a.id,d.id,true);activities.resolve(teacher,a.id,d.id,true);assertEquals(1,points(a,student));
        assertThrows(Problem.class,()->activities.resolve(teacher,a.id,d.id,false));assertThrows(Problem.class,()->draw(teacher,a,"draw0003"));
        activities.reset(other,a.id);assertEquals(1,points(a,student));assertNotNull(draw(other,a,"draw0004"));
    }
    @Test void repeatModeLevelBoundaryAndActivityIsolation(){
        Activity a=activity(true,student.id),b=activity(true,student.id);
        for(int n=0;n<10;n++){var d=draw(teacher,a,"score"+n);activities.resolve(teacher,a.id,d.id,true);}
        assertEquals(10,points(a,student));assertEquals(0,points(b,student));
        var g=(Map<?,?>)activities.growth(student,a.id);assertEquals(2,g.get("level"));
        activities.pet(student,a.id,"rabbit");assertThrows(Problem.class,()->activities.pet(student,a.id,"cat"));
        assertEquals(10,points(a,student));
    }
    @Test void teacherCanCreateRosterNamesWithActivityScopedPetGrowth(){
        Activity a=(Activity)activities.create(teacher,new Activities.Input("名单课堂",false,List.of(),List.of(),List.of("林小满","陈星野"),null));
        var detail=(Map<?,?>)activities.detail(teacher,a.id);var members=(List<Map<String,Object>>)detail.get("members");
        assertEquals(2,members.size());assertEquals(Set.of("林小满","陈星野"),Set.of(members.get(0).get("name"),members.get(1).get("name")));
        for(var member:members){
            assertTrue(Set.of("cat","rabbit","dragon").contains(member.get("pet")));assertEquals(1,member.get("level"));
            User hidden=users.selectById(((Number)member.get("id")).longValue());assertTrue(hidden.studentNo.startsWith(Accounts.ROSTER_STUDENT_PREFIX));assertNull(users.byAlias(hidden.studentNo));
        }
        var draw=draw(teacher,a,"roster0001");activities.resolve(teacher,a.id,draw.id,true);
        var refreshed=(Map<?,?>)activities.detail(teacher,a.id);var updated=(List<Map<String,Object>>)refreshed.get("members");
        assertTrue(updated.stream().anyMatch(member->Integer.valueOf(1).equals(member.get("points"))));
        assertTrue(updated.stream().allMatch(member->Integer.valueOf(1).equals(member.get("level"))));
    }
    @Test void removedAndReaddedStudentKeepsPointsAndRoundExclusion(){
        Activity a=activity(false,student.id);var d=draw(teacher,a,"round0001");activities.resolve(teacher,a.id,d.id,true);
        activities.update(teacher,a.id,input(mapper.selectById(a.id),List.of(),List.of(other.id),false));
        assertThrows(Problem.class,()->activities.growth(student,a.id));
        activities.update(teacher,a.id,input(mapper.selectById(a.id),List.of(student.id),List.of(other.id),false));
        assertEquals(1,points(a,student));assertThrows(Problem.class,()->draw(teacher,a,"round0002"));
    }
    @Test void concurrentDrawAndAwardSerialize() throws Exception {
        Activity a=activity(false,student.id);var gate=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)){
            var first=pool.submit(()->{gate.await();try{return draw(teacher,a,"concurrent1");}catch(Problem p){assertEquals(409,p.status);return null;}});
            var second=pool.submit(()->{gate.await();try{return draw(other,a,"concurrent2");}catch(Problem p){assertEquals(409,p.status);return null;}});gate.countDown();
            Draw d1=first.get(10,TimeUnit.SECONDS),d2=second.get(10,TimeUnit.SECONDS);assertNotEquals(d1==null,d2==null);Draw d=d1==null?d2:d1;
            var r1=pool.submit(()->activities.resolve(teacher,a.id,d.id,true));var r2=pool.submit(()->activities.resolve(other,a.id,d.id,true));r1.get(10,TimeUnit.SECONDS);r2.get(10,TimeUnit.SECONDS);
            assertEquals(1,points(a,student));assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM score_ledger WHERE draw_id=?",Integer.class,d.id));
        }
    }
    @Test void failedAwardRollsBackLedger(){
        Activity a=activity(false,student.id);var d=draw(teacher,a,"rollback1");
        jdbc.update("UPDATE activity_student SET active=0 WHERE activity_id=?",a.id);
        assertThrows(Problem.class,()->activities.resolve(teacher,a.id,d.id,true));
        assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM score_ledger WHERE draw_id=?",Integer.class,d.id));assertEquals("PENDING",draws.selectById(d.id).status);
    }
    @Test void excelPreviewHasRowErrorsAndAtomicConfirm() throws Exception {
        var bad=excel(new String[][]{{"有效",prefix+"valid","","Study123"},{"错误",prefix+"invalid","","123"}});
        Map<?,?> preview=(Map<?,?>)imports.preview(teacher,bad);assertFalse(((List<?>)preview.get("errors")).isEmpty());assertEquals("",preview.get("token"));assertNull(users.byAlias(prefix+"valid"));
        var good=excel(new String[][]{{"甲",prefix+"a","","Study123"},{"乙",prefix+"b","","Study123"}});
        preview=(Map<?,?>)imports.preview(teacher,good);String token=(String)preview.get("token");
        assertFalse(json.writeValueAsString(preview).contains("Study123"));
        account(teacher,"并发占用","STUDENT",teacher.id,"b");
        assertThrows(Problem.class,()->imports.confirm(teacher,token));assertNull(users.byAlias(prefix+"a"));
        var valid=(Map<?,?>)imports.preview(teacher,excel(new String[][]{{"小禾",prefix+"success","","Study123"}}));
        imports.confirm(teacher,(String)valid.get("token"));assertNotNull(users.byAlias(prefix+"success"));
    }
    @Test void oldVersionCannotOverwriteActivity(){
        Activity a=activity(false,student.id);activities.reset(other,a.id);
        assertThrows(Problem.class,()->activities.update(teacher,a.id,input(a,List.of(student.id),List.of(),false)));
    }
    @Test void aboutIsPublicButOnlyAdministratorCanUpdate() throws Exception {
        mvc.perform(get("/api/v1/site/about")).andExpect(status().isOk()).andExpect(jsonPath("$.content").exists());
        String teacherToken=sessions.create(teacher,"console");
        mvc.perform(put("/api/v1/site/about").header("Authorization","Bearer "+teacherToken)
            .contentType("application/json").content(json.writeValueAsString(Map.of("content","老师不能发布"))))
            .andExpect(status().isForbidden());
        String adminToken=sessions.create(admin,"console");
        mvc.perform(put("/api/v1/site/about").header("Authorization","Bearer "+adminToken)
            .contentType("application/json").content(json.writeValueAsString(Map.of("content","欢迎来到点点名。\n一起看见成长。"))))
            .andExpect(status().isOk()).andExpect(jsonPath("$.content").value("欢迎来到点点名。\n一起看见成长。"));
        assertEquals("欢迎来到点点名。\n一起看见成长。",siteSettings.about().get("content"));
    }
    private MockMultipartFile excel(String[][] rows) throws Exception {
        try(var book=new XSSFWorkbook();var out=new ByteArrayOutputStream()){
            var sheet=book.createSheet();var header=sheet.createRow(0);String[] titles={"姓名","学号","手机号","初始密码"};for(int i=0;i<4;i++)header.createCell(i).setCellValue(titles[i]);
            for(int n=0;n<rows.length;n++){var row=sheet.createRow(n+1);for(int i=0;i<4;i++)row.createCell(i).setCellValue(rows[n][i]);}
            book.write(out);return new MockMultipartFile("file","students.xlsx","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",out.toByteArray());
        }
    }
}
