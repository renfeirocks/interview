package com.renfei.booking.cinema.strategy;

import java.util.List;

public interface SeatingStrategy {
  List<int[]> selectSeats(
      int[][] seatingMap,
      int totalRows,
      int seatsPerRow,
      int numTickets,
      int[] defaultColPriority,
      String startPosition);
}
