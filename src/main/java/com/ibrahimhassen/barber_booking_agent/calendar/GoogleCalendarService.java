package com.ibrahimhassen.barber_booking_agent.calendar;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

@Service
public class GoogleCalendarService {

    private final GoogleTokenService tokenService;

    public GoogleCalendarService(GoogleTokenService tokenService) {
        this.tokenService = tokenService;
    }

    private Calendar calendar() throws GeneralSecurityException, IOException {
        HttpRequestInitializer initializer = request ->
                request.getHeaders().setAuthorization(
                        "Bearer " + tokenService.getAccessToken()
                );

        return new Calendar.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                initializer
        )
                .setApplicationName("Barber Booking Agent")
                .build();
    }

    public boolean isAvailable(String startTime, String endTime)
            throws GeneralSecurityException, IOException {

        FreeBusyRequest request = new FreeBusyRequest()
                .setTimeMin(new DateTime(startTime))
                .setTimeMax(new DateTime(endTime))
                .setTimeZone("America/New_York")
                .setItems(List.of(
                        new FreeBusyRequestItem().setId("primary")
                ));

        FreeBusyResponse response =
                calendar().freebusy().query(request).execute();

        List<TimePeriod> busy =
                response.getCalendars().get("primary").getBusy();

        return busy == null || busy.isEmpty();
    }

    public Event createEvent(
            String customerName,
            String service,
            String startTime,
            String endTime
    ) throws GeneralSecurityException, IOException {

        Event event = new Event()
                .setSummary(service + " - " + customerName);

        event.setStart(new EventDateTime()
                .setDateTime(new DateTime(startTime))
                .setTimeZone("America/New_York"));

        event.setEnd(new EventDateTime()
                .setDateTime(new DateTime(endTime))
                .setTimeZone("America/New_York"));

        return calendar()
                .events()
                .insert("primary", event)
                .execute();
    }
}
