package com.github.puhlikov.interviewbot.service;

import com.github.puhlikov.interviewbot.bot.constants.AppConstants;
import com.github.puhlikov.interviewbot.enums.RegistrationState;
import com.github.puhlikov.interviewbot.enums.SettingsState;
import com.github.puhlikov.interviewbot.model.BotUser;
import com.github.puhlikov.interviewbot.repo.BotUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RegistrationService {

    private static final Logger logger = LoggerFactory.getLogger(RegistrationService.class);
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern(AppConstants.TIME_FORMAT);
    
    private final BotUserRepository userRepository;
    private final Map<Long, SettingsState> userSettingsState = new ConcurrentHashMap<>();

    public RegistrationService(BotUserRepository userRepository) {
        this.userRepository = userRepository;
    }
    
    private Optional<BotUser> findUser(Long chatId) {
        return userRepository.findByChatId(chatId);
    }
    
    private BotUser updateUser(Long chatId, java.util.function.Consumer<BotUser> updater) {
        return findUser(chatId)
                .map(user -> {
                    updater.accept(user);
                    return userRepository.save(user);
                })
                .orElse(null);
    }

    public BotUser startRegistration(Long chatId, String firstName, String lastName, String username) {
        BotUser user = new BotUser();
        user.setChatId(chatId);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setUsername(username);
        user.setRegistrationState(RegistrationState.SCHEDULE_TIME); // Сразу переходим к настройке времени
        return userRepository.save(user);
    }
    
    // Обратная совместимость для существующих вызовов
    @Deprecated
    public BotUser startRegistration(Long chatId) {
        BotUser user = new BotUser();
        user.setChatId(chatId);
        user.setRegistrationState(RegistrationState.SCHEDULE_TIME);
        return userRepository.save(user);
    }

    public Optional<BotUser> getUserByChatId(Long chatId) {
        return userRepository.findByChatId(chatId);
    }

    public BotUser updateUserState(Long chatId, RegistrationState state) {
        return updateUser(chatId, user -> user.setRegistrationState(state));
    }

    public BotUser updateFirstName(Long chatId, String firstName) {
        return updateUser(chatId, user -> {
            user.setFirstName(firstName);
            user.setRegistrationState(RegistrationState.LAST_NAME);
        });
    }

    public BotUser updateLastName(Long chatId, String lastName) {
        return updateUser(chatId, user -> {
            user.setLastName(lastName);
            user.setRegistrationState(RegistrationState.USERNAME);
        });
    }

    public BotUser updateUsername(Long chatId, String username) {
        return updateUser(chatId, user -> {
            user.setUsername(username);
            user.setRegistrationState(RegistrationState.SCHEDULE_TIME);
        });
    }

    public BotUser updateScheduleTime(Long chatId, String timeString) {
        try {
            LocalTime scheduleTime = LocalTime.parse(timeString, TIME_FORMATTER);
            return updateUser(chatId, user -> {
                user.setScheduleTime(scheduleTime);
                user.setRegistrationState(RegistrationState.TIMEZONE);
            });
        } catch (DateTimeParseException e) {
            logger.warn("Invalid time format: {}", timeString);
            throw new IllegalArgumentException("Неверный формат времени. Используйте HH:mm (например, 14:00)");
        }
    }

    public BotUser updateScheduleTimeForSettings(Long chatId, String timeString) {
        try {
            LocalTime scheduleTime = LocalTime.parse(timeString, TIME_FORMATTER);
            return updateUser(chatId, user -> {
                user.setScheduleTime(scheduleTime);
                // Не меняем состояние регистрации при изменении в настройках
            });
        } catch (DateTimeParseException e) {
            logger.warn("Invalid time format: {}", timeString);
            throw new IllegalArgumentException("Неверный формат времени. Используйте HH:mm (например, 14:00)");
        }
    }

    public BotUser updateTimezone(Long chatId, String timezone) {
        return updateUser(chatId, user -> {
            user.setTimezone(timezone);
            user.setRegistrationState(RegistrationState.COMPLETED);
            user.setQuestionsPerSession(AppConstants.DEFAULT_QUESTIONS_PER_SESSION);
        });
    }

    public ReplyKeyboardMarkup getTimezoneKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow row1 = new KeyboardRow();
        row1.add("Europe/Moscow");
        row1.add("Europe/London");

        KeyboardRow row2 = new KeyboardRow();
        row2.add("Europe/Berlin");
        row2.add("America/New_York");

        KeyboardRow row3 = new KeyboardRow();
        row3.add("Asia/Tokyo");
        row3.add("Asia/Dubai");

        keyboard.add(row1);
        keyboard.add(row2);
        keyboard.add(row3);

        keyboardMarkup.setKeyboard(keyboard);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(true);

        return keyboardMarkup;
    }

    public BotUser updateQuestionsPerSession(Long chatId, String questionsCountStr) {
        try {
            int questionsCount = Integer.parseInt(questionsCountStr);
            if (questionsCount < AppConstants.MIN_QUESTIONS_PER_SESSION || 
                questionsCount > AppConstants.MAX_QUESTIONS_PER_SESSION) {
                throw new IllegalArgumentException(
                    String.format("Количество вопросов должно быть от %d до %d", 
                        AppConstants.MIN_QUESTIONS_PER_SESSION, 
                        AppConstants.MAX_QUESTIONS_PER_SESSION));
            }

            BotUser updated = updateUser(chatId, user -> {
                user.setQuestionsPerSession(questionsCount);
                clearSettingsState(chatId);
            });
            
            if (updated == null) {
                throw new IllegalArgumentException("Пользователь не найден");
            }
            
            return updated;
        } catch (NumberFormatException e) {
            logger.warn("Invalid questions count format: {}", questionsCountStr);
            throw new IllegalArgumentException(
                String.format("Пожалуйста, введите корректное число от %d до %d",
                    AppConstants.MIN_QUESTIONS_PER_SESSION,
                    AppConstants.MAX_QUESTIONS_PER_SESSION));
        }
    }

    public void startQuestionsCountSetting(Long chatId) {
        userSettingsState.put(chatId, SettingsState.AWAITING_QUESTIONS_COUNT);
    }

    public void startTimeSetting(Long chatId) {
        userSettingsState.put(chatId, SettingsState.AWAITING_TIME);
    }

    public boolean isInSettingsState(Long chatId, SettingsState state) {
        return userSettingsState.getOrDefault(chatId, SettingsState.NONE) == state;
    }

    public void clearSettingsState(Long chatId) {
        userSettingsState.remove(chatId);
    }

    public BotUser disableNotifications(Long chatId) {
        BotUser updated = updateUser(chatId, user -> {
            user.setScheduleTime(null);
        });
        
        if (updated == null) {
            throw new IllegalArgumentException("Пользователь не найден");
        }
        
        return updated;
    }
}
