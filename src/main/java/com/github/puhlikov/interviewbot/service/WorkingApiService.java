package com.github.puhlikov.interviewbot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
public class WorkingApiService {

    private final WebClient client;

    public WorkingApiService() {
        this.client = WebClient.builder()
                .baseUrl("https://chat.gpt-chatbot.ru")
                .defaultHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:144.0) Gecko/20100101 Firefox/144.0")
                .defaultHeader("Accept", "application/json, text/event-stream")
                .defaultHeader("Accept-Language", "ru-RU,ru;q=0.8,en-US;q=0.5,en;q=0.3")
                .defaultHeader("Accept-Encoding", "gzip, deflate, br, zstd")
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Referer", "https://chat.gpt-chatbot.ru/")
                .defaultHeader("Origin", "https://chat.gpt-chatbot.ru")
                .defaultHeader("Sec-Fetch-Dest", "empty")
                .defaultHeader("Sec-Fetch-Mode", "cors")
                .defaultHeader("Sec-Fetch-Site", "same-origin")
                .defaultHeader("Connection", "keep-alive")
                .defaultHeader("Priority", "u=0")
                .defaultHeader("TE", "trailers")
                .build();
    }

    public Mono<String> getAnswer(String questionText) {
        System.out.println("🤖 Sending request to GPT-Chatbot API for question: " +
                (questionText.length() > 50 ? questionText.substring(0, 50) + "..." : questionText));

        var requestBody = Map.of(
                "messages", List.of(
                        Map.of(
                                "role", "user",
                                "content", questionText
                        )
                ),
                "stream", false,
                "model", "chatgpt-4o-latest",
                "temperature", 0.5,
                "presence_penalty", 0,
                "frequency_penalty", 0,
                "top_p", 1
        );

        return client.post()
                .uri("/api/openai/v1/chat/completions")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .map(rawResponse -> {
                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        Map<String, Object> response = mapper.readValue(rawResponse, Map.class);

                        var choices = (java.util.List<Map<String, Object>>) response.get("choices");
                        if (choices != null && !choices.isEmpty()) {
                            var msg = (Map<String, Object>) choices.get(0).get("message");
                            return String.valueOf(msg.get("content"));
                        } else {
                            System.err.println("❌ No choices in response: " + rawResponse);
                            return "❌ Не удалось получить ответ от API. Ответ пуст.";
                        }
                    } catch (Exception e) {
                        System.err.println("❌ Error parsing API response: " + e.getMessage());
                        System.err.println("Raw response: " + rawResponse);
                        return "❌ Ошибка при обработке ответа от AI: " + e.getMessage();
                    }
                })
                .onErrorResume(error -> {
                    System.err.println("💥 API request failed: " + error.getMessage());
                    error.printStackTrace();
                    return Mono.just("❌ Ошибка сети при запросе к AI: " + error.getMessage());
                });
    }
}
