package com.ibrahimhassen.barber_booking_agent.sms;

import com.ibrahimhassen.barber_booking_agent.ai.BookingAiService;
import com.ibrahimhassen.barber_booking_agent.ai.BookingIntent;
import com.ibrahimhassen.barber_booking_agent.booking.Booking;
import com.ibrahimhassen.barber_booking_agent.booking.BookingService;
import com.ibrahimhassen.barber_booking_agent.calendar.GoogleCalendarService;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/sms")
public class SmsController {

    private final BookingAiService aiService;
    private final GoogleCalendarService calendarService;
    private final BookingService bookingService;

    public SmsController(
            BookingAiService aiService,
            GoogleCalendarService calendarService,
            BookingService bookingService) {

        this.aiService = aiService;
        this.calendarService = calendarService;
        this.bookingService = bookingService;
    }

    @PostMapping(
            value = "/incoming",
            produces = MediaType.APPLICATION_XML_VALUE
    )
    public String receiveSms(
            @RequestParam("From") String from,
            @RequestParam("Body") String body) {

        System.out.println("SMS from " + from + ": " + body);

        try {
            BookingIntent intent = aiService.understand(body);

            if (!"BOOK".equals(intent.intent())) {
                return twiml(
                        "I couldn't understand that booking request. Try something like: haircut tomorrow at 3 PM."
                );
            }

            OffsetDateTime start = OffsetDateTime.parse(intent.startTime());
            OffsetDateTime end = start.plusHours(1);

            String startRfc3339 = start.format(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
            );

            String endRfc3339 = end.format(
                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
            );

            boolean available = calendarService.isAvailable(
                    startRfc3339,
                    endRfc3339
            );

            if (!available) {
                return twiml(
                        "That time is already booked. Send me another time and I'll check it."
                );
            }

            Booking booking = new Booking();
            booking.setCustomerName("SMS Customer");
            booking.setCustomerPhone(from);
            booking.setService(intent.service());
            booking.setStartTime(start.toLocalDateTime());
            booking.setEndTime(end.toLocalDateTime());

            bookingService.createBooking(booking);

            calendarService.createEvent(
                    from,
                    intent.service(),
                    startRfc3339,
                    endRfc3339
            );

            String formattedTime = start.format(
                    DateTimeFormatter.ofPattern("EEE MMM d 'at' h:mm a")
            );

            return twiml(
                    "You're booked for " + intent.service()
                            + " on " + formattedTime + "."
            );

        } catch (Exception e) {
            e.printStackTrace();

            return twiml(
                    "Something went wrong while booking. Try again in a moment."
            );
        }
    }

    private String twiml(String message) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <Response>
                    <Message>%s</Message>
                </Response>
                """.formatted(message);
    }
}
