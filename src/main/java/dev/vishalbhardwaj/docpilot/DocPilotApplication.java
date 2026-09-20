package dev.vishalbhardwaj.docpilot;

import dev.vishalbhardwaj.docpilot.config.DocPilotProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan(basePackageClasses = DocPilotProperties.class)
public class DocPilotApplication {

    public static void main(String[] args) {
        SpringApplication.run(DocPilotApplication.class, args);
    }
}
