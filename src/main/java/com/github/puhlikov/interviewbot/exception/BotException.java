package com.github.puhlikov.interviewbot.exception;

/**
 * Базовое исключение для бота с сообщением для пользователя
 */
public class BotException extends RuntimeException {
    private final String userMessage;
    
    public BotException(String userMessage, String technicalMessage) {
        super(technicalMessage);
        this.userMessage = userMessage;
    }
    
    public BotException(String userMessage) {
        this(userMessage, userMessage);
    }
    
    public String getUserMessage() {
        return userMessage;
    }
}

