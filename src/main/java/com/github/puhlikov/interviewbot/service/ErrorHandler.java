package com.github.puhlikov.interviewbot.service;

import com.github.puhlikov.interviewbot.bot.constants.Messages;
import com.github.puhlikov.interviewbot.exception.BotException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Централизованный обработчик ошибок для бота
 */
@Service
public class ErrorHandler {
    
    private static final Logger logger = LoggerFactory.getLogger(ErrorHandler.class);
    private final MessageSender messageSender;
    
    public ErrorHandler(MessageSender messageSender) {
        this.messageSender = messageSender;
    }
    
    /**
     * Обрабатывает ошибку и отправляет сообщение пользователю
     * @param chatId ID чата пользователя
     * @param error Ошибка для обработки
     */
    public void handleError(Long chatId, Throwable error) {
        if (error instanceof BotException) {
            BotException botException = (BotException) error;
            messageSender.sendMessage(chatId, botException.getUserMessage());
            logger.warn("Bot exception for chatId {}: {}", chatId, botException.getMessage());
        } else if (error instanceof IllegalArgumentException) {
            String errorMessage = error.getMessage() != null 
                ? "❌ " + error.getMessage() 
                : Messages.INVALID_QUESTIONS_COUNT;
            messageSender.sendMessage(chatId, errorMessage);
            logger.warn("Invalid input for chatId {}: {}", chatId, error.getMessage());
        } else if (error instanceof NumberFormatException) {
            messageSender.sendMessage(chatId, String.format(Messages.INVALID_QUESTIONS_COUNT, 
                "Неверный формат числа"));
            logger.warn("Number format error for chatId {}: {}", chatId, error.getMessage());
        } else {
            messageSender.sendMessage(chatId, Messages.ERROR_OCCURRED);
            logger.error("Unexpected error for chatId: {}", chatId, error);
        }
    }
    
    /**
     * Обрабатывает ошибку без отправки сообщения пользователю (для внутренних ошибок)
     * @param error Ошибка для обработки
     */
    public void handleErrorSilently(Throwable error) {
        logger.error("Silent error", error);
    }
    
    /**
     * Обрабатывает ошибку из реактивного потока
     * @param chatId ID чата пользователя
     * @param error Ошибка для обработки
     */
    public void handleReactiveError(Long chatId, Throwable error) {
        handleError(chatId, error);
    }
    
    /**
     * Обрабатывает ошибку с кастомным сообщением
     * @param chatId ID чата пользователя
     * @param error Ошибка для обработки
     * @param customMessage Кастомное сообщение для пользователя
     */
    public void handleErrorWithMessage(Long chatId, Throwable error, String customMessage) {
        messageSender.sendMessage(chatId, customMessage);
        logger.error("Error for chatId {} with custom message: {}", chatId, error.getMessage(), error);
    }
}

