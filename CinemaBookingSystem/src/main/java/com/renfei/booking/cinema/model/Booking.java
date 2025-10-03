package com.renfei.booking.cinema.model;

import java.util.Collections;
import java.util.List;

public class Booking {
    private final String bookingId;
    private final int numTickets;
    // Stores the row index and column index of the reserved seats.
    private final List<int[]> selectedSeats;

    public Booking(String bookingId, int numTickets, List<int[]> selectedSeats) {
        this.bookingId = bookingId;
        this.numTickets = numTickets;
        this.selectedSeats = Collections.unmodifiableList(selectedSeats);
    }

    public String getBookingId() {
        return bookingId;
    }

    public int getNumTickets() {
        return numTickets;
    }

    public List<int[]> getSelectedSeats() {
        return selectedSeats;
    }
}
