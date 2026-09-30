package com.ibrahimhassen.barber_booking_agent.calendar;

import com.google.api.services.calendar.model.Event;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Map;

@RestController
@RequestMapping("/calendar")
public class CalendarController {

    private final GoogleCalendarService calendarService;

    public CalendarController(GoogleCalendarService calendarService) {
        this.calendarService = calendarService;
    }

    @PostMapping("/test-event")
    public Map<String, String> createTestEvent()
            throws GeneralSecurityException, IOException {

        Event event = calendarService.createEvent(
                "MVP Test Customer",
                "Haircut",
                "2026-10-01T17:00:00-04:00",
                "2026-10-01T18:00:00-04:00"
        );

        return Map.of(
                "message", "Calendar event created",
                "eventId", event.getId()
        );
    }

    @GetMapping("/availability")
    public Map<String, Boolean> checkAvailability(
            @RequestParam String start,
            @RequestParam String end)
            throws GeneralSecurityException, IOException {

        return Map.of(
                "available",
                calendarService.isAvailable(start, end)
        );
    }
}
