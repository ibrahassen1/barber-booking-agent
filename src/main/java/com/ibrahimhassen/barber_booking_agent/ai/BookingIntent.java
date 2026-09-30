package com.ibrahimhassen.barber_booking_agent.ai;

public record BookingIntent(
        String intent,
        String service,
        String startTime
) {}
