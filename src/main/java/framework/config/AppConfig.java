package framework.config;

import framework.annotations.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Loads {@code application.properties} from the classpath root into a {@link Properties} object.
 *
 * <p><b>Spring equivalent:</b> {@code Environment} / {@code @ConfigurationProperties} /
 * {@code application.yml} loaded by {@code PropertySourcesPlaceholderConfigurer}.
 *
 * <p><b>Classpath rule:</b> After {@code ./scripts/build.sh}, resources live at
 * {@code out/classes/application.properties}. {@code getResourceAsStream("application.properties")}
 * finds them because they're at the classpath root (same as Spring Boot's {@code src/main/resources}).
 *
 * <p><b>Learn:</b> Config externalizes environment-specific values (DB URL, API keys)
 * so you change behavior without recompiling code.
 */
@Component
public class AppConfig {

    private final Properties properties = new Properties();

    /**
     * Constructor runs when the container creates this bean — before @Inject wiring.
     * Loads properties immediately so other beans can read config during @PostConstruct.
     */
    public AppConfig() throws IOException {
        try (InputStream in = AppConfig.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (in == null) {
                throw new IOException("application.properties not found on classpath — did you run ./scripts/build.sh?");
            }
            properties.load(in);
        }
    }

    /** Returns a property value, or {@code null} if missing. */
    public String get(String key) {
        return properties.getProperty(key);
    }

    /** Returns a property value, or {@code defaultValue} if missing. */
    public String get(String key, String defaultValue) {
        return properties.getProperty(key, defaultValue);
    }

    /** Returns a property value or throws — use for required settings like {@code db.url}. */
    public String require(String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required property: " + key);
        }
        return value;
    }
}
