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

    public void forwardMessageToChatwoot(String userId, String content) {
        String url = String.format("%s/accounts/%s/inboxes/%d/messages",
                chatwootConfig.getApiUrl(),
                "8",  // 这里需要替换为实际的 accountId
                chatwootConfig.getInboxId());

        Map<String, Object> payload = new HashMap<>();
        payload.put("content", content);
        payload.put("sender", Map.of("identifier", userId));

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + chatwootConfig.getApiToken());
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

        try {
            restTemplate.postForObject(url, request, String.class);
        } catch (Exception e) {
            log.error("Error forwarding message to Chatwoot: {}", e.getMessage());
        }
    }
}

