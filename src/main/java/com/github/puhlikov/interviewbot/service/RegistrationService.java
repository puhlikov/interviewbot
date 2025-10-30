package com.github.puhlikov.interviewbot.service;

import com.github.puhlikov.interviewbot.enums.RegistrationState;
import com.github.puhlikov.interviewbot.enums.SettingsState;
import com.github.puhlikov.interviewbot.model.BotUser;
import com.github.puhlikov.interviewbot.repo.BotUserRepository;
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

    private final BotUserRepository userRepository;
    private final Map<Long, SettingsState> userSettingsState = new ConcurrentHashMap<>();

    public RegistrationService(BotUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public BotUser startRegistration(Long chatId) {
        BotUser user = new BotUser();
        user.setChatId(chatId);
        user.setRegistrationState(RegistrationState.FIRST_NAME);
        return userRepository.save(user);
    }

    public Optional<BotUser> getUserByChatId(Long chatId) {
        return userRepository.findByChatId(chatId);
    }

    public BotUser updateUserState(Long chatId, RegistrationState state) {
        Optional<BotUser> userOpt = userRepository.findByChatId(chatId);
        if (userOpt.isPresent()) {
            BotUser user = userOpt.get();
            user.setRegistrationState(state);
            return userRepository.save(user);
        }
        return null;
    }

    public BotUser updateFirstName(Long chatId, String firstName) {
        Optional<BotUser> userOpt = userRepository.findByChatId(chatId);
        if (userOpt.isPresent()) {
            BotUser user = userOpt.get();
            user.setFirstName(firstName);
            user.setRegistrationState(RegistrationState.LAST_NAME);
            return userRepository.save(user);
        }
        return null;
    }

    public BotUser updateLastName(Long chatId, String lastName) {
        Optional<BotUser> userOpt = userRepository.findByChatId(chatId);
        if (userOpt.isPresent()) {
            BotUser user = userOpt.get();
            user.setLastName(lastName);
            user.setRegistrationState(RegistrationState.USERNAME);
            return userRepository.save(user);
        }
        return null;
    }

    public BotUser updateUsername(Long chatId, String username) {
        Optional<BotUser> userOpt = userRepository.findByChatId(chatId);
        if (userOpt.isPresent()) {
            BotUser user = userOpt.get();
            user.setUsername(username);
            user.setRegistrationState(RegistrationState.SCHEDULE_TIME);
            return userRepository.save(user);
        }
        return null;
    }

    public BotUser updateScheduleTime(Long chatId, String timeString) {
        try {
            LocalTime scheduleTime = LocalTime.parse(timeString, DateTimeFormatter.ofPattern("HH:mm"));
            Optional<BotUser> userOpt = userRepository.findByChatId(chatId);
            if (userOpt.isPresent()) {
                BotUser user = userOpt.get();
                user.setScheduleTime(scheduleTime);
                user.setRegistrationState(RegistrationState.TIMEZONE);
                return userRepository.save(user);
            }
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Неверный формат времени. Используйте HH:mm (например, 14:00)");
        }
        return null;
    }

    public BotUser updateTimezone(Long chatId, String timezone) {
        Optional<BotUser> userOpt = userRepository.findByChatId(chatId);
        if (userOpt.isPresent()) {
            BotUser user = userOpt.get();
            user.setTimezone(timezone);
            user.setRegistrationState(RegistrationState.COMPLETED);
            user.setQuestionsPerSession(20);
            return userRepository.save(user);
        }
        return null;
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
            if (questionsCount < 1 || questionsCount > 50) {
                throw new IllegalArgumentException("Количество вопросов должно быть от 1 до 50");
            }

            Optional<BotUser> userOpt = userRepository.findByChatId(chatId);
            if (userOpt.isPresent()) {
                BotUser user = userOpt.get();
                user.setQuestionsPerSession(questionsCount);
                clearSettingsState(chatId);
                return userRepository.save(user);
            } else {
                throw new IllegalArgumentException("Пользователь не найден");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Пожалуйста, введите корректное число от 1 до 50");
        }
    }

    public void startQuestionsCountSetting(Long chatId) {
        userSettingsState.put(chatId, SettingsState.AWAITING_QUESTIONS_COUNT);
    }

    public boolean isInSettingsState(Long chatId, SettingsState state) {
        return userSettingsState.getOrDefault(chatId, SettingsState.NONE) == state;
    }

    public void clearSettingsState(Long chatId) {
        userSettingsState.remove(chatId);
    }
}
