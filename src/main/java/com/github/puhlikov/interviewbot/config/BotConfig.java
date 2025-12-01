package com.github.puhlikov.interviewbot.config;

import com.github.puhlikov.interviewbot.bot.InterviewTelegramBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
public class BotConfig {

    private static final Logger logger = LoggerFactory.getLogger(BotConfig.class);

    @Bean
    public TelegramBotsApi telegramBotsApi(InterviewTelegramBot bot) {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(bot);
            logger.info("✅ Bot successfully registered with Telegram API!");
            logger.info("✅ Bot username: {}", bot.getBotUsername());
            return botsApi;
        } catch (TelegramApiException e) {
            logger.error("❌ Failed to register bot: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to register bot", e);
        }
    }
}
