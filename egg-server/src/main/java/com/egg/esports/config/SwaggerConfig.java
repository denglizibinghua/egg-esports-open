package com.egg.esports.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc OpenAPI 配置
 * <p>
 * 启动后访问 <a href="http://localhost:8088/swagger-ui.html">Swagger UI</a> 查看自动生成的 API 文档。
 * JSON 格式文档在 <a href="http://localhost:8088/v3/api-docs">/v3/api-docs</a>。
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI eggEsportsOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Egg Esports API")
                        .description("Egg 赛事竞猜平台 — 非盈利电竞赛事预测平台 API 文档")
                        .version("v1.0.0")
                        .contact(new Contact()
                                .name("Egg Esports")
                                .url("https://eggesports.com")));
    }
}
