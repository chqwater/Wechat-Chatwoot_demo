package wechat.controller;
// WeChatController.java

import wechat.service.WeChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/wechat")
@RequiredArgsConstructor
public class WeChatController {

    private final WeChatService weChatService;

    @GetMapping
    public ResponseEntity<String> verifyWeChat(@RequestParam String signature,
                                               @RequestParam String timestamp,
                                               @RequestParam String nonce,
                                               @RequestParam String echostr) {
        return ResponseEntity.ok(echostr);
    }

    @PostMapping
    public ResponseEntity<String> receiveMessage(@RequestBody String xmlData) {
        weChatService.processIncomingMessage(xmlData);
        return ResponseEntity.ok("success");
    }
}
