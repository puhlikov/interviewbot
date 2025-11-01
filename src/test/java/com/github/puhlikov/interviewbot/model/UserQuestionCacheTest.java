package com.github.puhlikov.interviewbot.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UserQuestionCacheTest {

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
    void testConstructor_WithQuestions() {
        // Arrange
        List<Question> questions = List.of(question1, question2, question3);

        // Act
        UserQuestionCache cache = new UserQuestionCache(TEST_CHAT_ID, questions, 3);

        // Assert
        assertEquals(TEST_CHAT_ID, cache.getChatId());
        assertEquals(3, cache.getQuestions().size());
        assertEquals(0, cache.getCurrentIndex());
        assertEquals(3, cache.getQuestionsPerSession());
    }

    @Test
    void testGetCurrentQuestion_FirstQuestion() {
        // Arrange
        List<Question> questions = List.of(question1, question2, question3);
        UserQuestionCache cache = new UserQuestionCache(TEST_CHAT_ID, questions, 3);

        // Act
        Question current = cache.getCurrentQuestion();

        // Assert
        assertNotNull(current);
        assertEquals(question1.getId(), current.getId());
        assertEquals("Question 1", current.getQuestionText());
    }

    @Test
    void testGetNextQuestion_MovesIndex() {
        // Arrange
        List<Question> questions = List.of(question1, question2, question3);
        UserQuestionCache cache = new UserQuestionCache(TEST_CHAT_ID, questions, 3);

        // Act
        Question next = cache.getNextQuestion();

        // Assert
        assertNotNull(next);
        assertEquals(question2.getId(), next.getId());
        assertEquals(1, cache.getCurrentIndex());
    }

    @Test
    void testGetNextQuestion_MultipleTimes() {
        // Arrange
        List<Question> questions = List.of(question1, question2, question3);
        UserQuestionCache cache = new UserQuestionCache(TEST_CHAT_ID, questions, 3);

        // Act
        Question next1 = cache.getNextQuestion();
        Question next2 = cache.getNextQuestion();

        // Assert
        assertEquals(question2.getId(), next1.getId());
        assertEquals(question3.getId(), next2.getId());
        assertEquals(2, cache.getCurrentIndex());
    }

    @Test
    void testGetNextQuestion_LastQuestion() {
        // Arrange
        List<Question> questions = List.of(question1);
        UserQuestionCache cache = new UserQuestionCache(TEST_CHAT_ID, questions, 1);

        // Act
        Question next = cache.getNextQuestion();

        // Assert
        assertNull(next);
        assertEquals(1, cache.getCurrentIndex());
    }

    @Test
    void testGetCurrentQuestion_AfterLast() {
        // Arrange
        List<Question> questions = List.of(question1);
        UserQuestionCache cache = new UserQuestionCache(TEST_CHAT_ID, questions, 1);
        cache.getNextQuestion(); // Move to index 1

        // Act
        Question current = cache.getCurrentQuestion();

        // Assert
        assertNull(current);
    }

    @Test
    void testHasNext_True() {
        // Arrange
        List<Question> questions = List.of(question1, question2, question3);
        UserQuestionCache cache = new UserQuestionCache(TEST_CHAT_ID, questions, 3);

        // Act & Assert
        assertTrue(cache.hasNext());
    }

    @Test
    void testHasNext_False_LastQuestion() {
        // Arrange
        List<Question> questions = List.of(question1, question2);
        UserQuestionCache cache = new UserQuestionCache(TEST_CHAT_ID, questions, 2);
        cache.getNextQuestion(); // Move to last question (index 1)

        // Act & Assert
        assertFalse(cache.hasNext());
    }

    @Test
    void testHasNext_False_SingleQuestion() {
        // Arrange
        List<Question> questions = List.of(question1);
        UserQuestionCache cache = new UserQuestionCache(TEST_CHAT_ID, questions, 1);

        // Act & Assert
        assertFalse(cache.hasNext());
    }

    @Test
    void testHasNext_EmptyList() {
        // Arrange
        List<Question> questions = Collections.emptyList();
        UserQuestionCache cache = new UserQuestionCache(TEST_CHAT_ID, questions, 0);

        // Act & Assert
        assertFalse(cache.hasNext());
    }

    @Test
    void testGettersAndSetters() {
        // Arrange
        List<Question> questions = List.of(question1);
        UserQuestionCache cache = new UserQuestionCache(TEST_CHAT_ID, questions, 1);

        // Act & Assert
        assertEquals(TEST_CHAT_ID, cache.getChatId());
        assertEquals(1, cache.getQuestions().size());
        assertEquals(0, cache.getCurrentIndex());
        assertEquals(1, cache.getQuestionsPerSession());

        // Test setters
        Long newChatId = 99999L;
        cache.setChatId(newChatId);
        cache.setCurrentIndex(5);
        cache.setQuestionsPerSession(10);

        assertEquals(newChatId, cache.getChatId());
        assertEquals(5, cache.getCurrentIndex());
        assertEquals(10, cache.getQuestionsPerSession());
    }
}

