package com.github.puhlikov.interviewbot.bot.constants;

/**
 * Константы для callback data кнопок
 */
public final class CallbackData {
    
    // Answer callbacks
    public static final String ANSWER_PREFIX = "ANS:";
    
    // Reply callbacks
    public static final String REPLY_PREFIX = "REPLY:";
    
    // Test callbacks
    public static final String YES_TEST = "YES_TEST";
    public static final String NO_TEST = "NO_TEST";
    
    // Difficulty callbacks
    public static final String DIFFICULTY_PREFIX = "DIFF_";
    public static final String DIFF_JUNIOR = "DIFF_JUNIOR";
    public static final String DIFF_MIDDLE = "DIFF_MIDDLE";
    public static final String DIFF_SENIOR = "DIFF_SENIOR";
    
    // Question session callbacks
    public static final String RANDOM_QUESTION = "RANDOM_QUESTION";
    public static final String NEXT_QUESTION = "NEXT_QUESTION";
    public static final String STOP_QUESTIONS = "STOP_QUESTIONS";
    public static final String EXIT_SESSION = "EXIT_SESSION";
    
    // Menu callbacks
    public static final String ADD_QUESTION = "ADD_QUESTION";
    public static final String SETTINGS_MENU = "SETTINGS_MENU";
    public static final String BACK_TO_MENU = "BACK_TO_MENU";
    
    // Settings callbacks
    public static final String SETTINGS_TIME = "SETTINGS_TIME";
    public static final String SETTINGS_COUNT = "SETTINGS_COUNT";
    public static final String SETTINGS_DISABLE_NOTIFICATIONS = "SETTINGS_DISABLE_NOTIFICATIONS";
    
    private CallbackData() {
        // Utility class
    }
    
    public static String answerCallback(Long questionId) {
        return ANSWER_PREFIX + questionId;
    }
    
    public static String replyCallback(Long questionId) {
        return REPLY_PREFIX + questionId;
    }
    
    public static String difficultyCallback(String difficulty) {
        return DIFFICULTY_PREFIX + difficulty;
    }
}

