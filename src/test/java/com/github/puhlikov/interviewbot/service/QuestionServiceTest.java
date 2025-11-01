package com.github.puhlikov.interviewbot.service;

import com.github.puhlikov.interviewbot.model.Question;
import com.github.puhlikov.interviewbot.repo.QuestionRepository;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class QuestionServiceTest {

    @Mock
    private QuestionRepository repository;

    @InjectMocks
    private QuestionService questionService;

    private Question testQuestion;

    @BeforeEach
    void setUp() {
        testQuestion = new Question();
        testQuestion.setId(1L);
        testQuestion.setQuestionText("What is Java?");
        testQuestion.setCategory("Java");
        testQuestion.setDifficultyLevel("Junior");
        testQuestion.setIsActive(true);
        testQuestion.setCreatedAt(Instant.now());
    }

    @Test
    void testGetRandomQuestions_Success() {
        // Arrange
        List<Question> expectedQuestions = Arrays.asList(testQuestion);
        when(repository.findRandomActive(anyInt())).thenReturn(expectedQuestions);

        // Act
        List<Question> result = questionService.getRandomQuestions(5);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("What is Java?", result.get(0).getQuestionText());
        verify(repository, times(1)).findRandomActive(5);
    }

    @Test
    void testGetRandomQuestions_EmptyList() {
        // Arrange
        when(repository.findRandomActive(anyInt())).thenReturn(Collections.emptyList());

        // Act
        List<Question> result = questionService.getRandomQuestions(10);

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
        verify(repository, times(1)).findRandomActive(10);
    }

    @Test
    void testGetById_Success() {
        // Arrange
        when(repository.findById(1L)).thenReturn(Optional.of(testQuestion));

        // Act
        Optional<Question> result = questionService.getById(1L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
        assertEquals("What is Java?", result.get().getQuestionText());
        verify(repository, times(1)).findById(1L);
    }

    @Test
    void testGetById_NotFound() {
        // Arrange
        when(repository.findById(999L)).thenReturn(Optional.empty());

        // Act
        Optional<Question> result = questionService.getById(999L);

        // Assert
        assertFalse(result.isPresent());
        verify(repository, times(1)).findById(999L);
    }

    @Test
    void testSave_Success() {
        // Arrange
        when(repository.save(any(Question.class))).thenReturn(testQuestion);

        // Act
        questionService.save(testQuestion);

        // Assert
        verify(repository, times(1)).save(testQuestion);
    }

    @Test
    void testSave_NewQuestion() {
        // Arrange
        Question newQuestion = new Question();
        newQuestion.setQuestionText("New question?");
        newQuestion.setCategory("SQL");
        when(repository.save(any(Question.class))).thenReturn(newQuestion);

        // Act
        questionService.save(newQuestion);

        // Assert
        verify(repository, times(1)).save(newQuestion);
    }
}

