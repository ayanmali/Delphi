package com.delphi.delphi.components;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import com.delphi.delphi.entities.ChatMessage;
import com.delphi.delphi.repositories.AssessmentRepository;
import com.delphi.delphi.services.GithubService;
import com.delphi.delphi.services.UserService;
import com.delphi.delphi.utils.git.GithubBranchDetails;
import com.delphi.delphi.utils.git.GithubFile;
import com.delphi.delphi.utils.git.GithubReference;
import com.delphi.delphi.utils.git.GithubRepoBranch;
import com.delphi.delphi.utils.git.GithubRepoContents;

@Component
/*
 * Contains tool definitions for the LLM to make GitHub API calls
 * Uses the methods defined in the GithubService class
 */
public class GithubTools {

    private final AssessmentRepository assessmentRepository;
    private final UserService userService; // for getting user info (PAT, username)
    private final GithubService githubService; // for making GitHub API calls
    private final Base64.Encoder base64Encoder; // for encoding content to base64 for GitHub API
    private final Base64.Decoder base64Decoder; // for decoding content from base64 for GitHub API

    public GithubTools(UserService userService, GithubService githubService, AssessmentRepository assessmentRepository) {
        this.userService = userService;
        this.githubService = githubService;
        this.base64Encoder = Base64.getEncoder();
        this.base64Decoder = Base64.getDecoder();
        this.assessmentRepository = assessmentRepository;
    }

    /* Helper methods */

    // get the decrypted PAT from the user service
    private String getPAT(Object userIdObj) {
        try {
            Long userId = (Long) userIdObj;
            if (userId == null) throw new IllegalStateException("User not set in context");
            return userService.getDecryptedGithubToken(userId);
        } catch (ClassCastException e) {
            throw new IllegalStateException("User ID must be a Long", e);
        } catch (NullPointerException e) {
            throw new IllegalStateException("User ID not set in context", e);
        }
    }

    private String getCurrentRepoName(Object assessmentIdObj) {
        try {
            Long assessmentId = (Long) assessmentIdObj;
            return assessmentRepository.findById(assessmentId)
                    .orElseThrow(() -> new RuntimeException("Assessment not found"))
                    .getGithubRepoName();
        } catch (NullPointerException e) {
            throw new IllegalStateException("Assessment ID not set in context", e);
        }
        catch (ClassCastException e) {
            throw new IllegalStateException("Assessment ID must be a Long", e);
        }
    }

    private String getGithubUsername(Object userIdObj) {
        try {
            Long userId = (Long) userIdObj;
            return userService.getUserById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"))
                    .getGithubUsername();
        } catch (NullPointerException e) {
            throw new IllegalStateException("User ID not set in context", e);
        }
        catch (ClassCastException e) {
            throw new IllegalStateException("User ID must be a Long", e);
        }
    }

    private Long getCurrentChatHistoryId(Object chatHistoryIdObj) {
        try {
            Long chatHistoryId = (Long) chatHistoryIdObj;
            return chatHistoryId;
        } catch (NullPointerException e) {
            throw new IllegalStateException("Chat history ID not set in context", e);
        }
        catch (ClassCastException e) {
            throw new IllegalStateException("Chat history ID must be a Long", e);
        }
    }

    // GitHub API requires base64-encoded content for file contents
    private String encodeToBase64(String content) {
        return base64Encoder.encodeToString(content.getBytes(StandardCharsets.UTF_8));
    } 

    private String decodeFromBase64(String content) {
        return new String(base64Decoder.decode(content), StandardCharsets.UTF_8);
    }

    /* Tool definitions */

    // @Tool(description = "Creates a Git repository in the user's GitHub account using the GitHub API.")
    // public ResponseEntity<String> createRepo(@ToolParam(required = true, description = "The name of the repository to create") String repoName) {
    //     String pat = getPAT();
    //     return githubService.createRepo(pat, repoName);
    // }

    @Tool(description = "Adds a file to a repository using the GitHub API.")
    public GithubFile addFileToRepo(
        @ToolParam(required = true, description = "The path of the file to add") String filePath, 
        @ToolParam(required = true, description = "The content of the file to add") String fileContent,
        @ToolParam(required = true, description = "The commit message for the file") String commitMessage,
        @ToolParam(required = false, description = "The branch to add the file to. If not provided, the default branch will be used.") String branch,
        ToolContext toolContext) {
        String pat = getPAT(toolContext.getContext().get("userId"));
        String username = getGithubUsername(toolContext.getContext().get("userId"));
        GithubFile fileResponse = githubService.addFileToRepo(pat, username, getCurrentRepoName((Long) toolContext.getContext().get("assessmentId")), filePath, branch=null, encodeToBase64(fileContent),
                commitMessage).block();
        if (fileResponse != null) {
            GithubFile file = fileResponse;
            file.setContent(fileContent);
            return file;
        } else {
            throw new RuntimeException("Error adding file to repo");
        }
    }

    @Tool(description = "Gets the file and directory contents of the default branch of the Git repository using the GitHub API.")
    public GithubRepoContents getRepoContents(
        @ToolParam(required = true, description = "The path of the file to get the contents of") String filePath,
        @ToolParam(required = false, description = "The branch to get the contents of. If not provided, the default branch will be used.") String branch,
        ToolContext toolContext) {
        String pat = getPAT(toolContext.getContext().get("userId"));
        String username = getGithubUsername(toolContext.getContext().get("userId"));
        GithubRepoContents repoContentsResponse = githubService.getRepoContents(pat, username, getCurrentRepoName((Long) toolContext.getContext().get("assessmentId")), filePath, branch).block();
        if (repoContentsResponse != null) {
            GithubRepoContents repoContents = repoContentsResponse;
            if (repoContents.getType().equals("file")) {
                repoContents.setContent(decodeFromBase64(repoContents.getContent()));
            }
            return repoContents;
        } else {
            throw new RuntimeException("Error getting repo contents");
        }
    }

    @Tool(description = "Gets the branches of the repository using the GitHub API.")
    public List<GithubRepoBranch> getRepoBranches(ToolContext toolContext) {
        String pat = getPAT(toolContext.getContext().get("userId"));
        String username = getGithubUsername(toolContext.getContext().get("userId"));
        List<GithubRepoBranch> repoBranchesResponse = githubService.getRepoBranches(pat, username, getCurrentRepoName((Long) toolContext.getContext().get("assessmentId"))).block();
        if (repoBranchesResponse != null) {
            return repoBranchesResponse;
        } else {
            throw new RuntimeException("Error getting repo branches");
        }
    }

    @Tool(description = "Adds a branch to the repository using the GitHub API.")
    public GithubReference addBranch(
        @ToolParam(required = true, description = "The name of the branch to add") String branchName,
        @ToolParam(required = true, description = "The base branch to add the new branch from") String baseBranch,
        ToolContext toolContext) {
        String pat = getPAT(toolContext.getContext().get("userId"));
        String username = getGithubUsername(toolContext.getContext().get("userId"));
        GithubReference addBranchResponse = githubService.addBranch(pat, username, getCurrentRepoName((Long) toolContext.getContext().get("assessmentId")), branchName, baseBranch).block();
        if (addBranchResponse != null) {
            return addBranchResponse;
        } else {
            throw new RuntimeException("Error adding branch");
        }
    }

    @Tool(description = "Replaces the contents of an existing file in the repository with new content using the GitHub API.")
    public GithubFile editFile(
        @ToolParam(required = true, description = "The path of the file to edit") String filePath,
        @ToolParam(required = true, description = "The content of the file to edit") String fileContent,
        @ToolParam(required = true, description = "The commit message for the file") String commitMessage,
        @ToolParam(required = true, description = "The SHA of the file to edit") String sha,
        ToolContext toolContext) {
        String pat = getPAT(toolContext.getContext().get("userId"));
        String username = getGithubUsername(toolContext.getContext().get("userId"));
        GithubFile editFileResponse = githubService.editFile(pat, username, getCurrentRepoName((Long) toolContext.getContext().get("assessmentId")), filePath, encodeToBase64(fileContent), commitMessage,
                sha).block();
        if (editFileResponse != null) {
            GithubFile file = editFileResponse;
            file.setContent(fileContent);
            return file;
        } else {
            throw new RuntimeException("Error editing file");
        }
    }

    @Tool(description = "Deletes a file in the Git repository using the GitHub API.")
    public String deleteFile(
        @ToolParam(required = true, description = "The path of the file to delete") String filePath,
        @ToolParam(required = true, description = "The commit message for the file") String commitMessage,
        @ToolParam(required = true, description = "The SHA of the file to delete") String sha,
        ToolContext toolContext) {
        String pat = getPAT(toolContext.getContext().get("userId"));
        String username = getGithubUsername(toolContext.getContext().get("userId"));
        String deleteFileResponse = githubService.deleteFile(pat, username, getCurrentRepoName((Long) toolContext.getContext().get("assessmentId")), filePath, commitMessage, sha).block();
        if (deleteFileResponse != null) {
            return "File: " + filePath + " deleted successfully";
        } else {
            throw new RuntimeException("Error deleting file");
        }
    }

    @Tool(description = "Gets the details of a branch in the Git repository using the GitHub API.")
    public GithubBranchDetails getBranchDetails(
        @ToolParam(required = true, description = "The name of the branch to get the details of") String branchName,
        ToolContext toolContext) {
        String pat = getPAT(toolContext.getContext().get("userId"));
        String username = getGithubUsername(toolContext.getContext().get("userId"));
        GithubBranchDetails branchDetailsResponse = githubService.getBranchDetails(pat, username, getCurrentRepoName((Long) toolContext.getContext().get("assessmentId")), branchName).block();
        if (branchDetailsResponse != null) {
            return branchDetailsResponse;
        } else {
            throw new RuntimeException("Error getting branch details");
        }
    }

    @Tool(description = "Sends a message to the user after applying changes to the repository.", returnDirect=true)
    public ChatMessage sendMessageToUser(
        @ToolParam(required = true, description = "The message to send to the user") String message,
        ToolContext toolContext) {
        return githubService.sendMessageToUser(message, getCurrentChatHistoryId(toolContext.getContext().get("chatHistoryId")), (String) toolContext.getContext().get("model"));
    }

}