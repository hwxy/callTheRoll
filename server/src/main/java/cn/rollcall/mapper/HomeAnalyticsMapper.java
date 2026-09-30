package cn.rollcall.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface HomeAnalyticsMapper {
    @Insert("INSERT INTO site_home_visit_daily(visit_date,pv,uv) VALUES(#{date},1,0) "
            + "ON DUPLICATE KEY UPDATE pv=pv+1")
    int incrementPv(@Param("date") LocalDate date);

    @Insert("INSERT IGNORE INTO site_home_visit_visitor(visit_date,visitor_hash) VALUES(#{date},#{visitorHash})")
    int recordVisitor(@Param("date") LocalDate date, @Param("visitorHash") String visitorHash);

    @Update("UPDATE site_home_visit_daily SET uv=uv+1 WHERE visit_date=#{date}")
    int incrementUv(@Param("date") LocalDate date);

    @Delete("DELETE FROM site_home_visit_visitor WHERE visit_date < #{date}")
    int removePreviousVisitorHashes(@Param("date") LocalDate date);

    @Select("SELECT COUNT(DISTINCT visitor_hash) FROM site_home_visit_visitor WHERE visit_date BETWEEN #{from} AND #{to}")
    long countUniqueVisitors(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Select("SELECT DATE_FORMAT(visit_date,'%Y-%m-%d') AS visitDate,pv,uv "
            + "FROM site_home_visit_daily WHERE visit_date BETWEEN #{from} AND #{to} ORDER BY visit_date DESC")
    List<Map<String, Object>> daily(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
