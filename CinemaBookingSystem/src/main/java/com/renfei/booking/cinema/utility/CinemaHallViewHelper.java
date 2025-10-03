package com.renfei.booking.cinema.utility;

import com.renfei.booking.cinema.model.Booking;
import com.renfei.booking.cinema.configuration.CinemaHallConfig;
import java.util.List;
import java.util.Map;

import static com.renfei.booking.cinema.configuration.CinemaHallConfig.SEAT_AVAILABLE;
import static com.renfei.booking.cinema.configuration.CinemaHallConfig.SEAT_RESERVED;

public class CinemaHallViewHelper {
    public static String displaySeatingMap(int[][] seatingMap, int totalRows, int seatsPerRow, Map<String, Booking> bookings, String currentBookingId) {
        StringBuilder sb = new StringBuilder();
        int seatSymbolWidth = 3;
        int totalWidth = seatsPerRow * seatSymbolWidth;
        String screenLabel = CinemaHallConfig.SCREEN;
        int screenLabelPadding = Math.max(0, (totalWidth - screenLabel.length()) / 2);
        sb.append("  ").append(" ".repeat(screenLabelPadding)).append(screenLabel).append("\n");
        sb.append("    ").append("-".repeat(totalWidth - 2)).append("\n");
        for (int r = totalRows - 1; r >= 0; r--) {
            sb.append(SeatUtil.rowIndexToLabel(r)).append(" ");
            for (int c = 0; c < seatsPerRow; c++) {
                char symbol = '.';
                boolean isBooked = false;
                String seatBookingId = null;
                if (seatingMap[r][c] == 1) {
                    isBooked = true;
                    for (Map.Entry<String, Booking> entry : bookings.entrySet()) {
                        for (int[] seat : entry.getValue().getSelectedSeats()) {
                            if (seat[0] == r && seat[1] == c) {
                                seatBookingId = entry.getKey();
                                break;
                            }
                        }
                        if (seatBookingId != null) break;
                    }
                }
                if (isBooked) {
                    if (currentBookingId != null && currentBookingId.equals(seatBookingId)) {
                        symbol = SEAT_AVAILABLE;
                    } else {
                        symbol = SEAT_RESERVED;
                    }
                }
                sb.append(String.format(" %2s", symbol));
            }
            sb.append("\n");
        }
        sb.append("  ");
        for (int c = 1; c <= seatsPerRow; c++) {
            if (c > 9) {
                sb.append(String.format(" %2d", c));
            } else if (c == 9) {
                sb.append(String.format(" %2d ", c));
            } else {
                sb.append(String.format(" %2d", c));
            }
        }
        sb.append("\n");
        return sb.toString();
    }
}

