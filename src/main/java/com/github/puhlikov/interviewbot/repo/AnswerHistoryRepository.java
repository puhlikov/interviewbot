package com.github.puhlikov.interviewbot.repo;

import com.github.puhlikov.interviewbot.model.AnswerHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnswerHistoryRepository extends JpaRepository<AnswerHistory, Long> {}


