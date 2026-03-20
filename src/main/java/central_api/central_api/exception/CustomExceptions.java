package central_api.central_api.exception;

public class CustomExceptions {

    public static class UserNotFoundException extends RuntimeException {
        public UserNotFoundException(String message) {
            super(message);
        }
    }

    public static class InvalidCredentialsException extends RuntimeException {
        public InvalidCredentialsException(String message) {
            super(message);
        }
    }

    public static class FlightNotFoundException extends RuntimeException {
        public FlightNotFoundException(String message) {
            super(message);
        }
    }

    public static class SeatNotAvailableException extends RuntimeException {
        public SeatNotAvailableException(String message) {
            super(message);
        }
    }

    public static class BookingException extends RuntimeException {
        public BookingException(String message) {
            super(message);
        }
    }

    public static class AirlineNotFoundException extends RuntimeException {
        public AirlineNotFoundException(String message) {
            super(message);
        }
    }

    public static class UnauthorizedException extends RuntimeException {
        public UnauthorizedException(String message) {
            super(message);
        }
    }

    public static class ValidationException extends RuntimeException {
        public ValidationException(String message) {
            super(message);
        }
    }

    // ✅ BadRequestException for 400 Bad Request errors
    public static class BadRequestException extends RuntimeException {
        public BadRequestException(String message) {
            super(message);
        }

        public BadRequestException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}