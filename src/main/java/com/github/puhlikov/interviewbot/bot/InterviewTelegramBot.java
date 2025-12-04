package com.github.puhlikov.interviewbot.bot;

import com.github.puhlikov.interviewbot.bot.constants.ButtonText;
import com.github.puhlikov.interviewbot.bot.constants.CallbackData;
import com.github.puhlikov.interviewbot.bot.constants.Messages;
import com.github.puhlikov.interviewbot.bot.util.KeyboardBuilder;
import com.github.puhlikov.interviewbot.enums.QuestionState;
import com.github.puhlikov.interviewbot.enums.RegistrationState;
import com.github.puhlikov.interviewbot.enums.SettingsState;
import com.github.puhlikov.interviewbot.model.BotUser;
import com.github.puhlikov.interviewbot.model.Question;
import com.github.puhlikov.interviewbot.service.ErrorHandler;
import com.github.puhlikov.interviewbot.service.MessageSender;
import com.github.puhlikov.interviewbot.service.QuestionCacheService;
import com.github.puhlikov.interviewbot.service.QuestionService;
import com.github.puhlikov.interviewbot.service.QuestionSessionService;
import com.github.puhlikov.interviewbot.service.RegistrationService;
import com.github.puhlikov.interviewbot.service.WorkingApiService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InterviewTelegramBot extends TelegramLongPollingBot {

    private static final Logger logger = LoggerFactory.getLogger(InterviewTelegramBot.class);
    
    private final String username;
    private final QuestionService questionService;
    private final RegistrationService registrationService;
    private final QuestionSessionService questionSessionService;
    private final WorkingApiService workingApiService;
    private final QuestionCacheService questionCacheService;
    private final MessageSender messageSender;
    private final ErrorHandler errorHandler;
    private final Set<Long> awaitingText = ConcurrentHashMap.newKeySet();

    public InterviewTelegramBot(
            @Value("${telegram.bot.username}") String username,
            @Value("${telegram.bot.token}") String token,
            QuestionService questionService,
            RegistrationService registrationService,
            QuestionSessionService questionSessionService,
            WorkingApiService workingApiService,
            QuestionCacheService questionCacheService,
            MessageSender messageSender,
            ErrorHandler errorHandler
    ) {
        super(token);
        this.username = username;
        this.questionService = questionService;
        this.registrationService = registrationService;
        this.questionSessionService = questionSessionService;
        this.workingApiService = workingApiService;
        this.questionCacheService = questionCacheService;
        this.messageSender = messageSender;
        this.errorHandler = errorHandler;
        this.messageSender.setBot(this);
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
            Long chatId = update.hasMessage() 
                ? update.getMessage().getChatId() 
                : (update.hasCallbackQuery() ? update.getCallbackQuery().getMessage().getChatId() : null);
            if (chatId != null) {
                errorHandler.handleError(chatId, e);
            } else {
                errorHandler.handleErrorSilently(e);
            }
        }
    }

    private void handleMessage(Update update) {
        var msg = update.getMessage();
        var chatId = msg.getChatId();
        var text = msg.getText().trim();

        Optional<BotUser> userOpt = registrationService.getUserByChatId(chatId);

        if (!userOpt.isPresent() || userOpt.get().getRegistrationState() == RegistrationState.START) {
            if ("/start".equalsIgnoreCase(text)) {
                startRegistration(update);
            } else {
                execSend(chatId, Messages.START_COMMAND_REQUIRED);
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

        // Если пользователь в сессии вопросов и пишет текст (не команду) - автоматически обрабатываем как ответ
        if (questionCacheService.getUserCache(chatId) != null && 
            !text.startsWith("/") &&
            !registrationService.isInSettingsState(chatId, SettingsState.AWAITING_QUESTIONS_COUNT)) {
            
            var cache = questionCacheService.getUserCache(chatId);
            Question currentQuestion = cache.getCurrentQuestion();
            
            if (currentQuestion != null) {
                // Удаляем из awaitingText, если там был (на случай, если нажали кнопку)
                awaitingText.remove(chatId);
                
                execSend(chatId, "⏳ Оцениваю ваш ответ...");
                
                // Оцениваем ответ пользователя
                workingApiService.evaluateAnswer(currentQuestion.getQuestionText(), text)
                    .subscribe(evaluation -> {
                        // Сохраняем оценку только в кэш сессии
                        cache.addScore(evaluation.getScore());
                        
                        // Формируем сообщение с оценкой и дополнениями
                        StringBuilder message = new StringBuilder();
                        message.append(String.format("✅ **Ваш ответ оценен: %d/10**\n\n", evaluation.getScore()));
                        message.append(String.format("📝 **Ваш ответ:** %s\n\n", text));
                        
                        if (evaluation.hasFeedback()) {
                            message.append("💡 **Дополнения и рекомендации:**\n");
                            message.append(evaluation.getFeedback());
                        }
                        
                        execSend(chatId, message.toString());
                        
                        // Проверяем, был ли это последний вопрос
                        if (questionCacheService.isLastQuestion(chatId)) {
                            // Это был последний вопрос - завершаем сессию
                            finishQuestionSession(chatId);
                        } else {
                            showContinueOptions(chatId);
                        }
                    }, error -> {
                        errorHandler.handleErrorWithMessage(chatId, error, 
                            "❌ Произошла ошибка при оценке ответа. Попробуйте еще раз.");
                        showContinueOptions(chatId);
                    });
            }
            return;
        }

        if (user.getRegistrationState() == RegistrationState.COMPLETED) {
            handleCompletedUser(chatId, text, user);
        } else {
            handleRegistrationStep(chatId, text, user);
        }
    }

    private void startRegistration(Update update) {
        var msg = update.getMessage();
        var chatId = msg.getChatId();
        var from = msg.getFrom();
        
        // Получаем данные из Telegram
        String firstName = from.getFirstName() != null ? from.getFirstName() : "";
        String lastName = from.getLastName() != null ? from.getLastName() : "";
        String username = from.getUserName() != null ? from.getUserName() : "";
        
        // Автоматически заполняем данные из Telegram
        registrationService.startRegistration(chatId, firstName, lastName, username);
        
        // Формируем приветственное сообщение с данными пользователя
        StringBuilder welcomeMessage = new StringBuilder("Добро пожаловать! 👋\n\n");
        welcomeMessage.append("Ваши данные из Telegram:\n");
        if (!firstName.isEmpty()) {
            welcomeMessage.append("Имя: ").append(firstName).append("\n");
        }
        if (!lastName.isEmpty()) {
            welcomeMessage.append("Фамилия: ").append(lastName).append("\n");
        }
        if (!username.isEmpty()) {
            welcomeMessage.append("Username: @").append(username).append("\n");
        }
        welcomeMessage.append("\nТеперь введите время для ежедневной рассылки в формате HH:mm (например, 14:00):");
        
        execSend(chatId, welcomeMessage.toString());
    }

    private void handleRegistrationStep(Long chatId, String text, BotUser user) {
        switch (user.getRegistrationState()) {
            case START:
            case SCHEDULE_TIME:
                try {
                    registrationService.updateScheduleTime(chatId, text);
                    var keyboard = registrationService.getTimezoneKeyboard();
                    execSend(chatId, Messages.ENTER_TIME, keyboard);
                } catch (IllegalArgumentException e) {
                    errorHandler.handleError(chatId, e);
                }
                break;

            case TIMEZONE:
                registrationService.updateTimezone(chatId, text);
                int questionsPerSession = user.getQuestionsPerSession() != null 
                    ? user.getQuestionsPerSession() 
                    : com.github.puhlikov.interviewbot.bot.constants.AppConstants.DEFAULT_QUESTIONS_PER_SESSION;
                execSend(chatId,
                        Messages.registrationComplete(user.getScheduleTime().toString(), text, questionsPerSession),
                        KeyboardBuilder.createMainReplyKeyboard()
                );
                break;

            case COMPLETED:
                handleCompletedUser(chatId, text, user);
                break;
        }
    }

    private void handleCompletedUser(Long chatId, String text, BotUser user) {
        // Обработка постоянных кнопок
        if (ButtonText.START_SESSION.equals(text) || "/question".equalsIgnoreCase(text)) {
            startQuestionSession(chatId, user);
        } else if (ButtonText.STOP_SESSION.equals(text)) {
            finishQuestionSession(chatId);
        } else if (ButtonText.SETTINGS.equals(text) || "/settings".equalsIgnoreCase(text)) {
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
        var questionList = questionService.getRandomQuestions(1);
        if (questionList.isEmpty()) {
            execSend(chatId, Messages.NO_QUESTIONS_IN_DB);
            return;
        }

        Question question = questionList.get(0);
        var keyboard = KeyboardBuilder.createSimpleQuestionKeyboard(question.getId());
        execSend(chatId, Messages.QUESTION_PREFIX + question.getQuestionText(), keyboard);
    }

    private void handleCallback(CallbackQuery cq) {
        var data = cq.getData();
        var chatId = cq.getMessage().getChatId();

        if (data == null) {
            return;
        }

        if (data.startsWith(CallbackData.ANSWER_PREFIX)) {
            handleAnswerCallback(cq, data.substring(CallbackData.ANSWER_PREFIX.length()));
        } else if (data.startsWith(CallbackData.DIFFICULTY_PREFIX)) {
            handleDifficultySelection(cq, data.substring(CallbackData.DIFFICULTY_PREFIX.length()));
        } else {
            switch (data) {
                case CallbackData.YES_TEST -> handleTestResponse(cq, true);
                case CallbackData.NO_TEST -> handleTestResponse(cq, false);
                case CallbackData.RANDOM_QUESTION -> handleRandomQuestion(cq);
                case CallbackData.ADD_QUESTION -> handleAddQuestion(cq);
                case CallbackData.NEXT_QUESTION -> handleNextQuestion(chatId);
                case CallbackData.STOP_QUESTIONS, CallbackData.EXIT_SESSION -> handleStopQuestions(chatId);
                case CallbackData.SETTINGS_TIME -> handleSettingsTime(chatId);
                case CallbackData.SETTINGS_COUNT -> handleSettingsCount(chatId);
                case CallbackData.SETTINGS_MENU -> handleSettingsMenu(cq);
                case CallbackData.BACK_TO_MENU -> handleBackToMenu(chatId);
            }
        }
    }

    private void handleAnswerCallback(CallbackQuery cq, String questionId) {
        try {
            var qid = Long.parseLong(questionId);
            var chatId = cq.getMessage().getChatId();
            questionService.getById(qid).ifPresent(q -> {
                answerCallback(cq, Messages.GENERATING_ANSWER);
                
                // Проверяем, находится ли пользователь в сессии вопросов
                var cache = questionCacheService.getUserCache(chatId);
                if (cache != null) {
                    // Пользователь в сессии - ставим оценку 0 только в кэш
                    cache.addScore(0);
                }
                
                workingApiService.getAnswer(q.getQuestionText()).subscribe(answer -> {
                    execSend(chatId, Messages.formattedAnswer(answer));
                    if (cache != null) {
                        execSend(chatId, "⚠️ Поскольку вы посмотрели ответ, за этот вопрос поставлена оценка **0/10**");
                        
                        // Проверяем, был ли это последний вопрос
                        if (questionCacheService.isLastQuestion(chatId)) {
                            // Это был последний вопрос - завершаем сессию
                            finishQuestionSession(chatId);
                        } else {
                            showContinueOptions(chatId);
                        }
                    } else {
                        showContinueOptions(chatId);
                    }
                });
            });
        } catch (NumberFormatException e) {
            errorHandler.handleErrorSilently(e);
        }
    }

    private void handleTestResponse(CallbackQuery cq, boolean acceptTest) {
        if (acceptTest) {
            answerCallback(cq, Messages.STARTING_TEST);
            sendQuestion(cq.getMessage().getChatId());
        } else {
            answerCallback(cq, Messages.DECLINED_TEST);
        }
    }

    public void sendDailyNotification(Long chatId) {
        var keyboard = KeyboardBuilder.createDailyNotificationKeyboard();
        execSend(chatId, Messages.DAILY_TEST_PROMPT, keyboard);
    }

    private ReplyKeyboardMarkup getMainReplyKeyboard() {
        return KeyboardBuilder.createMainReplyKeyboard();
    }

    private void execSend(Long chatId, String text) {
        execSend(chatId, text, null);
    }

    private void execSend(Long chatId, String text, Object replyMarkup) {
        messageSender.sendMessage(chatId, text, (ReplyKeyboard) replyMarkup);
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
        execSend(chatId, Messages.ADDING_QUESTION);
    }

    private void handleAwaitingQuestionText(Long chatId, String text) {
        // Проверяем, относится ли вопрос к программированию
        execSend(chatId, Messages.CHECKING_QUESTION);
        
        workingApiService.isProgrammingRelated(text)
            .subscribe(isRelated -> {
                if (isRelated) {
                    questionSessionService.setQuestionText(chatId, text);
                    questionSessionService.updateState(chatId, QuestionState.AWAITING_QUESTION_CATEGORY);
                    execSend(chatId, Messages.ENTER_CATEGORY);
                } else {
                    execSend(chatId, Messages.QUESTION_NOT_PROGRAMMING_RELATED);
                }
            }, error -> {
                logger.error("Error checking if question is programming-related for chatId: {}", chatId, error);
                // В случае ошибки разрешаем вопрос, чтобы не блокировать пользователя
                questionSessionService.setQuestionText(chatId, text);
                questionSessionService.updateState(chatId, QuestionState.AWAITING_QUESTION_CATEGORY);
                execSend(chatId, Messages.ENTER_CATEGORY);
            });
    }

    private void handleAwaitingQuestionCategory(Long chatId, String text) {
        questionSessionService.setCategory(chatId, text);
        questionSessionService.updateState(chatId, QuestionState.AWAITING_QUESTION_DIFFICULTY);
        var keyboard = KeyboardBuilder.createDifficultyKeyboard();
        execSend(chatId, Messages.SELECT_DIFFICULTY, keyboard);
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

                try {
                    questionService.save(question);
                    questionSessionService.completeSession(chatId);

                    execSend(chatId, Messages.questionAdded(
                            question.getQuestionText(),
                            question.getCategory(),
                            question.getDifficultyLevel()
                    ));

                    answerCallback(cq, "Сложность выбрана: " + difficultyText);
                } catch (org.springframework.dao.DataIntegrityViolationException e) {
                    // Обработка ошибок целостности данных (например, дублирование ключа)
                    logger.error("Failed to save question due to database constraint violation", e);
                    errorHandler.handleErrorWithMessage(chatId, e, 
                        "❌ Ошибка при сохранении вопроса. Возможно, произошла ошибка базы данных. Попробуйте еще раз.");
                    questionSessionService.completeSession(chatId);
                } catch (Exception e) {
                    logger.error("Unexpected error while saving question", e);
                    errorHandler.handleErrorWithMessage(chatId, e, Messages.ERROR_SAVING_QUESTION);
                    questionSessionService.completeSession(chatId);
                }
            }
        } catch (Exception e) {
            errorHandler.handleErrorWithMessage(cq.getMessage().getChatId(), e, Messages.ERROR_SAVING_QUESTION);
            questionSessionService.completeSession(cq.getMessage().getChatId());
        }
    }

    private void showMainMenu(Long chatId) {
        var keyboard = KeyboardBuilder.createMainMenuKeyboard();
        execSend(chatId, Messages.MAIN_MENU_TITLE, keyboard);
        execSend(chatId, Messages.USE_PERSISTENT_BUTTONS, KeyboardBuilder.createMainReplyKeyboard());
    }

    private void startQuestionSession(Long chatId, BotUser user) {
        int questionsCount = user.getQuestionsPerSession() != null 
            ? user.getQuestionsPerSession() 
            : com.github.puhlikov.interviewbot.bot.constants.AppConstants.DEFAULT_QUESTIONS_PER_SESSION;

        // Проверяем, есть ли достаточное количество вопросов в базе
        var availableQuestions = questionService.getRandomQuestions(1);
        if (availableQuestions.isEmpty()) {
            execSend(chatId, Messages.NO_QUESTIONS_FOR_SESSION);
            return;
        }

        questionCacheService.initializeUserCache(chatId, questionsCount);

        // Проверяем, что кэш инициализирован корректно
        if (questionCacheService.getUserCache(chatId) == null ||
                questionCacheService.getUserCache(chatId).getQuestions().isEmpty()) {
            execSend(chatId, Messages.FAILED_TO_LOAD_QUESTIONS);
            return;
        }

        // Обновляем клавиатуру на "Закончить сессию"
        var cache = questionCacheService.getUserCache(chatId);
        int totalQuestions = cache != null ? cache.getQuestions().size() : questionsCount;
        String sessionStartMessage = String.format(
            "✅ **Сессия начата!**\n\n" +
            "📊 Количество вопросов в сессии: **%d**\n\n" +
            "Используйте кнопки ниже для управления.",
            totalQuestions
        );
        execSend(chatId, sessionStartMessage, KeyboardBuilder.createSessionReplyKeyboard());
        
        sendNextQuestion(chatId, true, questionsCount);
    }

    private void sendNextQuestion(Long chatId, boolean isFirstQuestion) {
        sendNextQuestion(chatId, isFirstQuestion, null);
    }
    
    private void sendNextQuestion(Long chatId, boolean isFirstQuestion, Integer totalQuestions) {
        Question question = questionCacheService.getCurrentQuestion(chatId);

        if (question == null) {
            execSend(chatId, Messages.FAILED_TO_GET_QUESTION);
            questionCacheService.clearUserCache(chatId);
            return;
        }

        var keyboard = KeyboardBuilder.createQuestionKeyboard(question.getId());
        var cache = questionCacheService.getUserCache(chatId);
        int questionNumber = cache != null ? cache.getCurrentIndex() + 1 : 1;
        
        String message;
        if (isFirstQuestion) {
            int total = totalQuestions != null ? totalQuestions : (cache != null ? cache.getQuestions().size() : 1);
            message = String.format(
                "%s\n\n📊 Вопросов в сессии: **%d**\n\n%s%s",
                Messages.QUESTION_SESSION_STARTED,
                total,
                Messages.questionNumber(questionNumber),
                question.getQuestionText()
            );
        } else {
            message = Messages.questionNumber(questionNumber) + question.getQuestionText();
        }

        execSend(chatId, message, keyboard);
    }

    private void showContinueOptions(Long chatId) {
        boolean hasNext = questionCacheService.hasNextQuestion(chatId);
        var keyboard = KeyboardBuilder.createContinueKeyboard(hasNext);
        String message = hasNext ? Messages.WHAT_NEXT : Messages.SESSION_COMPLETED;
        
        // Если сессия завершена (нет следующего вопроса), завершаем сессию
        if (!hasNext) {
            finishQuestionSession(chatId);
            return;
        }
        
        execSend(chatId, message, keyboard);
    }

    private void handleNextQuestion(Long chatId) {
        Question nextQuestion = questionCacheService.getNextQuestion(chatId);
        if (nextQuestion != null) {
            sendNextQuestion(chatId, false);
        } else {
            // Сессия завершена автоматически - показываем среднюю оценку
            finishQuestionSession(chatId);
        }
    }

    private void handleStopQuestions(Long chatId) {
        finishQuestionSession(chatId);
    }
    
    /**
     * Завершает сессию вопросов и показывает среднюю оценку
     */
    private void finishQuestionSession(Long chatId) {
        var cache = questionCacheService.getUserCache(chatId);
        
        if (cache == null) {
            execSend(chatId, "❌ Активная сессия не найдена.", KeyboardBuilder.createMainReplyKeyboard());
            return;
        }
        
        // Вычисляем среднюю оценку на основе отвеченных вопросов
        String completionMessage;
        int totalQuestions = cache.getTotalQuestions();
        int answeredCount = cache.getScores().size();
        
        if (!cache.getScores().isEmpty()) {
            double averageScore = cache.getAverageScore();
            
            completionMessage = String.format(
                "🏁 **Сессия вопросов завершена!**\n\n" +
                "📊 **Ваш результат:**\n" +
                "• Средняя оценка: **%.1f/10**\n" +
                "• Отвечено вопросов: **%d из %d**\n\n" +
                "Спасибо за прохождение сессии!",
                averageScore, answeredCount, totalQuestions
            );
        } else {
            completionMessage = String.format(
                "🏁 **Сессия вопросов завершена!**\n\n" +
                "📊 Вопросов в сессии: **%d**\n\n" +
                "Вы не ответили ни на один вопрос.",
                totalQuestions
            );
        }
        
        execSend(chatId, completionMessage, KeyboardBuilder.createMainReplyKeyboard());
        questionCacheService.clearUserCache(chatId);
    }

    private void showSettingsMenu(Long chatId, BotUser user) {
        var keyboard = KeyboardBuilder.createSettingsKeyboard();
        int questionsPerSession = user.getQuestionsPerSession() != null 
            ? user.getQuestionsPerSession() 
            : com.github.puhlikov.interviewbot.bot.constants.AppConstants.DEFAULT_QUESTIONS_PER_SESSION;
        String currentSettings = Messages.currentSettings(
                user.getScheduleTime().toString(),
                questionsPerSession
        );
        execSend(chatId, currentSettings + "\n\n" + Messages.SELECT_SETTING_TO_CHANGE, keyboard);
    }

    private void handleSettingsTime(Long chatId) {
        registrationService.updateUserState(chatId, RegistrationState.SCHEDULE_TIME);
        execSend(chatId, Messages.ENTER_NEW_TIME);
    }

    private void handleSettingsCount(Long chatId) {
        registrationService.startQuestionsCountSetting(chatId);
        execSend(chatId, Messages.ENTER_QUESTIONS_COUNT);
    }

    private boolean handleSettings(Long chatId, String text, BotUser user) {
        // Проверяем, находится ли пользователь в состоянии настройки количества вопросов
        if (registrationService.isInSettingsState(chatId, SettingsState.AWAITING_QUESTIONS_COUNT)) {
            try {
                // Обновляем количество вопросов и получаем обновленного пользователя
                BotUser updatedUser = registrationService.updateQuestionsPerSession(chatId, text);
                execSend(chatId, String.format(Messages.QUESTIONS_COUNT_UPDATED, text));
                // Используем обновленного пользователя для показа настроек
                showSettingsMenu(chatId, updatedUser);
                return true;
            } catch (IllegalArgumentException e) {
                errorHandler.handleError(chatId, e);
                return true;
            }
        }
        return false;
    }


    private void handleRandomQuestion(CallbackQuery cq) {
        answerCallback(cq, Messages.STARTING_SESSION);
        var chatId = cq.getMessage().getChatId();
        Optional<BotUser> userOpt = registrationService.getUserByChatId(chatId);

        if (userOpt.isPresent()) {
            startQuestionSession(chatId, userOpt.get());
        } else {
            errorHandler.handleError(chatId, new com.github.puhlikov.interviewbot.exception.UserNotFoundException());
        }
    }

    private void handleAddQuestion(CallbackQuery cq) {
        answerCallback(cq, Messages.ADDING_QUESTION_START);
        startAddingQuestion(cq.getMessage().getChatId());
    }

    private void handleSettingsMenu(CallbackQuery cq) {
        var chatId = cq.getMessage().getChatId();
        Optional<BotUser> userOpt = registrationService.getUserByChatId(chatId);

        if (userOpt.isPresent()) {
            answerCallback(cq, Messages.OPENING_SETTINGS);
            showSettingsMenu(chatId, userOpt.get());
        } else {
            errorHandler.handleError(chatId, new com.github.puhlikov.interviewbot.exception.UserNotFoundException());
        }
    }
    
    private void answerCallback(CallbackQuery cq, String text) {
        try {
            execute(AnswerCallbackQuery.builder()
                    .callbackQueryId(cq.getId())
                    .text(text)
                    .build());
        } catch (Exception e) {
            errorHandler.handleErrorSilently(e);
        }
    }

    private void handleBackToMenu(Long chatId) {
        showMainMenu(chatId);
    }
}