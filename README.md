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
- Book a seat for a valid movie, showtime, and seat.
- Book multiple seats in a single transaction.
- Cancel a previously booked seat.
- View available seats for a showtime.
- Book a seat after a cancellation (seat becomes available again).
- Book seats for different movies and showtimes.
- Apply a valid discount or promo code.
- Book a seat using different payment methods.
- Retrieve booking history for a user.
- Book a seat at the last available slot.

### Negative Test Cases
- Book a seat that is already booked.
- Book a seat for a non-existent movie or showtime.
- Book a seat with invalid seat number or format.
- Book a seat with invalid payment details.
- Cancel a booking that does not exist.
- Book more seats than available.
- Book a seat with an expired promo code.
- Book a seat with missing required fields.
- Attempt to book a seat after the showtime has started.
- Book a seat with insufficient account balance.

### Multi-threading Test Cases
- Simultaneously book the same seat from multiple threads (only one should succeed).
- Simultaneously cancel the same booking from multiple threads (only one should succeed).
- Simultaneously book different seats for the same showtime (all should succeed if available).
- Simultaneously book seats for different showtimes (no cross-interference).
- Simultaneously update seat availability and process bookings (ensure data consistency).
- Simultaneously apply and remove promo codes during booking (ensure correct application).
- Simultaneously retrieve available seats while bookings are being made (ensure accurate seat status).
