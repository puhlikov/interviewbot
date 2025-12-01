package com.github.puhlikov.interviewbot.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;

/**
 * Сервис для отправки сообщений в Telegram
 */
@Service
public class MessageSender {
    
    private static final Logger logger = LoggerFactory.getLogger(MessageSender.class);
    private TelegramLongPollingBot bot;
    
    public MessageSender() {
        // Constructor for Spring
    }
    
    public void setBot(TelegramLongPollingBot bot) {
        this.bot = bot;
    }
    
    public void sendMessage(Long chatId, String text) {
        sendMessage(chatId, text, null);
    }
    
    public void sendMessage(Long chatId, String text, ReplyKeyboard replyMarkup) {
        try {
            SendMessage message = SendMessage.builder()
                    .chatId(chatId.toString())
                    .text(text)
                    .replyMarkup(replyMarkup)
                    .build();
            bot.execute(message);
        } catch (Exception e) {
            logger.error("Failed to send message to chatId: {}", chatId, e);
        }
    }
}

