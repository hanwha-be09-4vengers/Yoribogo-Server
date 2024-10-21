package com.avengers.yoribogo.notification.notification.dto;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification")
@Data
public class NotificationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long notificationId;

    @Column(name = "notification_content")
    private String notificationContent;

    @Column(name = "notification_read_status")
    @Enumerated(EnumType.STRING)
    private NotificationStatus notificationStatus;

    @Column(name = "notification_created_at")
    private LocalDateTime notificationCreatedAt;

    @Column(name = "notification_read_at")
    private LocalDateTime notificationReadAt;

    @Column(name = "user_id")
    private Long userId;
}