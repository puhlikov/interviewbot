package com.github.puhlikov.interviewbot.bot.constants;

/**
 * Константы для текстов сообщений бота
 */
public final class Messages {
    
    // Registration messages
    public static final String WELCOME = "Добро пожаловать! Давайте зарегистрируем вас в системе.\n\nВведите ваше имя:";
    public static final String ENTER_FIRST_NAME = "Отлично! Теперь введите вашу фамилию:";
    public static final String ENTER_LAST_NAME = "Хорошо! Теперь введите ваш username (без @):";
    public static final String ENTER_USERNAME = "Отлично! Теперь введите время для ежедневной рассылки в формате HH:mm (например, 14:00):";
    public static final String ENTER_TIME = "Прекрасно! Теперь выберите ваш часовой пояс:";
    public static final String INVALID_TIME_FORMAT = "Неверный формат времени. Пожалуйста, введите время в формате HH:mm (например, 14:00):";
    
    public static String registrationComplete(String scheduleTime, String timezone, int questionsPerSession) {
        return "🎉 Регистрация завершена! 🎉\n\n" +
                "Теперь вы будете получать ежедневные уведомления в " +
                scheduleTime + " по времени " + timezone + "\n\n" +
                "Количество вопросов в сессии: " + questionsPerSession + "\n\n" +
                "Используйте кнопки ниже для быстрого доступа к функциям бота.";
    }
    
    public static final String START_COMMAND_REQUIRED = "Для начала работы отправьте /start";
    
    // Question session messages
    public static final String NO_QUESTIONS_IN_DB = "В базе нет вопросов.";
    public static final String QUESTION_SESSION_STARTED = "🎯 **Начинаем сессию вопросов!**";
    public static final String NO_QUESTIONS_FOR_SESSION = "❌ В базе нет вопросов. Сначала добавьте вопросы с помощью /add_question";
    public static final String FAILED_TO_LOAD_QUESTIONS = "❌ Не удалось загрузить вопросы. Попробуйте позже или уменьшите количество вопросов в настройках.";
    public static final String FAILED_TO_GET_QUESTION = "❌ Не удалось получить вопрос. Попробуйте снова.";
    
    public static String questionNumber(int number) {
        return "❓ **Вопрос " + number + ":**\n\n";
    }
    
    public static final String WAITING_FOR_TEXT_ANSWER = "✍️ Ответьте на вопрос и отправьте одним сообщением.";
    public static final String GENERATING_ANSWER = "Генерируем ответ...";
    public static final String WAITING_TEXT_RESPONSE = "Ожидаю ваш текстовый ответ…";
    
    public static final String SESSION_COMPLETED = "🎉 **Вы ответили на все вопросы в этой сессии!**";
    public static final String SESSION_STOPPED = "🏁 **Сессия вопросов завершена.**\n\n" +
            "Чтобы начать новую сессию, используйте кнопку «🎲 Начать сессию вопросов» или главное меню.";
    
    public static final String WHAT_NEXT = "**Что делаем дальше?**";
    public static final String STARTING_SESSION = "Начинаем сессию вопросов...";
    
    // Daily notification
    public static final String DAILY_TEST_PROMPT = "🕐 Время для ежедневного теста!\n\nХотите пройти тест сегодня?";
    public static final String STARTING_TEST = "Начинаем тест!";
    public static final String DECLINED_TEST = "Хорошо, в другой раз!";
    
    // Add question messages
    public static final String ADDING_QUESTION = "📝 **Добавление нового вопроса**\n\n" +
            "Пожалуйста, введите текст вопроса:";
    public static final String CHECKING_QUESTION = "⏳ Проверяю вопрос...";
    public static final String QUESTION_NOT_PROGRAMMING_RELATED = 
            "❌ Вопрос не относится к программированию или IT-технологиям.\n\n" +
            "Пожалуйста, введите вопрос, связанный с:\n" +
            "• Языками программирования\n" +
            "• Алгоритмами и структурами данных\n" +
            "• Разработкой ПО\n" +
            "• Базами данных\n" +
            "• Фреймворками и библиотеками\n" +
            "• IT-инфраструктурой\n" +
            "• Тестированием и QA\n" +
            "• DevOps и CI/CD\n\n" +
            "Попробуйте еще раз:";
    public static final String ENTER_CATEGORY = "📚 Теперь введите категорию вопроса (например: Java, SQL, Algorithms):";
    public static final String SELECT_DIFFICULTY = "🎯 Выберите уровень сложности:";
    
    public static String questionAdded(String text, String category, String difficulty) {
        return "✅ **Вопрос успешно добавлен!**\n\n" +
                "📖 Текст: " + text + "\n" +
                "📚 Категория: " + category + "\n" +
                "🎯 Сложность: " + difficulty + "\n\n" +
                "Вопрос теперь доступен в базе для всех пользователей.";
    }
    
    public static final String ERROR_SAVING_QUESTION = "❌ Произошла ошибка при сохранении вопроса. Пожалуйста, попробуйте снова.";
    public static final String ADDING_QUESTION_START = "Переходим к добавлению вопроса...";
    
    // Main menu
    public static final String MAIN_MENU_TITLE = "📋 **Главное меню**\n\nВыберите действие:";
    public static final String USE_PERSISTENT_BUTTONS = "💡 Также можете использовать постоянные кнопки ниже для быстрого доступа:";
    
    // Settings messages
    public static String currentSettings(String scheduleTime, int questionsPerSession) {
        String timeDisplay = scheduleTime != null ? scheduleTime : "❌ Отключено";
        return "⚙️ **Текущие настройки:**\n\n" +
                "🕐 Время рассылки: " + timeDisplay + "\n" +
                "📊 Вопросов в сессии: " + questionsPerSession;
    }
    
    public static final String NOTIFICATIONS_DISABLED = "✅ Уведомления отключены. Вы больше не будете получать ежедневные уведомления о прохождении сессии.";
    
    public static final String SELECT_SETTING_TO_CHANGE = "Выберите, что хотите изменить:";
    public static final String ENTER_NEW_TIME = "🕐 Введите новое время для ежедневной рассылки в формате HH:mm (например, 14:00):";
    public static final String ENTER_QUESTIONS_COUNT = "📊 Введите новое количество вопросов для сессии (от 1 до 50):";
    public static final String QUESTIONS_COUNT_UPDATED = "✅ Количество вопросов в сессии изменено на: %s";
    public static final String INVALID_QUESTIONS_COUNT = "❌ %s\n\nПожалуйста, введите число от 1 до 50:";
    public static final String OPENING_SETTINGS = "Открываем настройки...";
    
    // Answer format
    public static String formattedAnswer(String answer) {
        return "🤖 **Ответ:**\n\n" + answer;
    }
    
    // GPT verification prefix
    public static String gptVerificationPrompt(String userAnswer) {
        return "Верно ли утверждение: \"" + userAnswer + "\"? Ответь кратко: верно/неверно и короткое пояснение.";
    }
    
    // Question prefix
    public static final String QUESTION_PREFIX = "❓ Вопрос:\n\n";
    
    // Error messages
    public static final String USER_NOT_FOUND = "❌ Сначала зарегистрируйтесь с помощью /start";
    public static final String ERROR_OCCURRED = "❌ Произошла ошибка. Попробуйте позже.";
    
    private Messages() {
        // Utility class
    }
}

