package com.github.puhlikov.interviewbot.bot;

import com.github.puhlikov.interviewbot.enums.QuestionState;
import com.github.puhlikov.interviewbot.enums.RegistrationState;
import com.github.puhlikov.interviewbot.enums.SettingsState;
import com.github.puhlikov.interviewbot.model.BotUser;
import com.github.puhlikov.interviewbot.model.Question;
import com.github.puhlikov.interviewbot.repo.BotUserRepository;
import com.github.puhlikov.interviewbot.service.QuestionCacheService;
import com.github.puhlikov.interviewbot.service.QuestionService;
import com.github.puhlikov.interviewbot.service.QuestionSessionService;
import com.github.puhlikov.interviewbot.service.RegistrationService;
import com.github.puhlikov.interviewbot.service.WorkingApiService;
// voice-to-text removed
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InterviewTelegramBot extends TelegramLongPollingBot {

    private final String username;
    private final BotUserRepository users;
    private final QuestionService questions;
//    private final DeepSeekService deepseek;
    private final RegistrationService registrationService;
    private final QuestionSessionService questionSessionService;
    private final QuestionService questionService;
    private final WorkingApiService workingApiService;
    private final QuestionCacheService questionCacheService;
    private final Set<Long> awaitingText = ConcurrentHashMap.newKeySet();

    public InterviewTelegramBot(
            @Value("${telegram.bot.username}") String username,
            @Value("${telegram.bot.token}") String token,
            BotUserRepository users,
            QuestionService questions,
//            DeepSeekService deepseek,
            RegistrationService registrationService,
            QuestionSessionService questionSessionService,
            QuestionService questionService,
            WorkingApiService workingApiService,
            QuestionCacheService questionCacheService
    ) {
        super(token);
        this.username = username;
        this.users = users;
        this.questions = questions;
//        this.deepseek = deepseek;
        this.registrationService = registrationService;
        this.questionSessionService = questionSessionService;
        this.questionService = questionService;
        this.workingApiService = workingApiService;
        this.questionCacheService = questionCacheService;
    }

    @Override
    public String getBotUsername() {
        return username;
    }

    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasMessage() && update.getMessage().hasText()) {
                handleMessage(update);
            } else if (update.hasCallbackQuery()) {
                handleCallback(update.getCallbackQuery());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleMessage(Update update) {
        var msg = update.getMessage();
        var chatId = msg.getChatId();
        var text = msg.getText().trim();

        Optional<BotUser> userOpt = registrationService.getUserByChatId(chatId);

        if (!userOpt.isPresent() || userOpt.get().getRegistrationState() == RegistrationState.START) {
            if ("/start".equalsIgnoreCase(text)) {
                startRegistration(chatId);
            } else {
                execSend(chatId, "Для начала работы отправьте /start");
            }
            return;
        }

        BotUser user = userOpt.get();

        // Проверяем сессию вопросов
        if (handleQuestionSession(chatId, text)) {
            return;
        }

        // Проверяем настройки
        if (handleSettings(chatId, text, user)) {
            return;
        }

        // Если пользователь в сессии вопросов и отправил произвольный текст —
        // отправляем его на проверку в WorkingApiService с префиксом
        if (questionCacheService.getUserCache(chatId) != null && !text.startsWith("/")) {
            String prompt = "Верно ли утверждение: \"" + text + "\"? Ответь кратко: верно/неверно и короткое пояснение.";
            workingApiService.getAnswer(prompt).subscribe(gptResult -> {
                execSend(chatId, "🤖 " + gptResult);
                // без возврата в меню
            });
            return;
        }

        if (user.getRegistrationState() == RegistrationState.COMPLETED) {
            handleCompletedUser(chatId, text, user);
        } else {
            handleRegistrationStep(chatId, text, user);
        }
    }

    private void startRegistration(Long chatId) {
        registrationService.startRegistration(chatId);
        execSend(chatId, "Добро пожаловать! Давайте зарегистрируем вас в системе.\n\nВведите ваше имя:");
    }

    private void handleRegistrationStep(Long chatId, String text, BotUser user) {
        switch (user.getRegistrationState()) {
            case FIRST_NAME:
                registrationService.updateFirstName(chatId, text);
                execSend(chatId, "Отлично! Теперь введите вашу фамилию:");
                break;

            case LAST_NAME:
                registrationService.updateLastName(chatId, text);
                execSend(chatId, "Хорошо! Теперь введите ваш username (без @):");
                break;

            case USERNAME:
                registrationService.updateUsername(chatId, text);
                execSend(chatId, "Отлично! Теперь введите время для ежедневной рассылки в формате HH:mm (например, 14:00):");
                break;

            case SCHEDULE_TIME:
                try {
                    registrationService.updateScheduleTime(chatId, text);
                    var keyboard = registrationService.getTimezoneKeyboard();
                    execSend(chatId, "Прекрасно! Теперь выберите ваш часовой пояс:", keyboard);
                } catch (IllegalArgumentException e) {
                    execSend(chatId, "Неверный формат времени. Пожалуйста, введите время в формате HH:mm (например, 14:00):");
                }
                break;

            case TIMEZONE:
                registrationService.updateTimezone(chatId, text);
                execSend(chatId,
                        "🎉 Регистрация завершена! 🎉\n\n" +
                                "Теперь вы будете получать ежедневные уведомления в " +
                                user.getScheduleTime() + " по времени " + text + "\n\n" +
                                "Количество вопросов в сессии: " + (user.getQuestionsPerSession() != null ? user.getQuestionsPerSession() : 20) + "\n\n" +
                                "Используйте кнопки ниже для быстрого доступа к функциям бота.",
                        getMainReplyKeyboard()
                );
                break;

            case COMPLETED:
                handleCompletedUser(chatId, text, user);
                break;
        }
    }

    private void handleCompletedUser(Long chatId, String text, BotUser user) {
        // Обработка постоянных кнопок
        if ("🎲 Начать сессию вопросов".equals(text) || "/question".equalsIgnoreCase(text)) {
            startQuestionSession(chatId, user);
        } else if ("⚙️ Настройки".equals(text) || "/settings".equalsIgnoreCase(text)) {
            showSettingsMenu(chatId, user);
        } else if ("/add_question".equalsIgnoreCase(text)) {
            startAddingQuestion(chatId);
        } else if ("/menu".equalsIgnoreCase(text)) {
            showMainMenu(chatId);
        } else {
            showMainMenu(chatId);
        }
    }

    private void sendQuestion(Long chatId) {
        List<Question> questionList = questions.getRandomQuestions(1);
        if (questionList.isEmpty()) {
            execSend(chatId, "В базе нет вопросов.");
            return;
        }

        Question question = questionList.get(0);
        var kb = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        var btn = InlineKeyboardButton.builder()
                .text("Показать ответ")
                .callbackData("ANS:" + question.getId())
                .build();
        var replyBtn = InlineKeyboardButton.builder()
                .text("Ответить")
                .callbackData("REPLY:" + question.getId())
                .build();
        rows.add(List.of(btn, replyBtn));
        kb.setKeyboard(rows);
        execSend(chatId, "❓ Вопрос:\n\n" + question.getQuestionText(), kb);
    }

    private void handleCallback(CallbackQuery cq) {
        var data = cq.getData();
        var chatId = cq.getMessage().getChatId();

        if (data != null && data.startsWith("ANS:")) {
            handleAnswerCallback(cq, data.substring(4));
        } else if (data != null && data.startsWith("REPLY:")) {
            awaitingText.add(chatId);
            try {
                execute(AnswerCallbackQuery.builder()
                        .callbackQueryId(cq.getId())
                        .text("Ожидаю ваш текстовый ответ…")
                        .build());
            } catch (Exception ignored) {}
            execSend(chatId, "✍️ Ответьте на вопрос и отправьте одним сообщением.");
        } else if (data != null && data.equals("YES_TEST")) {
            handleTestResponse(cq, true);
        } else if (data != null && data.equals("NO_TEST")) {
            handleTestResponse(cq, false);
        } else if (data != null && data.startsWith("DIFF_")) {
            handleDifficultySelection(cq, data.substring(5));
        } else if (data != null && data.equals("RANDOM_QUESTION")) {
            handleRandomQuestion(cq);
        } else if (data != null && data.equals("ADD_QUESTION")) {
            handleAddQuestion(cq);
        } else if (data != null && data.equals("NEXT_QUESTION")) {
            handleNextQuestion(chatId);
        } else if (data != null && data.equals("STOP_QUESTIONS")) {
            handleStopQuestions(chatId);
        } else if (data != null && data.equals("EXIT_SESSION")) {
            handleStopQuestions(chatId);
        } else if (data != null && data.equals("SETTINGS_TIME")) {
            handleSettingsTime(chatId);
        } else if (data != null && data.equals("SETTINGS_COUNT")) {
            handleSettingsCount(chatId);
        } else if (data != null && data.equals("SETTINGS_MENU")) {
            handleSettingsMenu(cq);
        } else if (data != null && data.equals("BACK_TO_MENU")) {
            handleBackToMenu(chatId);
        }
    }

    private void handleAnswerCallback(CallbackQuery cq, String questionId) {
        var qid = Long.parseLong(questionId);
        questions.getById(qid).ifPresent(q -> {
            try {
                execute(AnswerCallbackQuery.builder()
                        .callbackQueryId(cq.getId())
                        .text("Генерируем ответ...")
                        .build());
            } catch (Exception ignored) {}

            workingApiService.getAnswer(q.getQuestionText()).subscribe(answer -> {
                try {
                    String formattedAnswer = "🤖 **Ответ:**\n\n" + answer;
                    execSend(cq.getMessage().getChatId(), formattedAnswer);

                    showContinueOptions(cq.getMessage().getChatId());
                } catch (Exception ignored) {}
            });
        });
    }

    private void handleTestResponse(CallbackQuery cq, boolean acceptTest) {
        try {
            if (acceptTest) {
                execute(AnswerCallbackQuery.builder()
                        .callbackQueryId(cq.getId())
                        .text("Начинаем тест!")
                        .build());
                sendQuestion(cq.getMessage().getChatId());
            } else {
                execute(AnswerCallbackQuery.builder()
                        .callbackQueryId(cq.getId())
                        .text("Хорошо, в другой раз!")
                        .build());
            }
        } catch (Exception ignored) {
        }
    }

    public void sendDailyNotification(Long chatId) {
        var kb = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        var yesBtn = InlineKeyboardButton.builder()
                .text("✅ Да")
                .callbackData("YES_TEST")
                .build();

        var noBtn = InlineKeyboardButton.builder()
                .text("❌ Нет")
                .callbackData("NO_TEST")
                .build();

        rows.add(List.of(yesBtn, noBtn));
        kb.setKeyboard(rows);

        execSend(chatId, "🕐 Время для ежедневного теста!\n\nХотите пройти тест сегодня?", kb);
    }

    private ReplyKeyboardMarkup getMainReplyKeyboard() {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        keyboard.setResizeKeyboard(true);
        keyboard.setOneTimeKeyboard(false);
        keyboard.setSelective(true);

        List<KeyboardRow> rows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add(new KeyboardButton("🎲 Начать сессию вопросов"));
        row.add(new KeyboardButton("⚙️ Настройки"));
        rows.add(row);
        keyboard.setKeyboard(rows);

        return keyboard;
    }

    private void execSend(Long chatId, String text) {
        execSend(chatId, text, null);
    }

    private void execSend(Long chatId, String text, Object replyMarkup) {
        try {
            var sm = SendMessage.builder()
                    .chatId(chatId.toString())
                    .text(text)
                    .replyMarkup((ReplyKeyboard) replyMarkup)
                    .build();
            execute(sm);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean handleQuestionSession(Long chatId, String text) {
        QuestionSessionService.QuestionSession session = questionSessionService.getSession(chatId);

        if (session == null) {
            if ("/add_question".equalsIgnoreCase(text)) {
                startAddingQuestion(chatId);
                return true;
            }
            return false;
        }

        switch (session.getState()) {
            case AWAITING_QUESTION_TEXT:
                handleAwaitingQuestionText(chatId, text);
                return true;

            case AWAITING_QUESTION_CATEGORY:
                handleAwaitingQuestionCategory(chatId, text);
                return true;

            default:
                return false;
        }
    }

    private void startAddingQuestion(Long chatId) {
        questionSessionService.startSession(chatId);
        execSend(chatId,
                "📝 **Добавление нового вопроса**\n\n" +
                        "Пожалуйста, введите текст вопроса:"
        );
    }

    private void handleAwaitingQuestionText(Long chatId, String text) {
        questionSessionService.setQuestionText(chatId, text);
        questionSessionService.updateState(chatId, QuestionState.AWAITING_QUESTION_CATEGORY);
        execSend(chatId, "📚 Теперь введите категорию вопроса (например: Java, SQL, Algorithms):");
    }

    private void handleAwaitingQuestionCategory(Long chatId, String text) {
        questionSessionService.setCategory(chatId, text);
        questionSessionService.updateState(chatId, QuestionState.AWAITING_QUESTION_DIFFICULTY);


        var keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        var juniorBtn = InlineKeyboardButton.builder()
                .text("👶 Junior")
                .callbackData("DIFF_JUNIOR")
                .build();

        var middleBtn = InlineKeyboardButton.builder()
                .text("💼 Middle")
                .callbackData("DIFF_MIDDLE")
                .build();

        var seniorBtn = InlineKeyboardButton.builder()
                .text("🎯 Senior")
                .callbackData("DIFF_SENIOR")
                .build();

        rows.add(List.of(juniorBtn, middleBtn));
        rows.add(List.of(seniorBtn));

        keyboard.setKeyboard(rows);

        execSend(chatId, "🎯 Выберите уровень сложности:", keyboard);
    }

    private void handleDifficultySelection(CallbackQuery cq, String difficulty) {
        try {
            var chatId = cq.getMessage().getChatId();
            var session = questionSessionService.getSession(chatId);

            if (session != null && session.getState() == QuestionState.AWAITING_QUESTION_DIFFICULTY) {
                String difficultyText = switch (difficulty) {
                    case "JUNIOR" -> "Junior";
                    case "MIDDLE" -> "Middle";
                    case "SENIOR" -> "Senior";
                    default -> difficulty;
                };

                Question question = new Question();
                question.setQuestionText(session.getQuestionText());
                question.setCategory(session.getCategory());
                question.setDifficultyLevel(difficultyText);
                question.setIsActive(true);

                questionService.save(question);

                questionSessionService.completeSession(chatId);

                execSend(chatId,
                        "✅ **Вопрос успешно добавлен!**\n\n" +
                                "📖 Текст: " + question.getQuestionText() + "\n" +
                                "📚 Категория: " + question.getCategory() + "\n" +
                                "🎯 Сложность: " + question.getDifficultyLevel() + "\n\n" +
                                "Вопрос теперь доступен в базе для всех пользователей."
                );

                execute(AnswerCallbackQuery.builder()
                        .callbackQueryId(cq.getId())
                        .text("Сложность выбрана: " + difficultyText)
                        .build());
            }
        } catch (Exception e) {
            e.printStackTrace();
            execSend(cq.getMessage().getChatId(), "❌ Произошла ошибка при сохранении вопроса. Пожалуйста, попробуйте снова.");
            questionSessionService.completeSession(cq.getMessage().getChatId());
        }
    }

    private void showMainMenu(Long chatId) {
        var keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        var addQuestionBtn = InlineKeyboardButton.builder()
                .text("➕ Добавить вопрос")
                .callbackData("ADD_QUESTION")
                .build();

        rows.add(List.of(addQuestionBtn));
        keyboard.setKeyboard(rows);

        execSend(chatId,
                "📋 **Главное меню**\n\n" +
                        "Выберите действие:",
                keyboard
        );
        execSend(chatId, "💡 Также можете использовать постоянные кнопки ниже для быстрого доступа:", getMainReplyKeyboard());
    }

    private void startQuestionSession(Long chatId, BotUser user) {
        int questionsCount = user.getQuestionsPerSession() != null ? user.getQuestionsPerSession() : 20;

        // Проверяем, есть ли достаточное количество вопросов в базе
        var availableQuestions = questionService.getRandomQuestions(1);
        if (availableQuestions.isEmpty()) {
            execSend(chatId, "❌ В базе нет вопросов. Сначала добавьте вопросы с помощью /add_question");
            return;
        }

        questionCacheService.initializeUserCache(chatId, questionsCount);

        // Проверяем, что кэш инициализирован корректно
        if (questionCacheService.getUserCache(chatId) == null ||
                questionCacheService.getUserCache(chatId).getQuestions().isEmpty()) {
            execSend(chatId, "❌ Не удалось загрузить вопросы. Попробуйте позже или уменьшите количество вопросов в настройках.");
            return;
        }

        sendNextQuestion(chatId, true);
    }

    private void sendNextQuestion(Long chatId, boolean isFirstQuestion) {
        Question question = questionCacheService.getCurrentQuestion(chatId);

        if (question == null) {
            execSend(chatId, "❌ Не удалось получить вопрос. Попробуйте снова.");
            questionCacheService.clearUserCache(chatId);
            return;
        }

        var kb = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        var answerBtn = InlineKeyboardButton.builder()
                .text("Показать ответ")
                .callbackData("ANS:" + question.getId())
                .build();
        var replyBtn = InlineKeyboardButton.builder()
                .text("Ответить")
                .callbackData("REPLY:" + question.getId())
                .build();
        var exitBtn = InlineKeyboardButton.builder()
                .text("❌ Выйти из сессии")
                .callbackData("EXIT_SESSION")
                .build();
        rows.add(List.of(answerBtn, replyBtn));
        rows.add(List.of(exitBtn));
        kb.setKeyboard(rows);

        String message = isFirstQuestion ?
                "🎯 **Начинаем сессию вопросов!**\n\n" +
                        "❓ **Вопрос " + (questionCacheService.getUserCache(chatId).getCurrentIndex() + 1) + ":**\n\n" +
                        question.getQuestionText() :
                "❓ **Вопрос " + (questionCacheService.getUserCache(chatId).getCurrentIndex() + 1) + ":**\n\n" +
                        question.getQuestionText();

        execSend(chatId, message, kb);
    }

    private void showContinueOptions(Long chatId) {
        var kb = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        if (questionCacheService.hasNextQuestion(chatId)) {
            var nextBtn = InlineKeyboardButton.builder()
                    .text("✅ Следующий вопрос")
                    .callbackData("NEXT_QUESTION")
                    .build();
            rows.add(List.of(nextBtn));
        }

        var stopBtn = InlineKeyboardButton.builder()
                .text("❌ Завершить")
                .callbackData("STOP_QUESTIONS")
                .build();
        rows.add(List.of(stopBtn));

        kb.setKeyboard(rows);

        String message = questionCacheService.hasNextQuestion(chatId) ?
                "**Что делаем дальше?**" :
                "🎉 **Вы ответили на все вопросы в этой сессии!**";

        execSend(chatId, message, kb);
    }

    private void handleNextQuestion(Long chatId) {
        Question nextQuestion = questionCacheService.getNextQuestion(chatId);
        if (nextQuestion != null) {
            sendNextQuestion(chatId, false);
        } else {
            execSend(chatId, "🎉 **Вы ответили на все вопросы в этой сессии!**", getMainReplyKeyboard());
            questionCacheService.clearUserCache(chatId);
        }
    }

    private void handleStopQuestions(Long chatId) {
        questionCacheService.clearUserCache(chatId);
        execSend(chatId, "🏁 **Сессия вопросов завершена.**\n\n" +
                "Чтобы начать новую сессию, используйте кнопку «🎲 Начать сессию вопросов» или главное меню.", getMainReplyKeyboard());
    }

    private void showSettingsMenu(Long chatId, BotUser user) {
        var keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        var timeBtn = InlineKeyboardButton.builder()
                .text("🕐 Изменить время рассылки")
                .callbackData("SETTINGS_TIME")
                .build();

        var countBtn = InlineKeyboardButton.builder()
                .text("📊 Изменить количество вопросов")
                .callbackData("SETTINGS_COUNT")
                .build();

        var backBtn = InlineKeyboardButton.builder()
                .text("🔙 Назад")
                .callbackData("BACK_TO_MENU")
                .build();

        rows.add(List.of(timeBtn));
        rows.add(List.of(countBtn));
        rows.add(List.of(backBtn));
        keyboard.setKeyboard(rows);

        String currentSettings = "⚙️ **Текущие настройки:**\n\n" +
                "🕐 Время рассылки: " + user.getScheduleTime() + "\n" +
                "📊 Вопросов в сессии: " + (user.getQuestionsPerSession() != null ? user.getQuestionsPerSession() : 20);

        execSend(chatId, currentSettings + "\n\nВыберите, что хотите изменить:", keyboard);
    }

    // Обработка изменения времени
    private void handleSettingsTime(Long chatId) {
        registrationService.updateUserState(chatId, RegistrationState.SCHEDULE_TIME);
        execSend(chatId, "🕐 Введите новое время для ежедневной рассылки в формате HH:mm (например, 14:00):");
    }

    private void handleSettingsCount(Long chatId) {
        registrationService.startQuestionsCountSetting(chatId);
        execSend(chatId, "📊 Введите новое количество вопросов для сессии (от 1 до 50):");
    }

    private boolean handleSettings(Long chatId, String text, BotUser user) {
        // Проверяем, находится ли пользователь в состоянии настройки количества вопросов
        if (registrationService.isInSettingsState(chatId, SettingsState.AWAITING_QUESTIONS_COUNT)) {
            try {
                registrationService.updateQuestionsPerSession(chatId, text);
                execSend(chatId, "✅ Количество вопросов в сессии изменено на: " + text);
                showSettingsMenu(chatId, user);
                return true;
            } catch (IllegalArgumentException e) {
                execSend(chatId, "❌ " + e.getMessage() + "\n\nПожалуйста, введите число от 1 до 50:");
                return true;
            }
        }
        return false;
    }


    private void handleRandomQuestion(CallbackQuery cq) {
        try {
            execute(AnswerCallbackQuery.builder()
                    .callbackQueryId(cq.getId())
                    .text("Начинаем сессию вопросов...")
                    .build());
        } catch (Exception ignored) {
        }

        var chatId = cq.getMessage().getChatId();
        Optional<BotUser> userOpt = registrationService.getUserByChatId(chatId);

        if (userOpt.isPresent()) {
            BotUser user = userOpt.get();
            startQuestionSession(chatId, user);
        } else {
            execSend(chatId, "❌ Сначала зарегистрируйтесь с помощью /start");
        }
    }

    private void handleAddQuestion(CallbackQuery cq) {
        try {
            execute(AnswerCallbackQuery.builder()
                    .callbackQueryId(cq.getId())
                    .text("Переходим к добавлению вопроса...")
                    .build());
        } catch (Exception ignored) {
        }

        startAddingQuestion(cq.getMessage().getChatId());
    }

    private void handleSettingsMenu(CallbackQuery cq) {
        var chatId = cq.getMessage().getChatId();
        Optional<BotUser> userOpt = registrationService.getUserByChatId(chatId);

        if (userOpt.isPresent()) {
            try {
                execute(AnswerCallbackQuery.builder()
                        .callbackQueryId(cq.getId())
                        .text("Открываем настройки...")
                        .build());
            } catch (Exception ignored) {
            }
            showSettingsMenu(chatId, userOpt.get());
        }
    }

    private void handleBackToMenu(Long chatId) {
        showMainMenu(chatId);
    }
}