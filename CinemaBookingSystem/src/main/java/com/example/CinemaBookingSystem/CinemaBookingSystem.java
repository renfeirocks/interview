package com.example.CinemaBookingSystem;

/*Since this request is asking for a **production-ready, high-performance, multithreading Java implementation** for a cinema booking system based on the provided specifications, I need to focus on generating the necessary Java classes and logic. I will design the system with concurrency in mind, using features like `ReentrantLock` or `synchronized` blocks to ensure thread-safe seat allocation, which is crucial for a booking system.

        The core components will be:

        1.  **`CinemaBookingSystem`**: The main class handling the menu, input, and managing the cinema hall.
        2.  **`CinemaHall`**: Manages the seating map, available seats, and the core booking logic (default and custom selection). This class will need thread-safe methods for seat allocation.
        3.  **`Booking`**: A data class to store booking information (ID, tickets booked, selected seats).
        4.  **`Seat`**: A simple class or enum/constant to represent a seat's state (Available, Booked by other, Booked by current transaction).

        I will use an array of arrays (`char[][]` or `Seat[][]`) for the seating map and use Java's concurrency utilities for thread safety.

        Given the complexity and requirement for a complete, production-ready solution, I will structure the response as a single, detailed code block containing all necessary classes.

        Since this is a code-generation task based on specific requirements from the provided images, and the requirements are self-contained, no external search is needed.

        **Analysis of Requirements:**

        * **Initialization:** Input `[Title] [Row] [SeatsPerRow]`. Example: `Inception 8 10`.
        * Rows: A-H (8 rows). Seats per row: 1-10. Total seats: 80.
        * **Default Seat Selection Rules (Priority 1):**
        1.  Start from **furthest row** from the screen (H $\rightarrow$ A).
        2.  Start from the **middle-most possible col**. (If 10 cols, middle-most is 5 or 6).
        3.  If a row is not enough, **overflow** to the next row closer to the screen.
        * **Custom Seat Selection Rules (Priority 2):**
        1.  Specify a **starting position** (e.g., `B05`).
        2.  Fill all empty seats **in the same row** to the **right** (e.g., B05, B06, B07...).
        3.  If not enough seats in the row, **overflow** to the **next row closer** to the screen.
        4.  Seat allocation for overflow follows the **default seat selection rules**.
        * **Seating Map Display:**
        * Unreserved: `.`
        * Reserved by other bookings: `#`
        * Reserved by current booking: `O`
        * **Concurrency:** Must be high-performance and multithreading-ready.
        * **Booking ID:** Sequential (GIC0001, GIC0002, ...).
        * **Menu Options:** [1] Book tickets, [2] Check bookings, [3] Exit.

        I will use the middle-most column as the one that allows the most symmetrical placement. For 10 columns (1-10), the indices are 0-9. The center is between 4 and 5. I will choose index 4 (column 5) as the starting point, then index 5 (column 6), index 3 (column 4), index 6 (column 7), and so on, radiating outwards. This gives the best center-biased seating.

        **Seating Priority (10 columns):**
        | Col Index | Col Number | Priority |
        | :-------: | :--------: | :------: |
        | 4 | 5 | 1 |
        | 5 | 6 | 2 |
        | 3 | 4 | 3 |
        | 6 | 7 | 4 |
        | 2 | 3 | 5 |
        | 7 | 8 | 6 |
        | 1 | 2 | 7 |
        | 8 | 9 | 8 |
        | 0 | 1 | 9 |
        | 9 | 10 | 10 |

        I will create a helper array for this middle-out column order.
        The row order is 'H' (index 7) $\rightarrow$ 'A' (index 0).

        The class `CinemaHall` will be the heart of the concurrency control.*/

        import java.util.ArrayList;
        import java.util.Collections;
        import java.util.HashMap;
        import java.util.List;
        import java.util.Map;
        import java.util.Scanner;
        import java.util.concurrent.atomic.AtomicInteger;
        import java.util.concurrent.locks.ReentrantLock;
        import java.util.stream.IntStream;

// --- Data Classes ---

/**
 * Represents a single booking in the cinema system.
 * This class is immutable after creation.
 */
class Booking {
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

// --- Utility Class ---

/**
 * Manages the state and booking logic of the cinema hall.
 * Thread-safe operations are ensured using ReentrantLock.
 */
class CinemaHall {
    private final String movieTitle;
    private final int totalRows;
    private final int seatsPerRow;
    // Map to store current bookings (BookingId -> Booking)
    final Map<String, Booking> bookings = new HashMap<>();
    // Seating map: 0 = Available, 1 = Booked
    final int[][] seatingMap;
    // Lock for all critical sections (seat allocation, map modification)
    final ReentrantLock bookingLock = new ReentrantLock();
    // Atomic counter for sequential booking IDs
    private final AtomicInteger bookingCounter = new AtomicInteger(0);

    // Pre-calculated column priority for "middle-most" selection
    private final int[] defaultColPriority;

    public CinemaHall(String movieTitle, int rows, int seatsPerRow) {
        if (rows > 26 || seatsPerRow > 50) {
            throw new IllegalArgumentException("Max rows is 26, max seats per row is 50.");
        }
        this.movieTitle = movieTitle;
        this.totalRows = rows;
        this.seatsPerRow = seatsPerRow;
        this.seatingMap = new int[rows][seatsPerRow];
        this.defaultColPriority = calculateDefaultColPriority(seatsPerRow);
    }

    private int[] calculateDefaultColPriority(int count) {
        int centerLeft = (count - 1) / 2; // e.g., 10 -> 4, 9 -> 4
        int centerRight = count / 2; // e.g., 10 -> 5, 9 -> 4

        List<Integer> priority = new ArrayList<>();
        if (count % 2 != 0) {
            // Odd number of seats, start from the single center seat
            priority.add(centerLeft);
            for (int i = 1; i <= centerLeft; i++) {
                priority.add(centerLeft - i); // Left outward
                priority.add(centerRight + i); // Right outward
            }
        } else {
            // Even number of seats, start from the two middle seats
            for (int i = 0; i < count / 2; i++) {
                priority.add(centerLeft - i); // Left outward
                priority.add(centerRight + i); // Right outward
            }
        }
        return priority.stream().mapToInt(i -> i).toArray();
    }

    public String getMovieTitle() {
        return movieTitle;
    }

    /**
     * Calculates the total number of available seats.
     */
    public int getAvailableSeatsCount() {
        return (int) IntStream.range(0, totalRows)
                .mapToLong(r -> IntStream.of(seatingMap[r]).filter(seat -> seat == 0).count())
                .sum();
    }

    /**
     * Converts a row index to its character label (0 -> 'A', 25 -> 'Z').
     */
    private char rowIndexToLabel(int rowIndex) {
        return (char) ('A' + rowIndex);
    }

    /**
     * Converts a seat position (e.g., "B05") to a row and column index array.
     * Returns null if the format is invalid or out of bounds.
     */
    private int[] parseSeatPosition(String position) {
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

    /**
     * Allocates seats based on the default rules (furthest row, middle-out).
     * This is a thread-safe operation.
     * @param numTickets The number of tickets to book.
     * @return A new Booking object or null if not enough seats.
     */
    public Booking bookDefault(int numTickets) {
        bookingLock.lock();
        try {
            if (numTickets > getAvailableSeatsCount()) {
                return null;
            }

            List<int[]> selectedSeats = new ArrayList<>();
            // Row order: H -> A (indices totalRows-1 to 0)
            for (int r = totalRows - 1; r >= 0 && selectedSeats.size() < numTickets; r--) {
                for (int c : defaultColPriority) {
                    if (seatingMap[r][c] == 0) {
                        selectedSeats.add(new int[]{r, c});
                        if (selectedSeats.size() == numTickets) break;
                    }
                }
            }

            if (selectedSeats.size() == numTickets) {
                return finalizeBooking(numTickets, selectedSeats);
            }
            return null; // Should not happen if count was checked, but safer.
        } finally {
            bookingLock.unlock();
        }
    }

    /**
     * Allocates seats based on a custom starting position and overflow rules.
     * This is a thread-safe operation.
     * @param numTickets The number of tickets to book.
     * @param startPosition The starting seat position (e.g., "B05").
     * @return A new Booking object or null if not enough seats, or invalid position.
     */
    public Booking bookCustom(int numTickets, String startPosition) {
        int[] start = parseSeatPosition(startPosition);
        if (start == null) {
            System.out.println("Invalid or out-of-bounds starting position: " + startPosition);
            return null;
        }

        bookingLock.lock();
        try {
            if (numTickets > getAvailableSeatsCount()) {
                return null;
            }

            List<int[]> selectedSeats = new ArrayList<>();
            int startRow = start[0];
            int startCol = start[1];

            // 1. Same row, rightward allocation
            for (int c = startCol; c < seatsPerRow && selectedSeats.size() < numTickets; c++) {
                if (seatingMap[startRow][c] == 0) {
                    selectedSeats.add(new int[]{startRow, c});
                }
            }

            // 2. Overflow to next rows (closer to screen), using default rules
            if (selectedSeats.size() < numTickets) {
                int ticketsToAllocate = numTickets - selectedSeats.size();

                // Row order: startRow-1 to 0 (rows closer to the screen)
                // Use defaultColPriority for overflow
                for (int r = startRow - 1; r >= 0 && ticketsToAllocate > 0; r--) {
                    for (int c : defaultColPriority) {
                        if (seatingMap[r][c] == 0) {
                            selectedSeats.add(new int[]{r, c});
                            ticketsToAllocate--;
                            if (ticketsToAllocate == 0) break;
                        }
                    }
                }
            }

            if (selectedSeats.size() == numTickets) {
                return finalizeBooking(numTickets, selectedSeats);
            }
            return null; // Failed to find contiguous seats/overflow
        } finally {
            bookingLock.unlock();
        }
    }

    /**
     * Generates ID, marks seats as booked (1), and stores the booking.
     */
    private Booking finalizeBooking(int numTickets, List<int[]> selectedSeats) {
        String bookingId = String.format("GIC%04d", bookingCounter.incrementAndGet());
        for (int[] seat : selectedSeats) {
            seatingMap[seat[0]][seat[1]] = 1; // Mark as booked
        }
        Booking newBooking = new Booking(bookingId, numTickets, selectedSeats);
        bookings.put(bookingId, newBooking);
        return newBooking;
    }

    /**
     * Retrieves a booking by its ID.
     */
    public Booking getBooking(String bookingId) {
        return bookings.get(bookingId);
    }

    /**
     * Generates a string representation of the seating map.
     * @param currentBookingId The ID of the booking to highlight with 'O'. Other bookings are '#'.
     * @return The formatted seating map string.
     */
    public String displaySeatingMap(String currentBookingId) {
        StringBuilder sb = new StringBuilder();
        sb.append("S C R E E N\n");
        sb.append("-----------\n");

        Map<String, List<int[]>> allBookedSeats = new HashMap<>();
        // Populate allBookedSeats outside the lock is safe as Booking is immutable.
        bookings.forEach((id, booking) -> allBookedSeats.put(id, booking.getSelectedSeats()));

        // Row order: H -> A (indices totalRows-1 to 0)
        for (int r = totalRows - 1; r >= 0; r--) {
            sb.append(rowIndexToLabel(r)).append(" ");
            for (int c = 0; c < seatsPerRow; c++) {
                char symbol = '.';
                // Check if seat is booked
                boolean isBooked = false;
                String seatBookingId = null;

                // Simple check for booking status
                if (seatingMap[r][c] == 1) {
                    isBooked = true;
                    // Find which booking this seat belongs to (inefficient, but necessary for display rule)
                    // In a production system, a Seat object would store its bookingId
                    for (Map.Entry<String, List<int[]>> entry : allBookedSeats.entrySet()) {
                        for (int[] seat : entry.getValue()) {
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
                        symbol = 'O'; // Current booking
                    } else {
                        symbol = '#'; // Other booking
                    }
                }
                sb.append(symbol).append(" ");
            }
            sb.append("\n");
        }

        sb.append("  ");
        for (int c = 1; c <= seatsPerRow; c++) {
            sb.append(c % 10).append(" "); // Use modulo for alignment (1-10)
        }
        sb.append("\n");

        return sb.toString();
    }
}

// --- Main Application Class ---

/**
 * Main application for the GIC Cinemas Booking System.
 * Handles user input and menu navigation.
 */
public class CinemaBookingSystem {
    private CinemaHall hall;
    private final Scanner scanner = new Scanner(System.in);
    // Use an AtomicBoolean to control the main loop in a multithreading context (though scanner use complicates this)
    private volatile boolean running = true;

    /**
     * Main entry point for the application.
     */
    public void start() {
        if (!initializeSystem()) {
            System.out.println("Failed to initialize system. Exiting.");
            return;
        }

        while (running) {
            displayMenu();
            String selection = scanner.nextLine().trim();
            switch (selection) {
                case "1":
                    handleBookTickets();
                    break;
                case "2":
                    handleCheckBookings();
                    break;
                case "3":
                    handleExit();
                    break;
                default:
                    System.out.println("Invalid selection. Please enter 1, 2, or 3.");
            }
        }
    }

    /**
     * Initializes the cinema hall configuration from user input.
     * @return true if successful, false otherwise.
     */
    private boolean initializeSystem() {
        System.out.println("Please define movie title and seating map in [Title] [Row] [SeatsPerRow] format:");
        System.out.print("> ");
        String line = scanner.nextLine().trim();

        String[] parts = line.split(" ");
        if (parts.length != 3) {
            System.out.println("Invalid format. Expected: [Title] [Row] [SeatsPerRow].");
            return false;
        }

        String title = parts[0];
        try {
            int rows = Integer.parseInt(parts[1]);
            int seatsPerRow = Integer.parseInt(parts[2]);
            hall = new CinemaHall(title, rows, seatsPerRow);
            return true;
        } catch (IllegalArgumentException e) {
            System.out.println("Error in dimensions: " + e.getMessage());
            return false;
        }
    }

    private void displayMenu() {
        System.out.println("\nWelcome to GIC Cinemas");
        System.out.printf("[1] Book tickets for %s (%d seats available)\n",
                hall.getMovieTitle(), hall.getAvailableSeatsCount());
        System.out.println("[2] Check bookings");
        System.out.println("[3] Exit");
        System.out.print("Please enter your selection:\n> ");
    }

    private void handleBookTickets() {
        int ticketsToBook = -1;
        while (ticketsToBook < 0) {
            System.out.println("Enter number of tickets to book, or enter blank to go back to main menu:");
            System.out.print("> ");
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) return;

            try {
                ticketsToBook = Integer.parseInt(input);
                if (ticketsToBook <= 0) {
                    System.out.println("Please enter a positive number of tickets.");
                    ticketsToBook = -1; // Keep loop running
                    continue;
                }
                if (ticketsToBook > hall.getAvailableSeatsCount()) {
                    System.out.printf("Sorry, there are only %d seats available.\n", hall.getAvailableSeatsCount());
                    ticketsToBook = -1; // Keep loop running
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
                ticketsToBook = -1; // Keep loop running
            }
        }

        // --- Seat Allocation (Initial Default) ---
        Booking newBooking = hall.bookDefault(ticketsToBook);

        if (newBooking == null) {
            System.out.println("Error: Could not reserve seats (system state inconsistency). Returning to main menu.");
            return;
        }

        System.out.printf("Successfully reserved %d %s tickets.\n", ticketsToBook, hall.getMovieTitle());
        System.out.printf("Booking id: %s\n", newBooking.getBookingId());
        System.out.println("Selected seats:");
        System.out.print(hall.displaySeatingMap(newBooking.getBookingId()));

        // --- Custom Seat Selection Loop ---
        while (true) {
            System.out.println("Enter blank to accept seat selection, or enter new seating position:");
            System.out.print("> ");
            String newPosition = scanner.nextLine().trim();

            if (newPosition.isEmpty()) {
                System.out.printf("Booking id: %s confirmed.\n", newBooking.getBookingId());
                break; // Exit loop, booking is confirmed
            }

            // Note: Re-booking logic requires un-reserving the old seats first, which is complex.
            // Simplified approach based on problem statement: The system attempts to replace the *entire* booking.
            // A production system would have a more robust mechanism (e.g., hold-lock-commit/rollback).

            // For this prototype, we'll try to find new seats and if successful, "move" the booking.
            // This requires a method to 'unbook' and 'rebook' which is not explicitly in CinemaHall.
            // To align with the example flow:
            // The example shows that after entering B03/B05, the map changes, and a new Booking ID is *not* generated.
            // It appears the prompt implies: If a new position is entered, the **existing** tickets are re-allocated.

            // Since this is a complex re-allocation, for a robust, thread-safe solution, I will
            // create a temporary unbook/rebook mechanism. A true production system would use transactions.
            System.out.println("Attempting to re-allocate seats...");

            // Temporary un-book (not exposed in public API, only for re-allocation logic)
            // A real system would use a transaction rollback mechanism.
            List<int[]> oldSeats = newBooking.getSelectedSeats();
            hall.bookingLock.lock();
            try {
                // Mark old seats as available (0)
                for (int[] seat : oldSeats) {
                    hall.seatingMap[seat[0]][seat[1]] = 0;
                }

                Booking tempBooking = hall.bookCustom(ticketsToBook, newPosition);

                if (tempBooking != null) {
                    // Success! The system has effectively moved the booking.
                    // Now, we need to update the Booking ID to the *original* one,
                    // and replace the booking in the map.
                    // Since Booking is immutable, we create a new one with the original ID.
                    Booking finalBooking = new Booking(newBooking.getBookingId(), ticketsToBook, tempBooking.getSelectedSeats());

                    // Remove the temporary booking (GIC00XX+1)
                    hall.bookings.remove(tempBooking.getBookingId());
                    // Update the original booking with new seats
                    hall.bookings.put(finalBooking.getBookingId(), finalBooking);
                    newBooking = finalBooking; // Update reference for next loop/confirmation

                    System.out.printf("Re-reserved %d %s tickets to new selection.\n", ticketsToBook, hall.getMovieTitle());
                    System.out.println("Selected seats:");
                    System.out.print(hall.displaySeatingMap(newBooking.getBookingId()));
                } else {
                    // Re-allocation failed. Must restore the old booking.
                    for (int[] seat : oldSeats) {
                        hall.seatingMap[seat[0]][seat[1]] = 1; // Mark old seats as booked (1) again
                    }
                    System.out.println("Could not re-reserve seats with the starting position: " + newPosition + ". Please try again.");
                    // Display the seating map with the old selection highlighted.
                    System.out.print(hall.displaySeatingMap(newBooking.getBookingId()));
                }
            } finally {
                hall.bookingLock.unlock();
            }
        }
    }

    private void handleCheckBookings() {
        System.out.println("Enter booking id, or enter blank to go back to main menu:");
        System.out.print("> ");
        String bookingId = scanner.nextLine().trim();

        if (bookingId.isEmpty()) return;

        Booking booking = hall.getBooking(bookingId);

        if (booking == null) {
            System.out.println("Error: Booking id not found: " + bookingId);
        } else {
            System.out.printf("Booking id: %s\n", booking.getBookingId());
            System.out.println("Selected seats:");
            System.out.print(hall.displaySeatingMap(bookingId));
        }
    }

    private void handleExit() {
        System.out.println("Thank you for using GIC Cinemas system. Bye!");
        running = false;
        scanner.close(); // Close scanner cleanly
    }

    public static void main(String[] args) {
        // High performance and multithreading readiness is achieved by:
        // 1. Thread-safe CinemaHall (using ReentrantLock for critical sections).
        // 2. Using AtomicInteger for sequential ID generation.
        // 3. Immutability of the Booking class.

        // Note: The main application loop uses a single Scanner, which is inherently
        // not thread-safe and is best for a single-user CLI. For a *true* multi-threaded
        // application (e.g., a web server), the CinemaHall and Booking logic would be
        // exposed through an API, and each request would run on its own thread,
        // safely accessing the hall's state using the locking mechanisms.

        // To demonstrate the *readiness* for multithreading, one could start multiple
        // threads that all call a booking method on the CinemaHall instance.
        // E.g., a simple test driver for multithreading:
        // CinemaHall hallTest = new CinemaHall("Test", 8, 10);
        // Runnable r1 = () -> hallTest.bookDefault(2);
        // new Thread(r1).start();
        // new Thread(r1).start();

        // However, for the specified CLI workflow, we start the single-user application.
        new CinemaBookingSystem().start();
    }
}
