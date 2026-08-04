package com.nh.nsight.harness;

import com.nh.nsight.harness.cli.HarnessCommandRouter;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class HarnessApplication implements CommandLineRunner {
    public static void main(String[] args) {
        SpringApplication.run(HarnessApplication.class, args);
    }

    @Override
    public void run(String... args) {
        int exitCode = new HarnessCommandRouter(System.out, System.err).run(args);
        if (exitCode != 0) {
            throw new IllegalStateException("Harness command failed with exit code " + exitCode);
        }
    }
}
