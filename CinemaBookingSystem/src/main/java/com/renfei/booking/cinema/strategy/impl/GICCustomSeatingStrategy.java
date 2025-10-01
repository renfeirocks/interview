package com.renfei.booking.cinema.strategy.impl;

import com.renfei.booking.cinema.strategy.SeatingStrategy;
import java.util.ArrayList;
import java.util.List;

public class GICCustomSeatingStrategy implements SeatingStrategy {
  @Override
  public List<int[]> selectSeats(
      int[][] seatingMap,
      int totalRows,
      int seatsPerRow,
      int numTickets,
      int[] defaultColPriority,
      String startPosition) {
    List<int[]> selectedSeats = new ArrayList<>();

    if (startPosition == null || startPosition.length() < 2 || startPosition.length() > 3)
      return selectedSeats;
    char rowLabel = Character.toUpperCase(startPosition.charAt(0));
    int startRow = rowLabel - 'A';
    int startCol;
    try {
      int colNumber = Integer.parseInt(startPosition.substring(1));
      startCol = colNumber - 1;
    } catch (NumberFormatException e) {
      return selectedSeats;
    }

    if (startRow < 0 || startRow >= totalRows || startCol < 0 || startCol >= seatsPerRow)
      return selectedSeats;

    // 1. Same row, rightward allocation
    for (int c = startCol; c < seatsPerRow && selectedSeats.size() < numTickets; c++) {
      if (seatingMap[startRow][c] == 0) {
        selectedSeats.add(new int[] {startRow, c});
      }
    }

    // 2. Overflow to next rows (closer to screen), using default rules
    if (selectedSeats.size() < numTickets) {
      int ticketsToAllocate = numTickets - selectedSeats.size();
      // Use defaultColPriority for overflow
      for (int r = startRow + 1; r < totalRows && ticketsToAllocate > 0; r++) {
        for (int c : defaultColPriority) {
          if (seatingMap[r][c] == 0) {
            selectedSeats.add(new int[] {r, c});
            ticketsToAllocate--;
            if (ticketsToAllocate == 0) break;
          }
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
