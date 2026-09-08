package cn.rollcall.service;

import cn.rollcall.model.Models.User;
import cn.rollcall.api.Problem;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.Objects;

public final class Access {
    private Access() {}
    public static User user() {
        var auth=SecurityContextHolder.getContext().getAuthentication();
        Problem.require(auth!=null && auth.getPrincipal() instanceof User,401,"请先登录");
        return (User)auth.getPrincipal();
    }
    public static boolean admin(User u) { return "ADMIN".equals(u.role); }
    public static void staff(User u) { Problem.require(admin(u)||"TEACHER".equals(u.role),403,"仅老师和管理员可操作"); }
    public static void adminOnly(User u) { Problem.require(admin(u),403,"仅管理员可操作"); }
    public static void account(User actor,User target) {
        staff(actor);
        Problem.require(target!=null,404,"账号不存在");
        Problem.require(admin(actor)||("STUDENT".equals(target.role)&&Objects.equals(target.ownerTeacherId,actor.id)),403,"只能管理自己创建的学生");
    }
}
