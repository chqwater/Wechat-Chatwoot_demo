package wechat.service;

import wechat.config.ChatwootConfig;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
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
        String url = String.format("%s/accounts/%s/inboxes/%s/messages",
                chatwootConfig.getApiUrl(),
                "8", // 假设这是固定的账户 ID，可以根据需要动态获取
                chatwootConfig.getInboxId());

        // 创建消息 Payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("content", content);
        payload.put("sender", Map.of("identifier", userId));

        // 设置请求头
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + chatwootConfig.getApiToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 构建请求实体
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        // 发送请求并处理响应
        try {
            // 记录请求 URL 和 Payload
            log.info("Sending message to Chatwoot: URL={} Payload={}", url, payload);

            // 发送 POST 请求
            restTemplate.postForObject(url, request, String.class);

            // 记录请求成功
            log.info("Message successfully forwarded to Chatwoot.");
        } catch (Exception e) {
            // 记录错误日志
            log.error("Error forwarding message to Chatwoot: {}", e.getMessage());
            log.error("Request URL: {}", url);
            log.error("Payload: {}", payload);
        }
    }
}
