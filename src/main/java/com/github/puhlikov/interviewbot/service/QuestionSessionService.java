package com.github.puhlikov.interviewbot.service;

import com.github.puhlikov.interviewbot.enums.QuestionState;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class QuestionSessionService {

    private final Map<Long, QuestionSession> userSessions = new ConcurrentHashMap<>();

    public void startSession(Long chatId) {
        userSessions.put(chatId, new QuestionSession(QuestionState.AWAITING_QUESTION_TEXT));
    }

    public void updateState(Long chatId, QuestionState state) {
        QuestionSession session = userSessions.get(chatId);
        if (session != null) {
            session.setState(state);
        }
    }

    public void setQuestionText(Long chatId, String text) {
        QuestionSession session = userSessions.get(chatId);
        if (session != null) {
            session.setQuestionText(text);
        }
    }

    public void setCategory(Long chatId, String category) {
        QuestionSession session = userSessions.get(chatId);
        if (session != null) {
            session.setCategory(category);
        }
    }

    public QuestionSession getSession(Long chatId) {
        return userSessions.get(chatId);
    }

    public void completeSession(Long chatId) {
        userSessions.remove(chatId);
    }

    public static class QuestionSession {
        private QuestionState state;
        private String questionText;
        private String category;

        public QuestionSession(QuestionState state) {
            this.state = state;
        }

        public QuestionState getState() {
            return state;
        }

        public void setState(QuestionState state) {
            this.state = state;
        }

        public String getQuestionText() {
            return questionText;
        }

        public void setQuestionText(String questionText) {
            this.questionText = questionText;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }
    }
}