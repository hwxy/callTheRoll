package cn.rollcall.service;

import cn.rollcall.api.Problem;
import cn.rollcall.mapper.UserMapper;
import cn.rollcall.model.Models.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.time.Duration;
import java.util.*;

@Service
public class ExcelImports {
    static {
        org.apache.poi.openxml4j.util.ZipSecureFile.setMaxEntrySize(8L*1024*1024);
        org.apache.poi.openxml4j.util.ZipSecureFile.setMaxTextSize(8L*1024*1024);
    }
    private final Accounts accounts;private final UserMapper users;private final StringRedisTemplate redis;private final ObjectMapper json;
    public ExcelImports(Accounts accounts,UserMapper users,StringRedisTemplate redis,ObjectMapper json) {this.accounts=accounts;this.users=users;this.redis=redis;this.json=json;}
    public byte[] template(User actor) throws IOException {
        Access.staff(actor);
        try(var book=new XSSFWorkbook();var out=new ByteArrayOutputStream()) {
            var sheet=book.createSheet("学生导入");var header=sheet.createRow(0);
            var titles=Access.admin(actor)?List.of("姓名","学号","手机号","初始密码","归属老师账号"):List.of("姓名","学号","手机号","初始密码");
            var style=book.createCellStyle();style.setDataFormat(book.createDataFormat().getFormat("@"));
            for(int i=0;i<titles.size();i++){header.createCell(i).setCellValue(titles.get(i));sheet.setColumnWidth(i,6000);sheet.setDefaultColumnStyle(i,style);}
            book.write(out);return out.toByteArray();
        }
    }
    public Object preview(User actor,MultipartFile file) throws IOException {
        Access.staff(actor);
        Problem.require(file.getSize()>0&&file.getSize()<=2*1024*1024&&file.getOriginalFilename()!=null&&file.getOriginalFilename().endsWith(".xlsx"),400,"请选择不超过 2MB 的 .xlsx 文件");
        var errors=new ArrayList<Map<String,Object>>();var rows=new ArrayList<Accounts.ImportRow>();var shown=new ArrayList<Map<String,Object>>();var aliases=new HashSet<String>();
        try(var book=new XSSFWorkbook(file.getInputStream())) {
            var sheet=book.getSheetAt(0);Problem.require(sheet.getLastRowNum()<=500,400,"每次最多导入 500 行学生");
            var expected=Access.admin(actor)?List.of("姓名","学号","手机号","初始密码","归属老师账号"):List.of("姓名","学号","手机号","初始密码");
            var format=new DataFormatter();Row header=sheet.getRow(0);Problem.require(header!=null,400,"缺少表头，请使用提供的模板");
            for(int i=0;i<expected.size();i++)Problem.require(expected.get(i).equals(format.formatCellValue(header.getCell(i)).strip()),400,"表头不匹配，请使用提供的模板");
            for(int n=1;n<=sheet.getLastRowNum();n++) {
                Row row=sheet.getRow(n);if(row==null)continue;
                String[] cells=new String[expected.size()];boolean empty=true;
                for(int i=0;i<cells.length;i++){Cell c=row.getCell(i);cells[i]=format.formatCellValue(c).strip();if(!cells[i].isEmpty())empty=false;}
                if(empty)continue;
                try {
                    for(Cell c:row)Problem.require(c.getCellType()!=CellType.FORMULA,400,"不支持公式单元格");
                    for(int i=1;i<=3;i++) {Cell c=row.getCell(i);Problem.require(c==null||c.getCellType()==CellType.STRING||c.getCellType()==CellType.BLANK,400,"学号、手机号和密码列必须为文本格式，以保留前导零");}
                    Long owner=actor.id;
                    if(Access.admin(actor)) {User teacher=users.byAlias(cells[4]);Problem.require(teacher!=null&&teacher.role.equals("TEACHER")&&teacher.enabled,400,"归属老师账号不存在或不可用");owner=teacher.id;}
                    var input=new Accounts.Input(cells[0],cells[1],cells[2],cells[3],"STUDENT",owner,true,null);
                    User prepared=accounts.prepare(actor,input,null);
                    var own=new HashSet<String>();if(prepared.studentNo!=null)own.add(prepared.studentNo);if(prepared.phone!=null)own.add(prepared.phone);
                    for(String a:own)Problem.require(!aliases.contains(a),400,"文件内存在重复学号或手机号");
                    String hash=accounts.hashPassword(cells[3]);aliases.addAll(own);
                    rows.add(new Accounts.ImportRow(prepared.name,prepared.studentNo,prepared.phone,owner,hash));
                    var display=new LinkedHashMap<String,Object>();display.put("row",n+1);display.put("name",prepared.name);display.put("studentNo",prepared.studentNo);display.put("phone",prepared.phone);shown.add(display);
                } catch(Problem e) {errors.add(Map.of("row",n+1,"message",e.getMessage()));}
            }
        } catch(Problem e) {throw e;} catch(Exception e) {throw new Problem(400,"无法读取工作簿，请使用有效的 .xlsx 模板");}
        Problem.require(!rows.isEmpty()||!errors.isEmpty(),400,"文件没有学生数据");
        String token="";
        if(errors.isEmpty()) {token=UUID.randomUUID().toString();redis.opsForValue().set(key(actor,token),json.writeValueAsString(rows),Duration.ofMinutes(10));}
        return Map.of("rows",shown,"errors",errors,"token",token,"count",rows.size());
    }
    public Object confirm(User actor,String token) throws IOException {
        Access.staff(actor);Problem.require(token!=null&&token.matches("[0-9a-f-]{36}"),400,"导入凭证不正确");
        String value=redis.opsForValue().get(key(actor,token));Problem.require(value!=null,409,"导入预览已过期或已提交，请重新上传");
        List<Accounts.ImportRow> rows=json.readValue(value,new TypeReference<>(){});
        int count=accounts.importRows(actor,rows);redis.delete(key(actor,token));return Map.of("count",count);
    }
    private String key(User u,String token){return "rollcall:import:"+u.id+":"+token;}
}
