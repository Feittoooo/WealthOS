package com.wealthos.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;

/**
 * Serves the Angular build from classpath:/static. Any path that isn't a file and isn't part of
 * the API gets index.html, so deep links like /investments survive a page reload.
 */
@Configuration
public class SpaConfig implements WebMvcConfigurer {

    private static final String[] NOT_SPA = {"api/", "actuator", "v3/", "swagger-ui", "h2-console"};

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                // file names carry a hash, but index.html doesn't: always revalidate
                .setCacheControl(CacheControl.noCache())
                .resourceChain(true)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String path, Resource location) throws IOException {
                        Resource requested = location.createRelative(path);
                        if (requested.exists() && requested.isReadable()) return requested;
                        for (String prefix : NOT_SPA) {
                            if (path.startsWith(prefix)) return null;
                        }
                        Resource index = new ClassPathResource("/static/index.html");
                        return index.exists() ? index : null;
                    }
                });
    }
}
