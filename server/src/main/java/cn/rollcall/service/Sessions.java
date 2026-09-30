package cn.rollcall.service;

import cn.rollcall.api.Problem;
import cn.rollcall.mapper.UserMapper;
import cn.rollcall.model.Models.User;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Duration;
import java.util.*;

@Service
public class Sessions {
    private final StringRedisTemplate redis;
    private final UserMapper users;
    private final SecureRandom random=new SecureRandom();
    private final long hours;
    public Sessions(StringRedisTemplate redis,UserMapper users,@Value("${app.session-hours}") long hours) {
        this.redis=redis; this.users=users; this.hours=hours;
    }
    private String digest(String input) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    public void throttle(String key,int limit) {
        var script=new DefaultRedisScript<Long>("local n=redis.call('INCR',KEYS[1]); if n==1 then redis.call('EXPIRE',KEYS[1],600) end; return n",Long.class);
        Long n=redis.execute(script,List.of("rollcall:limit:"+digest(key)));
        Problem.require(n!=null && n<=limit,429,"尝试次数过多，请 10 分钟后再试");
    }
    public String create(User u,String client) {
        byte[] bytes=new byte[32];random.nextBytes(bytes);
        String token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        redis.opsForValue().set("rollcall:session:"+digest(token),u.id+":"+u.authVersion+":"+client,Duration.ofHours(hours));
        return token;
    }
    public String createConsoleTicket(User user) {
        Problem.require("TEACHER".equals(user.role),403,"仅老师账号可以进入管理后台");
        byte[] bytes=new byte[32];random.nextBytes(bytes);
        String ticket=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        redis.opsForValue().set("rollcall:console-ticket:"+digest(ticket),user.id+":"+user.authVersion,Duration.ofSeconds(60));
        return ticket;
    }
    public Map<String,Object> exchangeConsoleTicket(String ticket) {
        Problem.require(ticket!=null && ticket.length()<=128,401,"登录凭证无效或已过期");
        var consume=new DefaultRedisScript<String>("local v=redis.call('GET',KEYS[1]); if v then redis.call('DEL',KEYS[1]) end; return v",String.class);
        String value=redis.execute(consume,List.of("rollcall:console-ticket:"+digest(ticket)));
        Problem.require(value!=null,401,"登录凭证无效或已过期");
        String[] parts=value.split(":");
        User user=users.selectById(Long.valueOf(parts[0]));
        Problem.require(user!=null && Boolean.TRUE.equals(user.enabled) && user.authVersion.toString().equals(parts[1]),401,"账号状态已改变，请重新登录");
        Problem.require("TEACHER".equals(user.role),403,"仅老师账号可以进入管理后台");
        return Map.of("token",create(user,"console"),"user",user);
    }
    public void checkFailures(String alias) {
        String count=redis.opsForValue().get("rollcall:limit:"+digest("login:"+alias));
        Problem.require(count==null||Long.parseLong(count)<10,429,"密码尝试次数过多，请 10 分钟后再试");
    }
    public void failedLogin(String alias) { throttle("login:"+alias,10); }
    public void successfulLogin(String alias) { redis.delete("rollcall:limit:"+digest("login:"+alias)); }
    public User resolve(String token,String path) {
        if(token==null || token.length()>128) throw new Problem(401,"登录已失效");
        String value=redis.opsForValue().get("rollcall:session:"+digest(token));
        Problem.require(value!=null,401,"登录已失效，请重新登录");
        String[] parts=value.split(":");
        User u=users.selectById(Long.valueOf(parts[0]));
        Problem.require(u!=null && Boolean.TRUE.equals(u.enabled) && u.authVersion.toString().equals(parts[1]),401,"账号状态已改变，请重新登录");
        Problem.require(("console".equals(parts[2])&&!"STUDENT".equals(u.role))||("app".equals(parts[2])&&!"ADMIN".equals(u.role)),403,"当前账号不能使用此入口");
        return u;
    }
    public void revoke(String token) { if(token!=null) redis.delete("rollcall:session:"+digest(token)); }
}
