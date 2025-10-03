package com.renfei.booking.cinema.service;

import com.renfei.booking.cinema.strategy.impl.GICDefaultSeatingStrategy;
import com.renfei.booking.cinema.strategy.impl.GICCustomSeatingStrategy;
import java.util.List;

public class SeatSelectionService {
    public List<int[]> selectDefaultSeats(int[][] seatingMap, int totalRows, int seatsPerRow, int numTickets, int[] defaultColPriority) {
        return new GICDefaultSeatingStrategy().selectSeats(seatingMap, totalRows, seatsPerRow, numTickets, defaultColPriority, null);
    }

    public List<int[]> selectCustomSeats(int[][] seatingMap, int totalRows, int seatsPerRow, int numTickets, int[] defaultColPriority, String startPosition) {
        return new GICCustomSeatingStrategy().selectSeats(seatingMap, totalRows, seatsPerRow, numTickets, defaultColPriority, startPosition);
    }
}
