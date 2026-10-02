package com.ibrahimhassen.barber_booking_agent.calendar;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.time.Instant;

@Service
public class GoogleTokenService {

    private final RestClient restClient = RestClient.create();
    private final GoogleCredentialRepository credentialRepository;

    @Value("${google.oauth.client-id}")
    private String clientId;

    @Value("${google.oauth.client-secret}")
    private String clientSecret;

    @Value("${google.oauth.redirect-uri}")
    private String redirectUri;

    private String accessToken;
    private Instant accessTokenExpiresAt;

    public GoogleTokenService(
            GoogleCredentialRepository credentialRepository) {
        this.credentialRepository = credentialRepository;
    }

    public void exchangeCode(String code) {

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();

        body.add("code", code);
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("redirect_uri", redirectUri);
        body.add("grant_type", "authorization_code");

        TokenResponse response = restClient.post()
                .uri("https://oauth2.googleapis.com/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .body(TokenResponse.class);

        if (response == null || response.accessToken() == null) {
            throw new IllegalStateException(
                    "Google did not return an access token"
            );
        }

        this.accessToken = response.accessToken();

        long expiresIn = response.expiresIn() != null
                ? response.expiresIn()
                : 3600;

        this.accessTokenExpiresAt =
                Instant.now().plusSeconds(expiresIn);

        if (response.refreshToken() != null) {
            credentialRepository.save(
                    new GoogleCredential(
                            1L,
                            response.refreshToken()
                    )
            );

            System.out.println(
                    "Google refresh token saved to database"
            );
        }
    }

    public synchronized String getAccessToken() {

        if (accessToken != null
                && accessTokenExpiresAt != null
                && Instant.now().isBefore(
                        accessTokenExpiresAt.minusSeconds(60)
                )) {

            return accessToken;
        }

        refreshAccessToken();

        return accessToken;
    }

    private void refreshAccessToken() {

        GoogleCredential credential =
                credentialRepository.findById(1L)
                        .orElseThrow(() ->
                                new IllegalStateException(
                                        "Google Calendar has not been authorized yet"
                                )
                        );

        MultiValueMap<String, String> body =
                new LinkedMultiValueMap<>();

        body.add(
                "refresh_token",
                credential.getRefreshToken()
        );

        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("grant_type", "refresh_token");

        TokenResponse response = restClient.post()
                .uri("https://oauth2.googleapis.com/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(body)
                .retrieve()
                .body(TokenResponse.class);

        if (response == null || response.accessToken() == null) {
            throw new IllegalStateException(
                    "Could not refresh Google access token"
            );
        }

        this.accessToken = response.accessToken();

        long expiresIn = response.expiresIn() != null
                ? response.expiresIn()
                : 3600;

        this.accessTokenExpiresAt =
                Instant.now().plusSeconds(expiresIn);

        System.out.println(
                "Google access token refreshed automatically"
        );
    }

    public record TokenResponse(
            @JsonProperty("access_token")
            String accessToken,

            @JsonProperty("refresh_token")
            String refreshToken,

            @JsonProperty("expires_in")
            Long expiresIn,

            @JsonProperty("token_type")
            String tokenType,

            @JsonProperty("scope")
            String scope
    ) {}
}
