# Cinema Booking System

## Overview
This Cinema Booking System is a modular, thread-safe application for managing seat reservations in a cinema hall. It allows users to book seats for movies, supports customizable seat selection strategies, and ensures concurrency safety for multiple users booking simultaneously.

## Assumptions
- The cinema hall layout (number of rows and seats per row) is provided at startup.
- Each booking is for a single movie session; multi-session or multi-hall support is not included.
- Seat selection is based on either default or custom strategies, but only one strategy is active per booking.
- No payment or user authentication is implemented; the focus is on seat allocation logic.
- All bookings are processed in-memory; no external database is used.

## Features
- **Seat Booking:** Reserve seats using default or custom selection logic.
- **Thread Safety:** All seat allocation is concurrency-safe.
- **Customizable Strategies:** Pluggable seat selection and priority strategies.
- **Extensible Architecture:** Easily add new booking logic, seating arrangements, or priority strategies.

## Technology Stack
- Java 17+
- Maven
- Spring Boot (for REST API and configuration)

## Getting Started

### Prerequisites
- Java 17 or higher
- Maven

### Setup & Run
1. Open a terminal and navigate to the project directory:
   ```bash
   cd /path/to/CinemaBookingSystem
   ```
2. Build the project:
   ```bash
   mvn clean install
   ```
3. Run the application:
   ```bash
   mvn spring-boot:run
   ```
4. Access the application:
   - For CLI: On startup, enter `[Title] [Row] [SeatsPerRow]` (e.g., `Inception 8 12`)
   - For REST API: Use endpoints as defined in the controller (see source code for details).

### Running Tests
To run unit tests:
```bash
mvn test
```

### Notes
- Ensure JAVA_HOME is set to Java 17+.
- No external database or authentication is required.
- For advanced configuration, edit `src/main/resources/application.properties`.

### CLI Usage
- On startup, enter: `[Title] [Row] [SeatsPerRow]` (e.g., `Inception 8 12`)
- Menu options:
  - [1] Book tickets
  - [2] Check bookings
  - [3] Exit
- Follow prompts to book, reallocate, or view seat map.

## Project Structure

The system follows a layered architecture for clarity and extensibility:

- **Controller:** Handles user/API requests and routes them to services. Includes CLI logic with looping for booking checks.
- **Service:** Contains business logic for booking, seat allocation, and concurrency.
- **Model:** Defines core data structures (Booking, Movie, Seat).
- **Strategy:** Pluggable seat selection and priority algorithms.
- **Configuration:** Manages hall layout and system properties.
- **Utility:** Helper functions for seat mapping and validation.

Directory layout:
```
CinemaBookingSystem/
├── configuration/   # Hall setup and system config
├── controller/      # API endpoints and CLI entry
├── model/           # Booking and input data models
├── service/         # Business logic and seat management
├── strategy/        # Seat selection and priority strategies
├── utility/         # Helper classes
```

## Extensibility & Customization
- **Seating Strategies:** Implement `SeatingStrategy` for new seat selection logic.
- **Priority Strategies:** Implement `DefaultPriorityStrategy` for custom seat allocation order.

## Error Handling
- All booking and seat selection errors throw custom exceptions for clear diagnostics.
- Invalid input, overbooking, and seat position errors are handled gracefully.

## Cinema Booking System Test Cases

### Valid Test Cases
- **bookLastAvailableSeat_DefaultStrategy:** Ensures the default strategy can successfully book the very last remaining seat in the hall.
- **bookMultipleSeats_DefaultAndCustomStrategy:** Checks that the default strategy selects a group of seats sequentially following default priority, and the custom strategy selects a group starting from the specified position (e.g., 'C3') and filling rightward.
- **bookMultipleSeatsInSingleTransaction:** Confirms that multiple seats can be successfully booked and stored in the bookings map as part of a single Booking object.
- **bookSeatAfterCancellation:** Verifies that a seat can be successfully booked after its previous booking was canceled.
- **bookSeats_CustomStrategyOverflow:** Tests the custom strategy's ability to fill a row starting from a specific point and correctly overflow to the next available row/seat following the default priority when the initial row is full.
- **bookSeats_LastRemainingSeat_ShouldSucceed:** Confirms successful booking when there is only one seat left in a 1×1 hall.
- **bookSeats_MoreThanAvailable_ShouldReturnNull:** Checks that attempting to book more seats than available returns null and doesn't change the available seat count.
- **bookSeats_SingleSeat_ShouldReturnBooking:** Basic check for successful booking of a single seat using the default strategy.
- **bookSeats_SuccessfulBooking_ShouldReturnBooking:** Basic check for successful booking of two seats using the default strategy, verifying return value and available seat count.
- **bookSingleSeat_DefaultAndCustomStrategy:** Verifies that the default strategy selects the preferred single seat (center-row, preferred column) and the custom strategy selects the single seat based on the provided starting position (e.g., 'B2').
- **bookWithMaxTickets:** Verifies successful booking for a group size equal to a practical limit (e.g., 5).
- **cancelPreviouslyBookedSeat:** Tests the removal of a booking from the bookings map, simulating a successful cancellation.
- **reallocateSeats_SuccessfulReallocation:** Tests the successful reallocation of a single seat booking to a new specified custom seat (e.g., 'C5').
- **stressTestHighVolumeBookings:** Simulates high volume by booking all 100 seats one-by-one to check system stability and state management under load.
- **testBookFourCornerSeatsWhenOnlyCornersLeft:** An edge-case test ensuring the default strategy can successfully book scattered seats (the four corners) when they are the only ones remaining.
- **testBookingWithVariousHallSizes:** A parameterized test using @CsvSource to ensure booking logic works correctly across various small, medium, and large cinema hall dimensions.
- **testCustomSeatingStrategyFillRowAndOverflow:** Tests complex custom strategy logic where it fills a row and then overflows to a subsequent row based on priority.
- **testCustomSeatingStrategyFillRowWithBookedSeats:** Verifies the custom strategy's ability to skip an already booked seat within the target row and continue selecting seats before overflowing to the next row.
- **testDefaultPriorityStrategy:** Asserts that when booking multiple seats in a small hall, the default strategy prioritizes the row furthest from the screen (row 0).
- **testDefaultSeatingStrategy:** Checks that for a small hall, the default strategy fills all seats, prioritizing the rows according to the default order (row 0, then row 1).
- **testMaxRowsAndSeatsPerRow:** Confirms successful booking in a hall configured with the maximum allowed rows and seats per row.
- **testMaxRowsOneSeat:** Tests booking in a hall with the maximum number of rows but only one seat per row.
- **testOneRowMaxSeats:** Tests booking in a hall with a single row and the maximum number of seats.
- **viewAvailableSeats:** Checks that after a booking is made, the booked seat is considered occupied, while an adjacent unbooked seat remains free.

### Negative and Edge Cases
- **testHandleCheckBookingsInvalidId:** Verifies that the system correctly detects and sets the error message when a user attempts to check details for a non-existent booking ID (GIC0000).
- **testPromptForTicketsBookingsAllBooked:** Simulates a booking attempt when all tickets are already booked, ensuring the correct error message is generated.
- **testPromptForTicketsBookingsOverBooked:** Simulates a booking attempt for a number of tickets greater than the number of available seats, checking for the correct "not enough seats" error message.
- **testPromptForTicketsBookingsNumberFormatError:** Verifies input validation by checking for the correct error message when the user provides non-numeric input (e.g., 'A') for the number of tickets.
- **testMovieTitleNullOrEmpty:** Checks that the system handles null or empty/blank movie titles during CinemaHall initialization by setting the appropriate error message, but does not throw an exception (graceful handling).
- **testRowsOrSeatsExceedMax:** Tests hall creation with dimensions that exceed the maximum allowed rows or seats per row (CinemaHallConfig.MAX_ROWS), checking for the specific dimension limit error message.
- **testRowsOrSeatsBelowMin:** Tests hall creation with dimensions that are below the minimum allowed rows or seats per row (CinemaHallConfig.MIN_ROWS), checking for the "at least 1" dimension error message.
- **testStartPositionOutOfBoundsAndInvalidReturnsEmptyList:** Tests the custom seating strategy's input validation for the start position: checking for an empty list and no seats selected when the start position is out of bounds (e.g., 'Z1', 'A6') or null.
- **bookCustom_Overbooking_ShouldReturnNullAndSetError:** Simulates a scenario where only one seat is left, and a user attempts a custom booking for three tickets, ensuring the operation fails, returns null, and sets the overbooking error message.
- **bookSeats_NegativeSeats_ShouldThrowException:** Checks for the correct error message when a user attempts to book a negative number of tickets (e.g., -1).
- **reallocateSeats_ToInvalidSeat_ShouldReturnNull:** Tests the failure case for reallocation where the target seat position (e.g., 'Z99') is invalid or out of bounds, ensuring the method returns null and sets the appropriate reallocation failure error message.

### Multi-Threading Test Cases
- **concurrentBookingSameSeat_DefaultStrategy:** A crucial stress test using a CountDownLatch and ExecutorService to simulate multiple threads attempting to book the same number of tickets. It ensures thread safety by confirming all bookings succeed and that the booked seats follow the default priority strategy sequentially, implying atomic booking operations.
- **concurrentBookingDifferentSeats:** Simulates concurrent bookings of seats in different rows to verify thread-safe state management when the operations do not directly conflict. Uses `synchronized (cinemaHall)` for thread-safe map update.
