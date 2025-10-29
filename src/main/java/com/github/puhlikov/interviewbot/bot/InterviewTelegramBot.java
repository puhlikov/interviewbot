package com.github.puhlikov.interviewbot.bot;

import com.github.puhlikov.interviewbot.enums.QuestionState;
import com.github.puhlikov.interviewbot.enums.RegistrationState;
import com.github.puhlikov.interviewbot.model.BotUser;
import com.github.puhlikov.interviewbot.model.Question;
import com.github.puhlikov.interviewbot.repo.BotUserRepository;
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
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    public InterviewTelegramBot(
            @Value("${telegram.bot.username}") String username,
            @Value("${telegram.bot.token}") String token,
            BotUserRepository users,
            QuestionService questions,
//            DeepSeekService deepseek,
            RegistrationService registrationService,
            QuestionSessionService questionSessionService,
            QuestionService questionService,
            WorkingApiService workingApiService
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

        System.out.println("🤖 Bot initialized:");
        System.out.println("   Username: " + username);
        System.out.println("   Token: " + (token != null ? token.substring(0, 10) + "..." : "null"));
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

        if (handleQuestionSession(chatId, text)) {
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
                var removeKeyboard = ReplyKeyboardRemove.builder().removeKeyboard(true).build();
                execSend(chatId,
                        "🎉 Регистрация завершена! 🎉\n\n" +
                                "Теперь вы будете получать ежедневные уведомления в " +
                                user.getScheduleTime() + " по времени " + text + "\n\n" +
                                "Когда придет время, я спрошу, хотите ли вы пройти тест.",
                        removeKeyboard
                );
                break;

            case COMPLETED:
                handleCompletedUser(chatId, text, user);
                break;
        }
    }

    private void handleCompletedUser(Long chatId, String text, BotUser user) {
        if ("/question".equalsIgnoreCase(text)) {
            sendQuestion(chatId);
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
        rows.add(List.of(btn));
        kb.setKeyboard(rows);
        execSend(chatId, "❓ Вопрос:\n\n" + question.getQuestionText(), kb);
    }

    private void handleCallback(CallbackQuery cq) {
        var data = cq.getData();
        if (data != null && data.startsWith("ANS:")) {
            var qid = Long.parseLong(data.substring(4));
            questions.getById(qid).ifPresent(q -> {
                try {
                    execute(AnswerCallbackQuery.builder()
                            .callbackQueryId(cq.getId())
                            .text("Генерируем ответ...")
                            .build());
                } catch (Exception ignored) {
                }

                workingApiService.getAnswer(q.getQuestionText()).subscribe(answer -> { // Заменяем deepseek на workingApiService
                    try {
                        String formattedAnswer = "🤖 Ответ:\n\n" + answer;
                        execSend(cq.getMessage().getChatId(), formattedAnswer);
                    } catch (Exception ignored) {
                    }
                });
//                deepseek.getAnswer(q.getQuestionText()).subscribe(answer -> {
//                    try {
//                        String formattedAnswer = "🤖 Ответ:\n\n" + answer;
//                        execSend(cq.getMessage().getChatId(), formattedAnswer);
//                    } catch (Exception ignored) {
//                    }
//                });
            });
        } else if (data != null && data.equals("YES_TEST")) {
            handleTestResponse(cq, true);
        } else if (data != null && data.equals("NO_TEST")) {
            handleTestResponse(cq, false);
        } else if (data != null && data.startsWith("DIFF_")) {
            handleDifficultySelection(cq, data.substring(5));
        } else if (data != null && data.equals("RANDOM_QUESTION")) {
            try {
                execute(AnswerCallbackQuery.builder()
                        .callbackQueryId(cq.getId())
                        .text("Ищем случайный вопрос...")
                        .build());
            } catch (Exception ignored) {
            }
            sendQuestion(cq.getMessage().getChatId());
        } else if (data != null && data.equals("ADD_QUESTION")) {
            try {
                execute(AnswerCallbackQuery.builder()
                        .callbackQueryId(cq.getId())
                        .text("Начинаем добавление вопроса...")
                        .build());
            } catch (Exception ignored) {
            }
            startAddingQuestion(cq.getMessage().getChatId());
        }
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

        var questionBtn = InlineKeyboardButton.builder()
                .text("🎲 Случайный вопрос")
                .callbackData("RANDOM_QUESTION")
                .build();

        var addQuestionBtn = InlineKeyboardButton.builder()
                .text("➕ Добавить вопрос")
                .callbackData("ADD_QUESTION")
                .build();

        rows.add(List.of(questionBtn));
        rows.add(List.of(addQuestionBtn));
        keyboard.setKeyboard(rows);

        execSend(chatId,
                "📋 **Главное меню**\n\n" +
                        "Выберите действие:",
                keyboard
        );
    }

}