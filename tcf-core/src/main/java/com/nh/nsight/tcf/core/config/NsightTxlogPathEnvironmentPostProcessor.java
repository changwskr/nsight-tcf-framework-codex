package com.nh.nsight.tcf.core.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

/**
 * bootRun·IDE 등 실행 위치가 달라도 프로젝트 루트 {@code data/nsight-txlog} H2 파일을 공유하도록
 * {@code nsight.txlog.path}를 자동 설정한다.
 */
public class NsightTxlogPathEnvironmentPostProcessor implements EnvironmentPostProcessor {
    private static final Logger log = LoggerFactory.getLogger(NsightTxlogPathEnvironmentPostProcessor.class);
    private static final String PROPERTY = "nsight.txlog.path";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (hasExplicitPath(environment)) {
            return;
        }
        Path txlogDir = resolveTxlogDir();
        Map<String, Object> map = new HashMap<>();
        map.put(PROPERTY, txlogDir.toString().replace('\\', '/'));
        environment.getPropertySources().addFirst(new MapPropertySource("nsightTxlogPathAuto", map));
        log.info("{} auto-resolved to {}", PROPERTY, txlogDir);
    }

    private boolean hasExplicitPath(ConfigurableEnvironment environment) {
        if (environment.containsProperty(PROPERTY)) {
            return true;
        }
        String sys = System.getProperty(PROPERTY);
        if (sys != null && !sys.isBlank()) {
            return true;
        }
        String env = System.getenv("NSIGHT_TXLOG_PATH");
        return env != null && !env.isBlank();
    }

    static Path resolveTxlogDir() {
        Path cwd = Paths.get("").toAbsolutePath().normalize();
        for (Path dir = cwd; dir != null; dir = dir.getParent()) {
            if (Files.exists(dir.resolve("settings.gradle"))) {
                return dir.resolve("data").resolve("nsight-txlog").toAbsolutePath().normalize();
            }
        }
        return cwd.resolve("data").resolve("nsight-txlog").toAbsolutePath().normalize();
    }
}
