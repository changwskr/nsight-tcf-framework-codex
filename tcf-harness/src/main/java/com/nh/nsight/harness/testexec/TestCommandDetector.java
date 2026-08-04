package com.nh.nsight.harness.testexec;

import com.nh.nsight.harness.domain.TestCommand;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class TestCommandDetector {
    public List<TestCommand> detect(Path repositoryRoot) {
        List<TestCommand> candidates = new ArrayList<>();
        if (Files.exists(repositoryRoot.resolve("gradlew")) || Files.exists(repositoryRoot.resolve("gradlew.bat"))) {
            String gradle = Files.exists(repositoryRoot.resolve("gradlew")) ? "./gradlew" : "gradlew.bat";
            candidates.add(new TestCommand("UNIT_TEST", gradle + " test", false, 900));
            candidates.add(new TestCommand("QUALITY_GATE", gradle + " check", false, 1800));
        } else if (Files.exists(repositoryRoot.resolve("pom.xml"))) {
            candidates.add(new TestCommand("UNIT_TEST", "mvn test", false, 900));
            candidates.add(new TestCommand("QUALITY_GATE", "mvn verify", false, 1800));
        } else if (Files.exists(repositoryRoot.resolve("package.json"))) {
            candidates.add(new TestCommand("UNIT_TEST", "npm test", false, 900));
        }
        if (Files.exists(repositoryRoot.resolve("scripts/test.sh"))) {
            candidates.add(new TestCommand("CUSTOM_TEST", "sh scripts/test.sh", false, 1800));
        }
        return List.copyOf(candidates);
    }
}
