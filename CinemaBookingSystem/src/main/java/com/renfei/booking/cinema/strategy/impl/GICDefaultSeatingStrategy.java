package com.renfei.booking.cinema.strategy.impl;

import com.renfei.booking.cinema.strategy.SeatingStrategy;

import java.util.ArrayList;
import java.util.List;

public class GICDefaultSeatingStrategy implements SeatingStrategy {
  @Override
  public List<int[]> selectSeats(
      int[][] seatingMap,
      int totalRows,
      int seatsPerRow,
      int numTickets,
      int[] defaultColPriority,
      String startPosition) {
    List<int[]> selectedSeats = new ArrayList<>();
    for (int r = totalRows - 1; r >= 0 && selectedSeats.size() < numTickets; r--) {
      for (int c : defaultColPriority) {
        if (seatingMap[r][c] == 0) {
          selectedSeats.add(new int[] {r, c});
          if (selectedSeats.size() == numTickets) break;
        }
      }
    }
    return selectedSeats;
  }
}
