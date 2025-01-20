package wechat.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "wechat")
@Data
public class WeChatConfig {
    private String appId;
    private String token;
    private String encodingAesKey;

}

