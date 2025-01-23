package wechat.service;

import wechat.config.ChatwootConfig;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChatwootService {

    private final ChatwootConfig chatwootConfig;
    private final RestTemplate restTemplate = new RestTemplate();
    private static final Logger log = LoggerFactory.getLogger(ChatwootService.class);

    /**
     * 将消息转发到 Chatwoot
     *
     * @param userId 用户标识符
     * @param content 消息内容
     */
    public void forwardMessageToChatwoot(String userId, String content) {
        String url = String.format("%s/public/api/v1/inboxes/%s/contacts",
                chatwootConfig.getApiUrl(),
                chatwootConfig.getInboxId());

        // 创建联系人请求的 Payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("name", userId);
        payload.put("identifier", Map.of("identifier", userId));

        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + chatwootConfig.getApiToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        String conversationId = "";

        // 构建请求实体
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        try {
            log.info("Sending message to Chatwoot: URL={} Payload={}", url, payload);

            // 创建或获取联系人
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, request, String.class);

            if (responseEntity.getStatusCode() == HttpStatus.OK || responseEntity.getStatusCode() == HttpStatus.CREATED) {
                log.info("Contact successfully created or retrieved.");

                // 提取联系人响应体
                String responseBody = responseEntity.getBody();
                log.info("Response Body: {}", responseBody);

                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
                String sourceId = (String) responseMap.get("source_id");

                // 获取联系人会话列表
                String conversationUrl = String.format("%s/%s/conversations", url, sourceId);
                responseEntity = restTemplate.exchange(conversationUrl, HttpMethod.GET, request, String.class);

                if (responseEntity.getStatusCode() == HttpStatus.OK) {
                    responseBody = responseEntity.getBody();
                    log.info("Fetched conversations: {}", responseBody);

                    // 解析会话列表
                    List<Map<String, Object>> conversations = objectMapper.readValue(responseBody, List.class);

                    if (!conversations.isEmpty()) {
                        // 获取第一个会话的 ID
                        Map<String, Object> firstConversation = conversations.get(0);
                        Object idObject = firstConversation.get("id");

                        if (idObject instanceof Integer) {
                            conversationId = String.valueOf(idObject);
                        } else if (idObject instanceof String) {
                            conversationId = (String) idObject;
                        } else {
                            throw new IllegalArgumentException("Unexpected type for conversation ID: " + idObject.getClass().getName());
                        }
                    } else {
                        log.info("No active conversation found. Creating a new one.");
                        conversationId = createNewConversation(conversationUrl, headers, objectMapper);
                    }
                } else {
                    log.error("Failed to fetch conversations. Status: {}", responseEntity.getStatusCode());
                }

                // 发送消息到会话
                if (!conversationId.isEmpty()) {
                    String messageUrl = String.format("%s/%s/messages", conversationUrl, conversationId);
                    payload = new HashMap<>();
                    payload.put("content", content);

                    request = new HttpEntity<>(payload, headers);
                    responseEntity = restTemplate.postForEntity(messageUrl, request, String.class);

                    if (responseEntity.getStatusCode() == HttpStatus.OK || responseEntity.getStatusCode() == HttpStatus.CREATED) {
                        log.info("Message successfully sent to Chatwoot conversation.");
                    } else {
                        log.error("Failed to send message to Chatwoot conversation. Status: {}", responseEntity.getStatusCode());
                    }
                }
            } else {
                log.error("Unexpected response status: {}", responseEntity.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error forwarding message to Chatwoot: {}", e.getMessage(), e);
        }
    }

    /**
     * 创建一个新的会话
     *
     * @param conversationUrl 会话 API 的 URL
     * @param headers 请求头
     * @param objectMapper JSON 解析器
     * @return 创建的会话 ID
     */
    private String createNewConversation(String conversationUrl, HttpHeaders headers, ObjectMapper objectMapper) {
        try {
            Map<String, Object> createConversationPayload = new HashMap<>();
            HttpEntity<Map<String, Object>> createConversationRequest = new HttpEntity<>(createConversationPayload, headers);

            ResponseEntity<String> createConversationResponse = restTemplate.postForEntity(conversationUrl, createConversationRequest, String.class);

            if (createConversationResponse.getStatusCode() == HttpStatus.CREATED || createConversationResponse.getStatusCode() == HttpStatus.OK) {
                String responseBody = createConversationResponse.getBody();
                log.info("Successfully created a new conversation: {}", responseBody);

                Map<String, Object> createResponseMap = objectMapper.readValue(responseBody, Map.class);
                Object idObject = createResponseMap.get("id");

                if (idObject instanceof Integer) {
                    return String.valueOf(idObject);
                } else if (idObject instanceof String) {
                    return (String) idObject;
                }
            } else {
                log.error("Failed to create a new conversation. Status: {}", createConversationResponse.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error occurred while creating a new conversation: {}", e.getMessage(), e);
        }
        return "";
    }
}
