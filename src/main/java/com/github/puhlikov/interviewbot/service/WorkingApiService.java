package com.github.puhlikov.interviewbot.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class WorkingApiService {

    private final WebClient client;

    public WorkingApiService() {
        this.client = WebClient.builder()
                .baseUrl("https://chat.gpt-chatbot.ru")
                .defaultHeader("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 YaBrowser/25.8.0.0 Safari/537.36")
                .defaultHeader("Accept", "application/json, text/event-stream")
                .defaultHeader("Accept-Language", "ru,en;q=0.9,la;q=0.8,sr;q=0.7,bg;q=0.6")
                .defaultHeader("Content-Type", "application/json")
                .defaultHeader("Referer", "https://chat.gpt-chatbot.ru/")
                .defaultHeader("Origin", "https://chat.gpt-chatbot.ru")
                .defaultHeader("Priority", "u=1, i")
                .defaultHeader("Sec-Fetch-Dest", "empty")
                .defaultHeader("Sec-Fetch-Mode", "cors")
                .defaultHeader("Sec-Fetch-Site", "same-origin")
                .defaultHeader("Sec-CH-UA", "\"Not)A;Brand\";v=\"8\", \"Chromium\";v=\"138\", \"YaBrowser\";v=\"25.8\", \"Yowser\";v=\"2.5\"")
                .defaultHeader("Sec-CH-UA-Mobile", "?0")
                .defaultHeader("Sec-CH-UA-Platform", "\"Linux\"")
                .build();
    }

    public Mono<String> getAnswer(String questionText) {
        System.out.println("🤖 Sending request to GPT-Chatbot API for question: " +
                (questionText.length() > 50 ? questionText.substring(0, 50) + "..." : questionText));

        // Используем HashMap для поддержки max_tokens
        var requestBody = new HashMap<String, Object>();
        requestBody.put("messages", List.of(
                Map.of(
                        "role", "user",
                        "content", questionText
                )
        ));
        requestBody.put("stream", true); // true для получения stream ответа
        requestBody.put("model", "gpt-4.1-mini");
        requestBody.put("temperature", 0.5);
        requestBody.put("presence_penalty", 0);
        requestBody.put("frequency_penalty", 0);
        requestBody.put("top_p", 1);
        requestBody.put("max_tokens", 4000);

        ObjectMapper mapper = new ObjectMapper();

        return client.post()
                .uri("/api/openai/v1/chat/completions")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToFlux(String.class)
                .filter(line -> line != null && !line.trim().isEmpty())
                .map(line -> {
                    // Удаляем ANSI escape-коды
                    return line.replaceAll("\u001B\\[[;\\d]*m", "");
                })
                .filter(line -> line.startsWith("data: "))
                .map(line -> {
                    // Убираем префикс "data: "
                    String jsonData = line.substring(6).trim();
                    
                    // Пропускаем строку "[DONE]"
                    if ("[DONE]".equals(jsonData)) {
                        return null;
                    }
                    
                    try {
                        Map<String, Object> chunk = mapper.readValue(jsonData, Map.class);
                        
                        // Проверяем на ошибку
                        if (chunk.containsKey("error")) {
                            Map<String, Object> error = (Map<String, Object>) chunk.get("error");
                            String errorMsg = String.valueOf(error.get("message"));
                            System.err.println("❌ API returned error: " + errorMsg);
                            throw new RuntimeException("API Error: " + errorMsg);
                        }
                        
                        // Извлекаем content из delta
                        var choices = (java.util.List<Map<String, Object>>) chunk.get("choices");
                        if (choices != null && !choices.isEmpty()) {
                            var choice = choices.get(0);
                            var delta = (Map<String, Object>) choice.get("delta");
                            if (delta != null && delta.containsKey("content")) {
                                return String.valueOf(delta.get("content"));
                            }
                        }
                        return null;
                    } catch (RuntimeException e) {
                        // Пробрасываем ошибки API дальше
                        throw e;
                    } catch (Exception e) {
                        System.err.println("❌ Error parsing stream chunk: " + e.getMessage());
                        System.err.println("Chunk: " + jsonData);
                        return null;
                    }
                })
                .filter(content -> content != null)
                .collectList()
                .map(chunks -> {
                    // Объединяем все части в одну строку
                    StringBuilder fullContent = new StringBuilder();
                    for (String chunk : chunks) {
                        fullContent.append(chunk);
                    }
                    String result = fullContent.toString();
                    if (result.isEmpty()) {
                        System.err.println("❌ Empty response from stream");
                        return "❌ Не удалось получить ответ от API. Ответ пуст.";
                    }
                    System.out.println("✅ Successfully received stream answer from API (length: " + result.length() + ")");
                    return result;
                })
                .onErrorResume(error -> {
                    System.err.println("💥 API request failed: " + error.getMessage());
                    error.printStackTrace();
                    return Mono.just("❌ Ошибка сети при запросе к AI: " + error.getMessage());
                });
    }
}
