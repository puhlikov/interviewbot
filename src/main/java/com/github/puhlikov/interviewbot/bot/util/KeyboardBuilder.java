package com.github.puhlikov.interviewbot.bot.util;

import com.github.puhlikov.interviewbot.bot.constants.ButtonText;
import com.github.puhlikov.interviewbot.bot.constants.CallbackData;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

/**
 * Утилитный класс для построения клавиатур Telegram бота
 */
public final class KeyboardBuilder {
    
    private KeyboardBuilder() {
        // Utility class
    }
    
    /**
     * Создает основную Reply клавиатуру с постоянными кнопками
     */
    public static ReplyKeyboardMarkup createMainReplyKeyboard() {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);
        keyboard.setOneTimeKeyboard(false);
        keyboard.setSelective(true);
        
        List<KeyboardRow> rows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add(new KeyboardButton(ButtonText.START_SESSION));
        row.add(new KeyboardButton(ButtonText.SETTINGS));
        rows.add(row);
        keyboard.setKeyboard(rows);
        
        return keyboard;
    }
    
    /**
     * Создает Reply клавиатуру во время активной сессии вопросов
     */
    public static ReplyKeyboardMarkup createSessionReplyKeyboard() {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);
        keyboard.setOneTimeKeyboard(false);
        keyboard.setSelective(true);
        
        List<KeyboardRow> rows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add(new KeyboardButton(ButtonText.STOP_SESSION));
        row.add(new KeyboardButton(ButtonText.SETTINGS));
        rows.add(row);
        keyboard.setKeyboard(rows);
        
        return keyboard;
    }
    
    /**
     * Создает Inline клавиатуру для вопроса с кнопками "Показать ответ" и "Выйти"
     */
    public static InlineKeyboardMarkup createQuestionKeyboard(Long questionId) {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        
        var answerBtn = InlineKeyboardButton.builder()
                .text(ButtonText.SHOW_ANSWER)
                .callbackData(CallbackData.answerCallback(questionId))
                .build();
        
        var exitBtn = InlineKeyboardButton.builder()
                .text(ButtonText.EXIT_SESSION)
                .callbackData(CallbackData.EXIT_SESSION)
                .build();
        
        rows.add(List.of(answerBtn));
        rows.add(List.of(exitBtn));
        keyboard.setKeyboard(rows);
        
        return keyboard;
    }
    
    /**
     * Создает Inline клавиатуру для продолжения после ответа на вопрос
     */
    public static InlineKeyboardMarkup createContinueKeyboard(boolean hasNextQuestion) {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        
        if (hasNextQuestion) {
            var nextBtn = InlineKeyboardButton.builder()
                    .text(ButtonText.NEXT_QUESTION)
                    .callbackData(CallbackData.NEXT_QUESTION)
                    .build();
            rows.add(List.of(nextBtn));
        }
        
        var exitBtn = InlineKeyboardButton.builder()
                .text(ButtonText.EXIT_SESSION)
                .callbackData(CallbackData.EXIT_SESSION)
                .build();
        rows.add(List.of(exitBtn));
        
        keyboard.setKeyboard(rows);
        return keyboard;
    }
    
    /**
     * Создает Inline клавиатуру для ежедневного уведомления
     */
    public static InlineKeyboardMarkup createDailyNotificationKeyboard() {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        
        var yesBtn = InlineKeyboardButton.builder()
                .text(ButtonText.YES)
                .callbackData(CallbackData.YES_TEST)
                .build();
        
        var noBtn = InlineKeyboardButton.builder()
                .text(ButtonText.NO)
                .callbackData(CallbackData.NO_TEST)
                .build();
        
        rows.add(List.of(yesBtn, noBtn));
        keyboard.setKeyboard(rows);
        
        return keyboard;
    }
    
    /**
     * Создает Inline клавиатуру для выбора сложности вопроса
     */
    public static InlineKeyboardMarkup createDifficultyKeyboard() {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        
        var juniorBtn = InlineKeyboardButton.builder()
                .text(ButtonText.JUNIOR)
                .callbackData(CallbackData.DIFF_JUNIOR)
                .build();
        
        var middleBtn = InlineKeyboardButton.builder()
                .text(ButtonText.MIDDLE)
                .callbackData(CallbackData.DIFF_MIDDLE)
                .build();
        
        var seniorBtn = InlineKeyboardButton.builder()
                .text(ButtonText.SENIOR)
                .callbackData(CallbackData.DIFF_SENIOR)
                .build();
        
        rows.add(List.of(juniorBtn, middleBtn));
        rows.add(List.of(seniorBtn));
        keyboard.setKeyboard(rows);
        
        return keyboard;
    }
    
    /**
     * Создает Inline клавиатуру главного меню
     */
    public static InlineKeyboardMarkup createMainMenuKeyboard() {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        
        var startSessionBtn = InlineKeyboardButton.builder()
                .text(ButtonText.START_SESSION)
                .callbackData(CallbackData.RANDOM_QUESTION)
                .build();
        
        var settingsBtn = InlineKeyboardButton.builder()
                .text(ButtonText.SETTINGS)
                .callbackData(CallbackData.SETTINGS_MENU)
                .build();
        
        rows.add(List.of(startSessionBtn, settingsBtn));
        keyboard.setKeyboard(rows);
        
        return keyboard;
    }
    
    /**
     * Создает Inline клавиатуру меню настроек
     */
    public static InlineKeyboardMarkup createSettingsKeyboard() {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        
        var timeBtn = InlineKeyboardButton.builder()
                .text(ButtonText.CHANGE_TIME)
                .callbackData(CallbackData.SETTINGS_TIME)
                .build();
        
        var countBtn = InlineKeyboardButton.builder()
                .text(ButtonText.CHANGE_COUNT)
                .callbackData(CallbackData.SETTINGS_COUNT)
                .build();
        
        var disableNotificationsBtn = InlineKeyboardButton.builder()
                .text(ButtonText.DISABLE_NOTIFICATIONS)
                .callbackData(CallbackData.SETTINGS_DISABLE_NOTIFICATIONS)
                .build();
        
        var addQuestionBtn = InlineKeyboardButton.builder()
                .text(ButtonText.ADD_QUESTION)
                .callbackData(CallbackData.ADD_QUESTION)
                .build();
        
        var backBtn = InlineKeyboardButton.builder()
                .text(ButtonText.BACK)
                .callbackData(CallbackData.BACK_TO_MENU)
                .build();
        
        rows.add(List.of(timeBtn));
        rows.add(List.of(countBtn));
        rows.add(List.of(disableNotificationsBtn));
        rows.add(List.of(addQuestionBtn));
        rows.add(List.of(backBtn));
        keyboard.setKeyboard(rows);
        
        return keyboard;
    }
    
    /**
     * Создает простую Inline клавиатуру с кнопкой "Показать ответ"
     */
    public static InlineKeyboardMarkup createSimpleQuestionKeyboard(Long questionId) {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        
        var answerBtn = InlineKeyboardButton.builder()
                .text(ButtonText.SHOW_ANSWER)
                .callbackData(CallbackData.answerCallback(questionId))
                .build();
        
        rows.add(List.of(answerBtn));
        keyboard.setKeyboard(rows);
        
        return keyboard;
    }
}

