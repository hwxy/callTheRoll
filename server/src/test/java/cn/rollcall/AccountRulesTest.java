package cn.rollcall;

import cn.rollcall.api.Problem;
import cn.rollcall.model.Models.User;
import cn.rollcall.service.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AccountRulesTest {
    @Test void passwordMustBeSixCharactersAndWithinBcryptByteLimit(){
        assertThrows(Problem.class,()->Accounts.password("12345"));
        assertThrows(Problem.class,()->Accounts.password("中".repeat(25)));
        assertDoesNotThrow(()->Accounts.password("Study123"));
    }
    @Test void teacherCannotEditOtherTeachersStudents(){
        var teacher=new User();teacher.id=1L;teacher.role="TEACHER";
        var student=new User();student.role="STUDENT";student.ownerTeacherId=2L;
        assertThrows(Problem.class,()->Access.account(teacher,student));
        student.ownerTeacherId=1L;assertDoesNotThrow(()->Access.account(teacher,student));
        student.role="ADMIN";assertThrows(Problem.class,()->Access.account(teacher,student));
    }
    @Test void studentCannotActAsStaff(){var u=new User();u.role="STUDENT";assertThrows(Problem.class,()->Access.staff(u));}
}
