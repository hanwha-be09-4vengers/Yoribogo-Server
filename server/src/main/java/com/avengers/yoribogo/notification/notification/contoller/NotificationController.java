package com.avengers.yoribogo.notification.notification.contoller;

import com.avengers.yoribogo.notification.notification.service.NotificationService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    // SSE 연결 엔드포인트 (Authorization 헤더 전달)
    @GetMapping(value = "/notifications", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeToNotifications(@RequestHeader("Authorization") String authorizationHeader) {
        // Service로 Authorization 헤더 전달하여 처리
        return notificationService.subscribe(authorizationHeader);
    }
}
