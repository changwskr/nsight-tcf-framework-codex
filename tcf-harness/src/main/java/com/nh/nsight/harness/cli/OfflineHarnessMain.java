package com.nh.nsight.harness.cli;

public final class OfflineHarnessMain {
    private OfflineHarnessMain() {
    }

    public static void main(String[] args) {
        int exit = new HarnessCommandRouter(System.out, System.err).run(args);
        if (exit != 0) {
            System.exit(exit);
        }
    }
}
