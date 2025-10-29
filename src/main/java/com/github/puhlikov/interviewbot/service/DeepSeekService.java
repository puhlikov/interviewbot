//package com.github.puhlikov.interviewbot.service;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.http.MediaType;
//import org.springframework.stereotype.Service;
//import org.springframework.web.reactive.function.client.WebClient;
//import reactor.core.publisher.Mono;
//
//import java.util.List;
//import java.util.Map;
//
//@Service
//public class DeepSeekService {
//
//	private final WebClient client;
//	private final String apiKey;
//
//	public DeepSeekService(
//			@Value("${openrouter.api.key}") String apiKey
//	) {
//		this.apiKey = apiKey;
//
//		System.out.println("🔧 OpenRouter Service initialized:");
//		System.out.println("   URL: https://openrouter.ai/api/v1");
//		System.out.println("   API Key: " + (apiKey != null && !apiKey.isEmpty() ?
//				apiKey.substring(0, Math.min(5, apiKey.length())) + "..." : "NOT SET"));
//
//		this.client = WebClient.builder()
//				.baseUrl("https://openrouter.ai/api/v1")
//				.defaultHeader("Authorization", "Bearer " + apiKey)
//				.defaultHeader("Content-Type", "application/json")
//				.defaultHeader("HTTP-Referer", "https://github.com/puhlikov/interviewbot") // Рекомендуется
//				.defaultHeader("X-Title", "InterviewBot") // Рекомендуется
//				.build();
//	}
//
//	public Mono<String> getAnswer(String questionText) {
//		System.out.println("🤖 Sending request to OpenRouter for question: " +
//				(questionText.length() > 50 ? questionText.substring(0, 50) + "..." : questionText));
//
//		// Используем бесплатную модель DeepSeek через OpenRouter
//		var body = Map.of(
//				"model", "deepseek/deepseek-chat:free", // Бесплатная модель
//				"messages", List.of(
//						Map.of("role", "user", "content", questionText)
//				),
//				"temperature", 0.7,
//				"max_tokens", 1000,
//				"stream", false
//		);
//
//		return client.post()
//				.uri("/chat/completions")
//				.bodyValue(body)
//				.retrieve()
//				.bodyToMono(Map.class)
//				.map(resp -> {
//					try {
//						System.out.println("✅ Received response from OpenRouter");
//						var choices = (java.util.List<Map<String, Object>>) resp.get("choices");
//						if (choices != null && !choices.isEmpty()) {
//							var msg = (Map<String, Object>) choices.get(0).get("message");
//							String answer = String.valueOf(msg.get("content"));
//							System.out.println("📝 Answer length: " + answer.length());
//							return answer;
//						} else {
//							return "❌ Не удалось получить ответ от OpenRouter.";
//						}
//					} catch (Exception e) {
//						System.err.println("❌ Error parsing OpenRouter response: " + e.getMessage());
//						System.err.println("Response: " + resp);
//						return "❌ Ошибка при обработке ответа от AI.";
//					}
//				})
//				.onErrorReturn("❌ Ошибка при запросе к AI сервису. Пожалуйста, попробуйте позже.")
//				.doOnError(error -> {
//					System.err.println("💥 OpenRouter API error: " + error.getMessage());
//					error.printStackTrace();
//				});
//	}
//}