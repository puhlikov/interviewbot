package com.github.puhlikov.interviewbot.model;

import com.github.puhlikov.interviewbot.enums.RegistrationState;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;
import java.time.LocalTime;

@Entity
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@AllArgsConstructor
@Table(name = "users")
public class BotUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chat_id", nullable = false, unique = true)
    private Long chatId;

    @Column(name = "username")
    private String username;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(name = "schedule_time")
    private LocalTime scheduleTime;

    @Column(name = "timezone")
    private String timezone = "Europe/Moscow";

    @Enumerated(EnumType.STRING)
    @Column(name = "registration_state")
    private RegistrationState registrationState = RegistrationState.START;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();
}