package com.nh.nsight.gateway.support;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class GatewayBusinessModules {
    private GatewayBusinessModules() {
    }

    public static final Module CC = new Module("CC", 8081);
    public static final Module BC = new Module("BC", 8084);
    public static final Module EB = new Module("EB", 8089);
    public static final Module EP = new Module("EP", 8090);
    public static final Module IC = new Module("IC", 8082);
    public static final Module MG = new Module("MG", 8096);
    public static final Module MS = new Module("MS", 8085);
    public static final Module OM = new Module("OM", 8097);
    public static final Module PC = new Module("PC", 8083);
    public static final Module PD = new Module("PD", 8087);
    public static final Module SS = new Module("SS", 8093);
    public static final Module SV = new Module("SV", 8086);
    public static final Module JWT = new Module("JWT", 8110, "/online");

    private static final Map<String, Module> BY_CODE = Arrays.stream(new Module[]{
            CC, BC, EB, EP, IC, MG, MS, OM, PC, PD, SS, SV, JWT
    }).collect(Collectors.toUnmodifiableMap(module -> module.code(), Function.identity()));

    public static Optional<Module> find(String code) {
        return Optional.ofNullable(BY_CODE.get(code.toUpperCase(Locale.ROOT)));
    }

    public static Module require(String code) {
        return find(code).orElseThrow(() ->
                new IllegalArgumentException("지원하지 않는 업무코드입니다: " + code));
    }

    public record Module(String code, int bootrunPort, String onlinePath) {
        public Module(String code, int bootrunPort) {
            this(code, bootrunPort, "/" + code.toLowerCase(Locale.ROOT) + "/online");
        }

        public String pathPrefix() {
            if ("/online".equals(onlinePath)) {
                return "";
            }
            int suffix = onlinePath.lastIndexOf("/online");
            return suffix > 0 ? onlinePath.substring(0, suffix) : "/" + code.toLowerCase(Locale.ROOT);
        }

        public String tomcatOnlinePath() {
            return onlinePath;
        }

        public String defaultBootrunOnlineUrl() {
            return "http://127.0.0.1:" + bootrunPort + tomcatOnlinePath();
        }

        public String serviceHint() {
            if ("OM".equals(code)) {
                return "tcf-om";
            }
            if ("JWT".equals(code)) {
                return "tcf-jwt";
            }
            return code.toLowerCase(Locale.ROOT) + "-service";
        }
    }
}
