package nhnis.fw.commons.context;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;

@Component
public class SpringContext {

    @Autowired
    private Environment environment;

    private static Environment staticEnvironment;

    @PostConstruct
    private void initStatic() {
        staticEnvironment = this.environment;
    }

    public static String getProperty(String key) {
        return staticEnvironment.getProperty(key);
    }

    public static String getProperty(String key, String key2) {
        return staticEnvironment.getProperty(key, key2);
    }
}
