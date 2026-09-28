package com.recalldesk.app.integration;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Low-level HTTP client for the Hindsight REST API.
 *
 * Hindsight API Reference: https://hindsight.vectorize.io/api-reference
 * Base URL: https://api.hindsight.vectorize.io
 *
 * Core operations:
 *   POST /v1/default/banks/{bank_id}/retain   — Store memories
 *   POST /v1/default/banks/{bank_id}/recall   — Search memories
 *   POST /v1/default/banks/{bank_id}/reflect  — AI reasoning over memories
 *   POST /v1/default/banks                    — Create a bank
 *   GET  /v1/default/banks/{bank_id}/memories — List memories
 */
@Component
public class HindsightClient {

    private static final Logger log = LoggerFactory.getLogger(HindsightClient.class);

    @Value("${hindsight.api.base-url}")
    private String baseUrl;

    @Value("${hindsight.api.key}")
    private String apiKey;

    @Value("${hindsight.api.fail-gracefully:true}")
    private boolean failGracefully;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public HindsightClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }

    // -----------------------------------------------------------------------
    // Bank Management
    // -----------------------------------------------------------------------

    /**
     * Create a memory bank for a customer.
     */
    public boolean createBank(String bankId, String name, String background) {
        try {
            String url = baseUrl + "/v1/default/banks";

            ObjectNode body = objectMapper.createObjectNode();
            body.put("bank_id", bankId);
            body.put("name", name);
            if (background != null) body.put("background", background);

            ObjectNode disposition = objectMapper.createObjectNode();
            disposition.put("empathy", 5);
            disposition.put("skepticism", 2);
            disposition.put("literalism", 3);
            body.set("disposition", disposition);

            HttpHeaders headers = buildHeaders();
            HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
            log.info("Hindsight bank created: {} — status {}", bankId, response.getStatusCode());
            return response.getStatusCode().is2xxSuccessful();

        } catch (HttpClientErrorException.Conflict e) {
            log.debug("Hindsight bank {} already exists", bankId);
            return true;
        } catch (HttpClientErrorException e) {
            // 405 also signals the bank already exists in some Hindsight deployments
            if (e.getStatusCode().value() == 405 || e.getStatusCode().value() == 422) {
                log.debug("Hindsight bank {} already exists or not supported: {}", bankId, e.getStatusCode());
                return true;
            }
            log.warn("Failed to create Hindsight bank {}: {}", bankId, e.getMessage());
            return failGracefully;
        } catch (Exception e) {
            log.warn("Failed to create Hindsight bank {}: {}", bankId, e.getMessage());
            return failGracefully;
        }
    }

    // -----------------------------------------------------------------------
    // Retain — Store Memories
    // -----------------------------------------------------------------------

    public boolean retain(String bankId, String content, String context,
                          String documentId, Map<String, String> metadata) {
        try {
            String url = baseUrl + "/v1/default/banks/" + bankId + "/retain";

            ObjectNode itemNode = objectMapper.createObjectNode();
            itemNode.put("content", content);
            if (context != null) itemNode.put("context", context);
            if (documentId != null) itemNode.put("document_id", documentId);
            itemNode.put("timestamp", Instant.now().toString());

            if (metadata != null && !metadata.isEmpty()) {
                ObjectNode metaNode = objectMapper.createObjectNode();
                metadata.forEach(metaNode::put);
                itemNode.set("metadata", metaNode);
            }

            ArrayNode items = objectMapper.createArrayNode();
            items.add(itemNode);

            ObjectNode body = objectMapper.createObjectNode();
            body.set("items", items);

            HttpHeaders headers = buildHeaders();
            HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
            log.info("Hindsight retain OK for bank {}: doc={}", bankId, documentId);
            return response.getStatusCode().is2xxSuccessful();

        } catch (Exception e) {
            log.warn("Hindsight retain failed for bank {}: {}", bankId, e.getMessage());
            return failGracefully;
        }
    }

    public boolean retainConversation(String bankId, String conversationText,
                                      String conversationId, String subject) {
        Map<String, String> metadata = Map.of(
                "conversation_id", conversationId,
                "subject", subject != null ? subject : "Support conversation"
        );
        return retain(bankId, conversationText, "support conversation",
                "conv_" + conversationId, metadata);
    }

    // -----------------------------------------------------------------------
    // Recall — Search / Retrieve Memories
    // -----------------------------------------------------------------------

    public HindsightRecallResult recall(String bankId, String query, int maxTokens) {
        try {
            String url = baseUrl + "/v1/default/banks/" + bankId + "/recall";

            ObjectNode body = objectMapper.createObjectNode();
            body.put("query", query);
            body.put("max_tokens", maxTokens);
            body.put("budget", "mid");
            body.put("include_entities", true);

            HttpHeaders headers = buildHeaders();
            HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parseRecallResponse(response.getBody());
            }
            return HindsightRecallResult.empty();

        } catch (ResourceAccessException e) {
            log.warn("Hindsight recall timeout for bank {}: {}", bankId, e.getMessage());
            return HindsightRecallResult.unavailable("Connection timeout — Hindsight may be unavailable");
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Hindsight bank not found: {}", bankId);
            return HindsightRecallResult.empty();
        } catch (Exception e) {
            log.warn("Hindsight recall failed for bank {}: {}", bankId, e.getMessage());
            return HindsightRecallResult.unavailable(e.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // Reflect — AI Reasoning Over Memories
    // -----------------------------------------------------------------------

    public HindsightReflectResult reflect(String bankId, String query, String context) {
        try {
            String url = baseUrl + "/v1/default/banks/" + bankId + "/reflect";

            ObjectNode body = objectMapper.createObjectNode();
            body.put("query", query);
            if (context != null) body.put("context", context);
            body.put("budget", "mid");

            HttpHeaders headers = buildHeaders();
            HttpEntity<String> request = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parseReflectResponse(response.getBody());
            }
            return HindsightReflectResult.empty();

        } catch (Exception e) {
            log.warn("Hindsight reflect failed for bank {}: {}", bankId, e.getMessage());
            return HindsightReflectResult.empty();
        }
    }

    // -----------------------------------------------------------------------
    // List Memories
    // -----------------------------------------------------------------------

    public HindsightRecallResult listMemories(String bankId, int limit) {
        try {
            String url = baseUrl + "/v1/default/banks/" + bankId + "/memories?limit=" + limit;

            HttpHeaders headers = buildHeaders();
            HttpEntity<Void> request = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return parseListMemoriesResponse(response.getBody());
            }
            return HindsightRecallResult.empty();

        } catch (Exception e) {
            log.warn("Hindsight list memories failed for bank {}: {}", bankId, e.getMessage());
            return HindsightRecallResult.empty();
        }
    }

    // -----------------------------------------------------------------------
    // Parse Responses
    // -----------------------------------------------------------------------

    private HindsightRecallResult parseRecallResponse(String json) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        List<MemoryItem> items = new ArrayList<>();

        JsonNode results = root.path("results");
        if (results.isArray()) {
            for (JsonNode node : results) {
                MemoryItem item = new MemoryItem();
                item.setText(node.path("text").asText(""));
                item.setType(node.path("type").asText("observation"));
                item.setId(node.path("id").asText(""));
                items.add(item);
            }
        }

        HindsightRecallResult result = new HindsightRecallResult();
        result.setMemories(items);
        result.setAvailable(true);
        result.setTotalFound(items.size());
        return result;
    }

    private HindsightRecallResult parseListMemoriesResponse(String json) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        List<MemoryItem> items = new ArrayList<>();

        JsonNode memories = root.path("items");
        if (memories.isArray()) {
            for (JsonNode node : memories) {
                MemoryItem item = new MemoryItem();
                item.setText(node.path("text").asText(node.path("content").asText("")));
                item.setType(node.path("type").asText("world"));
                item.setId(node.path("id").asText(""));
                items.add(item);
            }
        }

        HindsightRecallResult result = new HindsightRecallResult();
        result.setMemories(items);
        result.setAvailable(true);
        result.setTotalFound(root.path("total").asInt(items.size()));
        return result;
    }

    private HindsightReflectResult parseReflectResponse(String json) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        HindsightReflectResult result = new HindsightReflectResult();
        result.setText(root.path("text").asText(""));
        result.setAvailable(true);

        List<String> basedOn = new ArrayList<>();
        JsonNode basedOnNode = root.path("based_on");
        if (basedOnNode.isArray()) {
            basedOnNode.forEach(n -> basedOn.add(n.asText()));
        }
        result.setBasedOn(basedOn);
        return result;
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);
        headers.set("Accept", "application/json");
        return headers;
    }

    // -----------------------------------------------------------------------
    // Result types — plain Java classes (no Lombok to avoid annotation issues)
    // -----------------------------------------------------------------------

    public static class HindsightRecallResult {
        private List<MemoryItem> memories = new ArrayList<>();
        private boolean available = true;
        private int totalFound = 0;
        private String errorMessage;

        public List<MemoryItem> getMemories() { return memories; }
        public void setMemories(List<MemoryItem> memories) { this.memories = memories; }
        public boolean isAvailable() { return available; }
        public void setAvailable(boolean available) { this.available = available; }
        public int getTotalFound() { return totalFound; }
        public void setTotalFound(int totalFound) { this.totalFound = totalFound; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

        public static HindsightRecallResult empty() {
            HindsightRecallResult r = new HindsightRecallResult();
            r.available = true;
            return r;
        }

        public static HindsightRecallResult unavailable(String message) {
            HindsightRecallResult r = new HindsightRecallResult();
            r.available = false;
            r.errorMessage = message;
            return r;
        }

        public boolean hasMemories() {
            return memories != null && !memories.isEmpty();
        }

        public String toContextString() {
            if (!hasMemories()) return "";
            StringBuilder sb = new StringBuilder();
            for (MemoryItem m : memories) {
                sb.append("- ").append(m.getText()).append("\n");
            }
            return sb.toString().trim();
        }
    }

    public static class HindsightReflectResult {
        private String text;
        private List<String> basedOn = new ArrayList<>();
        private boolean available = true;

        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public List<String> getBasedOn() { return basedOn; }
        public void setBasedOn(List<String> basedOn) { this.basedOn = basedOn; }
        public boolean isAvailable() { return available; }
        public void setAvailable(boolean available) { this.available = available; }

        public static HindsightReflectResult empty() {
            HindsightReflectResult r = new HindsightReflectResult();
            r.available = false;
            r.text = "";
            return r;
        }
    }

    public static class MemoryItem {
        private String id;
        private String text;
        private String type;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getText() { return text; }
        public void setText(String text) { this.text = text; }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
    }
}
