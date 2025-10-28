package com.github.puhlikov.interviewbot.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "questions")
public class Question {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "question_text", nullable = false, columnDefinition = "text")
	private String questionText;

	@Column(name = "category")
	private String category;

	@Column(name = "difficulty_level")
	private String difficultyLevel;

	@Column(name = "is_active")
	private Boolean isActive = Boolean.TRUE;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt = Instant.now();

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getQuestionText() {
		return questionText;
	}

	public void setQuestionText(String questionText) {
		this.questionText = questionText;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public String getDifficultyLevel() {
		return difficultyLevel;
	}

	public void setDifficultyLevel(String difficultyLevel) {
		this.difficultyLevel = difficultyLevel;
	}

	public Boolean getIsActive() {
		return isActive;
	}

	public void setIsActive(Boolean active) {
		isActive = active;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}
}


