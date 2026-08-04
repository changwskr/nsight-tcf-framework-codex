package com.nh.nsight.harness.prompt;

import com.nh.nsight.harness.agent.AgentExecutionRequest;

import java.nio.file.Path;

public record StagePromptContract(Path directory, AgentExecutionRequest request) {
}
