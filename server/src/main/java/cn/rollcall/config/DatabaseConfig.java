package cn.rollcall.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.*;
import org.springframework.context.annotation.*;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.*;

@Configuration
public class DatabaseConfig {
    @Bean
    MybatisPlusInterceptor interceptor() {
        var i = new MybatisPlusInterceptor();
        i.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        var page = new PaginationInnerInterceptor(DbType.MYSQL);
        page.setMaxLimit(100L);
        i.addInnerInterceptor(page);
        return i;
    }

    @Bean
    OpenAPI openAPI() {
        return new OpenAPI().info(new Info().title("点点名 · API").version("1.0"))
                .components(new Components().addSecuritySchemes("session", new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer")))
                .addSecurityItem(new SecurityRequirement().addList("session"));
    }
}
