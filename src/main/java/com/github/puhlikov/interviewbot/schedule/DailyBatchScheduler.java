package com.github.puhlikov.interviewbot.schedule;

import com.github.puhlikov.interviewbot.bot.InterviewTelegramBot;
import com.github.puhlikov.interviewbot.repo.BotUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class DailyBatchScheduler {

	private final BotUserRepository users;
	private final InterviewTelegramBot bot;

	public DailyBatchScheduler(BotUserRepository users, InterviewTelegramBot bot,
	                          @Value("${app.schedule.zone:Europe/Moscow}") String zone) {
		this.users = users;
		this.bot = bot;
	}

	@Scheduled(cron = "${app.schedule.cron:0 0 14 * * *}", zone = "${app.schedule.zone:Europe/Moscow}")
	public void sendDailyBatch() {
		users.findAll().forEach(u -> bot.sendQuestionBatch(u.getChatId()));
	}
}


