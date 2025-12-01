package com.github.puhlikov.interviewbot.exception;

/**
 * Исключение для невалидного ввода пользователя
 */
public class InvalidInputException extends BotException {
    public InvalidInputException(String message) {
        super("❌ " + message);
    }
}

