# Cinema Booking System

## Overview
This Cinema Booking System is a modular, thread-safe application for managing seat reservations in a cinema hall. It allows users to book seats for movies, supports customizable seat selection strategies, and ensures concurrency safety for multiple users booking simultaneously.

## Assumptions
- The cinema hall layout (number of rows and seats per row) is provided at startup.
- Each booking is for a single movie session; multi-session or multi-hall support is not included.
- Seat selection is based on either default or custom strategies, but only one strategy is active per booking.
- No payment or user authentication is implemented; the focus is on seat allocation logic.
- All bookings are processed in-memory; no external database is used.

A robust, extensible, and thread-safe cinema seat booking system implemented in Java. This project demonstrates best practices in modular design, concurrency, extensibility, and error handling for a real-world booking application.

## Features
- **Seat Booking:** Reserve seats using default or custom selection logic.
- **Thread Safety:** All seat allocation is concurrency-safe.
- **Customizable Strategies:** Pluggable seat selection and priority strategies.
- **Error Handling:** Custom exceptions for booking errors and invalid input.
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

- **Controller:** Handles user/API requests and routes them to services.
- **Service:** Contains business logic for booking, seat allocation, and concurrency.
- **Model:** Defines core data structures (Booking, Movie, Seat).
- **Strategy:** Pluggable seat selection and priority algorithms.
- **Configuration:** Manages hall layout and system properties.
- **Exception:** Custom error handling for booking and input issues.
- **Utility:** Helper functions for seat mapping and validation.

Directory layout:
```
CinemaBookingSystem/
├── configuration/   # Hall setup and system config
├── controller/      # API endpoints and CLI entry
├── exception/       # Custom exceptions
├── model/           # Booking and input data models
├── service/         # Business logic and seat management
├── strategy/        # Seat selection and priority strategies
├── utility/         # Helper classes
```

## Extensibility & Customization
- **Seating Strategies:** Implement `SeatingStrategy` for new seat selection logic.
- **Priority Strategies:** Implement `DefaultPriorityStrategy` for custom seat allocation order.
- **Error Handling:** Extend custom exceptions for new error types.

## Error Handling
- All booking and seat selection errors throw custom exceptions for clear diagnostics.
- Invalid input, overbooking, and seat position errors are handled gracefully.

## Cinema Booking System Test Cases

### Positive Test Cases
- **bookSingleSeat_DefaultAndCustomStrategy:**
  Ensures a user can successfully book a seat for a valid movie and showtime using both default and custom strategies.
- **bookMultipleSeats_DefaultAndCustomStrategy:**
  Allows booking more than one seat at once, verifying correct seat allocation for both strategies.
- **bookLastAvailableSeat_DefaultStrategy:**
  Ensures the system can handle booking the very last seat in the hall.
- **bookMultipleSeatsInSingleTransaction:**
  Allows booking multiple seats in a single transaction.
- **cancelPreviouslyBookedSeat:**
  Tests that a user can cancel a booking and the seat becomes available again.
- **viewAvailableSeats:**
  Displays the current seating map, showing which seats are booked and available.
- **bookSeatAfterCancellation:**
  Verifies that a cancelled seat can be rebooked by another user.
- **bookSeatsForDifferentMoviesAndShowtimes:**
  Confirms that bookings are isolated per movie and showtime.
- **bookSeats_CustomStrategyOverflow:**
  Tests custom strategy overflow logic for seat selection.
- **bookSeats_CustomStrategyPrioritizesGivenSeat:**
  Verifies custom strategy prioritizes the given seat.
- **bookSeats_DefaultStrategyFillsAvailableSeats:**
  Ensures default strategy fills available seats correctly.
- **testDefaultPriorityStrategy:**
  Verifies default priority strategy seat selection.
- **testCustomSeatingStrategyFillRowAndOverflow:**
  Tests custom strategy row fill and overflow.
- **testCustomSeatingStrategyFillRowWithBookedSeats:**
  Verifies custom strategy skips already booked seats.
- **testDefaultSeatingStrategy:**
  Ensures default strategy fills rows in priority order.
- **testBookFourCornerSeatsWhenOnlyCornersLeft:**
  Verifies correct allocation when only the four corners remain.
- **bookWithDuplicateSeatsInSameBooking:**
  Checks system robustness for duplicate seats in a booking.
- **bookWithEmptyBookingId:**
  Checks system robustness for empty booking IDs.
- **bookWithSpecialCharactersInBookingId:**
  Checks system robustness for special character booking IDs.
- **bookWithMaxTickets:**
  Validates booking logic for maximum allowed ticket count.
- **bookWithMinTickets:**
  Validates booking logic for minimum allowed ticket count.
- **stressTestHighVolumeBookings:**
  Simulates booking all seats in the hall to test performance and correctness.
- **bookSeats_SuccessfulBooking_ShouldReturnBooking:**
  Ensures successful booking returns a valid Booking object.
- **bookSeats_SingleSeat_ShouldReturnBooking:**
  Ensures single seat booking returns a valid Booking object.
- **bookSeats_LastRemainingSeat_ShouldSucceed:**
  Ensures booking the last seat succeeds.
- **bookSeats_MoreThanAvailable_ShouldReturnNull:**
  Ensures booking more seats than available returns null.

### Negative Test Cases
- **bookSeats_ZeroSeats_ShouldThrowException:**
  Throws an exception for zero seat booking.
- **bookSeats_NegativeSeats_ShouldThrowException:**
  Throws an exception and logs an error for negative seat booking.
- **reallocateSeats_ToAlreadyBooked_ShouldReturnNull:**
  Ensures seat reallocation fails if the target seat is already booked.
- **reallocateSeats_ToInvalidSeat_ShouldReturnNull:**
  Ensures seat reallocation fails if the target seat is invalid.

### Multi-threading Test Cases
- **concurrentBookingSameSeat:**
  Simultaneously book the same seat from multiple threads; only one should succeed.
- **concurrentBookingSameSeat_DefaultStrategy:**
  Simultaneously book the same seat using default strategy; only one should succeed and seat follows defaultColPriority.
- **concurrentCancellationSameBooking:**
  Simultaneously cancel the same booking from multiple threads; only one should succeed.
- **concurrentBookingDifferentSeats:**
  Simultaneously book different seats for the same showtime; all should succeed if seats are available.
- **testBookingWithVariousHallSizes:**
  Verifies booking logic for various hall sizes.
- **testMaxRowsAndSeatsPerRow:**
  Verifies booking logic for maximum rows and seats per row.
- **testOneRowMaxSeats:**
  Verifies booking logic for one row with maximum seats.
- **testMaxRowsOneSeat:**
  Verifies booking logic for maximum rows with one seat per row.
