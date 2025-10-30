package com.github.puhlikov.interviewbot.service;

import com.github.puhlikov.interviewbot.model.Question;
import com.github.puhlikov.interviewbot.repo.QuestionRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class QuestionService {

	private final QuestionRepository repository;

	public QuestionService(QuestionRepository repository) {
		this.repository = repository;
	}

	public List<Question> getRandomQuestions(int count) {
		return repository.findRandomActive(count);
	}

	public Optional<Question> getById(Long id) {
		return repository.findById(id);
	}

	public void save(Question question) {
		repository.save(question);
	}
}


