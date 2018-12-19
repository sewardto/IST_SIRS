package rda.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("storage")
public class StorageProperties {

    /**
     * Folder location for storing files
     */
    private final String root = "upload-dir\\";

    private String location = root;

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        System.out.println(location);
        this.location = location;
    }

    public String getRoot() {
        return root;
    }
}
