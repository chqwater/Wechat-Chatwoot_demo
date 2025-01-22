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
        // 拼接 Chatwoot API URL
        String url = String.format("%s/public/api/v1/inboxes/%s/contacts",
                chatwootConfig.getApiUrl(),
                chatwootConfig.getInboxId());

        // 创建消息 Payload
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
            // 记录请求 URL 和 Payload
            log.info("Sending message to Chatwoot: URL={} Payload={}", url, payload);

            // 发送 POST 请求并获取响应
            ResponseEntity<String> responseEntity = restTemplate.postForEntity(url, request, String.class);

            // 检查响应状态码
            if (responseEntity.getStatusCode() == HttpStatus.OK || responseEntity.getStatusCode() == HttpStatus.CREATED) {
                log.info("Message successfully forwarded to Chatwoot.");

                // 提取响应体
                String responseBody = responseEntity.getBody();
                log.info("Response Body: {}", responseBody);

                // 解析 JSON 响应提取 source_id
                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, Object> responseMap = objectMapper.readValue(responseBody, Map.class);
                String sourceId = (String) responseMap.get("source_id");

                // 获取当前用户的 conversation 列表
                String conversationUrl = String.format("%s/%s/conversations", url, sourceId);
                responseEntity = restTemplate.getForEntity(conversationUrl, String.class);
                if (responseEntity.getStatusCode() == HttpStatus.OK) {
                    responseBody = responseEntity.getBody();
                    List<Map<String, Object>> conversations = objectMapper.readValue(responseBody, List.class);

                    if (!conversations.isEmpty()) {
                        // 获取第一个 conversation 的 id
                        Map<String, Object> firstConversation = conversations.get(0);
                        Object idObject = firstConversation.get("id");

                        if (idObject instanceof Integer) {
                            conversationId = String.valueOf(idObject); // 将 Integer 转换为 String
                        } else if (idObject instanceof String) {
                            conversationId = (String) idObject; // 如果是 String，直接使用
                        } else {
                            throw new IllegalArgumentException("Unexpected type for conversation ID: " + idObject.getClass().getName());
                        }
                    } else {
                        // 当前无 conversation，创建一个
                        request = new HttpEntity<>(new HashMap<>(), headers);
                        responseEntity = restTemplate.postForEntity(conversationUrl, request, String.class);
                        if (responseEntity.getStatusCode() == HttpStatus.OK) {
                            responseBody = responseEntity.getBody();
                            responseMap = objectMapper.readValue(responseBody, Map.class);
                            Object idObject = responseMap.get("id");

                            if (idObject instanceof Integer) {
                                conversationId = String.valueOf(idObject); // 将 Integer 转换为 String
                            } else if (idObject instanceof String) {
                                conversationId = (String) idObject; // 如果是 String，直接使用
                            } else {
                                throw new IllegalArgumentException("Unexpected type for conversation ID: " + idObject.getClass().getName());
                            }
                        }
                    }
                } else {
                    log.error("Failed to fetch conversations. Status: {}", responseEntity.getStatusCode());
                }

                // 发送消息到 conversation
                String messageUrl = String.format("%s/%s/messages", conversationUrl, conversationId);
                payload = new HashMap<>();
                payload.put("content", content);
                payload.put("echo_id", "none");

                request = new HttpEntity<>(payload, headers);
                responseEntity = restTemplate.postForEntity(messageUrl, request, String.class);

                if (responseEntity.getStatusCode() == HttpStatus.OK || responseEntity.getStatusCode() == HttpStatus.CREATED) {
                    log.info("Message successfully sent to Chatwoot conversation.");
                } else {
                    log.error("Failed to send message to Chatwoot conversation. Status: {}", responseEntity.getStatusCode());
                }
            } else {
                log.error("Unexpected response status: {}", responseEntity.getStatusCode());
            }
        } catch (Exception e) {
            // 记录错误日志
            log.error("Error forwarding message to Chatwoot: {}", e.getMessage(), e);
            log.error("Request URL: {}", url);
            log.error("Payload: {}", payload);
        }
    }
}
