package com.avengers.yoribogo.notification.notification.service;


import com.avengers.yoribogo.security.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class NotificationService {

    @Autowired
    private final JwtUtil jwtUtil;  // JwtUtil 주입
    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public NotificationService(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    // Authorization 헤더를 받아서 사용자별로 Emitter 관리
    public SseEmitter subscribe(String authorizationHeader) {
        // Authorization 헤더에서 "Bearer " 이후의 JWT 토큰 추출
        String token = authorizationHeader.replace("Bearer ", "");

        // JWT 토큰에서 userAuthId를 추출
        String userAuthId = jwtUtil.getUserId(token);

        // 새로운 SseEmitter 생성 및 사용자와 매핑
        SseEmitter emitter = new SseEmitter(0L);  // SSE 객체 유효 시간 설정 필요
        emitters.put(userAuthId, emitter);

        // 연결 완료, 타임아웃, 에러 발생 시 emitter 제거
        emitter.onCompletion(() -> emitters.remove(userAuthId));
        emitter.onTimeout(() -> emitters.remove(userAuthId));
        emitter.onError((ex) -> emitters.remove(userAuthId));

        return emitter;
    }

    // 특정 사용자에게 알림을 전송하는 메소드
    public void sendNotificationToLoggedInUsers(String message) {
        emitters.forEach((userAuthId, emitter) -> {
            try {
                emitter.send(SseEmitter.event().name("notification").data(message));
            } catch (Exception e) {
                log.error("알림 전송에 실패하였습니다. {}: {}", userAuthId, e.getMessage());
                emitters.remove(userAuthId);  // 실패 시 emitter 제거
            }
        });
    }

    // 사용자에게 보낸 알림을 DB에 저장하는 메소드
}
