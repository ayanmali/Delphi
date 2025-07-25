package com.delphi.delphi.controllers;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.delphi.delphi.dtos.FetchCandidateDto;
import com.delphi.delphi.dtos.NewCandidateDto;
import com.delphi.delphi.entities.Candidate;
import com.delphi.delphi.entities.User;
import com.delphi.delphi.services.AssessmentService;
import com.delphi.delphi.services.CandidateService;
import com.delphi.delphi.services.UserService;
import com.delphi.delphi.utils.AttemptStatus;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/candidates")
public class CandidateController {

    private final AssessmentService assessmentService;
    
    private final UserService userService;
    private final CandidateService candidateService;

    public CandidateController(CandidateService candidateService, UserService userService, AssessmentService assessmentService) {
        this.candidateService = candidateService;
        this.userService = userService;
        this.assessmentService = assessmentService;
    }

    private User getCurrentUser() {
        return userService.getUserByEmail(getCurrentUserEmail());
    }

    private String getCurrentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return userDetails.getUsername();
    }
    
    // Create a new candidate
    @PostMapping("/new")
    public ResponseEntity<?> createCandidate(@Valid @RequestBody NewCandidateDto newCandidateDto) {
        try {
            Candidate candidate = new Candidate();
            candidate.setFirstName(newCandidateDto.getFirstName());
            candidate.setLastName(newCandidateDto.getLastName());
            candidate.setEmail(newCandidateDto.getEmail());
            
            // Set user relationship
            User user = getCurrentUser();
            candidate.setUser(user);
            
            Candidate createdCandidate = candidateService.createCandidate(candidate);
            return ResponseEntity.status(HttpStatus.CREATED).body(new FetchCandidateDto(createdCandidate));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Error creating candidate: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Internal server error: " + e.getMessage());
        }
    }

    /*
     * Add a new candidate and invite them to an assessment
     */
    @PostMapping("/add-and-invite/{assessmentId}")
    public ResponseEntity<?> addAndInviteCandidate(@Valid @RequestBody NewCandidateDto newCandidateDto, @PathVariable Long assessmentId) {
        try {
            Candidate candidate = assessmentService.addCandidateFromNew(assessmentId, newCandidateDto.getFirstName(), newCandidateDto.getLastName(), newCandidateDto.getEmail());
            return ResponseEntity.status(HttpStatus.CREATED).body(new FetchCandidateDto(candidate));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Error creating candidate: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Internal server error: " + e.getMessage());
        }
    }
    
    // Get candidate by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getCandidateById(@PathVariable Long id) {
        try {
            Optional<Candidate> candidate = candidateService.getCandidateById(id);
            if (candidate.isPresent()) {
                return ResponseEntity.ok(new FetchCandidateDto(candidate.get()));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error retrieving candidate: " + e.getMessage());
        }
    }
    
    // Get candidate by email
    @GetMapping("/email/{email}")
    public ResponseEntity<?> getCandidateByEmail(@PathVariable String email) {
        try {
            Optional<Candidate> candidate = candidateService.getCandidateByEmail(email);
            if (candidate.isPresent()) {
                return ResponseEntity.ok(new FetchCandidateDto(candidate.get()));
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error retrieving candidate: " + e.getMessage());
        }
    }
    
    // Get all candidates with pagination and filtering
    @GetMapping("/filter")
    public ResponseEntity<?> getAllCandidates(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection,
            @RequestParam(required = false) Long assessmentId,
            @RequestParam(required = false) AttemptStatus attemptStatus,
            @RequestParam(required = false) String emailDomain,
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdAfter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdBefore,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime attemptCompletedAfter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime attemptCompletedBefore) {
        try {
            Sort sort = sortDirection.equalsIgnoreCase("desc") 
                ? Sort.by(sortBy).descending() 
                : Sort.by(sortBy).ascending();
            
            Pageable pageable = PageRequest.of(page, size, sort);
            Page<Candidate> candidates = candidateService.getCandidatesWithFilters(
                assessmentId, attemptStatus, emailDomain, firstName, lastName,
                createdAfter, createdBefore, attemptCompletedAfter, attemptCompletedBefore, pageable);
            Page<FetchCandidateDto> candidateDtos = candidates.map(FetchCandidateDto::new);
            
            return ResponseEntity.ok(candidateDtos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error retrieving candidates: " + e.getMessage());
        }
    }
    
    // Update candidate
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCandidate(@PathVariable Long id, @Valid @RequestBody NewCandidateDto candidateUpdates) {
        try {
            Candidate updateCandidate = new Candidate();
            updateCandidate.setFirstName(candidateUpdates.getFirstName());
            updateCandidate.setLastName(candidateUpdates.getLastName());
            updateCandidate.setEmail(candidateUpdates.getEmail());
            
            Candidate updatedCandidate = candidateService.updateCandidate(id, updateCandidate);
            return ResponseEntity.ok(new FetchCandidateDto(updatedCandidate));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Error updating candidate: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error updating candidate: " + e.getMessage());
        }
    }
    
    // Delete candidate
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCandidate(@PathVariable Long id) {
        try {
            candidateService.deleteCandidate(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error deleting candidate: " + e.getMessage());
        }
    }
    
    // Get candidates by user ID
    // @GetMapping("/get")
    // public ResponseEntity<?> getCandidatesByUser(
    //         @RequestParam(defaultValue = "0") int page,
    //         @RequestParam(defaultValue = "10") int size) {
    //     try {
    //         User user = getCurrentUser();
    //         Pageable pageable = PageRequest.of(page, size);
    //         Page<Candidate> candidates = candidateService.getCandidatesByUserId(user.getId(), pageable);
    //         Page<FetchCandidateDto> candidateDtos = candidates.map(FetchCandidateDto::new);
            
    //         return ResponseEntity.ok(candidateDtos);
    //     } catch (Exception e) {
    //         return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
    //             .body("Error retrieving candidates: " + e.getMessage());
    //     }
    // }
    
    // Search candidates by first name
    @GetMapping("/search/first-name")
    public ResponseEntity<?> searchCandidatesByFirstName(
            @RequestParam String firstName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Candidate> candidates = candidateService.searchCandidatesByFirstName(firstName, pageable);
            Page<FetchCandidateDto> candidateDtos = candidates.map(FetchCandidateDto::new);
            
            return ResponseEntity.ok(candidateDtos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error searching candidates: " + e.getMessage());
        }
    }
    
    // Search candidates by last name
    @GetMapping("/search/last-name")
    public ResponseEntity<?> searchCandidatesByLastName(
            @RequestParam String lastName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Candidate> candidates = candidateService.searchCandidatesByLastName(lastName, pageable);
            Page<FetchCandidateDto> candidateDtos = candidates.map(FetchCandidateDto::new);
            
            return ResponseEntity.ok(candidateDtos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error searching candidates: " + e.getMessage());
        }
    }
    
    // Search candidates by full name
    @GetMapping("/search/full-name")
    public ResponseEntity<?> searchCandidatesByFullName(
            @RequestParam String fullName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Candidate> candidates = candidateService.searchCandidatesByFullName(fullName, pageable);
            Page<FetchCandidateDto> candidateDtos = candidates.map(FetchCandidateDto::new);
            
            return ResponseEntity.ok(candidateDtos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error searching candidates: " + e.getMessage());
        }
    }
    
    // Get candidates by email domain
    @GetMapping("/domain/{domain}")
    public ResponseEntity<?> getCandidatesByEmailDomain(
            @PathVariable String domain,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Candidate> candidates = candidateService.getCandidatesByEmailDomain(domain, pageable);
            Page<FetchCandidateDto> candidateDtos = candidates.map(FetchCandidateDto::new);
            
            return ResponseEntity.ok(candidateDtos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error retrieving candidates: " + e.getMessage());
        }
    }
    
    // Get candidates created within date range
    // @GetMapping("/created-between")
    // public ResponseEntity<?> getCandidatesCreatedBetween(
    //         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
    //         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
    //         @RequestParam(defaultValue = "0") int page,
    //         @RequestParam(defaultValue = "10") int size) {
    //     try {
    //         Pageable pageable = PageRequest.of(page, size);
    //         Page<Candidate> candidates = candidateService.getCandidatesCreatedBetween(startDate, endDate, pageable);
    //         Page<FetchCandidateDto> candidateDtos = candidates.map(FetchCandidateDto::new);
            
    //         return ResponseEntity.ok(candidateDtos);
    //     } catch (Exception e) {
    //         return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
    //             .body("Error retrieving candidates: " + e.getMessage());
    //     }
    // }
    
    // Get candidates by assessment ID
    // @GetMapping("/assessment/{assessmentId}")
    // public ResponseEntity<?> getCandidatesByAssessmentId(
    //         @PathVariable Long assessmentId,
    //         @RequestParam(defaultValue = "0") int page,
    //         @RequestParam(defaultValue = "10") int size) {
    //     try {
    //         Pageable pageable = PageRequest.of(page, size);
    //         Page<Candidate> candidates = candidateService.getCandidatesByAssessmentId(assessmentId, pageable);
    //         Page<FetchCandidateDto> candidateDtos = candidates.map(FetchCandidateDto::new);
            
    //         return ResponseEntity.ok(candidateDtos);
    //     } catch (Exception e) {
    //         return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
    //             .body("Error retrieving candidates: " + e.getMessage());
    //     }
    // }
    
    // Get candidates with no attempts
    @GetMapping("/no-attempts")
    public ResponseEntity<?> getCandidatesWithNoAttempts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Candidate> candidates = candidateService.getCandidatesWithNoAttempts(pageable);
            Page<FetchCandidateDto> candidateDtos = candidates.map(FetchCandidateDto::new);
            
            return ResponseEntity.ok(candidateDtos);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error retrieving candidates: " + e.getMessage());
        }
    }
    
    // Count candidates by user
    @GetMapping("/count")
    public ResponseEntity<?> countCandidatesByUser() {
        try {
            User user = getCurrentUser();
            Long count = candidateService.countCandidatesByUser(user.getId());
            return ResponseEntity.ok(count);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error counting candidates: " + e.getMessage());
        }
    }
    
    // Update candidate metadata
    @PutMapping("/{id}/metadata")
    public ResponseEntity<?> updateCandidateMetadata(
            @PathVariable Long id,
            @RequestBody Map<String, String> metadata) {
        try {
            Candidate updatedCandidate = candidateService.updateCandidateMetadata(id, metadata);
            return ResponseEntity.ok(new FetchCandidateDto(updatedCandidate));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Error updating metadata: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error updating metadata: " + e.getMessage());
        }
    }
    
    // Add metadata entry
    @PostMapping("/{id}/metadata")
    public ResponseEntity<?> addMetadata(
            @PathVariable Long id,
            @RequestParam String key,
            @RequestParam String value) {
        try {
            Candidate updatedCandidate = candidateService.addMetadata(id, key, value);
            return ResponseEntity.ok(new FetchCandidateDto(updatedCandidate));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Error adding metadata: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error adding metadata: " + e.getMessage());
        }
    }
    
    // Remove metadata entry
    @DeleteMapping("/{id}/metadata/{key}")
    public ResponseEntity<?> removeMetadata(
            @PathVariable Long id,
            @PathVariable String key) {
        try {
            Candidate updatedCandidate = candidateService.removeMetadata(id, key);
            return ResponseEntity.ok(new FetchCandidateDto(updatedCandidate));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("Error removing metadata: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error removing metadata: " + e.getMessage());
        }
    }
    
    // Check if email exists
    @GetMapping("/exists/email/{email}")
    public ResponseEntity<Boolean> emailExists(@PathVariable String email) {
        try {
            boolean exists = candidateService.emailExists(email);
            return ResponseEntity.ok(exists);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(false);
        }
    }
} 