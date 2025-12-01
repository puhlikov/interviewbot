package com.github.puhlikov.interviewbot.service;

import com.github.puhlikov.interviewbot.model.Question;
import com.github.puhlikov.interviewbot.model.UserQuestionCache;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class QuestionCacheService {

    private final Map<Long, UserQuestionCache> userCache = new ConcurrentHashMap<>();
    private final QuestionService questionService;

    public QuestionCacheService(QuestionService questionService) {
        this.questionService = questionService;
    }

    public void initializeUserCache(Long chatId, int questionsCount) {
        var questions = questionService.getRandomQuestions(questionsCount);
        var cache = new UserQuestionCache(chatId, questions, questionsCount);
        userCache.put(chatId, cache);
    }

    public UserQuestionCache getUserCache(Long chatId) {
        return userCache.get(chatId);
    }

    public Question getCurrentQuestion(Long chatId) {
        var cache = userCache.get(chatId);
        return cache != null ? cache.getCurrentQuestion() : null;
    }

    public Question getNextQuestion(Long chatId) {
        var cache = userCache.get(chatId);
        return cache != null ? cache.getNextQuestion() : null;
    }

    public boolean hasNextQuestion(Long chatId) {
        var cache = userCache.get(chatId);
        return cache != null && cache.hasNext();
    }
    
    public boolean isLastQuestion(Long chatId) {
        var cache = userCache.get(chatId);
        return cache != null && cache.isLastQuestion();
    }
    
    public int getTotalQuestions(Long chatId) {
        var cache = userCache.get(chatId);
        return cache != null ? cache.getTotalQuestions() : 0;
    }

    public void clearUserCache(Long chatId) {
        userCache.remove(chatId);
    }

    public boolean isUserInSession(Long chatId) {
        return userCache.containsKey(chatId);
    }
}
