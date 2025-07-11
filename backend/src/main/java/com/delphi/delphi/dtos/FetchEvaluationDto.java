package com.delphi.delphi.dtos;

import java.time.LocalDateTime;
import java.util.Map;

import com.delphi.delphi.entities.Evaluation;

public class FetchEvaluationDto {
    private Long id;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    private Long candidateAttemptId;
    private Map<String, String> metadata;
    // private String evaluationStatus;
    // private String evaluationFeedback;
    // private String evaluationScore;
    // private String evaluationComments;
    // private String evaluationRecommendations;
    // private String evaluationImprovements;

    public FetchEvaluationDto() {
    }

    public FetchEvaluationDto(Evaluation evaluation) {
        this.id = evaluation.getId();
        this.createdDate = evaluation.getCreatedDate();
        this.updatedDate = evaluation.getUpdatedDate();
        this.candidateAttemptId = evaluation.getCandidateAttempt().getId();
        this.metadata = evaluation.getMetadata();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getUpdatedDate() {
        return updatedDate;
    }

    public void setUpdatedDate(LocalDateTime updatedDate) {
        this.updatedDate = updatedDate;
    }

    public Long getCandidateAttemptId() {
        return candidateAttemptId;
    }

    public void setCandidateAttemptId(Long candidateAttemptId) {
        this.candidateAttemptId = candidateAttemptId;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }
   
}