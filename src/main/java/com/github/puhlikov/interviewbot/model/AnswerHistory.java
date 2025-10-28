package com.github.puhlikov.interviewbot.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "answer_history")
public class AnswerHistory {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id")
	private BotUser user;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "question_id")
	private Question question;

	@Lob
	@Column(name = "deepseek_response")
	private String deepseekResponse;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt = Instant.now();

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public BotUser getUser() {
		return user;
	}

	public void setUser(BotUser user) {
		this.user = user;
	}

	public Question getQuestion() {
		return question;
	}

	public void setQuestion(Question question) {
		this.question = question;
	}

	public String getDeepseekResponse() {
		return deepseekResponse;
	}

	public void setDeepseekResponse(String deepseekResponse) {
		this.deepseekResponse = deepseekResponse;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}
}


