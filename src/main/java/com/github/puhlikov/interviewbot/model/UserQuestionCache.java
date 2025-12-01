package com.github.puhlikov.interviewbot.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class UserQuestionCache {
    private Long chatId;
    private List<Question> questions;
    private int currentIndex;
    private int questionsPerSession;
    private String sessionId; // Уникальный идентификатор сессии
    private List<Integer> scores; // Список оценок для текущей сессии

    public UserQuestionCache(Long chatId, List<Question> questions, int questionsPerSession) {
        this.chatId = chatId;
        this.questions = questions;
        this.currentIndex = 0;
        this.questionsPerSession = questionsPerSession;
        this.sessionId = UUID.randomUUID().toString(); // Генерируем уникальный ID сессии
        this.scores = new ArrayList<>();
    }

    public boolean hasNext() {
        // Проверяем, есть ли еще вопросы после текущего
        // Если currentIndex уже на последнем вопросе (questions.size() - 1), то следующего нет
        return currentIndex < questions.size() - 1;
    }
    
    public boolean isLastQuestion() {
        // Проверяем, является ли текущий вопрос последним
        return currentIndex == questions.size() - 1;
    }
    
    public int getTotalQuestions() {
        return questions != null ? questions.size() : 0;
    }

    public Question getCurrentQuestion() {
        if (currentIndex < questions.size()) {
            return questions.get(currentIndex);
        }
        return null;
    }

    public Question getNextQuestion() {
        currentIndex++;
        return getCurrentQuestion();
    }

    public void addScore(Integer score) {
        this.scores.add(score);
    }

    public double getAverageScore() {
        if (scores.isEmpty()) {
            return 0.0;
        }
        return scores.stream().mapToInt(Integer::intValue).average().orElse(0.0);
    }
}
