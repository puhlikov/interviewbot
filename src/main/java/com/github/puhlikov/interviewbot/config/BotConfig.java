package com.github.puhlikov.interviewbot.config;

import com.github.puhlikov.interviewbot.bot.InterviewTelegramBot;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
public class BotConfig {

    @Bean
    public TelegramBotsApi telegramBotsApi(InterviewTelegramBot bot) {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(bot);
            System.out.println("✅ Bot successfully registered with Telegram API!");
            System.out.println("✅ Bot username: " + bot.getBotUsername());
            return botsApi;
        } catch (TelegramApiException e) {
            System.err.println("❌ Failed to register bot: " + e.getMessage());
            throw new RuntimeException("Failed to register bot", e);
        }
    }
}
