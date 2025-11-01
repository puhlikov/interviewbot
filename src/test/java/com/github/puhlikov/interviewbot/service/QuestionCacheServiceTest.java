package com.github.puhlikov.interviewbot.service;

import com.github.puhlikov.interviewbot.model.Question;
import com.github.puhlikov.interviewbot.model.UserQuestionCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuestionCacheServiceTest {

    @Mock
    private QuestionService questionService;

    @InjectMocks
    private QuestionCacheService questionCacheService;

    private Question question1;
    private Question question2;
    private Question question3;
    private static final Long TEST_CHAT_ID = 12345L;

    @BeforeEach
    void setUp() {
        question1 = createQuestion(1L, "Question 1");
        question2 = createQuestion(2L, "Question 2");
        question3 = createQuestion(3L, "Question 3");
    }

    private Question createQuestion(Long id, String text) {
        Question q = new Question();
        q.setId(id);
        q.setQuestionText(text);
        q.setCategory("Java");
        q.setDifficultyLevel("Junior");
        q.setIsActive(true);
        q.setCreatedAt(Instant.now());
        return q;
    }

    @Test
    void testInitializeUserCache_Success() {
        // Arrange
        List<Question> questions = Arrays.asList(question1, question2, question3);
        when(questionService.getRandomQuestions(3)).thenReturn(questions);

        // Act
        questionCacheService.initializeUserCache(TEST_CHAT_ID, 3);

        // Assert
        UserQuestionCache cache = questionCacheService.getUserCache(TEST_CHAT_ID);
        assertNotNull(cache);
        assertEquals(3, cache.getQuestions().size());
        assertTrue(questionCacheService.isUserInSession(TEST_CHAT_ID));
        verify(questionService, times(1)).getRandomQuestions(3);
    }

    @Test
    void testInitializeUserCache_EmptyQuestions() {
        // Arrange
        when(questionService.getRandomQuestions(5)).thenReturn(Collections.emptyList());

        // Act
        questionCacheService.initializeUserCache(TEST_CHAT_ID, 5);

        // Assert
        UserQuestionCache cache = questionCacheService.getUserCache(TEST_CHAT_ID);
        assertNotNull(cache);
        assertTrue(cache.getQuestions().isEmpty());
    }

    @Test
    void testGetCurrentQuestion_Success() {
        // Arrange
        List<Question> questions = Arrays.asList(question1, question2, question3);
        when(questionService.getRandomQuestions(3)).thenReturn(questions);
        questionCacheService.initializeUserCache(TEST_CHAT_ID, 3);

        // Act
        Question current = questionCacheService.getCurrentQuestion(TEST_CHAT_ID);

        // Assert
        assertNotNull(current);
        assertEquals(question1.getId(), current.getId());
        assertEquals("Question 1", current.getQuestionText());
    }

    @Test
    void testGetCurrentQuestion_NoCache() {
        // Act
        Question current = questionCacheService.getCurrentQuestion(TEST_CHAT_ID);

        // Assert
        assertNull(current);
    }

    @Test
    void testGetNextQuestion_Success() {
        // Arrange
        List<Question> questions = Arrays.asList(question1, question2, question3);
        when(questionService.getRandomQuestions(3)).thenReturn(questions);
        questionCacheService.initializeUserCache(TEST_CHAT_ID, 3);

        // Act
        Question next = questionCacheService.getNextQuestion(TEST_CHAT_ID);

        // Assert
        assertNotNull(next);
        assertEquals(question2.getId(), next.getId());
    }

    @Test
    void testGetNextQuestion_NoCache() {
        // Act
        Question next = questionCacheService.getNextQuestion(TEST_CHAT_ID);

        // Assert
        assertNull(next);
    }

    @Test
    void testHasNextQuestion_True() {
        // Arrange
        List<Question> questions = Arrays.asList(question1, question2, question3);
        when(questionService.getRandomQuestions(3)).thenReturn(questions);
        questionCacheService.initializeUserCache(TEST_CHAT_ID, 3);

        // Act
        boolean hasNext = questionCacheService.hasNextQuestion(TEST_CHAT_ID);

        // Assert
        assertTrue(hasNext);
    }

    @Test
    void testHasNextQuestion_False() {
        // Arrange
        List<Question> questions = Collections.singletonList(question1);
        when(questionService.getRandomQuestions(1)).thenReturn(questions);
        questionCacheService.initializeUserCache(TEST_CHAT_ID, 1);

        // Act - get current question to move index
        questionCacheService.getCurrentQuestion(TEST_CHAT_ID);
        questionCacheService.getNextQuestion(TEST_CHAT_ID);

        // Assert
        assertFalse(questionCacheService.hasNextQuestion(TEST_CHAT_ID));
    }

    @Test
    void testHasNextQuestion_NoCache() {
        // Act
        boolean hasNext = questionCacheService.hasNextQuestion(TEST_CHAT_ID);

        // Assert
        assertFalse(hasNext);
    }

    @Test
    void testClearUserCache_Success() {
        // Arrange
        List<Question> questions = Arrays.asList(question1, question2);
        when(questionService.getRandomQuestions(2)).thenReturn(questions);
        questionCacheService.initializeUserCache(TEST_CHAT_ID, 2);
        assertTrue(questionCacheService.isUserInSession(TEST_CHAT_ID));

        // Act
        questionCacheService.clearUserCache(TEST_CHAT_ID);

        // Assert
        assertNull(questionCacheService.getUserCache(TEST_CHAT_ID));
        assertFalse(questionCacheService.isUserInSession(TEST_CHAT_ID));
    }

    @Test
    void testIsUserInSession_True() {
        // Arrange
        List<Question> questions = Collections.singletonList(question1);
        when(questionService.getRandomQuestions(1)).thenReturn(questions);
        questionCacheService.initializeUserCache(TEST_CHAT_ID, 1);

        // Act & Assert
        assertTrue(questionCacheService.isUserInSession(TEST_CHAT_ID));
    }

    @Test
    void testIsUserInSession_False() {
        // Act & Assert
        assertFalse(questionCacheService.isUserInSession(TEST_CHAT_ID));
    }

    @Test
    void testMultipleUsersSessions() {
        // Arrange
        Long chatId1 = 111L;
        Long chatId2 = 222L;
        List<Question> questions1 = Arrays.asList(question1, question2);
        List<Question> questions2 = Collections.singletonList(question3);
        
        when(questionService.getRandomQuestions(2)).thenReturn(questions1);
        when(questionService.getRandomQuestions(1)).thenReturn(questions2);

        // Act
        questionCacheService.initializeUserCache(chatId1, 2);
        questionCacheService.initializeUserCache(chatId2, 1);

        // Assert
        assertTrue(questionCacheService.isUserInSession(chatId1));
        assertTrue(questionCacheService.isUserInSession(chatId2));
        assertEquals(2, questionCacheService.getUserCache(chatId1).getQuestions().size());
        assertEquals(1, questionCacheService.getUserCache(chatId2).getQuestions().size());

        // Clear one session
        questionCacheService.clearUserCache(chatId1);
        assertFalse(questionCacheService.isUserInSession(chatId1));
        assertTrue(questionCacheService.isUserInSession(chatId2));
    }
}

