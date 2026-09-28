package com.recalldesk.app.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Low-level HTTP client for the Groq LLM API (OpenAI-compatible).
 */
@Component
public class GroqClient {

    private static final Logger log = LoggerFactory.getLogger(GroqClient.class);

    @Value("${groq.api.base-url}")
    private String baseUrl;

    @Value("${groq.api.key}")
    private String apiKey;

    @Value("${groq.api.model}")
    private String model;

    @Value("${groq.api.max-tokens:1024}")
    private int maxTokens;

    @Value("${groq.api.temperature:0.7}")
    private double temperature;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GroqClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }

    public GroqResponse chat(List<GroqMessage> messages) {
        try {
            String url = baseUrl + "/chat/completions";

            ArrayNode messagesNode = objectMapper.createArrayNode();
            for (GroqMessage msg : messages) {
                ObjectNode msgNode = objectMapper.createObjectNode();
                msgNode.put("role", msg.getRole());
                msgNode.put("content", msg.getContent());
                messagesNode.add(msgNode);
            }

            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", model);
            body.set("messages", messagesNode);
            body.put("max_tokens", maxTokens);
            body.put("temperature", temperature);
            body.put("stream", false);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parseResponse(response.getBody());
            }
            return GroqResponse.error("Empty response from Groq");

        } catch (Exception e) {
            log.error("Groq API error: {}", e.getMessage());
            return GroqResponse.error("Groq API error: " + e.getMessage());
        }
    }

    public GroqResponse complete(String systemPrompt, String userMessage) {
        return chat(List.of(
                new GroqMessage("system", systemPrompt),
                new GroqMessage("user", userMessage)
        ));
    }

    private GroqResponse parseResponse(String json) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode choice = root.path("choices").path(0);
            String content = choice.path("message").path("content").asText("");
            String finishReason = choice.path("finish_reason").asText("stop");
            int inputTokens = root.path("usage").path("prompt_tokens").asInt(0);
            int outputTokens = root.path("usage").path("completion_tokens").asInt(0);

            GroqResponse r = new GroqResponse();
            r.setContent(content);
            r.setFinishReason(finishReason);
            r.setInputTokens(inputTokens);
            r.setOutputTokens(outputTokens);
            r.setSuccess(true);
            return r;

        } catch (Exception e) {
            log.error("Failed to parse Groq response: {}", e.getMessage());
            return GroqResponse.error("Failed to parse response");
        }
    }

    // -----------------------------------------------------------------------
    // DTOs — plain Java (no Lombok inner-class shorthand)
    // -----------------------------------------------------------------------

    public static class GroqMessage {
        private String role;
        private String content;

        public GroqMessage() {}
        public GroqMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
    }

    public static class GroqResponse {
        private String content;
        private String finishReason;
        private int inputTokens;
        private int outputTokens;
        private boolean success;
        private String errorMessage;

        public GroqResponse() {}

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getFinishReason() { return finishReason; }
        public void setFinishReason(String finishReason) { this.finishReason = finishReason; }
        public int getInputTokens() { return inputTokens; }
        public void setInputTokens(int inputTokens) { this.inputTokens = inputTokens; }
        public int getOutputTokens() { return outputTokens; }
        public void setOutputTokens(int outputTokens) { this.outputTokens = outputTokens; }
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

        public static GroqResponse error(String message) {
            GroqResponse r = new GroqResponse();
            r.success = false;
            r.errorMessage = message;
            r.content = "I'm sorry, I'm having trouble processing your request right now. Please try again.";
            return r;
        }
    }
}
