package com.nh.nsight.harness.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

final class StreamCollector implements Runnable {
    private final InputStream input;
    private final Charset charset;
    private volatile String content = "";

    StreamCollector(InputStream input, Charset charset) {
        this.input = input;
        this.charset = charset;
    }

    @Override
    public void run() {
        try (input) {
            content = new String(input.readAllBytes(), charset);
        } catch (IOException e) {
            content = "[stream read failure] " + e.getMessage();
        }
    }

    String content() {
        return content;
    }
}
