package com.github.puhlikov.interviewbot.repo;

import com.github.puhlikov.interviewbot.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {

	@Query(value = "SELECT * FROM questions WHERE is_active = true ORDER BY random() LIMIT :limit", nativeQuery = true)
	List<Question> findRandomActive(@Param("limit") int limit);
}


