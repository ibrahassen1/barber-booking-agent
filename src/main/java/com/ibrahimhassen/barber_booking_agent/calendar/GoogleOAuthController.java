package com.ibrahimhassen.barber_booking_agent.calendar;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/oauth2")
public class GoogleOAuthController {

    private final GoogleTokenService tokenService;

    public GoogleOAuthController(GoogleTokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Value("${google.oauth.client-id}")
    private String clientId;

    @Value("${google.oauth.client-secret}")
    private String clientSecret;

    @Value("${google.oauth.redirect-uri}")
    private String redirectUri;

    @GetMapping("/google")
    public ResponseEntity<Void> authorizeGoogle() {

        String scope = "https://www.googleapis.com/auth/calendar";

        String authorizationUrl =
                "https://accounts.google.com/o/oauth2/v2/auth"
                        + "?client_id=" + encode(clientId)
                        + "&redirect_uri=" + encode(redirectUri)
                        + "&response_type=code"
                        + "&scope=" + encode(scope)
                        + "&access_type=offline"
                        + "&prompt=consent";

        return ResponseEntity
                .status(302)
                .header("Location", authorizationUrl)
                .build();
    }

    @GetMapping("/callback/google")
    public Map<String, String> googleCallback(@RequestParam String code) {
        tokenService.exchangeCode(code);

        return Map.of(
                "message", "Google Calendar connected successfully"
        );
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
