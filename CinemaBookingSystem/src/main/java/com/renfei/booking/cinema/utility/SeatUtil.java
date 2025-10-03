package com.renfei.booking.cinema.utility;

public class SeatUtil {
    public static char rowIndexToLabel(int rowIndex) {
        return (char) ('A' + rowIndex);
    }

    public static int[] parseSeatPosition(String position, int totalRows, int seatsPerRow) {
        if (position == null || position.length() < 2 || position.length() > 3) return null;
        char rowLabel = Character.toUpperCase(position.charAt(0));
        int rowIndex = rowLabel - 'A';
        int colIndex;
        try {
            int colNumber = Integer.parseInt(position.substring(1));
            colIndex = colNumber - 1;
        } catch (NumberFormatException e) {
            return null;
        }
        if (rowIndex >= 0 && rowIndex < totalRows && colIndex >= 0 && colIndex < seatsPerRow) {
            return new int[]{rowIndex, colIndex};
        }
        return null;
    }
}
