package com.github.puhlikov.interviewbot.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class UserQuestionCache {
    private Long chatId;
    private List<Question> questions;
    private int currentIndex;
    private int questionsPerSession;

    public UserQuestionCache(Long chatId, List<Question> questions, int questionsPerSession) {
        this.chatId = chatId;
        this.questions = questions;
        this.currentIndex = 0;
        this.questionsPerSession = questionsPerSession;
    }

    public boolean hasNext() {
        return currentIndex < questions.size() - 1;
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
}
