package com.delphi.delphi.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.delphi.delphi.dtos.NewAssessmentDto;
import com.delphi.delphi.entities.Assessment;
import com.delphi.delphi.entities.Candidate;
import com.delphi.delphi.entities.ChatHistory;
import com.delphi.delphi.entities.User;
import com.delphi.delphi.repositories.AssessmentRepository;
import com.delphi.delphi.utils.AssessmentCreationPrompts;
import com.delphi.delphi.utils.AssessmentStatus;
import com.delphi.delphi.utils.AssessmentType;
import com.delphi.delphi.utils.git.GithubAccountType;

@Service
@Transactional
// TODO: add cache annotations for other entity caches
public class AssessmentService {

    private final CandidateService candidateService;

    private final AssessmentRepository assessmentRepository;
    private final UserService userService;
    private final ChatService chatService;
    private final GithubService githubService;

    public AssessmentService(AssessmentRepository assessmentRepository, UserService userService, ChatService chatService, GithubService githubService, CandidateService candidateService) {
        this.assessmentRepository = assessmentRepository;
        this.userService = userService;
        this.chatService = chatService;
        this.githubService = githubService;
        this.candidateService = candidateService;
    }

    // Create a new assessment
    @CachePut(value = "assessments", key = "#assessment.id")
    public Assessment createAssessment(Assessment assessment) {
        // Validate that the associated user exists
        if (assessment.getUser() != null && assessment.getUser().getId() != null) {
            User user = userService.getUserByIdOrThrow(assessment.getUser().getId());
            assessment.setUser(user);
        }

        // Set default status if not provided
        if (assessment.getStatus() == null) {
            assessment.setStatus(AssessmentStatus.DRAFT);
        }

        // Validate date logic
        if (assessment.getStartDate() != null && assessment.getEndDate() != null) {
            if (!assessment.getEndDate().isAfter(assessment.getStartDate())) {
                throw new IllegalArgumentException("End date must be after start date");
            }
        }

        return assessmentRepository.save(assessment);
    }

    @CachePut(value = "assessments", key = "#result.id")
    public Assessment createAssessment(NewAssessmentDto newAssessmentDto, User user) throws Exception {
        Assessment assessment = new Assessment();
        assessment.setName(newAssessmentDto.getName());
        assessment.setDescription(newAssessmentDto.getDescription());
        assessment.setRoleName(newAssessmentDto.getRoleName());
        assessment.setAssessmentType(newAssessmentDto.getAssessmentType());
        assessment.setStartDate(newAssessmentDto.getStartDate());
        assessment.setEndDate(newAssessmentDto.getEndDate());
        assessment.setDuration(newAssessmentDto.getDuration());
        assessment.setSkills(newAssessmentDto.getSkills());
        assessment.setLanguageOptions(newAssessmentDto.getLanguageOptions());
        assessment.setUser(user);

        ChatHistory chatHistory = new ChatHistory();
        chatHistory.setAssessment(assessment);
        chatHistory.setMessages(List.of());

        // create chat history and add system prompt
        chatHistory = chatService.createChatHistory(chatHistory, AssessmentCreationPrompts.SYSTEM_PROMPT);

        // set chat history to assessment
        assessment.setChatHistory(chatHistory);

        // create github repo and add Delphi as a contributor
        if (user.getGithubAccountType() == GithubAccountType.USER) {
            githubService.createPersonalRepo(user.getGithubAccessToken(), assessment.getGithubRepoName());
        } else {
            githubService.createOrgRepo(user.getGithubAccessToken(), user.getGithubUsername(), assessment.getGithubRepoName());
        }
        //githubService.addContributor(user.getGithubAccessToken(), DelphiGithubConstants.DELPHI_GITHUB_NAME, assessment.getName(), user.getGithubUsername());

        // save assessment
        return assessmentRepository.save(assessment);

    }

    // Get assessment by ID
    @Cacheable(value = "assessments", key = "#id")
    @Transactional(readOnly = true)
    public Optional<Assessment> getAssessmentById(Long id) {
        return assessmentRepository.findById(id);
    }

    // Get chat history by assessment ID
    //@Cacheable(value = "chatHistories", key = "#id")
    @Transactional(readOnly = true)
    public ChatHistory getChatHistoryById(Long id) {
        return assessmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Assessment not found with id: " + id))
                .getChatHistory();
    }

    // Get assessment by ID or throw exception
    @Cacheable(value = "assessments", key = "#id")
    @Transactional(readOnly = true)
    public Assessment getAssessmentByIdOrThrow(Long id) {
        return assessmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Assessment not found with id: " + id));
    }

    // Get all assessments with pagination
    @Cacheable(value = "assessments", key = "#pageable.pageNumber + ':' + #pageable.pageSize")
    @Transactional(readOnly = true)
    public Page<Assessment> getAllAssessments(Pageable pageable) {
        return assessmentRepository.findAll(pageable);
    }

    // Get assessments with multiple filters
    @Cacheable(value = "assessments", key = "#user.id + ':' + #status + ':' + #assessmentType + ':' + #startDate + ':' + #endDate + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    @Transactional(readOnly = true)
    public Page<Assessment> getAssessmentsWithFilters(User user, AssessmentStatus status, AssessmentType assessmentType, 
                                                     LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {
        return assessmentRepository.findWithFilters(status, assessmentType, startDate, endDate, pageable);
    }

    // Update assessment
    public Assessment updateAssessment(Long id, Assessment assessmentUpdates) {
        Assessment existingAssessment = getAssessmentByIdOrThrow(id);

        // Update fields if provided
        if (assessmentUpdates.getName() != null) {
            existingAssessment.setName(assessmentUpdates.getName());
        }
        if (assessmentUpdates.getDescription() != null) {
            existingAssessment.setDescription(assessmentUpdates.getDescription());
        }
        if (assessmentUpdates.getRoleName() != null) {
            existingAssessment.setRoleName(assessmentUpdates.getRoleName());
        }
        if (assessmentUpdates.getStatus() != null) {
            existingAssessment.setStatus(assessmentUpdates.getStatus());
        }
        if (assessmentUpdates.getAssessmentType() != null) {
            existingAssessment.setAssessmentType(assessmentUpdates.getAssessmentType());
        }
        if (assessmentUpdates.getStartDate() != null) {
            existingAssessment.setStartDate(assessmentUpdates.getStartDate());
        }
        if (assessmentUpdates.getEndDate() != null) {
            existingAssessment.setEndDate(assessmentUpdates.getEndDate());
        }
        if (assessmentUpdates.getDuration() != null) {
            existingAssessment.setDuration(assessmentUpdates.getDuration());
        }
        if (assessmentUpdates.getGithubRepositoryLink() != null) {
            existingAssessment.setGithubRepositoryLink(assessmentUpdates.getGithubRepositoryLink());
        }
        if (assessmentUpdates.getSkills() != null) {
            existingAssessment.setSkills(assessmentUpdates.getSkills());
        }
        if (assessmentUpdates.getLanguageOptions() != null) {
            existingAssessment.setLanguageOptions(assessmentUpdates.getLanguageOptions());
        }
        if (assessmentUpdates.getMetadata() != null) {
            existingAssessment.setMetadata(assessmentUpdates.getMetadata());
        }

        // Validate date logic after updates
        if (existingAssessment.getStartDate() != null && existingAssessment.getEndDate() != null) {
            if (!existingAssessment.getEndDate().isAfter(existingAssessment.getStartDate())) {
                throw new IllegalArgumentException("End date must be after start date");
            }
        }

        return assessmentRepository.save(existingAssessment);
    }

    // Delete assessment
    @CacheEvict(value = "assessments", beforeInvocation = true, key = "#id")
    public void deleteAssessment(Long id) {
        if (!assessmentRepository.existsById(id)) {
            throw new IllegalArgumentException("Assessment not found with id: " + id);
        }
        assessmentRepository.deleteById(id);
    }

    // Get assessments by user ID
    @Cacheable(value = "assessments", key = "#userId + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    @Transactional(readOnly = true)
    public Page<Assessment> getAssessmentsByUserId(Long userId, Pageable pageable) {
        return assessmentRepository.findByUserId(userId, pageable);
    }

    // Get assessments by status
    @Cacheable(value = "assessments", key = "#status + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    @Transactional(readOnly = true)
    public Page<Assessment> getAssessmentsByStatus(AssessmentStatus status, Pageable pageable) {
        return assessmentRepository.findByStatus(status, pageable);
    }

    // Get assessments by type
    @Cacheable(value = "assessments", key = "#assessmentType + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    @Transactional(readOnly = true)
    public Page<Assessment> getAssessmentsByType(AssessmentType assessmentType, Pageable pageable) {
        return assessmentRepository.findByAssessmentType(assessmentType, pageable);
    }

    // Get assessments by user and status
    @Cacheable(value = "assessments", key = "#userId + ':' + #status + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    @Transactional(readOnly = true)
    public Page<Assessment> getAssessmentsByUserAndStatus(Long userId, AssessmentStatus status, Pageable pageable) {
        return assessmentRepository.findByUserIdAndStatus(userId, status, pageable);
    }

    // Search assessments by name
    @Cacheable(value = "assessments", key = "#user.id + ':' + #name + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    @Transactional(readOnly = true)
    public Page<Assessment> searchAssessmentsByName(User user, String name, Pageable pageable) {
        return assessmentRepository.findByNameContainingIgnoreCase(name, pageable);
    }

    // Search assessments by role name
    @Cacheable(value = "assessments", key = "#user.id + ':' + #roleName + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    @Transactional(readOnly = true)
    public Page<Assessment> searchAssessmentsByRoleName(User user, String roleName, Pageable pageable) {
        return assessmentRepository.findByRoleNameContainingIgnoreCase(roleName, pageable);
    }

    // Get assessments within date range
    @Cacheable(value = "assessments", key = "#startDate + ':' + #endDate + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    @Transactional(readOnly = true)
    public Page<Assessment> getAssessmentsInDateRange(LocalDateTime startDate, LocalDateTime endDate,
            Pageable pageable) {
        return assessmentRepository.findByDateRange(startDate, endDate, pageable);
    }

    // Get active assessments within current date
    @Cacheable(value = "assessments", key = "#currentDate + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    @Transactional(readOnly = true)
    public Page<Assessment> getActiveAssessmentsInDateRange(LocalDateTime currentDate, Pageable pageable) {
        return assessmentRepository.findActiveAssessmentsInDateRange(currentDate, pageable);
    }

    // Get assessments by duration range
    @Cacheable(value = "assessments", key = "#minDuration + ':' + #maxDuration + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    @Transactional(readOnly = true)
    public Page<Assessment> getAssessmentsByDurationRange(Integer minDuration, Integer maxDuration, Pageable pageable) {
        return assessmentRepository.findByDurationBetween(minDuration, maxDuration, pageable);
    }

    // Get assessments by skill
    @Cacheable(value = "assessments", key = "#user.id + ':' + #skill + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    @Transactional(readOnly = true)
    public Page<Assessment> getAssessmentsBySkill(User user, String skill, Pageable pageable) {
        return assessmentRepository.findBySkill(skill, pageable);
    }

    // Get assessments by language option
    @Cacheable(value = "assessments", key = "#language + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    @Transactional(readOnly = true)
    public Page<Assessment> getAssessmentsByLanguageOption(String language, Pageable pageable) {
        return assessmentRepository.findByLanguageOption(language, pageable);
    }

    // Get assessments with attempt count
    @Cacheable(value = "assessments", key = "#pageable.pageNumber + ':' + #pageable.pageSize")
    @Transactional(readOnly = true)
    public Page<Object[]> getAssessmentsWithAttemptCount(Pageable pageable) {
        return assessmentRepository.findAssessmentsWithAttemptCount(pageable);
    }

    // Get assessments created by user in date range
    @Cacheable(value = "assessments", key = "#userId + ':' + #startDate + ':' + #endDate + ':' + #pageable.pageNumber + ':' + #pageable.pageSize")
    @Transactional(readOnly = true)
    public Page<Assessment> getAssessmentsByUserInDateRange(Long userId, LocalDateTime startDate, LocalDateTime endDate,
            Pageable pageable) {
        return assessmentRepository.findByUserIdAndCreatedDateBetween(userId, startDate, endDate, pageable);
    }

    // Count assessments by status for a user
    @Cacheable(value = "assessments", key = "#user.id + ':' + #status + ':' + #count")
    @Transactional(readOnly = true)
    public Long countAssessmentsByUserAndStatus(User user, AssessmentStatus status) {
        return assessmentRepository.countByUserIdAndStatus(user.getId(), status);
    }

    // Activate assessment
    @CacheEvict(value = "assessments", beforeInvocation = true, key = "#id")
    public Assessment activateAssessment(Long id) {
        Assessment assessment = getAssessmentByIdOrThrow(id);
        assessment.setStatus(AssessmentStatus.ACTIVE);
        return assessmentRepository.save(assessment);
    }

    // Deactivate assessment
    @CacheEvict(value = "assessments", beforeInvocation = true, key = "#id")
    public Assessment deactivateAssessment(Long id) {
        Assessment assessment = getAssessmentByIdOrThrow(id);
        assessment.setStatus(AssessmentStatus.INACTIVE);
        return assessmentRepository.save(assessment);
    }

    // Publish assessment (change from draft to active)
    @CacheEvict(value = "assessments", beforeInvocation = true, key = "#id")
    public Assessment publishAssessment(Long id) {
        Assessment assessment = getAssessmentByIdOrThrow(id);
        if (assessment.getStatus() != AssessmentStatus.DRAFT) {
            throw new IllegalStateException("Only draft assessments can be published");
        }
        assessment.setStatus(AssessmentStatus.ACTIVE);
        return assessmentRepository.save(assessment);
    }

    // Add skill to assessment
    @CacheEvict(value = "assessments", beforeInvocation = true, key = "#id")
    public Assessment addSkill(Long id, String skill) {
        Assessment assessment = getAssessmentByIdOrThrow(id);
        if (assessment.getSkills() != null && !assessment.getSkills().contains(skill)) {
            assessment.getSkills().add(skill);
        }
        return assessmentRepository.save(assessment);
    }

    // Add candidate from existing candidate
    @CacheEvict(value = "assessments", beforeInvocation = true, key = "#assessmentId")
    public Candidate addCandidateFromExisting(Long assessmentId, Long candidateId) {
        Assessment assessment = getAssessmentByIdOrThrow(assessmentId);
        Candidate candidate = candidateService.getCandidateByIdOrThrow(candidateId);
        assessment.addCandidate(candidate);
        assessmentRepository.save(assessment);
        return candidate;
    }

    // Add a new candidate to the assessmentthat doesn't already exist in the database
    @CacheEvict(value = "assessments", beforeInvocation = true, key = "#assessmentId")
    public Candidate addCandidateFromNew(Long assessmentId, String firstName, String lastName, String email) {
        Assessment assessment = getAssessmentByIdOrThrow(assessmentId);
        Candidate candidate = candidateService.createCandidate(firstName, lastName, email, assessment.getUser());
        assessment.addCandidate(candidate);
        assessmentRepository.save(assessment);
        return candidate;
    }

    // Remove skill from assessment
    @CacheEvict(value = "assessments", beforeInvocation = true, key = "#id")
    public Assessment removeSkill(Long id, String skill) {
        Assessment assessment = getAssessmentByIdOrThrow(id);
        if (assessment.getSkills() != null) {
            assessment.getSkills().remove(skill);
        }
        return assessmentRepository.save(assessment);
    }

    // Update skills
    @CacheEvict(value = "assessments", beforeInvocation = true, key = "#id")
    public Assessment updateSkills(Long id, List<String> skills) {
        Assessment assessment = getAssessmentByIdOrThrow(id);
        assessment.setSkills(skills);
        return assessmentRepository.save(assessment);
    }

    @CacheEvict(value = "assessments", beforeInvocation = true, key = "#assessmentId")
    public Candidate removeCandidateFromAssessment(Long assessmentId, Long candidateId) {
        Assessment assessment = getAssessmentByIdOrThrow(assessmentId);
        Candidate candidate = candidateService.getCandidateByIdOrThrow(candidateId);
        assessment.setCandidates(assessment.getCandidates().stream().filter(c -> !c.getId().equals(candidateId)).collect(Collectors.toList()));
        assessmentRepository.save(assessment);
        return candidate;
    }

    // Update language options
    @CacheEvict(value = "assessments", beforeInvocation = true, key = "#id")
    public Assessment updateLanguageOptions(Long id, List<String> languageOptions) {
        Assessment assessment = getAssessmentByIdOrThrow(id);
        assessment.setLanguageOptions(languageOptions);
        return assessmentRepository.save(assessment);
    }

    // Update metadata
    @CacheEvict(value = "assessments", beforeInvocation = true, key = "#id")
    public Assessment updateMetadata(Long id, Map<String, String> metadata) {
        Assessment assessment = getAssessmentByIdOrThrow(id);
        assessment.setMetadata(metadata);
        return assessmentRepository.save(assessment);
    }
}
