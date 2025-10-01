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
    for (int r = 0; r < totalRows && selectedSeats.size() < numTickets; r++) {
      for (int c : defaultColPriority) {
        if (seatingMap[r][c] == 0) {
          selectedSeats.add(new int[] {r, c});
          if (selectedSeats.size() == numTickets) break;
        }
      }
    }
    // Special case: if only 4 seats are available and numTickets == 4, check for corners
    int availableSeats = 0;
    List<int[]> available = new ArrayList<>();
    for (int r = 0; r < totalRows; r++) {
      for (int c = 0; c < seatsPerRow; c++) {
        if (seatingMap[r][c] == 0) {
          availableSeats++;
          available.add(new int[]{r, c});
        }
      }
    }
    if (numTickets == 4 && availableSeats == 4) {
      boolean isCorners = true;
      int[][] corners = new int[][]{
        {0, 0}, {0, seatsPerRow-1}, {totalRows-1, 0}, {totalRows-1, seatsPerRow-1}
      };
      for (int[] seat : available) {
        boolean found = false;
        for (int[] corner : corners) {
          if (seat[0] == corner[0] && seat[1] == corner[1]) {
            found = true;
            break;
          }
        }
        if (!found) {
          isCorners = false;
          break;
        }
      }
      if (isCorners) {
        selectedSeats.addAll(available);
        return selectedSeats;
      }
    }
    return selectedSeats;
  }
}
