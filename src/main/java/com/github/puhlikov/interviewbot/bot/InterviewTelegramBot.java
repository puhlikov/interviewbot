package com.github.puhlikov.interviewbot.bot;

import com.github.puhlikov.interviewbot.model.BotUser;
import com.github.puhlikov.interviewbot.model.Question;
import com.github.puhlikov.interviewbot.repo.BotUserRepository;
import com.github.puhlikov.interviewbot.service.DeepSeekService;
import com.github.puhlikov.interviewbot.service.QuestionService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

@Component
public class InterviewTelegramBot extends TelegramLongPollingBot {

	private final String username;
	private final BotUserRepository users;
	private final QuestionService questions;
	private final DeepSeekService deepseek;

	public InterviewTelegramBot(
		@Value("${telegram.bot.username}") String username,
		@Value("${telegram.bot.token}") String token,
		BotUserRepository users,
		QuestionService questions,
		DeepSeekService deepseek
	) {
		super(token);
		this.username = username;
		this.users = users;
		this.questions = questions;
		this.deepseek = deepseek;
	}

	@Override
	public String getBotUsername() {
		return username;
	}

	@Override
	public void onUpdateReceived(Update update) {
		try {
			if (update.hasMessage() && update.getMessage().hasText()) {
				var msg = update.getMessage();
				var chatId = msg.getChatId();
				var text = msg.getText().trim();
				ensureUser(chatId, msg.getFrom());

				if ("/start".equalsIgnoreCase(text)) {
					execSend(chatId, "Привет! Я пришлю каждый день в 14:00 подборку из 20 вопросов. Команда: /question — получить сейчас.");
				} else if ("/question".equalsIgnoreCase(text)) {
					sendQuestionBatch(chatId);
				}
			} else if (update.hasCallbackQuery()) {
				handleCallback(update.getCallbackQuery());
			}
		} catch (Exception ignored) {}
	}

	private void ensureUser(Long chatId, User from) {
		users.findByChatId(chatId).orElseGet(() -> {
			var u = new BotUser();
			u.setChatId(chatId);
			u.setUsername(from != null ? from.getUserName() : null);
			u.setFirstName(from != null ? from.getFirstName() : null);
			u.setLastName(from != null ? from.getLastName() : null);
			return users.save(u);
		});
	}

	public void sendQuestionBatch(Long chatId) {
		var list = questions.getRandomQuestions(20);
		execSend(chatId, "Подборка из 20 вопросов. Нажимайте «Показать ответ» под нужным вопросом.");
		for (Question q : list) {
			var kb = new InlineKeyboardMarkup();
			List<List<InlineKeyboardButton>> rows = new ArrayList<>();
			var btn = InlineKeyboardButton.builder()
				.text("Показать ответ")
				.callbackData("ANS:" + q.getId())
				.build();
			rows.add(List.of(btn));
			kb.setKeyboard(rows);
			execSend(chatId, q.getQuestionText(), kb);
		}
	}

	private void handleCallback(CallbackQuery cq) {
		var data = cq.getData();
		if (data != null && data.startsWith("ANS:")) {
			var qid = Long.parseLong(data.substring(4));
			questions.getById(qid).ifPresent(q ->
				deepseek.getAnswer(q.getQuestionText()).subscribe(answer -> {
					try {
						execSend(cq.getMessage().getChatId(), answer);
						execute(AnswerCallbackQuery.builder().callbackQueryId(cq.getId()).build());
					} catch (Exception ignored) {}
				})
			);
		}
	}

	private void execSend(Long chatId, String text) {
		execSend(chatId, text, null);
	}

	private void execSend(Long chatId, String text, InlineKeyboardMarkup kb) {
		try {
			var sm = SendMessage.builder()
				.chatId(chatId.toString())
				.text(text)
				.replyMarkup(kb)
				.build();
			execute(sm);
		} catch (Exception ignored) {}
	}
}


