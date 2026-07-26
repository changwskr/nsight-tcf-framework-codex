package com.nh.nsight.marketing.om.client;

import com.nh.nsight.tcf.core.support.error.BusinessException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OmCicdClientService {
    private static final Logger log = LoggerFactory.getLogger(OmCicdClientService.class);

    private final Path frameworkRoot;
    private final Path webappsDir;
    private final String gatewayBaseUrl;
    private final String gradleCommand;

    public OmCicdClientService(
            @Value("${nsight.om.cicd.framework-root:}") String frameworkRootOverride,
            @Value("${nsight.om.cicd.webapps-dir:}") String webappsDirOverride,
            @Value("${nsight.gateway.base-url:http://127.0.0.1:8080}") String gatewayBaseUrl,
            @Value("${nsight.om.cicd.gradle-command:}") String gradleCommand) {
        Path cwd = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        this.frameworkRoot = frameworkRootOverride == null || frameworkRootOverride.isBlank()
                ? cwd
                : Path.of(frameworkRootOverride).toAbsolutePath().normalize();
        this.webappsDir = webappsDirOverride == null || webappsDirOverride.isBlank()
                ? this.frameworkRoot.resolve("ztomcat/apache-tomcat-10.1.34/webapps")
                : Path.of(webappsDirOverride).toAbsolutePath().normalize();
        this.gatewayBaseUrl = gatewayBaseUrl.endsWith("/") ? gatewayBaseUrl.substring(0, gatewayBaseUrl.length() - 1)
                : gatewayBaseUrl;
        this.gradleCommand = resolveGradleCommand(gradleCommand, this.frameworkRoot);
        log.info("OM CICD gradle command: {}", this.gradleCommand);
    }

    public record ModuleSpec(String moduleName, String warFileName, String deployWarName, String contextPath) {
    }

    public ModuleSpec resolveModule(String businessCode, String moduleName) {
        if (moduleName != null && !moduleName.isBlank()) {
            return moduleByName(moduleName);
        }
        String code = businessCode == null ? "" : businessCode.trim().toUpperCase();
        return switch (code) {
            case "IC" -> new ModuleSpec("ic-service", "ic.war", "ic.war", "ic");
            case "PC" -> new ModuleSpec("pc-service", "pc.war", "pc.war", "pc");
            case "MS" -> new ModuleSpec("ms-service", "ms.war", "ms.war", "ms");
            case "SV" -> new ModuleSpec("sv-service", "sv.war", "sv.war", "sv");
            case "PD" -> new ModuleSpec("pd-service", "pd.war", "pd.war", "pd");
            case "EB" -> new ModuleSpec("eb-service", "eb.war", "eb.war", "eb");
            case "EP" -> new ModuleSpec("ep-service", "ep.war", "ep.war", "ep");
            case "SS" -> new ModuleSpec("ss-service", "ss.war", "ss.war", "ss");
            case "MG" -> new ModuleSpec("mg-service", "mg.war", "mg.war", "mg");
            case "OM" -> new ModuleSpec("tcf-om", "tcf-om.war", "om.war", "om");
            case "UI" -> new ModuleSpec("tcf-ui", "tcf-ui.war", "ui.war", "ui");
            case "BATCH" -> new ModuleSpec("tcf-batch", "tcf-batch.war", "zz-batch.war", "batch");
            default -> throw new BusinessException("E-OM-BIZ-0003", "지원하지 않는 businessCode: " + code);
        };
    }

    private ModuleSpec moduleByName(String moduleName) {
        String name = moduleName.trim();
        if ("tcf-om".equals(name)) {
            return new ModuleSpec("tcf-om", "tcf-om.war", "om.war", "om");
        }
        if ("tcf-ui".equals(name)) {
            return new ModuleSpec("tcf-ui", "tcf-ui.war", "ui.war", "ui");
        }
        if ("tcf-batch".equals(name)) {
            return new ModuleSpec("tcf-batch", "tcf-batch.war", "zz-batch.war", "batch");
        }
        if (!name.endsWith("-service")) {
            name = name + "-service";
        }
        String ctx = name.substring(0, name.indexOf('-'));
        return new ModuleSpec(name, ctx + ".war", ctx + ".war", ctx);
    }

    public Map<String, Object> runGradleBuild(String moduleName, String gradleTask) {
        String task = gradleTask == null || gradleTask.isBlank() ? ":" + moduleName + ":bootWar" : gradleTask;
        ProcessResult result = runGradle(frameworkRoot, gradleCommand, task);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", result.exitCode() == 0);
        body.put("exitCode", result.exitCode());
        body.put("command", result.commandLine());
        body.put("output", truncate(result.output(), 8000));
        return body;
    }

    public Map<String, Object> deployWar(ModuleSpec spec) {
        Path src = frameworkRoot.resolve(spec.moduleName()).resolve("build/libs").resolve(spec.warFileName());
        if (!Files.isRegularFile(src)) {
            throw new BusinessException("E-OM-BIZ-0003", "WAR 파일이 없습니다. 먼저 빌드를 실행하세요: " + src);
        }
        try {
            Files.createDirectories(webappsDir);
            Path dest = webappsDir.resolve(spec.deployWarName());
            Files.copy(src, dest, StandardCopyOption.REPLACE_EXISTING);
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("success", true);
            body.put("source", src.toString());
            body.put("destination", dest.toString());
            body.put("contextPath", "/" + spec.contextPath());
            return body;
        } catch (IOException e) {
            throw new BusinessException("E-OM-BIZ-0003", "WAR 배포 실패: " + e.getMessage());
        }
    }

    public Map<String, Object> healthCheck(String contextPath) {
        String path = contextPath.startsWith("/") ? contextPath : "/" + contextPath;
        String url = gatewayBaseUrl + path + "/actuator/health";
        try {
            HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestMethod("GET");
            int code = conn.getResponseCode();
            String body = readStream(code >= 400 ? conn.getErrorStream() : conn.getInputStream());
            String result = code == 200 && body.contains("\"status\":\"UP\"") ? "UP" : "DOWN";
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("healthCheckUrl", url);
            map.put("httpStatus", code);
            map.put("healthCheckResult", result);
            map.put("responseBody", truncate(body, 2000));
            return map;
        } catch (IOException e) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("healthCheckUrl", url);
            map.put("healthCheckResult", "DOWN");
            map.put("responseBody", e.getMessage());
            return map;
        }
    }

    private ProcessResult runGradle(Path projectRoot, String gradleExecutable, String task) {
        if (isWindows() && gradleExecutable.toLowerCase().endsWith(".bat")) {
            String projectDir = projectRoot.toAbsolutePath().normalize().toString();
            String commandLine = "call " + quoteForCmd(gradleExecutable)
                    + " -p " + quoteForCmd(projectDir)
                    + " " + task;
            return runWindowsCommand(commandLine);
        }
        return runProcess(projectRoot, gradleExecutable, task);
    }

    private ProcessResult runWindowsCommand(String commandLine) {
        try {
            ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", commandLine);
            pb.redirectErrorStream(true);
            log.debug("OM CICD execute: {}", commandLine);
            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            boolean finished = process.waitFor(20, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                throw new BusinessException("E-OM-BIZ-0003", "Gradle 빌드 시간 초과");
            }
            return new ProcessResult(process.exitValue(), output, commandLine);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("E-OM-BIZ-0003", "Gradle 실행 실패: " + e.getMessage());
        }
    }

    private ProcessResult runProcess(Path cwd, String... command) {
        try {
            ProcessBuilder pb;
            if (isWindows() && command.length > 0 && command[0].toLowerCase().endsWith(".bat")) {
                StringBuilder commandLine = new StringBuilder("call ").append(quoteForCmd(command[0]));
                for (int i = 1; i < command.length; i++) {
                    commandLine.append(' ').append(command[i]);
                }
                pb = new ProcessBuilder("cmd.exe", "/c", commandLine.toString());
            } else {
                pb = new ProcessBuilder(command);
            }
            pb.directory(cwd.toFile());
            pb.redirectErrorStream(true);
            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            boolean finished = process.waitFor(20, TimeUnit.MINUTES);
            if (!finished) {
                process.destroyForcibly();
                throw new BusinessException("E-OM-BIZ-0003", "Gradle 빌드 시간 초과");
            }
            String commandLine = String.join(" ", command);
            return new ProcessResult(process.exitValue(), output, commandLine);
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("E-OM-BIZ-0003", "Gradle 실행 실패: " + e.getMessage());
        }
    }

    private static String quoteForCmd(String value) {
        if (value == null || value.isBlank()) {
            return "\"\"";
        }
        if (value.indexOf(' ') >= 0 || value.indexOf('(') >= 0 || value.indexOf(')') >= 0) {
            return "\"" + value + "\"";
        }
        return value;
    }

    static String resolveGradleCommand(String configured, Path frameworkRoot) {
        if (configured != null && !configured.isBlank()) {
            Path configuredPath = Path.of(configured);
            if (Files.isRegularFile(configuredPath)) {
                return configuredPath.toAbsolutePath().normalize().toString();
            }
            if (!"gradle".equalsIgnoreCase(configured.trim())) {
                return configured.trim();
            }
        }
        String fromOverride = gradleFromHome(System.getenv("GRADLE_HOME_OVERRIDE"));
        if (fromOverride != null) {
            return fromOverride;
        }
        String fromHome = gradleFromHome(System.getenv("GRADLE_HOME"));
        if (fromHome != null) {
            return fromHome;
        }
        Path parent = frameworkRoot.getParent();
        if (parent != null) {
            try (var stream = Files.list(parent)) {
                Path found = stream
                        .filter(p -> Files.isDirectory(p) && p.getFileName().toString().startsWith("gradle-"))
                        .map(OmCicdClientService::gradleBinaryInHome)
                        .filter(p -> p != null && Files.isRegularFile(p))
                        .findFirst()
                        .orElse(null);
                if (found != null) {
                    return found.toAbsolutePath().normalize().toString();
                }
            } catch (IOException ignored) {
                /* fall through */
            }
        }
        return "gradle";
    }

    private static String gradleFromHome(String home) {
        if (home == null || home.isBlank()) {
            return null;
        }
        Path binary = gradleBinaryInHome(Path.of(home));
        return binary != null && Files.isRegularFile(binary) ? binary.toAbsolutePath().normalize().toString() : null;
    }

    private static Path gradleBinaryInHome(Path home) {
        String name = isWindows() ? "gradle.bat" : "gradle";
        return home.resolve("bin").resolve(name);
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("win");
    }

    private static String readStream(InputStream stream) throws IOException {
        if (stream == null) {
            return "";
        }
        return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        return value.length() <= max ? value : value.substring(0, max) + "...";
    }

    private record ProcessResult(int exitCode, String output, String commandLine) {
    }
}
