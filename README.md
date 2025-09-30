# Cinema Booking System

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

### Build & Run
```bash
mvn clean install
mvn spring-boot:run
```

### CLI Usage
- On startup, enter: `[Title] [Row] [SeatsPerRow]` (e.g., `Inception 8 12`)
- Menu options:
  - [1] Book tickets
  - [2] Check bookings
  - [3] Exit
- Follow prompts to book, reallocate, or view seat map.

## Project Structure
```
CinemaBookingSystem/
├── configuration/         # Config classes (e.g., max rows/seats)
├── controller/            # REST controllers (if API enabled)
├── exception/             # Custom exceptions (e.g., BookingException)
├── model/                 # Data models (Booking, etc.)
├── service/               # Booking services, CinemaHall, interfaces
├── strategy/              # Seat selection and priority strategies
├── utility/               # Utility classes (SeatUtil, etc.)
```

## Extensibility & Customization
- **Seating Strategies:** Implement `SeatingStrategy` for new seat selection logic.
- **Priority Strategies:** Implement `DefaultPriorityStrategy` for custom seat allocation order.
- **Error Handling:** Extend custom exceptions for new error types.

## Error Handling
- All booking and seat selection errors throw custom exceptions for clear diagnostics.
- Invalid input, overbooking, and seat position errors are handled gracefully.

## Contributing
1. Fork the repository
2. Create your feature branch (`git checkout -b feature/YourFeature`)
3. Commit your changes
4. Push to the branch (`git push origin feature/YourFeature`)
5. Open a pull request

## License
This project is licensed under the MIT License.

## Contact
For questions or support, please open an issue or contact the maintainer.
