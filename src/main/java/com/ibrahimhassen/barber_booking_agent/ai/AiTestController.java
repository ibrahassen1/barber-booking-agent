package com.ibrahimhassen.barber_booking_agent.ai;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ai")
public class AiTestController {

    private final BookingAiService aiService;

    public AiTestController(BookingAiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/parse")
    public BookingIntent parse(@RequestBody String message) {
        return aiService.understand(message);
    }
}
