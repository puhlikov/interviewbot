package com.github.puhlikov.interviewbot.bot.util;

import com.github.puhlikov.interviewbot.bot.constants.ButtonText;
import com.github.puhlikov.interviewbot.bot.constants.CallbackData;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class KeyboardBuilderTest {

    @Test
    void testCreateMainReplyKeyboard() {
        // Act
        ReplyKeyboardMarkup keyboard = KeyboardBuilder.createMainReplyKeyboard();

        // Assert
        assertNotNull(keyboard);
        assertTrue(keyboard.getResizeKeyboard());
        assertFalse(keyboard.getOneTimeKeyboard());
        assertTrue(keyboard.getSelective());
        assertNotNull(keyboard.getKeyboard());
        assertEquals(1, keyboard.getKeyboard().size());
        assertEquals(2, keyboard.getKeyboard().get(0).size());
        assertEquals(ButtonText.START_SESSION, keyboard.getKeyboard().get(0).get(0).getText());
        assertEquals(ButtonText.SETTINGS, keyboard.getKeyboard().get(0).get(1).getText());
    }

    @Test
    void testCreateQuestionKeyboard() {
        // Arrange
        Long questionId = 123L;

        // Act
        InlineKeyboardMarkup keyboard = KeyboardBuilder.createQuestionKeyboard(questionId);

        // Assert
        assertNotNull(keyboard);
        assertNotNull(keyboard.getKeyboard());
        assertEquals(2, keyboard.getKeyboard().size()); // Two rows
        
        // First row: Show Answer and Reply buttons
        List<InlineKeyboardButton> firstRow = keyboard.getKeyboard().get(0);
        assertEquals(2, firstRow.size());
        assertEquals(ButtonText.SHOW_ANSWER, firstRow.get(0).getText());
        assertEquals(CallbackData.answerCallback(questionId), firstRow.get(0).getCallbackData());
        assertEquals(ButtonText.REPLY, firstRow.get(1).getText());
        assertEquals(CallbackData.replyCallback(questionId), firstRow.get(1).getCallbackData());
        
        // Second row: Exit button
        List<InlineKeyboardButton> secondRow = keyboard.getKeyboard().get(1);
        assertEquals(1, secondRow.size());
        assertEquals(ButtonText.EXIT_SESSION, secondRow.get(0).getText());
        assertEquals(CallbackData.EXIT_SESSION, secondRow.get(0).getCallbackData());
    }

    @Test
    void testCreateContinueKeyboard_WithNext() {
        // Act
        InlineKeyboardMarkup keyboard = KeyboardBuilder.createContinueKeyboard(true);

        // Assert
        assertNotNull(keyboard);
        assertNotNull(keyboard.getKeyboard());
        assertEquals(2, keyboard.getKeyboard().size()); // Next button + Exit button
        
        // First row: Next question button
        List<InlineKeyboardButton> firstRow = keyboard.getKeyboard().get(0);
        assertEquals(1, firstRow.size());
        assertEquals(ButtonText.NEXT_QUESTION, firstRow.get(0).getText());
        assertEquals(CallbackData.NEXT_QUESTION, firstRow.get(0).getCallbackData());
        
        // Second row: Exit button
        List<InlineKeyboardButton> secondRow = keyboard.getKeyboard().get(1);
        assertEquals(1, secondRow.size());
        assertEquals(ButtonText.EXIT_SESSION, secondRow.get(0).getText());
    }

    @Test
    void testCreateContinueKeyboard_WithoutNext() {
        // Act
        InlineKeyboardMarkup keyboard = KeyboardBuilder.createContinueKeyboard(false);

        // Assert
        assertNotNull(keyboard);
        assertNotNull(keyboard.getKeyboard());
        assertEquals(1, keyboard.getKeyboard().size()); // Only Exit button
        
        List<InlineKeyboardButton> row = keyboard.getKeyboard().get(0);
        assertEquals(1, row.size());
        assertEquals(ButtonText.EXIT_SESSION, row.get(0).getText());
        assertEquals(CallbackData.EXIT_SESSION, row.get(0).getCallbackData());
    }

    @Test
    void testCreateDailyNotificationKeyboard() {
        // Act
        InlineKeyboardMarkup keyboard = KeyboardBuilder.createDailyNotificationKeyboard();

        // Assert
        assertNotNull(keyboard);
        assertNotNull(keyboard.getKeyboard());
        assertEquals(1, keyboard.getKeyboard().size());
        
        List<InlineKeyboardButton> row = keyboard.getKeyboard().get(0);
        assertEquals(2, row.size());
        assertEquals(ButtonText.YES, row.get(0).getText());
        assertEquals(CallbackData.YES_TEST, row.get(0).getCallbackData());
        assertEquals(ButtonText.NO, row.get(1).getText());
        assertEquals(CallbackData.NO_TEST, row.get(1).getCallbackData());
    }

    @Test
    void testCreateDifficultyKeyboard() {
        // Act
        InlineKeyboardMarkup keyboard = KeyboardBuilder.createDifficultyKeyboard();

        // Assert
        assertNotNull(keyboard);
        assertNotNull(keyboard.getKeyboard());
        assertEquals(2, keyboard.getKeyboard().size()); // Two rows
        
        // First row: Junior and Middle
        List<InlineKeyboardButton> firstRow = keyboard.getKeyboard().get(0);
        assertEquals(2, firstRow.size());
        assertEquals(ButtonText.JUNIOR, firstRow.get(0).getText());
        assertEquals(CallbackData.DIFF_JUNIOR, firstRow.get(0).getCallbackData());
        assertEquals(ButtonText.MIDDLE, firstRow.get(1).getText());
        assertEquals(CallbackData.DIFF_MIDDLE, firstRow.get(1).getCallbackData());
        
        // Second row: Senior
        List<InlineKeyboardButton> secondRow = keyboard.getKeyboard().get(1);
        assertEquals(1, secondRow.size());
        assertEquals(ButtonText.SENIOR, secondRow.get(0).getText());
        assertEquals(CallbackData.DIFF_SENIOR, secondRow.get(0).getCallbackData());
    }

    @Test
    void testCreateMainMenuKeyboard() {
        // Act
        InlineKeyboardMarkup keyboard = KeyboardBuilder.createMainMenuKeyboard();

        // Assert
        assertNotNull(keyboard);
        assertNotNull(keyboard.getKeyboard());
        assertEquals(1, keyboard.getKeyboard().size());
        
        List<InlineKeyboardButton> row = keyboard.getKeyboard().get(0);
        assertEquals(2, row.size());
        assertEquals(ButtonText.START_SESSION, row.get(0).getText());
        assertEquals(CallbackData.RANDOM_QUESTION, row.get(0).getCallbackData());
        assertEquals(ButtonText.SETTINGS, row.get(1).getText());
        assertEquals(CallbackData.SETTINGS_MENU, row.get(1).getCallbackData());
    }

    @Test
    void testCreateSettingsKeyboard() {
        // Act
        InlineKeyboardMarkup keyboard = KeyboardBuilder.createSettingsKeyboard();

        // Assert
        assertNotNull(keyboard);
        assertNotNull(keyboard.getKeyboard());
        assertEquals(4, keyboard.getKeyboard().size()); // Four rows
        
        // Check all buttons
        assertEquals(ButtonText.CHANGE_TIME, keyboard.getKeyboard().get(0).get(0).getText());
        assertEquals(ButtonText.CHANGE_COUNT, keyboard.getKeyboard().get(1).get(0).getText());
        assertEquals(ButtonText.ADD_QUESTION, keyboard.getKeyboard().get(2).get(0).getText());
        assertEquals(ButtonText.BACK, keyboard.getKeyboard().get(3).get(0).getText());
    }

    @Test
    void testCreateSimpleQuestionKeyboard() {
        // Arrange
        Long questionId = 456L;

        // Act
        InlineKeyboardMarkup keyboard = KeyboardBuilder.createSimpleQuestionKeyboard(questionId);

        // Assert
        assertNotNull(keyboard);
        assertNotNull(keyboard.getKeyboard());
        assertEquals(1, keyboard.getKeyboard().size());
        
        List<InlineKeyboardButton> row = keyboard.getKeyboard().get(0);
        assertEquals(2, row.size());
        assertEquals(ButtonText.SHOW_ANSWER, row.get(0).getText());
        assertEquals(ButtonText.REPLY, row.get(1).getText());
    }
}

