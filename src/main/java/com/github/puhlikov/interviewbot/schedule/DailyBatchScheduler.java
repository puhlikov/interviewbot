package com.github.puhlikov.interviewbot.schedule;

import com.github.puhlikov.interviewbot.bot.InterviewTelegramBot;
import com.github.puhlikov.interviewbot.enums.RegistrationState;
import com.github.puhlikov.interviewbot.model.BotUser;
import com.github.puhlikov.interviewbot.repo.BotUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@Component
public class DailyBatchScheduler {

	private final BotUserRepository users;
	private final InterviewTelegramBot bot;

	public DailyBatchScheduler(BotUserRepository users, InterviewTelegramBot bot) {
		this.users = users;
		this.bot = bot;
	}

	@Scheduled(cron = "0 * * * * *")
	public void sendDailyNotifications() {
		List<BotUser> allUsers = users.findAll();

		for (BotUser user : allUsers) {
			if (user.getRegistrationState() == RegistrationState.COMPLETED &&
					user.getScheduleTime() != null) {

				try {
					ZoneId zone = ZoneId.of(user.getTimezone());
					LocalTime nowInUserZone = LocalTime.now(zone);

					if (nowInUserZone.getHour() == user.getScheduleTime().getHour() &&
							nowInUserZone.getMinute() == user.getScheduleTime().getMinute()) {

						bot.sendDailyNotification(user.getChatId());
					}
				} catch (Exception e) {
					System.err.println("Error sending notification to user " + user.getChatId() + ": " + e.getMessage());
				}
			}
		}
	}
}


