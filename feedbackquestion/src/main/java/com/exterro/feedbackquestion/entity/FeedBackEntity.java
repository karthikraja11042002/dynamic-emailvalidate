package com.exterro.feedbackquestion.entity;

import java.util.Arrays;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

@Entity
public class FeedBackEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int feedBackId;
    
    @OneToOne
    @JoinColumn(name = "userId")
    private UserEntity userId;
    
    private String[] answers;

    public FeedBackEntity() {
    }

    public FeedBackEntity(String[] answers) {
       
        this.answers = answers;
    }

    public int getFeedBackId() {
        return feedBackId;
    }

    public void setFeedBackId(int feedBackId) {
        this.feedBackId = feedBackId;
    }

    public UserEntity getUserId() {
        return userId;
    }

    public void setUserId(UserEntity userId) {
        this.userId = userId;
    }

    public String[] getAnswers() {
        return answers;
    }

    public void setAnswers(String[] answers) {
        this.answers = answers;
    }

    @Override
    public String toString() {
        return "FeedBackEntity [feedBackId=" + feedBackId + ", userId=" + userId + ", answers=" + Arrays.toString(answers) + "]";
    }
}
