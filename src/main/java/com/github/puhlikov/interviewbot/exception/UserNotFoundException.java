package com.github.puhlikov.interviewbot.exception;

import com.github.puhlikov.interviewbot.bot.constants.Messages;

/**
 * Исключение, когда пользователь не найден
 */
public class UserNotFoundException extends BotException {
    public UserNotFoundException() {
        super(Messages.USER_NOT_FOUND);
    }
}

