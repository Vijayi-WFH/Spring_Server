package com.tse.core_application.service.Impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.tse.core_application.constants.ErrorConstant;
import com.tse.core_application.custom.model.OrgIdOrgName;
import com.tse.core_application.dto.AddGithubAccountAndItsRepoRequest;
import com.tse.core_application.dto.github.*;
import com.tse.core_application.exception.UnauthorizedException;
import com.tse.core_application.exception.UserDoesNotExistException;
import com.tse.core_application.exception.ValidationFailedException;
import com.tse.core_application.model.Constants;
import com.tse.core_application.model.User;
import com.tse.core_application.model.github.*;
import com.tse.core_application.repository.*;
import com.tse.core_application.utils.CommonUtils;
import com.tse.core_application.utils.DateTimeUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.persistence.EntityNotFoundException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

@Service
public class GithubService {

    @Autowired
    private GithubAccountRepository githubAccountRepository;
    @Autowired
    private WorkItemGithubBranchRepository workItemGithubBranchRepository;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private AccessDomainRepository accessDomainRepository;
    @Autowired
    private GithubAccountAndRepoPreferenceRepository githubAccountAndRepoPreferenceRepository;
    @Autowired
    private UserAccountRepository userAccountRepository;
    @Autowired
    private OrganizationRepository organizationRepository;
    @Autowired
    @Qualifier("githubExecutor")
    private Executor githubExecutor;

    private static final String GITHUB_CLIENT_ID = "src/main/java/com/tse/core_application/keys/client_id";
    private static final String GITHUB_CLIENT_SECRET = "src/main/java/com/tse/core_application/keys/client_secret_key";

    public LinkedGithubResponse linkGithub(LinkGithubRequest request, User user, String timeZone) throws Exception {
        if (!userAccountRepository.existsByFkUserIdUserIdAndOrgIdAndIsActive(user.getUserId(), request.getOrgId(), true)) {
            throw new UserDoesNotExistException();
        }

        String oauthAccessToken = exchangeCodeForAccessToken(request.getGithubUserCode());

        String githubUserName = getGithubUserNameFromOAuthToken(oauthAccessToken);

        GithubAccount githubAccount = githubAccountRepository
                .findByFkUserIdUserIdAndOrgId(user.getUserId(), request.getOrgId())
                .orElse(new GithubAccount());

        githubAccount.setFkUserId(user);
        githubAccount.setOrgId(request.getOrgId());
        githubAccount.setGithubUserCode(request.getGithubUserCode());
        githubAccount.setGithubUserName(githubUserName);
        githubAccount.setGithubAccessToken(oauthAccessToken);
        githubAccount.setIsLinked(true);
        githubAccountRepository.save(githubAccount);

        LinkedGithubResponse response = new LinkedGithubResponse();
        generateGitHubAccountResponse(response, githubAccount, timeZone);
        response.setMessage("GitHub account linked successfully.");
        return response;
    }

    private String getGithubUserNameFromOAuthToken(String oauthAccessToken) throws Exception {
        String url = "https://api.github.com/user";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(oauthAccessToken);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<JsonNode> response = new RestTemplate().exchange(url, HttpMethod.GET, entity, JsonNode.class);
        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            return response.getBody().get("login").asText();
        }
        throw new ValidationFailedException("Unable to fetch GitHub user profile.");
    }

    public String exchangeCodeForAccessToken(String githubUserCode) throws Exception {

        MultiValueMap<String, String> bodyMap = new LinkedMultiValueMap<>();
        bodyMap.add("client_id", CommonUtils.getGithubClientId(GITHUB_CLIENT_ID));
        bodyMap.add("client_secret", CommonUtils.getGithubClientId(GITHUB_CLIENT_SECRET));
        bodyMap.add("code", githubUserCode);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON)); // Ensures JSON response

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(bodyMap, headers);

        ResponseEntity<GithubTokenResponse> response = new RestTemplate().exchange(
                "https://github.com/login/oauth/access_token",
                HttpMethod.POST,
                entity,
                GithubTokenResponse.class
        );

        GithubTokenResponse body = response.getBody();
        if (response.getStatusCode() == HttpStatus.OK && body != null) {
            if (body.getAccess_token() != null) {
                return body.getAccess_token();
            }
        }

        throw new IllegalStateException("Failed to retrieve access_token from GitHub.");
    }

    public JsonNode getGitHubRepositories(String accessToken) throws Exception {
        String url = "https://api.github.com/installation/repositories";

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = createGithubHeaderForRestTemplate(accessToken);

        HttpEntity<String> entity = new HttpEntity<>(null, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                ObjectMapper objectMapper = new ObjectMapper();
                return objectMapper.readTree(response.getBody()); // Return full JSON response
            } else {
                throw new IllegalStateException("Failed to retrieve repositories.");
            }
        } catch (HttpClientErrorException e) {
            throw e;
        }
    }

    public LinkedGithubResponse getGithubStatus(GithubStatusRequest request, User foundUser, String timeZone) throws Exception {
        if (!userAccountRepository.existsByFkUserIdUserIdAndOrgIdAndIsActive(foundUser.getUserId(), request.getOrgId(), true)) {
            throw new UserDoesNotExistException();
        }
        Optional<GithubAccount> githubAccountOpt = githubAccountRepository.findByFkUserIdUserIdAndOrgId(foundUser.getUserId(), request.getOrgId());

        if (githubAccountOpt.isEmpty()) {
            throw new EntityNotFoundException("Your account is not connected with GitHub.");
        }

        GithubAccount githubAccount = githubAccountOpt.get();
        LinkedGithubResponse response = new LinkedGithubResponse();

        String accessToken = githubAccount.getGithubAccessToken();
        boolean tokenValid = isOAuthTokenValid(accessToken);

        if (!Boolean.TRUE.equals(githubAccount.getIsLinked()) || !tokenValid) {
            if (tokenValid && accessToken != null) {
                try {
                    revokeGithubOAuthToken(accessToken);
                } catch (Exception e) {
//                    System.err.println("Failed to revoke GitHub access token: " + e.getMessage());
                }
            }

            githubAccount.setIsLinked(false);
            githubAccountRepository.save(githubAccount);

            generateGitHubAccountResponse(response, githubAccount, timeZone);
            response.setMessage("GitHub account is not linked or token is invalid. Please re-link your account.");
            return response;
        }

        generateGitHubAccountResponse(response, githubAccount, timeZone);
        response.setMessage("GitHub status fetched successfully.");
        return response;
    }


    private boolean isOAuthTokenValid(String accessToken) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(accessToken);
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

            HttpEntity<Void> request = new HttpEntity<>(headers);

            // Lightweight endpoint to verify token
            ResponseEntity<String> response = new RestTemplate().exchange(
                    "https://api.github.com/user",
                    HttpMethod.GET,
                    request,
                    String.class
            );

            return response.getStatusCode() == HttpStatus.OK;
        } catch (HttpClientErrorException.Unauthorized e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private void generateGitHubAccountResponse(LinkedGithubResponse linkedGithubResponse, GithubAccount userGithubAccount, String timeZone) {

        if (userGithubAccount != null) {
            linkedGithubResponse.setUserId(userGithubAccount.getFkUserId().getUserId());
            linkedGithubResponse.setFirstName(userGithubAccount.getFkUserId().getFirstName());
            linkedGithubResponse.setLastName(userGithubAccount.getFkUserId().getLastName());
            linkedGithubResponse.setIsLinked(userGithubAccount.getIsLinked());
            linkedGithubResponse.setGithubUserName(userGithubAccount.getGithubUserName());
            linkedGithubResponse.setCreatedDateTime(DateTimeUtils.convertServerDateToUserTimezone(userGithubAccount.getCreatedDateTime(), timeZone));
            linkedGithubResponse.setLastUpdatedDateTime(DateTimeUtils.convertServerDateToUserTimezone(userGithubAccount.getLastUpdatedDateTime(), timeZone));
        }
        else {
            linkedGithubResponse.setIsLinked(false);
        }
    }

    public LinkedGithubResponse unlinkGithubAccount(GithubUnlinkRequest request, User foundUser, String timeZone) throws Exception {
        if (!userAccountRepository.existsByFkUserIdUserIdAndOrgIdAndIsActive(foundUser.getUserId(), request.getOrgId(), true)) {
            throw new UserDoesNotExistException();
        }

        Optional<GithubAccount> githubAccountOpt = githubAccountRepository.findByFkUserIdUserIdAndOrgId(foundUser.getUserId(), request.getOrgId());

        if (githubAccountOpt.isEmpty()) {
            throw new EntityNotFoundException("Your account is never connected with GitHub.");
        }

        GithubAccount githubAccount = githubAccountOpt.get();

        if (!Boolean.TRUE.equals(githubAccount.getIsLinked())) {
            throw new ValidationFailedException("Your GitHub account is already unlinked.");
        }

        try {
            revokeGithubOAuthToken(githubAccount.getGithubAccessToken());
        } catch (Exception e) {
            System.err.println("Warning: Failed to revoke GitHub token. Reason: " + e.getMessage());
        }

        githubAccount.setIsLinked(false);
        githubAccountRepository.save(githubAccount);

        LinkedGithubResponse linkedGithubResponse = new LinkedGithubResponse();
        generateGitHubAccountResponse(linkedGithubResponse, githubAccount, timeZone);
        linkedGithubResponse.setMessage("Your GitHub account has been successfully unlinked.");
        return linkedGithubResponse;
    }

    private void revokeGithubOAuthToken(String accessToken) throws Exception {
        String clientId = CommonUtils.getGithubClientId(GITHUB_CLIENT_ID);
        String clientSecret = CommonUtils.getGithubClientSecret(GITHUB_CLIENT_SECRET);

        String url = "https://api.github.com/applications/" + clientId + "/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Basic Auth using client_id:client_secret
        String basicAuth = Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));
        headers.set("Authorization", "Basic " + basicAuth);

        Map<String, String> body = new HashMap<>();
        body.put("access_token", accessToken);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<Void> response = new RestTemplate().exchange(
                url, HttpMethod.DELETE, request, Void.class
        );

        if (response.getStatusCode() != HttpStatus.NO_CONTENT) {
            throw new IllegalStateException("GitHub token revocation failed.");
        }
    }

    public RepositoryDetailsResponse getGithubUserRepositories(GithubRepoRequest request, User user) throws Exception {
        if (!userAccountRepository.existsByFkUserIdUserIdAndOrgIdAndIsActive(user.getUserId(), request.getOrgId(), true)) {
            throw new UserDoesNotExistException();
        }

        Optional<GithubAccount> githubAccountOpt = githubAccountRepository
                .findByFkUserIdUserIdAndOrgId(user.getUserId(), request.getOrgId());

        if (githubAccountOpt.isEmpty()) {
            throw new ValidationFailedException("Link your GitHub account.");
        }

        GithubAccount githubAccount = githubAccountOpt.get();

        if (!Boolean.TRUE.equals(githubAccount.getIsLinked())) {
            throw new ValidationFailedException("Re-Link your GitHub account.");
        }

        String accessToken = githubAccount.getGithubAccessToken();
        if (!isOAuthTokenValid(accessToken)) {
            githubAccount.setIsLinked(false);
            githubAccountRepository.save(githubAccount);
            return new RepositoryDetailsResponse("OAuth token expired. Re-Link your GitHub account.", null);
        }

        List<GithubRepositoryDTO> allRepos = fetchGithubRepositoriesAndBranchesUsingOAuth(accessToken);

        List<GithubAccountAndRepoPreference> preferences = githubAccountAndRepoPreferenceRepository
                .findAllByOrgIdAndIsActiveTrue(request.getOrgId());

        Set<String> allowedOwnerRepoKeys = preferences.stream()
                .map(pref -> (pref.getGithubAccountUserName() + "/" + pref.getGithubAccountRepoName()).toLowerCase())
                .collect(Collectors.toSet());

        boolean allowAll = allowedOwnerRepoKeys.contains("*/*");

        List<GithubRepositoryDTO> filteredRepos = allRepos.stream()
                .filter(repo -> {
                    String owner = repo.getOwner().toLowerCase();
                    String name = repo.getRepoName().toLowerCase();

                    return allowAll
                            || allowedOwnerRepoKeys.contains(owner + "/" + name)
                            || allowedOwnerRepoKeys.contains("*" + "/" + name)
                            || allowedOwnerRepoKeys.contains(owner + "/" + "*");
                })
                .collect(Collectors.toList());

        return new RepositoryDetailsResponse("Repositories and branches fetched successfully.", filteredRepos);
    }

    public List<GithubRepositoryDTO> fetchGithubRepositoriesAndBranchesUsingOAuth(String accessToken) throws Exception {
        JsonNode repoResponse = getGitHubRepositoriesWithOAuth(accessToken);
        List<CompletableFuture<GithubRepositoryDTO>> futures = new ArrayList<>();

        for (JsonNode repo : repoResponse) {
            futures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    String repoId = repo.get("id").asText();
                    String fullRepoName = repo.get("full_name").asText();
                    String owner = fullRepoName.substring(0, fullRepoName.indexOf("/"));
                    String repoName = fullRepoName.substring(fullRepoName.indexOf("/") + 1);
                    String repoUrl = "https://github.com/" + fullRepoName;

                    List<GithubBranchDTO> branches = fetchGithubBranchesForRepository(fullRepoName, accessToken);
                    return new GithubRepositoryDTO(repoId, repoName, owner, repoUrl, branches);
                } catch (Exception e) {
//                    System.err.println("Failed for repo: " + repo.get("full_name") + " → " + e.getMessage());
                    return null;
                }
            }, githubExecutor));
        }

        return futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }


    private List<GithubBranchDTO> fetchGithubBranchesForRepository(String fullRepoName, String accessToken) throws Exception {
        final String firstPageUrl = "https://api.github.com/repos/" + fullRepoName + "/branches?per_page=100";

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = createGithubHeaderForRestTemplate(accessToken);
        HttpEntity<String> entity = new HttpEntity<>(null, headers);

        ObjectMapper objectMapper = new ObjectMapper();
        List<GithubBranchDTO> allBranches = new ArrayList<>();

        String url = firstPageUrl;
        String baseBranch = getGithubBaseBranch(fullRepoName, accessToken);

        try {
            while (url != null) {
                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

                if (response.getStatusCode() != HttpStatus.OK) {
                    throw new IllegalStateException("Failed to retrieve branches for repository: " + fullRepoName);
                }

                JsonNode page = objectMapper.readTree(response.getBody());
                if (page.isArray()) {
                    for (JsonNode branch : page) {
                        String branchSha = branch.path("commit").path("sha").asText(null);
                        String branchName = branch.path("name").asText(null);
                        if (branchName == null || branchSha == null) continue;

                        String branchUrl = "https://github.com/" + fullRepoName + "/tree/" + branchName;
                        allBranches.add(new GithubBranchDTO(branchSha, branchName, branchUrl));
                    }
                }

                url = getNextPageUrlFromLinkHeader(response.getHeaders().getFirst("Link"));
            }

            allBranches.sort(
                    Comparator.<GithubBranchDTO>comparingInt(b -> computeBranchPriority(b, baseBranch))
            );

            return allBranches;
        } catch (HttpClientErrorException e) {
            throw e;
        }
    }

    private int computeBranchPriority(GithubBranchDTO branch, String baseBranch) {
        String name = branch.getBranchName();
        if (name == null) return 100;

        String lower = name.toLowerCase();
        String baseLower = baseBranch != null ? baseBranch.toLowerCase() : null;

        // i) base branch (usually main/master) at the very top
        if (baseLower != null && lower.equals(baseLower)) {
            return 0;
        }

        // In case default_branch is something else but you still want main/master high
        if (lower.equals("main") || lower.equals("master")) {
            return 1;
        }

        // ii) developer/development/etc
        if (lower.equals("develop") ||
                lower.equals("development") ||
                lower.equals("dev") ||
                lower.equals("developer") ||
                lower.startsWith("develop/") ||
                lower.startsWith("dev/")) {
            return 2;
        }

        // iii) release/rel
        if (lower.contains("release") || lower.startsWith("rel/")) {
            return 3;
        }

        // iv) everything else – keep GitHub order (same priority)
        return 10;
    }

    private String getGithubBaseBranch(String fullRepoName, String accessToken) throws Exception {
        String url = "https://api.github.com/repos/" + fullRepoName;

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = createGithubHeaderForRestTemplate(accessToken);

        HttpEntity<String> entity = new HttpEntity<>(null, headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                ObjectMapper objectMapper = new ObjectMapper();
                JsonNode repoResponse = objectMapper.readTree(response.getBody());
                return repoResponse.get("default_branch").asText();
            } else {
                throw new IllegalStateException("Failed to retrieve base branch for repository: " + fullRepoName);
            }
        } catch (HttpClientErrorException e) {
            throw e;
        }
    }

    public WorkItemGithubBranchResponse createGithubBranch(User user, WorkItemGithubBranchRequest request, Long orgId, String timeZone) throws Exception {
        GithubAccount githubAccount = validateGithubAccountForBranchCreation(user, orgId);

        String accessToken = githubAccount.getGithubAccessToken();
        JsonNode repositories = getGitHubRepositoriesWithOAuth(accessToken);

        String repoName = getRepoNameByIdFromOAuthRepoList(repositories, request.getRepoId(), githubAccount.getOrgId());

        String baseCommitSha = getLatestCommitShaOAuth(repoName, request.getBaseBranchName(), accessToken);

        if (branchExistsOnGitHubOAuth(repoName, request.getNewBranchName(), accessToken)) {
            throw new ValidationFailedException("Branch '" + request.getNewBranchName() + "' already exists.");
        }

        createBranchOnGitHubOAuth(repoName, request.getNewBranchName(), baseCommitSha, accessToken);

        // Save the branch info
        WorkItemGithubBranch savedBranch = saveWorkItemBranchRecord(request, baseCommitSha, repoName);

        return buildBranchResponse(request, baseCommitSha, savedBranch, timeZone, repoName, accessToken);
    }

    private WorkItemGithubBranchResponse buildBranchResponse(WorkItemGithubBranchRequest request, String sha,
                                                             WorkItemGithubBranch entity, String timeZone,
                                                             String repoFullName, String oauthToken) {
        return new WorkItemGithubBranchResponse(
                request.getWorkItemId(),
                sha,
                request.getNewBranchName(),
                getLatestCommitShaOrFallback(repoFullName, request.getBaseBranchName(), entity.getLastCommitHash(), oauthToken),
                request.getBaseBranchName(),
                request.getRepoId(),
                repoFullName.split("/")[1],
                entity.getLastCommitHash(),
                entity.getBranchLink(),
                DateTimeUtils.convertServerDateToUserTimezone(entity.getCreatedDateTime(), timeZone)
        );
    }


    private String getLatestCommitShaOrFallback(String repoFullName, String branchName, String fallbackSha, String oauthToken) {
        try {
            return getLatestCommitSha(repoFullName, branchName, oauthToken);
        } catch (Exception e) {
            return fallbackSha;
        }
    }

    private String getLatestCommitSha(String repoFullName, String branchName, String oauthAccessToken) throws Exception {
        String url = "https://api.github.com/repos/" + repoFullName + "/branches/" + branchName;

        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set("Authorization", "Bearer " + oauthAccessToken);

        HttpEntity<String> entity = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    JsonNode.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody().get("commit").get("sha").asText();
            } else {
                throw new IllegalStateException("Failed to retrieve commit SHA from GitHub.");
            }
        } catch (HttpClientErrorException e) {
            throw new IllegalStateException("Error while fetching latest commit SHA: " + e.getMessage(), e);
        }
    }


    private GithubAccount validateGithubAccountForBranchCreation(User user, Long orgId) {
        GithubAccount githubAccount = githubAccountRepository
                .findByFkUserIdUserIdAndOrgId(user.getUserId(), orgId)
                .orElseThrow(() -> new ValidationFailedException("Link your GitHub account first."));

        if (!Boolean.TRUE.equals(githubAccount.getIsLinked())) {
            throw new ValidationFailedException("Re-Link your GitHub account.");
        }

        if (!isOAuthTokenValid(githubAccount.getGithubAccessToken())) {
            githubAccount.setIsLinked(false);
            githubAccountRepository.save(githubAccount);
            throw new ValidationFailedException("Your GitHub access token is invalid or expired. Please re-link.");
        }

        return githubAccount;
    }

    private JsonNode getGitHubRepositoriesWithOAuth(String oauthToken) throws Exception {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(oauthToken);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        HttpEntity<String> entity = new HttpEntity<>(headers);

        String url = "https://api.github.com/user/repos?per_page=100";
        RestTemplate restTemplate = new RestTemplate();

        List<JsonNode> allRepos = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();

        while (url != null) {
            ResponseEntity<String> response;

            try {
                response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            } catch (HttpClientErrorException.Unauthorized e) {
                throw new UnauthorizedException("OAuth token expired or invalid. Please re-link GitHub.");
            }

            if (response.getStatusCode() != HttpStatus.OK) {
                throw new IllegalStateException("Failed to fetch GitHub repositories.");
            }

            JsonNode pageData = objectMapper.readTree(response.getBody());
            if (pageData.isArray()) {
                for (JsonNode repo : pageData) {
                    allRepos.add(repo);
                }
            }

            url = getNextPageUrlFromLinkHeader(response.getHeaders().getFirst("Link"));
        }

        ArrayNode result = objectMapper.createArrayNode();
        result.addAll(allRepos);
        return result;
    }

    private String getNextPageUrlFromLinkHeader(String linkHeader) {
        if (linkHeader == null) return null;

        String[] links = linkHeader.split(",");
        for (String link : links) {
            String[] parts = link.split(";");

            if (parts.length == 2 && parts[1].trim().equals("rel=\"next\"")) {
                String url = parts[0].trim();
                return url.substring(1, url.length() - 1); // remove < >
            }
        }
        return null;
    }


    private String getRepoNameByIdFromOAuthRepoList(JsonNode repos, String repoId, Long orgId) {
        List<GithubAccountAndRepoPreference> preferences =
                githubAccountAndRepoPreferenceRepository.findAllByOrgIdAndIsActiveTrue(orgId);

        Set<String> allowedOwnerRepoKeys = preferences.stream()
                .map(pref -> (pref.getGithubAccountUserName() + "/" + pref.getGithubAccountRepoName()).toLowerCase())
                .collect(Collectors.toSet());

        boolean allowAll = allowedOwnerRepoKeys.contains("*/*");

        for (JsonNode repo : repos) {
            String id = repo.get("id").asText();
            String fullName = repo.get("full_name").asText();
            String[] parts = fullName.split("/");
            String owner = parts[0].toLowerCase();
            String name = parts[1].toLowerCase();

            if (!id.equals(repoId)) continue;

            boolean isAllowed =
                    allowAll
                            || allowedOwnerRepoKeys.contains(owner + "/" + name)
                            || allowedOwnerRepoKeys.contains("*" + "/" + name)
                            || allowedOwnerRepoKeys.contains(owner + "/" + "*");

            if (isAllowed) {
                return fullName;
            } else {
                break;
            }
        }

        throw new ValidationFailedException("Selected repository is not allowed for your organization.");
    }

    private String getLatestCommitShaOAuth(String repoName, String baseBranch, String oauthToken) throws Exception {
        String url = "https://api.github.com/repos/" + repoName + "/branches/" + baseBranch;

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(oauthToken);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        HttpEntity<String> entity = new HttpEntity<>(headers);
        RestTemplate restTemplate = new RestTemplate();

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode branchNode = new ObjectMapper().readTree(response.getBody());

                // 🛡️ Extra Safety Check: Ensure the returned branch name matches exactly
                String returnedBranchName = branchNode.get("name").asText();
                if (!baseBranch.equals(returnedBranchName)) {
                    throw new ValidationFailedException("Base branch '" + baseBranch + "' does not exist in GitHub.");
                }

                if (branchNode.has("commit") && branchNode.get("commit").has("sha")) {
                    return branchNode.get("commit").get("sha").asText();
                } else {
                    throw new ValidationFailedException("Base branch exists but no commit SHA found.");
                }
            }

            throw new ValidationFailedException("Unexpected error fetching base branch from GitHub.");

        } catch (HttpClientErrorException.NotFound e) {
            throw new ValidationFailedException("Base branch '" + baseBranch + "' not found in GitHub repository.");
        } catch (HttpClientErrorException e) {
            throw new ValidationFailedException("Error fetching base branch: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
        }
    }

    private boolean branchExistsOnGitHubOAuth(String repoName, String newBranchName, String oauthToken) {
        String url = "https://api.github.com/repos/" + repoName + "/branches/" + newBranchName;

        try {
            HttpEntity<String> entity = new HttpEntity<>(createGithubOAuthHeaders(oauthToken));
            new RestTemplate().exchange(url, HttpMethod.GET, entity, String.class);
            return true;
        } catch (HttpClientErrorException.NotFound e) {
            return false;
        }
    }

    private void createBranchOnGitHubOAuth(String repoName, String newBranchName, String sha, String oauthToken) throws Exception {
        String url = "https://api.github.com/repos/" + repoName + "/git/refs";

        ObjectNode body = new ObjectMapper().createObjectNode();
        body.put("ref", "refs/heads/" + newBranchName);
        body.put("sha", sha);

        HttpHeaders headers = createGithubOAuthHeaders(oauthToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(body.toString(), headers);
        ResponseEntity<String> response = new RestTemplate().exchange(url, HttpMethod.POST, entity, String.class);

        if (response.getStatusCode() != HttpStatus.CREATED) {
            throw new IllegalStateException("Failed to create GitHub branch: " + response.getBody());
        }
    }

    private HttpHeaders createGithubOAuthHeaders(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "token " + token);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        return headers;
    }

    private WorkItemGithubBranch saveWorkItemBranchRecord(WorkItemGithubBranchRequest request, String lastCommitSha, String repoName) {
        WorkItemGithubBranch branch = new WorkItemGithubBranch();
        branch.setWorkItemId(request.getWorkItemId());
        branch.setRepoId(request.getRepoId());
        branch.setBranchName(request.getNewBranchName());
        branch.setBaseBranchName(request.getBaseBranchName());
        branch.setLastCommitHash(lastCommitSha);
        branch.setBranchLink("https://github.com/" + repoName + "/tree/" + request.getNewBranchName());
        branch.setCreatedDateTime(LocalDateTime.now());
        return workItemGithubBranchRepository.save(branch);
    }


    private String getGithubRepoNameById(String repoId, String accessToken) throws Exception {
        JsonNode repositories = getGitHubRepositoriesWithOAuth(accessToken);
        for (JsonNode repo : repositories) {
            if (repo.get("id").asText().equals(repoId)) {
                return repo.get("full_name").asText();
            }
        }
        throw new IllegalStateException("Repository not found.");
    }

    private String getLatestCommitHash(String repoName, String branchName, String accessToken) throws Exception {
        String url = "https://api.github.com/repos/" + repoName + "/branches/" + branchName;
        return makeGitHubGetRequest(url, accessToken).get("commit").get("sha").asText();
    }

    private JsonNode makeGitHubGetRequest(String url, String accessToken) throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = createGithubOAuthHeaders(accessToken);
        HttpEntity<String> entity = new HttpEntity<>(null, headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            return new ObjectMapper().readTree(response.getBody());
        } else {
            throw new IllegalStateException("Failed to fetch data from GitHub: " + url);
        }
    }

    public WorkItemBranchesResponse getWorkItemBranches(User user, Long workItemId, Long orgId, String timeZone) throws Exception {
        GithubAccount githubAccount = githubAccountRepository.findByFkUserIdUserIdAndOrgId(user.getUserId(), orgId)
                .orElseThrow(() -> new ValidationFailedException("Link your GitHub account."));
        validateGithubOAuthToken(githubAccount);

        String accessToken = githubAccount.getGithubAccessToken();

        List<GithubAccountAndRepoPreference> preferences = githubAccountAndRepoPreferenceRepository.findAllByOrgIdAndIsActiveTrue(orgId);
        Set<String> allowedOwnerRepoKeys = preferences.stream()
                .map(pref -> (pref.getGithubAccountUserName() + "/" + pref.getGithubAccountRepoName()).toLowerCase())
                .collect(Collectors.toSet());
        boolean allowAll = allowedOwnerRepoKeys.contains("*/*");

        List<WorkItemGithubBranch> workItemBranches = workItemGithubBranchRepository.findByWorkItemId(workItemId);

        List<CompletableFuture<WorkItemGithubBranchDetails>> futures = workItemBranches.stream()
                .map(branch -> CompletableFuture.supplyAsync(() -> {
                    try {
                        String repoFullName = getGithubRepoNameById(branch.getRepoId(), accessToken);
                        if (!isRepoAllowed(repoFullName, allowedOwnerRepoKeys, allowAll)) return null;

                        // Fetch branch SHA, base branch SHA, and PRs in parallel
                        CompletableFuture<String> branchShaFuture = CompletableFuture.supplyAsync(
                                () -> getBranchShaSafely(repoFullName, branch.getBranchName(), accessToken), githubExecutor);

                        CompletableFuture<String> baseBranchShaFuture = CompletableFuture.supplyAsync(
                                () -> getBranchShaSafely(repoFullName, branch.getBaseBranchName(), accessToken), githubExecutor);

                        CompletableFuture<List<PullRequestDto>> pullRequestsFuture = CompletableFuture.supplyAsync(
                                () -> {
                                    try {
                                        return getGithubPullRequestsForBranch(repoFullName, branch.getBranchName(), accessToken, timeZone);
                                    } catch (Exception e) {
                                        throw new CompletionException(e);
                                    }
                                }, githubExecutor);

                        // Once PRs are available, fetch commits
                        CompletableFuture<List<CommitDto>> commitsFuture = pullRequestsFuture.thenApplyAsync(pullRequests -> {
                            try {
                                // Ignore pullRequests list here; we’ll compute commits by branch name.
                                return getAllCommitsForBranchAndPRs(
                                        repoFullName,
                                        branch.getBaseBranchName(),
                                        branch.getBranchName(),
                                        accessToken,
                                        timeZone
                                );
                            } catch (Exception e) {
                                throw new CompletionException(e);
                            }
                        }, githubExecutor);

                        // Wait for all to complete
                        String branchSha = branchShaFuture.join();
                        String baseBranchSha = baseBranchShaFuture.join();
                        List<PullRequestDto> pullRequests = pullRequestsFuture.join();
                        List<CommitDto> commits = commitsFuture.join();

                        return new WorkItemGithubBranchDetails(
                                branchSha,
                                branch.getBranchName(),
                                baseBranchSha,
                                branch.getBaseBranchName(),
                                branch.getRepoId(),
                                repoFullName.substring(repoFullName.indexOf("/") + 1),
                                branch.getLastCommitHash(),
                                branch.getBranchLink(),
                                DateTimeUtils.convertServerDateToUserTimezone(branch.getCreatedDateTime(), timeZone),
                                commits,
                                pullRequests
                        );
                    } catch (Exception ex) {
                        System.err.println("Branch processing failed for branchId=" + branch.getBranchName() + ": " + ex.getMessage());
                        return null;
                    }
                }, githubExecutor))
                .collect(Collectors.toList());

        List<WorkItemGithubBranchDetails> branchDetailsList = futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return new WorkItemBranchesResponse(workItemId, branchDetailsList);
    }

    private void validateGithubOAuthToken(GithubAccount account) {
        if (!Boolean.TRUE.equals(account.getIsLinked())) {
            throw new ValidationFailedException("Re-Link your GitHub account.");
        }

        if (!isOAuthTokenValid(account.getGithubAccessToken())) {
            account.setIsLinked(false);
            githubAccountRepository.save(account);
            throw new ValidationFailedException("GitHub access token is invalid. Please re-link your account.");
        }
    }

    private boolean isRepoAllowed(String fullRepoName, Set<String> allowedOwnerRepoKeys, boolean allowAll) {
        String[] parts = fullRepoName.split("/");
        String owner = parts[0].toLowerCase();
        String repo = parts[1].toLowerCase();

        return allowAll
                || allowedOwnerRepoKeys.contains(owner + "/" + repo)
                || allowedOwnerRepoKeys.contains("*" + "/" + repo)
                || allowedOwnerRepoKeys.contains(owner + "/" + "*");
    }

    private String getBranchShaSafely(String repoName, String branchName, String accessToken) {
        try {
            return getLatestCommitHash(repoName, branchName, accessToken);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return "Branch Not Found";
            } else throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private List<CommitDto> getCommitsForAllPullRequestsForBranch(String repoName,
                                                                  String branchName,
                                                                  String accessToken,
                                                                  String timeZone) throws Exception {
        String[] parts = repoName.split("/");
        if (parts.length != 2) {
            return new ArrayList<>();
        }
        String owner = parts[0];

        // All PRs (open + closed + merged) whose head is exactly this branch
        String prListUrl = "https://api.github.com/repos/" + repoName
                + "/pulls?head=" + owner + ":" + branchName + "&state=all";

        JsonNode prResponse;
        try {
            prResponse = makeGitHubGetRequest(prListUrl, accessToken);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return new ArrayList<>();
            } else {
                throw e;
            }
        }

        if (prResponse == null || !prResponse.isArray() || prResponse.size() == 0) {
            return new ArrayList<>();
        }

        List<CommitDto> allCommits = new ArrayList<>();
        Set<String> seenCommitHashes = new HashSet<>();

        for (JsonNode prNode : prResponse) {
            if (!prNode.hasNonNull("commits_url")) {
                continue;
            }

            String commitsUrl = prNode.get("commits_url").asText();

            JsonNode commitsResponse;
            try {
                commitsResponse = makeGitHubGetRequest(commitsUrl, accessToken);
            } catch (HttpClientErrorException e) {
                if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                    continue; // skip broken PR
                } else {
                    throw e;
                }
            }

            if (commitsResponse == null || !commitsResponse.isArray()) {
                continue;
            }

            for (JsonNode commit : commitsResponse) {
                String sha = commit.get("sha").asText();
                if (seenCommitHashes.contains(sha)) {
                    continue;
                }

                JsonNode commitInfo = commit.get("commit");
                JsonNode authorNode = commitInfo.get("author");
                JsonNode userNode = commit.get("author");

                CommitDto dto = new CommitDto();
                dto.setCommitHash(sha);
                dto.setMessage(commitInfo.get("message").asText());
                dto.setAuthorName(authorNode != null && authorNode.hasNonNull("name")
                        ? authorNode.get("name").asText()
                        : null);

                if (authorNode != null && authorNode.hasNonNull("date")) {
                    Instant commitInstant = Instant.parse(authorNode.get("date").asText());
                    LocalDateTime utcCommitTime = LocalDateTime.ofInstant(commitInstant, ZoneOffset.UTC);
                    dto.setCommitDateTime(
                            DateTimeUtils.convertServerDateToUserTimezone(utcCommitTime, timeZone)
                    );
                }

                dto.setUrl(commit.get("html_url").asText());

                if (userNode != null && !userNode.isNull()) {
                    dto.setAuthorLogin(
                            userNode.hasNonNull("login") ? userNode.get("login").asText() : null
                    );
                    dto.setAuthorAvatarUrl(
                            userNode.hasNonNull("avatar_url") ? userNode.get("avatar_url").asText() : null
                    );
                }

                seenCommitHashes.add(sha);
                allCommits.add(dto);
            }
        }

        return allCommits;
    }

    private String getDefaultBranchName(String repoName, String accessToken) throws Exception {
        String url = "https://api.github.com/repos/" + repoName;
        JsonNode response = makeGitHubGetRequest(url, accessToken);

        if (response != null && response.hasNonNull("default_branch")) {
            return response.get("default_branch").asText();
        }
        // Fallback if GitHub response is weird
        return "main";
    }

    private List<CommitDto> getAllCommitsForBranchAndPRs(String repoName,
                                                         String baseBranch,
                                                         String branchName,
                                                         String accessToken,
                                                         String timeZone) throws Exception {
        List<CommitDto> result = new ArrayList<>();

        // 1) All commits from all PRs for this branch
        List<CommitDto> prCommits = getCommitsForAllPullRequestsForBranch(
                repoName, branchName, accessToken, timeZone
        );
        result.addAll(prCommits);

        Set<String> seenShas = prCommits.stream()
                .map(CommitDto::getCommitHash)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        String compareUrl = "https://api.github.com/repos/" + repoName
                + "/compare/" + baseBranch + "..." + branchName;

        JsonNode compareResponse;
        try {
            compareResponse = makeGitHubGetRequest(compareUrl, accessToken);
        } catch (HttpClientErrorException e) {
            // If branch is deleted or compare fails, just return PR commits
            if (e.getStatusCode() == HttpStatus.NOT_FOUND
                    || e.getStatusCode() == HttpStatus.UNPROCESSABLE_ENTITY) {
                return result;
            } else {
                throw e;
            }
        }

        if (compareResponse != null && compareResponse.hasNonNull("commits") && compareResponse.get("commits").isArray()) {
            for (JsonNode commit : compareResponse.get("commits")) {
                String sha = commit.get("sha").asText();
                if (seenShas.contains(sha)) {
                    continue;
                }

                JsonNode commitInfo = commit.get("commit");
                JsonNode authorNode = commitInfo.get("author");
                JsonNode userNode = commit.get("author");

                CommitDto dto = new CommitDto();
                dto.setCommitHash(sha);
                dto.setMessage(commitInfo.get("message").asText());
                dto.setAuthorName(authorNode != null && authorNode.hasNonNull("name")
                        ? authorNode.get("name").asText()
                        : null);

                if (authorNode != null && authorNode.hasNonNull("date")) {
                    Instant commitInstant = Instant.parse(authorNode.get("date").asText());
                    LocalDateTime utcCommitTime = LocalDateTime.ofInstant(commitInstant, ZoneOffset.UTC);
                    dto.setCommitDateTime(
                            DateTimeUtils.convertServerDateToUserTimezone(utcCommitTime, timeZone)
                    );
                }

                dto.setUrl(commit.get("html_url").asText());

                if (userNode != null && !userNode.isNull()) {
                    dto.setAuthorLogin(
                            userNode.hasNonNull("login") ? userNode.get("login").asText() : null
                    );
                    dto.setAuthorAvatarUrl(
                            userNode.hasNonNull("avatar_url") ? userNode.get("avatar_url").asText() : null
                    );
                }

                seenShas.add(sha);
                result.add(dto);
            }
        }

        return result;
    }

    private List<CommitDto> getGithubCommitsForBranch(String repoName,
                                                      String baseBranchName,
                                                      String branchName,
                                                      String accessToken,
                                                      String timeZone) throws Exception {
        // 1) If there is an OPEN PR for this branch, always prefer PR commits.
        //    This matches GitHub's PR UI exactly and avoids the conflict-history issue.
        List<CommitDto> openPrCommits = getCommitsFromPullRequest(repoName, branchName, accessToken, timeZone, true);
        if (!openPrCommits.isEmpty()) {
            return openPrCommits;
        }

        // 2) No open PR -> use compare (branch vs base)
        String compareUrl = "https://api.github.com/repos/" + repoName + "/compare/"
                + baseBranchName + "..." + branchName;

        JsonNode compareResponse;
        try {
            compareResponse = makeGitHubGetRequest(compareUrl, accessToken);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                // 3) Branch deleted -> fallback to commits from last (merged/closed) PR
                return getCommitsFromPullRequest(repoName, branchName, accessToken, timeZone, false);
            } else {
                throw e;
            }
        }

        // 4) Branch exists and no open PR -> build commits from compare as before
        List<CommitDto> commits = new ArrayList<>();
        for (JsonNode commit : compareResponse.get("commits")) {
            JsonNode commitInfo = commit.get("commit");
            JsonNode authorNode = commitInfo.get("author");
            JsonNode userNode = commit.get("author");

            CommitDto dto = new CommitDto();
            dto.setCommitHash(commit.get("sha").asText());
            dto.setMessage(commitInfo.get("message").asText());
            dto.setAuthorName(authorNode != null && authorNode.hasNonNull("name")
                    ? authorNode.get("name").asText()
                    : null);

            if (authorNode != null && authorNode.hasNonNull("date")) {
                Instant commitInstant = Instant.parse(authorNode.get("date").asText());
                LocalDateTime utcCommitTime = LocalDateTime.ofInstant(commitInstant, ZoneOffset.UTC);
                dto.setCommitDateTime(
                        DateTimeUtils.convertServerDateToUserTimezone(utcCommitTime, timeZone)
                );
            }

            dto.setUrl(commit.get("html_url").asText());

            if (userNode != null && !userNode.isNull()) {
                dto.setAuthorLogin(userNode.hasNonNull("login") ? userNode.get("login").asText() : null);
                dto.setAuthorAvatarUrl(userNode.hasNonNull("avatar_url") ? userNode.get("avatar_url").asText() : null);
            }

            commits.add(dto);
        }

        return commits;
    }


    private List<CommitDto> getCommitsFromPullRequest(String repoName,
                                                      String branchName,
                                                      String accessToken,
                                                      String timeZone,
                                                      boolean onlyOpen) throws Exception {
        String[] parts = repoName.split("/");
        if (parts.length != 2) {
            return new ArrayList<>();
        }
        String owner = parts[0];

        String state = onlyOpen ? "open" : "all";

        String prListUrl = "https://api.github.com/repos/" + repoName
                + "/pulls?head=" + owner + ":" + branchName + "&state=" + state;

        JsonNode prResponse;
        try {
            prResponse = makeGitHubGetRequest(prListUrl, accessToken);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return new ArrayList<>();
            } else {
                throw e;
            }
        }

        if (prResponse == null || !prResponse.isArray() || prResponse.size() == 0) {
            return new ArrayList<>();
        }

        // Choose latest PR based on updated_at (RFC 3339), but using String comparison fallback
        JsonNode selectedPr = prResponse.get(0);
        for (JsonNode prNode : prResponse) {
            if (prNode.hasNonNull("updated_at") && selectedPr.hasNonNull("updated_at")) {
                String currentUpdated = prNode.get("updated_at").asText();
                String selectedUpdated = selectedPr.get("updated_at").asText();
                // Lexical comparison works for ISO timestamps (RFC 3339)
                if (currentUpdated.compareTo(selectedUpdated) > 0) {
                    selectedPr = prNode;
                }
            }
        }

        String commitsUrl = selectedPr.get("commits_url").asText();

        JsonNode commitsResponse;
        try {
            commitsResponse = makeGitHubGetRequest(commitsUrl, accessToken);
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                return new ArrayList<>();
            } else {
                throw e;
            }
        }

        List<CommitDto> commits = new ArrayList<>();
        for (JsonNode commit : commitsResponse) {
            JsonNode commitInfo = commit.get("commit");
            JsonNode authorNode = commitInfo.get("author");
            JsonNode userNode = commit.get("author");

            CommitDto dto = new CommitDto();
            dto.setCommitHash(commit.get("sha").asText());
            dto.setMessage(commitInfo.get("message").asText());
            dto.setAuthorName(authorNode != null && authorNode.hasNonNull("name")
                    ? authorNode.get("name").asText()
                    : null);

            if (authorNode != null && authorNode.hasNonNull("date")) {
                Instant commitInstant = Instant.parse(authorNode.get("date").asText());
                LocalDateTime utcCommitTime = LocalDateTime.ofInstant(commitInstant, ZoneOffset.UTC);
                dto.setCommitDateTime(
                        DateTimeUtils.convertServerDateToUserTimezone(utcCommitTime, timeZone)
                );
            }

            dto.setUrl(commit.get("html_url").asText());

            if (userNode != null && !userNode.isNull()) {
                dto.setAuthorLogin(
                        userNode.hasNonNull("login") ? userNode.get("login").asText() : null
                );
                dto.setAuthorAvatarUrl(
                        userNode.hasNonNull("avatar_url") ? userNode.get("avatar_url").asText() : null
                );
            }

            commits.add(dto);
        }

        return commits;
    }

    private List<PullRequestDto> getGithubPullRequestsForBranch(String repoName, String branchName, String accessToken, String timeZone) throws Exception {
        String url = "https://api.github.com/repos/" + repoName + "/pulls?head=" + repoName.split("/")[0] + ":" + branchName + "&state=all";
        JsonNode prResponse = makeGitHubGetRequest(url, accessToken);

        List<PullRequestDto> pullRequests = new ArrayList<>();
        for (JsonNode pr : prResponse) {
            PullRequestDto dto = mapPullRequest(pr, accessToken, timeZone);
            pullRequests.add(dto);
        }

        return pullRequests;
    }

    private PullRequestDto mapPullRequest(JsonNode pr, String installationId, String timeZone) throws Exception {
        PullRequestDto dto = new PullRequestDto();

        dto.setTitle(pr.get("title").asText());
        dto.setStatus(pr.get("state").asText());

        // Convert created_at
        Instant createdAtInstant = Instant.parse(pr.get("created_at").asText());;

        LocalDateTime createdAtUtc = LocalDateTime.ofInstant(createdAtInstant, ZoneOffset.UTC);
        dto.setCreatedDateTime(DateTimeUtils.convertServerDateToUserTimezone(createdAtUtc, timeZone));

        // Convert updated_at
        Instant updatedAtInstant = Instant.parse(pr.get("updated_at").asText());
        LocalDateTime updatedAtUtc = LocalDateTime.ofInstant(updatedAtInstant, ZoneOffset.UTC);
        dto.setUpdatedDateTime(DateTimeUtils.convertServerDateToUserTimezone(updatedAtUtc, timeZone));

        dto.setUrl(pr.get("html_url").asText());
        dto.setPullRequestId(pr.get("number").asText());
        dto.setUserLogin(pr.get("user").get("login").asText());
        JsonNode userNode = pr.get("user");
        if (userNode != null && !userNode.isNull()) {
            dto.setAuthorName(userNode.hasNonNull("login") ? userNode.get("login").asText() : null);
            dto.setAuthorAvatarUrl(userNode.hasNonNull("avatar_url") ? userNode.get("avatar_url").asText() : null);
        }

        // Convert closed_at (if exists)
        if (pr.hasNonNull("closed_at")) {
            Instant closedAtInstant = Instant.parse(pr.get("closed_at").asText());
            LocalDateTime closedAtUtc = LocalDateTime.ofInstant(closedAtInstant, ZoneOffset.UTC);
            dto.setClosedDateTime(DateTimeUtils.convertServerDateToUserTimezone(closedAtUtc, timeZone));
        }

        // Convert merged_at (if exists)
        if (pr.hasNonNull("merged_at")) {
            dto.setIsMerged(true);
            Instant mergedAtInstant = Instant.parse(pr.get("merged_at").asText());
            LocalDateTime mergedAtUtc = LocalDateTime.ofInstant(mergedAtInstant, ZoneOffset.UTC);
            dto.setMergedDateTime(DateTimeUtils.convertServerDateToUserTimezone(mergedAtUtc, timeZone));
            dto.setStatus("merged"); // override status
        } else {
            dto.setIsMerged(false);
        }

        if (pr.hasNonNull("closed_by")) {
            dto.setClosedBy(pr.get("closed_by").get("login").asText());
        }

        dto.setLabels(extractLabels(pr));
        dto.setAssignees(extractAssignees(pr));
        dto.setRequestedReviewers(extractRequestedReviewers(pr));
        dto.setApprovedBy(extractApprovals(pr.get("url").asText(), installationId));

        return dto;
    }


    private List<String> extractLabels(JsonNode pr) {
        List<String> labels = new ArrayList<>();
        if (pr.has("labels")) {
            for (JsonNode label : pr.get("labels")) {
                labels.add(label.get("name").asText());
            }
        }
        return labels;
    }

    private List<String> extractAssignees(JsonNode pr) {
        List<String> assignees = new ArrayList<>();
        if (pr.has("assignees")) {
            for (JsonNode assignee : pr.get("assignees")) {
                assignees.add(assignee.get("login").asText());
            }
        }
        return assignees;
    }

    private List<String> extractRequestedReviewers(JsonNode pr) {
        List<String> reviewers = new ArrayList<>();
        if (pr.has("requested_reviewers")) {
            for (JsonNode reviewer : pr.get("requested_reviewers")) {
                reviewers.add(reviewer.get("login").asText());
            }
        }
        return reviewers;
    }

    private List<String> extractApprovals(String prApiUrl, String installationId) throws Exception {
        Set<String> approvedUsers = new HashSet<>();
        String reviewUrl = prApiUrl + "/reviews";
        JsonNode reviews = makeGitHubGetRequest(reviewUrl, installationId);

        for (JsonNode review : reviews) {
            if ("APPROVED".equalsIgnoreCase(review.get("state").asText())) {
                approvedUsers.add(review.get("user").get("login").asText());
            }
        }

        return new ArrayList<>(approvedUsers);
    }


    public HttpHeaders createGithubHeaderForRestTemplate (String bearerToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(bearerToken);
        headers.set("Accept", "application/vnd.github.v3+json");
        return headers;
    }

    public GithubAccountAndRepoPreferenceResponse addGithubAccountAndItsRepo(AddGithubAccountAndItsRepoRequest request, String accountIds, String timeZone) {

        List<Long> accountIdList = CommonUtils.convertToLongList(accountIds);

        if (!accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdInAndIsActiveAndRoleIdIn(Constants.EntityTypes.ORG, request.getOrgId(), accountIdList, true, Constants.ROLE_IDS_FOR_CREATE_UPDATE_GITHUB_IN_PREFERENCE)) {
            throw new ValidationFailedException("You do not have permission to add github account and repo to org preference.");
        }

        String username = request.getGithubAccountUserName().trim();
        String repo = request.getGithubAccountRepoName().trim();

        Optional<GithubAccountAndRepoPreference> githubAccountAndRepoPreferenceOptional = githubAccountAndRepoPreferenceRepository.findByOrgIdAndGithubAccountUserNameIgnoreCaseAndGithubAccountRepoNameIgnoreCase(request.getOrgId(), username, repo);

        GithubAccountAndRepoPreference saved;
        if (githubAccountAndRepoPreferenceOptional.isPresent()) {
            GithubAccountAndRepoPreference githubAccountAndRepoPreferenceDb = githubAccountAndRepoPreferenceOptional.get();
            if (Boolean.TRUE.equals(githubAccountAndRepoPreferenceDb.getIsActive())) {
                throw new ValidationFailedException(ErrorConstant.Github.DUPLICATE_ACTIVE_ENTRY);
            } else {
                githubAccountAndRepoPreferenceDb.setIsActive(true);
                githubAccountAndRepoPreferenceDb.setUpdatedDateTime(LocalDateTime.now());
                saved = githubAccountAndRepoPreferenceRepository.save(githubAccountAndRepoPreferenceDb);
            }
        } else {
            GithubAccountAndRepoPreference githubAccountAndRepoPreference = new GithubAccountAndRepoPreference();
            githubAccountAndRepoPreference.setGithubAccountUserName(username);
            githubAccountAndRepoPreference.setGithubAccountRepoName(repo);
            githubAccountAndRepoPreference.setOrgId(request.getOrgId());
            githubAccountAndRepoPreference.setIsActive(true);
            saved = githubAccountAndRepoPreferenceRepository.save(githubAccountAndRepoPreference);
        }

        GithubAccountAndRepoPreferenceResponse response = new GithubAccountAndRepoPreferenceResponse();
        BeanUtils.copyProperties(saved, response);

        if (response.getCreatedDateTime() != null) {
            response.setCreatedDateTime(DateTimeUtils.convertServerDateToUserTimezone(response.getCreatedDateTime(), timeZone));
        }
        if (response.getUpdatedDateTime() != null) {
            response.setUpdatedDateTime(DateTimeUtils.convertServerDateToUserTimezone(response.getUpdatedDateTime(), timeZone));
        }

        return response;
    }

    public String removeGithubAccountAndItsRepo(RemoveGithubAccountAndRepoRequest request, String accountIds) {
        List<Long> accountIdList = CommonUtils.convertToLongList(accountIds);

        GithubAccountAndRepoPreference githubAccountAndRepoPreference = githubAccountAndRepoPreferenceRepository.findById(request.getGithubAccountAndRepoPreferenceId()).orElseThrow(() -> new ValidationFailedException("GitHub account and repo entry not found"));

        if (!accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdInAndIsActiveAndRoleIdIn(Constants.EntityTypes.ORG, githubAccountAndRepoPreference.getOrgId(), accountIdList, true, Constants.ROLE_IDS_FOR_CREATE_UPDATE_GITHUB_IN_PREFERENCE)) {
            throw new ValidationFailedException("You do not have permission to remove this GitHub account and repo from org preference");
        }

        if (!Boolean.TRUE.equals(githubAccountAndRepoPreference.getIsActive())) {
            throw new ValidationFailedException("This GitHub account and repo is already inactive");
        }

        githubAccountAndRepoPreference.setIsActive(false);
        githubAccountAndRepoPreference.setUpdatedDateTime(LocalDateTime.now());
        githubAccountAndRepoPreferenceRepository.save(githubAccountAndRepoPreference);

        return "GitHub account and repo removed successfully from the org";
    }

    public List<GithubAccountAndRepoPreferenceResponse> getAllGithubAccountAndItsRepo(Long orgId, String accountIds, String timeZone) {
        List<Long> accountIdList = CommonUtils.convertToLongList(accountIds);

        if (!accessDomainRepository.existsByEntityTypeIdAndEntityIdAndAccountIdInAndIsActiveAndRoleIdIn(Constants.EntityTypes.ORG, orgId, accountIdList, true, Constants.ROLE_IDS_FOR_CREATE_UPDATE_GITHUB_IN_PREFERENCE)) {
            throw new ValidationFailedException("You do not have permission to fetch GitHub preferences for this org");
        }

        List<GithubAccountAndRepoPreference> githubAccountAndRepoPreferenceList = githubAccountAndRepoPreferenceRepository.findByOrgIdAndIsActive(orgId, true);
        List<GithubAccountAndRepoPreferenceResponse> responseList = new ArrayList<>();

        for (GithubAccountAndRepoPreference entity : githubAccountAndRepoPreferenceList) {
            GithubAccountAndRepoPreferenceResponse response = new GithubAccountAndRepoPreferenceResponse();
            BeanUtils.copyProperties(entity, response);

            if (response.getCreatedDateTime() != null) {
                response.setCreatedDateTime(DateTimeUtils.convertServerDateToUserTimezone(response.getCreatedDateTime(), timeZone));
            }
            if (response.getUpdatedDateTime() != null) {
                response.setUpdatedDateTime(DateTimeUtils.convertServerDateToUserTimezone(response.getUpdatedDateTime(), timeZone));
            }

            responseList.add(response);
        }

        return responseList;
    }

    @Transactional
    public LinkedGithubResponse linkGithubToCrossOrg(User user, CrossOrgGithubLinkRequest request, String timeZone) throws Exception {
        Long sourceOrg = request.getSourceOrgId();
        Long targetOrg = request.getTargetOrgId();

        if (!userAccountRepository.existsByFkUserIdUserIdAndOrgIdAndIsActive(user.getUserId(), sourceOrg, true)) {
            throw new ValidationFailedException("User is not part of source organization.");
        }
        if (!userAccountRepository.existsByFkUserIdUserIdAndOrgIdAndIsActive(user.getUserId(), targetOrg, true)) {
            throw new ValidationFailedException("User is not part of target organization.");
        }

        GithubAccount sourceAccount = githubAccountRepository
                .findByFkUserIdUserIdAndOrgId(user.getUserId(), sourceOrg)
                .orElseThrow(() -> new ValidationFailedException("GitHub is not linked in source organization."));

        if (!Boolean.TRUE.equals(sourceAccount.getIsLinked())) {
            throw new ValidationFailedException("GitHub account in source org is not linked.");
        }

        if (!isOAuthTokenValid(sourceAccount.getGithubAccessToken())) {
            throw new ValidationFailedException("GitHub access token in source org is invalid. Please re-link it first.");
        }

        GithubAccount targetAccount = githubAccountRepository
                .findByFkUserIdUserIdAndOrgId(user.getUserId(), targetOrg)
                .orElseGet(GithubAccount::new);

        targetAccount.setFkUserId(user);
        targetAccount.setOrgId(targetOrg);
        targetAccount.setGithubUserName(sourceAccount.getGithubUserName());
        targetAccount.setGithubUserCode(sourceAccount.getGithubUserCode());
        targetAccount.setGithubAccessToken(sourceAccount.getGithubAccessToken());
        targetAccount.setIsLinked(true);

        githubAccountRepository.save(targetAccount);

        return new LinkedGithubResponse(
                user.getUserId(),
                user.getFirstName(),
                user.getLastName(),
                true,
                "GitHub account successfully linked to target organization.",
                sourceAccount.getGithubUserName(),
                DateTimeUtils.convertServerDateToUserTimezone(targetAccount.getCreatedDateTime(), timeZone),
                DateTimeUtils.convertServerDateToUserTimezone(targetAccount.getLastUpdatedDateTime(), timeZone)
        );
    }


    public List<OrgIdOrgName> getAllOrgConnectedToGithub(User foundUser, String timeZone) {
        List<Long> orgIdList = githubAccountRepository.findOrgIdByFkUserIdUserIdAndIsValidAndIsLinked(foundUser.getUserId(), true);
        List<OrgIdOrgName> orgIdOrgNameList = organizationRepository.findOrgIdAndOrganizationNameByOrgId(orgIdList);
        return orgIdOrgNameList;
    }
}
