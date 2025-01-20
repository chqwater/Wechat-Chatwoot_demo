package wechat.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "chatwoot")
@Data
public class ChatwootConfig {
    private String apiUrl;
    private String apiToken;
    private Long inboxId;

    // Lombok's @Data generates the getter methods
}

