package com.github.puhlikov.interviewbot.repo;

import com.github.puhlikov.interviewbot.model.BotUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BotUserRepository extends JpaRepository<BotUser, Long> {
	Optional<BotUser> findByChatId(Long chatId);
}


