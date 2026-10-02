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
    private final TwilioSmsService smsService;

    public SmsController(
            BookingAiService aiService,
            GoogleCalendarService calendarService,
            BookingService bookingService,
            TwilioSmsService smsService) {

        this.aiService = aiService;
        this.calendarService = calendarService;
        this.bookingService = bookingService;
        this.smsService = smsService;
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
                smsService.sendSms(
                        from,
                        "I couldn't understand that booking request. Try something like: haircut tomorrow at 3 PM."
                );

                return emptyTwiml();
            }

            OffsetDateTime start = OffsetDateTime.parse(intent.startTime());
            OffsetDateTime end = start.plusHours(1);

            DateTimeFormatter rfc3339 =
                    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

            String startRfc3339 = start.format(rfc3339);
            String endRfc3339 = end.format(rfc3339);

            boolean available = calendarService.isAvailable(
                    startRfc3339,
                    endRfc3339
            );

            if (!available) {
                smsService.sendSms(
                        from,
                        "That time is already booked. Send me another time and I'll check it."
                );

                return emptyTwiml();
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

            smsService.sendSms(
                    from,
                    "You're booked for " + intent.service()
                            + " on " + formattedTime + "."
            );

            return emptyTwiml();

        } catch (Exception e) {
            e.printStackTrace();

            try {
                smsService.sendSms(
                        from,
                        "Something went wrong while booking. Try again in a moment."
                );
            } catch (Exception smsException) {
                smsException.printStackTrace();
            }

            return emptyTwiml();
        }
    }

    private String emptyTwiml() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <Response></Response>
                """;
    }
}
