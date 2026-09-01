package ng.edu.unilag.tradeup.config;

import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Serves uploaded listing photos straight off disk. */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final Path uploadDirectory;
    private final String publicPrefix;

    public WebConfig(
            @Value("${tradeup.storage.upload-dir}") Path uploadDirectory,
            @Value("${tradeup.storage.public-prefix:/uploads}") String publicPrefix) {
        this.uploadDirectory = uploadDirectory;
        this.publicPrefix = publicPrefix;
    }

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        registry.addResourceHandler(publicPrefix + "/**")
                .addResourceLocations(uploadDirectory.toUri().toString())
                .setCachePeriod(3600);
    }
}
