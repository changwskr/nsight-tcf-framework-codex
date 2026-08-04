package com.nh.nsight.harness.agent;

import java.io.IOException;

public interface AgentCommandAdapter {
    AgentExecutionResult execute(AgentConfig config, AgentExecutionRequest request) throws IOException, InterruptedException;
}
