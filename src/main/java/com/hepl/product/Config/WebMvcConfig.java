package com.hepl.product.Config;

import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:uploads/products}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absolutePath = Paths.get(uploadDir).toAbsolutePath().toString().replace("\\", "/");
        registry.addResourceHandler("/uploads/products/**")
                .addResourceLocations("file:///" + absolutePath + "/");
    }
}
