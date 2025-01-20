package wechat.entity;

import lombok.Data;

@Data
public class MessagePayload {
    private String content;
    private Sender sender;

    @Data
    public static class Sender {
        private String identifier;
    }
}
