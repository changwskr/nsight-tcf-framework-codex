package com.nh.nsight.tcf.web.support.runtime;

import java.lang.management.ManagementFactory;
import java.util.Optional;
import javax.management.MBeanServer;
import javax.management.ObjectName;

/** Tomcat ThreadPool JMX (Catalina / embedded Tomcat) 조회. */
final class TomcatThreadPoolProbe {
    private TomcatThreadPoolProbe() {}

    record Stats(int maxThreads, int currentThreads, int busyThreads, String poolName) {}

    record AcceptQueue(int current, int max) {}

    static Optional<Stats> resolvePrimaryHttpPool() {
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        Optional<Stats> best = Optional.empty();
        for (String pattern : new String[] {"Catalina:type=ThreadPool,*", "Tomcat:type=ThreadPool,*"}) {
            try {
                for (ObjectName name : server.queryNames(new ObjectName(pattern), null)) {
                    String poolName = name.getKeyProperty("name");
                    if (poolName == null || !poolName.toLowerCase().contains("http")) {
                        continue;
                    }
                    int max = intAttr(server, name, "maxThreads");
                    int current = intAttr(server, name, "currentThreadCount");
                    int busy = intAttr(server, name, "currentThreadsBusy");
                    if (max <= 0) {
                        continue;
                    }
                    Stats candidate = new Stats(max, current, busy, poolName);
                    if (best.isEmpty() || busy > best.get().busyThreads()) {
                        best = Optional.of(candidate);
                    }
                }
            } catch (Exception ignored) {
                // 다음 패턴 시도
            }
        }
        return best;
    }

    static Optional<AcceptQueue> resolveAcceptQueue() {
        MBeanServer server = ManagementFactory.getPlatformMBeanServer();
        for (String pattern : new String[] {"Catalina:type=Connector,*", "Tomcat:type=Connector,*"}) {
            try {
                for (ObjectName name : server.queryNames(new ObjectName(pattern), null)) {
                    String protocol = name.getKeyProperty("protocol");
                    if (protocol != null && !protocol.toLowerCase().contains("http")) {
                        continue;
                    }
                    int current = intAttr(server, name, "acceptCount");
                    int max = intAttr(server, name, "maxConnections");
                    if (max <= 0) {
                        max = 500;
                    }
                    return Optional.of(new AcceptQueue(Math.max(0, current), max));
                }
            } catch (Exception ignored) {
                // 다음 패턴
            }
        }
        return Optional.empty();
    }

    private static int intAttr(MBeanServer server, ObjectName name, String attribute) {
        try {
            Object value = server.getAttribute(name, attribute);
            if (value instanceof Number number) {
                return number.intValue();
            }
        } catch (Exception ignored) {
            // JMX 미지원·속성 없음
        }
        return 0;
    }
}
