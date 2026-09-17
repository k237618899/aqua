package icu.samnyan.aqua.api.config;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final boolean AQUAVIEWER_ENABLED;

    public WebConfig(@Value("${aquaviewer.server.enable}") boolean AQUAVIEWER_ENABLED) {
        this.AQUAVIEWER_ENABLED = AQUAVIEWER_ENABLED;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        if (AQUAVIEWER_ENABLED) {
            // Static assets (images), this priority must be higher than routes
            registry.addResourceHandler("/web/assets/**")
                    .addResourceLocations("classpath:/web/assets/", "file:web/assets/")
                    .setCachePeriod(10)
                    .resourceChain(true)
                    .addResolver(new PathResourceResolver());

            // For angularjs html5 routes
            registry.addResourceHandler("/web/**", "/web/", "/web")
                    .addResourceLocations("classpath:/web/", "file:web/")
                    .setCachePeriod(10)
                    .resourceChain(true)
                    .addResolver(new PathResourceResolver() {
                        @Override
                        protected Resource getResource(String resourcePath, Resource location) throws IOException {
                            Resource requestedResource = location.createRelative(resourcePath);
                            if (requestedResource.exists() && requestedResource.isReadable()) {
                                return requestedResource;
                            }

                            // If this looks like a static file request, do not fallback to index here.
                            // Returning null allows the next resource location (classpath) to be checked.
                            if (resourcePath.contains(".")) {
                                return null;
                            }

                            Resource fileIndex = new FileSystemResource("web/index.html");
                            if (fileIndex.exists() && fileIndex.isReadable()) {
                                return fileIndex;
                            }

                            Resource classpathIndex = new ClassPathResource("web/index.html");
                            return classpathIndex.exists() && classpathIndex.isReadable() ? classpathIndex : null;
                        }
                    });
        }
    }

}