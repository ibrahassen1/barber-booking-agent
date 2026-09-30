package com.ibrahimhassen.barber_booking_agent.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;

@Service
public class BookingAiService {

    private final RestClient restClient = RestClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${OPENAI_API_KEY}")
    private String apiKey;

    public BookingIntent understand(String message) {

        String now = ZonedDateTime.now(
                ZoneId.of("America/New_York")
        ).toString();

        String prompt = """
                You extract booking information for a barber.

                Current date/time: %s
                Timezone: America/New_York.

                Understand casual language and slang:
                - cut = Haircut
                - tmr = tomorrow
                - slide = come in for an appointment

                Return ONLY JSON exactly like:
                {"intent":"BOOK","service":"Haircut","startTime":"2026-10-01T15:00:00-04:00"}

                intent must be BOOK or UNKNOWN.
                startTime must be RFC3339 with UTC offset.

                Customer message:
                %s
                """.formatted(now, message);

        Map<String, Object> body = Map.of(
                "model", "gpt-5.6-luna",
                "input", prompt
        );

        String response = restClient.post()
                .uri("https://api.openai.com/v1/responses")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .body(body)
                .retrieve()
                .body(String.class);

        try {
            JsonNode root = objectMapper.readTree(response);

            for (JsonNode output : root.path("output")) {
                for (JsonNode content : output.path("content")) {

                    if ("output_text".equals(content.path("type").asText())) {

                        String text = content.path("text").asText()
                                .replace("```json", "")
                                .replace("```", "")
                                .trim();

                        System.out.println("AI parsed booking: " + text);

                        return objectMapper.readValue(
                                text,
                                BookingIntent.class
                        );
                    }
                }
            }

            throw new RuntimeException(
                    "OpenAI response contained no output_text: " + response
            );

        } catch (Exception e) {
            throw new RuntimeException(
                    "Could not parse AI booking response",
                    e
            );
        }
    }
}
