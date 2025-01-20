package wechat.service;

import wechat.config.WeChatConfig;
import wechat.utils.WeChatUtils;
import lombok.RequiredArgsConstructor;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class WeChatService {

    private final WeChatConfig weChatConfig;
    private final ChatwootService chatwootService;

    public boolean verifySignature(String signature, String timestamp, String nonce) {
        String[] arr = {weChatConfig.getToken(), timestamp, nonce};
        Arrays.sort(arr);
        String concatenated = String.join("", arr);
        String calculatedSignature = DigestUtils.sha1Hex(concatenated);
        return calculatedSignature.equals(signature);
    }

    public void processIncomingMessage(String xmlData) {
        Map<String, String> message = WeChatUtils.parseXml(xmlData);
        String userId = message.get("FromUserName");
        String content = message.get("Content");

        chatwootService.forwardMessageToChatwoot(userId, content);
    }
}
