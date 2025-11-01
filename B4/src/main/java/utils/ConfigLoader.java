package utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

/**
 * Simple singleton utility that loads application.properties from the classpath.
 * Allows other layers to centralise access to configuration values.
 */
public class ConfigLoader {
    private static ConfigLoader instance;
    private final Properties properties;

    private ConfigLoader() throws IOException {
        properties = new Properties();
        loadPropertiesFromClasspath();
    }

    public static ConfigLoader getInstance() {
        if (instance == null) {
            synchronized (ConfigLoader.class) {
                if (instance == null) {
                    try {
                        instance = new ConfigLoader();
                    } catch (IOException e) {
                        throw new IllegalStateException("No se pudo cargar la configuración", e);
                    }
                }
            }
        }
        return instance;
    }

    public String getProperty(String key) {
        return properties.getProperty(key);
    }

    public String getProperty(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    private void loadPropertiesFromClasspath() throws IOException {
        try (InputStream input = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream("application.properties")) {
            if (Objects.isNull(input)) {
                return; // no properties file found; callers will rely on defaults
            }
            properties.load(input);
        }
    }
}
