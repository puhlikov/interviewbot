package com.github.puhlikov.interviewbot.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
public class DeepSeekService {

	private final WebClient client;

	public DeepSeekService(
		@Value("${deepseek.api.url}") String baseUrl,
		@Value("${deepseek.api.key}") String apiKey
	) {
		this.client = WebClient.builder()
			.baseUrl(baseUrl)
			.defaultHeader("Authorization", "Bearer " + apiKey)
			.build();
	}

	public Mono<String> getAnswer(String questionText) {
		var body = Map.of(
			"model", "deepseek-chat",
			"messages", new Object[]{
				Map.of("role", "system", "content", "Ты опытный интервьюер. Отвечай кратко и по делу."),
				Map.of("role", "user", "content", "Ответь на вопрос: " + questionText)
			},
			"temperature", 0.2
		);

		return client.post()
			.contentType(MediaType.APPLICATION_JSON)
			.bodyValue(body)
			.retrieve()
			.bodyToMono(Map.class)
			.map(resp -> {
				try {
					var choices = (java.util.List<Map<String, Object>>) resp.get("choices");
					var msg = (Map<String, Object>) choices.get(0).get("message");
					return String.valueOf(msg.get("content"));
				} catch (Exception e) {
					return "Не удалось разобрать ответ DeepSeek.";
				}
			})
			.onErrorReturn("Ошибка запроса к DeepSeek.");
	}
}


